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

package DiscUtils.Streams.Block;

import DiscUtils.Streams.Util.Sizes;


/**
 * Settings controlling BlockCache instances.
 */
public final class BlockCacheSettings {
    /**
     * Initializes a new instance of the BlockCacheSettings class.
     */
    public BlockCacheSettings() {
        setBlockSize((int) (4 * Sizes.OneKiB));
        setReadCacheSize(4 * Sizes.OneMiB);
        setLargeReadSize(64 * Sizes.OneKiB);
        setOptimumReadSize((int) (64 * Sizes.OneKiB));
    }

    /**
     * Initializes a new instance of the BlockCacheSettings class.
     * 
     * @param settings The cache settings.
     */
    public BlockCacheSettings(BlockCacheSettings settings) {
        setBlockSize(settings.getBlockSize());
        setReadCacheSize(settings.getReadCacheSize());
        setLargeReadSize(settings.getLargeReadSize());
        setOptimumReadSize(settings.getOptimumReadSize());
    }

    /**
     * Gets or sets the size (in bytes) of each cached block.
     */
    private int __BlockSize;

    public int getBlockSize() {
        return __BlockSize;
    }

    public void setBlockSize(int value) {
        __BlockSize = value;
    }

    /**
     * Gets or sets the maximum read size that will be cached.
     * Large reads are not cached, on the assumption they will not
     * be repeated. This setting controls what is considered 'large'.
     * Any read that is more than this many bytes will not be cached.
     */
    private long __LargeReadSize;

    public long getLargeReadSize() {
        return __LargeReadSize;
    }

    public void setLargeReadSize(long value) {
        __LargeReadSize = value;
    }

    /**
     * Gets or sets the optimum size of a read to the wrapped stream.
     * This value must be a multiple of BlockSize.
     */
    private int __OptimumReadSize;

    public int getOptimumReadSize() {
        return __OptimumReadSize;
    }

    public void setOptimumReadSize(int value) {
        __OptimumReadSize = value;
    }

    /**
     * Gets or sets the size (in bytes) of the read cache.
     */
    private long __ReadCacheSize;

    public long getReadCacheSize() {
        return __ReadCacheSize;
    }

    public void setReadCacheSize(long value) {
        __ReadCacheSize = value;
    }

}
