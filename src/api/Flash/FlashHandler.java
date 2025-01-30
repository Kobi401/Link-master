package api.Flash;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

public class FlashHandler {

    private static final String RUFFLE_JS_RESOURCE_PATH = "/ruffle/ruffle.js";
    private static final String RUFFLE_WASM_RESOURCE_PATH = "/ruffle/ruffle.wasm";

    private static final Logger LOGGER = Logger.getLogger(FlashHandler.class.getName());


    private boolean isFlashEnabled;

    /**
     * Constructor for FlashHandler.
     *
     * @param isFlashEnabled Flag to enable or disable Flash/Ruffle.
     */
    public FlashHandler(boolean isFlashEnabled) {
        this.isFlashEnabled = isFlashEnabled;
    }

    /**
     * Sets the Flash/Ruffle enabled state.
     *
     * @param isFlashEnabled True to enable Flash/Ruffle, false to disable.
     */
    public void setFlashEnabled(boolean isFlashEnabled) {
        this.isFlashEnabled = isFlashEnabled;
    }

    /**
     * Checks if Flash/Ruffle is enabled.
     *
     * @return True if enabled, false otherwise.
     */
    public boolean isFlashEnabled() {
        return isFlashEnabled;
    }

    /**
     * Injects Ruffle's JavaScript and configures it to replace Flash content within the WebEngine.
     *
     * @param webEngine The WebEngine instance where Flash content needs to be handled.
     */
    public void injectRuffleScript(WebEngine webEngine) {
        if (!isFlashEnabled) {
            System.out.println("Flash is disabled. Skipping Ruffle injection.");
            return;
        }

        // Load Ruffle's JavaScript
        String ruffleJs = loadResourceAsString(RUFFLE_JS_RESOURCE_PATH);
        if (ruffleJs == null) {
            System.err.println("Failed to load Ruffle JavaScript.");
            return;
        }

        // Execute Ruffle's JavaScript to initialize RufflePlayer
        Platform.runLater(() -> {
            webEngine.executeScript(ruffleJs);
            System.out.println("Ruffle JavaScript loaded successfully.");
        });

        // Listen for the page load completion to inject the Flash replacement script
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // After the page has fully loaded, inject the script to replace Flash objects
                injectFlashReplacementScript(webEngine);
            }
        });
    }

    /**
     * Loads a resource file as a String.
     *
     * @param resourcePath The path to the resource file.
     * @return The content of the resource file as a String, or null if an error occurs.
     */
    private String loadResourceAsString(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();

        } catch (IOException | NullPointerException e) {
            System.err.println("Error loading resource " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Injects the JavaScript code that replaces Flash objects with Ruffle players.
     *
     * @param webEngine The WebEngine instance where Flash content needs to be handled.
     */
    private void injectFlashReplacementScript(WebEngine webEngine) {
        String wasmUrl = getWasmUrl();
        if (wasmUrl == null) {
            LOGGER.severe("WASM file not found. Ruffle cannot function without it.");
            //showErrorDialog("WASM Loading Error", "Failed to load Flash emulator.", "Ruffle's WASM file could not be found. Flash content will not be available.");
            return;
        }

        // Escape any '%' characters in the wasmUrl to prevent String.format issues
        String sanitizedWasmUrl = wasmUrl.replace("%", "%%");

        String replacementScript = String.format("""
            (function() {
                if (!window.RufflePlayer) {
                    console.error('RufflePlayer is not available.');
                    return;
                }

                // Configure Ruffle's WASM location
                window.RufflePlayer.config = {
                    wasmLocation: '%s'
                };

                // Initialize Ruffle
                const ruffle = window.RufflePlayer.newest();

                // Replace all Flash object/embed elements
                const flashObjects = document.querySelectorAll('object[data$=".swf"], embed[src$=".swf"]');
                flashObjects.forEach((flashObject) => {
                    const parent = flashObject.parentElement;
                    const rufflePlayer = ruffle.createPlayer();
                    rufflePlayer.style.width = flashObject.width || '100%';
                    rufflePlayer.style.height = flashObject.height || '100%';
                    parent.replaceChild(rufflePlayer, flashObject);
                    rufflePlayer.load(flashObject.data || flashObject.src);
                });
            })();
            """, sanitizedWasmUrl);

        Platform.runLater(() -> {
            try {
                webEngine.executeScript(replacementScript);
                LOGGER.info("Flash replacement script injected successfully.");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error injecting Flash replacement script: " + e.getMessage(), e);
                //showErrorDialog("Script Injection Error", "Failed to inject Flash replacement script.", "An error occurred while trying to replace Flash content with Ruffle.");
            }
        });
    }

    /**
     * Retrieves the URL of the Ruffle WASM file.
     *
     * @return The URL of the Ruffle WASM file as a String, or null if not found.
     */
    private String getWasmUrl() {
        try {
            return getClass().getResource(RUFFLE_WASM_RESOURCE_PATH).toExternalForm();
        } catch (NullPointerException e) {
            System.err.println("WASM file not found at path: " + RUFFLE_WASM_RESOURCE_PATH);
            return null;
        }
    }
}
