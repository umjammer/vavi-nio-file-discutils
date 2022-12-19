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

package discUtils.vhd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import discUtils.streams.MappedStream;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;
import vavi.util.ByteUtil;


public class DynamicStream extends MappedStream {

    private boolean atEof;

    private boolean autoCommitFooter = true;

    private int[] blockAllocationTable;

    private final byte[][] blockBitmaps;

    private final int blockBitmapSize;

    private final DynamicHeader dynamicHeader;

    private final Stream fileStream;

    private byte[] footerCache;

    private final long length;

    private boolean newBlocksAllocated;

    private long nextBlockStart;

    private final Ownership ownsParentStream;

    private SparseStream parentStream;

    private long position;

    public DynamicStream(Stream fileStream,
            DynamicHeader dynamicHeader,
            long length,
            SparseStream parentStream,
            Ownership ownsParentStream) {
        if (fileStream == null) {
            throw new IllegalArgumentException("fileStream");
        }

        if (dynamicHeader == null) {
            throw new IllegalArgumentException("dynamicHeader");
        }

        if (parentStream == null) {
            throw new IllegalArgumentException("parentStream");
        }

        if (length < 0) {
            throw new IndexOutOfBoundsException("Negative lengths not allowed");
        }

        this.fileStream = fileStream;
        this.dynamicHeader = dynamicHeader;
        this.length = length;
        this.parentStream = parentStream;
        this.ownsParentStream = ownsParentStream;
        blockBitmaps = new byte[this.dynamicHeader.maxTableEntries][];
        blockBitmapSize = MathUtilities.roundUp(MathUtilities.ceil(this.dynamicHeader.blockSize, Sizes.Sector * 8), Sizes.Sector);
        readBlockAllocationTable();
        // Detect where next block should go (cope if the footer is missing)
        this.fileStream.position(MathUtilities.roundDown(this.fileStream.getLength(), Sizes.Sector) - Sizes.Sector);
        byte[] footerBytes = StreamUtilities.readExact(this.fileStream, Sizes.Sector);
        Footer footer = Footer.fromBytes(footerBytes, 0);
        nextBlockStart = this.fileStream.position() - (footer.isValid() ? Sizes.Sector : 0);
    }

    public boolean getAutoCommitFooter() {
        return autoCommitFooter;
    }

    public void setAutoCommitFooter(boolean value) {
        autoCommitFooter = value;
        if (autoCommitFooter) {
            updateFooter();
        }
    }

    @Override
    public boolean canRead() {
        checkDisposed();
        return true;
    }

    @Override
    public boolean canSeek() {
        checkDisposed();
        return true;
    }

    @Override
    public boolean canWrite() {
        checkDisposed();
        return fileStream.canWrite();
    }

    @Override
    public List<StreamExtent> getExtents() {
        return getExtentsInRange(0, getLength());
    }

    @Override
    public long getLength() {
        checkDisposed();
        return length;
    }

    @Override
    public long position() {
        checkDisposed();
        return position;
    }

    @Override
    public void position(long value) {
        checkDisposed();
        atEof = false;
        position = value;
    }

    @Override
    public void flush() {
        checkDisposed();
    }

    @Override
    public List<StreamExtent> mapContent(long start, long length) {
        List<StreamExtent> result = new ArrayList<>();
        long position = start;
        int maxToRead = (int) Math.min(length, this.length - position);
        int numRead = 0;

        while (numRead < maxToRead) {
            long block = position / dynamicHeader.blockSize;
            int offsetInBlock = (int) (position % dynamicHeader.blockSize);

            if (populateBlockBitmap(block)) {
                int sectorInBlock = offsetInBlock / Sizes.Sector;
                int offsetInSector = offsetInBlock % Sizes.Sector;
                int toRead = Math.min(maxToRead - numRead, dynamicHeader.blockSize - offsetInBlock);

                // 512 - offsetInSector);

                if (offsetInSector != 0 || toRead < Sizes.Sector) {
                    byte mask = (byte) (1 << (7 - sectorInBlock % 8));
                    if ((blockBitmaps[(int) block][sectorInBlock / 8] & mask) != 0) {
                        long extentStart = (long) (blockAllocationTable[(int) block] + sectorInBlock) * Sizes.Sector +
                                blockBitmapSize + offsetInSector;
                        result.add(new StreamExtent(extentStart, toRead));
                    }

                    numRead += toRead;
                    position += toRead;
                } else {
                    // Processing at least one whole sector, read as many as possible
                    int toReadSectors = toRead / Sizes.Sector;

                    byte mask = (byte) (1 << (7 - sectorInBlock % 8));
                    boolean readFromParent = (blockBitmaps[(int) block][sectorInBlock / 8] & mask) == 0;

                    int numSectors = 1;
                    while (numSectors < toReadSectors) {
                        mask = (byte) (1 << (7 - (sectorInBlock + numSectors) % 8));
                        if ((blockBitmaps[(int) block][(sectorInBlock + numSectors) / 8] & mask) == 0 != readFromParent) {
                            break;
                        }

                        ++numSectors;
                    }

                    toRead = numSectors * Sizes.Sector;

                    if (!readFromParent) {
                        long extentStart = (long) (blockAllocationTable[(int) block] + sectorInBlock) * Sizes.Sector +
                                blockBitmapSize;
                        result.add(new StreamExtent(extentStart, toRead));
                    }

                    numRead += toRead;
                    position += toRead;
                }
            } else {
                int toRead = Math.min(maxToRead - numRead, dynamicHeader.blockSize - offsetInBlock);
                numRead += toRead;
                position += toRead;
            }
        }
        return result;
    }

