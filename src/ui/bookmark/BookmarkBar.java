package ui.bookmark;

import api.BrowserView;
import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

public class BookmarkBar {
    private HBox bookmarkBarContainer;
    private Button addBookmarkButton;
    private Button viewBookmarksButton;
    private List<Bookmark> bookmarks;
    private BrowserView browserView; // Reference to BrowserView to handle navigation

    public BookmarkBar(BrowserView browserView) {
        this.browserView = browserView;
        bookmarks = new ArrayList<>();
        initializeBookmarkBar();
        loadBookmarks();
    }

    private void initializeBookmarkBar() {
        // Initialize HBox with spacing
        bookmarkBarContainer = new HBox(10); // Spacing between nodes
        bookmarkBarContainer.setPadding(new Insets(5, 10, 5, 10));
        bookmarkBarContainer.setAlignment(Pos.CENTER_LEFT);
        bookmarkBarContainer.getStyleClass().add("bookmark-bar"); // Apply CSS style

        // Create bookmark buttons
        addBookmarkButton = new Button("Add Bookmark");
        viewBookmarksButton = new Button("View Bookmarks");

        // Set actions for buttons
        //addBookmarkButton.setOnAction(e -> addBookmark());
        //viewBookmarksButton.setOnAction(e -> viewBookmarks());

        // Add buttons to the HBox
        bookmarkBarContainer.getChildren().addAll(addBookmarkButton, viewBookmarksButton);

        // Allow HBox to grow horizontally within its parent
        HBox.setHgrow(bookmarkBarContainer, Priority.ALWAYS);
    }

    /**
     * Adds a new bookmark to the bar.
     *
     * @param name The display name of the website.
     * @param url  The URL of the website.
     */
    public void addBookmark(String name, String url) {
        Bookmark bookmark = new Bookmark(name, url);
        bookmarks.add(bookmark);
        createBookmarkButton(bookmark);
        BookmarkPersistence.saveBookmarks(bookmarks);
    }

    /**
     * Removes a bookmark from the bar.
     *
     * @param url The URL of the bookmark to remove.
     */
    public void removeBookmark(String url) {
        bookmarks.removeIf(b -> b.getUrl().equals(url));
        refreshBookmarks();
        BookmarkPersistence.saveBookmarks(bookmarks);
    }

    /**
     * Loads bookmarks from persistent storage.
     */
    private void loadBookmarks() {
        List<Bookmark> loadedBookmarks = BookmarkPersistence.loadBookmarks();
        for (Bookmark bookmark : loadedBookmarks) {
            addBookmark(bookmark.getName(), bookmark.getUrl());
        }
    }

    /**
     * Creates a bookmark button with favicon and name.
     *
     * @param bookmark The Bookmark object.
     */
    private void createBookmarkButton(Bookmark bookmark) {
        Button bookmarkButton = new Button();
        bookmarkButton.getStyleClass().add("bookmark-button"); // Apply CSS style
        bookmarkButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        bookmarkButton.setAlignment(Pos.CENTER_LEFT);

        // ImageView for favicon
        ImageView faviconView = new ImageView();
        faviconView.setFitWidth(16);
        faviconView.setFitHeight(16);
        faviconView.setPreserveRatio(true);
        faviconView.setSmooth(true);
        faviconView.setCache(true);

        // Label for bookmark name
        javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(bookmark.getName());
        nameLabel.getStyleClass().add("bookmark-label"); // Apply CSS style
        nameLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 12px;");

        HBox content = new HBox(5, faviconView, nameLabel);
        content.setAlignment(Pos.CENTER_LEFT);
        bookmarkButton.setGraphic(content);

        // Fetch favicon asynchronously
        FaviconFetcher.fetchFaviconTask(bookmark.getUrl()).setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                Image favicon = (Image) event.getSource().getValue();
                faviconView.setImage(favicon);
            }
        });
        Thread faviconThread = new Thread(FaviconFetcher.fetchFaviconTask(bookmark.getUrl()));
        faviconThread.setDaemon(true);
        faviconThread.start();

        // Handle button click to navigate to the website
        //bookmarkButton.setOnAction(e -> browserView.navigateTo(bookmark.getUrl()));

        // Optional: Add right-click context menu for removing bookmarks
        javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
        javafx.scene.control.MenuItem removeItem = new javafx.scene.control.MenuItem("Remove Bookmark");
        removeItem.setOnAction(e -> removeBookmark(bookmark.getUrl()));
        contextMenu.getItems().add(removeItem);
        bookmarkButton.setContextMenu(contextMenu);

        bookmarkBarContainer.getChildren().add(bookmarkButton);
    }

    /**
     * Refreshes the bookmark bar UI after removal.
     */
    private void refreshBookmarks() {
        Platform.runLater(() -> {
            bookmarkBarContainer.getChildren().clear();
            for (Bookmark bookmark : bookmarks) {
                createBookmarkButton(bookmark);
            }
        });
    }

    /**
     * Gets the container FlowPane containing the bookmarks.
     *
     * @return The FlowPane containing the bookmarks.
     */
    public HBox getBookmarkBarContainer() {
        return bookmarkBarContainer;
    }

    /**
     * Gets the list of current bookmarks.
     *
     * @return List of bookmarks.
     */
    public List<Bookmark> getBookmarks() {
        return bookmarks;
    }
}

