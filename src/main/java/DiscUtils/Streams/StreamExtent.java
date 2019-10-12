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

package DiscUtils.Streams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Range;


/**
 * Represents a range of bytes in a stream.
 * This is normally used to represent regions of a SparseStream that
 * are actually stored in the underlying storage medium (rather than implied
 * zero bytes). Extents are stored as a zero-based byte offset (from the
 * beginning of the stream), and a byte length.
 */
public final class StreamExtent implements Comparable<StreamExtent> {
    /**
     * Initializes a new instance of the StreamExtent class.
     *
     * @param start The start of the extent.
     * @param length The length of the extent.
     */
    public StreamExtent(long start, long length) {
        this.start = start;
        this.length = length;
    }

    /**
     * Gets the start of the extent (in bytes).
     */
    private long length;

    public long getLength() {
        return length;
    }

    /**
     * Gets the start of the extent (in bytes).
     */
    private long start;

    public long getStart() {
        return start;
    }

    /**
     * Compares this stream extent to another.
     *
     * @param other The extent to compare.
     * @return Value greater than zero if this extent starts after
     *
     *         {@code other}
     *         , zero if they start at the same position, else
     *         a value less than zero.
     */
    public int compareTo(StreamExtent other) {
        if (getStart() > other.getStart()) {
            return 1;
        }

        if (getStart() == other.getStart()) {
            return 0;
        }

        return -1;
    }

    /**
     * Indicates if this StreamExtent is equal to another.
     *
     * @param other The extent to compare.
     * @return
     *         {@code true}
     *         if the extents are equal, else
     *         {@code false}
     *         .
     */
    public boolean equals(StreamExtent other) {
        if (other == null) {
            return false;
        }

        return getStart() == other.getStart() && getLength() == other.getLength();
    }

    public static List<StreamExtent> union() {
        return union(Arrays.asList());
    }

    @SuppressWarnings("unchecked")
    public static List<StreamExtent> union(StreamExtent[]... streams) {
        return union(Arrays.stream(streams).map(Arrays::asList).toArray(List[]::new));
    }

    /**
     * Calculates the union of a list of extents with another extent.
     *
     * @param extents The list of extents.
     * @param other The other extent.
     * @return The union of the extents.
     */
    public static List<StreamExtent> union(List<StreamExtent> extents, StreamExtent other) {
        return union(extents, Arrays.asList(other));
    }

    /**
     * Calculates the union of the extents of multiple streams.
     *
     * @param streams The stream extents.
     * @return The union of the extents from multiple streams.A typical use of
     *         this method is to calculate the combined set of
     *         stored extents from a number of overlayed sparse streams.
     */
    @SafeVarargs
    public static List<StreamExtent> union(List<StreamExtent>... streams) {
        long extentStart = Long.MAX_VALUE;
        long extentEnd = 0;
        // Initialize enumerations and find first stored byte position
        @SuppressWarnings("unchecked")
        Iterator<StreamExtent>[] enums = new Iterator[streams.length];
        boolean[] streamsValid = new boolean[streams.length];
        int validStreamsRemaining = 0;
        StreamExtent[] current = new StreamExtent[streams.length];
        for (int i = 0; i < streams.length; ++i) {
            enums[i] = streams[i].iterator();
            streamsValid[i] = enums[i].hasNext();
            if (streamsValid[i]) {
                current[i] = enums[i].next();
                ++validStreamsRemaining;
                if (current[i].getStart() < extentStart) {
                    extentStart = current[i].getStart();
                    extentEnd = current[i].getStart() + current[i].getLength();
                }
            }
        }
        List<StreamExtent> result = new ArrayList<>();
        while (validStreamsRemaining > 0) {
            // Find the end of this extent
            boolean foundIntersection;
            do {
                foundIntersection = false;
                validStreamsRemaining = 0;
                for (int i = 0; i < streams.length; ++i) {
                    while (streamsValid[i] && current[i].getStart() + current[i].getLength() <= extentEnd) {
                        streamsValid[i] = enums[i].hasNext();
                        if (streamsValid[i])
                            current[i] = enums[i].next();
                    }
                    if (streamsValid[i]) {
                        ++validStreamsRemaining;
                    }

                    if (streamsValid[i] && current[i].getStart() <= extentEnd) {
                        extentEnd = current[i].getStart() + current[i].getLength();
                        foundIntersection = true;
                        streamsValid[i] = enums[i].hasNext();
                        if (streamsValid[i])
                            current[i] = enums[i].next();
                    }
                }
            } while (foundIntersection && validStreamsRemaining > 0);

            // Return the discovered extent
            result.add(new StreamExtent(extentStart, extentEnd - extentStart));

            // Find the next extent start point
            extentStart = Long.MAX_VALUE;
            validStreamsRemaining = 0;
            for (int i = 0; i < streams.length; ++i) {
                if (streamsValid[i]) {
                    ++validStreamsRemaining;
                    if (current[i].getStart() < extentStart) {
                        extentStart = current[i].getStart();
                        extentEnd = current[i].getStart() + current[i].getLength();
                    }
                }
            }
        }
        return result;
    }

