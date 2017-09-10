package com.github.mtakaki.credentialstorage.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.mtakaki.credentialstorage.client.model.Credential;
import com.github.mtakaki.credentialstorage.client.util.DecryptionUtil;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.setup.Environment;

import jodd.util.Base64;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Provides an abstraction layer over the credential storage service API. It
 * will auto-deserialize and decrypt the incoming data using the client's
 * private key and it will auto-serialize the incoming {@link Credential}
 * objects.
 *
 * @author mtakaki
 *
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CredentialStorageServiceClient {
    private static final String AUTHENTICATION_HEADER = "X-Auth-RSA";
    private static final String CLIENT_NAME = "credential-client";
    private static final String CREDENTIAL_URL_PATH = "/credential";

    private final DecryptionUtil decryptionUtil;
    private final WebTarget serverTarget;
    private final String publicKey;

    public CredentialStorageServiceClient(final File privateKey, final File publicKey,
            final String serverURL, final JerseyClientConfiguration clientConfiguration)
                    throws NoSuchAlgorithmException, InvalidKeySpecException,
                    FileNotFoundException, IOException {
        this(new DecryptionUtil(privateKey),
                createJerseyClient(clientConfiguration).target(serverURL).path(CREDENTIAL_URL_PATH),
                readFileContentAsBase64(publicKey));
    }

    public CredentialStorageServiceClient(final File privateKey, final File publicKey,
            final String serverURL, final JerseyClientConfiguration clientConfiguration,
            final Environment environment) throws NoSuchAlgorithmException, InvalidKeySpecException,
                    IOException, URISyntaxException {
        this(new DecryptionUtil(privateKey),
                createJerseyClient(clientConfiguration, environment).target(serverURL)
                        .path(CREDENTIAL_URL_PATH),
                readFileContentAsBase64(publicKey));
    }

    private static Client createJerseyClient(final JerseyClientConfiguration clientConfiguration) {
        return new JerseyClientBuilder(new MetricRegistry())
                // TODO This should not be hard-coded.
                .using(new ThreadPoolExecutor(5, 10, 1L, TimeUnit.MINUTES,
                        new LinkedBlockingQueue<>()))
                .using(clientConfiguration)
                .using(new ObjectMapper().setPropertyNamingStrategy(
                        PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES))
                .build(CLIENT_NAME);
    }

    private static Client createJerseyClient(final JerseyClientConfiguration clientConfiguration,
            final Environment environment) {
        return new JerseyClientBuilder(environment).using(clientConfiguration)
                .build(CLIENT_NAME);
    }

    private static String readFileContentAsBase64(final File publicKey)
            throws IOException, FileNotFoundException {
        return Base64.encodeToString(IOUtils.toByteArray(new FileInputStream(publicKey)));
    }

    /**
     * Initiates the creation of a request by adding the authentication header
     * and content media type to the request.
     *
     * @return A client builder.
     */
    private Builder getClient() {
        return this.serverTarget.request(MediaType.APPLICATION_JSON)
                .header(AUTHENTICATION_HEADER, this.publicKey);
    }

    /**
     * Retrieves the credentials from the remote server. It will automatically
     * decrypt the contents of the response and return it in plain text.
     *
     * @return The decrypted {@link Credential} object.
     * @throws InvalidKeyException
     *             Thrown if the symmetric key has an invalid format.
     * @throws NoSuchAlgorithmException
     *             Thrown if the AES algorithm is not available.
     * @throws NoSuchPaddingException
     *             Thrown if the padding is not available.
     * @throws IllegalBlockSizeException
     *             Thrown if the size of the encrypted text is too long.
     * @throws BadPaddingException
     *             Thrown if there's a bad padding in the payload.
     */
    public Credential getCredential()
            throws InvalidKeyException, NoSuchAlgorithmException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        final Credential credential = this.getClient().get(Credential.class);

        final SecretKey secretKey = this.decryptionUtil
                .decryptSecretKey(credential.getSymmetricKey());
        credential.setPrimary(this.decryptionUtil.decryptText(secretKey, credential.getPrimary()));
        credential.setSecondary(
                this.decryptionUtil.decryptText(secretKey, credential.getSecondary()));

        return credential;
    }

    /**
     * Uploads the given credential to the server. If the credential already
     * exists, it will be completely overwritten with the new one.
     *
     * @param credential
     *            The credential object that will be uploaded.
     * @return {@code true} if the credential was successfully uploaded and
     *         {@code false} if otherwise.
     */
    public boolean uploadNewCredential(final Credential credential) {
        return this.executeRequest(builder -> {
            return builder.post(Entity.json(credential));
        } , response -> {
            return response.getStatus() == Status.CREATED.getStatusCode();
        });
    }

    /**
     * Updates existing credentials stored under the public key. If it doesn't
     * exist it will throw a {@link NotFoundException}.
     *
     * @param credential
     *            The new credential that will be stored.
     * @return {@code true} if the credential was successfully updated and
     *         {@code false} if otherwise.
     */
    public boolean updateCredential(final Credential credential) {
        return this.executeRequest(builder -> {
            return builder.put(Entity.json(credential));
        } , response -> {
            return response.getStatus() == Status.OK.getStatusCode();
        });
    }

    /**
     * Deletes existing credentials from the server. If it doesn't exist it will
     * throw a {@link NotFoundException}.
     *
     * @return {@code true} if the credential was successfully deleted and
     *         {@code false} if otherwise.
     */
    public boolean deleteCredential() {
        return this.executeRequest(builder -> {
            return builder.delete();
        } , response -> {
            return response.getStatus() == Status.OK.getStatusCode();
        });
    }

    /**
     * Executes the request and automatically closes the response object. It
     * delegates to the given clientHttpVerb function to finalize the request
     * and responseStatusCheck to validate the response status.
     *
     * @param clientHttpVerb
     *            Lambda function that will do the final request step.
     * @param responseStatusCheck
     *            Lambda function that will validate the response.
     * @return {@code true} if the request was successful and {@code false} if
     *         otherwise.
     */
    private boolean executeRequest(final Function<Builder, Response> clientHttpVerb,
            final Function<Response, Boolean> responseStatusCheck) {
        final Response response = clientHttpVerb.apply(this.getClient());
        try {
            return responseStatusCheck.apply(response);
        } finally {
            response.close();
        }
    }
}