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

import discUtils.core.FileLocator;
import discUtils.core.Geometry;
import discUtils.core.VirtualDiskLayer;
import discUtils.streams.SparseStream;
import discUtils.streams.block.BlockCacheSettings;
import discUtils.streams.block.BlockCacheStream;
import discUtils.streams.buffer.BufferStream;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Sizes;
import dotnet4j.io.FileAccess;


public final class DiscImageFile extends VirtualDiskLayer {

    public static final int Mode1SectorSize = 2048;

    public DiscImageFile(URI uri, String userName, String password) {
        content = new BufferStream(new DiscContentBuffer(uri, userName, password), FileAccess.Read);

        BlockCacheSettings cacheSettings = new BlockCacheSettings();
        cacheSettings.setBlockSize((int) (32 * Sizes.OneKiB));
        cacheSettings.setOptimumReadSize((int) (128 * Sizes.OneKiB));

        content = new BlockCacheStream(getContent(), Ownership.Dispose);
    }

    public long getCapacity() {
        return getContent().getLength();
    }

    private SparseStream content;

    public SparseStream getContent() {
        return content;
    }

    // Note external sector size is always 2048
    public Geometry getGeometry() {
        return new Geometry(1, 1, 1, Mode1SectorSize);
    }

    public boolean isSparse() {
        return false;
    }

    public boolean needsParent() {
        return false;
    }

    public FileLocator getRelativeFileLocator() {
        return null;
    }

    public SparseStream openContent(SparseStream parent, Ownership ownsParent) {
        if (ownsParent == Ownership.Dispose && parent != null) {
            try {
                parent.close();
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }

        return SparseStream.fromStream(getContent(), Ownership.None);
    }

    public List<String> getParentLocations() {
        return Collections.emptyList();
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
    }
}
