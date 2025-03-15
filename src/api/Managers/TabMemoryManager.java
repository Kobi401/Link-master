package api.Managers;

import javafx.scene.control.Tab;
import java.text.DecimalFormat;

public class TabMemoryManager {

    private long initialMemory;
    private long currentMemory;
    private Tab associatedTab;
    private static final Runtime RUNTIME = Runtime.getRuntime();
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    public TabMemoryManager(Tab tab) {
        this.associatedTab = tab;
        this.initialMemory = RUNTIME.totalMemory() - RUNTIME.freeMemory();
        this.currentMemory = 0;
    }

    /**
     * Updates the memory usage for this tab.
     * The difference is computed as the current global memory usage minus the memory usage when the tab was created.
     */
    public void updateMemoryUsage() {
        long globalUsed = RUNTIME.totalMemory() - RUNTIME.freeMemory();
        this.currentMemory = globalUsed - this.initialMemory;
        if (this.currentMemory < 0) {
            this.currentMemory = 0;
        }
    }

    /**
     * Returns a formatted string representing the memory usage difference for this tab.
     */
    public String getFormattedMemoryUsage() {
        return formatBytes(currentMemory);
    }

    /**
     * Helper method to format a number of bytes into a human-readable string.
     *
     * @param bytes The number of bytes.
     * @return A string such as "850 Bytes", "512 KB", "1.23 MB", or "2.34 GB".
     */
    private static String formatBytes(long bytes) {
        double value = bytes;
        String unit = "Bytes";
        if (bytes >= 1024 && bytes < 1024 * 1024) {
            value = bytes / 1024.0;
            unit = "KB";
        } else if (bytes >= 1024 * 1024 && bytes < 1024 * 1024 * 1024) {
            value = bytes / (1024.0 * 1024.0);
            unit = "MB";
        } else if (bytes >= 1024 * 1024 * 1024) {
            value = bytes / (1024.0 * 1024.0 * 1024.0);
            unit = "GB";
        }
        return DECIMAL_FORMAT.format(value) + " " + unit;
    }

    public Tab getAssociatedTab() {
        return associatedTab;
    }
}