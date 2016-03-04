package com.github.mtakaki.credentialstorage.resources;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
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

import org.apache.commons.lang3.StringUtils;

import com.codahale.metrics.annotation.Timed;
import com.github.mtakaki.credentialstorage.CredentialStorageConfiguration;
import com.github.mtakaki.credentialstorage.database.CredentialDAO;
import com.github.mtakaki.credentialstorage.database.model.Credential;
import com.github.mtakaki.credentialstorage.encryption.EncryptionUtil;
import com.github.mtakaki.dropwizard.circuitbreaker.jersey.CircuitBreaker;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;

import io.dropwizard.hibernate.UnitOfWork;
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
    @UnitOfWork
    public Optional<Credential> getByKey(@HeaderParam("X-Auth-RSA") final String userPublicKey) {
        if (StringUtils.isBlank(userPublicKey)) {
            return Optional.absent();
        }

        return this.credentialDAO.getCredentialByKey(userPublicKey);
    }

    @POST
    @ApiOperation(
        value = "Stores the given credential pair into the database.",
        notes = "The credential pair is encrypted using a symmetric algorithm. "
                + "The symmetrical key is encrypted using the public assymetrical key and stored in the database.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    @CircuitBreaker
    @UnitOfWork
    public Response storeCredential(@HeaderParam("X-Auth-RSA") final String userPublicKey,
            @Valid final Credential credential) throws ExecutionException, InvalidKeyException,
                    NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException,
                    BadPaddingException, UnsupportedEncodingException {
        if (StringUtils.isBlank(userPublicKey)) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        final Optional<Credential> savedCredentialOptional = this.credentialDAO
                .getCredentialByKey(userPublicKey);
        this.fillUpEncryptAndSaveCredential(userPublicKey,
                savedCredentialOptional.isPresent() ? savedCredentialOptional.get() : credential,
                credential);

        return Response.created(URI.create("/credential/" + userPublicKey)).build();
    }

    @PUT
    @ApiOperation(
        value = "Updates the credential pair stored under the given public asymmetrical key.",
        notes = "The credential pair is re-encrypted with a symetric algorithm and its key is stored and encrypted using the given assymetrical public key.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    @CircuitBreaker
    @UnitOfWork
    public Response updateCredential(@HeaderParam("X-Auth-RSA") final String userPublicKey,
            @Valid final Credential credential) throws ExecutionException, InvalidKeyException,
                    NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException,
                    BadPaddingException, UnsupportedEncodingException {
        if (StringUtils.isBlank(userPublicKey)) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        // As this is an update, we need to query and verify the credentials
        // exist in the database.
        final Optional<Credential> savedCredentialOptional = this.credentialDAO
                .getCredentialByKey(userPublicKey);
        if (!savedCredentialOptional.isPresent()) {
            return Response.status(Status.NOT_FOUND).build();
        }

        final Credential savedCredential = savedCredentialOptional.get();

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
     * @throws NoSuchPaddingException
     *             Thrown if the padding algorithm is not available.
     * @throws InvalidKeyException
     *             Thrown if the incoming symmetric key is invalid. We don't
     *             have any way of validating it beforehand.
     * @throws IllegalBlockSizeException
     *             Thrown if the data is too long to be encrypted.
     * @throws BadPaddingException
     *             Thrown if the padding data is incorrect.
     * @throws UnsupportedEncodingException
     *             Thrown if we can't convert the plain text strings to UTF-8
     *             bytes.
     * @throws ExecutionException
     *             Thrown if we fail to create the {@link EncryptionUtil} from
     *             within the cache.
     */
    private void fillUpEncryptAndSaveCredential(final String userPublicKey,
            final Credential credential, final Credential incomingCredential)
                    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
                    IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException,
                    ExecutionException {
        final EncryptionUtil encryptionUtil = this.getEncryptionUtilFromCache(userPublicKey);
        final SecretKey symetricKey = encryptionUtil.generateSymmetricKey();

        // The asymmetric key is stored as it is. At this point there is not
        // security threat to store it like this.
        credential.setKey(userPublicKey);
        // The symmetric key is stored encrypted using the asymmetric public
        // key. This can only be decrypted using the private keys, so not even
        // us can decrypt it later.
        credential.setSymmetricKey(encryptionUtil.encrypt(symetricKey));

        credential.setPrimary(encryptionUtil.encrypt(symetricKey, incomingCredential.getPrimary()));
        if (StringUtils.isNotBlank(incomingCredential.getSecondary())) {
            credential.setSecondary(
                    encryptionUtil.encrypt(symetricKey, incomingCredential.getSecondary()));
        }

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
        return this.encryptionUtil.get(userPublicKey,
                new Callable<EncryptionUtil>() {
                    @Override
                    public EncryptionUtil call() throws Exception {
                        return new EncryptionUtil(userPublicKey,
                                CredentialResource.this.configuration.getSymmetricKeySize());
                    }
                });
    }

    @DELETE
    @ApiOperation("Deletes a credential pair from the database.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    @CircuitBreaker
    @UnitOfWork
    public Response deleteCredential(@HeaderParam("X-Auth-RSA") final String userPublicKey) {
        if (StringUtils.isBlank(userPublicKey)) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        if (this.credentialDAO.deleteByKey(userPublicKey)) {
            return Response.ok().build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }
}