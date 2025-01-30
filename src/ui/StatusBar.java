package ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class StatusBar {

    private Label statusLabel;
    private ProgressBar loadingBar;
    private HBox statusBarContainer;
    private String currentLoadingBarStyle;

    public StatusBar() {
        statusLabel = new Label("Ready");
        statusLabel.setPadding(new Insets(3, 5, 3, 5));

        loadingBar = new ProgressBar();
        loadingBar.setPrefWidth(150);
        loadingBar.setVisible(false);

        setLoadingBarStyle("red");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusBarContainer = new HBox(5);
        statusBarContainer.setPadding(new Insets(2, 5, 2, 5));
        statusBarContainer.getChildren().addAll(statusLabel, spacer, loadingBar);

        statusBarContainer.setStyle("-fx-background-color: #e5e5e5; "
                + "-fx-border-color: #7f9db9; -fx-border-width: 1; "
                + "-fx-padding: 2; -fx-font-size: 11px; -fx-font-family: Tahoma;");
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void showLoadingBar(boolean show) {
        loadingBar.setVisible(show);
    }

    public void setLoadingProgress(double progress) {
        loadingBar.setProgress(progress);
    }

    public void setLoadingBarStyle(String style) {
        switch (style.toLowerCase()) {
            case "blue":
                loadingBar.setStyle("-fx-accent: #0a64ad; -fx-control-inner-background: #e0e7ef;");
                break;
            case "green":
                loadingBar.setStyle("-fx-accent: #28a745; -fx-control-inner-background: #e0f7e7;");
                break;
            case "red":
                loadingBar.setStyle("-fx-accent: #dc3545; -fx-control-inner-background: #f8e6e6;");
                break;
            case "default":
            default:
                loadingBar.setStyle("-fx-accent: #0a64ad; -fx-control-inner-background: #c3c7c9;");
                break;
        }
        currentLoadingBarStyle = style;
    }

    public HBox getStatusBarContainer() {
        return statusBarContainer;
    }

    public String getCurrentLoadingBarStyle() {
        return currentLoadingBarStyle;
    }
}
