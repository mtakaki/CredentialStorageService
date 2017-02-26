package com.github.mtakaki.credentialstorage.managed;

import java.net.URI;

import com.github.mtakaki.credentialstorage.configuration.RedisConfiguration;

import io.dropwizard.lifecycle.Managed;

import lombok.Getter;
import redis.clients.jedis.JedisPool;

public class JedisManaged implements Managed {
    @Getter
    private final JedisPool jedisPool;

    public JedisManaged(final RedisConfiguration configuration) {
        this.jedisPool = new JedisPool(configuration.getPoolConfig(),
                URI.create(configuration.getUrl()));
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
        this.jedisPool.close();
    }
}