import api.BrowserView;
import api.Managers.TabManager;
import api.plugins.PluginManager;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

//git commit -m "first commit"
//git add .
//git branch -M main
//git remote add origin https://github.com/Kobi401/Link-master.git
//git push -u origin main

//##POSSIBLE IDEAS##
//we could do an undecorated frame and do our own window frame?
//do we want to keep javaFX webView or make our own engine?
//how about making a Windows XP - 8.1 Build?
//Encryption for user data.
//Maybe Full customizability to the UI if the user wants? (Move UI elements to where ever, Colors, etc)
//Proxy service so we can bypass SESAC or whatever
//how are we doing versioning? (Link 1.0 or what?)

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

//For users that want to run this in an ide
//--module-path "/home/kobi401/IdeaProjects/Link-master/JavaFXLinux/lib" --add-modules javafx.controls,javafx.fxml,javafx.web -Dprism.verbose=true -Dprism.order=es2 -Dprism.forceGPU=true -Djavafx.platform=gtk -Djava.library.path="/home/kobi401/IdeaProjects/Link-master/JavaFXLinux/lib"

public class LinkBrowser extends Application {

    private Stage splashStage;
    private Label pluginStatusLabel;
    private PluginManager pluginManager;
    private String buildType;
    public String detectedOS;

    @Override
    public void start(Stage primaryStage) {
        System.setProperty("prism.maxvram", "8G");

        buildType = System.getProperty("build.type", "STABLE").toUpperCase(Locale.ROOT);
        showSplashScreen();
        Duration minSplashDuration = Duration.seconds(3);

        CompletableFuture<Void> pluginLoadingFuture = CompletableFuture.runAsync(() -> {
            pluginManager = new PluginManager(getEngine(), message -> {
                Platform.runLater(() -> {
                    pluginStatusLabel.setText(message);
                    System.out.println(message);
                });
            });

            pluginManager.loadPlugins();
        });

        CompletableFuture<Void> splashDurationFuture = new CompletableFuture<>();
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(minSplashDuration);
        pause.setOnFinished(event -> splashDurationFuture.complete(null));
        pause.play();

        CompletableFuture.allOf(pluginLoadingFuture, splashDurationFuture).thenRun(() -> {
            Platform.runLater(() -> {
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), splashStage.getScene().getRoot());
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    splashStage.close();
                    showMainStage(primaryStage);
                });
                fadeOut.play();
            });
        });
    }

    private void showSplashScreen() {
        splashStage = new Stage();
        splashStage.initStyle(StageStyle.TRANSPARENT);

        StackPane root = new StackPane();
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #ffffff, #e0e0e0);" +
                        "-fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 10, 0, 0, 2);"
        );

        VBox splashContent = new VBox(20);
        splashContent.setAlignment(Pos.CENTER);
        splashContent.setPadding(new Insets(30));

        ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("Images/LinkLogo_Big.png")));
        logo.setFitWidth(120);
        logo.setPreserveRatio(true);

        Label welcomeLabel = new Label("Welcome to Link");
        welcomeLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        String osName = System.getProperty("os.name");
        this.detectedOS = osName;

        Label versionLabel = new Label("Version 1.3-" + osName + " (" + buildType + ")");
        versionLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #555555;");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressIndicator.setPrefSize(50, 50);

        pluginStatusLabel = new Label("Loading plugins...");
        pluginStatusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");

        Label devLabel = new Label("Developed by Kobi401");
        devLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");

        splashContent.getChildren().addAll(logo, welcomeLabel, versionLabel, progressIndicator, pluginStatusLabel, devLabel);
        root.getChildren().add(splashContent);

        Scene splashScene = new Scene(root, 400, 500);
        splashScene.setFill(null);

        splashStage.setScene(splashScene);
        splashStage.setTitle("Loading...");
        splashStage.show();

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    /**
     * Set up and display main browser UI.
     */
    private void showMainStage(Stage primaryStage) {
        TabManager tabManager = new TabManager();

        BrowserView initialView = new BrowserView(tabManager);
        tabManager.createNewTab("Home", initialView);

        BorderPane root = new BorderPane();
        root.setCenter(tabManager.getTabPane());

        Scene scene = new Scene(root, 1024, 768);
        primaryStage.setTitle("Link (Linux)");
        primaryStage.setScene(scene);

        primaryStage.show();
        optimizeUI(primaryStage);
    }

    /**
     * Retrieves the WebEngine instance used by the browser.
     *
     * @return The WebEngine instance.
     */
    private WebEngine getEngine() {
        return BrowserView.getWebEngine();
    }

    private void optimizeUI(Stage stage) {
        stage.setResizable(true);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.widthProperty().addListener((obs, oldVal, newVal) -> System.gc());
        stage.heightProperty().addListener((obs, oldVal, newVal) -> System.gc());
    }

    public static void main(String[] args) {
        launch(args);
    }
}


