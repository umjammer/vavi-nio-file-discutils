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

package DiscUtils.Xva;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.github.fge.filesystem.exceptions.UnsupportedOptionException;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VirtualDiskClass;
import DiscUtils.Core.VirtualDiskLayer;
import DiscUtils.Core.VirtualDiskTypeInfo;
import DiscUtils.Streams.SparseStream;


/**
 * Class representing a disk containing within an XVA file.
 */
public final class Disk extends VirtualDisk {
    private final long _capacity;

    private final String _location;

    private final VirtualMachine _vm;

    private SparseStream _content;

    public Disk(VirtualMachine vm, String id, String displayname, String location, long capacity) {
        _vm = vm;
        __Uuid = id;
        __DisplayName = displayname;
        _location = location;
        _capacity = capacity;
    }

    /**
     * Gets the disk's capacity (in bytes).
     */
    public long getCapacity() {
        return _capacity;
    }

    /**
     * Gets the content of the disk as a stream.
     */
    public SparseStream getContent() {
        if (_content == null) {
            _content = new DiskStream(_vm.getArchive(), _capacity, _location);
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
     * Gets the display name of the disk, as shown by XenServer.
     */
    private String __DisplayName;

    public String getDisplayName() {
        return __DisplayName;
    }

    /**
     * Gets the disk's geometry.
     * The geometry is not stored with the disk, so this is at best
     * a guess of the actual geometry.
     */
    public Geometry getGeometry() {
        return Geometry.fromCapacity(_capacity);
    }

    /**
     * Gets the (single) layer of an XVA disk.
     */
    public List<VirtualDiskLayer> getLayers() {
        return Arrays.asList(new DiskLayer(_vm, _capacity, _location));
    }

    /**
     * Gets the Unique id of the disk, as known by XenServer.
     */
    private String __Uuid;

    public String getUuid() {
        return __Uuid;
    }

    /**
     * Create a new differencing disk, possibly within an existing disk.
     *
     * @param fileSystem The file system to create the disk on.
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    public VirtualDisk createDifferencingDisk(DiscFileSystem fileSystem, String path) {
        throw new UnsupportedOptionException("Differencing disks not supported by XVA format");
    }

    /**
     * Create a new differencing disk.
     *
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    public VirtualDisk createDifferencingDisk(String path) {
        throw new UnsupportedOptionException("Differencing disks not supported by XVA format");
    }

    /**
     * Disposes of this instance, freeing underlying resources.
     */
    public void close() throws IOException {
        if (_content != null) {
            _content.close();
            _content = null;
        }

        super.close();
    }
}
