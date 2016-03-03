package com.github.mtakaki.credentialstorage;

import org.hibernate.SessionFactory;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.mtakaki.credentialstorage.database.model.Credential;
import com.github.mtakaki.credentialstorage.resources.CredentialResource;
import com.github.mtakaki.dropwizard.circuitbreaker.jersey.CircuitBreakerBundle;
import com.github.mtakaki.dropwizard.circuitbreaker.jersey.CircuitBreakerConfiguration;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.dropwizard.Application;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import jodd.petite.PetiteContainer;
import jodd.petite.config.AutomagicPetiteConfigurator;

/**
 * Credential storage application. This is our main application class.
 *
 * @author mtakaki
 *
 */
public class CredentialStorageApplication extends Application<CredentialStorageConfiguration> {
    public static void main(final String[] args) throws Exception {
        new CredentialStorageApplication().run(args);
    }

    private PetiteContainer petite;

    private final HibernateBundle<CredentialStorageConfiguration> hibernate = new HibernateBundle<CredentialStorageConfiguration>(
            Credential.class) {
        @Override
        public DataSourceFactory getDataSourceFactory(
                final CredentialStorageConfiguration configuration) {
            return configuration.getDatabase();
        }
    };

    private final CircuitBreakerBundle<CredentialStorageConfiguration> circuitBreakerBundle = new CircuitBreakerBundle<CredentialStorageConfiguration>() {
        @Override
        protected CircuitBreakerConfiguration getConfiguration(
                final CredentialStorageConfiguration configuration) {
            return configuration.getCircuitBreaker();
        }
    };

    @Override
    public String getName() {
        return "credential-storage-service";
    };

    @Override
    public void initialize(final Bootstrap<CredentialStorageConfiguration> bootstrap) {
        // Setting up the dependency injection and enabling the automatic
        // configuration.
        this.petite = new PetiteContainer();
        // Enables to use Class full names when referencing them for injection.
        // This will prevent us of having conflicts when generic class name
        // exists.
        this.petite.getConfig().setUseFullTypeNames(true);
        // This enables automatic registration of PetiteBeans.
        final AutomagicPetiteConfigurator petiteConfigurator = new AutomagicPetiteConfigurator();
        petiteConfigurator.configure(this.petite);

        bootstrap.addBundle(this.hibernate);
        bootstrap.addBundle(this.circuitBreakerBundle);
        bootstrap.addBundle(new SwaggerBundle<CredentialStorageConfiguration>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(
                    final CredentialStorageConfiguration configuration) {
                return configuration.getSwaggerBundleConfiguration();
            }
        });
    }

    @Override
    public void run(final CredentialStorageConfiguration configuration,
            final Environment environment) throws Exception {
        this.registerExternalDependencies(configuration, environment);

        environment.getObjectMapper().setPropertyNamingStrategy(
                PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
                .setSerializationInclusion(Include.NON_NULL);
        environment.jersey().register(this.petite.getBean(CredentialResource.class));
    }

    /**
     * Creates all the dependencies that does not belong to our project or that
     * we need a specific instance. All the beans that we have no control need
     * to be specified here or when there are multiple instances of the same
     * class and we need to differentiate it by name.
     *
     * @param configuration
     *            The application configuration object.
     * @param environment
     *            The application environment.
     */
    protected void registerExternalDependencies(
            final CredentialStorageConfiguration configuration, final Environment environment) {
        this.petite.addSelf();
        // The SessionFactory that provides connection to the database.
        this.petite.addBean(SessionFactory.class.getName(), this.hibernate.getSessionFactory());
        // Hooking up our configuration just in case we need to pass it around.
        this.petite.addBean(CredentialStorageConfiguration.class.getName(), configuration);
        // Registering our metric registry.
        this.petite.addBean(MetricRegistry.class.getName(), environment.metrics());
        // Our cache that will be used to reduce the load on the database.
        this.petite.addBean(Cache.class.getName(),
                CacheBuilder.from(configuration.getPublicKeysCache()).recordStats().build());
    }
}