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
 */
public class Range {
    /**
     * Initializes a new instance of the Range class.
     *
     * @param offset The offset (i.e. start) of the range.
     * @param count The size of the range.
     */
    public Range(long offset, long count) {
        this.offset = offset;
        this.count = count;
    }

    private long count;

    /**
     * Gets the size of the range.
     */
    public long getCount() {
        return count;
    }

    private long offset;

    /**
     * Gets the offset (i.e. start) of the range.
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Compares this range to another.
     *
     * @param other The range to compare.
     * @return {@code true} if the ranges are equivalent, else {@code false} .
     */
    public boolean equals(Range other) {
        if (other == null) {
            return false;
        }

        return offset == other.offset && count == other.count;
    }

    /**
     * Merges sets of ranges into chunks.
     *
     * @param ranges The ranges to merge.
     * @param chunkSize The size of each chunk.
     * @return Ranges combined into larger chunks.
     */
    public static List<Range> chunked(List<Range> ranges, long chunkSize) {
        List<Range> result = new ArrayList<>();
        long chunkStart = -1;
        long chunkLength = 0;

        for (Range range : ranges) {
            if (range.getCount() != 0) {
                long rangeStart = MathUtilities.roundDown(range.getOffset(), chunkSize);
                long rangeNext = MathUtilities.roundUp(range.getOffset() + range.getCount(), chunkSize);

                if (chunkStart != -1 && rangeStart > chunkStart + chunkLength) {
                    // This extent is non-contiguous (in terms of blocks), so write out the last
                    // range and start new
                    chunkStart = rangeStart;
                } else if (chunkStart == -1) {
                    // First extent, so start first range
                    result.add(new Range(chunkStart, chunkLength));
                    chunkStart = rangeStart;
                }

                // Set the length of the current range, based on the end of this extent
                chunkLength = rangeNext - chunkStart;
            }
        }

        // Final range (if any ranges at all) hasn't been returned yet, so do
        // that now
        if (chunkStart != -1) {
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
        return "Range: {" + offset + ":+" + count + "}";
    }
}
