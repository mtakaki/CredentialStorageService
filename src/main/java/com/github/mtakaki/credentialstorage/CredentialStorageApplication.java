package com.github.mtakaki.credentialstorage;

import org.hibernate.SessionFactory;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.mtakaki.credentialstorage.database.model.Credential;
import com.github.mtakaki.credentialstorage.resources.CredentialResource;
import com.github.mtakaki.dropwizard.circuitbreaker.jersey.CircuitBreakerBundle;
import com.github.mtakaki.dropwizard.circuitbreaker.jersey.CircuitBreakerConfiguration;
import com.github.mtakaki.dropwizard.petite.PetiteBundle;
import com.github.mtakaki.dropwizard.petite.PetiteConfiguration;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.dropwizard.Application;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import jodd.petite.PetiteContainer;

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

    private final PetiteBundle<CredentialStorageConfiguration> petite = new PetiteBundle<CredentialStorageConfiguration>() {
        @Override
        protected PetiteConfiguration getConfiguration(
                final CredentialStorageConfiguration configuration) {
            return configuration.getPetite();
        }
    };

    @Override
    public String getName() {
        return "credential-storage-service";
    };

    @Override
    public void initialize(final Bootstrap<CredentialStorageConfiguration> bootstrap) {
        bootstrap.addBundle(this.hibernate);
        bootstrap.addBundle(this.circuitBreakerBundle);
        bootstrap.addBundle(this.petite);
        bootstrap.addBundle(new SwaggerBundle<CredentialStorageConfiguration>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(
                    final CredentialStorageConfiguration configuration) {
                return configuration.getSwaggerBundleConfiguration();
            }
        });
        bootstrap.addBundle(new TemplateConfigBundle());
    }

    @Override
    public void run(final CredentialStorageConfiguration configuration,
            final Environment environment) throws Exception {
        final PetiteContainer petiteContainer = this.petite.getPetiteContainer();
        this.registerExternalDependencies(configuration, environment, petiteContainer);

        environment.getObjectMapper().setPropertyNamingStrategy(
                PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        environment.jersey().register(petiteContainer.getBean(CredentialResource.class));
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
     * @param petiteContainer
     *            The petite container where the beans will be registered.
     */
    protected void registerExternalDependencies(
            final CredentialStorageConfiguration configuration, final Environment environment,
            final PetiteContainer petiteContainer) {
        // The SessionFactory that provides connection to the database.
        petiteContainer.addBean(SessionFactory.class.getName(), this.hibernate.getSessionFactory());
        // Hooking up our configuration just in case we need to pass it around.
        petiteContainer.addBean(CredentialStorageConfiguration.class.getName(), configuration);
        // Our cache that will be used to reduce the load on the database.
        petiteContainer.addBean(Cache.class.getName(),
                CacheBuilder.from(configuration.getPublicKeysCache()).recordStats().build());
    }
}