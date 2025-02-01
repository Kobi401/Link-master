package api.Flash;

import api.injection.JSInjectionSystem;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FlashHandler {

    private static final String RUFFLE_JS_RESOURCE_PATH = "/ruffle/ruffle.js";
    private static final String RUFFLE_WASM_RESOURCE_PATH = "/ruffle/ruffle.wasm";
    private static final Logger LOGGER = Logger.getLogger(FlashHandler.class.getName());

    private boolean isFlashEnabled;
    private JSInjectionSystem injector; // Our JS injection helper

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
     * Initializes Ruffle injection on the provided WebEngine using JSInjectionSystem.
     * This method adds both Ruffle’s core JavaScript (loaded from a resource)
     * and the Flash replacement script to the injector.
     *
     * @param webEngine The WebEngine instance where Flash content should be handled.
     */
    public void injectRuffleScript(WebEngine webEngine) {
        if (!isFlashEnabled) {
            System.out.println("Flash is disabled. Skipping Ruffle injection.");
            return;
        }

        // Create the JS injection system for the given WebEngine.
        injector = new JSInjectionSystem(webEngine);

        // Load the Ruffle core JavaScript.
        String ruffleJs = loadResourceAsString(RUFFLE_JS_RESOURCE_PATH);
        if (ruffleJs == null) {
            LOGGER.severe("Failed to load Ruffle JavaScript.");
            return;
        }
        injector.addScript(ruffleJs);
        LOGGER.info("Ruffle JavaScript injection added successfully.");

        // Retrieve the URL for the WASM file.
        String wasmUrl = getWasmUrl();
        if (wasmUrl == null) {
            LOGGER.severe("WASM file not found. Ruffle cannot function without it.");
            return;
        }
        // Escape any '%' characters in the URL for String.format.
        String sanitizedWasmUrl = wasmUrl.replace("%", "%%");

        // Build the Flash replacement script.
        // Note: Literal '%' in '100%' must be written as '100%%' so that it prints correctly.
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
                    rufflePlayer.style.width = flashObject.width || '100%%';
                    rufflePlayer.style.height = flashObject.height || '100%%';
                    parent.replaceChild(rufflePlayer, flashObject);
                    rufflePlayer.load(flashObject.data || flashObject.src);
                });
            })();
            """, sanitizedWasmUrl);

        injector.addScript(replacementScript);
        LOGGER.info("Flash replacement injection script added successfully.");
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
            LOGGER.log(Level.SEVERE, "Error loading resource " + resourcePath + ": " + e.getMessage(), e);
            return null;
        }
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
            LOGGER.severe("WASM file not found at path: " + RUFFLE_WASM_RESOURCE_PATH);
            return null;
        }
    }
}
