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

import java.util.List;

import DiscUtils.Streams.Util.Ownership;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * Base class for streams that are essentially a mapping onto a parent stream.
 * 
 * This class provides access to the mapping underlying the stream, enabling
 * callers to convert a byte range in this stream into one or more ranges in
 * the parent stream.
 */
public abstract class MappedStream extends SparseStream {
    /**
     * Converts any stream into a non-linear stream.
     * 
     * @param stream The stream to convert.
     * @param takeOwnership
     *            {@code true}
     *            to have the new stream dispose the wrapped
     *            stream when it is disposed.
     * @return A sparse stream.The wrapped stream is assumed to be a linear
     *         stream (such that any byte range
     *         maps directly onto the parent stream).
     */
    public static MappedStream fromStream(Stream stream, Ownership takeOwnership) {
        return new WrappingMappedStream<>(stream, takeOwnership, null);
    }

    /**
     * Converts any stream into a non-linear stream.
     * 
     * @param stream The stream to convert.
     * @param takeOwnership
     *            {@code true}
     *            to have the new stream dispose the wrapped
     *            stream when it is disposed.
     * @param extents The set of extents actually stored in
     *            {@code stream}
     *            .
     * @return A sparse stream.The wrapped stream is assumed to be a linear
     *         stream (such that any byte range
     *         maps directly onto the parent stream).
     */
    public static MappedStream fromStream(Stream stream,
                                          Ownership takeOwnership,
                                          List<StreamExtent> extents) {
        return new WrappingMappedStream<>(stream, takeOwnership, extents);
    }

    /**
     * Maps a logical range down to storage locations.
     * 
     * @param start The first logical range to map.
     * @param length The length of the range to map.
     * @return One or more stream extents specifying the storage locations that
     *         correspond
     *         to the identified logical extent range.As far as possible, the
     *         stream extents are returned in logical disk order -
     *         however, due to the nature of non-linear streams, not all of the
     *         range may actually
     *         be stored, or some or all of the range may be compressed - thus
     *         reading the
     *         returned stream extents is not equivalent to reading the logical
     *         disk range.
     */
    public abstract List<StreamExtent> mapContent(long start, long length);

}
