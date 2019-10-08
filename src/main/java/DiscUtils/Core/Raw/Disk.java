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

package DiscUtils.Core.Raw;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.FloppyDiskType;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VirtualDiskClass;
import DiscUtils.Core.VirtualDiskLayer;
import DiscUtils.Core.VirtualDiskTypeInfo;
import DiscUtils.Core.Internal.LocalFileLocator;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Util.Ownership;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.FileShare;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * Represents a raw disk image.
 * This disk format is simply an uncompressed capture of all blocks on a disk.
 */
public final class Disk extends VirtualDisk {
    private DiskImageFile _file;

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
        _file = new DiskImageFile(stream, ownsStream, geometry);
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
        _file = new DiskImageFile(locator.open(path, FileMode.Open, access, share), Ownership.Dispose, null);
    }

    /**
     * Initializes a new instance of the Disk class.
     *
     * @param file The contents of the disk.
     */
    private Disk(DiskImageFile file) {
        _file = file;
    }

    /**
     * Gets the capacity of the disk (in bytes).
     */
    public long getCapacity() {
        return _file.getCapacity();
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
    public SparseStream getContent() {
        return _file.getContent();
    }

    /**
     * Gets the type of disk represented by this object.
     */
    public VirtualDiskClass getDiskClass() {
        return _file.getDiskType();
    }

    /**
     * Gets information about the type of disk.
     * This property provides access to meta-data about the disk format, for
     * example whether the
     * BIOS geometry is preserved in the disk file.
     */
    public VirtualDiskTypeInfo getDiskTypeInfo() {
        return DiskFactory.makeDiskTypeInfo();
    }

    /**
     * Gets the geometry of the disk.
     */
    public Geometry getGeometry() {
        return _file.getGeometry();
    }

    /**
     * Gets the layers that make up the disk.
     */
    public List<VirtualDiskLayer> getLayers() {
        return Arrays.asList(_file);
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
     *            {@code null}
     *            for default.
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
    public VirtualDisk createDifferencingDisk(DiscFileSystem fileSystem, String path) {
        throw new UnsupportedOperationException("Differencing disks not supported for raw disks");
    }

    /**
     * Create a new differencing disk.
     *
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    public VirtualDisk createDifferencingDisk(String path) {
        throw new UnsupportedOperationException("Differencing disks not supported for raw disks");
    }

    /**
     * Disposes of underlying resources.
     */
    public void close() throws IOException {
        try {
            if (_file != null) {
                _file.close();
            }

            _file = null;
        } finally {
            super.close();
        }
    }
}
