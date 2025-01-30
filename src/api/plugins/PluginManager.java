package api.plugins;

import javafx.scene.web.WebEngine;

import org.kobi401.Plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * PluginManager handles the loading, initialization, and management of browser plugins.
 */
public class PluginManager {
    private static final String PLUGINS_DIR = System.getProperty("user.home") + File.separator + "LinkBrowser" + File.separator + "plugins";
    private List<Plugin> loadedPlugins;
    private WebEngine webEngine;
    private Consumer<String> messageConsumer;

    /**
     * Constructor for PluginManager.
     *
     * @param webEngine The WebEngine instance of the browser.
     */
    public PluginManager(WebEngine webEngine, Consumer<String> messageConsumer) {
        this.webEngine = webEngine;
        this.loadedPlugins = new ArrayList<>();
        this.messageConsumer = messageConsumer;
        loadPlugins();
    }

    /**
     * Loads all plugins from the plugins directory.
     */
    public void loadPlugins() {
        File pluginsDir = new File(PLUGINS_DIR);
        if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
            messageConsumer.accept("Plugins directory does not exist. Creating at: " + PLUGINS_DIR);
            boolean created = pluginsDir.mkdirs();
            if (!created) {
                messageConsumer.accept("Failed to create plugins directory.");
                return;
            }
        }

        File[] jarFiles = pluginsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            messageConsumer.accept("No plugins found in the plugins directory.");
            return;
        }

        messageConsumer.accept("Found " + jarFiles.length + " plugin(s). Loading...");

        for (File jar : jarFiles) {
            messageConsumer.accept("Loading plugin from: " + jar.getName());
            try {
                loadPluginFromJar(jar);
            } catch (IOException e) {
                messageConsumer.accept("Failed to load plugin from " + jar.getName() + ": " + e.getMessage());
            }
        }

        messageConsumer.accept("All plugins loaded successfully.");
    }

    /**
     * Loads a plugin from a JAR file.
     *
     * @param jarFile The JAR file containing the plugin.
     * @throws IOException If an I/O error occurs.
     */
    private void loadPluginFromJar(File jarFile) throws IOException {
        JarFile jar = new JarFile(jarFile);
        Enumeration<JarEntry> entries = jar.entries();

        URL[] urls = { new URL("jar:file:" + jarFile.getAbsolutePath() + "!/") };
        try (URLClassLoader cl = URLClassLoader.newInstance(urls)) {
            while (entries.hasMoreElements()) {
                JarEntry je = entries.nextElement();
                if(je.isDirectory() || !je.getName().endsWith(".class")){
                    continue;
                }
                // -6 because of .class
                String className = je.getName().substring(0, je.getName().length()-6);
                className = className.replace('/', '.');
                try {
                    Class<?> c = cl.loadClass(className);
                    if (Plugin.class.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers())) {
                        Plugin plugin = (Plugin) c.getDeclaredConstructor().newInstance();
                        plugin.initialize(webEngine);
                        loadedPlugins.add(plugin);
                        System.out.println("Loaded plugin: " + plugin.getName() + " v" + plugin.getVersion());
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    System.err.println("Class not found: " + className);
                } catch (Exception e) {
                    System.err.println("Failed to instantiate plugin class: " + className + " - " + e.getMessage());
                }
            }
        } catch (MalformedURLException e) {
            System.err.println("Malformed URL for JAR file: " + jarFile.getName());
        }
    }

    /**
     * Shuts down all loaded plugins.
     */
    public void shutdownPlugins() {
        for (Plugin plugin : loadedPlugins) {
            try {
                plugin.shutdown();
                System.out.println("Shutdown plugin: " + plugin.getName());
            } catch (Exception e) {
                System.err.println("Error shutting down plugin " + plugin.getName() + ": " + e.getMessage());
            }
        }
        loadedPlugins.clear();
    }

    /**
     * Returns the list of loaded plugins.
     *
     * @return List of loaded plugins.
     */
    public List<Plugin> getLoadedPlugins() {
        return loadedPlugins;
    }
}
