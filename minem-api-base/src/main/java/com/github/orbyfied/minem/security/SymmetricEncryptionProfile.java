package com.github.orbyfied.minem.security;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class SymmetricEncryptionProfile extends EncryptionProfile<SymmetricEncryptionProfile> {

    public static final int AES_KEY_LENGTH       = 128;
    public static final int AES_KEY_LENGTH_BYTES = 16;

    public static SymmetricEncryptionProfile newSymmetricEncryptionProfile() {
        return new SymmetricEncryptionProfile("AES", "ECB", "PKCS5Padding", "AES", AES_KEY_LENGTH);
    }

    public static KeyGenerator getKeyGeneratorSafe(String name) {
        try {
            return KeyGenerator.getInstance(name);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    /* --------------------------------- */

    public static final int UNPADDED_BLOCK_SIZE = 117;
    public static final int PADDED_BLOCK_SIZE   = 128;

    // the secret key
    private SecretKey secretKey;

    // the key algorithm
    private String     keyAlgorithm;
    private int        keyLength;

    public SymmetricEncryptionProfile() {
        super(CipherType.ASYMMETRIC, UNPADDED_BLOCK_SIZE, PADDED_BLOCK_SIZE);
    }

    public SymmetricEncryptionProfile(String algorithm, String mode, String padding, String keyAlgorithm) {
        super(CipherType.SYMMETRIC, UNPADDED_BLOCK_SIZE, PADDED_BLOCK_SIZE,
                algorithm, mode, padding);
        withKeyAlgorithm(keyAlgorithm);
    }

    public SymmetricEncryptionProfile(String algorithm, String mode, String padding, String keyAlgorithm, int keyLength) {
        this(algorithm, mode, padding, keyAlgorithm);
        withKeyLength(keyLength);
    }

    public String getKeyAlgorithm() {
        return keyAlgorithm;
    }

    public int getKeyLength() {
        return keyLength;
    }

    /*
        Keys.
     */

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public SymmetricEncryptionProfile withKeyAlgorithm(String keyAlgorithm) {
        this.keyAlgorithm = keyAlgorithm;
        return this;
    }
    public SymmetricEncryptionProfile withKeyLength(int len) {
        this.keyLength = len;
        return this;
    }


    @Override
    public SymmetricEncryptionProfile generateKeys(int len) {
        if (keyAlgorithm == null)
            throw new IllegalStateException();

        try {
            KeyGenerator generator = getKeyGeneratorSafe(keyAlgorithm);
            if (generator == null)
                return this;
            generator.init(len);
            secretKey = generator.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    public SymmetricEncryptionProfile generateKeys() {
        return generateKeys(keyLength);
    }

    @Override
    public Key getEncryptionKey() {
        return secretKey;
    }

    @Override
    public Key getDecryptionKey() {
        return secretKey;
    }

    @Override
    public SymmetricEncryptionProfile withKey(String name, Key key) {
        this.secretKey = (SecretKey) key;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends Key> K decodeKey(Class<K> kClass, byte[] bytes) {
        if (keyAlgorithm == null)
            throw new IllegalStateException();
        try {
            if (SecretKey.class.isAssignableFrom(kClass)) /* decode secret key */ {
                SecretKeySpec spec = new SecretKeySpec(bytes, keyAlgorithm);
                return (K) spec;
            }

            // unknown key type
            throw new IllegalArgumentException("unknown key type to decode to: " + kClass);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
