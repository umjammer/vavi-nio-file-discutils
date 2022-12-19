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

package discUtils.xva;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.github.fge.filesystem.exceptions.UnsupportedOptionException;
import discUtils.core.DiscFileSystem;
import discUtils.core.Geometry;
import discUtils.core.VirtualDisk;
import discUtils.core.VirtualDiskClass;
import discUtils.core.VirtualDiskLayer;
import discUtils.core.VirtualDiskTypeInfo;
import discUtils.streams.SparseStream;


/**
 * Class representing a disk containing within an XVA file.
 */
public final class Disk extends VirtualDisk {

    private final long capacity;

    private final String location;

    private final VirtualMachine vm;

    private SparseStream content;

    public Disk(VirtualMachine vm, String id, String displayname, String location, long capacity) {
        this.vm = vm;
        uuid = id;
        displayName = displayname;
        this.location = location;
        this.capacity = capacity;
    }

    /**
     * Gets the disk's capacity (in bytes).
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * Gets the content of the disk as a stream.
     */
    public SparseStream getContent() {
        if (content == null) {
            content = new DiskStream(vm.getArchive(), capacity, location);
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
    private String displayName;

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the disk's geometry.
     * The geometry is not stored with the disk, so this is at best
     * a guess of the actual geometry.
     */
    public Geometry getGeometry() {
        return Geometry.fromCapacity(capacity);
    }

    /**
     * Gets the (single) layer of an XVA disk.
     */
    public List<VirtualDiskLayer> getLayers() {
        return Collections.singletonList(new DiskLayer(vm, capacity, location));
    }

    /**
     * Gets the Unique id of the disk, as known by XenServer.
     */
    private String uuid;

    public String getUuid() {
        return uuid;
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
        if (content != null) {
            content.close();
            content = null;
        }

        super.close();
    }
}
