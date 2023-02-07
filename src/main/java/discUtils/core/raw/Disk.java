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

package discUtils.core.raw;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import discUtils.core.DiscFileSystem;
import discUtils.core.FloppyDiskType;
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
 * Represents a raw disk image.
 * This disk format is simply an uncompressed capture of all blocks on a disk.
 */
public final class Disk extends VirtualDisk {

    private DiskImageFile file;

    /**
     * Initializes a new instance of the Disk class.
     *
     * @param stream The stream to read.
     * @param ownsStream Indicates if the new instance should control the
     *            lifetime of the stream.
     */
    public Disk(Stream stream, Ownership ownsStream) {
        this(stream, ownsStream, null);
    }

    /**
     * Initializes a new instance of the Disk class.
     *
     * @param stream The stream to read.
     * @param ownsStream Indicates if the new instance should control the
     *            lifetime of the stream.
     * @param geometry The emulated geometry of the disk.
     */
    public Disk(Stream stream, Ownership ownsStream, Geometry geometry) {
        file = new DiskImageFile(stream, ownsStream, geometry);
    }

    /**
     * Initializes a new instance of the Disk class.
     *
     * @param path The path to the disk image.
     */
    public Disk(String path) throws IOException {
        this(path, FileAccess.ReadWrite);
    }

    /**
     * Initializes a new instance of the Disk class.
     *
     * @param path The path to the disk image.
     * @param access The access requested to the disk.
     */
    public Disk(String path, FileAccess access) throws IOException {
        FileShare share = access == FileAccess.Read ? FileShare.Read : FileShare.None;
        LocalFileLocator locator = new LocalFileLocator("");
        file = new DiskImageFile(locator.open(path, FileMode.Open, access, share), Ownership.Dispose, null);
    }

    /**
     * Initializes a new instance of the Disk class.
     *
     * @param file The contents of the disk.
     */
    private Disk(DiskImageFile file) {
        this.file = file;
    }

    /**
     * Gets the capacity of the disk (in bytes).
     */
    @Override public long getCapacity() {
        return file.getCapacity();
    }

    /**
     * Gets the content of the disk as a stream.
     * Note the returned stream is not guaranteed to be at any particular
     * position. The actual position
     * will depend on the last partition table/file system activity, since all
     * access to the disk contents pass
     * through a single stream instance. Set the stream position before
     * accessing the stream.
     */
    @Override public SparseStream getContent() {
        return file.getContent();
    }

    /**
     * Gets the type of disk represented by this object.
     */
    @Override public VirtualDiskClass getDiskClass() {
        return file.getDiskType();
    }

    /**
     * Gets information about the type of disk.
     * This property provides access to meta-data about the disk format, for
     * example whether the
     * BIOS geometry is preserved in the disk file.
     */
    @Override public VirtualDiskTypeInfo getDiskTypeInfo() {
        return DiskFactory.makeDiskTypeInfo();
    }

    /**
     * Gets the geometry of the disk.
     */
    @Override public Geometry getGeometry() {
        return file.getGeometry();
    }

    /**
     * Gets the layers that make up the disk.
     */
    @Override public List<VirtualDiskLayer> getLayers() {
        return Collections.singletonList(file);
    }

    /**
     * Initializes a stream as an unformatted disk.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @return An object that accesses the stream as a disk.
     */
    public static Disk initialize(Stream stream, Ownership ownsStream, long capacity) {
        return initialize(stream, ownsStream, capacity, null);
    }

    /**
     * Initializes a stream as an unformatted disk.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @param geometry The desired geometry of the new disk, or
     *            {@code null} for default.
     * @return An object that accesses the stream as a disk.
     */
    public static Disk initialize(Stream stream,
                                  Ownership ownsStream,
                                  long capacity,
                                  Geometry geometry) {
        return new Disk(DiskImageFile.initialize(stream, ownsStream, capacity, geometry));
    }

    /**
     * Initializes a stream as an unformatted floppy disk.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param type The type of floppy disk image to create.
     * @return An object that accesses the stream as a disk.
     */
    public static Disk initialize(Stream stream, Ownership ownsStream, FloppyDiskType type) {
        return new Disk(DiskImageFile.initialize(stream, ownsStream, type));
    }

    /**
     * Create a new differencing disk, possibly within an existing disk.
     *
     * @param fileSystem The file system to create the disk on.
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    @Override public VirtualDisk createDifferencingDisk(DiscFileSystem fileSystem, String path) {
        throw new UnsupportedOperationException("Differencing disks not supported for raw disks");
    }

    /**
     * Create a new differencing disk.
     *
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    @Override public VirtualDisk createDifferencingDisk(String path) {
        throw new UnsupportedOperationException("Differencing disks not supported for raw disks");
    }

    /**
     * Disposes of underlying resources.
     */
    @Override public void close() throws IOException {
        try {
            if (file != null) {
                file.close();
            }

            file = null;
        } finally {
            super.close();
        }
    }
}
