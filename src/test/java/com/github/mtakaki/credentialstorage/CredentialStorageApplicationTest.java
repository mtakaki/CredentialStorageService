package com.github.mtakaki.credentialstorage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.mtakaki.credentialstorage.database.model.Credential;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.dropwizard.util.Duration;

import jodd.util.Base64;

public class CredentialStorageApplicationTest {
    private static final byte[] TEST_RSA_PUBLIC_KEY = new byte[] { 48, -126, 2, 34, 48, 13, 6, 9,
            42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 2, 15, 0, 48, -126, 2, 10, 2, -126,
            2, 1, 0, -56, 114, -107, 40, 75, -102, 18, -24, 65, 81, -75, 56, -36, -125, 115, -115,
            115, 73, 10, -88, -12, 41, -121, -89, 106, -99, -34, 33, 112, 61, 117, -44, 90, 41, -83,
            39, 74, -116, 34, -119, -3, 3, -127, 56, -12, 119, -25, -54, 11, 71, 8, -103, 31, 99,
            47, -96, -29, -85, -66, -79, 26, 91, -103, -95, -18, 57, -31, 89, -39, 47, 52, 70, 115,
            31, 22, 103, 44, 68, -100, -37, 106, -17, -21, -17, -98, -44, 3, -36, 63, 113, -107,
            -33, 89, 44, 23, -111, 33, -90, -58, 20, 72, -13, -62, -78, -48, -94, -71, 65, -17, 103,
            24, -115, -45, 115, 20, 81, -14, -18, 79, 75, -62, 127, -61, 121, -102, -86, -128, 57,
            -100, -36, 109, 15, -109, -117, 109, 63, -90, -81, -57, 69, -128, -7, 43, -38, 97, -8,
            22, 106, 98, 56, 120, -120, 55, 63, 62, -39, 75, -125, -93, 107, 15, 16, -48, -117, 102,
            -96, -37, -63, 28, 77, -9, 90, 104, 75, 123, -49, 2, -44, -117, -64, 0, 109, 35, 114,
            -116, 124, -2, -63, -124, -105, 70, 104, -122, -95, -68, -15, 73, 5, 24, -93, 40, -120,
            69, 89, 100, -64, 97, -44, -22, -69, -79, -117, -34, 7, -35, 36, -61, -108, 32, 70,
            -109, 25, 24, 112, -43, 28, -86, -57, -60, 56, -128, -18, 70, -51, -109, 118, -61, 109,
            -54, 114, -43, 89, 14, -62, -38, -33, -95, 113, 85, 12, 107, 72, 17, 93, 9, 17, -23, 31,
            35, 90, 58, -11, -126, -10, -37, 85, 45, -7, -50, -117, 109, 31, -125, 25, 116, -1, -36,
            -126, -17, 92, -12, 62, 97, 118, -35, 95, -66, -28, -1, -116, 93, -72, 3, 112, 72, 108,
            -31, -89, 66, -41, 127, -71, -77, 44, 33, 20, -66, 10, 57, 17, 66, 38, -50, -59, -59,
            10, -119, 118, 27, -118, -49, -57, 64, -2, 37, -62, -54, 64, -36, 17, -54, 76, -67, 123,
            19, -40, 108, 94, 54, -108, -116, -34, -122, 113, 16, -29, -69, 113, -102, 30, -92, -57,
            22, -42, 106, 61, -26, -55, -47, -78, 117, 78, 66, 26, 74, 52, -109, -50, 45, 64, 58,
            22, -105, -32, 8, -64, -34, 102, 66, 122, -29, 39, 15, 19, -66, 68, 92, 36, 36, 64, -29,
            49, 115, -79, 105, 76, 118, 64, -65, -110, 94, 103, -72, 90, -6, 52, 77, 48, 37, -73,
            19, -15, -31, -13, 105, 29, -20, 27, 121, -34, 58, -59, 21, 12, 88, -9, 76, -28, -57,
            88, -62, 89, 23, -47, -71, 27, -16, 57, 119, -69, 118, -83, -5, -42, 35, -119, 31, 9,
            121, 46, 87, -104, 20, -51, -92, -53, 95, 82, -69, -32, 39, -98, 37, 94, 27, -58, -94,
            -105, 56, -124, 99, 25, -69, 102, 34, 91, 90, -38, -22, 73, 91, -22, 50, 43, -32, -50,
            114, -33, -104, -109, 79, -128, 92, -16, 80, -21, -8, 26, 50, 104, 109, 93, 98, 59, 126,
            109, -86, -75, -41, 100, -32, -57, 2, 3, 1, 0, 1 };
    private static final String BASE_64_PUBLIC_KEY = Base64.encodeToString(TEST_RSA_PUBLIC_KEY);

