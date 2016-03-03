package com.github.mtakaki.credentialstorage.database.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.mtakaki.credentialstorage.database.model.Credential;

import io.dropwizard.jackson.Jackson;
import io.dropwizard.testing.FixtureHelpers;

public class CredentialTest {
    private static final ObjectMapper MAPPER = Jackson.newObjectMapper().setPropertyNamingStrategy(
            PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);

    @Test
    public void deserializesFromJSON() throws Exception {
        final Credential credential = Credential.builder()
                .id(10)
                .key("abc")
                .symmetricKey("sym")
                .primary("user")
                .secondary("password").build();
        assertThat(
                MAPPER.readValue(FixtureHelpers.fixture("fixtures/credential.json"),
                        Credential.class))
                                .isEqualToComparingOnlyGivenFields(credential, "symmetricKey",
                                        "primary", "secondary");
    }
}