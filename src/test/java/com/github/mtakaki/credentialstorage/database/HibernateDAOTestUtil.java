package com.github.mtakaki.credentialstorage.database;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.context.internal.ManagedSessionContext;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.github.mtakaki.credentialstorage.database.model.Credential;

import io.dropwizard.db.ManagedPooledDataSource;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import lombok.Getter;

/**
 * Test utility class that will setup hibernate and {@link SessionFactory}. For
 * each test it will spawn an in memory database so every test is completely
 * isolated from each other.
 *
 * @author mtakaki
 *
 */
public class HibernateDAOTestUtil {
    @Getter
    private SessionFactory sessionFactory;
    @Getter
    private Session session;
    private final List<Class<?>> entitiesClass;

    public HibernateDAOTestUtil() {
        // Getting rid of too many warnings in the console.
        final Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.WARN);

        this.entitiesClass = Arrays.asList(Credential.class);

        this.setupSessionAndTransaction();
    }

    private void setupSessionAndTransaction() {
        this.sessionFactory = this.buildSessionFactory();
        this.openSession();
    }

    public Session openSession() {
        this.session = this.sessionFactory.openSession();
        ManagedSessionContext.bind(this.session);
        return this.session;
    }

    /**
     * Setups hibernate and builds database connection and
     * {@link SessionFactory}.
     *
     * @return A {@link SessionFactory} used to connect to the in memory
     *         database.
     */
    private SessionFactory buildSessionFactory() {
        final PoolProperties poolConfig = new PoolProperties();
        poolConfig.setDriverClassName("org.hsqldb.jdbcDriver");
        poolConfig.setUrl("jdbc:hsqldb:mem:test");
        poolConfig.setUsername("sa");
        poolConfig.setPassword("");
        final ManagedPooledDataSource dataSource = new ManagedPooledDataSource(poolConfig,
                new MetricRegistry());

        final Properties properties = new Properties();
        properties.setProperty("hibernate.connection.autoReconnect", "true");

        final Configuration configuration = new Configuration();
        configuration.setProperty(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed");
        configuration.setProperty(AvailableSettings.GENERATE_STATISTICS, "false");
        configuration.setProperty(AvailableSettings.SHOW_SQL, "true");
        configuration.setProperty("jadira.usertype.autoRegisterUserTypes", "true");
        configuration.setProperty(AvailableSettings.DIALECT, "org.hibernate.dialect.HSQLDialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "create-drop");

        // Registering the entity classes.
        for (final Class<?> entityClass : this.entitiesClass) {
            configuration.addAnnotatedClass(entityClass);
        }

        final DatasourceConnectionProviderImpl connectionProvider = new DatasourceConnectionProviderImpl();
        connectionProvider.setDataSource(dataSource);
        connectionProvider.configure(properties);

        final ServiceRegistry registry = new StandardServiceRegistryBuilder()
                .addService(ConnectionProvider.class, connectionProvider)
                .applySettings(properties)
                .build();

        return configuration.buildSessionFactory(registry);
    }

    public void closeSessionAndDropSchema() {
        if (this.session.isOpen()) {
            this.session.createSQLQuery("TRUNCATE SCHEMA PUBLIC AND COMMIT").executeUpdate();
            this.session.close();
        }
        this.sessionFactory.close();
    }
}