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
import java.util.Collections;
import java.util.List;

import DiscUtils.Core.FileLocator;
import DiscUtils.Core.FloppyDiskType;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.VirtualDiskClass;
import DiscUtils.Core.VirtualDiskLayer;
import DiscUtils.Core.Partitions.BiosPartitionTable;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * Represents a single raw disk image file.
 */
public final class DiskImageFile extends VirtualDiskLayer {
    private Ownership _ownsContent;

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param stream The stream to interpret.
     */
    public DiskImageFile(Stream stream) {
        this(stream, Ownership.None, null);
    }

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param stream The stream to interpret.
     * @param ownsStream Indicates if the new instance should control the
     *            lifetime of the stream.
     * @param geometry The emulated geometry of the disk.
     */
    public DiskImageFile(Stream stream, Ownership ownsStream, Geometry geometry) {
        setContent(stream instanceof SparseStream ? (SparseStream) stream : (SparseStream) null);
        _ownsContent = ownsStream;
        if (getContent() == null) {
            setContent(SparseStream.fromStream(stream, ownsStream));
            _ownsContent = Ownership.Dispose;
        }

        __Geometry = geometry != null ? geometry : detectGeometry(getContent());
    }

    public long getCapacity() {
        return getContent().getLength();
    }

    private SparseStream __Content;

    public SparseStream getContent() {
        return __Content;
    }

    public void setContent(SparseStream value) {
        __Content = value;
    }

    /**
     * Gets the type of disk represented by this object.
     */
    public VirtualDiskClass getDiskType() {
        return detectDiskType(getCapacity());
    }

    /**
     * Gets the geometry of the file.
     */
    private Geometry __Geometry;

    public Geometry getGeometry() {
        return __Geometry;
    }

    /**
     * Gets a value indicating if the layer only stores meaningful sectors.
     */
    public boolean isSparse() {
        return false;
    }

    /**
     * Gets a value indicating whether the file is a differencing disk.
     */
    public boolean needsParent() {
        return false;
    }

    public FileLocator getRelativeFileLocator() {
        return null;
    }

    /**
     * Initializes a stream as a raw disk image.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @param geometry The geometry of the new disk.
     * @return An object that accesses the stream as a raw disk image.
     */
    public static DiskImageFile initialize(Stream stream, Ownership ownsStream, long capacity, Geometry geometry) {
        stream.setLength(MathUtilities.roundUp(capacity, Sizes.Sector));
        // Wipe any pre-existing master boot record / BPB
        stream.setPosition(0);
        stream.write(new byte[Sizes.Sector], 0, Sizes.Sector);
        stream.setPosition(0);
        return new DiskImageFile(stream, ownsStream, geometry);
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
    public static DiskImageFile initialize(Stream stream, Ownership ownsStream, FloppyDiskType type) {
        return initialize(stream, ownsStream, floppyCapacity(type), null);
    }

    /**
     * Gets the content of this layer.
     *
     * @param parent The parent stream (if any).
     * @param ownsParent Controls ownership of the parent stream.
     * @return The content as a stream.
     */
    public SparseStream openContent(SparseStream parent, Ownership ownsParent) {
        if (ownsParent == Ownership.Dispose && parent != null) {
            try {
                parent.close();
            } catch (IOException e) {
                throw new moe.yo3explorer.dotnetio4j.IOException(e);
            }
        }

        return SparseStream.fromStream(getContent(), Ownership.None);
    }

    /**
     * Gets the possible locations of the parent file (if any).
     *
     * @return Array of strings, empty if no parent.
     */
    public List<String> getParentLocations() {
        return Collections.EMPTY_LIST;
    }

    /**
     * Disposes of underlying resources.
     */
    public void close() throws IOException {
        try {
            if (_ownsContent == Ownership.Dispose && getContent() != null) {
                getContent().close();
            }

            setContent(null);

        } finally {
            super.close();
        }
    }

    /**
     * Calculates the best guess geometry of a disk.
     *
     * @param disk The disk to detect the geometry of.
     * @return The geometry of the disk.
     */
    private static Geometry detectGeometry(Stream disk) {
        long capacity = disk.getLength();
        // First, check for floppy disk capacities - these have well-defined geometries
        if (capacity == Sizes.Sector * 1440) {
            return new Geometry(80, 2, 9);
        }

        if (capacity == Sizes.Sector * 2880) {
            return new Geometry(80, 2, 18);
        }

        if (capacity == Sizes.Sector * 5760) {
            return new Geometry(80, 2, 36);
        }

        return BiosPartitionTable.detectGeometry(disk);
    }

    // Failing that, try to detect the geometry from any partition table.
    // Note: this call falls back to guessing the geometry from the capacity

    /**
     * Calculates the best guess disk type (i.e. floppy or hard disk).
     *
     * @param capacity The capacity of the disk.
     * @return The disk type.
     */
    private static VirtualDiskClass detectDiskType(long capacity) {
        if (capacity == Sizes.Sector * 1440 || capacity == Sizes.Sector * 2880 || capacity == Sizes.Sector * 5760) {
            return VirtualDiskClass.FloppyDisk;
        }

        return VirtualDiskClass.HardDisk;
    }

    private static long floppyCapacity(FloppyDiskType type) {
        switch (type) {
        case DoubleDensity:
            return Sizes.Sector * 1440;
        case HighDensity:
            return Sizes.Sector * 2880;
        case Extended:
            return Sizes.Sector * 5760;
        default:
            throw new IllegalArgumentException("Invalid floppy disk type " + type);

        }
    }
}
