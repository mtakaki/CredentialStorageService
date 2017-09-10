package com.github.mtakaki.credentialstorage;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.mtakaki.credentialstorage.configuration.RedisConfiguration;
import com.github.mtakaki.dropwizard.circuitbreaker.jersey.CircuitBreakerConfiguration;
import com.github.mtakaki.dropwizard.petite.PetiteConfiguration;
import com.google.common.cache.CacheBuilderSpec;

import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import jodd.petite.meta.PetiteBean;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@PetiteBean
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredentialStorageConfiguration extends Configuration {
    @NotNull
    @Valid
    private CircuitBreakerConfiguration circuitBreaker;

    @NotNull
    private CacheBuilderSpec publicKeysCache;

    @JsonProperty("swagger")
    private SwaggerBundleConfiguration swaggerBundleConfiguration;

    private int symmetricKeySize;

    @NotNull
    @Valid
    private final PetiteConfiguration petite = new PetiteConfiguration();

    @NotNull
    @Valid
    private RedisConfiguration redis;
}