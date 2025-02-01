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
        this.injector = new JSInjectionSystem(webEngine);
        injectUI();
    }

    /**
     * Injects a JavaScript UI overlay into the current document.
     * The UI consists of:
     * - A Chrome Dark Mode–themed control panel that can be configured to appear from the top or left.
     *   It includes a search bar (with autocomplete/autofill) and buttons for Search, Refresh, Home,
     *   Settings, About, and "Edit UI".
     * - A status bar fixed at the bottom with status text and a progress bar.
     * - A modal UI Editor window that allows editing of all UI parameters (colors, fonts, positions, side, etc.).
     * - Popup notifications for events.
     * - An initial notice modal that is shown once per session.
     *
     * The Settings and About buttons will, if no Java bridge is available, navigate to the URLs
     * provided by the local resources (using settingsUrl and aboutUrl).
     *
     *
     * Known Bugs
     * 1. Settings.html, About.html dont work
     * 2. the uicfg will reset on yt?
     * 3. Fonts like to change randomly
     * 4. the ui is not clean code-wise and design-wise, maybe moving things to separate classes will be better
     */
    private void injectUI() {
        String settingsUrl = getClass().getResource("/SettingsPage.html").toExternalForm();
        String aboutUrl = getClass().getResource("/AboutPage.html").toExternalForm();

        String uiScript = String.format("""
        (function() {
            // --- Global UI Configuration Object ---
            var uiConfig = {
                darkBackground: '#202124',
                darkAccent: '#4285f4',
                darkShadow: '0 0 15px #4285f4',
                darkFont: 'Roboto, sans-serif',
                darkText: '#e8eaed',
                // Control panel sizing and side configuration:
                controlPanelSide: 'top', // Options: 'top', 'left'
                controlsWidth: '500px',
                // Slide positions for top controls:
                slideInTop: '10px',
                slideOutTop: '-100px',
                // Slide positions for left controls:
                slideInLeft: '10px',
                slideOutLeft: '-500px'
            };

            // --- Preferences Persistence ---
            function loadUIPreferences() {
                try {
                    var prefs = localStorage.getItem('linkBrowserUIPrefs');
                    if (prefs) {
                        var obj = JSON.parse(prefs);
                        for (var key in obj) {
                            if (uiConfig.hasOwnProperty(key)) {
                                uiConfig[key] = obj[key];
                            }
                        }
                    }
                } catch (e) {
                    console.error("Error loading UI preferences:", e);
                }
            }
            function saveUIPreferences() {
                try {
                    localStorage.setItem('linkBrowserUIPrefs', JSON.stringify(uiConfig));
                } catch (e) {
                    console.error("Error saving UI preferences:", e);
                }
            }
            loadUIPreferences();

            // --- Helper: Process Search Query ---
            function processQuery(query) {
                query = query.trim();
                if (query.length === 0) return query;
                if (query.indexOf(" ") !== -1 || query.indexOf(".") === -1) {
                    return "https://www.google.com/search?q=" + encodeURIComponent(query);
                }
                if (!query.startsWith("http://") && !query.startsWith("https://")) {
                    return "http://" + query;
                }
                return query;
            }

            // --- Create Control Panel ---
            function createControlPanel() {
                var controls = document.createElement('div');
                controls.id = 'js-ui-controls';
                controls.style.position = 'fixed';
                if (uiConfig.controlPanelSide === 'top') {
                    controls.style.top = uiConfig.slideOutTop;
                    controls.style.left = '10px';
                    controls.style.width = uiConfig.controlsWidth;
                } else if (uiConfig.controlPanelSide === 'left') {
                    controls.style.left = uiConfig.slideOutLeft;
                    controls.style.top = '10px';
                    controls.style.width = uiConfig.controlsWidth;
                }
                controls.style.background = uiConfig.darkBackground;
                controls.style.padding = '10px';
                controls.style.border = '1px solid ' + uiConfig.darkAccent;
                controls.style.boxShadow = uiConfig.darkShadow;
                controls.style.zIndex = '10000';
                controls.style.fontFamily = uiConfig.darkFont;
                controls.style.borderRadius = '5px';
                controls.style.transition = 'all 0.5s ease-in-out';
                controls.style.pointerEvents = 'none';

                // Create search bar.
                var searchBar = document.createElement('input');
                searchBar.type = 'text';
                searchBar.placeholder = 'Enter URL or search query...';
                searchBar.style.width = '250px';
                searchBar.style.padding = '5px';
                searchBar.style.border = '1px solid #555';
                searchBar.style.borderRadius = '3px';
                searchBar.style.background = '#303134';
                searchBar.style.color = uiConfig.darkText;
                searchBar.style.fontSize = '14px';
                searchBar.id = 'search-bar';
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
                document.body.appendChild(suggestionsList);
                searchBar.setAttribute('list', 'search-suggestions');

                // Listen for Enter key on the search bar.
                searchBar.addEventListener('keydown', function(event) {
                    if (event.key === 'Enter' || event.keyCode === 13) {
                        var query = processQuery(searchBar.value);
                        console.log('Search query (Enter): ' + query);
                        if (window.javaSearch && typeof window.javaSearch.loadPage === 'function') {
                            window.javaSearch.loadPage(query);
                        } else {
                            location.href = query;
                        }
                    }
                });

                // Helper to create buttons.
                function createButton(label, onClick) {
                    var btn = document.createElement('button');
                    btn.innerHTML = label;
                    btn.style.marginLeft = '5px';
                    btn.style.padding = '5px 10px';
                    btn.style.background = uiConfig.darkAccent;
                    btn.style.border = 'none';
                    btn.style.borderRadius = '3px';
                    btn.style.color = uiConfig.darkBackground;
                    btn.style.cursor = 'pointer';
                    btn.style.fontSize = '14px';
                    btn.onclick = onClick;
                    return btn;
                }

                // Create buttons.
                var searchButton = createButton('Search', function() {
                    var query = processQuery(searchBar.value);
                    console.log('Search query (Button): ' + query);
                    if (window.javaSearch && typeof window.javaSearch.loadPage === 'function') {
                        window.javaSearch.loadPage(query);
                    } else {
                        location.href = query;
                    }
                });
                controls.appendChild(searchButton);

                var refreshButton = createButton('Refresh', function() {
                    location.reload();
                });
                controls.appendChild(refreshButton);

                var homeButton = createButton('Home', function() {
                    if (window.javaSearch && typeof window.javaSearch.home === 'function') {
                        window.javaSearch.home();
                    } else {
                        location.href = 'about:blank';
                    }
                });
                controls.appendChild(homeButton);

                var settingsButton = createButton('Settings', function() {
                    if (window.javaSearch && typeof window.javaSearch.settings === 'function') {
                        window.javaSearch.settings();
                    } else {
                        location.href = '%s';
                    }
                });
                controls.appendChild(settingsButton);

                var aboutButton = createButton('About', function() {
                    if (window.javaSearch && typeof window.javaSearch.about === 'function') {
                        window.javaSearch.about();
                    } else {
                        location.href = '%s';
                    }
                });
                controls.appendChild(aboutButton);

                var editorButton = createButton('Edit UI', function() {
                    var editor = document.getElementById('js-ui-editor');
                    if (editor) {
                        editor.style.display = 'block';
                    }
                });
                controls.appendChild(editorButton);

                var hideButton = createButton('Hide Controls', function() {
                    if (uiConfig.controlPanelSide === 'top') {
                        controls.style.top = uiConfig.slideOutTop;
                    } else if (uiConfig.controlPanelSide === 'left') {
                        controls.style.left = uiConfig.slideOutLeft;
                    }
                    controls.style.pointerEvents = 'none';
                });
                controls.appendChild(hideButton);

                // --- Create Tabs Container ---
                // This container will hold the list of open tabs.
                var tabList = document.createElement('div');
                tabList.id = 'js-tab-list';
                tabList.style.marginTop = '10px';
                tabList.style.padding = '5px';
                tabList.style.background = uiConfig.darkBackground;
                tabList.style.border = '1px solid ' + uiConfig.darkAccent;
                tabList.style.borderRadius = '3px';
                tabList.style.fontFamily = uiConfig.darkFont;
                tabList.style.color = uiConfig.darkText;
                // Append the tab list to the control panel.
                controls.appendChild(tabList);

                document.body.appendChild(controls);

                //keep controls open if mouse is inside the panel.
                document.addEventListener('mousemove', function(e) {
                    var rect = controls.getBoundingClientRect();
                    var inControls = (e.clientX >= rect.left && e.clientX <= rect.right &&
                                      e.clientY >= rect.top && e.clientY <= rect.bottom);
                    if (uiConfig.controlPanelSide === 'top') {
                        if (e.clientY < 50 || inControls) {
                            controls.style.top = uiConfig.slideInTop;
                            controls.style.pointerEvents = 'auto';
                        } else {
                            controls.style.top = uiConfig.slideOutTop;
                            controls.style.pointerEvents = 'none';
                        }
                    } else if (uiConfig.controlPanelSide === 'left') {
                        if (e.clientX < 50 || inControls) {
                            controls.style.left = uiConfig.slideInLeft;
                            controls.style.pointerEvents = 'auto';
                        } else {
                            controls.style.left = uiConfig.slideOutLeft;
                            controls.style.pointerEvents = 'none';
                        }
                    }
                });
            }

            // --- Create Status Bar ---
            function createStatusBar() {
                var statusBarContainer = document.createElement('div');
                statusBarContainer.id = 'js-status-bar';
                statusBarContainer.style.position = 'fixed';
                statusBarContainer.style.bottom = '0';
                statusBarContainer.style.left = '0';
                statusBarContainer.style.width = '100%%';
                statusBarContainer.style.background = uiConfig.darkBackground;
                statusBarContainer.style.borderTop = '1px solid ' + uiConfig.darkAccent;
                statusBarContainer.style.padding = '5px 10px';
                statusBarContainer.style.fontFamily = uiConfig.darkFont;
                statusBarContainer.style.display = 'flex';
                statusBarContainer.style.alignItems = 'center';
                statusBarContainer.style.justifyContent = 'space-between';
                statusBarContainer.style.boxShadow = '0 -2px 4px rgba(0,0,0,0.5)';
                statusBarContainer.style.zIndex = '10000';

                var statusText = document.createElement('span');
                statusText.id = 'status-text';
                statusText.innerText = 'Ready';
                statusText.style.color = uiConfig.darkAccent;
                statusText.style.fontSize = '14px';
                statusBarContainer.appendChild(statusText);

                var progressContainer = document.createElement('div');
                progressContainer.id = 'progress-container';
                progressContainer.style.position = 'relative';
                progressContainer.style.width = '200px';
                progressContainer.style.height = '10px';
                progressContainer.style.background = '#444';
                progressContainer.style.borderRadius = '5px';
                progressContainer.style.overflow = 'hidden';
                progressContainer.style.marginLeft = '10px';
                progressContainer.style.display = 'none';

                var progressBar = document.createElement('div');
                progressBar.id = 'progress-bar';
                progressBar.style.width = '0%%';
                progressBar.style.height = '100%%';
                progressBar.style.background = uiConfig.darkAccent;
                progressBar.style.transition = 'width 0.2s ease';
                progressContainer.appendChild(progressBar);

                statusBarContainer.appendChild(progressContainer);
                document.body.appendChild(statusBarContainer);
            }

            // --- Create UI Editor Modal ---
            function createUIEditor() {
                var editor = document.createElement('div');
                editor.id = 'js-ui-editor';
                editor.style.position = 'fixed';
                editor.style.top = '0';
                editor.style.left = '0';
                editor.style.width = '100%%';
                editor.style.height = '100%%';
                editor.style.backgroundColor = 'rgba(0, 0, 0, 0.75)';
                editor.style.display = 'none';
                editor.style.zIndex = '11000';
                editor.style.overflow = 'auto';

                var content = document.createElement('div');
                content.style.background = uiConfig.darkBackground;
                content.style.margin = '5%% auto';
                content.style.padding = '20px';
                content.style.border = '1px solid ' + uiConfig.darkAccent;
                content.style.width = '60%%';
                content.style.borderRadius = '5px';
                content.style.fontFamily = uiConfig.darkFont;
                content.style.color = uiConfig.darkText;

                var header = document.createElement('h2');
                header.innerText = 'UI Editor';
                header.style.color = uiConfig.darkAccent;
                content.appendChild(header);

                // Helper to create labeled controls.
                function createLabeledControl(labelText, control) {
                    var container = document.createElement('div');
                    container.style.marginBottom = '10px';

                    var label = document.createElement('label');
                    label.innerText = labelText;
                    label.style.display = 'block';
                    label.style.marginBottom = '5px';
                    container.appendChild(label);

                    container.appendChild(control);
                    return container;
                }

                var bgColorPicker = document.createElement('input');
                bgColorPicker.type = 'color';
                bgColorPicker.id = 'editor-darkBackground';
                bgColorPicker.value = uiConfig.darkBackground;

                var accentColorPicker = document.createElement('input');
                accentColorPicker.type = 'color';
                accentColorPicker.id = 'editor-darkAccent';
                accentColorPicker.value = uiConfig.darkAccent;

                var fontSelect = document.createElement('select');
                fontSelect.id = 'editor-darkFont';
                var fonts = ['Roboto, sans-serif', 'Arial, sans-serif', 'Courier New, monospace', 'Verdana, sans-serif'];
                fonts.forEach(function(f) {
                    var option = document.createElement('option');
                    option.value = f;
                    option.text = f.split(',')[0];
                    if (f === uiConfig.darkFont) {
                        option.selected = true;
                    }
                    fontSelect.appendChild(option);
                });

                var textColorPicker = document.createElement('input');
                textColorPicker.type = 'color';
                textColorPicker.id = 'editor-darkText';
                textColorPicker.value = uiConfig.darkText;

                var slideInInput = document.createElement('input');
                slideInInput.type = 'text';
                slideInInput.id = 'editor-slideInTop';
                slideInInput.value = uiConfig.slideInTop;

                var slideOutInput = document.createElement('input');
                slideOutInput.type = 'text';
                slideOutInput.id = 'editor-slideOutTop';
                slideOutInput.value = uiConfig.slideOutTop;

                var sideSelect = document.createElement('select');
                sideSelect.id = 'editor-controlPanelSide';
                var sides = ['top', 'left'];
                sides.forEach(function(side) {
                    var option = document.createElement('option');
                    option.value = side;
                    option.text = side.charAt(0).toUpperCase() + side.slice(1);
                    if (side === uiConfig.controlPanelSide) {
                        option.selected = true;
                    }
                    sideSelect.appendChild(option);
                });

                content.appendChild(createLabeledControl('Background Color:', bgColorPicker));
                content.appendChild(createLabeledControl('Accent Color:', accentColorPicker));
                content.appendChild(createLabeledControl('Font:', fontSelect));
                content.appendChild(createLabeledControl('Text Color:', textColorPicker));
                content.appendChild(createLabeledControl('Slide In Position (top/left):', slideInInput));
                content.appendChild(createLabeledControl('Slide Out Position (top/left):', slideOutInput));
                content.appendChild(createLabeledControl('Control Panel Side:', sideSelect));

                var btnContainer = document.createElement('div');
                btnContainer.style.textAlign = 'right';

                var applyBtn = document.createElement('button');
                applyBtn.innerText = 'Apply';
                applyBtn.style.marginRight = '10px';
                applyBtn.style.padding = '5px 10px';
                applyBtn.style.background = uiConfig.darkAccent;
                applyBtn.style.border = 'none';
                applyBtn.style.borderRadius = '3px';
                applyBtn.style.color = uiConfig.darkBackground;
                applyBtn.style.cursor = 'pointer';
                applyBtn.onclick = function() {
                    uiConfig.darkBackground = document.getElementById('editor-darkBackground').value;
                    uiConfig.darkAccent = document.getElementById('editor-darkAccent').value;
                    uiConfig.darkFont = document.getElementById('editor-darkFont').value;
                    uiConfig.darkText = document.getElementById('editor-darkText').value;
                    uiConfig.slideInTop = document.getElementById('editor-slideInTop').value;
                    uiConfig.slideOutTop = document.getElementById('editor-slideOutTop').value;
                    uiConfig.controlPanelSide = document.getElementById('editor-controlPanelSide').value;
                    if (uiConfig.controlPanelSide === 'left') {
                        uiConfig.slideInLeft = '10px';
                        uiConfig.slideOutLeft = '-' + uiConfig.controlsWidth;
                    }
                    uiConfig.darkShadow = '0 0 15px ' + uiConfig.darkAccent;
                    updateUIStyles();
                    saveUIPreferences();
                    showNotification("UI updated successfully!", 3000);
                    editor.style.display = 'none';
                };
                btnContainer.appendChild(applyBtn);

                var closeBtn = document.createElement('button');
                closeBtn.innerText = 'Close';
                closeBtn.style.padding = '5px 10px';
                closeBtn.style.background = uiConfig.darkAccent;
                closeBtn.style.border = 'none';
                closeBtn.style.borderRadius = '3px';
                closeBtn.style.color = uiConfig.darkBackground;
                closeBtn.style.cursor = 'pointer';
                closeBtn.onclick = function() {
                    editor.style.display = 'none';
                };
                btnContainer.appendChild(closeBtn);

                content.appendChild(btnContainer);
                editor.appendChild(content);
                document.body.appendChild(editor);
            }

            // --- Update UI Styles Based on uiConfig ---
            function updateUIStyles() {
                var controls = document.getElementById('js-ui-controls');
                if (controls) {
                    controls.style.background = uiConfig.darkBackground;
                    controls.style.border = '1px solid ' + uiConfig.darkAccent;
                    controls.style.boxShadow = '0 0 15px ' + uiConfig.darkAccent;
                    controls.style.fontFamily = uiConfig.darkFont;
                    if (uiConfig.controlPanelSide === 'top') {
                        controls.style.top = (controls.style.pointerEvents === 'auto' ? uiConfig.slideInTop : uiConfig.slideOutTop);
                        controls.style.left = '10px';
                        controls.style.width = uiConfig.controlsWidth;
                    } else if (uiConfig.controlPanelSide === 'left') {
                        controls.style.left = (controls.style.pointerEvents === 'auto' ? uiConfig.slideInLeft : uiConfig.slideOutLeft);
                        controls.style.top = '10px';
                        controls.style.width = uiConfig.controlsWidth;
                    }
                }
                var statusBar = document.getElementById('js-status-bar');
                if (statusBar) {
                    statusBar.style.background = uiConfig.darkBackground;
                    statusBar.style.borderTop = '1px solid ' + uiConfig.darkAccent;
                    statusBar.style.fontFamily = uiConfig.darkFont;
                }
            }

            // --- Create Notification Popup ---
            function createNotification(message, duration) {
                duration = duration || 3000;
                var notification = document.createElement('div');
                notification.innerText = message;
                notification.style.position = 'fixed';
                notification.style.bottom = '20px';
                notification.style.right = '20px';
                notification.style.background = uiConfig.darkAccent;
                notification.style.color = uiConfig.darkBackground;
                notification.style.padding = '10px 15px';
                notification.style.borderRadius = '3px';
                notification.style.boxShadow = '0 0 10px rgba(0,0,0,0.5)';
                notification.style.zIndex = '12000';
                notification.style.opacity = '1';
                document.body.appendChild(notification);
                setTimeout(function() {
                    notification.style.transition = 'opacity 1s ease';
                    notification.style.opacity = '0';
                    setTimeout(function() {
                        notification.remove();
                    }, 1000);
                }, duration);
            }

            function showNotification(msg, dur) {
                createNotification(msg, dur);
            }

            // --- Create Tabs Container & Render Tabs ---
            function createTabListContainer() {
                var tabList = document.createElement('div');
                tabList.id = 'js-tab-list';
                // Style the container as desired. Here we position it inside the control panel.
                tabList.style.marginTop = '10px';
                tabList.style.padding = '5px';
                tabList.style.background = uiConfig.darkBackground;
                tabList.style.border = '1px solid ' + uiConfig.darkAccent;
                tabList.style.borderRadius = '3px';
                tabList.style.fontFamily = uiConfig.darkFont;
                tabList.style.color = uiConfig.darkText;
                // Append the tab list container to the control panel.
                var controls = document.getElementById('js-ui-controls');
                if (controls) {
                    controls.appendChild(tabList);
                } else {
                    // Fallback: append to document body.
                    document.body.appendChild(tabList);
                }
            }

            // Global function to render tabs. Expects a JSON string.
            function renderTabs(tabsJson) {
                var tabList = document.getElementById('js-tab-list');
                if (!tabList) {
                    createTabListContainer();
                    tabList = document.getElementById('js-tab-list');
                }
                var tabs = JSON.parse(tabsJson);
                var html = "";
                for (var i = 0; i < tabs.length; i++) {
                    html += "<span style='margin-right:10px;'>" + tabs[i].title + " (Mem: " + tabs[i].memory + ")</span>";
                }
                tabList.innerHTML = html;
            }

            // --- Setup Global Functions ---
            function setupGlobalFunctions() {
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

                window.showNotification = showNotification;
                window.renderTabs = renderTabs;
            }

            // --- Show Initial Notice (once per session) as a Modal Window ---
            function showInitialNotice() {
                if (!sessionStorage.getItem('uiInitialNoticeShown')) {
                    var modal = document.createElement('div');
                    modal.id = 'initial-notice-modal';
                    modal.style.position = 'fixed';
                    modal.style.top = '0';
                    modal.style.left = '0';
                    modal.style.width = '100%%';
                    modal.style.height = '100%%';
                    modal.style.backgroundColor = 'rgba(0, 0, 0, 0.75)';
                    modal.style.display = 'flex';
                    modal.style.alignItems = 'center';
                    modal.style.justifyContent = 'center';
                    modal.style.zIndex = '13000';

                    var windowDiv = document.createElement('div');
                    var bg = (typeof uiConfig !== 'undefined' && uiConfig.darkBackground) ? uiConfig.darkBackground : '#202124';
                    var accent = (typeof uiConfig !== 'undefined' && uiConfig.darkAccent) ? uiConfig.darkAccent : '#4285f4';
                    var textColor = (typeof uiConfig !== 'undefined' && uiConfig.darkText) ? uiConfig.darkText : '#e8eaed';
                    var font = (typeof uiConfig !== 'undefined' && uiConfig.darkFont) ? uiConfig.darkFont : 'Roboto, sans-serif';
                    
                    windowDiv.style.background = bg;
                    windowDiv.style.border = '1px solid ' + accent;
                    windowDiv.style.boxShadow = '0 0 15px ' + accent;
                    windowDiv.style.borderRadius = '5px';
                    windowDiv.style.padding = '20px';
                    windowDiv.style.width = '400px';
                    windowDiv.style.color = textColor;
                    windowDiv.style.fontFamily = font;
                    windowDiv.style.textAlign = 'center';

                    var message = document.createElement('p');
                    message.innerText = "Welcome to LinkBrowser - In Development.\\n\\nThis interface is currently under development and may contain bugs or incomplete features. We appreciate your patience as we refine the experience.\\n\\nDeveloped by Kobi401. Future development will be transferred to im_owen25.";
                    message.style.whiteSpace = 'pre-wrap';
                    message.style.marginBottom = '20px';
                    windowDiv.appendChild(message);

                    var closeButton = document.createElement('button');
                    closeButton.innerText = 'Close';
                    closeButton.style.padding = '5px 10px';
                    closeButton.style.background = accent;
                    closeButton.style.color = bg;
                    closeButton.style.border = 'none';
                    closeButton.style.borderRadius = '3px';
                    closeButton.style.cursor = 'pointer';
                    closeButton.onclick = function() {
                        modal.remove();
                    };
                    windowDiv.appendChild(closeButton);

                    modal.appendChild(windowDiv);
                    document.body.appendChild(modal);

                    sessionStorage.setItem('uiInitialNoticeShown', 'true');
                }
            }

            // --- Initialize UI ---
            function initUI() {
                if (!document.getElementById('js-ui-controls')) {
                    createControlPanel();
                }
                if (!document.getElementById('js-status-bar')) {
                    createStatusBar();
                }
                if (!document.getElementById('js-ui-editor')) {
                    createUIEditor();
                }
                // Create the Tabs container if it doesn't exist.
                if (!document.getElementById('js-tab-list')) {
                    createTabListContainer();
                }
                setupGlobalFunctions();
                showInitialNotice();
            }

            initUI();
        })();
        """, settingsUrl, aboutUrl);

        injector.addScript(uiScript);
    }

}
