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
        _blockSize = (int) (4 * Sizes.OneKiB);
        _readCacheSize = 4 * Sizes.OneMiB;
        _largeReadSize = 64 * Sizes.OneKiB;
        _optimumReadSize = (int) (64 * Sizes.OneKiB);
    }

    /**
     * Initializes a new instance of the BlockCacheSettings class.
     *
     * @param settings The cache settings.
     */
    public BlockCacheSettings(BlockCacheSettings settings) {
        _blockSize = settings._blockSize;
        _readCacheSize = settings._readCacheSize;
        _largeReadSize = settings._largeReadSize;
        _optimumReadSize = settings._optimumReadSize;
    }

    /**
     * Gets or sets the size (in bytes) of each cached block.
     */
    private int _blockSize;

    public int getBlockSize() {
        return _blockSize;
    }

    public void setBlockSize(int value) {
        _blockSize = value;
    }

    /**
     * Gets or sets the maximum read size that will be cached.
     * Large reads are not cached, on the assumption they will not
     * be repeated. This setting controls what is considered 'large'.
     * Any read that is more than this many bytes will not be cached.
     */
    private long _largeReadSize;

    public long getLargeReadSize() {
        return _largeReadSize;
    }

    public void setLargeReadSize(long value) {
        _largeReadSize = value;
    }

    /**
     * Gets or sets the optimum size of a read to the wrapped stream.
     * This value must be a multiple of BlockSize.
     */
    private int _optimumReadSize;

    public int getOptimumReadSize() {
        return _optimumReadSize;
    }

    public void setOptimumReadSize(int value) {
        _optimumReadSize = value;
    }

    /**
     * Gets or sets the size (in bytes) of the read cache.
     */
    private long _readCacheSize;

    public long getReadCacheSize() {
        return _readCacheSize;
    }

    public void setReadCacheSize(long value) {
        _readCacheSize = value;
    }
}
