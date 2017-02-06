package com.github.mtakaki.credentialstorage.encryption;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Optional;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;

import jodd.util.Base64;

/**
 * Utility class that wraps encryption operations using RSA asymmetric algorithm
 * and AES symmetric algorithm.
 *
 * @author mtakaki
 *
 */
public class EncryptionUtil {
    private static final String ASYMMETRIC_CIPHER = "RSA/ECB/PKCS1Padding";
    private static final String ASYMMETRIC_KEY_ALGORITHM = "RSA";
    private static final String SYMMETRIC_CIPHER = "AES/ECB/PKCS5Padding";
    private static final String SYMMETRIC_KEY_ALGORITHM = "AES";

    private final PublicKey publicKey;
    private final int symmetricKeySize;

    public EncryptionUtil(final byte[] publicKeyBytes, final int symmetricKeySize)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        final X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
        final KeyFactory kf = KeyFactory.getInstance(ASYMMETRIC_KEY_ALGORITHM);
        this.publicKey = kf.generatePublic(spec);
        this.symmetricKeySize = symmetricKeySize;
    }

    public EncryptionUtil(final String base64PublicKey, final int symmetricKeySize)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        this(Base64.decode(base64PublicKey), symmetricKeySize);
    }

    /**
     * Generates a new random symmetric key that is used to encrypt credentials.
     *
     * @return A new random symmetric key.
     * @throws NoSuchAlgorithmException
     *             Thrown if AES algorithm is not supported in the host.
     */
    public SecretKey generateSymmetricKey()
            throws NoSuchAlgorithmException {
        final KeyGenerator keyGenerator = KeyGenerator.getInstance(SYMMETRIC_KEY_ALGORITHM);
        keyGenerator.init(this.symmetricKeySize);
        return keyGenerator.generateKey();
    }

    /**
     * Creates a {@link SecretKey} from a base64 string format.
     *
     * @param base64SecretKey
     *            The symmetric key in a base64 string format.
     * @return A {@link SecretKey} loaded from the given symmetric key string.
     */
    public SecretKey loadSecretKey(final String base64SecretKey) {
        return new SecretKeySpec(Base64.decode(base64SecretKey), SYMMETRIC_KEY_ALGORITHM);
    }

    /**
     * Encrypts the given symmetric key using the internal asymmetrical public
     * key. It can only be decrypted using the private key, which is not
     * available in the server side as only the the client has it.
     *
     * @param symetricKey
     *            The symmetric key that will be encrypted and converted into
     *            base64.
     * @return The encrypted symmetric key in a base64 string.
     * @throws InitializationException
     *             Thrown if the RSA algorithm is not available, or if PKCS1
     *             padding is not available, or if the public key is invalid, or
     *             if the symmetric key length is too long to be encrypted using
     *             RSA algorithm, or if the data is not padded correctly.
     */
    public String encrypt(final SecretKey symetricKey)
            throws InitializationException {
        try {
            final Cipher cipher = Cipher.getInstance(ASYMMETRIC_CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, this.publicKey);

            final byte[] encryptedSymmetricKey = cipher.doFinal(symetricKey.getEncoded());
            return Base64.encodeToString(encryptedSymmetricKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | IllegalBlockSizeException | BadPaddingException e) {
            throw new InitializationException(e);
        }
    }

    /**
     * Encrypts the plain text using the given symmetric key. This text can be
     * decrypted using the same symmetric key.
     *
     * @param symmetricSecretKey
     *            The symmetric key used to encrypt the given plain text.
     * @param plainText
     *            The text that will be encrypted.
     * @return A base64 string representing the encrypted given plain text. If
     *         the give plain text is blank, we'll return just an
     *         {@code Optional.empty()}.
     * @throws InitializationException
     *             Thrown if the AES algorithm is not available, or if PKCS1
     *             padding is not available, or if the symmetric key is invalid,
     *             or if the plain text is too long to be encrypted, or if the
     *             data is not padded correctly.
     */
    public Optional<String> encrypt(final SecretKey symmetricSecretKey, final String plainText)
            throws InitializationException {
        if (StringUtils.isBlank(plainText)) {
            return Optional.empty();
        }

        try {
            final Cipher cipher = Cipher.getInstance(SYMMETRIC_CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, symmetricSecretKey);
            final byte[] encryptedBytes = cipher
                    .doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Optional.of(Base64.encodeToString(encryptedBytes));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | IllegalBlockSizeException | BadPaddingException e) {
            throw new InitializationException(e);
        }
    }
}