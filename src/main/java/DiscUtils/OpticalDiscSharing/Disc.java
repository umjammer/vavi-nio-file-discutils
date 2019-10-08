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

package DiscUtils.OpticalDiscSharing;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VirtualDiskClass;
import DiscUtils.Core.VirtualDiskLayer;
import DiscUtils.Core.VirtualDiskTypeInfo;
import DiscUtils.Streams.SparseStream;


public final class Disc extends VirtualDisk {
    private DiscImageFile _file;

    public Disc(URI uri, String userName, String password) {
        _file = new DiscImageFile(uri, userName, password);
    }

    /**
     * Gets the sector size of the disk (2048 for optical discs).
     */
    public int getBlockSize() {
        return DiscImageFile.Mode1SectorSize;
    }

    /**
     * Gets the capacity of the disc (in bytes).
     */
    public long getCapacity() {
        return _file.getCapacity();
    }

    /**
     * Gets the content of the disc as a stream.
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
        return VirtualDiskClass.OpticalDisk;
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
     * Gets the geometry of the disk.
     */
    public Geometry getGeometry() {
        return _file.getGeometry();
    }

    /**
     * Gets the layers that make up the disc.
     */
    public List<VirtualDiskLayer> getLayers() {
        return Arrays.asList(_file);
    }

    /**
     * Not supported for Optical Discs.
     *
     * @param fileSystem The file system to create the disc on.
     * @param path The path (or URI) for the disk to create.
     * @return Not Applicable.
     */
    public VirtualDisk createDifferencingDisk(DiscFileSystem fileSystem, String path) {
        throw new UnsupportedOperationException("Differencing disks not supported for optical disks");
    }

    /**
     * Not supported for Optical Discs.
     *
     * @param path The path (or URI) for the disk to create.
     * @return Not Applicable.
     */
    public VirtualDisk createDifferencingDisk(String path) {
        throw new UnsupportedOperationException("Differencing disks not supported for optical disks");
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
