package com.github.mtakaki.credentialstorage.configuration;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import redis.clients.jedis.JedisPoolConfig;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RedisConfiguration {
    @NotNull
    private String url;
    private final JedisPoolConfig poolConfig = new JedisPoolConfig();
}