package api.Managers;

import api.BrowserView;
import javafx.application.Platform;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class TabManager {

    private TabPane tabPane;
    private Tab addTab;

    private Map<Tab, TabMemoryManager> tabMemoryManagers;
    private static final long MEMORY_UPDATE_INTERVAL_MS = 2000;

    public TabManager() {
        tabPane = new TabPane();
        tabMemoryManagers = new HashMap<>();
        createAddTabButton();
    }

    private void createAddTabButton() {
        addTab = new Tab();

        Button addButton = new Button("+");
        addButton.setOnAction(e -> createNewTab("New Tab", new BrowserView(this)));

        HBox buttonContainer = new HBox(addButton);
        addTab.setGraphic(buttonContainer);
        addTab.setClosable(false);

        tabPane.getTabs().add(addTab);
    }

    public Tab createNewTab(String title, BrowserView browserView) {
        Tab tab = new Tab(title);
        tab.setContent(browserView.createBrowserLayout());

        TabMemoryManager tabMemoryManager = new TabMemoryManager(tab);
        tabMemoryManagers.put(tab, tabMemoryManager);
        updateTabMemoryUsage(tab, tabMemoryManager, title);

        tabPane.getTabs().add(tabPane.getTabs().size() - 1, tab);
        tabPane.getSelectionModel().select(tab);

        return tab;
    }

    private void updateTabMemoryUsage(Tab tab, TabMemoryManager tabMemoryManager, String baseTitle) {
        Timer memoryUpdateTimer = new Timer(true);  // Daemon thread to update memory usage
        memoryUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                tabMemoryManager.updateMemoryUsage();

                String updatedTitle = baseTitle + " (Memory: " + tabMemoryManager.getFormattedMemoryUsage() + ")";
                Platform.runLater(() -> tab.setText(updatedTitle));
            }
        }, 0, MEMORY_UPDATE_INTERVAL_MS);
    }

    public void createHtmlTab(String title, String htmlUrl) {
        BrowserView browserView = new BrowserView(this);
        browserView.loadPage(htmlUrl);
        createNewTab(title, browserView);
    }

    public TabPane getTabPane() {
        return tabPane;
    }
}
