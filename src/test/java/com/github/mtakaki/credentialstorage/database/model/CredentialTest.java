package com.github.mtakaki.credentialstorage.database.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.DateTime;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import io.dropwizard.jackson.Jackson;
import io.dropwizard.testing.FixtureHelpers;

public class CredentialTest {
    private static final ObjectMapper MAPPER = Jackson.newObjectMapper().setPropertyNamingStrategy(
            PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);

    @Test
    public void deserializesFromJSON() throws Exception {
        final Credential credential = Credential.builder()
                .key("abc")
                .symmetricKey("sym")
                .primary("user")
                .secondary("password")
                .updatedAt(DateTime.parse("2017-02-25T20:10:00").toDate())
                .lastAccess(DateTime.parse("2017-02-25T20:10:00").toDate())
                .createdAt(DateTime.parse("2017-02-23T10:15:20").toDate())
                .build();
        assertThat(
                MAPPER.readValue(FixtureHelpers.fixture("fixtures/credential.json"),
                        Credential.class))
                                .isEqualToIgnoringGivenFields(credential, "key");
    }
}