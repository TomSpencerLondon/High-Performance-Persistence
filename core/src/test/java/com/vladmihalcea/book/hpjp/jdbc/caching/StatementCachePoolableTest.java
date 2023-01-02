package com.vladmihalcea.book.hpjp.jdbc.caching;

import com.vladmihalcea.book.hpjp.util.DataSourceProviderIntegrationTest;
import com.vladmihalcea.book.hpjp.util.ReflectionUtils;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.JTDSDataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.MySQLDataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.OracleDataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.PostgreSQLDataSourceProvider;
import net.sourceforge.jtds.jdbcx.JtdsDataSource;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;

/**
 * StatementCacheTest - Test Statement cache
 *
 * @author Vlad Mihalcea
 */
public class StatementCachePoolableTest extends DataSourceProviderIntegrationTest {

    public static class CachingOracleDataSourceProvider extends OracleDataSourceProvider {
        private final int cacheSize;

        CachingOracleDataSourceProvider(int cacheSize) {
            this.cacheSize = cacheSize;
        }

        @Override
        public DataSource dataSource() {
            DataSource dataSource = super.dataSource();
            try {
                Properties connectionProperties = ReflectionUtils.invokeGetter(dataSource, "connectionProperties");
                if(connectionProperties == null) {
                    connectionProperties = new Properties();
                }
                connectionProperties.put("oracle.jdbc.implicitStatementCacheSize", Integer.toString(cacheSize));
                ReflectionUtils.invokeSetter(dataSource, "connectionProperties", connectionProperties);
            } catch (Exception e) {
                fail(e.getMessage());
            }
            return dataSource;
        }

        @Override
        public String toString() {
            return "CachingOracleDataSourceProvider{" +
                    "cacheSize=" + cacheSize +
                    '}';
        }
    }

    public static class CachingJTDSDataSourceProvider extends JTDSDataSourceProvider {
        private final int cacheSize;

        CachingJTDSDataSourceProvider(int cacheSize) {
            this.cacheSize = cacheSize;
        }

        @Override
        public DataSource dataSource() {
            JtdsDataSource dataSource = (JtdsDataSource) super.dataSource();
            dataSource.setMaxStatements(cacheSize);
            return dataSource;
        }

        @Override
        public String toString() {
            return "CachingJTDSDataSourceProvider{" +
                    "cacheSize=" + cacheSize +
                    '}';
        }
    }

    public static class CachingPostgreSQLDataSourceProvider extends PostgreSQLDataSourceProvider {
        private final int cacheSize;

        CachingPostgreSQLDataSourceProvider(int cacheSize) {
            this.cacheSize = cacheSize;
        }

        @Override
        public DataSource dataSource() {
            PGSimpleDataSource dataSource = (PGSimpleDataSource) super.dataSource();
            dataSource.setPreparedStatementCacheQueries(cacheSize);
            return dataSource;
        }

        @Override
        public String toString() {
            return "CachingPostgreSQLDataSourceProvider{" +
                    "cacheSize=" + cacheSize +
                    '}';
        }
    }

    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    public static final String INSERT_POST_COMMENT = "insert into post_comment (post_id, review, version, id) values (?, ?, ?, ?)";

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    public StatementCachePoolableTest(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @Parameterized.Parameters
    public static Collection<DataSourceProvider[]> rdbmsDataSourceProvider() {
        List<DataSourceProvider[]> providers = new ArrayList<>();
        providers.add(new DataSourceProvider[]{
                new CachingOracleDataSourceProvider(1)
        });
        providers.add(new DataSourceProvider[]{
                new CachingJTDSDataSourceProvider(1)
        });
        providers.add(new DataSourceProvider[]{
                new CachingPostgreSQLDataSourceProvider(1)
        });
        MySQLDataSourceProvider mySQLCachingDataSourceProvider = new MySQLDataSourceProvider();
        mySQLCachingDataSourceProvider.setUseServerPrepStmts(true);
        mySQLCachingDataSourceProvider.setCachePrepStmts(true);
        providers.add(new DataSourceProvider[]{
                mySQLCachingDataSourceProvider
        });
        return providers;
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    public void init() {
        super.init();
        doInJDBC(connection -> {
            try (
                    PreparedStatement postStatement = connection.prepareStatement(INSERT_POST);
                    PreparedStatement postCommentStatement = connection.prepareStatement(INSERT_POST_COMMENT);
            ) {
                int postCount = getPostCount();
                int postCommentCount = getPostCommentCount();

                int index;

                for (int i = 0; i < postCount; i++) {
                    index = 0;
                    postStatement.setString(++index, String.format("Post no. %1$d", i));
                    postStatement.setInt(++index, 0);
                    postStatement.setLong(++index, i);
                    postStatement.executeUpdate();
                }

                for (int i = 0; i < postCount; i++) {
                    for (int j = 0; j < postCommentCount; j++) {
                        index = 0;
                        postCommentStatement.setLong(++index, i);
                        postCommentStatement.setString(++index, String.format("Post comment %1$d", j));
                        postCommentStatement.setInt(++index, (int) (Math.random() * 1000));
                        postCommentStatement.setLong(++index, (postCommentCount * i) + j);
                        postCommentStatement.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void selectWhenCaching() {
        AtomicInteger counter = new AtomicInteger();
        doInJDBC(connection -> {
            for (int i = 0; i < 2; i++) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT p.title, pd.created_on
                        FROM post p
                        LEFT JOIN post_details pd ON p.id = pd.id
                        WHERE EXISTS (
                           SELECT 1 
                           FROM post_comment 
                           WHERE post_id > p.id AND version = ?
                        )"""
                )) {
                    statement.setPoolable(false);
                    statement.setInt(1, counter.incrementAndGet());
                    statement.execute();
                } catch (Throwable e) {
                    LOGGER.error("Failed test", e);
                }
            }
        });
        LOGGER.info("When using {}, throughput is {} statements",
                dataSourceProvider(),
                counter.get());
    }

    protected int getPostCount() {
        return 1000;
    }

    protected int getPostCommentCount() {
        return 5;
    }

    @Override
    protected boolean proxyDataSource() {
        return false;
    }
}
