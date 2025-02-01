package api.Managers;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class UserManager {
    private static final String USER_DATA_DIR = System.getProperty("user.home") + "/LinkBrowser/userdata";
    private static final String USER_DATA_FILE = USER_DATA_DIR + "/user.json";

    /**
     * Builds a JSON string for user data.
     */
    private static String buildUserJson(String username, String password, boolean guest) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"guest\":" + guest + "}";
    }

    /**
     * Retrieves a String value from a JSON-like string based on a given key.
     * Assumes the JSON object is in the format: {"key":"value",...}
     */
    private static String getJsonStringValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start < 0) return null;
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    /**
     * Retrieves a boolean value from a JSON-like string based on a given key.
     * Assumes the JSON object is in the format: {"key":true,...} or {"key":false,...}
     */
    private static boolean getJsonBooleanValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start < 0) return false;
        start += searchKey.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        if (json.startsWith("true", start)) {
            return true;
        }
        return false;
    }

    /**
     * Initializes the user system. If user data exists, it loads the user info;
     * otherwise, it loads the login/registration page.
     */
    public static void initializeUser(WebEngine webEngine) {
        File file = new File(USER_DATA_FILE);
        if (!file.exists()) {
            String loginUrl = UserManager.class.getResource("/LoginPage.html").toExternalForm();
            Platform.runLater(() -> webEngine.load(loginUrl));
        } else {
            try {
                String content = new String(Files.readAllBytes(Paths.get(USER_DATA_FILE)));
                String username = getJsonStringValue(content, "username");
                boolean guest = getJsonBooleanValue(content, "guest");
                System.out.println("Logged in as: " + (username != null && !username.isEmpty() ? username : (guest ? "Guest" : "Unknown")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates a new user account with the specified username and password.
     * (Note: This sample saves passwords in plaintext; for a real application, use proper hashing.)
     */
    public static void createAccount(String username, String password) {
        String json = buildUserJson(username, password, false);
        saveUserData(json);
    }

    /**
     * Validates a login attempt.
     */
    public static boolean login(String username, String password) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(USER_DATA_FILE)));
            String storedUsername = getJsonStringValue(content, "username");
            String storedPassword = getJsonStringValue(content, "password");
            return username.equals(storedUsername) && password.equals(storedPassword);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Logs in as a guest.
     */
    public static void guestLogin() {
        String json = buildUserJson("", "", true);
        saveUserData(json);
    }

    private static void saveUserData(String json) {
        try {
            File dir = new File(USER_DATA_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            FileWriter writer = new FileWriter(USER_DATA_FILE);
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}