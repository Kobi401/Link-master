package api.Managers;

import javafx.scene.control.Tab;

import java.text.DecimalFormat;

public class TabMemoryManager {

    private String initialMemory;  //memory when this tab was created
    private String currentMemory;  //current memory usage for this tab
    //private final Runtime runtime;  //runtime instance for memory calculations
    private Tab associatedTab;  //the tab associated with this memory manager

    // Singleton Runtime instance to avoid multiple calls to Runtime.getRuntime()
    private static final Runtime RUNTIME = Runtime.getRuntime();

    // DecimalFormat for formatting GB values to two decimal places
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    public TabMemoryManager(Tab tab) {
        //this.runtime = Runtime.getRuntime();
        this.associatedTab = tab;
        this.initialMemory = calculateMemoryUsage();
        this.currentMemory = initialMemory;
    }

    /**
     * Calculates the current memory usage of the JVM and returns it as a formatted string.
     * The method automatically selects the most appropriate unit (KB, MB, GB, TB) based on the magnitude.
     *
     * @return A string representing the current memory usage, e.g., "850 MB" or "1.25 GB".
     */
    public static String calculateMemoryUsage() {
        long usedMemoryBytes = RUNTIME.totalMemory() - RUNTIME.freeMemory();
        double usedMemory = usedMemoryBytes;
        String unit = "Bytes";
        if (usedMemoryBytes >= 1024 && usedMemoryBytes < 1024 * 1024) {
            usedMemory = bytesToKilobytes(usedMemoryBytes);
            unit = "KB";
        } else if (usedMemoryBytes >= 1024 * 1024 && usedMemoryBytes < 1024 * 1024 * 1024) {
            usedMemory = bytesToMegabytes(usedMemoryBytes);
            unit = "MB";
        } else if (usedMemoryBytes >= 1024 * 1024 * 1024 && usedMemoryBytes < 1024L * 1024 * 1024 * 1024) {
            usedMemory = bytesToGigabytes(usedMemoryBytes);
            unit = "GB";
        } else if (usedMemoryBytes >= 1024L * 1024 * 1024 * 1024) {
            usedMemory = bytesToTerabytes(usedMemoryBytes);
            unit = "TB";
        }
        if (unit.equals("GB") && usedMemory > 1000) {
            usedMemory /= 1024.0;
            unit = "TB";
        }
        //format to two decimal places if in KB, MB, GB, or TB
        if (!unit.equals("Bytes")) {
            return DECIMAL_FORMAT.format(usedMemory) + " " + unit;
        } else {
            return String.format("%d %s", usedMemoryBytes, unit);
        }
    }

    /**
     * Converts bytes to kilobytes (KB).
     *
     * @param bytes The value in bytes.
     * @return The equivalent value in kilobytes.
     */
    private static double bytesToKilobytes(long bytes) {
        return bytes / 1024.0;
    }

    /**
     * Converts bytes to terabytes (TB).
     *
     * @param bytes The value in bytes.
     * @return The equivalent value in terabytes.
     */
    private static double bytesToTerabytes(long bytes) {
        return bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);
    }

    /**
     * Converts bytes to megabytes (MB).
     *
     * @param bytes The value in bytes.
     * @return The equivalent value in megabytes.
     */
    private static double bytesToMegabytes(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }

    /**
     * Converts bytes to gigabytes (GB).
     *
     * @param bytes The value in bytes.
     * @return The equivalent value in gigabytes.
     */
    private static double bytesToGigabytes(long bytes) {
        return bytes / (1024.0 * 1024.0 * 1024.0);
    }

    public void updateMemoryUsage() {
        currentMemory = calculateMemoryUsage();
    }

    public String getFormattedMemoryUsage() {
        return currentMemory + "";
    }

    public Tab getAssociatedTab() {
        return associatedTab;
    }
}
