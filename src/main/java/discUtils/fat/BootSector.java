/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.fat;


/**
 * BootSector.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-08-06 nsano initial version <br>
 */
public interface BootSector {

    /**
     * Gets the number of FATs present.
     */
    int getFatCount();

    /**
     * Gets the active FAT (zero-based index).
     */
    byte getActiveFat();

    /**
     * Gets the number of bytes per sector (as stored in the file-system meta
     * data).
     */
    int getBytesPerSector();

    /**
     * Gets the size of a single FAT, in sectors.
     */
    long getFatSize();

    /**
     * Gets the FAT variant of the file system.
     */
    FatType getFatVariant();

    /**
     * Gets the OEM name from the file system.
     */
    String getOemName();

    /**
     * Gets the number of reserved sectors at the start of the disk.
     */
    int getReservedSectorCount();

    /**
     * Gets the number of contiguous sectors that make up one cluster.
     */
    int getSectorsPerCluster();

    /**
     * Gets the total number of sectors on the disk.
     */
    long getTotalSectors();

    /**
     * Gets the volume label.
     */
    String getVolumeLabel();

    /**
     * Gets the maximum number of root directory entries (on FAT variants that
     * have a limit).
     */
    int getMaxRootDirectoryEntries();

    /**
     * Gets the cluster number of the first cluster of the root directory (FAT32
     * only).
     */
    long getRootDirectoryCluster();

    /**
     * Gets the size of a single FAT, in sectors.
     * for 16bit fat.
     */
    int getFatSize16();
}
