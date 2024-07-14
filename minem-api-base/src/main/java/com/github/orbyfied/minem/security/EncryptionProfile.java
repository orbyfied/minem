package com.github.orbyfied.minem.security;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * General utility encryption handler, replacement
 * for the old LegacyEncryptionProfile, which was
 * horribly coded.
 */
@SuppressWarnings("rawtypes")
public abstract class EncryptionProfile<S extends EncryptionProfile> {

    public static Cipher getCipherSafe(String name) {
        try {
            return Cipher.getInstance(name);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

        return null;
    }

    /* -------------------------------- */

    public enum CipherType {

        SYMMETRIC,
        ASYMMETRIC

    }

    // utilities

    protected String toBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public byte[] fromBase64(String str) {
        return Base64.getDecoder().decode(str);
    }

    /* -------------------------------- */

    @SuppressWarnings("unchecked")
    private final S self = (S) this;

    // implementation properties
    protected final int unpaddedBlockSize;
    protected final int paddedBlockSize;

    // cipher properties
    protected final CipherType cipherType;
    protected String algorithm;
    protected String mode;
    protected String padding;

    // the instances
    protected Cipher cipher;

    public EncryptionProfile(CipherType type, int unpaddedBlockSize, int paddedBlockSize) {
        this.cipherType = type;

        this.unpaddedBlockSize = unpaddedBlockSize;
        this.paddedBlockSize   = paddedBlockSize;
    }

    public EncryptionProfile(CipherType type, int unpaddedBlockSize, int paddedBlockSize,
            /* actual parameters */
                             String algorithm, String mode, String padding) {
        this(type, unpaddedBlockSize, paddedBlockSize);
        withCipher(algorithm, mode, padding);
    }

    public CipherType getCipherType() {
        return cipherType;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getMode() {
        return mode;
    }

    public String getPadding() {
        return padding;
    }

    public Cipher getCipher() {
        return cipher;
    }

    /*
        Configuration
     */

    public S withCipher(Cipher cipher) {
        // set properties
        this.algorithm = cipher.getAlgorithm();

        // set cipher
        this.cipher = cipher;

        // return
        return self;
    }

    public S withCipher(String algorithm, String mode, String padding) {
        // set properties basic
        this.algorithm = algorithm;
        this.mode      = mode;
        this.padding   = padding;

        try {
            cipher = Cipher.getInstance(algorithm + "/" + mode + "/" + padding);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // return
        return self;
    }

    /*
        Keys
     */

    public abstract S generateKeys(int len);
    public abstract Key getEncryptionKey();
    public abstract Key getDecryptionKey();

    public abstract S withKey(String name, Key key);
    public abstract <K extends Key> K decodeKey(Class<K> kClass, byte[] bytes);
    public byte[] encodeKey(Key key) {
        return (key != null ? key.getEncoded() : null); // kinda obvious
    }

    public <K extends Key> K decodeKeyFromBase64(Class<K> kClass, String key) {
        return decodeKey(kClass, fromBase64(key));
    }

    public String encodeKeyToBase64(Key key) {
        return toBase64(encodeKey(key));
    }

    /*
        Encryption and Decryption
     */

    public byte[] encrypt(byte[] bytes) {
        if (cipher == null)
            throw new IllegalStateException();
        try {
            cipher.init(Cipher.ENCRYPT_MODE, getEncryptionKey());
            return cipher.doFinal(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] decrypt(byte[] bytes) {
        if (cipher == null)
            throw new IllegalStateException();
        try {
            cipher.init(Cipher.DECRYPT_MODE, getDecryptionKey());
            return cipher.doFinal(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] encryptUTF(String str) {
        return encrypt(str.getBytes(StandardCharsets.UTF_8));
    }

    public String decryptUTF(byte[] bytes) {
        return new String(decrypt(bytes), StandardCharsets.UTF_8);
    }

    public String encryptToBase64(byte[] bytes) {
        return toBase64(encrypt(bytes));
    }

    public byte[] decryptFromBase64(String str) {
        return decrypt(fromBase64(str));
    }

    /*
        Large Data Encryption and Decryption
     */

    /**
     * Output stream which wraps an output stream,
     * writing into a buffer which is encrypted at
     * the end of a block.
     */
    public class EncryptingOutputStream extends OutputStream {
        // the block size, for buffer alignment
        final int blockSize =
                /* padding doesnt have to be taken into account when writing */ unpaddedBlockSize;

        // the output stream
        final OutputStream stream;
        public EncryptingOutputStream(OutputStream stream) {
            this.stream = stream;
        }

        // the encrypted data buffer
        byte[] buf = new byte[blockSize];
        // the index in the buffer to read to
        int bi = 0;

        @Override
        public void write(int b) throws IOException {
            // put byte
            byte by = (byte) b;
            buf[bi++] = by;

            // flush and write encrypted if
            // the buffer is full
            if (bi >= buf.length) {
                flush();

                // allocate new empty buffer
                buf = new byte[blockSize];
                // reset buffer index
                bi = 0;
            }
        }

        @Override
        public void flush() throws IOException {
            // encrypt and write buffer
            byte[] encrypted = encrypt(buf);
            stream.write(encrypted);
        }

        public DataOutputStream toDataStream() {
            return new DataOutputStream(this);
        }

        public ObjectOutputStream toObjectStream() throws IOException {
            return new ObjectOutputStream(this);
        }
    }

    /**
     * Input stream which wraps an input stream,
     * reading from it into a buffer and decrypting that.
     */
    public class DecryptingInputStream extends InputStream {
        // the block size, for buffer alignment
        final int blockSize = paddedBlockSize;

        // the input stream
        InputStream stream;
        public DecryptingInputStream(InputStream stream) {
            this.stream = stream;
        }

        // the decrypted data buffer
        byte[] buf = new byte[blockSize];
        // the amount of indices read from the buffer
        int used = 0;

        @Override
        public int read() throws IOException {
            // decrypt next block if used
            if (used == 0 || used >= buf.length) {
                // read next encrypted block
                byte[] encrypted = stream.readNBytes(blockSize);
                // decrypt and put in buffer
                buf = decrypt(encrypted);
            }

            // read decrypted byte
            byte by = buf[used++];

            // return
            return by;
        }

        public DataInputStream toDataStream() {
            return new DataInputStream(this);
        }

        public ObjectInputStream toObjectStream() throws IOException {
            return new ObjectInputStream(this);
        }
    }

    public EncryptingOutputStream encryptingOutputStream(final OutputStream out) {
        return new EncryptingOutputStream(out);
    }

    public DecryptingInputStream decryptingInputStream(final InputStream in) {
        return new DecryptingInputStream(in);
    }

}
