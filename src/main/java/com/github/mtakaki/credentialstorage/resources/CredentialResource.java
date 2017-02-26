package com.github.mtakaki.credentialstorage.resources;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

import javax.crypto.SecretKey;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.hibernate.validator.constraints.NotEmpty;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.mtakaki.credentialstorage.CredentialStorageConfiguration;
import com.github.mtakaki.credentialstorage.database.CredentialDAO;
import com.github.mtakaki.credentialstorage.database.model.Credential;
import com.github.mtakaki.credentialstorage.database.model.view.UserView;
import com.github.mtakaki.credentialstorage.encryption.EncryptionUtil;
import com.github.mtakaki.credentialstorage.encryption.InitializationException;
import com.github.mtakaki.dropwizard.circuitbreaker.jersey.CircuitBreaker;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import jodd.petite.meta.PetiteBean;
import lombok.AllArgsConstructor;

/**
 * Resource that handles the basic credential CRUD operations.
 *
 * @author mtakaki
 *
 */
@Path("/credential")
@Api("/credential")
@Produces(MediaType.APPLICATION_JSON)
@AllArgsConstructor
@PetiteBean
public class CredentialResource {
    private static final String CREDENTIAL_PATH = "/credential/";
    private static final String PUBLIC_KEY_HEADER = "X-Auth-RSA";

    private final CredentialDAO credentialDAO;
    private final Cache<String, EncryptionUtil> encryptionUtil;
    private final CredentialStorageConfiguration configuration;

    @GET
    @ApiOperation(
        value = "Retrieves the credential pair for the given public key",
        notes = "Returns a symetrical key, encrypted using the given assymetrical public key. "
                + "The symetrical key should be used to decrypt the credential pair.")
    @Timed
    @CircuitBreaker
    @JsonView(UserView.class)
    public Optional<Credential> getByKey(
            @HeaderParam(PUBLIC_KEY_HEADER) @NotEmpty final String userPublicKey) {
        return this.credentialDAO.getCredentialByKey(userPublicKey);
    }

    @POST
    @ApiOperation(
        value = "Stores the given credential pair into the database.",
        notes = "The credential pair is encrypted using a symmetric algorithm. "
                + "The symmetrical key is encrypted using the public assymetrical key and stored in the database. "
                + "If the credential already exists in the database, it will be completely overwritten with the new one.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    @CircuitBreaker
    public Response storeCredential(
            @HeaderParam(PUBLIC_KEY_HEADER) @NotEmpty final String userPublicKey,
            @Valid final Credential credential)
            throws ExecutionException, NoSuchAlgorithmException, InitializationException {
        // TODO Create token and client signs it with the private key. The
        // server must verify the signature is valid, using client's public key.

        final Optional<Credential> savedCredentialOptional = this.credentialDAO
                .getCredentialByKey(userPublicKey);
        this.fillUpEncryptAndSaveCredential(userPublicKey, savedCredentialOptional.or(credential),
                credential);

        return Response.created(URI.create(CREDENTIAL_PATH + userPublicKey)).build();
    }

    @PUT
    @ApiOperation(
        value = "Updates the credential pair stored under the given public asymmetrical key.",
        notes = "The credential pair is re-encrypted with a symetric algorithm and its new key is stored and encrypted using the given assymetrical public key.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    @CircuitBreaker
    public Response updateCredential(
            @HeaderParam(PUBLIC_KEY_HEADER) @NotEmpty final String userPublicKey,
            @Valid final Credential credential)
            throws ExecutionException, InitializationException, NoSuchAlgorithmException {
        // As this is an update, we need to query and verify the credentials
        // exist in the database.
        final Optional<Credential> savedCredentialOptional = this.credentialDAO
                .getCredentialByKey(userPublicKey);

        final Credential savedCredential = savedCredentialOptional.get();
        savedCredential.setPrimary(credential.getPrimary());
        savedCredential.setSecondary(credential.getSecondary());

        this.fillUpEncryptAndSaveCredential(userPublicKey, savedCredential, credential);

        return Response.ok().build();
    }

    /**
     * Will encrypt the given credential using the incoming credential data and
     * will save it to the database. It will generate new symmetric keys every
     * time this method is called and it will be used to encrypt the
     * credentials.
     *
     * @param userPublicKey
     *            The incoming public key used to encrypt the symmetric key.
     * @param credential
     *            The credential object that will be updated and saved to the
     *            database.
     * @param incomingCredential
     *            The incoming credential payload. It will be used as the source
     *            of data.
     * @throws NoSuchAlgorithmException
     *             Thrown if either AES or RSA algorithms are not available.
     * @throws InitializationException
     *             Thrown if the padding algorithm is not available, or if the
     *             incoming symmetric key is invalid. We don't have any way of
     *             validating it beforehand, or if the data is too long to be
     *             encrypted, or if the padding data is incorrect.
     * @throws ExecutionException
     *             Thrown if we fail to create the {@link EncryptionUtil} from
     *             within the cache.
     */
    private void fillUpEncryptAndSaveCredential(final String userPublicKey,
            final Credential credential, final Credential incomingCredential)
            throws InitializationException, ExecutionException, NoSuchAlgorithmException {
        final EncryptionUtil cachedEncryptionUtil = this.getEncryptionUtilFromCache(userPublicKey);
        final SecretKey symetricKey = cachedEncryptionUtil.generateSymmetricKey();

        // Setting the values from incomingCredential into credential to ensure
        // we update the entry, when a field is removed.
        credential.setPrimary(incomingCredential.getPrimary());
        credential.setSecondary(incomingCredential.getSecondary());

        // The asymmetric key is stored as it is. At this point there is no
        // security threat to store it like this.
        credential.setKey(userPublicKey);
        // The symmetric key is stored encrypted using the asymmetric public
        // key. This can only be decrypted using the private keys, so not even
        // us can decrypt it later.
        credential.setSymmetricKey(cachedEncryptionUtil.encrypt(symetricKey));
        cachedEncryptionUtil.encrypt(symetricKey, incomingCredential.getPrimary())
                .ifPresent(encryptedPrimary -> credential.setPrimary(encryptedPrimary));
        cachedEncryptionUtil.encrypt(symetricKey, incomingCredential.getSecondary())
                .ifPresent(encryptedSecondary -> credential.setSecondary(encryptedSecondary));

        this.credentialDAO.save(credential);
    }

    /**
     * Retrieves an {@link EncryptionUtil} from the cache or creates a new one
     * if it cannot be found.
     *
     * @param userPublicKey
     *            The incoming user's public key.
     * @return An {@link EncryptionUtil} ready to be used for encryption.
     * @throws ExecutionException
     *             Thrown if we can't create a new {@link EncryptionUtil}.
     */
    private EncryptionUtil getEncryptionUtilFromCache(final String userPublicKey)
            throws ExecutionException {
        return this.encryptionUtil.get(userPublicKey, () -> new EncryptionUtil(userPublicKey,
                CredentialResource.this.configuration.getSymmetricKeySize()));
    }

    @DELETE
    @ApiOperation("Deletes a credential pair from the database.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    @CircuitBreaker
    public Response deleteCredential(
            @HeaderParam(PUBLIC_KEY_HEADER) @NotEmpty final String userPublicKey) {
        if (this.credentialDAO.deleteByKey(userPublicKey)) {
            return Response.ok().build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }
}