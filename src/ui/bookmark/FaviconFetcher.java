package ui.bookmark;

import javafx.concurrent.Task;
import javafx.scene.image.Image;

import java.net.MalformedURLException;
import java.net.URL;

public class FaviconFetcher {
    /**
     * Fetches the favicon for the given website URL.
     *
     * @param websiteUrl The URL of the website.
     * @return A Task that returns the Image of the favicon.
     */
    public static Task<Image> fetchFaviconTask(String websiteUrl) {
        return new Task<>() {
            @Override
            protected Image call() {
                try {
                    URL url = new URL(websiteUrl);
                    String faviconUrl = url.getProtocol() + "://" + url.getHost() + "/favicon.ico";
                    return new Image(faviconUrl, 16, 16, true, true, true);
                } catch (MalformedURLException e) {
                    System.err.println("Invalid URL: " + websiteUrl);
                    return new Image(FaviconFetcher.class.getResourceAsStream("/Images/default_favicon.png"));
                } catch (Exception e) {
                    System.err.println("Failed to fetch favicon for: " + websiteUrl);
                    return new Image(FaviconFetcher.class.getResourceAsStream("/Images/default_favicon.png"));
                }
            }
        };
    }
}

