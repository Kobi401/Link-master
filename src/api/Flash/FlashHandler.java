package api.Flash;

import javafx.scene.web.WebEngine;

import java.net.URL;

public class FlashHandler {

    private static final String RUFFLE_WASM_RESOURCE_PATH = "/ruffle/7ba5efac283c4a200e5d.wasm";
    private boolean isFlashEnabled;

    public FlashHandler(boolean isFlashEnabled) {
        this.isFlashEnabled = isFlashEnabled;
    }

    public void setFlashEnabled(boolean isFlashEnabled) {
        this.isFlashEnabled = isFlashEnabled;
    }

    public boolean isFlashEnabled() {
        return isFlashEnabled;
    }

    public void injectRuffleScript(WebEngine webEngine) {
        if (!isFlashEnabled) {
            System.out.println("Flash is disabled. Skipping Ruffle injection.");
            return;
        }

        URL wasmUrl = getClass().getResource(RUFFLE_WASM_RESOURCE_PATH);
        if (wasmUrl == null) {
            System.err.println("Ruffle .wasm file not found! Ensure the path is correct.");
            return;
        }

        System.out.println("WASM Path: " + wasmUrl.toExternalForm());
        String wasmLocation = wasmUrl.toExternalForm();

        String script = """
            (function() {
                if (!window.RufflePlayer) return;

                // Set a custom path for Ruffle's WASM
                const ruffle = window.RufflePlayer.newest();
                ruffle.config = {
                    wasmLocation: '%s'
                };

                const flashObjects = document.querySelectorAll('object[data$=".swf"], embed[src$=".swf"]');
                flashObjects.forEach((flashObject) => {
                    const parent = flashObject.parentElement;

                    const player = ruffle.createPlayer();
                    parent.replaceChild(player, flashObject);
                    player.load(flashObject.data || flashObject.src);
                });
            })();
        """.formatted(wasmLocation);

        webEngine.executeScript(script);
    }
}