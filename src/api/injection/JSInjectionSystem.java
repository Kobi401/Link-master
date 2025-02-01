package api.injection;

import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;
import java.util.ArrayList;
import java.util.List;

public class JSInjectionSystem {
    private final WebEngine webEngine;
    private final List<String> scripts = new ArrayList<>();
    private final List<JavaBridge> bridges = new ArrayList<>();

    //a small helper class for storing bridge info.
    public static class JavaBridge {
        public final String name;
        public final Object object;
        public JavaBridge(String name, Object object) {
            this.name = name;
            this.object = object;
        }
    }

    public JSInjectionSystem(WebEngine engine) {
        this.webEngine = engine;
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                //when a new page loads, re‑inject bridges and scripts.
                injectBridges();
                injectAllScripts();
            }
        });
    }

    /** Adds a JavaScript snippet to be injected. */
    public void addScript(String script) {
        scripts.add(script);
        //if the document is already loaded, inject immediately.
        if (webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            webEngine.executeScript(script);
        }
    }

    /** Adds a Java bridge to be exposed as window.[name]. */
    public void addJavaBridge(String name, Object bridge) {
        bridges.add(new JavaBridge(name, bridge));
        //if the document is already loaded, add it immediately.
        if (webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            JSObject window = (JSObject) webEngine.executeScript("window");
            window.setMember(name, bridge);
        }
    }

    //called on every successful page load.
    private void injectBridges() {
        JSObject window = (JSObject) webEngine.executeScript("window");
        for (JavaBridge bridge : bridges) {
            window.setMember(bridge.name, bridge.object);
        }
    }

    /** Injects all registered scripts. */
    private void injectAllScripts() {
        for (String script : scripts) {
            webEngine.executeScript(script);
        }
    }
}


