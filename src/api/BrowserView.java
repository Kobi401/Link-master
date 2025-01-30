package api;

import api.Flash.FlashHandler;
import api.Managers.ConfigManager;
import api.Managers.TabManager;
import api.plugins.PluginManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import ui.SearchBar;
import ui.StatusBar;
import ui.bookmark.BookmarkBar;

import java.util.List;
import java.util.regex.Pattern;

import org.kobi401.Plugin;

public class BrowserView {

    private SearchBar searchBar;
    private BookmarkBar bookmarkBar;
    private StatusBar statusBar;
    private WebView browserArea;
    private static WebEngine webEngine;
    private BorderPane mainLayout;
    private MenuButton mainMenuButton;

    private ConfigManager configManager;
    private TabManager tabManager;
    private FlashHandler flashHandler;
    private PluginManager pluginManager;

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?|file|ftp|link)://[^\\s/$.?#].[^\\s]*$", Pattern.CASE_INSENSITIVE);

    //"awesome" easter egg
    private String typedKeys = "";

    public BrowserView(TabManager tabManager) {
        this.tabManager = tabManager;
        initializeComponents();
        configureWebEngine();
        createMainMenuButton();
        createEventHandlers();
        loadPage("https://www.google.com");
    }

    public static WebEngine getWebEngine() {
        return webEngine;
    }

    /**
     * Creates event handlers for detecting the "awesome" Easter Egg.
     */
    private void createEventHandlers() {
        mainLayout.setOnKeyTyped(event -> {
            typedKeys += event.getCharacter();
            if (typedKeys.toLowerCase().contains("awesome")) {
                typedKeys = "";
                statusBar.setLoadingBarStyle("rainbow");
                System.out.println("Easter Egg: Rainbow loading bar activated!");
            }
        });
    }

    private void initializeComponents() {
        searchBar = new SearchBar();
        bookmarkBar = new BookmarkBar(this);
        statusBar = new StatusBar();

        searchBar.getBackButton().setOnAction(e -> goBack());
        searchBar.getForwardButton().setOnAction(e -> goForward());
        searchBar.getRefreshButton().setOnAction(e -> refreshPage());

        configManager = new ConfigManager();
        flashHandler = new FlashHandler(configManager.isFlashEnabled());

        browserArea = new WebView();
        webEngine = browserArea.getEngine();
        webEngine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) LinkEngine/1.0 LinkBrowser/Prototype rv:1.0 Gecko/20230101 Safari/537.36");

        mainLayout = new BorderPane();
        mainLayout.setTop(setupSearchBarContainer());
        mainLayout.setCenter(browserArea);
        mainLayout.setBottom(statusBar.getStatusBarContainer());
    }

    /**
     * Get the list of loaded plugins.
     */
    public void getLoadedPlugins() {
        List<Plugin> plugins = pluginManager.getLoadedPlugins();
        // Convert the list to a JavaScript-friendly format (e.g., JSON)
        StringBuilder pluginsJson = new StringBuilder("[");
        for (int i = 0; i < plugins.size(); i++) {
            Plugin plugin = plugins.get(i);
            pluginsJson.append("{")
                    .append("\"name\":\"").append(plugin.getName()).append("\",")
                    .append("\"version\":\"").append(plugin.getVersion()).append("\",")
                    .append("\"description\":\"").append(plugin.getDescription()).append("\",")
                    .append("\"enabled\":").append("null").append("}");
            if (i < plugins.size() - 1) {
                pluginsJson.append(",");
            }
        }
        pluginsJson.append("]");
        // Call the JavaScript function to display plugins
        Platform.runLater(() -> webEngine.executeScript("onPluginListReceived(" + pluginsJson.toString() + ");"));
    }

    /**
     * Toggle a specific plugin's enabled state.
     */
    public void togglePlugin(String pluginName, boolean isEnabled) {
        //pluginManager.setPluginEnabled(pluginName, isEnabled);
        System.out.println("Plugin " + pluginName + " enabled: " + isEnabled);
    }

    /**
     * Uninstall a specific plugin.
     */
    public void uninstallPlugin(String pluginName) {
        //pluginManager.uninstallPlugin(pluginName);
        System.out.println("Plugin " + pluginName + " uninstalled.");
        // Refresh the plugin list after uninstallation
        getLoadedPlugins();
    }

    /**
     * Sets up the search bar container to span the entire width.
     *
     * @return HBox containing the search bar.
     */
    private HBox setupSearchBarContainer() {
        HBox searchBarContainer = searchBar.getSearchBarContainer();

        HBox.setHgrow(searchBar.getSearchField(), Priority.ALWAYS);
        HBox.setHgrow(searchBarContainer, Priority.ALWAYS);

        searchBarContainer.setPadding(new Insets(2, 10, 2, 10));
        searchBarContainer.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #ccc; -fx-border-radius: 5;");

        return searchBarContainer;
    }

    private void configureWebEngine() {
        webEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.startsWith("link://open/")) {
                handleCustomUrl(newValue);
            } else {
                statusBar.setStatus("Loading: " + newValue);
                searchBar.getSearchField().setText(newValue);
            }
        });

        webEngine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            switch (newState) {
                case SCHEDULED -> updateStatus("Loading...", true);
                case RUNNING -> statusBar.setStatus("Running...");
                case SUCCEEDED -> handlePageLoadSuccess();
                case FAILED -> updateStatus("Failed to load the page", false);
                case CANCELLED -> updateStatus("Loading cancelled", false);
            }
        });

        searchBar.getSearchField().setOnAction(e -> loadPage(searchBar.getSearchField().getText()));
    }

    /** Handles page load success */
    private void handlePageLoadSuccess() {
        webEngine.executeScript("document.cookie = 'block=false';"); // Bypass basic blockers for now
        updateStatus("Done", false);
        if (configManager.isFlashEnabled()) {
            flashHandler.injectRuffleScript(webEngine);
        }
    }

    /** Updates the status and shows/hides the loading bar */
    private void updateStatus(String message, boolean showLoading) {
        statusBar.setStatus(message);
        statusBar.showLoadingBar(showLoading);
    }

    /**
     * Creates the main menu button and populates its menu items.
     */
    private void createMainMenuButton() {
        MenuItem aboutItem = new MenuItem("About Link");
        aboutItem.setOnAction(e -> loadAboutPage());

        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.setOnAction(e -> loadSettingsPage());

        MenuItem refreshItem = new MenuItem("Refresh");
        refreshItem.setOnAction(e -> webEngine.reload());

        MenuItem backItem = new MenuItem("Back");
        backItem.setOnAction(e -> navigateHistory(-1));

        MenuItem forwardItem = new MenuItem("Forward");
        forwardItem.setOnAction(e -> navigateHistory(1));

        mainMenuButton = new MenuButton("≡");
        mainMenuButton.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        mainMenuButton.getItems().addAll(refreshItem, backItem, forwardItem, new SeparatorMenuItem(), aboutItem, settingsItem);
    }

    private void navigateHistory(int direction) {
        int currentIndex = webEngine.getHistory().getCurrentIndex();
        int newIndex = currentIndex + direction;

        if (newIndex >= 0 && newIndex < webEngine.getHistory().getEntries().size()) {
            webEngine.getHistory().go(direction);
        }
    }

    public void loadPage() {
        String url = searchBar.getSearchField().getText();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        webEngine.load(url);
        statusBar.setStatus("Loading: " + url);
    }

    public void loadPage(String url) {
        if (url.startsWith("Link/AboutPage")) {
            webEngine.load(getClass().getResource("/AboutPage.html").toExternalForm());
        } else if (url.startsWith("Link/SettingsPage")) {
            webEngine.load(getClass().getResource("/SettingsPage.html").toExternalForm());
        } else {
            url = normalizeUrl(url);
            webEngine.load(url);
        }
        statusBar.setStatus("Loading: " + url);
    }

    /**
     * Goes back to the previous page in the WebEngine history.
     */
    private void goBack() {
        if (webEngine.getHistory().getCurrentIndex() > 0) {
            webEngine.getHistory().go(-1);
            statusBar.setStatus("Going back...");
        }
    }

    /**
     * Goes forward to the next page in the WebEngine history.
     */
    private void goForward() {
        if (webEngine.getHistory().getCurrentIndex() < webEngine.getHistory().getEntries().size() - 1) {
            webEngine.getHistory().go(1);
            statusBar.setStatus("Going forward...");
        }
    }

    /**
     * Refreshes the current page in the WebEngine.
     */
    private void refreshPage() {
        webEngine.reload();
        statusBar.setStatus("Refreshing page...");
    }

    /** Normalizes URL for consistent loading */
    private String normalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) return "about:blank";
        return URL_PATTERN.matcher(url).matches() ? url : "http://" + url;
    }

    private void loadAboutPage() {
        String aboutUrl = "Link/AboutPage.html";
        tabManager.createHtmlTab("About Link", aboutUrl);
    }

    private void loadSettingsPage() {
        String settingsUrl = "Link/SettingsPage.html";
        tabManager.createHtmlTab("Settings", settingsUrl);
    }

    private void handleCustomUrl(String url) {
        switch (url) {
            case "link://open/github" -> tabManager.createHtmlTab("GitHub", "https://github.com/Kobi401/Link");
            case "link://open/settings" -> loadSettingsPage();
            case "link://open/about" -> loadAboutPage();
            case "link://settings/flash/on" -> {
                configManager.setFlashEnabled(true);
                flashHandler.setFlashEnabled(true);
                System.out.println("Flash enabled.");
            }
            case "link://settings/flash/off" -> {
                configManager.setFlashEnabled(false);
                flashHandler.setFlashEnabled(false);
                System.out.println("Flash disabled.");
            }
            default -> System.out.println("Unhandled URL: " + url);
        }
    }

    /** Create a browser layout for the UI */
    public BorderPane createBrowserLayout() {
        BorderPane layout = new BorderPane();

        HBox searchBarWithButtons = new HBox(5);
        searchBarWithButtons.setPadding(new Insets(5, 5, 5, 5));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchBarWithButtons.getChildren().addAll(searchBar.getSearchBarContainer(), spacer, mainMenuButton);
        layout.setTop(searchBarWithButtons);
        layout.setCenter(browserArea);
        layout.setBottom(statusBar.getStatusBarContainer());
        return layout;
    }

    public WebView getBrowserArea() {
        return browserArea;
    }
}