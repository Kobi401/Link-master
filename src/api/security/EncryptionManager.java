package api.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * EncryptionManager for securely handling data encryption and decryption.
 */
public class EncryptionManager {

    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    private static final String SECRET_KEY_FILE = System.getProperty("user.home") + "\\AppData\\Local\\LinkBrowser\\LinkService(x32).dat";
    private SecretKey secretKey;

    /**
     * Initializes the EncryptionManager by generating or loading a secret key.
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
        }
    }

    /**
     * Encrypts the given text and returns the encrypted string.
     *
     * @param data The plain text data to be encrypted.
     * @return The encrypted string.
     * @throws Exception If encryption fails.
     */
    public String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Decrypts the given encrypted string and returns the plain text.
     *
     * @param encryptedData The encrypted string.
     * @return The decrypted plain text.
     * @throws Exception If decryption fails.
     */
    public String decrypt(String encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Generates a new secret key and saves it to a file for future use.
     */
    private void generateAndSaveSecretKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
        keyGen.init(256, new SecureRandom());  // Use a 256-bit key
        SecretKey key = keyGen.generateKey();
        saveSecretKey(key);
        this.secretKey = key;
    }

    /**
     * Saves the given secret key to a file.
     *
     * @param key The secret key to be saved.
     */
    private void saveSecretKey(SecretKey key) throws Exception {
        byte[] keyBytes = key.getEncoded();
        try (FileOutputStream fos = new FileOutputStream(SECRET_KEY_FILE)) {
            fos.write(keyBytes);
        }
    }

    /**
     * Loads the secret key from a file.
     */
    private void loadSecretKey() throws Exception {
        File keyFile = new File(SECRET_KEY_FILE);
        byte[] keyBytes = new byte[(int) keyFile.length()];
        try (FileInputStream fis = new FileInputStream(keyFile)) {
            fis.read(keyBytes);
            this.secretKey = new SecretKeySpec(keyBytes, ENCRYPTION_ALGORITHM);
        }
    }
}
