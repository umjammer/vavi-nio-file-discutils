/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.emu;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import discUtils.core.DiscFileSystem;
import discUtils.core.FileLocator;
import discUtils.core.Geometry;
import discUtils.core.VirtualDisk;
import discUtils.core.VirtualDiskClass;
import discUtils.core.VirtualDiskLayer;
import discUtils.core.VirtualDiskTypeInfo;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Ownership;
import dotnet4j.io.FileAccess;


/**
 * Represents a vavi-nio-file-emu-backed disk.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/07/24 umjammer initial version <br>
 */
public final class Disk extends VirtualDisk {

    /**
     * The stream representing the disk's contents.
     */
    private SparseStream content;

    /**
     * The disk.
     */
    private DiskImageFile file;

    /**
     * Initializes a new instance of the Disk class. Differencing disks are
     * supported.
     *
     * @param path The path to the disk image.
     * @param access The access requested to the disk.
     */
    public Disk(String path, FileAccess access) throws IOException {
        file = new DiskImageFile(path, access);
    }

    /**
     * Initializes a new instance of the Disk class. Differencing disks are
     * supported.
     *
     * @param locator The locator to access relative files.
     * @param path The path to the disk image.
     * @param access The access requested to the disk.
     */
    Disk(FileLocator locator, String path, FileAccess access) throws IOException {
        file = new DiskImageFile(locator, path, access);
    }

    /**
     * Gets the capacity of the disk (in bytes).
     */
    public long getCapacity() {
        return file.getCapacity();
    }

    /**
     * Gets the content of the disk as a stream. Note the returned stream is not
     * guaranteed to be at any particular position. The actual position will
     * depend on the last partition table/file system activity, since all access
     * to the disk contents pass through a single stream instance. Set the
     * stream position before accessing the stream.
     */
    public SparseStream getContent() {
        if (content == null) {
            SparseStream stream = null;
            stream = file.openContent(stream, Ownership.Dispose);
            content = stream;
        }

        return content;
    }

    /**
     * Gets the type of disk represented by this object.
     */
    public VirtualDiskClass getDiskClass() {
        return VirtualDiskClass.HardDisk;
    }

    /**
     * Gets information about the type of disk. This property provides access to
     * meta-data about the disk format, for example whether the BIOS geometry is
     * preserved in the disk file.
     */
    public VirtualDiskTypeInfo getDiskTypeInfo() {
        return DiskFactory.makeDiskTypeInfo(file.isSparse() ? "dynamic" : "fixed");
    }

    /**
     * Gets the geometry of the disk.
     */
    public Geometry getGeometry() {
        return file.getGeometry();
    }

    /**
     * Gets the layers that make up the disk.
     */
    public List<VirtualDiskLayer> getLayers() {
        return Collections.singletonList(file);
    }

    /**
     * Create a new differencing disk, possibly within an existing disk.
     *
     * @param fileSystem The file system to create the disk on.
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    public VirtualDisk createDifferencingDisk(DiscFileSystem fileSystem, String path) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a new differencing disk.
     *
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    public VirtualDisk createDifferencingDisk(String path) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Disposes of underlying resources.
     */
    public void close() throws IOException {
        try {
            if (content != null) {
                content.close();
                content = null;
            }

            if (file != null) {
                file = null;
            }
        } finally {
            super.close();
        }
    }
}
