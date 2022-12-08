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

package discUtils.streams;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import discUtils.streams.builder.BuilderExtent;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public class BuiltStream extends SparseStream {

    private Stream baseStream;

    private BuilderExtent currentExtent;

    private final List<BuilderExtent> extents;

    private final long length;

    private long position;

    public BuiltStream(long length, List<BuilderExtent> extents) {
        baseStream = new ZeroStream(length);
        this.length = length;
        this.extents = extents;

        // Make sure the extents are sorted, so binary searches will work.
        this.extents.sort(new ExtentStartComparer());
    }

    public boolean canRead() {
        return true;
    }

    public boolean canSeek() {
        return true;
    }

    public boolean canWrite() {
        return false;
    }

    public List<StreamExtent> getExtents() {
        return extents.stream().flatMap(extent -> extent.getStreamExtents().stream()).collect(Collectors.toList());
    }

    public long getLength() {
        return length;
    }

    @Override public long position() {
        return position;
    }

    @Override public void position(long value) {
        position = value;
    }

    public void flush() {
    }

    public int read(byte[] buffer, int offset, int count) {
        if (position >= length) {
            return 0;
        }

        if (position + count > length) {
            count = (int) (length - position);
        }

        int totalRead = 0;
        while (totalRead < count && position < length) {
            // If current region is outside the area of interest, clean it up
            if (currentExtent != null && (position < currentExtent.getStart() ||
                                           position >= currentExtent.getStart() + currentExtent.getLength())) {
                currentExtent.disposeReadState();
                currentExtent = null;
            }

            // If we need to find a new region, look for it
            if (currentExtent == null) {
                try (SearchExtent searchExtent = new SearchExtent(position)) {
                    int idx = Collections.binarySearch(extents, searchExtent, new ExtentRangeComparer());
                    if (idx >= 0) {
                        BuilderExtent extent = extents.get(idx);
//Debug.println(position() + ", " + offset + ", " + extent);
                        extent.prepareForRead();
                        currentExtent = extent;
                    }
                }
            }

            int numRead = 0;

            // If the block is outside any known extent, defer to base stream.
            if (currentExtent == null) {
                baseStream.position(position);
                BuilderExtent nextExtent = findNext(position);
                if (nextExtent != null) {
                    numRead = baseStream.read(buffer,
                                               offset + totalRead,
                                               (int) Math.min(count - totalRead, nextExtent.getStart() - position));
                } else {
                    numRead = baseStream.read(buffer, offset + totalRead, count - totalRead);
                }
            } else {
                numRead = currentExtent.read(position, buffer, offset + totalRead, count - totalRead);
//Debug.println(currentExtent + ", " + position + ", " + numRead + ", " + count + "\n" + StringUtil.getDump(buffer, offset + totalRead, 64));
            }

            position += numRead;
            totalRead += numRead;
            if (numRead == 0)
                break;
        }

        return totalRead;
    }

    public long seek(long offset, SeekOrigin origin) {
        long newPos = offset;
        if (origin == SeekOrigin.Current) {
            newPos += position;
        } else if (origin == SeekOrigin.End) {
            newPos += length;
        }

        position = newPos;
        return newPos;
    }

    public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    public void write(byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    public void close() throws IOException {
        if (currentExtent != null) {
            currentExtent.disposeReadState();
            currentExtent = null;
        }

        if (baseStream != null) {
            baseStream.close();
            baseStream = null;
        }
    }

    private BuilderExtent findNext(long pos) {
        int min = 0;
        int max = extents.size() - 1;

        if (extents.size() == 0 ||
            extents.get(extents.size() - 1).getStart() + extents.get(extents.size() - 1).getLength() <= pos) {
            return null;
        }

        while (true) {
            if (min >= max) {
                return extents.get(min);
            }

            int mid = (max + min) / 2;
            if (extents.get(mid).getStart() < pos) {
                min = mid + 1;
            } else if (extents.get(mid).getStart() > pos) {
                max = mid;
            } else {
                return extents.get(mid);
            }
        }
    }

    private static class SearchExtent extends BuilderExtent {
        public SearchExtent(long pos) {
            super(pos, 1);
        }

        public void close() {
        }

        public void prepareForRead() {
            // Not valid to use this 'dummy' extent for actual construction
            throw new UnsupportedOperationException();
        }

        public int read(long diskOffset, byte[] block, int offset, int count) {
            // Not valid to use this 'dummy' extent for actual construction
            throw new UnsupportedOperationException();
        }

        public void disposeReadState() {
            // Not valid to use this 'dummy' extent for actual construction
            throw new UnsupportedOperationException();
        }
    }

    private static class ExtentRangeComparer implements Comparator<BuilderExtent> {
        public int compare(BuilderExtent x, BuilderExtent y) {
            if (x == null) {
                throw new NullPointerException("x");
            }

            if (y == null) {
                throw new NullPointerException("y");
            }

            if (x.getStart() + x.getLength() <= y.getStart()) {
                // x < y, with no intersection
                return -1;
            }

            if (x.getStart() >= y.getStart() + y.getLength()) {
                // x > y, with no intersection
                return 1;
            }

            // x intersects y
            return 0;
        }
    }

    private static class ExtentStartComparer implements Comparator<BuilderExtent> {
        public int compare(BuilderExtent x, BuilderExtent y) {
            if (x == null) {
                throw new NullPointerException("x");
            }

            if (y == null) {
                throw new NullPointerException("y");
            }

            long val = x.getStart() - y.getStart();
            if (val < 0) {
                return -1;
            }
            if (val > 0) {
                return 1;
            }
            return 0;
        }
    }
}
