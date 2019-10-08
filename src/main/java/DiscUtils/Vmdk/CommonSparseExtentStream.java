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

package DiscUtils.Vmdk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import DiscUtils.Core.Internal.ObjectCache;
import DiscUtils.Streams.MappedStream;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.SeekOrigin;
import moe.yo3explorer.dotnetio4j.Stream;


public abstract class CommonSparseExtentStream extends MappedStream {
    /**
     * Indicator to whether end-of-stream has been reached.
     */
    protected boolean _atEof;

    /**
     * The current grain that's loaded into _grainTable.
     */
    protected int _currentGrainTable;

    /**
     * Offset of this extent within the disk.
     */
    protected long _diskOffset;

    /**
     * Stream containing the sparse extent.
     */
    protected Stream _fileStream;

    /**
     * The Global Directory for this extent.
     */
    protected int[] _globalDirectory;

    /**
     * The data corresponding to the current grain (or null).
     */
    protected byte[] _grainTable;

    /**
     * Cache of recently used grain tables.
     */
    private final ObjectCache<Integer, byte[]> _grainTableCache = new ObjectCache<>();

    /**
     * The number of bytes controlled by a single grain table.
     */
    protected long _gtCoverage;

    /**
     * The header from the start of the extent.
     */
    protected CommonSparseExtentHeader _header;

    /**
     * Indicates if this object controls the lifetime of _fileStream.
     */
    protected Ownership _ownsFileStream;

    /**
     * Indicates if this object controls the lifetime of _parentDiskStream.
     */
    protected Ownership _ownsParentDiskStream;

    /**
     * The stream containing the unstored bytes.
     */
    protected SparseStream _parentDiskStream;

    /**
     * Current position in the extent.
     */
    protected long _position;

    /**
     * The Redundant Global Directory for this extent.
     */
    protected int[] _redundantGlobalDirectory;

    public boolean canRead() {
        checkDisposed();
        return true;
    }

    public boolean canSeek() {
        checkDisposed();
        return true;
    }

    public boolean canWrite() {
        checkDisposed();
        return _fileStream.canWrite();
    }

    public List<StreamExtent> getExtents() {
        return getExtentsInRange(0, getLength());
    }

    public long getLength() {
        checkDisposed();
        return _header.Capacity * Sizes.Sector;
    }

    public long getPosition() {
        checkDisposed();
        return _position;
    }

    public void setPosition(long value) {
        checkDisposed();
        _position = value;
        _atEof = false;
    }

    public void flush() {
        checkDisposed();
    }

    public int read(byte[] buffer, int offset, int count) {
        checkDisposed();
        if (_position > getLength()) {
            _atEof = true;
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to read beyond end of stream");
        }

        if (_position == getLength()) {
            if (_atEof) {
                throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to read beyond end of stream");
            }

            _atEof = true;
            return 0;
        }

        int maxToRead = (int) Math.min(count, getLength() - _position);
        int totalRead = 0;
        int numRead;
        do {
            int grainTable = (int) (_position / _gtCoverage);
            int grainTableOffset = (int) (_position - grainTable * _gtCoverage);
            numRead = 0;
            if (!loadGrainTable(grainTable)) {
                // Read from parent stream, to at most the end of grain table's coverage
                _parentDiskStream.setPosition(_position + _diskOffset);
                numRead = _parentDiskStream.read(buffer,
                                                 offset + totalRead,
                                                 (int) Math.min(maxToRead - totalRead, _gtCoverage - grainTableOffset));
            } else {
                int grainSize = (int) (_header.GrainSize * Sizes.Sector);
                int grain = grainTableOffset / grainSize;
                int grainOffset = grainTableOffset - grain * grainSize;
                int numToRead = Math.min(maxToRead - totalRead, grainSize - grainOffset);
                if (getGrainTableEntry(grain) == 0) {
                    _parentDiskStream.setPosition(_position + _diskOffset);
                    numRead = _parentDiskStream.read(buffer, offset + totalRead, numToRead);
                } else {
                    int bufferOffset = offset + totalRead;
                    long grainStart = (long) getGrainTableEntry(grain) * Sizes.Sector;
                    numRead = readGrain(buffer, bufferOffset, grainStart, grainOffset, numToRead);
                }
            }
            _position += numRead;
            totalRead += numRead;
        } while (numRead != 0 && totalRead < maxToRead);
        return totalRead;
    }

    public void setLength(long value) {
        checkDisposed();
        throw new UnsupportedOperationException();
    }

    public long seek(long offset, SeekOrigin origin) {
        checkDisposed();
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += _position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += _header.Capacity * Sizes.Sector;
        }

        _atEof = false;
        if (effectiveOffset < 0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to move before beginning of disk");
        }

