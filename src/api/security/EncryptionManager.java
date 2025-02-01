package api.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionManager {

    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String SECRET_KEY_FILE = System.getProperty("user.home")
            + File.separator + "AppData" + File.separator + "Local" + File.separator + "LinkBrowser"
            + File.separator + "LinkService(x32).dat"; //put this in a different dir to hide it better

    private SecretKey secretKey;

    /**
     * Initializes the EncryptionManager by loading a secret key from file or generating a new one.
     */
    public EncryptionManager() {
        try {
            File keyFile = new File(SECRET_KEY_FILE);
            if (keyFile.exists()) {
                loadSecretKey();
            } else {
                generateAndSaveSecretKey();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize EncryptionManager", e);
        }
    }

    /**
     * Encrypts the given plain text using AES/GCM/NoPadding.
     * A random 12-byte IV is generated for each encryption.
     * The returned string is Base64-encoded and contains the IV concatenated with the ciphertext.
     *
     * @param data The plain text to encrypt.
     * @return A Base64-encoded string containing the IV and ciphertext.
     * @throws Exception If encryption fails.
     */
    public String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        //generate a random IV.
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
        byte[] ciphertext = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        //prepend the IV to the ciphertext.
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        byteBuffer.put(iv);
        byteBuffer.put(ciphertext);
        byte[] cipherMessage = byteBuffer.array();

        return Base64.getEncoder().encodeToString(cipherMessage);
    }

    /**
     * Decrypts a Base64-encoded string containing an IV and ciphertext.
     *
     * @param encryptedData The Base64-encoded string to decrypt.
     * @return The decrypted plain text.
     * @throws Exception If decryption fails.
     */
    public String decrypt(String encryptedData) throws Exception {
        byte[] cipherMessage = Base64.getDecoder().decode(encryptedData);
        ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);
        byte[] ciphertext = new byte[byteBuffer.remaining()];
        byteBuffer.get(ciphertext);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
        byte[] plainText = cipher.doFinal(ciphertext);

        return new String(plainText, StandardCharsets.UTF_8);
    }

    /**
     * Generates a new 256-bit AES secret key, saves it to file, and sets it.
     *
     * @throws Exception If key generation or saving fails.
     */
    private void generateAndSaveSecretKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
        keyGen.init(256, new SecureRandom());
        SecretKey key = keyGen.generateKey();
        saveSecretKey(key);
        this.secretKey = key;
    }

    /**
     * Saves the provided secret key to the designated file.
     *
     * @param key The secret key to save.
     * @throws Exception If file operations fail.
     */
    private void saveSecretKey(SecretKey key) throws Exception {
        byte[] keyBytes = key.getEncoded();
        File keyFile = new File(SECRET_KEY_FILE);
        File parentDir = keyFile.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new Exception("Could not create directories for secret key");
        }
        try (FileOutputStream fos = new FileOutputStream(keyFile)) {
            fos.write(keyBytes);
        }
    }

    /**
     * Loads the secret key from the designated file.
     *
     * @throws Exception If the key cannot be loaded.
     */
    private void loadSecretKey() throws Exception {
        File keyFile = new File(SECRET_KEY_FILE);
        byte[] keyBytes = new byte[(int) keyFile.length()];
        try (FileInputStream fis = new FileInputStream(keyFile)) {
            int read = fis.read(keyBytes);
            if (read != keyBytes.length) {
                throw new Exception("Failed to read the complete secret key file");
            }
        }
        this.secretKey = new SecretKeySpec(keyBytes, ENCRYPTION_ALGORITHM);
    }

    // support for multiple algorithms, integration with a KeyStore would be nice
}
