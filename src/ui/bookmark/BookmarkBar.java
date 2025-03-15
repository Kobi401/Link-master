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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import java.util.ArrayList;
import java.util.List;

public class BookmarkBar {
    private FlowPane bookmarkBarContainer;
    private List<Bookmark> bookmarks;
    private BrowserView browserView;

    public BookmarkBar(BrowserView browserView) {
        this.browserView = browserView;
        bookmarks = new ArrayList<>();
        initializeBookmarkBar();
        loadBookmarks();
    }

    /**
     * Initializes the bookmark bar container.
     */
    private void initializeBookmarkBar() {
        bookmarkBarContainer = new FlowPane();
        bookmarkBarContainer.setHgap(10);
        bookmarkBarContainer.setVgap(5);
        bookmarkBarContainer.setPadding(new Insets(5, 10, 5, 10));
        bookmarkBarContainer.setAlignment(Pos.CENTER_LEFT);
        bookmarkBarContainer.getStyleClass().add("bookmark-bar");
        bookmarkBarContainer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bookmarkBarContainer, Priority.ALWAYS);
    }

    /**
     * Adds a new bookmark and immediately renders it.
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
     * Removes a bookmark and refreshes the UI.
     *
     * @param url The URL of the bookmark to remove.
     */
    public void removeBookmark(String url) {
        bookmarks.removeIf(b -> b.getUrl().equals(url));
        refreshBookmarks();
        BookmarkPersistence.saveBookmarks(bookmarks);
    }

    /**
     * Loads bookmarks from persistent storage and ensures they render properly.
     */
    private void loadBookmarks() {
        List<Bookmark> loadedBookmarks = BookmarkPersistence.loadBookmarks();
        for (Bookmark bookmark : loadedBookmarks) {
            bookmarks.add(bookmark);
            createBookmarkButton(bookmark);
        }
    }

    /**
     * Creates a bookmark button with favicon and label.
     *
     * @param bookmark The Bookmark object.
     */
    private void createBookmarkButton(Bookmark bookmark) {
        Button bookmarkButton = new Button();
        bookmarkButton.getStyleClass().add("bookmark-button");
        bookmarkButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        bookmarkButton.setAlignment(Pos.CENTER_LEFT);

        ImageView faviconView = new ImageView();
        faviconView.setFitWidth(16);
        faviconView.setFitHeight(16);
        faviconView.setPreserveRatio(true);
        faviconView.setSmooth(true);
        faviconView.setCache(true);

        javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(bookmark.getName());
        nameLabel.getStyleClass().add("bookmark-label");
        nameLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 12px;");

        HBox content = new HBox(5, faviconView, nameLabel);
        content.setAlignment(Pos.CENTER_LEFT);
        bookmarkButton.setGraphic(content);

        FaviconFetcher.fetchFaviconTask(bookmark.getUrl()).setOnSucceeded(event -> {
            Image favicon = (Image) event.getSource().getValue();
            faviconView.setImage(favicon);
        });
        new Thread(FaviconFetcher.fetchFaviconTask(bookmark.getUrl())).start();

        javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
        javafx.scene.control.MenuItem removeItem = new javafx.scene.control.MenuItem("Remove Bookmark");
        removeItem.setOnAction(e -> removeBookmark(bookmark.getUrl()));
        contextMenu.getItems().add(removeItem);
        bookmarkButton.setContextMenu(contextMenu);

        Platform.runLater(() -> bookmarkBarContainer.getChildren().add(bookmarkButton));
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
    public FlowPane getBookmarkBarContainer() {
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
