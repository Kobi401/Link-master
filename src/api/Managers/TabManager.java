package api.Managers;

import api.BrowserView;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.animation.TranslateTransition;
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
    private VBox memoryUsagePanel;
    private boolean isPanelVisible = false;

    public TabManager() {
        tabPane = new TabPane();
        tabMemoryManagers = new HashMap<>();
        createAddTabButton();
        setupMemoryUsagePanel();
    }

    private void createAddTabButton() {
        addTab = new Tab();
        Button addButton = new Button("+");
        addButton.setOnAction(e -> createNewTab("New Tab", new BrowserView(this)));
        HBox buttonContainer = new HBox(addButton);
        buttonContainer.setAlignment(Pos.CENTER);
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

        Platform.runLater(() -> updateMemoryUsagePanel());
        return tab;
    }

    public void createHtmlTab(String title, String htmlUrl) {
        BrowserView browserView = new BrowserView(this);
        browserView.loadPage(htmlUrl);
        createNewTab(title, browserView);
    }

    private void updateTabMemoryUsage(Tab tab, TabMemoryManager tabMemoryManager, String baseTitle) {
        Timer memoryUpdateTimer = new Timer(true);
        memoryUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                tabMemoryManager.updateMemoryUsage();
                String updatedTitle = baseTitle + " (Mem: " + tabMemoryManager.getFormattedMemoryUsage() + ")";
                Platform.runLater(() -> {
                    tab.setText(updatedTitle);
                    updateMemoryUsagePanel();
                });
            }
        }, 0, MEMORY_UPDATE_INTERVAL_MS);
    }

    public TabPane getTabPane() {
        return tabPane;
    }

    private void setupMemoryUsagePanel() {
        memoryUsagePanel = new VBox(5);
        memoryUsagePanel.setId("memoryUsagePanel");
        memoryUsagePanel.setAlignment(Pos.CENTER_LEFT);
        memoryUsagePanel.setStyle("-fx-background-color: rgba(34,34,34,0.9); -fx-padding: 10px; -fx-background-radius: 5;");
        memoryUsagePanel.setPrefWidth(300);
        memoryUsagePanel.setTranslateY(-memoryUsagePanel.getPrefHeight());

        tabPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    Pane root = (Pane) newScene.getRoot();
                    root.getChildren().add(memoryUsagePanel);
                    memoryUsagePanel.setLayoutX((root.getWidth() - 300) / 2);
                    memoryUsagePanel.setLayoutY(0);
                    addMouseListenerForMemoryPanel(newScene);
                });
            }
        });
    }

    private void updateMemoryUsagePanel() {
        if (memoryUsagePanel == null) return;
        memoryUsagePanel.getChildren().clear();

        for (Tab tab : tabPane.getTabs()) {
            if (tab == addTab) continue;
            TabMemoryManager tmm = tabMemoryManagers.get(tab);
            if (tmm != null) {
                HBox hbox = new HBox(10);
                hbox.setAlignment(Pos.CENTER_LEFT);

                Label tabTitle = new Label(tab.getText());
                tabTitle.setStyle("-fx-text-fill: #e8eaed; -fx-font-size: 12px;");

                double percentage = 0.0;
                try {
                    String memStr = tmm.getFormattedMemoryUsage().replaceAll("[^\\d.]", "");
                    percentage = Double.parseDouble(memStr) / 1000.0;
                    percentage = Math.min(percentage, 1.0);
                } catch (NumberFormatException ex) {
                    percentage = 0.0;
                }

                ProgressBar memBar = new ProgressBar(percentage);
                memBar.setPrefWidth(100);

                hbox.getChildren().addAll(tabTitle, memBar);
                memoryUsagePanel.getChildren().add(hbox);
            }
        }
    }

    private void addMouseListenerForMemoryPanel(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (event.getSceneY() < 50) {
                slideMemoryPanelIn();
            } else {
                slideMemoryPanelOut();
            }
        });
    }

    private void slideMemoryPanelIn() {
        if (memoryUsagePanel == null || isPanelVisible) return;
        isPanelVisible = true;
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), memoryUsagePanel);
        tt.setToY(0);
        tt.play();
    }

    private void slideMemoryPanelOut() {
        if (memoryUsagePanel == null || !isPanelVisible) return;
        isPanelVisible = false;
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), memoryUsagePanel);
        tt.setToY(-memoryUsagePanel.getHeight());
        tt.play();
    }
}
