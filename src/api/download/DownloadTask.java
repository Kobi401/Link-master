package api.download;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.concurrent.Task;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class DownloadTask extends Task<Void> {
    private static final int BUFFER_SIZE = 8192;

    private final String fileURL;
    private final Path destinationPath;

    // Track the download speed in bytes/sec
    private final LongProperty bytesPerSecond = new SimpleLongProperty();

    // Optionally track total file size if known
    private long totalBytes = -1;

    public DownloadTask(String fileURL, Path destinationPath) {
        this.fileURL = fileURL;
        this.destinationPath = destinationPath;
    }

    public ReadOnlyLongProperty bytesPerSecondProperty() {
        return bytesPerSecond;
    }

    @Override
    protected Void call() throws Exception {
        URL url = new URL(fileURL);
        URLConnection connection = url.openConnection();
        connection.connect();

        // If the server gives us a content length, we store it:
        totalBytes = connection.getContentLengthLong();
        if (totalBytes <= 0) totalBytes = -1;  // if unknown or not provided

        try (InputStream in = connection.getInputStream();
             OutputStream out = Files.newOutputStream(destinationPath, StandardOpenOption.CREATE)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            long downloaded = 0;
            long lastTime = System.currentTimeMillis();
            long lastDownloaded = 0;

            int read;
            while ((read = in.read(buffer)) != -1) {
                if (isCancelled()) {
                    break;
                }
                out.write(buffer, 0, read);
                downloaded += read;

                // Update progress if totalBytes is known
                if (totalBytes > 0) {
                    updateProgress(downloaded, totalBytes);
                }

                // Calculate speed every BUFFER_SIZE read (simple approach)
                long currentTime = System.currentTimeMillis();
                long timeDiff = currentTime - lastTime;
                if (timeDiff >= 1000) {
                    // Bytes downloaded in the last second
                    long bytesInLastSecond = downloaded - lastDownloaded;
                    bytesPerSecond.set(bytesInLastSecond * 1000 / timeDiff);

                    lastTime = currentTime;
                    lastDownloaded = downloaded;
                }
            }

            // If totalBytes is unknown, we can just set progress to "indeterminate" or done:
            if (totalBytes <= 0) {
                updateProgress(1, 1);
            }

        } catch (IOException e) {
            // You may wish to handle errors more thoroughly
            e.printStackTrace();
            cancel(true);
        }

        return null;
    }
}

