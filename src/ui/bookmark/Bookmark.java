package ui.bookmark;

public class Bookmark {
    private String name;
    private String url;

    public Bookmark(String name, String url) {
        this.name = name;
        this.url = url;
    }

    // Getters
    public String getName() { return name; }
    public String getUrl() { return url; }

    // Setters (if needed)
    public void setName(String name) { this.name = name; }
    public void setUrl(String url) { this.url = url; }
}

