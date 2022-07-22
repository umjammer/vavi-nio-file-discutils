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

package discUtils.vmdk;

import java.io.IOException;
import java.util.ArrayList;

import java.util.Collections;
import java.util.List;

import discUtils.core.internal.ObjectCache;
import discUtils.streams.MappedStream;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public abstract class CommonSparseExtentStream extends MappedStream {

    /**
     * Indicator to whether end-of-stream has been reached.
     */
    protected boolean atEof;

    /**
     * The current grain that's loaded into grainTable.
     */
    protected int currentGrainTable;

    /**
     * Offset of this extent within the disk.
     */
    protected long diskOffset;

    /**
     * Stream containing the sparse extent.
     */
    protected Stream fileStream;

    /**
     * The Global Directory for this extent.
     */
    protected int[] globalDirectory;

    /**
     * The data corresponding to the current grain (or null).
     */
    protected byte[] grainTable;

    /**
     * Cache of recently used grain tables.
     */
    private final ObjectCache<Integer, byte[]> grainTableCache = new ObjectCache<>();

    /**
     * The number of bytes controlled by a single grain table.
     */
    protected long gtCoverage;

    /**
     * The header from the start of the extent.
     */
    protected CommonSparseExtentHeader header;

    /**
     * Indicates if this object controls the lifetime of fileStream.
     */
    protected Ownership ownsFileStream;

    /**
     * Indicates if this object controls the lifetime of parentDiskStream.
     */
    protected Ownership ownsParentDiskStream;

    /**
     * The stream containing the unstored bytes.
     */
    protected SparseStream parentDiskStream;

    /**
     * Current position in the extent.
     */
    protected long position;

    /**
     * The Redundant Global Directory for this extent.
     */
    protected int[] redundantGlobalDirectory;

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
        return fileStream.canWrite();
    }

    public List<StreamExtent> getExtents() {
        return getExtentsInRange(0, getLength());
    }

    public long getLength() {
        checkDisposed();
        return header.capacity * Sizes.Sector;
    }

    public long getPosition() {
        checkDisposed();
        return position;
    }

    public void setPosition(long value) {
        checkDisposed();
        position = value;
        atEof = false;
    }

    public void flush() {
        checkDisposed();
    }

    public int read(byte[] buffer, int offset, int count) {
        checkDisposed();

        if (position > getLength()) {
            atEof = true;
            throw new dotnet4j.io.IOException("Attempt to read beyond end of stream");
        }

        if (position == getLength()) {
            if (atEof) {
                throw new dotnet4j.io.IOException("Attempt to read beyond end of stream");
            }
            atEof = true;
            return 0;
        }

        int maxToRead = (int) Math.min(count, getLength() - position);
        int totalRead = 0;
        int numRead;

        do {
            int grainTable = (int) (position / gtCoverage);
            int grainTableOffset = (int) (position - grainTable * gtCoverage);
            numRead = 0;

            if (!loadGrainTable(grainTable)) {
                // Read from parent stream, to at most the end of grain table's coverage
                parentDiskStream.setPosition(position + diskOffset);
                numRead = parentDiskStream.read(buffer,
                                                 offset + totalRead,
                                                 (int) Math.min(maxToRead - totalRead, gtCoverage - grainTableOffset));
            } else {
                int grainSize = (int) (header.grainSize * Sizes.Sector);
                int grain = grainTableOffset / grainSize;
                int grainOffset = grainTableOffset - grain * grainSize;

                int numToRead = Math.min(maxToRead - totalRead, grainSize - grainOffset);

                if (getGrainTableEntry(grain) == 0) {
                    parentDiskStream.setPosition(position + diskOffset);
                    numRead = parentDiskStream.read(buffer, offset + totalRead, numToRead);
                } else {
                    int bufferOffset = offset + totalRead;
                    long grainStart = (long) getGrainTableEntry(grain) * Sizes.Sector;
                    numRead = readGrain(buffer, bufferOffset, grainStart, grainOffset, numToRead);
                }
            }

            position += numRead;
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
            effectiveOffset += position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += header.capacity * Sizes.Sector;
        }

        atEof = false;

        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of disk");
        }
        position = effectiveOffset;
        return position;
    }

    public List<StreamExtent> getExtentsInRange(long start, long count) {
        checkDisposed();

        long maxCount = Math.min(getLength(), start + count) - start;
        if (maxCount < 0) {
            return Collections.emptyList();
        }

        List<StreamExtent> parentExtents = parentDiskStream.getExtentsInRange(diskOffset + start, maxCount);
        parentExtents = StreamExtent.offset(parentExtents, -diskOffset);

        List<StreamExtent> result = StreamExtent.union(layerExtents(start, maxCount), parentExtents);
        result = StreamExtent.intersect(result, Collections.singletonList(new StreamExtent(start, maxCount)));
        return result;
    }

    public List<StreamExtent> mapContent(long start, long length) {
        checkDisposed();

        List<StreamExtent> result = new ArrayList<>();
        if (start < getLength()) {
            long end = Math.min(start + length, getLength());

            long pos = start;

            do {
                int grainTable = (int) (pos / gtCoverage);
                int grainTableOffset = (int) (pos - grainTable * gtCoverage);

                if (loadGrainTable(grainTable)) {
                    int grainSize = (int) (header.grainSize * Sizes.Sector);
                    int grain = grainTableOffset / grainSize;
                    int grainOffset = grainTableOffset - grain * grainSize;

                    int numToRead = (int) Math.min(end - pos, grainSize - grainOffset);

                    if (getGrainTableEntry(grain) != 0) {
                        long grainStart = (long) getGrainTableEntry(grain) * Sizes.Sector;
                        result.add(mapGrain(grainStart, grainOffset, numToRead));
                    }

                    pos += numToRead;
                } else {
                    pos = (grainTable + 1) * gtCoverage;
                }
            } while (pos < end);
        }
        return result;
    }

    public void close() throws IOException {
        if (ownsFileStream == Ownership.Dispose && fileStream != null) {
            fileStream.close();
        }

        fileStream = null;
        if (ownsParentDiskStream == Ownership.Dispose && parentDiskStream != null) {
            parentDiskStream.close();
        }

        parentDiskStream = null;
    }

    protected int getGrainTableEntry(int grain) {
        return EndianUtilities.toUInt32LittleEndian(grainTable, grain * 4);
    }

    protected void setGrainTableEntry(int grain, int value) {
        EndianUtilities.writeBytesLittleEndian(value, grainTable, grain * 4);
    }

    protected int readGrain(byte[] buffer,
                            int bufferOffset,
                            long grainStart,
                            int grainOffset,
                            int numToRead) {
        fileStream.setPosition(grainStart + grainOffset);
        return fileStream.read(buffer, bufferOffset, numToRead);
    }

    protected StreamExtent mapGrain(long grainStart, int grainOffset, int numToRead) {
        return new StreamExtent(grainStart + grainOffset, numToRead);
    }

    protected void loadGlobalDirectory() {
        int numGTs = (int) MathUtilities.ceil(header.capacity * Sizes.Sector, gtCoverage);

        globalDirectory = new int[numGTs];
        fileStream.setPosition(header.gdOffset * Sizes.Sector);
        byte[] gdAsBytes = StreamUtilities.readExact(fileStream, numGTs * 4);
        for (int i = 0; i < globalDirectory.length; ++i) {
            globalDirectory[i] = EndianUtilities.toUInt32LittleEndian(gdAsBytes, i * 4);
        }
    }

    protected boolean loadGrainTable(int index) {
        // Current grain table, so early-out
        if (grainTable != null && currentGrainTable == index) {
            return true;
        }

        // This grain table not present in grain directory, so can't load it...
        if (globalDirectory[index] == 0) {
            return false;
        }

        // Cached grain table?
        byte[] cachedGrainTable = grainTableCache.get(index);
        if (cachedGrainTable != null) {
            currentGrainTable = index;
            grainTable = cachedGrainTable;
            return true;
        }

        // Not cached, so read
        fileStream.setPosition((long) globalDirectory[index] * Sizes.Sector);
        byte[] newGrainTable = StreamUtilities.readExact(fileStream, header.numGTEsPerGT * 4);
        currentGrainTable = index;
        grainTable = newGrainTable;

        grainTableCache.put(index, newGrainTable);

        return true;
    }

    protected void checkDisposed() {
        if (fileStream == null) {
            throw new dotnet4j.io.IOException("CommonSparseExtentStream");
        }
    }

    private List<StreamExtent> layerExtents(long start, long count) {
        List<StreamExtent> result = new ArrayList<>();
        long maxPos = start + count;
        long pos = findNextPresentGrain(MathUtilities.roundDown(start, header.grainSize * Sizes.Sector), maxPos);
        while (pos < maxPos) {
            long end = findNextAbsentGrain(pos, maxPos);
            result.add(new StreamExtent(pos, end - pos));

            pos = findNextPresentGrain(end, maxPos);
        }
        return result;
    }

    private long findNextPresentGrain(long pos, long maxPos) {
        int grainSize = (int) (header.grainSize * Sizes.Sector);

        boolean foundStart = false;
        while (pos < maxPos && !foundStart) {
            int grainTable = (int) (pos / gtCoverage);

            if (!loadGrainTable(grainTable)) {
                pos += gtCoverage;
            } else {
                int grainTableOffset = (int) (pos - grainTable * gtCoverage);

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
        int grainSize = (int) (header.grainSize * Sizes.Sector);

        boolean foundEnd = false;
        while (pos < maxPos && !foundEnd) {
            int grainTable = (int) (pos / gtCoverage);

            if (!loadGrainTable(grainTable)) {
                foundEnd = true;
            } else {
                int grainTableOffset = (int) (pos - grainTable * gtCoverage);

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
