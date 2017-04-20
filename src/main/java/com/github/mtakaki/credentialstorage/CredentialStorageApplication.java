package com.github.mtakaki.credentialstorage;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.mtakaki.credentialstorage.configuration.RedisConfiguration;
import com.github.mtakaki.credentialstorage.healthchecks.RedisHealthCheck;
import com.github.mtakaki.credentialstorage.managed.JedisManaged;
import com.github.mtakaki.credentialstorage.resources.CredentialResource;
import com.github.mtakaki.credentialstorage.resources.admin.AuditResource;
import com.github.mtakaki.dropwizard.admin.AdminResourceBundle;
import com.github.mtakaki.dropwizard.circuitbreaker.jersey.CircuitBreakerBundle;
import com.github.mtakaki.dropwizard.circuitbreaker.jersey.CircuitBreakerConfiguration;
import com.github.mtakaki.dropwizard.petite.PetiteBundle;
import com.github.mtakaki.dropwizard.petite.PetiteConfiguration;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import jodd.petite.PetiteContainer;
import redis.clients.jedis.JedisPool;

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

    private final AdminResourceBundle adminResourceBundle = new AdminResourceBundle();

    @Override
    public String getName() {
        return "credential-storage-service";
    };

    @Override
    public void initialize(final Bootstrap<CredentialStorageConfiguration> bootstrap) {
        bootstrap.addBundle(this.circuitBreakerBundle);
        bootstrap.addBundle(this.petite);
        bootstrap.addBundle(this.adminResourceBundle);
        bootstrap.addBundle(new SwaggerBundle<CredentialStorageConfiguration>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(
                    final CredentialStorageConfiguration configuration) {
                return configuration.getSwaggerBundleConfiguration();
            }
        });
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)));
    }

    @Override
    public void run(final CredentialStorageConfiguration configuration,
            final Environment environment) throws Exception {
        final JedisManaged jedisManaged = this.buildJedis(configuration.getRedis());
        environment.lifecycle().manage(jedisManaged);
        final PetiteContainer petiteContainer = this.petite.getPetiteContainer();
        this.registerExternalDependencies(configuration, environment, jedisManaged,
                petiteContainer);

        environment.getObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        environment.jersey().register(petiteContainer.getBean(CredentialResource.class));

        // Admin resources.
        final JerseyEnvironment adminJerseyEnvironment = this.adminResourceBundle
                .getJerseyEnvironment();
        adminJerseyEnvironment.register(petiteContainer.getBean(AuditResource.class));

        // Health checks
        environment.healthChecks().register("redis",
                this.petite.getPetiteContainer().getBean(RedisHealthCheck.class));
    }

    protected JedisManaged buildJedis(final RedisConfiguration configuration) {
        return new JedisManaged(configuration);
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
     * @param jedisManaged
     *            The managed Jedis object, providing access to a redis
     *            instance.
     * @param petiteContainer
     *            The petite container where the beans will be registered.
     */
    protected void registerExternalDependencies(
            final CredentialStorageConfiguration configuration, final Environment environment,
            final JedisManaged jedisManaged, final PetiteContainer petiteContainer) {
        // The SessionFactory that provides connection to the database.
        petiteContainer.addBean(JedisPool.class.getName(), jedisManaged.getJedisPool());
        // Hooking up our configuration just in case we need to pass it around.
        petiteContainer.addBean(CredentialStorageConfiguration.class.getName(), configuration);
        // Our cache that will be used to reduce the load on the database.
        petiteContainer.addBean(Cache.class.getName(),
                CacheBuilder.from(configuration.getPublicKeysCache()).recordStats().build());
    }
}