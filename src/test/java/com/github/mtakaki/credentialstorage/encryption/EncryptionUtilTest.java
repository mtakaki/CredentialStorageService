package com.github.mtakaki.credentialstorage.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.junit.Before;
import org.junit.Test;

import jodd.util.Base64;

public class EncryptionUtilTest {
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
    private static final byte[] TEST_DES_SYMETRIC_KEY = new byte[] { 7, -30, -25, 53, 86, 48, 48,
            -105, 75, -84, -117, -24, 28, 84, -74, 4 };

    private EncryptionUtil encryptionUtil;

    @Before
    public void setup() throws IOException, InvalidKeyException, NoSuchAlgorithmException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException,
            NoSuchProviderException, InvalidKeySpecException {
        this.encryptionUtil = new EncryptionUtil(TEST_RSA_PUBLIC_KEY, 128);
    }

    @Test
    public void testGenerateSymmetricKey() throws Exception {
        final SecretKey secretKey = this.encryptionUtil.generateSymmetricKey();
        assertThat(secretKey).isNotNull();
        assertThat(secretKey.getEncoded()).hasSize(16);
        assertThat(secretKey.getAlgorithm()).isEqualTo("AES");
    }

    @Test
    public void testLoadSecretKey() {
        final SecretKey secretKey = this.encryptionUtil
                .loadSecretKey(Base64.encodeToString(TEST_DES_SYMETRIC_KEY));
        assertThat(secretKey).isNotNull();
        assertThat(secretKey.getEncoded()).hasSize(16);
        assertThat(secretKey.getAlgorithm()).isEqualTo("AES");
    }

    @Test
    public void testEncryptSecretKey() throws Exception {
        final SecretKey secretKey = this.encryptionUtil
                .loadSecretKey(Base64.encodeToString(TEST_DES_SYMETRIC_KEY));
        assertThat(this.encryptionUtil.encrypt(secretKey)).hasSize(684);
    }

    @Test
    public void testEncryptWithSymetricKey() throws Exception {
        final SecretKey secretKey = this.encryptionUtil
                .loadSecretKey(Base64.encodeToString(TEST_DES_SYMETRIC_KEY));
        assertThat(this.encryptionUtil.encrypt(secretKey, "123"))
                .isEqualTo("s+KpNGC/0McSdf4W2YxBuw==");
    }
}