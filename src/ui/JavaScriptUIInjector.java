package ui;

import api.injection.JSInjectionSystem;
import javafx.scene.web.WebEngine;

/**
 * JavaScriptUIInjector sets up a UI built entirely in JavaScript and injects it into a WebEngine.
 * The UI includes:
 * - A search bar with a search button.
 * - A refresh button.
 * - A hide button to dismiss the UI overlay.
 * - A status bar whose text can be updated via a global JavaScript function.
 *
 * Global functions exposed:
 *   window.updateStatus(newStatus) - sets the status bar text.
 *   window.getSearchQuery() - returns the current search bar value.
 *
 * Additionally, when the user presses Enter in the search bar or clicks the Search button,
 * the UI will try to call a global Java bridge function (window.javaSearch.loadPage) with the query.
 * Make sure to expose such a bridge from Java if you want to handle loading via Java.
 */
public class JavaScriptUIInjector {

    private final WebEngine webEngine;
    private final JSInjectionSystem injector;

    public JavaScriptUIInjector(WebEngine webEngine) {
        this.webEngine = webEngine;
        // Create a new JSInjectionSystem for this WebEngine.
        this.injector = new JSInjectionSystem(webEngine);
        injectUI();
    }

    /**
     * Injects a JavaScript UI overlay into the current document.
     * The UI consists of:
     * - A Chrome Dark Mode–themed control panel at the top that auto-hides when the mouse is away.
     *   It includes a search bar (with autofill/autocomplete) and buttons for Search, Refresh, Home, Settings, and About.
     * - A status bar fixed at the bottom with status text and a progress bar.
     *
     * The Settings and About buttons will, if no Java bridge is available, navigate to the URLs
     * provided by the local resources (using settingsUrl and aboutUrl).
     */
    private void injectUI() {
        // Obtain the URLs from resources.
        String settingsUrl = getClass().getResource("/SettingsPage.html").toExternalForm();
        String aboutUrl = getClass().getResource("/AboutPage.html").toExternalForm();

        // Build the UI script. Use String.format to inject settingsUrl and aboutUrl.
        String uiScript = String.format("""
        (function() {
            // --- Chrome Dark Mode Style Variables ---
            var darkBackground = '#202124';
            var darkAccent = '#4285f4';       // Blue accent.
            var darkShadow = '0 0 15px ' + darkAccent;
            var darkFont = 'Roboto, sans-serif';
            var darkText = '#e8eaed';
            var slideInTop = '10px';    // Visible state.
            var slideOutTop = '-100px'; // Hidden (off-screen) state.
    
            // --- Control Panel (Top) ---
            var controls = document.getElementById('js-ui-controls');
            if (!controls) {
                controls = document.createElement('div');
                controls.id = 'js-ui-controls';
                // Initially hidden.
                controls.style.position = 'fixed';
                controls.style.top = slideOutTop;
                controls.style.left = '10px';
                controls.style.width = 'calc(100%% - 20px)';
                controls.style.background = darkBackground;
                controls.style.padding = '10px';
                controls.style.border = '1px solid ' + darkAccent;
                controls.style.boxShadow = darkShadow;
                controls.style.zIndex = '10000';
                controls.style.fontFamily = darkFont;
                controls.style.borderRadius = '5px';
                controls.style.transition = 'top 0.5s ease-in-out, pointer-events 0.5s ease-in-out';
                controls.style.pointerEvents = 'none'; // Disabled when hidden.
    
                // Create search bar.
                var searchBar = document.createElement('input');
                searchBar.type = 'text';
                searchBar.placeholder = 'Enter URL or search query...';
                searchBar.style.width = '250px';
                searchBar.style.padding = '5px';
                searchBar.style.border = '1px solid #555';
                searchBar.style.borderRadius = '3px';
                searchBar.style.background = '#303134';
                searchBar.style.color = darkText;
                searchBar.style.fontSize = '14px';
                searchBar.id = 'search-bar';
                // Enable native autocomplete/autofill.
                searchBar.setAttribute('autocomplete', 'on');
                searchBar.setAttribute('autocorrect', 'off');
                searchBar.setAttribute('autocapitalize', 'none');
                searchBar.setAttribute('spellcheck', 'false');
                controls.appendChild(searchBar);
    
                // Create a datalist for autocomplete suggestions.
                var suggestionsList = document.createElement('datalist');
                suggestionsList.id = 'search-suggestions';
                var suggestions = ['www.google.com', 'www.youtube.com', 'www.facebook.com', 'www.twitter.com', 'www.wikipedia.org'];
                for (var i = 0; i < suggestions.length; i++) {
                    var option = document.createElement('option');
                    option.value = suggestions[i];
                    suggestionsList.appendChild(option);
                }
                // Append the datalist to the document body.
                document.body.appendChild(suggestionsList);
                // Link the search bar to the datalist.
                searchBar.setAttribute('list', 'search-suggestions');
    
                // Listen for Enter key on the search bar.
                searchBar.addEventListener('keydown', function(event) {
                    if (event.key === 'Enter' || event.keyCode === 13) {
                        var query = searchBar.value;
                        console.log('Search query (Enter): ' + query);
                        if (window.javaSearch && typeof window.javaSearch.loadPage === 'function') {
                            window.javaSearch.loadPage(query);
                        } else {
                            location.href = query;
                        }
                    }
                });
    
                // Create Search button.
                var searchButton = document.createElement('button');
                searchButton.innerHTML = 'Search';
                searchButton.style.marginLeft = '5px';
                searchButton.style.padding = '5px 10px';
                searchButton.style.background = darkAccent;
                searchButton.style.border = 'none';
                searchButton.style.borderRadius = '3px';
                searchButton.style.color = darkBackground;
                searchButton.style.cursor = 'pointer';
                searchButton.style.fontSize = '14px';
                searchButton.onclick = function() {
                    var query = searchBar.value;
                    console.log('Search query (Button): ' + query);
                    if (window.javaSearch && typeof window.javaSearch.loadPage === 'function') {
                        window.javaSearch.loadPage(query);
                    } else {
                        location.href = query;
                    }
                };
                controls.appendChild(searchButton);
    
                // Create Refresh button.
                var refreshButton = document.createElement('button');
                refreshButton.innerHTML = 'Refresh';
                refreshButton.style.marginLeft = '5px';
                refreshButton.style.padding = '5px 10px';
                refreshButton.style.background = darkAccent;
                refreshButton.style.border = 'none';
                refreshButton.style.borderRadius = '3px';
                refreshButton.style.color = darkBackground;
                refreshButton.style.cursor = 'pointer';
                refreshButton.style.fontSize = '14px';
                refreshButton.onclick = function() {
                    location.reload();
                };
                controls.appendChild(refreshButton);
    
                // Create Home button.
                var homeButton = document.createElement('button');
                homeButton.innerHTML = 'Home';
                homeButton.style.marginLeft = '5px';
                homeButton.style.padding = '5px 10px';
                homeButton.style.background = darkAccent;
                homeButton.style.border = 'none';
                homeButton.style.borderRadius = '3px';
                homeButton.style.color = darkBackground;
                homeButton.style.cursor = 'pointer';
                homeButton.style.fontSize = '14px';
                homeButton.onclick = function() {
                    if (window.javaSearch && typeof window.javaSearch.home === 'function') {
                        window.javaSearch.home();
                    } else {
                        location.href = 'about:blank';
                    }
                };
                controls.appendChild(homeButton);
    
                // Create Settings button.
                var settingsButton = document.createElement('button');
                settingsButton.innerHTML = 'Settings';
                settingsButton.style.marginLeft = '5px';
                settingsButton.style.padding = '5px 10px';
                settingsButton.style.background = darkAccent;
                settingsButton.style.border = 'none';
                settingsButton.style.borderRadius = '3px';
                settingsButton.style.color = darkBackground;
                settingsButton.style.cursor = 'pointer';
                settingsButton.style.fontSize = '14px';
                settingsButton.onclick = function() {
                    if (window.javaSearch && typeof window.javaSearch.settings === 'function') {
                        window.javaSearch.settings();
                    } else {
                        location.href = '%s';
                    }
                };
                controls.appendChild(settingsButton);
    
                // Create About button.
                var aboutButton = document.createElement('button');
                aboutButton.innerHTML = 'About';
                aboutButton.style.marginLeft = '5px';
                aboutButton.style.padding = '5px 10px';
                aboutButton.style.background = darkAccent;
                aboutButton.style.border = 'none';
                aboutButton.style.borderRadius = '3px';
                aboutButton.style.color = darkBackground;
                aboutButton.style.cursor = 'pointer';
                aboutButton.style.fontSize = '14px';
                aboutButton.onclick = function() {
                    if (window.javaSearch && typeof window.javaSearch.about === 'function') {
                        window.javaSearch.about();
                    } else {
                        location.href = '%s';
                    }
                };
                controls.appendChild(aboutButton);
    
                // Create Hide Controls button.
                var hideButton = document.createElement('button');
                hideButton.innerHTML = 'Hide Controls';
                hideButton.style.marginLeft = '5px';
                hideButton.style.padding = '5px 10px';
                hideButton.style.background = darkAccent;
                hideButton.style.border = 'none';
                hideButton.style.borderRadius = '3px';
                hideButton.style.color = darkBackground;
                hideButton.style.cursor = 'pointer';
                hideButton.style.fontSize = '14px';
                hideButton.onclick = function() {
                    controls.style.top = slideOutTop;
                    controls.style.pointerEvents = 'none';
                };
                controls.appendChild(hideButton);
    
                // Append the control panel to the document body.
                document.body.appendChild(controls);
    
                // Auto-hide logic: slide controls in/out based on mouse position.
                document.addEventListener('mousemove', function(e) {
                    if (e.clientY < 50) {
                        controls.style.top = slideInTop;
                        controls.style.pointerEvents = 'auto';
                    } else {
                        controls.style.top = slideOutTop;
                        controls.style.pointerEvents = 'none';
                    }
                });
            }
    
            // --- Status Bar (Bottom) ---
            if (!document.getElementById('js-status-bar')) {
                var statusBarContainer = document.createElement('div');
                statusBarContainer.id = 'js-status-bar';
                statusBarContainer.style.position = 'fixed';
                statusBarContainer.style.bottom = '0';
                statusBarContainer.style.left = '0';
                statusBarContainer.style.width = '100%%';
                statusBarContainer.style.background = darkBackground;
                statusBarContainer.style.borderTop = '1px solid ' + darkAccent;
                statusBarContainer.style.padding = '5px 10px';
                statusBarContainer.style.fontFamily = darkFont;
                statusBarContainer.style.display = 'flex';
                statusBarContainer.style.alignItems = 'center';
                statusBarContainer.style.justifyContent = 'space-between';
                statusBarContainer.style.boxShadow = '0 -2px 4px rgba(0,0,0,0.5)';
                statusBarContainer.style.zIndex = '10000';
    
                // Create status text element.
                var statusText = document.createElement('span');
                statusText.id = 'status-text';
                statusText.innerText = 'Ready';
                statusText.style.color = darkAccent;
                statusText.style.fontSize = '14px';
                statusBarContainer.appendChild(statusText);
    
                // Create progress bar container.
                var progressContainer = document.createElement('div');
                progressContainer.id = 'progress-container';
                progressContainer.style.position = 'relative';
                progressContainer.style.width = '200px';
                progressContainer.style.height = '10px';
                progressContainer.style.background = '#444';
                progressContainer.style.borderRadius = '5px';
                progressContainer.style.overflow = 'hidden';
                progressContainer.style.marginLeft = '10px';
                progressContainer.style.display = 'none'; // Hidden by default.
    
                // Create progress bar element.
                var progressBar = document.createElement('div');
                progressBar.id = 'progress-bar';
                progressBar.style.width = '0%%';
                progressBar.style.height = '100%%';
                progressBar.style.background = darkAccent;
                progressBar.style.transition = 'width 0.2s ease';
                progressContainer.appendChild(progressBar);
    
                statusBarContainer.appendChild(progressContainer);
                document.body.appendChild(statusBarContainer);
            }
    
            // --- Global Functions ---
            window.updateStatus = function(newStatus) {
                var statusText = document.getElementById('status-text');
                if (statusText) {
                    statusText.innerText = newStatus;
                }
            };
    
            window.setProgress = function(percentage) {
                var progressBar = document.getElementById('progress-bar');
                if (progressBar) {
                    progressBar.style.width = percentage + '%%';
                }
            };
    
            window.showProgress = function(show) {
                var progressContainer = document.getElementById('progress-container');
                if (progressContainer) {
                    progressContainer.style.display = show ? 'block' : 'none';
                }
            };
    
            window.getSearchQuery = function() {
                var searchBar = document.getElementById('search-bar');
                return searchBar ? searchBar.value : '';
            };
        })();
        """, settingsUrl, aboutUrl);

        // Inject the script using the JS injection system.
        injector.addScript(uiScript);
    }
}
