package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.hibernate.config;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.logging.LoggingStatementInspector;
import com.vladmihalcea.book.hpjp.util.DataSourceProxyType;
import com.vladmihalcea.book.hpjp.util.logging.InlineQueryLogEntryCreator;
import com.vladmihalcea.hibernate.type.util.ClassImportIntegrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Properties;

/**
 *
 * @author Vlad Mihalcea
 */
@Configuration
@PropertySource({"/META-INF/jdbc-hsqldb.properties"})
@ComponentScan(basePackages = "com.vladmihalcea.book.hpjp.hibernate.transaction.spring.hibernate")
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class HibernateTransactionManagerConfiguration {

    public static final String DATA_SOURCE_PROXY_NAME = DataSourceProxyType.DATA_SOURCE_PROXY.name();

    @Value("${jdbc.dataSourceClassName}")
    private String dataSourceClassName;

    @Value("${jdbc.url}")
    private String jdbcUrl;

    @Value("${jdbc.username}")
    private String jdbcUser;

    @Value("${jdbc.password}")
    private String jdbcPassword;

    @Value("${hibernate.dialect}")
    private String hibernateDialect;

    @Bean(destroyMethod = "close")
    public HikariDataSource actualDataSource() {
        Properties driverProperties = new Properties();
        driverProperties.setProperty("url", jdbcUrl);
        driverProperties.setProperty("user", jdbcUser);
        driverProperties.setProperty("password", jdbcPassword);

        Properties properties = new Properties();
        properties.put("dataSourceClassName", dataSourceClassName);
        properties.put("dataSourceProperties", driverProperties);
        properties.setProperty("maximumPoolSize", String.valueOf(3));
        return new HikariDataSource(new HikariConfig(properties));
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public DataSource dataSource() {
        SLF4JQueryLoggingListener loggingListener = new SLF4JQueryLoggingListener();
        loggingListener.setQueryLogEntryCreator(new InlineQueryLogEntryCreator());
        return ProxyDataSourceBuilder
                .create(actualDataSource())
                .name(DATA_SOURCE_PROXY_NAME)
                .listener(loggingListener)
                .build();
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory() {
        LocalSessionFactoryBean sessionFactoryBean = new LocalSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource());
        sessionFactoryBean.setPackagesToScan(packagesToScan());
        sessionFactoryBean.setHibernateProperties(additionalProperties());
        sessionFactoryBean.setHibernateIntegrators(new ClassImportIntegrator(Arrays.asList(PostDTO.class)));
        return sessionFactoryBean;
    }

    @Bean
    public HibernateTransactionManager transactionManager(SessionFactory sessionFactory){
        //HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        HibernateTransactionManager transactionManager = new MonitoringHibernateTransactionManager();
        transactionManager.setSessionFactory(sessionFactory);
        return transactionManager;
    }

    @Bean
    public TransactionTemplate transactionTemplate(SessionFactory sessionFactory) {
        return new TransactionTemplate(transactionManager(sessionFactory));
    }

    protected Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", hibernateDialect);
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        properties.put(
            "hibernate.session_factory.statement_inspector",
            new LoggingStatementInspector("com.vladmihalcea.book.hpjp.hibernate.transaction")
        );
        return properties;
    }

    protected String[] packagesToScan() {
        return new String[]{
            "com.vladmihalcea.book.hpjp.hibernate.transaction.forum"
        };
    }
}
