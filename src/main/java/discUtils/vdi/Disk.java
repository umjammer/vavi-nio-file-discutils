//
// Copyright (c) 2008-2011, Kenneth Bell
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.
//

package discUtils.vdi;

import java.io.IOException;
import java.util.List;

import discUtils.core.DiscFileSystem;
import discUtils.core.FileLocator;
import discUtils.core.Geometry;
import discUtils.core.VirtualDisk;
import discUtils.core.VirtualDiskClass;
import discUtils.core.VirtualDiskLayer;
import discUtils.core.VirtualDiskTypeInfo;
import discUtils.core.internal.LocalFileLocator;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.Stream;


/**
 * Represents a disk stored in VirtualBox (Sun xVM) format.
 */
public final class Disk extends VirtualDisk {

    private SparseStream content;

    private final DiskImageFile diskImage;

    /**
     * Initializes a new instance of the Disk class.
     *
     * @param path The path to the disk.
     * @param access The access requested to the disk.
     */
    public Disk(String path, FileAccess access) {
        FileShare share = access == FileAccess.Read ? FileShare.Read : FileShare.None;
        FileLocator locator = new LocalFileLocator("");
        diskImage = new DiskImageFile(locator.open(path, FileMode.Open, access, share), Ownership.Dispose);
    }

    /**
     * Initializes a new instance of the Disk class.
     *
     * @param file The file containing the disk image.
     */
    public Disk(DiskImageFile file) {
        diskImage = file;
    }

    /**
     * Initializes a new instance of the Disk class. Differencing disks are not
     * supported.
     *
     * @param stream The stream to read.
     */
    public Disk(Stream stream) {
        diskImage = new DiskImageFile(stream);
    }

    /**
     * Initializes a new instance of the Disk class. Differencing disks are not
     * supported.
     *
     * @param stream The stream to read.
     * @param ownsStream Indicates if the new disk should take ownership of
     *            {@code stream} lifetime.
     */
    public Disk(Stream stream, Ownership ownsStream) {
        diskImage = new DiskImageFile(stream, ownsStream);
    }

    /**
     * Gets the capacity of the disk (in bytes).
     */
    @Override
    public long getCapacity() {
        return diskImage.getCapacity();
    }

    /**
     * Gets the content of the disk as a stream.
     * 
     * Note the returned stream is not guaranteed to be at any particular
     * position. The actual position will depend on the last partition
     * table/file system activity, since all access to the disk contents pass
     * through a single stream instance. Set the stream position before
     * accessing the stream.
     */
    @Override
    public SparseStream getContent() {
        if (content == null) {
            content = diskImage.openContent(null, Ownership.None);
        }

        return content;
    }

    /**
     * Gets the type of disk represented by this object.
     */
    @Override
    public VirtualDiskClass getDiskClass() {
        return VirtualDiskClass.HardDisk;
    }

    /**
     * Gets information about the type of disk. This property provides access to
     * meta-data about the disk format, for example whether the BIOS geometry is
     * preserved in the disk file.
     */
    @Override
    public VirtualDiskTypeInfo getDiskTypeInfo() {
        return DiskFactory.makeDiskTypeInfo(diskImage.isSparse() ? "dynamic" : "fixed");
    }

    /**
     * Gets the geometry of the disk.
     */
    @Override
    public Geometry getGeometry() {
        return diskImage.getGeometry();
    }

    /**
     * Gets the layers that make up the disk.
     */
    @Override
    public List<VirtualDiskLayer> getLayers() {
        return null;
    }

    /**
     * Initializes a stream as a fixed-sized VDI file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @return An object that accesses the stream as a VDI file.
     */
    public static Disk initializeFixed(Stream stream, Ownership ownsStream, long capacity) {
        return new Disk(DiskImageFile.initializeFixed(stream, ownsStream, capacity));
    }

    /**
     * Initializes a stream as a dynamically-sized VDI file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @return An object that accesses the stream as a VDI file.
     */
    public static Disk initializeDynamic(Stream stream, Ownership ownsStream, long capacity) {
        return new Disk(DiskImageFile.initializeDynamic(stream, ownsStream, capacity));
    }

    /**
     * Create a new differencing disk, possibly within an existing disk.
     *
     * @param fileSystem The file system to create the disk on.
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    @Override
    public VirtualDisk createDifferencingDisk(DiscFileSystem fileSystem, String path) {
        throw new UnsupportedOperationException("Differencing disks not implemented for the VDI format");
    }

    /**
     * Create a new differencing disk.
     *
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    @Override
    public VirtualDisk createDifferencingDisk(String path) {
        throw new UnsupportedOperationException("Differencing disks not implemented for the VDI format");
    }

    /**
     * Disposes of underlying resources.
     */
    @Override
    public void close() throws IOException {
        try {
            if (content != null) {
                content.close();
                content = null;
            }

            if (diskImage != null) {
                diskImage.close();
            }
        } finally {
            super.close();
        }
    }
}
