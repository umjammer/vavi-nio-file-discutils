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
import java.io.IOException;

import discUtils.streams.MappedStream;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Ownership;


/**
 * base class represented a stored extent of a virtual disk.
 *
 * Some file formats can divide a logical disk layer into multiple extents,
 * stored in
 * different files. This class represents those extents. Normally, all virtual
 * disks
 * have at least one extent.
 */
public abstract class VirtualDiskExtent implements Closeable {
    /**
     * Gets the capacity of the extent (in bytes).
     */
    public abstract long getCapacity();

    /**
     * Gets a value indicating whether the extent only stores meaningful
     * sectors.
     */
    public abstract boolean isSparse();

    /**
     * Gets the size of the extent (in bytes) on underlying storage.
     */
    public abstract long getStoredSize();

    /**
     * Disposes of this instance, freeing underlying resources.
     */
    public void close() {
    }

    /**
     * Gets the content of this extent.
     *
     * @param parent The parent stream (if any).
     * @param ownsParent Controls ownership of the parent stream.
     * @return The content as a stream.
     */
    public abstract MappedStream openContent(SparseStream parent, Ownership ownsParent) throws IOException;
}
