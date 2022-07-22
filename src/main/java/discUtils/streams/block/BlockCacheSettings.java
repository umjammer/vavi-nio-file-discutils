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

package discUtils.streams.block;

import discUtils.streams.util.Sizes;


/**
 * Settings controlling BlockCache instances.
 */
public final class BlockCacheSettings {

    /**
     * Initializes a new instance of the BlockCacheSettings class.
     */
    public BlockCacheSettings() {
        blockSize = (int) (4 * Sizes.OneKiB);
        readCacheSize = 4 * Sizes.OneMiB;
        largeReadSize = 64 * Sizes.OneKiB;
        optimumReadSize = (int) (64 * Sizes.OneKiB);
    }

    /**
     * Initializes a new instance of the BlockCacheSettings class.
     *
     * @param settings The cache settings.
     */
    public BlockCacheSettings(BlockCacheSettings settings) {
        blockSize = settings.blockSize;
        readCacheSize = settings.readCacheSize;
        largeReadSize = settings.largeReadSize;
        optimumReadSize = settings.optimumReadSize;
    }

    /**
     * Gets or sets the size (in bytes) of each cached block.
     */
    private int blockSize;

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int value) {
        blockSize = value;
    }

    /**
     * Gets or sets the maximum read size that will be cached.
     * Large reads are not cached, on the assumption they will not
     * be repeated. This setting controls what is considered 'large'.
     * Any read that is more than this many bytes will not be cached.
     */
    private long largeReadSize;

    public long getLargeReadSize() {
        return largeReadSize;
    }

    public void setLargeReadSize(long value) {
        largeReadSize = value;
    }

    /**
     * Gets or sets the optimum size of a read to the wrapped stream.
     * This value must be a multiple of BlockSize.
     */
    private int optimumReadSize;

    public int getOptimumReadSize() {
        return optimumReadSize;
    }

    public void setOptimumReadSize(int value) {
        optimumReadSize = value;
    }

    /**
     * Gets or sets the size (in bytes) of the read cache.
     */
    private long readCacheSize;

    public long getReadCacheSize() {
        return readCacheSize;
    }

    public void setReadCacheSize(long value) {
        readCacheSize = value;
    }
}