    private static final byte[] TEST_RSA_PUBLIC_KEY_2 = new byte[] { 48, -126, 2, 34, 48, 13, 6, 9,
            42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 2, 15, 0, 48, -126, 2, 10, 2, -126,
            2, 1, 0, -68, -64, -41, 126, 39, 52, 121, 78, -29, -127, -19, 122, 106, 54, -127, -38,
            -18, 123, -49, 12, -43, -6, 26, -101, -42, 60, -63, 11, -23, 9, 54, -72, -48, -116,
            -100, 13, -112, -49, -101, -110, -42, 0, -20, -3, 16, -68, -2, 102, -107, -77, 110, 73,
            -97, 94, -117, -70, -110, 67, -99, -23, 43, 24, -61, 115, 76, 4, -3, 70, -127, 42, 23,
            93, -43, 108, -98, 98, 85, 12, -106, -107, 11, -40, -55, -50, 113, 64, 58, 115, -75,
            -71, -53, -87, -75, 54, -16, 86, -10, -55, -91, 34, -60, -101, -93, -128, 20, 36, -27,
            82, 126, -109, -66, -16, -92, -44, 50, 53, 73, 65, 69, 87, 53, -73, 40, -123, -105, 89,
            75, 67, -96, 109, 10, 58, 123, 76, 10, -30, -103, 60, -109, -17, -45, 93, 23, 38, 69,
            -31, 11, 48, -34, 86, -85, 60, -52, -39, -7, -38, -105, 82, -14, 55, -51, -11, 23, 112,
            41, -12, -113, -90, -63, 73, -54, -126, 105, -123, -40, 11, -10, -31, -57, -1, -94, -2,
            -95, -88, 100, 19, 25, -2, 49, -3, -45, -70, 76, -81, 58, -34, -124, 83, 113, -20, 10,
            -14, -116, -86, -61, -48, 96, -77, 106, -59, 122, 123, -101, -5, 85, -92, -3, -125, 7,
            -92, -39, 52, -73, 11, 77, 114, 0, -2, -1, 11, -16, 0, -22, 82, -69, 57, 119, 70, 59,
            112, 38, -125, -12, -84, 9, -61, -5, 83, 92, -122, 33, 82, 72, 68, -54, 28, -44, 72, 11,
            66, -57, -5, 116, -32, -67, -20, 2, 122, -90, -106, 14, -39, -43, -50, 102, -118, -94,
            30, -111, -39, 44, 104, -37, -72, -61, -74, 68, -29, -110, 91, 50, 34, -99, 84, 76, 73,
            -43, 68, -61, 121, -1, 65, -67, 108, -15, -50, -53, -97, 24, 124, 70, -5, -87, 108, -54,
            -33, 59, -1, 10, 79, -34, -56, 2, 24, 90, 4, 94, -104, 15, -99, 107, -111, 35, 79, -27,
            30, -34, -72, 70, -56, 70, 110, -47, -9, -39, 115, 87, 34, -26, -48, -100, -44, -127,
            108, 38, 102, 27, -74, -58, -70, -48, -89, -122, -56, 95, -48, 124, 125, 19, -77, 48,
            121, -19, -3, 84, 67, 118, 92, 112, 125, -16, 35, 25, -54, 17, 51, -114, 5, -53, -2, 32,
            -107, 46, 2, 16, 108, 38, -29, -61, 45, -10, -86, 69, -93, 26, -93, -116, -13, 73, 29,
            126, 73, 37, 53, 26, 66, 13, -106, 0, 85, 35, 9, 65, -27, -127, 53, -78, -96, 120, 48,
            91, -109, -9, 6, -38, -23, -20, -119, 104, 27, 13, -105, 45, 88, -126, 72, 50, 61, -105,
            -52, -22, -6, -2, -119, -18, -113, -13, 69, 117, -54, 29, -81, -101, 70, 28, -128, 16,
            92, 24, -33, -38, -9, -54, -103, 34, 94, -81, 71, -111, -20, -12, -63, 72, -65, 85, 80,
            65, 120, -99, 7, -52, 100, -3, -6, 117, 16, 64, -108, -69, 64, 38, -7, 68, 64, 112, -82,
            67, 85, 57, -27, -49, 95, 2, 3, 1, 0, 1 };
    private static final String BASE_64_PUBLIC_KEY_2 = Base64.encodeToString(TEST_RSA_PUBLIC_KEY_2);

