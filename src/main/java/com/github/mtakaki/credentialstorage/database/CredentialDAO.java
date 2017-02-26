package com.github.mtakaki.credentialstorage.database;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mtakaki.credentialstorage.database.model.Credential;
import com.google.common.base.Optional;

import jodd.petite.meta.PetiteBean;
import lombok.AllArgsConstructor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Response;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Transaction;

/**
 * Database Access Object that handles all credential operations.
 *
 * @author mitsuo
 *
 */
@PetiteBean
@AllArgsConstructor
public class CredentialDAO {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS"));
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
            .appendYear(4, 4).appendLiteral('-').appendMonthOfYear(2).appendLiteral('-')
            .appendDayOfMonth(2).appendLiteral('T').appendHourOfDay(2).appendLiteral(':')
            .appendMinuteOfHour(2).appendLiteral(':').appendSecondOfMinute(2).appendLiteral('.')
            .appendMillisOfSecond(3).toFormatter();

    private final JedisPool jedisPool;

    /**
     * Queries for a {@link Credential} stored under the given key.
     *
     * @param key
     *            Key used to store the credentials.
     * @return The credential stored under the given key or
     *         {@code Optional.absent()} if it's missing.
     */
    public Optional<Credential> getCredentialByKey(final String key) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.watch(key);
            final Transaction transaction = jedis.multi();
            final Response<Map<String, String>> credentialJson = transaction.hgetAll(key);
            // We update lastAccess after the get.
            transaction.hset(key, "lastAccess", TIMESTAMP_FORMATTER.print(new DateTime()));
            transaction.exec();
            return this.createAndPopulateBean(credentialJson.get());
        }
    }

    private Optional<Credential> createAndPopulateBean(final Map<String, String> propertyValues) {
        if (propertyValues.isEmpty()) {
            return Optional.absent();
        }

        return Optional.of(MAPPER.convertValue(propertyValues, Credential.class));
    }

    /**
     * Saves or updates the given credential.
     *
     * @param credential
     *            The credential that will be persisted to the database.
     */
    public void save(final Credential credential) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            final Date updatedTimestamp = new Date();
            if (credential.getCreatedAt() == null) {
                credential.setCreatedAt(updatedTimestamp);
            }
            credential.setUpdatedAt(updatedTimestamp);
            credential.setLastAccess(updatedTimestamp);

            final Transaction transaction = jedis.multi();
            final String credentialKey = credential.getKey();
            transaction.del(credentialKey);
            transaction.hmset(credentialKey,
                    MAPPER.convertValue(credential, new TypeReference<Map<String, String>>() {
                    }));
            transaction.exec();
        }
    }

    /**
     * Deletes the credential stored under the given key.
     *
     * @param key
     *            The key that were used to store the credential.
     * @return {@code true} if the credential could be found and could be
     *         delete. {@code false} if otherwise.
     */
    public boolean deleteByKey(final String key) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.del(key) != 0L;
        }
    }

    /**
     * Retrieves all credentials from the database.
     *
     * @return All credentials.
     */
    public List<String> getAllCredentialsKey() {
        try (Jedis jedis = this.jedisPool.getResource()) {
            final List<String> keys = new LinkedList<>();
            ScanResult<String> result = jedis.scan("0");
            do {
                keys.addAll(result.getResult());
                result = jedis.scan(result.getStringCursor());
            } while (!result.getStringCursor().equals("0"));
            return keys;
        }
    }
}