package com.github.mtakaki.credentialstorage.client.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import jodd.util.Base64;

/**
 * Encryption utility class that helps decrypt using asymmetrical and
 * symmetrical keys.
 *
 * @author mtakaki
 *
 */
public class DecryptionUtil {
    private static final String ASYMMETRIC_CIPHER = "RSA/ECB/PKCS1Padding";
    private static final String ASYMMETRIC_KEY_ALGORITHM = "RSA";
    private static final String SYMMETRIC_CIPHER = "AES/ECB/PKCS5Padding";
    private static final String SYMMETRIC_KEY_ALGORITHM = "AES";

    private final PrivateKey privateKey;

    public DecryptionUtil(final byte[] privateKeyBytes)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        this.privateKey = this.loadPrivateKey(privateKeyBytes);
    }

    public DecryptionUtil(final File privateKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException, FileNotFoundException,
            IOException {
        this(IOUtils.toByteArray(new FileInputStream(privateKey)));
    }

    /**
     * Loads the {@link PrivateKey} with the given bytes. The incoming private
     * key should be in a PKCS8 format.
     *
     * @param keyBytes
     *            The private key read as a byte array.
     * @return The {@link PrivateKey} object representing the given private key.
     * @throws NoSuchAlgorithmException
     *             Thrown if the RSA algorithm is not available.
     * @throws InvalidKeySpecException
     *             Thrown if the key format is invalid.
     */
    private PrivateKey loadPrivateKey(final byte[] keyBytes)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        final KeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        final KeyFactory kf = KeyFactory.getInstance(ASYMMETRIC_KEY_ALGORITHM);
        return kf.generatePrivate(spec);
    }

    /**
     * Receives a symmetric key, encoded as a base64 string, and decrypts it
     * using the asymmetrical private key. The incoming key must have been
     * encrypted using the asymmetrical public key in order to be able to
     * decrypt it.
     *
     * @param base64EncryptedSymmetricKey
     *            The incoming symmetrical key encoded as a base 64 string.
     * @return A {@link SecretKey} built using the given base64 string.
     * @throws NoSuchAlgorithmException
     *             Thrown if the RSA algorithm is not available.
     * @throws NoSuchPaddingException
     *             Thrown if the padding PKCS1 is not available.
     * @throws InvalidKeyException
     *             Thrown if the private key is invalid.
     * @throws IllegalBlockSizeException
     *             Thrown if the incoming string being decoded is too long to be
     *             decrypted.
     * @throws BadPaddingException
     *             Thrown if the padding is incorrect.
     */
    public SecretKey decryptSecretKey(final String base64EncryptedSymmetricKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        if (StringUtils.isBlank(base64EncryptedSymmetricKey)) {
            return null;
        }

        final Cipher cipher = Cipher.getInstance(ASYMMETRIC_CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, this.privateKey);
        final byte[] decryptedSymmetricKey = cipher
                .doFinal(Base64.decode(base64EncryptedSymmetricKey));
        return new SecretKeySpec(decryptedSymmetricKey, SYMMETRIC_KEY_ALGORITHM);
    }

    /**
     * Decrypts the given base64 encoded encrypted text using the symmetric key.
     *
     * @param secretKey
     *            Symmetric key used to decrypt the incoming text.
     * @param base64EncryptedText
     *            Base64 encoded encrypted text.
     * @return The text decrypted.
     * @throws NoSuchAlgorithmException
     *             Thrown if AES algorithm is not available.
     * @throws NoSuchPaddingException
     *             Thrown if PKCS5 padding is not available.
     * @throws InvalidKeyException
     *             Thrown if the symmetric key is invalid.
     * @throws IllegalBlockSizeException
     *             Thrown if the incoming string being decoded is too long to be
     *             decrypted.
     * @throws BadPaddingException
     *             Thrown if the padding is incorrect.
     */
    public String decryptText(final SecretKey secretKey, final String base64EncryptedText)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        if (StringUtils.isBlank(base64EncryptedText)) {
            return null;
        }

        final Cipher cipher = Cipher.getInstance(SYMMETRIC_CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return new String(cipher.doFinal(Base64.decode(base64EncryptedText)),
                StandardCharsets.UTF_8);
    }
}