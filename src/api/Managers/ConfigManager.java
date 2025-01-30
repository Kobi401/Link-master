package api.Managers;

import api.security.EncryptionManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * ConfigManager class for managing and storing user settings securely using encryption.
 */
public class ConfigManager {

    private static final String USER_HOME = System.getProperty("user.home");
    private static final String CONFIG_DIRECTORY = USER_HOME + "\\AppData\\Local\\LinkBrowser\\UserSettings";
    private static final String CONFIG_FILE = CONFIG_DIRECTORY + "\\config.properties";

    private Properties properties;
    private EncryptionManager encryptionManager;

    /**
     * Initializes the configuration manager and loads properties securely.
     */
    public ConfigManager() {
        properties = new Properties();
        encryptionManager = new EncryptionManager();

        File configDir = new File(CONFIG_DIRECTORY);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        loadEncryptedProperties();
    }

    /**
     * Checks if Flash is enabled.
     *
     * @return True if Flash is enabled, otherwise false.
     */
    public boolean isFlashEnabled() {
        return Boolean.parseBoolean(properties.getProperty("enable_flash", "true"));
    }

    /**
     * Sets the Flash enable/disable property.
     *
     * @param enabled True to enable Flash, otherwise false.
     */
    public void setFlashEnabled(boolean enabled) {
        properties.setProperty("enable_flash", Boolean.toString(enabled));
        saveEncryptedProperties();
    }

    /**
     * Saves the properties to an encrypted file.
     */
    private void saveEncryptedProperties() {
        try {
            String propertiesString = convertPropertiesToString(properties);
            String encryptedContent = encryptionManager.encrypt(propertiesString);
            try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
                fos.write(encryptedContent.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the encrypted properties from the configuration file.
     */
    private void loadEncryptedProperties() {
        try {
            if (Files.exists(Paths.get(CONFIG_FILE))) {
                String encryptedContent = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
                if (!encryptedContent.isEmpty()) {
                    String decryptedContent = encryptionManager.decrypt(encryptedContent);
                    properties.load(new java.io.StringReader(decryptedContent));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts Properties to a formatted string.
     *
     * @param properties The properties object.
     * @return The formatted properties string.
     */
    private String convertPropertiesToString(Properties properties) {
        StringBuilder sb = new StringBuilder();
        properties.forEach((key, value) -> sb.append(key).append("=").append(value).append("\n"));
        return sb.toString();
    }
}
