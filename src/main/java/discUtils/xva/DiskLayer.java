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

import discUtils.core.FileLocator;
import discUtils.core.Geometry;
import discUtils.core.VirtualDiskLayer;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Ownership;


/**
 * Class representing a single layer of an XVA disk.
 * XVA only supports a single layer.
 */
public final class DiskLayer extends VirtualDiskLayer {

    private final long capacity;

    private final String location;

    private final VirtualMachine vm;

    public DiskLayer(VirtualMachine vm, long capacity, String location) {
        this.vm = vm;
        this.capacity = capacity;
        this.location = location;
    }

    /**
     * Gets the capacity of the layer (in bytes).
     */
    @Override
    public long getCapacity() {
        return capacity;
    }

    /**
     * Gets the disk's geometry.
     * The geometry is not stored with the disk, so this is at best
     * a guess of the actual geometry.
     */
    @Override
    public Geometry getGeometry() {
        return Geometry.fromCapacity(capacity);
    }

    /**
     * Gets a indication of whether the disk is 'sparse'.
     * Always true for XVA disks.
     */
    @Override
    public boolean isSparse() {
        return true;
    }

    /**
     * Gets a value indicating whether the file is a differencing disk.
     */
    @Override
    public boolean needsParent() {
        return false;
    }

    @Override
    public FileLocator getRelativeFileLocator() {
        return null;
    }

    /**
     * Opens the content of the disk layer as a stream.
     *
     * @param parent The parent file's content (if any).
     * @param ownsParent Whether the created stream assumes ownership of parent
     *            stream.
     * @return The new content stream.
     */
    @Override
    public SparseStream openContent(SparseStream parent, Ownership ownsParent) {
        if (ownsParent == Ownership.Dispose && parent != null) {
            try {
                parent.close();
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }

        return new DiskStream(vm.getArchive(), capacity, location);
    }

    /**
     * Gets the possible locations of the parent file (if any).
     *
     * @return list of strings, empty if no parent.
     */
    @Override
    public List<String> getParentLocations() {
        return Collections.emptyList();
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
    }
}
