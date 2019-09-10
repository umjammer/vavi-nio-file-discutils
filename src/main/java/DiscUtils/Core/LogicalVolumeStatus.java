
package DiscUtils.Core;

import java.util.Arrays;


public enum LogicalVolumeStatus {
    /**
     * Enumeration of the health status of a logical volume.
     * 
     * The volume is healthy and fully functional.
     */
    Healthy,
    /**
     * The volume is completely accessible, but at degraded redundancy.
     */
    FailedRedundancy,
    /**
     * The volume is wholey, or partly, inaccessible.
     */
    Failed;
    public static LogicalVolumeStatus valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.ordinal() == value).findFirst().get();
    }
}
