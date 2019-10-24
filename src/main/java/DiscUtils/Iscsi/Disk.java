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

package DiscUtils.Iscsi;

import java.util.Arrays;
import java.util.List;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VirtualDiskClass;
import DiscUtils.Core.VirtualDiskLayer;
import DiscUtils.Core.VirtualDiskTypeInfo;
import DiscUtils.Core.Partitions.BiosPartitionTable;
import DiscUtils.Streams.SparseStream;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;


/**
 * Represents a disk accessed via iSCSI.
 */
public class Disk extends VirtualDisk {
    private final FileAccess _access;

    private LunCapacity _capacity;

    private final long _lun;

    private final Session _session;

    private DiskStream _stream;

    public Disk(Session session, long lun, FileAccess access) {
        _session = session;
        _lun = lun;
        _access = access;
    }

    /**
     * Gets the size of the disk's logical blocks (in bytes).
     */
    public int getBlockSize() {
        if (_capacity == null) {
            _capacity = _session.getCapacity(_lun);
        }

        return _capacity.getBlockSize();
    }

    /**
     * The capacity of the disk.
     */
    public long getCapacity() {
        if (_capacity == null) {
            _capacity = _session.getCapacity(_lun);
        }

        return _capacity.getBlockSize() * _capacity.getLogicalBlockCount();
    }

    /**
     * Gets a stream that provides access to the disk's content.
     */
    public SparseStream getContent() {
        if (_stream == null) {
            _stream = new DiskStream(_session, _lun, _access);
        }

        return _stream;
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
        return new VirtualDiskTypeInfo();
    }

    /**
     * The Geometry of the disk.
     */
    public Geometry getGeometry() {
        // We detect the geometry (which will return a sensible default if the disk has no partitions).
        // We don't rely on asking the iSCSI target for the geometry because frequently values are returned
        // that are not valid as BIOS disk geometries.
        Stream stream = getContent();
        long pos = stream.getPosition();
        Geometry result = BiosPartitionTable.detectGeometry(stream);
        stream.setPosition(pos);
        return result;
    }

    /**
     * Gets the disk layers that constitute the disk.
     */
    public List<VirtualDiskLayer> getLayers() {
        return Arrays.asList();
    }

    /**
     * Create a new differencing disk, possibly within an existing disk.
     *
     * @param fileSystem The file system to create the disk on.
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    public VirtualDisk createDifferencingDisk(DiscFileSystem fileSystem, String path) {
        throw new UnsupportedOperationException("Differencing disks not supported for iSCSI disks");
    }

    /**
     * Create a new differencing disk.
     *
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    public VirtualDisk createDifferencingDisk(String path) {
        throw new UnsupportedOperationException("Differencing disks not supported for iSCSI disks");
    }
}
