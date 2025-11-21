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

package discUtils.iscsi;

/**
 * Class representing the capacity of a LUN.
 */
public class LunCapacity {

    /**
     * Initializes a new instance of the LunCapacity class.
     *
     * @param logicalBlockCount The number of logical blocks.
     * @param blockSize The size of each block.
     */
    public LunCapacity(long logicalBlockCount, int blockSize) {
        this.logicalBlockCount = logicalBlockCount;
        this.blockSize = blockSize;
    }

    /**
     * Gets the size of each logical block.
     */
    private final int blockSize;

    public int getBlockSize() {
        return blockSize;
    }

    /**
     * Gets the number of logical blocks in the LUN.
     */
    private final long logicalBlockCount;

    public long getLogicalBlockCount() {
        return logicalBlockCount;
    }

    /**
     * Gets the capacity (in bytes) as a string.
     *
     * @return A string containing an integer.
     */
    public String toString() {
        return String.valueOf(blockSize * logicalBlockCount);
    }
}
