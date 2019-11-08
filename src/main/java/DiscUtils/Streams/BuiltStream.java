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

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import DiscUtils.Streams.Builder.BuilderExtent;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public class BuiltStream extends SparseStream {
    private Stream _baseStream;

    private BuilderExtent _currentExtent;

    private final List<BuilderExtent> _extents;

    private final long _length;

    private long _position;

    public BuiltStream(long length, List<BuilderExtent> extents) {
        _baseStream = new ZeroStream(length);
        _length = length;
        _extents = extents;

        // Make sure the extents are sorted, so binary searches will work.
        Collections.sort(_extents, new ExtentStartComparer());
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
        return _extents.stream().flatMap(extent -> extent.getStreamExtents().stream()).collect(Collectors.toList());
    }

    public long getLength() {
        return _length;
    }

    public long getPosition() {
        return _position;
    }

    public void setPosition(long value) {
        _position = value;
    }

    public void flush() {
    }

    public int read(byte[] buffer, int offset, int count) {
        if (_position >= _length) {
            return 0;
        }

        if (_position + count > _length) {
            count = (int) (_length - _position);
        }

        int totalRead = 0;
        while (totalRead < count && _position < _length) {
            // If current region is outside the area of interest, clean it up
            if (_currentExtent != null && (_position < _currentExtent.getStart() ||
                                           _position >= _currentExtent.getStart() + _currentExtent.getLength())) {
                _currentExtent.disposeReadState();
                _currentExtent = null;
            }

            // If we need to find a new region, look for it
            if (_currentExtent == null) {
                try (SearchExtent searchExtent = new SearchExtent(_position)) {
                    int idx = Collections.binarySearch(_extents, searchExtent, new ExtentRangeComparer());
                    if (idx >= 0) {
                        BuilderExtent extent = _extents.get(idx);
//Debug.println(getPosition() + ", " + offset + ", " + extent);
                        extent.prepareForRead();
                        _currentExtent = extent;
                    }
                }
            }

            int numRead = 0;

            // If the block is outside any known extent, defer to base stream.
            if (_currentExtent == null) {
                _baseStream.setPosition(_position);
                BuilderExtent nextExtent = findNext(_position);
                if (nextExtent != null) {
                    numRead = _baseStream.read(buffer,
                                               offset + totalRead,
                                               (int) Math.min(count - totalRead, nextExtent.getStart() - _position));
                } else {
                    numRead = _baseStream.read(buffer, offset + totalRead, count - totalRead);
                }
            } else {
                numRead = _currentExtent.read(_position, buffer, offset + totalRead, count - totalRead);
//Debug.println(_currentExtent + ", " + _position + ", " + numRead + ", " + count + "\n" + StringUtil.getDump(buffer, offset + totalRead, 64));
            }

            _position += numRead;
            totalRead += numRead;
            if (numRead == 0)
                break;
        }

        return totalRead;
    }

    public long seek(long offset, SeekOrigin origin) {
        long newPos = offset;
        if (origin == SeekOrigin.Current) {
            newPos += _position;
        } else if (origin == SeekOrigin.End) {
            newPos += _length;
        }

        _position = newPos;
        return newPos;
    }

    public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    public void write(byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    public void close() throws IOException {
        if (_currentExtent != null) {
            _currentExtent.disposeReadState();
            _currentExtent = null;
        }

        if (_baseStream != null) {
            _baseStream.close();
            _baseStream = null;
        }
    }

    private BuilderExtent findNext(long pos) {
        int min = 0;
        int max = _extents.size() - 1;

        if (_extents.size() == 0 ||
            _extents.get(_extents.size() - 1).getStart() + _extents.get(_extents.size() - 1).getLength() <= pos) {
            return null;
        }

        while (true) {
            if (min >= max) {
                return _extents.get(min);
            }

            int mid = (max + min) / 2;
            if (_extents.get(mid).getStart() < pos) {
                min = mid + 1;
            } else if (_extents.get(mid).getStart() > pos) {
                max = mid;
            } else {
                return _extents.get(mid);
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