    public static List<StreamExtent> intersect() {
        return intersect(Arrays.asList());
    }

    @SuppressWarnings("unchecked")
    public static List<StreamExtent> intersect(StreamExtent[]... streams) {
        return intersect(Arrays.stream(streams).map(Arrays::asList).toArray(List[]::new));
    }

    /**
     * Calculates the intersection of the extents of a stream with another
     * extent.
     *
     * @param extents The stream extents.
     * @param other The extent to intersect.
     * @return The intersection of the extents.
     */
    public static List<StreamExtent> intersect(List<StreamExtent> extents, StreamExtent other) {
        return intersect(extents, Arrays.asList(other));
    }

    /**
     * Calculates the intersection of the extents of multiple streams.
     *
     * @param streams The stream extents.
     * @return The intersection of the extents from multiple streams.A typical
     *         use of this method is to calculate the extents in a
     *         region of a stream..
     */
    @SafeVarargs
    public static List<StreamExtent> intersect(List<StreamExtent>... streams) {
        List<StreamExtent> result = new ArrayList<>();
        long extentStart = Long.MIN_VALUE;
        long extentEnd = Long.MAX_VALUE;
        @SuppressWarnings("unchecked")
        Iterator<StreamExtent>[] enums = new Iterator[streams.length];
        StreamExtent[] current = new StreamExtent[streams.length];
        for (int i = 0; i < streams.length; ++i) {
            enums[i] = streams[i].iterator();
            if (!enums[i].hasNext()) {
                // Gone past end of one stream (in practice was empty), so no intersections
                return result;
            }
            current[i] = enums[i].next();
        }

        int overlapsFound = 0;
        while (true) {
            // We keep cycling round the streams, until we get streams.length continuous overlaps
            for (int i = 0; i < streams.length; ++i) {
                // Move stream on past all extents that are earlier than our candidate start point
                while (current[i].getLength() == 0 || current[i].getStart() + current[i].getLength() <= extentStart) {
                    if (!enums[i].hasNext()) {
                        // Gone past end of this stream, no more intersections possible
                        return result;
                    }
                    current[i] = enums[i].next();
                }

                // If this stream has an extent that spans over the candidate start point
                if (current[i].getStart() <= extentStart) {
                    extentEnd = Math.min(extentEnd, current[i].getStart() + current[i].getLength());
                    overlapsFound++;
                } else {
                    extentStart = current[i].getStart();
                    extentEnd = extentStart + current[i].getLength();
                    overlapsFound = 1;
                }
                // We've just done a complete loop of all streams, they overlapped this start position
                // and we've cut the extent's end down to the shortest run.
                if (overlapsFound == streams.length) {
                    result.add(new StreamExtent(extentStart, extentEnd - extentStart));
                    extentStart = extentEnd;
                    extentEnd = Long.MAX_VALUE;
                    overlapsFound = 0;
                }
            }
        }
    }

    /**
     * Calculates the subtraction of the extents of a stream by another extent.
     *
     * @param extents The stream extents.
     * @param other The extent to subtract.
     * @return The subtraction of
     *         {@code other}
     *         from
     *         {@code extents}
     *         .
     */
    public static List<StreamExtent> subtract(List<StreamExtent> extents, StreamExtent other) {
        return subtract(extents, Arrays.asList(other));
    }

    /**
     * Calculates the subtraction of the extents of a stream by another stream.
     *
     * @param a The stream extents to subtract from.
     * @param b The stream extents to subtract.
     * @return The subtraction of the extents of b from a.
     */
    public static List<StreamExtent> subtract(List<StreamExtent> a, List<StreamExtent> b) {
        return intersect(a, invert(b));
    }

    /**
     * Calculates the inverse of the extents of a stream.
     *
     * @param extents The stream extents to inverse.
     * @return The inverted extents.
     *         This method assumes a logical stream addressable from
     *         {@code 0}
     *         to
     *         {@code Long.MAX_VALUE}
     *         , and is undefined
     *         should any stream extent start at less than 0. To constrain the
     *         extents to a specific range, use the
     *
     *         {@code Intersect}
     *         method.
     */
    public static List<StreamExtent> invert(List<StreamExtent> extents) {
        List<StreamExtent> result = new ArrayList<>();
        StreamExtent last = new StreamExtent(0, 0);
        for (StreamExtent extent : extents) {
            // Skip over any 'noise'
            if (extent.getLength() == 0) {
                continue;
            }

            long lastEnd = last.getStart() + last.getLength();
            if (lastEnd < extent.getStart()) {
                result.add(new StreamExtent(lastEnd, extent.getStart() - lastEnd));
            }

            last = extent;
        }
        long finalEnd = last.getStart() + last.getLength();
        if (finalEnd < Long.MAX_VALUE) {
            result.add(new StreamExtent(finalEnd, Long.MAX_VALUE - finalEnd));
        }
        return result;
    }