    @Override
    public int read(byte[] buffer, int offset, int count) {
        checkDisposed();
        if (atEof || position > length) {
            atEof = true;
            throw new dotnet4j.io.IOException("Attempt to read beyond end of file");
        }

        if (position == length) {
            atEof = true;
            return 0;
        }

        int maxToRead = (int) Math.min(count, length - position);
        int numRead = 0;
        while (numRead < maxToRead) {
            long block = position / dynamicHeader.blockSize;
            int offsetInBlock = (int) (position % dynamicHeader.blockSize);
            if (populateBlockBitmap(block)) {
                int sectorInBlock = offsetInBlock / Sizes.Sector;
                int offsetInSector = offsetInBlock % Sizes.Sector;
                int toRead = Math.min(maxToRead - numRead, dynamicHeader.blockSize - offsetInBlock);
                // 512 - offsetInSector);
                if (offsetInSector != 0 || toRead < Sizes.Sector) {
                    byte mask = (byte) (1 << (7 - sectorInBlock % 8));
                    if ((blockBitmaps[(int) block][sectorInBlock / 8] & mask) != 0) {
                        fileStream.position((long) (blockAllocationTable[(int) block] + sectorInBlock) * Sizes.Sector +
                                blockBitmapSize + offsetInSector);
                        StreamUtilities.readExact(fileStream, buffer, offset + numRead, toRead);
                    } else {
                        parentStream.position(position);
                        StreamUtilities.readExact(parentStream, buffer, offset + numRead, toRead);
                    }
                    numRead += toRead;
                    position += toRead;
                } else {
                    // Processing at least one whole sector, read as many as possible
                    int toReadSectors = toRead / Sizes.Sector;
                    byte mask = (byte) (1 << (7 - sectorInBlock % 8));
                    boolean readFromParent = (blockBitmaps[(int) block][sectorInBlock / 8] & mask) == 0;
                    int numSectors = 1;
                    while (numSectors < toReadSectors) {
                        mask = (byte) (1 << (7 - (sectorInBlock + numSectors) % 8));
                        if ((blockBitmaps[(int) block][(sectorInBlock + numSectors) / 8] & mask) == 0 != readFromParent) {
                            break;
                        }

                        ++numSectors;
                    }
                    toRead = numSectors * Sizes.Sector;
                    if (readFromParent) {
                        parentStream.position(position);
                        StreamUtilities.readExact(parentStream, buffer, offset + numRead, toRead);
                    } else {
                        fileStream.position((long) (blockAllocationTable[(int) block] + sectorInBlock) * Sizes.Sector + blockBitmapSize);
                        StreamUtilities.readExact(fileStream, buffer, offset + numRead, toRead);
                    }
                    numRead += toRead;
                    position += toRead;
                }
            } else {
                int toRead = Math.min(maxToRead - numRead, dynamicHeader.blockSize - offsetInBlock);
                parentStream.position(position);
                StreamUtilities.readExact(parentStream, buffer, offset + numRead, toRead);
                numRead += toRead;
                position += toRead;
            }
        }
        return numRead;
    }

    @Override
    public long seek(long offset, SeekOrigin origin) {
        checkDisposed();
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += length;
        }

