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

package discUtils.dmg;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import discUtils.core.DiscFileSystem;
import discUtils.core.Geometry;
import discUtils.core.VirtualDisk;
import discUtils.core.VirtualDiskClass;
import discUtils.core.VirtualDiskLayer;
import discUtils.core.VirtualDiskTypeInfo;
import discUtils.core.partitions.PartitionTable;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Ownership;
import dotnet4j.io.Stream;


/**
 * Represents a DMG (aka UDIF) backed disk.
 */
public class Disk extends VirtualDisk {

    private SparseStream content;

    private DiskImageFile file;

    /**
     * Initializes a new instance of the Disk class.
     *
     * @param stream The stream containing the disk.
     * @param ownsStream Whether the new instance takes ownership of stream.
     */
    public Disk(Stream stream, Ownership ownsStream) {
        file = new DiskImageFile(stream, ownsStream);
    }

    /**
     * Gets the capacity of the disk (in bytes).
     */
    @Override
    public long getCapacity() {
        return file.getCapacity();
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
    @Override
    public SparseStream getContent() {
        if (content == null) {
            content = file.openContent(null, Ownership.None);
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
     * Gets information about the type of disk.
     * This property provides access to meta-data about the disk format, for
     * example whether the
     * BIOS geometry is preserved in the disk file.
     */
    @Override
    public VirtualDiskTypeInfo getDiskTypeInfo() {
        return DiskFactory.makeDiskTypeInfo();
    }

    /**
     * Gets the geometry of the disk.
     */
    @Override
    public Geometry getGeometry() {
        return file.getGeometry();
    }

    /**
     * Gets the layers that make up the disk.
     */
    @Override
    public List<VirtualDiskLayer> getLayers() {
        return Collections.singletonList(file);
    }

    @Override
    public PartitionTable getPartitions() {
        return new UdifPartitionTable(this, file.getBuffer());
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
        throw new UnsupportedOperationException();
    }

    /**
     * Create a new differencing disk.
     *
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    @Override
    public VirtualDisk createDifferencingDisk(String path) {
        throw new UnsupportedOperationException();
    }

    /**
     * Disposes of this instance, freeing underlying resources.
     */
    @Override
    public void close() throws IOException {
        try {
            if (content != null) {
                content.close();
                content = null;
            }

            if (file != null) {
                file.close();
                file = null;
            }
        } finally {
            super.close();
        }
    }
}