        _position = effectiveOffset;
        return _position;
    }

    public List<StreamExtent> getExtentsInRange(long start, long count) {
        checkDisposed();
        long maxCount = Math.min(getLength(), start + count) - start;
        if (maxCount < 0) {
            return Arrays.asList();
        }

        List<StreamExtent> parentExtents = _parentDiskStream.getExtentsInRange(_diskOffset + start, maxCount);
        parentExtents = StreamExtent.offset(parentExtents, -_diskOffset);
        List<StreamExtent> result = StreamExtent.union(layerExtents(start, maxCount), parentExtents);
        result = StreamExtent.intersect(result);
        return result;
    }

    public List<StreamExtent> mapContent(long start, long length) {
        List<StreamExtent> result = new ArrayList<>();
        checkDisposed();
        if (start < getLength()) {
            long end = Math.min(start + length, getLength());
            long pos = start;
            do {
                int grainTable = (int) (pos / _gtCoverage);
                int grainTableOffset = (int) (pos - grainTable * _gtCoverage);
                if (loadGrainTable(grainTable)) {
                    int grainSize = (int) (_header.GrainSize * Sizes.Sector);
                    int grain = grainTableOffset / grainSize;
                    int grainOffset = grainTableOffset - grain * grainSize;
                    int numToRead = (int) Math.min(end - pos, grainSize - grainOffset);
                    if (getGrainTableEntry(grain) != 0) {
                        long grainStart = (long) getGrainTableEntry(grain) * Sizes.Sector;
                        result.add(mapGrain(grainStart, grainOffset, numToRead));
                    }

                    pos += numToRead;
                } else {
                    pos = (grainTable + 1) * _gtCoverage;
                }
            } while (pos < end);
        }
        return result;
    }

    public void close() throws IOException {
        if (_ownsFileStream == Ownership.Dispose && _fileStream != null) {
            _fileStream.close();
        }

        _fileStream = null;
        if (_ownsParentDiskStream == Ownership.Dispose && _parentDiskStream != null) {
            _parentDiskStream.close();
        }

        _parentDiskStream = null;
    }

    protected int getGrainTableEntry(int grain) {
        return EndianUtilities.toUInt32LittleEndian(_grainTable, grain * 4);
    }

    protected void setGrainTableEntry(int grain, int value) {
        EndianUtilities.writeBytesLittleEndian(value, _grainTable, grain * 4);
    }

    protected int readGrain(byte[] buffer,
                            int bufferOffset,
                            long grainStart,
                            int grainOffset,
                            int numToRead) {
        _fileStream.setPosition(grainStart + grainOffset);
        return _fileStream.read(buffer, bufferOffset, numToRead);
    }

    protected StreamExtent mapGrain(long grainStart, int grainOffset, int numToRead) {
        return new StreamExtent(grainStart + grainOffset, numToRead);
    }

    protected void loadGlobalDirectory() {
        int numGTs = (int) MathUtilities.ceil(_header.Capacity * Sizes.Sector, _gtCoverage);
        _globalDirectory = new int[numGTs];
        _fileStream.setPosition(_header.GdOffset * Sizes.Sector);
        byte[] gdAsBytes = StreamUtilities.readExact(_fileStream, numGTs * 4);
        for (int i = 0; i < _globalDirectory.length; ++i) {
            _globalDirectory[i] = EndianUtilities.toUInt32LittleEndian(gdAsBytes, i * 4);
        }
    }

    protected boolean loadGrainTable(int index) {
        // Current grain table, so early-out
        if (_grainTable != null && _currentGrainTable == index) {
            return true;
        }

        // This grain table not present in grain directory, so can't load it...
        if (_globalDirectory[index] == 0) {
            return false;
        }

        // Cached grain table?
        byte[] cachedGrainTable = _grainTableCache.get___idx(index);
        if (cachedGrainTable != null) {
            _currentGrainTable = index;
            _grainTable = cachedGrainTable;
            return true;
        }

        // Not cached, so read
        _fileStream.setPosition((long) _globalDirectory[index] * Sizes.Sector);
        byte[] newGrainTable = StreamUtilities.readExact(_fileStream, _header.NumGTEsPerGT * 4);
        _currentGrainTable = index;
        _grainTable = newGrainTable;
        _grainTableCache.set___idx(index, newGrainTable);
        return true;
    }

    protected void checkDisposed() {
        if (_fileStream == null) {
            throw new moe.yo3explorer.dotnetio4j.IOException("CommonSparseExtentStream");
        }

    }

    private List<StreamExtent> layerExtents(long start, long count) {
        List<StreamExtent> result = new ArrayList<>();
        long maxPos = start + count;
        long pos = findNextPresentGrain(MathUtilities.roundDown(start, _header.GrainSize * Sizes.Sector), maxPos);
        while (pos < maxPos) {
            long end = findNextAbsentGrain(pos, maxPos);
            result.add(new StreamExtent(pos, end - pos));

            pos = findNextPresentGrain(end, maxPos);
        }
        return result;
    }

    private long findNextPresentGrain(long pos, long maxPos) {
        int grainSize = (int) (_header.GrainSize * Sizes.Sector);
        boolean foundStart = false;
        while (pos < maxPos && !foundStart) {
            int grainTable = (int) (pos / _gtCoverage);
            if (!loadGrainTable(grainTable)) {
                pos += _gtCoverage;
            } else {
                int grainTableOffset = (int) (pos - grainTable * _gtCoverage);
                int grain = grainTableOffset / grainSize;
                if (getGrainTableEntry(grain) == 0) {
                    pos += grainSize;
                } else {
                    foundStart = true;
                }
            }
        }
        return Math.min(pos, maxPos);
    }

    private long findNextAbsentGrain(long pos, long maxPos) {
        int grainSize = (int) (_header.GrainSize * Sizes.Sector);
        boolean foundEnd = false;
        while (pos < maxPos && !foundEnd) {
            int grainTable = (int) (pos / _gtCoverage);
            if (!loadGrainTable(grainTable)) {
                foundEnd = true;
            } else {
                int grainTableOffset = (int) (pos - grainTable * _gtCoverage);
                int grain = grainTableOffset / grainSize;
                if (getGrainTableEntry(grain) == 0) {
                    foundEnd = true;
                } else {
                    pos += grainSize;
                }
            }
        }
        return Math.min(pos, maxPos);
    }

}
