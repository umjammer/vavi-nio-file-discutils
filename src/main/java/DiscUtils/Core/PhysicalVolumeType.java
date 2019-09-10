
package DiscUtils.Core;

public enum PhysicalVolumeType {
    /**
     * Enumeration of possible types of physical volume.
     * 
     * Unknown type.
     */
    None,
    /**
     * Physical volume encompasses the entire disk.
     */
    EntireDisk,
    /**
     * Physical volume is defined by a BIOS-style partition table.
     */
    BiosPartition,
    /**
     * Physical volume is defined by a GUID partition table.
     */
    GptPartition,
    /**
     * Physical volume is defined by an Apple partition map.
     */
    ApplePartition
}