    /**
     * Offsets the extents of a stream.
     *
     * @param stream The stream extents.
     * @param delta The amount to offset the extents by.
     * @return The stream extents, offset by delta.
     */
    public static List<StreamExtent> offset(List<StreamExtent> stream, long delta) {
        return stream.stream().map(e -> new StreamExtent(e.getStart() + delta, e.getLength())).collect(Collectors.toList());
    }

    public static long blockCount(StreamExtent[] stream, long blockSize) {
        return blockCount(Arrays.asList(stream), blockSize);
    }

    /**
     * Returns the number of blocks containing stream data.
     *
     * @param stream The stream extents.
     * @param blockSize The size of each block.
     * @return The number of blocks containing stream data.This method logically
     *         divides the stream into blocks of a specified
     *         size, then indicates how many of those blocks contain actual
     *         stream data.
     */
    public static long blockCount(List<StreamExtent> stream, long blockSize) {
        long totalBlocks = 0;
        long lastBlock = -1;
        for (StreamExtent extent : stream) {
            if (extent.getLength() > 0) {
                long extentStartBlock = extent.getStart() / blockSize;
                long extentNextBlock = MathUtilities.ceil(extent.getStart() + extent.getLength(), blockSize);
                long extentNumBlocks = extentNextBlock - extentStartBlock;
                if (extentStartBlock == lastBlock) {
                    extentNumBlocks--;
                }
                lastBlock = extentNextBlock - 1;
                totalBlocks += extentNumBlocks;
            }
        }
        return totalBlocks;
    }

    public static List<Range> blocks(StreamExtent[] stream, long blockSize) {
        return blocks(Arrays.asList(stream), blockSize);
    }

    /**
     * Returns all of the blocks containing stream data.
     *
     * @param stream The stream extents.
     * @param blockSize The size of each block.
     * @return Ranges of blocks, as block indexes.This method logically divides
     *         the stream into blocks of a specified
     *         size, then indicates ranges of blocks that contain stream data.
     */
    public static List<Range> blocks(List<StreamExtent> stream, long blockSize) {
        List<Range> result = new ArrayList<>();
        Long rangeStart = null;
        long rangeLength = 0;
        for (StreamExtent extent : stream) {
            if (extent.getLength() > 0) {
                long extentStartBlock = extent.getStart() / blockSize;
                long extentNextBlock = MathUtilities.ceil(extent.getStart() + extent.getLength(), blockSize);
                if (rangeStart != null && extentStartBlock > rangeStart + rangeLength) {

                    // This extent is non-contiguous (in terms of blocks), so write out the last range and start new
                    result.add(new Range(rangeStart, rangeLength));
                    rangeStart = extentStartBlock;
                } else if (rangeStart == null) {
                    // First extent, so start first range
                    rangeStart = extentStartBlock;
                }
                // Set the length of the current range, based on the end of this extent
                rangeLength = extentNextBlock - rangeStart;
            }
        }
        // Final range (if any ranges at all) hasn't been returned yet, so do that now
        if (rangeStart != null) {
            result.add(new Range(rangeStart, rangeLength));
        }
        return result;
    }

    /**
     * The equality operator.
     *
     * @param a The first extent to compare.
     * @param b The second extent to compare.
     * @return Whether the two extents are equal.
     */

    /**
     * The inequality operator.
     *
     * @param a The first extent to compare.
     * @param b The second extent to compare.
     * @return Whether the two extents are different.
     */

    /**
     * The less-than operator.
     *
     * @param a The first extent to compare.
     * @param b The second extent to compare.
     * @return Whether a is less than b.
     */

    /**
     * The greater-than operator.
     *
     * @param a The first extent to compare.
     * @param b The second extent to compare.
     * @return Whether a is greater than b.
     */

    /**
     * Returns a string representation of the extent as [start:+length].
     *
     * @return The string representation.
     */
    public String toString() {
        return "[" + getStart() + ":+" + getLength() + "]";
    }

    /**
     * Indicates if this stream extent is equal to another object.
     *
     * @param obj The object to test.
     * @return
     *         {@code true}
     *         if
     *         {@code obj}
     *         is equivalent, else
     *         {@code false}
     *         .
     */
    public boolean equals(Object obj) {
        return equals(obj instanceof StreamExtent ? (StreamExtent) obj : (StreamExtent) null);
    }

    /**
     * Gets a hash code for this extent.
     *
     * @return The extent's hash code.
     */
    public int hashCode() {
        return (int) (getStart() ^ getLength());
    }

}
