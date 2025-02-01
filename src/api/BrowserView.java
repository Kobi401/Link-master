package api;

import api.Flash.FlashHandler;
import api.Managers.ConfigManager;
import api.Managers.TabManager;
import api.download.DownloadTask;
import api.injection.JSInjectionSystem;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import ui.JavaScriptUIInjector;
import ui.SearchBar;
import ui.StatusBar;
import ui.bookmark.BookmarkBar;

import java.awt.*;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class BrowserView {

    //private SearchBar searchBar;
    //private BookmarkBar bookmarkBar;
    //private StatusBar statusBar;
    private WebView browserArea;
    private static WebEngine webEngine;
    //private BorderPane mainLayout;
    private MenuButton mainMenuButton;

    private ConfigManager configManager;
    private TabManager tabManager;
    private FlashHandler flashHandler;
    private JSInjectionSystem injector;

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?|file|ftp|link)://[^\\s/$.?#].[^\\s]*$", Pattern.CASE_INSENSITIVE);

    //"awesome" easter egg
    private String typedKeys = "";

    private VBox downloadOverlay;
    private List<DownloadTask> activeDownloads = new ArrayList<>();

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
        /*mainLayout.setOnKeyTyped(event -> {
            typedKeys += event.getCharacter();
            if (typedKeys.toLowerCase().contains("awesome")) {
                typedKeys = "";
                statusBar.setLoadingBarStyle("rainbow");
                System.out.println("Easter Egg: Rainbow loading bar activated!");
            }
        });*/
    }

    private void initializeComponents() {
        //searchBar = new SearchBar();
        //bookmarkBar = new BookmarkBar(this);
        //statusBar = new StatusBar();

        //searchBar.getBackButton().setOnAction(e -> goBack());
        //searchBar.getForwardButton().setOnAction(e -> goForward());
        //searchBar.getRefreshButton().setOnAction(e -> refreshPage());

        configManager = new ConfigManager();
        flashHandler = new FlashHandler(configManager.isFlashEnabled());

        browserArea = new WebView();
        webEngine = browserArea.getEngine();
        webEngine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) LinkEngine/1.0 LinkBrowser/Prototype rv:1.0 Gecko/20230101 Safari/537.36");
        browserArea.setContextMenuEnabled(false);
        new JavaScriptUIInjector(webEngine);

        //mainLayout = new BorderPane();
        //mainLayout.setTop(setupSearchBarContainer());
        //mainLayout.setTop(setupTopContainer());
        //mainLayout.setCenter(browserArea);
        //mainLayout.setBottom(statusBar.getStatusBarContainer());

        downloadOverlay = new VBox(5);
        downloadOverlay.setAlignment(Pos.TOP_RIGHT);
        downloadOverlay.setPadding(new Insets(10));
        downloadOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4); "
                + "-fx-background-radius: 5;");

        StackPane centerStack = new StackPane(browserArea);
        centerStack.getChildren().add(downloadOverlay);
        StackPane.setAlignment(downloadOverlay, Pos.TOP_RIGHT);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                injectContextMenuHandler();
            }
        });

        webEngine.setOnError(event -> {
            System.out.println("WebEngine error: " + event.getMessage());
        });
        webEngine.setOnAlert(event -> {
            System.out.println("WebEngine alert: " + event.getData());
        });

        //mainLayout.setCenter(centerStack);
    }

    //--------------------------------Context Menu----------------------------------------
    //called after the page loads (on SUCCEEDED)
    private void injectContextMenuHandler() {
        injector = new JSInjectionSystem(webEngine);
        injector.addJavaBridge("javaContext", new ContextMenuBridge());
        String contextMenuScript =
                "document.addEventListener('contextmenu', function(e) {" +
                        "    e.preventDefault();" +
                        "    var element = e.target;" +
                        "    var tagName = element.tagName.toLowerCase();" +
                        "    var src = element.src ? element.src : '';" +
                        "    var href = element.href ? element.href : '';" +
                        "    var x = e.pageX;" +
                        "    var y = e.pageY;" +
                        "    window.javaContext.showContextMenu(tagName, src, href, x, y);" +
                        "}, { capture: true });";
        injector.addScript(contextMenuScript);
    }

    /**
     * The Java bridge that JavaScript calls.
     * Its method is called from JavaScript when a right‐click occurs.
     */
    public class ContextMenuBridge {
        public void showContextMenu(String tagName, String src, String href, double pageX, double pageY) {
            Platform.runLater(() -> {
                System.out.println("JS called showContextMenu with tag=" + tagName);
                ContextMenu menu = buildContextMenu(tagName, src, href);
                Point2D screenCoords = browserArea.localToScreen(pageX, pageY);
                if (screenCoords != null) {
                    menu.show(browserArea, screenCoords.getX(), screenCoords.getY());
                }
            });
        }
    }

    /**
     * Build a context menu based on the clicked element.
     * Note: Do not call menu.show() here. That is done in the bridge.
     */
    private ContextMenu buildContextMenu(String tagName, String src, String href) {
        ContextMenu menu = new ContextMenu();
        MenuItem backItem = new MenuItem("Back");
        backItem.setOnAction(e -> goBack());
        MenuItem forwardItem = new MenuItem("Forward");
        forwardItem.setOnAction(e -> goForward());
        MenuItem refreshItem = new MenuItem("Refresh");
        refreshItem.setOnAction(e -> refreshPage());
        menu.getItems().addAll(backItem, forwardItem, refreshItem);

        if ("img".equalsIgnoreCase(tagName) && !src.isBlank()) {
            MenuItem saveImg = new MenuItem("Save Image As...");
            saveImg.setOnAction(e -> {
                String fileName = extractFileNameFromURL(src);
                startDownload(src, fileName);
            });
            menu.getItems().add(saveImg);
        }

        if ("a".equalsIgnoreCase(tagName) && !href.isBlank()) {
            MenuItem openLink = new MenuItem("Open Link");
            openLink.setOnAction(e -> loadPage(href));

            MenuItem downloadLink = new MenuItem("Download Link");
            downloadLink.setOnAction(e -> {
                String fileName = extractFileNameFromURL(href);
                startDownload(href, fileName);
            });
            menu.getItems().addAll(openLink, downloadLink);
        }
        return menu;
    }
    //-------------------------------- END Context Menu----------------------------------------

    private VBox setupTopContainer() {
        HBox searchBarContainer = setupSearchBarContainer();
        HBox bookmarkBarContainer = setupBookmarkBarContainer();
        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(searchBarContainer, bookmarkBarContainer);
        return topContainer;
    }

    /**
     * Sets up the search bar container to span the entire width.
     *
     * @return HBox containing the search bar.
     */
    private HBox setupSearchBarContainer() {
        /*HBox searchBarContainer = searchBar.getSearchBarContainer();
        HBox.setHgrow(searchBar.getSearchField(), Priority.ALWAYS);
        HBox.setHgrow(searchBarContainer, Priority.ALWAYS);
        searchBarContainer.setPadding(new Insets(2, 10, 2, 10));
        searchBarContainer.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #ccc; -fx-border-radius: 5;");
        */
        return null;
    }

    /**
     * Sets up the bookmark bar container to span the entire width.
     *
     * @return HBox containing the bookmark bar.
     */
    private HBox setupBookmarkBarContainer() {
        /*HBox bookmarkBarContainer = bookmarkBar.getBookmarkBarContainer();

        // Ensure the bookmark bar spans the available width
        HBox.setHgrow(bookmarkBarContainer, Priority.ALWAYS);

        // Set padding and styling
        bookmarkBarContainer.setPadding(new Insets(2, 10, 2, 10));
        bookmarkBarContainer.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-radius: 5;");
*/
        return null;
    }

    /**
     * Escapes single quotes (and backslashes) in the given string for safe JavaScript injection.
     *
     * @param s The string to escape.
     * @return The escaped string.
     */
    private String escapeForJS(String s) {
        if (s == null) {
            return "";
        }
        // Escape backslashes and single quotes.
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }

    private void configureWebEngine() {
        webEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.startsWith("link://open/")) {
                handleCustomUrl(newValue);
                return;
            }
            if (isLikelyDownload(newValue)) {
                String fileName = extractFileNameFromURL(newValue);
                startDownload(newValue, fileName);
                webEngine.getLoadWorker().cancel();
            } else {
                String escapedValue = escapeForJS(newValue);
                webEngine.executeScript("window.updateStatus('Loading: " + escapedValue + "');");
                webEngine.executeScript("document.getElementById('search-bar').value = '" + escapedValue + "';");
            }
        });
    }

    //This is ultra basic, someone needs to make this better
    private boolean isLikelyDownload(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.matches(".*\\.[a-z0-9]{2,5}(\\?.*)?$");
    }

    private String extractFileNameFromURL(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            String filename = Paths.get(path).getFileName().toString();
            if (filename.isBlank()) {
                filename = "unnamed";
            }

            return filename;
        } catch (Exception e) {
            return "unnamed";
        }
    }


    /** Handles page load success */
    private void handlePageLoadSuccess() {
        webEngine.executeScript("document.cookie = 'block=false';"); // Bypass basic blockers for now
        updateStatus("Done", false);
        if (configManager.isFlashEnabled()) {
            flashHandler.injectRuffleScript(webEngine);
        }
    }

    /**
     * Updates the status in the JS UI and shows/hides the loading bar.
     *
     * @param message The status message to display.
     * @param showLoading true to display the loading bar; false to hide it.
     */
    private void updateStatus(String message, boolean showLoading) {
        String escapedMessage = escapeForJS(message);
        webEngine.executeScript("window.updateStatus('" + escapedMessage + "');");
        webEngine.executeScript("if (typeof window.showLoadingBar === 'function') { window.showLoadingBar(" + showLoading + "); }");
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

    private HBox createDownloadBox(DownloadTask task, String fileName, Path destination) {
        Label fileLabel = new Label(fileName);
        fileLabel.setStyle("-fx-text-fill: white;");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(150);
        progressBar.progressProperty().bind(task.progressProperty());

        Label speedLabel = new Label();
        speedLabel.setStyle("-fx-text-fill: white;");

        task.bytesPerSecondProperty().addListener((obs, oldVal, newVal) -> {
            long bps = newVal.longValue();
            String speedString = formatSpeed(bps);
            speedLabel.setText(speedString + "/s");
        });

        task.setOnSucceeded(evt -> {
            downloadOverlay.getChildren().removeIf(node -> node == speedLabel.getParent());
        });
        task.setOnFailed(evt -> {
            downloadOverlay.getChildren().removeIf(node -> node == speedLabel.getParent());
        });
        task.setOnCancelled(evt -> {
            downloadOverlay.getChildren().removeIf(node -> node == speedLabel.getParent());
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> task.cancel(true));

        HBox hBox = new HBox(5, fileLabel, progressBar, speedLabel, cancelBtn);
        hBox.setAlignment(Pos.CENTER_LEFT);
        return hBox;
    }

    private String formatSpeed(long bytesPerSecond) {
        double kbps = bytesPerSecond / 1024.0;
        if (kbps < 1024) {
            return String.format("%.2f KB", kbps);
        } else {
            double mbps = kbps / 1024.0;
            return String.format("%.2f MB", mbps);
        }
    }

    private void startDownload(String fileURL, String fileName) {
        Path destination = Paths.get(System.getProperty("user.home"), "Downloads", fileName);
        DownloadTask task = new DownloadTask(fileURL, destination);
        activeDownloads.add(task);
        HBox downloadBox = createDownloadBox(task, fileName, destination);
        downloadOverlay.getChildren().add(downloadBox);
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    /**
     * Loads the specified page. Special pages (AboutPage, SettingsPage) are loaded
     * from local resources; otherwise, the URL is normalized and loaded.
     * The status message is updated via the JavaScript UI.
     *
     * @param url The URL or special page identifier to load.
     */
    public void loadPage(String url) {
        String statusMessage;
        if (url.startsWith("Link/AboutPage")) {
            String aboutUrl = getClass().getResource("/AboutPage.html").toExternalForm();
            webEngine.load(aboutUrl);
            statusMessage = "Loading: " + aboutUrl;
        } else if (url.startsWith("Link/SettingsPage")) {
            String settingsUrl = getClass().getResource("/SettingsPage.html").toExternalForm();
            webEngine.load(settingsUrl);
            statusMessage = "Loading: " + settingsUrl;
        } else {
            url = normalizeUrl(url);
            webEngine.load(url);
            statusMessage = "Loading: " + url;
        }

        // Safely update the JS UI status bar.
        webEngine.executeScript(
                "if (typeof window.updateStatus === 'function') { " +
                        "window.updateStatus('" + escapeForJS(statusMessage) + "'); }"
        );
    }

    /**
     * Goes back to the previous page in the WebEngine history and updates the status.
     */
    private void goBack() {
        if (webEngine.getHistory().getCurrentIndex() > 0) {
            webEngine.getHistory().go(-1);
            webEngine.executeScript("window.updateStatus('Going back...');");
        }
    }

    /**
     * Goes forward to the next page in the WebEngine history and updates the status.
     */
    private void goForward() {
        if (webEngine.getHistory().getCurrentIndex() < webEngine.getHistory().getEntries().size() - 1) {
            webEngine.getHistory().go(1);
            webEngine.executeScript("window.updateStatus('Going forward...');");
        }
    }

    /**
     * Refreshes the current page in the WebEngine and updates the status.
     */
    private void refreshPage() {
        webEngine.reload();
        webEngine.executeScript("window.updateStatus('Refreshing page...');");
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

        //searchBarWithButtons.getChildren().addAll(searchBar.getSearchBarContainer(), spacer, mainMenuButton);
        layout.setTop(searchBarWithButtons);
        layout.setCenter(browserArea);
        //layout.setBottom(statusBar.getStatusBarContainer());
        return layout;
    }

    public WebView getBrowserArea() {
        return browserArea;
    }
}