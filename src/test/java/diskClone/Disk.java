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

package diskClone;

import java.io.IOException;
import java.util.List;

import discUtils.core.DiscFileSystem;
import discUtils.core.Geometry;
import discUtils.core.VirtualDisk;
import discUtils.core.VirtualDiskClass;
import discUtils.core.VirtualDiskLayer;
import discUtils.core.VirtualDiskTypeInfo;
import discUtils.streams.SparseStream;

import com.sun.jna.Pointer;

public class Disk extends VirtualDisk {

    private String path;

    private Pointer handle;

    private SparseStream stream;

    public Disk(int number) {
        path = "\\\\.\\PhysicalDrive" + number;
        handle = Win32Wrapper.openFileHandle(path);
    }

    public void close() throws IOException {
        if (stream != null) {
            stream.close();
            stream = null;
        }

        if (!handle.IsClosed) {
            handle.close();
        }

        super.close();
    }

    public SparseStream getContent() {
        if (stream == null) {
            stream = new DiskStream(handle);
        }

        return stream;
    }

    public Geometry getGeometry() {
        return Geometry.fromCapacity(getCapacity());
    }

    public Geometry getBiosGeometry() {
        diskClone.NativeMethods.DiskGeometry diskGeometry = Win32Wrapper.getDiskGeometry(handle);
        return new Geometry((int) diskGeometry.cylinders,
                            diskGeometry.tracksPerCylinder,
                            diskGeometry.sectorsPerTrack,
                            diskGeometry.bytesPerSector);
    }

    public VirtualDiskClass getDiskClass() {
        return VirtualDiskClass.HardDisk;
    }

    public long getCapacity() {
        return Win32Wrapper.getDiskCapacity(handle);
    }

    public List<VirtualDiskLayer> getLayers() {
        throw new UnsupportedOperationException();
    }

    public VirtualDiskTypeInfo getDiskTypeInfo() {
        return new VirtualDiskTypeInfo();
    }

    public VirtualDisk createDifferencingDisk(DiscFileSystem fileSystem, String path) {
        throw new UnsupportedOperationException();
    }

    public VirtualDisk createDifferencingDisk(String path) {
        throw new UnsupportedOperationException();
    }
}
