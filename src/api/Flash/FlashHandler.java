package api.Flash;

import api.injection.JSInjectionSystem;
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

public class FlashHandler {

    private static final String RUFFLE_JS_RESOURCE_PATH = "/ruffle/ruffle.js";
    private static final String RUFFLE_WASM_RESOURCE_PATH = "/ruffle/ruffle.wasm";
    private static final Logger LOGGER = Logger.getLogger(FlashHandler.class.getName());

    private boolean isFlashEnabled;
    private JSInjectionSystem injector; // JS injection helper

    // Additional configuration options for Ruffle
    private boolean debugMode = false;
    private String additionalConfig = "{}"; // A JSON string with additional configuration (if desired)
    private String ruffleContainerId = "ruffle-container"; // ID for an optional container div

    /**
     * Constructor for FlashHandler.
     *
     * @param isFlashEnabled Flag to enable or disable Flash/Ruffle.
     */
    public FlashHandler(boolean isFlashEnabled) {
        this.isFlashEnabled = isFlashEnabled;
    }

    /**
     * Enables or disables Flash/Ruffle.
     *
     * @param isFlashEnabled True to enable, false to disable.
     */
    public void setFlashEnabled(boolean isFlashEnabled) {
        this.isFlashEnabled = isFlashEnabled;
    }

    /**
     * Sets an optional debug mode flag.
     *
     * @param debugMode True to enable debug logging for Ruffle.
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * Sets additional Ruffle configuration as a JSON string.
     *
     * @param additionalConfig Additional configuration (JSON format).
     */
    public void setAdditionalConfig(String additionalConfig) {
        this.additionalConfig = additionalConfig;
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
     * Injects Ruffle scripts into the given WebEngine.
     * The injection is deferred until the WebEngine finishes loading the page.
     *
     * @param webEngine The WebEngine instance where Flash content should be handled.
     */
    public void injectRuffleScript(WebEngine webEngine) {
        if (!isFlashEnabled) {
            LOGGER.info("Flash is disabled. Skipping Ruffle injection.");
            return;
        }

        injector = new JSInjectionSystem(webEngine);

        // Wait for the page to finish loading before injecting Ruffle.
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Load and inject the core Ruffle JavaScript.
                String ruffleJs = loadResourceAsString(RUFFLE_JS_RESOURCE_PATH);
                if (ruffleJs == null) {
                    LOGGER.severe("Failed to load Ruffle JavaScript.");
                    return;
                }
                injector.addScript(ruffleJs);
                LOGGER.info("Ruffle core JavaScript injected successfully.");

                // Retrieve and sanitize the WASM URL.
                String wasmUrl = getWasmUrl();
                if (wasmUrl == null) {
                    LOGGER.severe("WASM file not found. Ruffle cannot function without it.");
                    return;
                }
                String sanitizedWasmUrl = wasmUrl.replace("%", "%%");

                String replacementScript = String.format("""
    (function() {
        if (!window.RufflePlayer) {
            console.error('RufflePlayer is not available.');
            return;
        }
        // Set debug mode if enabled.
        window.RufflePlayer.debug = %b;
        
        // Merge additional configuration (passed as JSON) with the default config.
        var additionalConfig = %s;
        window.RufflePlayer.config = Object.assign({
            wasmLocation: '%s'
        }, additionalConfig);
        
        // Ensure there is a dedicated container for Ruffle if desired.
        var containerId = '%s';
        var container = document.getElementById(containerId);
        if (!container) {
            container = document.createElement('div');
            container.id = containerId;
            // Append container to the beginning of the body.
            document.body.insertBefore(container, document.body.firstChild);
        }
        
        // Initialize Ruffle.
        var ruffle = window.RufflePlayer.newest();
        
        // Find all Flash objects (<object> and <embed> with .swf) and replace them.
        var flashElements = document.querySelectorAll("object[data$='.swf'], embed[src$='.swf']");
        flashElements.forEach(function(element) {
            var parent = element.parentElement;
            if (!parent) return;
            var rufflePlayer = ruffle.createPlayer();
            // Set dimensions (fallback to 100%% if not specified)
            rufflePlayer.style.width = element.width || "100%%";
            rufflePlayer.style.height = element.height || "100%%";
            parent.replaceChild(rufflePlayer, element);
            rufflePlayer.load(element.data || element.src);
        });
        
        console.info("Ruffle flash emulation initialized successfully.");
    })();
    """, debugMode, additionalConfig, sanitizedWasmUrl, ruffleContainerId);

                injector.addScript(replacementScript);
                LOGGER.info("Ruffle replacement script injected successfully.");
            }
        });
    }

    /**
     * Loads a resource file from the classpath as a String.
     *
     * @param resourcePath The path to the resource file.
     * @return The content of the resource as a String, or null if not found.
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
     * @return The URL of the WASM file as a String, or null if not found.
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