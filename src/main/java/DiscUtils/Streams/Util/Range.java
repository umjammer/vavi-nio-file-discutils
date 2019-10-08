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

package DiscUtils.Streams.Util;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents a range of values.
 * The type of the offset element.The type of the size element.
 */
public class Range {
    /**
     * Initializes a new instance of the Range class.
     *
     * @param offset The offset (i.e. start) of the range.
     * @param count The size of the range.
     */
    public Range(long offset, long count) {
        __Offset = offset;
        __Count = count;
    }

    /**
     * Gets the size of the range.
     */
    private long __Count;

    public long getCount() {
        return __Count;
    }

    /**
     * Gets the offset (i.e. start) of the range.
     */
    private long __Offset;

    public long getOffset() {
        return __Offset;
    }

    /**
     * Compares this range to another.
     *
     * @param other The range to compare.
     * @return
     *         {@code true}
     *         if the ranges are equivalent, else
     *         {@code false}
     *         .
     */
    public boolean equals(Range other) {
        if (other == null) {
            return false;
        }

        return getOffset() == other.getOffset() && getCount() == other.getCount();
    }

    /**
     * Merges sets of ranges into chunks.
     *
     * @param ranges The ranges to merge.
     * @param chunkSize The size of each chunk.
     * @return Ranges combined into larger chunks.The type of the offset and
     *         count in the ranges.
     */
    public static List<Range> chunked(List<Range> ranges, long chunkSize) {
        List<Range> result = new ArrayList<>();
        long chunkStart = 0;
        long chunkLength = 0;
        for (Range range : ranges) {
            if (range.getCount() != 0) {
                long rangeStart = MathUtilities.roundDown(range.getOffset(), chunkSize);
                long rangeNext = MathUtilities.roundUp(range.getOffset() + range.getCount(), chunkSize);
                if (chunkStart != 0 && rangeStart > chunkStart + chunkLength) {

                    // This extent is non-contiguous (in terms of blocks), so write out the last range and start new
                    chunkStart = rangeStart;
                } else if (chunkStart == 0) {
                    // First extent, so start first range
                    result.add(new Range(chunkStart, chunkLength));
                    chunkStart = rangeStart;
                }

                // Set the length of the current range, based on the end of this extent
                chunkLength = rangeNext - chunkStart;
            }

        }
        // Final range (if any ranges at all) hasn't been returned yet, so do that now
        if (chunkStart != 0) {
            result.add(new Range(chunkStart, chunkLength));
        }
        return result;
    }

    /**
     * Returns a string representation of the extent as [start:+length].
     *
     * @return The string representation.
     */
    public String toString() {
        return "[" + getOffset() + ":+" + getCount() + "]";
    }
}
