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

import DiscUtils.Core.FileLocator;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.VirtualDiskLayer;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Block.BlockCacheSettings;
import DiscUtils.Streams.Block.BlockCacheStream;
import DiscUtils.Streams.Buffer.BufferStream;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import moe.yo3explorer.dotnetio4j.FileAccess;


public final class DiscImageFile extends VirtualDiskLayer {
    public static final int Mode1SectorSize = 2048;

    public DiscImageFile(URI uri, String userName, String password) {
        __Content = new BufferStream(new DiscContentBuffer(uri, userName, password), FileAccess.Read);

        BlockCacheSettings cacheSettings = new BlockCacheSettings();
        cacheSettings.setBlockSize((int) (32 * Sizes.OneKiB));
        cacheSettings.setOptimumReadSize((int) (128 * Sizes.OneKiB));

        __Content = new BlockCacheStream(getContent(), Ownership.Dispose);
    }

    public long getCapacity() {
        return getContent().getLength();
    }

    private SparseStream __Content;

    public SparseStream getContent() {
        return __Content;
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
                throw new moe.yo3explorer.dotnetio4j.IOException(e);
            }
        }

        return SparseStream.fromStream(getContent(), Ownership.None);
    }

    public List<String> getParentLocations() {
        return Arrays.asList();
    }
}
