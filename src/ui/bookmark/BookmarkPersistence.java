package ui.bookmark;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * BookmarkPersistence class handles saving and loading bookmarks manually without external libraries.
 * Bookmarks are stored in JSON format in the user.home/LinkBrowser/bookmarks.json file.
 */
public class BookmarkPersistence {
    private static final String BOOKMARKS_DIR = System.getProperty("user.home") + File.separator + "LinkBrowser";
    private static final String BOOKMARKS_FILE = BOOKMARKS_DIR + File.separator + "bookmarks.json";

    /**
     * Saves the list of bookmarks to a JSON file in user.home/LinkBrowser/bookmarks.json.
     *
     * @param bookmarks List of bookmarks to save.
     */
    public static void saveBookmarks(List<Bookmark> bookmarks) {
        File dir = new File(BOOKMARKS_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                System.err.println("Failed to create bookmarks directory: " + BOOKMARKS_DIR);
                return;
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(BOOKMARKS_FILE))) {
            String json = serializeBookmarksToJson(bookmarks);
            writer.write(json);
        } catch (IOException e) {
            System.err.println("Failed to save bookmarks: " + e.getMessage());
        }
    }

    /**
     * Loads the list of bookmarks from the JSON file in user.home/LinkBrowser/bookmarks.json.
     *
     * @return List of bookmarks, or an empty list if loading fails.
     */
    public static List<Bookmark> loadBookmarks() {
        File file = new File(BOOKMARKS_FILE);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        StringBuilder jsonBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(BOOKMARKS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line.trim());
            }
            String json = jsonBuilder.toString();
            return deserializeJsonToBookmarks(json);
        } catch (IOException e) {
            System.err.println("Failed to load bookmarks: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Serializes a list of Bookmark objects to a JSON-formatted string manually.
     *
     * @param bookmarks List of bookmarks to serialize.
     * @return JSON-formatted string representing the list of bookmarks.
     */
    private static String serializeBookmarksToJson(List<Bookmark> bookmarks) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[\n");
        for (int i = 0; i < bookmarks.size(); i++) {
            Bookmark bookmark = bookmarks.get(i);
            jsonBuilder.append("  {\n");
            jsonBuilder.append("    \"name\": \"").append(escapeJson(bookmark.getName())).append("\",\n");
            jsonBuilder.append("    \"url\": \"").append(escapeJson(bookmark.getUrl())).append("\"\n");
            jsonBuilder.append("  }");
            if (i < bookmarks.size() - 1) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\n");
        }
        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }

    /**
     * Deserializes a JSON-formatted string to a list of Bookmark objects manually.
     *
     * @param json JSON-formatted string representing the list of bookmarks.
     * @return List of Bookmark objects.
     */
    private static List<Bookmark> deserializeJsonToBookmarks(String json) {
        List<Bookmark> bookmarks = new ArrayList<>();
        json = json.trim();

        if (!json.startsWith("[") || !json.endsWith("]")) {
            System.err.println("Invalid JSON format for bookmarks.");
            return bookmarks;
        }
        json = json.substring(1, json.length() - 1).trim();
        String[] bookmarkEntries = json.split("\\},\\s*\\{");
        for (String entry : bookmarkEntries) {
            entry = entry.replaceAll("^\\{", "").replaceAll("\\}$", "").trim();
            String name = null;
            String url = null;
            String[] keyValuePairs = entry.split(",\\s*");
            for (String pair : keyValuePairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length != 2) continue;
                String key = keyValue[0].trim().replaceAll("^\"|\"$", "");
                String value = keyValue[1].trim().replaceAll("^\"|\"$", "");
                if (key.equals("name")) {
                    name = unescapeJson(value);
                } else if (key.equals("url")) {
                    url = unescapeJson(value);
                }
            }
            if (name != null && url != null) {
                bookmarks.add(new Bookmark(name, url));
            }
        }

        return bookmarks;
    }

    /**
     * Escapes special characters in a string for JSON compatibility.
     *
     * @param text The string to escape.
     * @return Escaped string.
     */
    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Unescapes special characters in a JSON string.
     *
     * @param text The JSON string to unescape.
     * @return Unescaped string.
     */
    private static String unescapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\b", "\b")
                .replace("\\f", "\f")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
