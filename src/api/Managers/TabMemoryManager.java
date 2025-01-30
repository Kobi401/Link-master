package api.Managers;

import javafx.scene.control.Tab;

public class TabMemoryManager {

    private long initialMemory;  //memory when this tab was created
    private long currentMemory;  //current memory usage for this tab
    private final Runtime runtime;  //runtime instance for memory calculations
    private Tab associatedTab;  //the tab associated with this memory manager

    public TabMemoryManager(Tab tab) {
        this.runtime = Runtime.getRuntime();
        this.associatedTab = tab;
        this.initialMemory = calculateMemoryUsage();
        this.currentMemory = initialMemory;
    }

    public long calculateMemoryUsage() {
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);  // Memory in MB
    }

    public void updateMemoryUsage() {
        currentMemory = calculateMemoryUsage();
    }

    public String getFormattedMemoryUsage() {
        return currentMemory + " MB";
    }

    public Tab getAssociatedTab() {
        return associatedTab;
    }
}
