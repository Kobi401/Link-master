package api.Managers;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;

import api.security.EncryptionManager;

public class UserManagerBridge {

    private WebEngine webEngine;
    private EncryptionManager encryptionManager;

    public UserManagerBridge(WebEngine webEngine) {
        this.webEngine = webEngine;
        this.encryptionManager = new EncryptionManager();
    }

    /**
     * Attempts to log in using the given username and password.
     * The password is encrypted before being passed to UserManager.
     * Returns true if the login is successful, false otherwise.
     * If login succeeds, it automatically redirects to HomePage.html.
     */
    public boolean login(String username, String password) {
        try {
            String encryptedPassword = encryptionManager.encrypt(password);
            boolean result = UserManager.login(username, encryptedPassword);
            if (result) {
                Platform.runLater(() -> {
                    webEngine.load("www.google.com");
                });
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates a new account with the specified username and password.
     * The password is encrypted before being stored.
     * After creating the account, it automatically redirects to HomePage.html.
     */
    public void createAccount(String username, String password) {
        try {
            String encryptedPassword = encryptionManager.encrypt(password);
            UserManager.createAccount(username, encryptedPassword);
            Platform.runLater(() -> {
                webEngine.load("www.google.com");
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Logs in as a guest.
     * This method can be called from JavaScript.
     * After logging in as guest, it automatically redirects to HomePage.html.
     */
    public void guestLogin() {
        UserManager.guestLogin();
        Platform.runLater(() -> {
            webEngine.load("www.google.com");
        });
    }
}