        atEof = false;
        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of disk");
        }

        position = effectiveOffset;
        return position;
    }

    @Override
    public void setLength(long value) {
        checkDisposed();
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(byte[] buffer, int offset, int count) {
        checkDisposed();
        if (!canWrite()) {
            throw new dotnet4j.io.IOException("Attempt to write to read-only stream");
        }

        if (position + count > length) {
            throw new dotnet4j.io.IOException("Attempt to write beyond end of the stream");
        }

        int numWritten = 0;
        while (numWritten < count) {
            long block = position / dynamicHeader.blockSize;
            int offsetInBlock = (int) (position % dynamicHeader.blockSize);
            if (!populateBlockBitmap(block)) {
                allocateBlock(block);
            }

            int sectorInBlock = offsetInBlock / Sizes.Sector;
            int offsetInSector = offsetInBlock % Sizes.Sector;
            int toWrite = Math.min(count - numWritten, dynamicHeader.blockSize - offsetInBlock);
            boolean blockBitmapDirty = false;
            // Need to read - we're not handling a full sector
            if (offsetInSector != 0 || toWrite < Sizes.Sector) {
                // Reduce the write to just the end of the current sector
                toWrite = Math.min(count - numWritten, Sizes.Sector - offsetInSector);
                byte sectorMask = (byte) (1 << (7 - sectorInBlock % 8));
                long sectorStart = (long) (blockAllocationTable[(int) block] + sectorInBlock) * Sizes.Sector + blockBitmapSize;
                // Get the existing sector data (if any), or otherwise the parent's content
                byte[] sectorBuffer;
                if ((blockBitmaps[(int) block][sectorInBlock / 8] & sectorMask) != 0) {
                    fileStream.position(sectorStart);
                    sectorBuffer = StreamUtilities.readExact(fileStream, Sizes.Sector);
                } else {
                    parentStream.position(position / Sizes.Sector * Sizes.Sector);
                    sectorBuffer = StreamUtilities.readExact(parentStream, Sizes.Sector);
                }
                // Overlay as much data as we have for this sector
                System.arraycopy(buffer, offset + numWritten, sectorBuffer, offsetInSector, toWrite);
                // Write the sector back
                fileStream.position(sectorStart);
                fileStream.write(sectorBuffer, 0, Sizes.Sector);
                // Update the in-memory block bitmap
                if ((blockBitmaps[(int) block][sectorInBlock / 8] & sectorMask) == 0) {
                    blockBitmaps[(int) block][sectorInBlock / 8] |= sectorMask;
                    blockBitmapDirty = true;
                }

            } else {
                // Processing at least one whole sector, just write (after making sure to trim any partial sectors from the end)...
                toWrite = toWrite / Sizes.Sector * Sizes.Sector;
                fileStream.position((long) (blockAllocationTable[(int) block] + sectorInBlock) * Sizes.Sector + blockBitmapSize);
                fileStream.write(buffer, offset + numWritten, toWrite);
                for (int i = offset; i < offset + toWrite; i += Sizes.Sector) {
                    // Update all of the bits in the block bitmap
                    byte sectorMask = (byte) (1 << (7 - sectorInBlock % 8));
                    if ((blockBitmaps[(int) block][sectorInBlock / 8] & sectorMask) == 0) {
                        blockBitmaps[(int) block][sectorInBlock / 8] |= sectorMask;
                        blockBitmapDirty = true;
                    }

                    sectorInBlock++;
                }
            }
            if (blockBitmapDirty) {
                writeBlockBitmap(block);
            }

            numWritten += toWrite;
            position += toWrite;
        }
        atEof = false;
    }

    @Override
    public List<StreamExtent> getExtentsInRange(long start, long count) {
        checkDisposed();
        long maxCount = Math.min(getLength(), start + count) - start;
        if (maxCount < 0) {
            return Collections.emptyList();
        }

        List<StreamExtent> parentExtents = parentStream.getExtentsInRange(start, maxCount);
        List<StreamExtent> result = StreamExtent.union(layerExtents(start, maxCount), parentExtents);
        result = StreamExtent.intersect(result, Collections.singletonList(new StreamExtent(start, maxCount)));
        return result;
    }

    @Override
    public void close() throws IOException {
        updateFooter();
        if (ownsParentStream == Ownership.Dispose && parentStream != null) {
            parentStream.close();
            parentStream = null;
        }
    }

    private List<StreamExtent> layerExtents(long start, long count) {
        List<StreamExtent> result = new ArrayList<>();
        long maxPos = start + count;
        long pos = findNextPresentSector(MathUtilities.roundDown(start, Sizes.Sector), maxPos);
        while (pos < maxPos) {
            long end = findNextAbsentSector(pos, maxPos);
            result.add(new StreamExtent(pos, end - pos));

            pos = findNextPresentSector(end, maxPos);
        }
        return result;
    }

    private long findNextPresentSector(long pos, long maxPos) {
        boolean foundStart = false;
        while (pos < maxPos && !foundStart) {
            long block = pos / dynamicHeader.blockSize;
            if (!populateBlockBitmap(block)) {
                pos += dynamicHeader.blockSize;
            } else {
                int offsetInBlock = (int) (pos % dynamicHeader.blockSize);
                int sectorInBlock = offsetInBlock / Sizes.Sector;
                if (blockBitmaps[(int) block][sectorInBlock / 8] == 0) {
                    pos += (8 - sectorInBlock % 8) * Sizes.Sector;
                } else {
                    byte mask = (byte) (1 << (7 - sectorInBlock % 8));
                    if ((blockBitmaps[(int) block][sectorInBlock / 8] & mask) != 0) {
                        foundStart = true;
                    } else {
                        pos += Sizes.Sector;
                    }
                }
            }
        }
        return Math.min(pos, maxPos);
    }

    private long findNextAbsentSector(long pos, long maxPos) {
        boolean foundEnd = false;
        while (pos < maxPos && !foundEnd) {
            long block = pos / dynamicHeader.blockSize;
            if (!populateBlockBitmap(block)) {
                foundEnd = true;
            } else {
                int offsetInBlock = (int) (pos % dynamicHeader.blockSize);
                int sectorInBlock = offsetInBlock / Sizes.Sector;
                if ((blockBitmaps[(int) block][sectorInBlock / 8] & 0xff) == 0xFF) {
                    pos += (8 - sectorInBlock % 8) * Sizes.Sector;
                } else {
                    byte mask = (byte) (1 << (7 - sectorInBlock % 8));
                    if ((blockBitmaps[(int) block][sectorInBlock / 8] & mask) == 0) {
                        foundEnd = true;
                    } else {
                        pos += Sizes.Sector;
                    }
                }
            }
        }
        return Math.min(pos, maxPos);
    }

    private void readBlockAllocationTable() {
        fileStream.position(dynamicHeader.tableOffset);
        byte[] data = StreamUtilities.readExact(fileStream, dynamicHeader.maxTableEntries * 4);
        int[] bat = new int[dynamicHeader.maxTableEntries];
        for (int i = 0; i < dynamicHeader.maxTableEntries; ++i) {
            bat[i] = ByteUtil.readBeInt(data, i * 4);
        }
        blockAllocationTable = bat;
    }

    private boolean populateBlockBitmap(long block) {
        if (blockBitmaps[(int) block] != null) {
            return true;
        }

        // Nothing to do...
        if (blockAllocationTable[(int) block] == 0xffff_ffff) { // uint.MAX_VALUE
            return false;
        }

        // No such block stored...
        // Read in bitmap
        fileStream.position((long) blockAllocationTable[(int) block] * Sizes.Sector);
        blockBitmaps[(int) block] = StreamUtilities.readExact(fileStream, blockBitmapSize);
        return true;
    }

    private void allocateBlock(long block) {
        if (blockAllocationTable[(int) block] != 0xffff_ffff) { // uint.MAX_VALUE
            throw new IllegalArgumentException("Attempt to allocate existing block");
        }

        newBlocksAllocated = true;
        long newBlockStart = nextBlockStart;
        // Create and write new sector bitmap
        byte[] bitmap = new byte[blockBitmapSize];
        fileStream.position(newBlockStart);
        fileStream.write(bitmap, 0, blockBitmapSize);
        blockBitmaps[(int) block] = bitmap;
        nextBlockStart += blockBitmapSize + dynamicHeader.blockSize;
        if (fileStream.getLength() < nextBlockStart) {
            fileStream.setLength(nextBlockStart);
        }

        // Update the BAT entry for the new block
        byte[] entryBuffer = new byte[4];
        ByteUtil.writeBeInt((int) (newBlockStart / 512), entryBuffer, 0);
        fileStream.position(dynamicHeader.tableOffset + block * 4);
        fileStream.write(entryBuffer, 0, 4);
        blockAllocationTable[(int) block] = (int) (newBlockStart / 512);
        if (autoCommitFooter) {
            updateFooter();
        }
    }

    private void writeBlockBitmap(long block) {
        fileStream.position((long) blockAllocationTable[(int) block] * Sizes.Sector);
        fileStream.write(blockBitmaps[(int) block], 0, blockBitmapSize);
    }

    private void checkDisposed() {
        if (parentStream == null) {
            throw new dotnet4j.io.IOException("DynamicStream: Attempt to use closed stream");
        }
    }

    private void updateFooter() {
        if (newBlocksAllocated) {
            // Update the footer at the end of the file (if we allocated new blocks).
            if (footerCache == null) {
                fileStream.position(0);
                footerCache = StreamUtilities.readExact(fileStream, Sizes.Sector);
            }

            fileStream.position(nextBlockStart);
            fileStream.write(footerCache, 0, footerCache.length);
        }
    }
}
