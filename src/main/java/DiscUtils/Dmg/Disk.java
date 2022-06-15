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

package DiscUtils.Dmg;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VirtualDiskClass;
import DiscUtils.Core.VirtualDiskLayer;
import DiscUtils.Core.VirtualDiskTypeInfo;
import DiscUtils.Core.Partitions.PartitionTable;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Util.Ownership;
import dotnet4j.io.Stream;


/**
 * Represents a DMG (aka UDIF) backed disk.
 */
public class Disk extends VirtualDisk {
    private SparseStream _content;

    private DiskImageFile _file;

    /**
     * Initializes a new instance of the Disk class.
     *
     * @param stream The stream containing the disk.
     * @param ownsStream Whether the new instance takes ownership of stream.
     */
    public Disk(Stream stream, Ownership ownsStream) {
        _file = new DiskImageFile(stream, ownsStream);
    }

    /**
     * Gets the capacity of the disk (in bytes).
     */
    public long getCapacity() {
        return _file.getCapacity();
    }

    /**
     * Gets the content of the disk as a stream.
     *
     * Note the returned stream is not guaranteed to be at any particular
     * position. The actual position
     * will depend on the last partition table/file system activity, since all
     * access to the disk contents pass
     * through a single stream instance. Set the stream position before
     * accessing the stream.
     */
    public SparseStream getContent() {
        if (_content == null) {
            _content = _file.openContent(null, Ownership.None);
        }

        return _content;
    }

    /**
     * Gets the type of disk represented by this object.
     */
    public VirtualDiskClass getDiskClass() {
        return VirtualDiskClass.HardDisk;
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
        return Collections.singletonList(_file);
    }

    public PartitionTable getPartitions() {
        return new UdifPartitionTable(this, _file.getBuffer());
    }

    /**
     * Create a new differencing disk, possibly within an existing disk.
     *
     * @param fileSystem The file system to create the disk on.
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    public VirtualDisk createDifferencingDisk(DiscFileSystem fileSystem, String path) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a new differencing disk.
     *
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    public VirtualDisk createDifferencingDisk(String path) {
        throw new UnsupportedOperationException();
    }

    /**
     * Disposes of this instance, freeing underlying resources.
     */
    public void close() throws IOException {
        try {
            if (_content != null) {
                _content.close();
                _content = null;
            }

            if (_file != null) {
                _file.close();
                _file = null;
            }
        } finally {
            super.close();
        }
    }
}
