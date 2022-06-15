
package DiscUtils.Core;

/**
 * Enumeration of the health status of a logical volume.
 */
public enum LogicalVolumeStatus {
    /**
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
    Failed
}