    @Rule
    public final DropwizardAppRule<CredentialStorageConfiguration> RULE = new DropwizardAppRule<CredentialStorageConfiguration>(
            CredentialStorageApplication.class,
            ResourceHelpers.resourceFilePath("config.yml"));

    private Client client;

    private final Credential credential = Credential.builder()
            .symmetricKey(BASE_64_PUBLIC_KEY)
            .primary("user")
            .secondary("password").build();

    private final Credential credential2 = Credential.builder()
            .symmetricKey(BASE_64_PUBLIC_KEY_2)
            .primary("another").build();

    @Before
    public void createClient() {
        final JerseyClientConfiguration configuration = new JerseyClientConfiguration();
        configuration.setTimeout(Duration.minutes(1L));
        configuration.setConnectionTimeout(Duration.minutes(1L));
        configuration.setConnectionRequestTimeout(Duration.minutes(1L));
        this.client = new JerseyClientBuilder(this.RULE.getEnvironment()).using(configuration)
                .build("test client");

        assertThat(this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .header("X-Auth-RSA", BASE_64_PUBLIC_KEY)
                .post(Entity.json(this.credential)).getStatus())
                        .isEqualTo(Status.CREATED.getStatusCode());
    }

    @Test
    public void testGetCredential() throws JsonParseException, JsonMappingException, IOException {
        final Response response = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .header("X-Auth-RSA", BASE_64_PUBLIC_KEY)
                .get();

        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
        assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);

