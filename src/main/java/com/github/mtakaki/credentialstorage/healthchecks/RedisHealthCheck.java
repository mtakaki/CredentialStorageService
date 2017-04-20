package com.github.mtakaki.credentialstorage.healthchecks;

import com.codahale.metrics.health.HealthCheck;

import jodd.petite.meta.PetiteBean;
import lombok.AllArgsConstructor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@AllArgsConstructor
@PetiteBean
public class RedisHealthCheck extends HealthCheck {
    private final JedisPool jedisPool;

    @Override
    protected Result check() throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return Result.healthy();
        }
    }
}