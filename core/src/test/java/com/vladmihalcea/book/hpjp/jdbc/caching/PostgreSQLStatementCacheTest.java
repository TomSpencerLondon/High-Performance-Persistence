package com.vladmihalcea.book.hpjp.jdbc.caching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.TaskEntityProvider;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLStatementCacheTest extends AbstractPostgreSQLIntegrationTest {

    public static final String INSERT_TASK = "insert into task (id, status) values (?, ?)";

    private TaskEntityProvider entityProvider = new TaskEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "100");
        properties.put("hibernate.order_inserts", "true");
    }

    @Override
    public void afterInit() {
        doInJDBC(connection -> {
            try (PreparedStatement postStatement = connection.prepareStatement(INSERT_TASK)) {
                int postCount = getPostCount();

                int index;

                for (int i = 0; i < postCount; i++) {
                    index = 0;
                    TaskEntityProvider.StatusType statusType;
                    if (i > postCount * 0.99) {
                        statusType = TaskEntityProvider.StatusType.FAILED;
                    } else if (i > postCount * 0.95) {
                        statusType = TaskEntityProvider.StatusType.TO_D0;
                    } else {
                        statusType = TaskEntityProvider.StatusType.DONE;
                    }
                    postStatement.setInt(++index, i);
                    postStatement.setString(++index, statusType.name());
                    postStatement.executeUpdate();
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void testStatementCaching() {
        doInJDBC(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT *
                FROM task
                WHERE status = ?
                """
            )) {
                statement.setString(1, TaskEntityProvider.StatusType.FAILED.name());
                statement.executeQuery();
            }
        });
    }

    protected int getPostCount() {
        return 10_000;
    }

    @Override
    protected boolean proxyDataSource() {
        return false;
    }
}