        final Credential responseCredential = IntegrationTestUtil.extractEntity(response,
                Credential.class);
        assertThat(responseCredential.getSymmetricKey()).hasSize(684);
        assertThat(responseCredential.getPrimary()).hasSize(24);
        assertThat(responseCredential.getSecondary()).hasSize(24);
    }

    @Test
    public void testGetCredentialNotFound()
            throws JsonParseException, JsonMappingException, IOException {
        final Response response = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .header("X-Auth-RSA", "not found")
                .get();

        assertThat(response.getStatus()).isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testGetCredentialMissingHeader()
            throws JsonParseException, JsonMappingException, IOException {
        final Response response = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testPostCredentialWithExistingCredential()
            throws JsonParseException, JsonMappingException, IOException {
        final Response response = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .header("X-Auth-RSA", BASE_64_PUBLIC_KEY)
                .post(Entity.json(this.credential));

        assertThat(response.getStatus()).isEqualTo(Status.CREATED.getStatusCode());
    }

    @Test
    public void testPostNewCredential()
            throws JsonParseException, JsonMappingException, IOException {
        // Verifying that our second credential is not present yet.
        final Response missingGetResponse = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .header("X-Auth-RSA", BASE_64_PUBLIC_KEY_2)
                .get();
        assertThat(missingGetResponse.getStatus()).isEqualTo(Status.NOT_FOUND.getStatusCode());

        // Posting the new credential.
        final Response response = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .header("X-Auth-RSA", BASE_64_PUBLIC_KEY_2)
                .post(Entity.json(this.credential2));
        assertThat(response.getStatus()).isEqualTo(Status.CREATED.getStatusCode());

        // Now verifying that the new credential was properly stored, including
        // missing the secondary credential.
        final Response getResponse = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .header("X-Auth-RSA", BASE_64_PUBLIC_KEY_2)
                .get();

        assertThat(getResponse.getStatus()).isEqualTo(Status.OK.getStatusCode());
        assertThat(getResponse.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);

        final Credential responseCredential = IntegrationTestUtil.extractEntity(getResponse,
                Credential.class);
        assertThat(responseCredential.getSymmetricKey()).hasSize(684);
        assertThat(responseCredential.getPrimary()).hasSize(24);
        assertThat(responseCredential.getSecondary()).isNull();
    }

    @Test
    public void testPostCredentialMissingHeader()
            throws JsonParseException, JsonMappingException, IOException {
        final Response response = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .post(Entity.json(this.credential));

        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testPostCredentialWithInvalidCredential()
            throws JsonParseException, JsonMappingException, IOException {
        final Response response = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .header("X-Auth-RSA", BASE_64_PUBLIC_KEY)
                .post(Entity.json("{\"missing\":\"data\"}"));

        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testPutCredentialWithExistingCredential()
            throws JsonParseException, JsonMappingException, IOException {
        final Response getResponse = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .header("X-Auth-RSA", BASE_64_PUBLIC_KEY)
                .get();

        assertThat(getResponse.getStatus()).isEqualTo(Status.OK.getStatusCode());
        assertThat(getResponse.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);

        final Credential existingCredential = IntegrationTestUtil.extractEntity(getResponse,
                Credential.class);

        final Credential newCredential = Credential.builder()
                .primary("user").build();
        final Response response = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .header("X-Auth-RSA", BASE_64_PUBLIC_KEY)
                .put(Entity.json(newCredential));
        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());

        // Now verifying that the new credential was properly updated.
        final Response updatedGetResponse = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .header("X-Auth-RSA", BASE_64_PUBLIC_KEY)
                .get();

        assertThat(updatedGetResponse.getStatus()).isEqualTo(Status.OK.getStatusCode());
        assertThat(updatedGetResponse.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);

        final Credential updatedCredential = IntegrationTestUtil.extractEntity(updatedGetResponse,
                Credential.class);
        assertThat(updatedCredential).isNotEqualTo(existingCredential);
        assertThat(updatedCredential.getSecondary()).isNull();
    }

    @Test
    public void testPutCredentialMissingHeader()
            throws JsonParseException, JsonMappingException, IOException {
        final Credential newCredential = Credential.builder()
                .primary("user")
                .secondary("another password").build();
        final Response response = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .put(Entity.json(newCredential));
        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testPutCredentialWithNonExistingCredential()
            throws JsonParseException, JsonMappingException, IOException {
        final Credential newCredential = Credential.builder()
                .primary("user")
                .secondary("another password").build();
        final Response response = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .header("X-Auth-RSA", "missing")
                .put(Entity.json(newCredential));
        assertThat(response.getStatus()).isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testPutCredentialWithInvalidPayload()
            throws JsonParseException, JsonMappingException, IOException {
        final Response response = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .put(Entity.json("{\"wrong\": \"stuff\"}"));
        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testDeleteCredentialWithExistingCredential()
            throws JsonParseException, JsonMappingException, IOException {
        final Response response = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .header("X-Auth-RSA", BASE_64_PUBLIC_KEY)
                .delete();
        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());

        // Now verifying that the credential was properly deleted.
        final Response updatedGetResponse = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .header("X-Auth-RSA", BASE_64_PUBLIC_KEY)
                .get();

        assertThat(updatedGetResponse.getStatus()).isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testDeleteCredentialWithMissingHeader()
            throws JsonParseException, JsonMappingException, IOException {
        // Now verifying that the credential was properly deleted.
        final Response response = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .delete();
        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testDeleteCredentialWithNonExistingCredential()
            throws JsonParseException, JsonMappingException, IOException {
        // Now verifying that the credential was properly deleted.
        final Response response = this.client
                .target(String.format("http://localhost:%d/credential", this.RULE.getLocalPort()))
                .request()
                .header("X-Auth-RSA", "not found")
                .delete();
        assertThat(response.getStatus()).isEqualTo(Status.NOT_FOUND.getStatusCode());
    }
}