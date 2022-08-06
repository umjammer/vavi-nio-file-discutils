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

package discUtils.core;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import discUtils.streams.SparseStream;
import discUtils.streams.util.Ownership;


/**
 * Represents the base layer, or a differencing layer of a VirtualDisk.
 * VirtualDisks are composed of one or more layers - a base layer which
 * represents the entire disk (even if not all bytes are actually stored), and a
 * number of differencing layers that store the disk sectors that are logically
 * different to the base layer.Disk Layers may not store all sectors. Any
 * sectors that are not stored are logically zero's (for base layers), or holes
 * through to the layer underneath (all other layers).
 */
public abstract class VirtualDiskLayer implements Closeable {

    /**
     * Gets the capacity of the disk (in bytes).
     */
    public abstract long getCapacity();

    /**
     * Gets and sets the logical extents that make up this layer.
     */
    public List<VirtualDiskExtent> getExtents() {
        return new ArrayList<>();
    }

    /**
     * Gets the full path to this disk layer, or empty string.
     */
    public String getFullPath() {
        return "";
    }

    /**
     * Gets the geometry of the virtual disk layer.
     */
    public abstract Geometry getGeometry();

    /**
     * Gets a value indicating whether the layer only stores meaningful sectors.
     */
    public abstract boolean isSparse();

    /**
     * Gets a value indicating whether this is a differential disk.
     */
    public abstract boolean needsParent();

    /**
     * Gets a {@link FileLocator} that can resolve relative paths, or
     * {@code null}.
     *
     * Typically, used to locate parent disks.
     */
    public abstract FileLocator getRelativeFileLocator();

    /**
     * Finalizes an instance of the VirtualDiskLayer class.
     */
    protected void finalize() throws Throwable {
        close();
    }

    /**
     * Gets the content of this layer.
     *
     * @param parent The parent stream (if any).
     * @param ownsParent Controls ownership of the parent stream.
     * @return The content as a stream.
     */
    public abstract SparseStream openContent(SparseStream parent, Ownership ownsParent);

    /**
     * Gets the possible locations of the parent file (if any).
     *
     * @return Array of strings, empty if no parent.
     */
    public abstract List<String> getParentLocations();
}
