package ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.control.ScrollPane;
import javafx.stage.Popup;

/**
 * SearchBar class with autofill (autocomplete) functionality and common website suggestions.
 */
public class SearchBar {
    private TextField searchField;
    private HBox searchBarContainer;
    private Button backButton;
    private Button forwardButton;
    private Button refreshButton;
    private Popup suggestionPopup;
    private ListView<String> suggestionListView;
    private ObservableList<String> commonWebsites;

    public SearchBar() {
        initializeCommonWebsites();
        initializeButtons();
        initializeSearchField();
        layoutSearchBar();
    }

    /**
     * Initializes the list of common websites.
     */
    private void initializeCommonWebsites() {
        commonWebsites = FXCollections.observableArrayList(
                "https://www.google.com",
                "https://www.youtube.com",
                "https://www.facebook.com",
                "https://www.twitter.com",
                "https://www.github.com",
                "https://www.stackoverflow.com",
                "https://www.linkedin.com",
                "https://www.reddit.com",
                "https://www.wikipedia.org",
                "https://www.amazon.com"
        );
    }

    /**
     * Initializes the back, forward, and refresh buttons with SVG icons and styles.
     */
    private void initializeButtons() {
        backButton = createIconButton("M15 18L9 12L15 6", "back-button");
        forwardButton = createIconButton("M9 18L15 12L9 6", "forward-button");
        refreshButton = createIconButton(
                "M19.9381 13C19.979 12.6724 20 12.3387 20 12C20 7.58172 16.4183 4 12 4C9.49942 4 7.26681 5.14727 5.7998 6.94416M4.06189 11C4.02104 11.3276 4 11.6613 4 12C4 16.4183 7.58172 20 12 20C14.3894 20 16.5341 18.9525 18 17.2916M15 17H18V17.2916M5.7998 4V6.94416M5.7998 6.94416V6.99993L8.7998 7M18 20V17.2916",
                "refresh-button"
        );
    }

    /**
     * Initializes the search field with autofill functionality.
     */
    private void initializeSearchField() {
        searchField = new TextField();
        searchField.setPromptText("Enter URL or search...");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        suggestionPopup = new Popup();
        suggestionPopup.setAutoHide(true);
        suggestionPopup.setAutoFix(true);

        suggestionListView = new ListView<>();
        suggestionListView.setPrefWidth(300);
        suggestionListView.setPrefHeight(150);
        suggestionListView.setItems(FXCollections.observableArrayList());

        suggestionListView.setOnMouseClicked(event -> {
            String selected = suggestionListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                searchField.setText(selected);
                suggestionPopup.hide();
            }
        });

        searchField.setOnKeyPressed(event -> {
            if (suggestionPopup.isShowing()) {
                if (event.getCode() == KeyCode.DOWN) {
                    suggestionListView.requestFocus();
                    suggestionListView.getSelectionModel().selectFirst();
                } else if (event.getCode() == KeyCode.ESCAPE) {
                    suggestionPopup.hide();
                }
            }
        });

        suggestionListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String selected = suggestionListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    searchField.setText(selected);
                    suggestionPopup.hide();
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                suggestionPopup.hide();
            }
        });

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                suggestionPopup.hide();
            } else {
                ObservableList<String> filteredSuggestions = commonWebsites.filtered(
                        url -> url.toLowerCase().contains(newValue.toLowerCase())
                );
                if (!filteredSuggestions.isEmpty()) {
                    suggestionListView.setItems(filteredSuggestions);
                    if (!suggestionPopup.isShowing()) {
                        suggestionPopup.getContent().clear();
                        suggestionPopup.getContent().add(new ScrollPane(suggestionListView));
                        suggestionPopup.show(searchField, searchField.localToScreen(0, 0).getX(),
                                searchField.localToScreen(0, 0).getY() + searchField.getHeight());
                    }
                } else {
                    suggestionPopup.hide();
                }
            }
        });
    }

    /**
     * Arranges the search bar components horizontally and applies container styling.
     */
    private void layoutSearchBar() {
        searchBarContainer = new HBox(5, backButton, forwardButton, refreshButton, searchField);
        searchBarContainer.setAlignment(Pos.CENTER_LEFT);
        searchBarContainer.setPadding(new Insets(2, 10, 2, 10));
        searchBarContainer.getStyleClass().add("search-bar"); // Apply CSS style
    }

    /**
     * Creates a button with an SVG icon.
     *
     * @param svgPathData The SVG path data string.
     * @param buttonClass The CSS class to assign to the button.
     * @return The styled button.
     */
    private Button createIconButton(String svgPathData, String buttonClass) {
        Button button = new Button();
        SVGPath icon = new SVGPath();
        icon.setContent(svgPathData);
        icon.setFill(Color.DIMGRAY);
        icon.getStyleClass().add("svg-icon");
        button.setGraphic(icon);
        button.getStyleClass().addAll("search-control-button", buttonClass);
        button.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand;");
        button.setPadding(new Insets(2));
        return button;
    }

    public TextField getSearchField() {
        return searchField;
    }

    public HBox getSearchBarContainer() {
        return searchBarContainer;
    }

    public Button getBackButton() {
        return backButton;
    }

    public Button getForwardButton() {
        return forwardButton;
    }

    public Button getRefreshButton() {
        return refreshButton;
    }
}

