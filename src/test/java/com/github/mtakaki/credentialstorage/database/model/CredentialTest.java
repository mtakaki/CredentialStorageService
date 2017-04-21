package com.github.mtakaki.credentialstorage.database.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import io.dropwizard.jackson.Jackson;
import io.dropwizard.testing.FixtureHelpers;

public class CredentialTest {
    private static final ObjectMapper MAPPER = Jackson.newObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

    @Before
    public void setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DateTimeZone.setDefault(DateTimeZone.UTC);
    }

    @Test
    public void deserializesFromJSON() throws Exception {
        final Credential credential = Credential.builder()
                .key("abc")
                .symmetricKey("sym")
                .primary("user")
                .secondary("password")
                .updatedAt(DateTime.parse("2017-02-26T04:10:00").toDate())
                .lastAccess(DateTime.parse("2017-02-26T04:10:00").toDate())
                .createdAt(DateTime.parse("2017-02-23T18:15:20").toDate())
                .build();
        assertThat(
                MAPPER.readValue(FixtureHelpers.fixture("fixtures/credential.json"),
                        Credential.class))
                                .isEqualToIgnoringGivenFields(credential, "key");
    }
}