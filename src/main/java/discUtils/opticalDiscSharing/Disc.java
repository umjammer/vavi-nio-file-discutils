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

package discUtils.opticalDiscSharing;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import discUtils.core.DiscFileSystem;
import discUtils.core.Geometry;
import discUtils.core.VirtualDisk;
import discUtils.core.VirtualDiskClass;
import discUtils.core.VirtualDiskLayer;
import discUtils.core.VirtualDiskTypeInfo;
import discUtils.streams.SparseStream;


public final class Disc extends VirtualDisk {

    private DiscImageFile file;

    public Disc(URI uri, String userName, String password) {
        file = new DiscImageFile(uri, userName, password);
    }

    /**
     * Gets the sector size of the disk (2048 for optical discs).
     */
    @Override public int getBlockSize() {
        return DiscImageFile.Mode1SectorSize;
    }

    /**
     * Gets the capacity of the disc (in bytes).
     */
    @Override public long getCapacity() {
        return file.getCapacity();
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
    @Override public SparseStream getContent() {
        return file.getContent();
    }

    /**
     * Gets the type of disk represented by this object.
     */
    @Override public VirtualDiskClass getDiskClass() {
        return VirtualDiskClass.OpticalDisk;
    }

    /**
     * Gets information about the type of disk.
     * This property provides access to meta-data about the disk format, for
     * example whether the
     * BIOS geometry is preserved in the disk file.
     */
    @Override public VirtualDiskTypeInfo getDiskTypeInfo() {
        return new VirtualDiskTypeInfo();
    }

    /**
     * Gets the geometry of the disk.
     */
    @Override public Geometry getGeometry() {
        return file.getGeometry();
    }

    /**
     * Gets the layers that make up the disc.
     */
    @Override public List<VirtualDiskLayer> getLayers() {
        return Collections.singletonList(file);
    }

    /**
     * Not supported for Optical Discs.
     *
     * @param fileSystem The file system to create the disc on.
     * @param path The path (or URI) for the disk to create.
     * @return Not Applicable.
     */
    @Override public VirtualDisk createDifferencingDisk(DiscFileSystem fileSystem, String path) {
        throw new UnsupportedOperationException("Differencing disks not supported for optical disks");
    }

    /**
     * Not supported for Optical Discs.
     *
     * @param path The path (or URI) for the disk to create.
     * @return Not Applicable.
     */
    @Override public VirtualDisk createDifferencingDisk(String path) {
        throw new UnsupportedOperationException("Differencing disks not supported for optical disks");
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
