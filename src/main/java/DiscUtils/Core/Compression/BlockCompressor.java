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

package DiscUtils.Core.Compression;

/**
 * Base class for block compression algorithms.
 */
public abstract class BlockCompressor {
    /**
     * Gets or sets the block size parameter to the algorithm.
     *
     * Some algorithms may use this to control both compression and
     * decompression, others may only use it to control compression. Some may
     * ignore it entirely.
     */
    private int _blockSize;

    public int getBlockSize() {
        return _blockSize;
    }

    public void setBlockSize(int value) {
        _blockSize = value;
    }

    /**
     * Compresses some data.
     *
     * @param source The uncompressed input.
     * @param sourceOffset Offset of the input data in {@code source} .
     * @param sourceLength The amount of uncompressed data.
     * @param compressed The destination for the output compressed data.
     * @param compressedOffset Offset for the output data in {@code compressed}.
     * @param compressedLength The maximum size of the compressed data on input,
     *            and the actual size on output.
     * @return Indication of success, or indication the data could not compress
     *         into the requested space.
     */
    public abstract CompressionResult compress(byte[] source,
                                               int sourceOffset,
                                               int sourceLength,
                                               byte[] compressed,
                                               int compressedOffset,
                                               int[] compressedLength);

    /**
     * Decompresses some data.
     *
     * @param source The compressed input.
     * @param sourceOffset Offset of the input data in {@code source} .
     * @param sourceLength The amount of compressed data.
     * @param decompressed The destination for the output decompressed data.
     * @param decompressedOffset Offset for the output data in
     *            {@code decompressed} .
     * @return The amount of decompressed data.
     */
    public abstract int decompress(byte[] source,
                                   int sourceOffset,
                                   int sourceLength,
                                   byte[] decompressed,
                                   int decompressedOffset);
}
