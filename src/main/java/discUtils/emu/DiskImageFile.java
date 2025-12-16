/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.emu;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import discUtils.core.FileLocator;
import discUtils.core.Geometry;
import discUtils.core.VirtualDiskExtent;
import discUtils.core.VirtualDiskLayer;
import discUtils.core.internal.LocalFileLocator;
import discUtils.core.internal.Utilities;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.Stream;
import vavi.emu.disk.LogicalDisk;


/**
 * Represents a single emu disk file.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2024/05/26 umjammer initial version <br>
 */
public final class DiskImageFile extends VirtualDiskLayer {

    private static final Logger logger = System.getLogger(DiskImageFile.class.getName());

    private vavi.emu.disk.Disk disk;

    /**
     * The object that can be used to locate relative file paths.
     */
    private FileLocator fileLocator;

    /**
     * The file name of this emu disk.
     */
    private String fileName;

    /**
     * The stream containing the emu disk file.
     */
    private Stream fileStream;

    /**
     * Indicates if this object controls the lifetime of the stream.
     */
    private Ownership ownership;

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param stream The stream to interpret.
     */
    public DiskImageFile(Stream stream) {
        fileStream = stream;
    }

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param stream The stream to interpret.
     * @param ownership Indicates if the new instance should control the
     *            lifetime of the stream.
     */
    public DiskImageFile(Stream stream, Ownership ownership) {
        fileStream = stream;
        this.ownership = ownership;
    }

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param path The file path to open.
     * @param access Controls how the file can be accessed.
     */
    public DiskImageFile(String path, FileAccess access) {
        this(new LocalFileLocator(Utilities.getDirectoryFromPath(path)), Utilities.getFileFromPath(path), access);
    }

    DiskImageFile(FileLocator locator, String path, Stream stream, Ownership ownsStream) {
        this(stream, ownsStream);

        fileLocator = locator.getRelativeLocator(locator.getDirectoryFromPath(path));
        fileName = locator.getFileFromPath(path);
    }

    DiskImageFile(FileLocator locator, String path, FileAccess access) {
        FileShare share = access == FileAccess.Read ? FileShare.Read : FileShare.None;

        try {
            disk = vavi.emu.disk.Disk.read(Paths.get(locator.getFullPath(path)));
            if (disk.getSectorSize() == -1) {
logger.log(Level.DEBUG, "no sector size, try to post read");
                try {
                    LogicalDisk logicalDisk = LogicalDisk.read(Path.of(locator.getFullPath(path)), disk);
logger.log(Level.DEBUG, "logicalDisk: " + logicalDisk.getClass().getSimpleName() + ", bps: " + disk.getSectorSize() + ", offset: " + disk.getOffset());
                    FileSystemFactory.logicalDisk.set(logicalDisk);
                } catch (IllegalArgumentException e) { // not found for logicalDisk
                    FileSystemFactory.logicalDisk.set(null);
logger.log(Level.DEBUG, "no logicalDisk: " + e);
                }
            }
            assert disk.getSectorSize() != -1 : "bytes per sector should be defined";

            fileStream = locator.open(path, FileMode.Open, access, share);
            ownership = Ownership.Dispose;

            fileLocator = locator.getRelativeLocator(locator.getDirectoryFromPath(path));
            fileName = locator.getFileFromPath(path);
        } catch (IOException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new dotnet4j.io.IOException(e);
        }
    }

    @Override
    public long getCapacity() {
        return disk.getLength();
    }

    /**
     * Gets the extent that comprises this file.
     */
    @Override public List<VirtualDiskExtent> getExtents() {
        List<VirtualDiskExtent> result = new ArrayList<>();
        return result;
    }

    /**
     * Gets the full path to this disk layer, or empty string.
     */
    @Override public String getFullPath() {
        if (fileLocator != null && fileName != null) {
            return fileLocator.getFullPath(fileName);
        }

        return "";
    }

    /**
     * Gets the geometry of the virtual disk.
     */
    @Override public Geometry getGeometry() {
        return new Geometry(
                disk.getGeometry().cylinders,
                disk.getGeometry().heads,
                disk.getGeometry().sectors
        );
    }

    /**
     * Gets a value indicating if the layer only stores meaningful sectors.
     */
    @Override public boolean isSparse() {
        return false;
    }

    /**
     * Gets a value indicating whether the file is a differencing disk.
     */
    @Override public boolean needsParent() {
        return false;
    }

    @Override public FileLocator getRelativeFileLocator() {
        return fileLocator;
    }

    /**
     * Opens the content of the disk image file as a stream.
     *
     * @param parent The parent file's content (if any).
     * @param ownsParent Whether the created stream assumes ownership of parent
     *            stream.
     * @return The new content stream.
     */
    @Override public SparseStream openContent(SparseStream parent, Ownership ownsParent) {
        if (parent != null && ownsParent == Ownership.Dispose) {
            try {
                // Not needed until differencing disks supported.
                parent.close();
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }

        DiskStream stream = new DiskStream(this.fileStream, Ownership.None, disk);
        return stream;
    }

    /**
     * Gets the location of the parent file, given a base path.
     *
     * @return list of candidate file locations.
     */
    @Override public List<String> getParentLocations() {
        return null;
    }

    /**
     * Disposes of underlying resources.
     */
    @Override public void close() throws IOException {
        if (ownership == Ownership.Dispose && fileStream != null) {
            fileStream.close();
        }

        fileStream = null;
    }
}
