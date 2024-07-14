package com.github.orbyfied.minem.security;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;

public class AsymmetricEncryptionProfile extends EncryptionProfile<AsymmetricEncryptionProfile> {

    public static KeyFactory getKeyFactorySafe(String name) {
        try {
            return KeyFactory.getInstance(name);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static KeyPairGenerator getKeyPairGeneratorSafe(String name) {
        try {
            return KeyPairGenerator.getInstance(name);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static final AsymmetricEncryptionProfile UTILITY_RSA_1024 =
            new AsymmetricEncryptionProfile("RSA", "ECB", "PKCS1Padding", "RSA", 1024);

    /* -------------------------------------- */

    public static final int UNPADDED_BLOCK_SIZE = 117;
    public static final int PADDED_BLOCK_SIZE   = 128;

    // the key pair
    private PublicKey  publicKey;
    private PrivateKey privateKey;

    // the key algorithm
    private String     keyAlgorithm;
    private KeyFactory keyFactory;
    private int        keyLength;

    public AsymmetricEncryptionProfile() {
        super(CipherType.SYMMETRIC, UNPADDED_BLOCK_SIZE, PADDED_BLOCK_SIZE);
    }

    public AsymmetricEncryptionProfile(String algorithm, String mode, String padding, String keyAlgorithm) {
        super(CipherType.ASYMMETRIC, UNPADDED_BLOCK_SIZE, PADDED_BLOCK_SIZE,
                algorithm, mode, padding);
        withKeyAlgorithm(keyAlgorithm);
    }

    public AsymmetricEncryptionProfile(String algorithm, String mode, String padding, String keyAlgorithm, int keyLength) {
        this(algorithm, mode, padding, keyAlgorithm);
        this.keyLength = keyLength;
    }

    public int getKeyLength() {
        return keyLength;
    }

    public KeyFactory getKeyFactory() {
        return keyFactory;
    }

    public String getKeyAlgorithm() {
        return keyAlgorithm;
    }

    /*
        Keys
     */

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public AsymmetricEncryptionProfile withKeyAlgorithm(String algorithm) {
        Objects.requireNonNull(algorithm, "algorithm cannot be null");
        this.keyAlgorithm = algorithm;
        this.keyFactory   = getKeyFactorySafe(algorithm);
        if (keyFactory == null)
            throw new IllegalArgumentException("key factory is null");
        return this;
    }

    public synchronized AsymmetricEncryptionProfile withPublicKey(PublicKey key) {
        this.publicKey = key;
        return this;
    }

    public synchronized AsymmetricEncryptionProfile withPrivateKey(PrivateKey key) {
        this.privateKey = key;
        return this;
    }

    public AsymmetricEncryptionProfile withKeyLength(int len) {
        this.keyLength = len;
        return this;
    }

    @Override
    public AsymmetricEncryptionProfile generateKeys(int len) {
        if (keyAlgorithm == null)
            throw new IllegalStateException();

        try {
            KeyPairGenerator gen = getKeyPairGeneratorSafe(keyAlgorithm);
            if (gen == null)
                return this;
            gen.initialize(len);
            KeyPair pair = gen.generateKeyPair();

            this.keyLength  = len;
            this.privateKey = pair.getPrivate();
            this.publicKey  = pair.getPublic();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    public AsymmetricEncryptionProfile generateKeys() {
        return generateKeys(keyLength);
    }

    @Override
    public Key getEncryptionKey() {
        return publicKey;
    }

    @Override
    public Key getDecryptionKey() {
        return privateKey;
    }

    @Override
    public synchronized AsymmetricEncryptionProfile withKey(String name, Key key) {
        switch (name.toLowerCase()) {
            case "public"  -> this.publicKey  = (PublicKey)  key;
            case "private" -> this.privateKey = (PrivateKey) key;
            default -> { throw new IllegalArgumentException("no key named '" + name + "'"); }
        }

        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends Key> K decodeKey(Class<K> kClass, byte[] bytes) {
        if (keyFactory == null)
            throw new IllegalStateException();
        try {
            if (PublicKey.class.isAssignableFrom(kClass)) /* decode pub key */ {
                X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(bytes);
                return (K) keyFactory.generatePublic(publicSpec);
            } else if (PrivateKey.class.isAssignableFrom(kClass)) /* decode prv key */ {
                PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(bytes);
                return (K) keyFactory.generatePrivate(privateSpec);
            }

            // unknown key type
            throw new IllegalArgumentException("unknown key type to decode to: " + kClass);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
