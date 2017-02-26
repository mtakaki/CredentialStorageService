package com.github.mtakaki.credentialstorage.database;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.mtakaki.credentialstorage.database.model.Credential;

import redis.clients.jedis.JedisPool;
import redis.embedded.RedisServer;
import redis.embedded.ports.EphemeralPortProvider;

public class CredentialDAOTest {
    private CredentialDAO dao;
    private RedisServer redisServer;

    @Before
    public void setUp() {
        this.redisServer = RedisServer.builder().port(new EphemeralPortProvider().next()).build();
        this.redisServer.start();
        this.dao = new CredentialDAO(
                new JedisPool(
                        String.format("redis://localhost:%d", this.redisServer.ports().get(0))));
    }

    @After
    public void tearDown() {
        if (this.redisServer.isActive()) {
            this.redisServer.stop();
        }
    }

    @Test
    public void testGetCredentialByKey() {
        final Credential credential = this.createCredentialAndSave();

        assertThat(this.dao.getCredentialByKey(credential.getKey()).get())
                .isEqualToComparingFieldByField(credential);
    }

    @Test
    public void testGetCredentialByKeyNotFound() {
        assertThat(this.dao.getCredentialByKey("missing").isPresent()).isFalse();
    }

    @Test
    public void testSave() {
        final Credential credential = this.createCredentialAndSave();

        assertThat(this.dao.getCredentialByKey(credential.getKey()).get())
                .isEqualToComparingFieldByField(credential);
    }

    @Test
    public void testDeleteByKey() {
        final Credential credential = this.createCredentialAndSave();

        assertThat(this.dao.getCredentialByKey(credential.getKey()).get())
                .isEqualToComparingFieldByField(credential);

        assertThat(this.dao.deleteByKey("a")).isTrue();
    }

    @Test
    public void testDeleteByKeyNotFound() {
        assertThat(this.dao.deleteByKey("a")).isFalse();
    }

    private Credential createCredentialAndSave() {
        final Credential credential = Credential.builder()
                .key("a")
                .primary("me@abc.com")
                .secondary("password")
                .symmetricKey("key")
                .build();
        this.dao.save(credential);
        return credential;
    }
}