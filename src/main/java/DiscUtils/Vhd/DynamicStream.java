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

package DiscUtils.Vhd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import DiscUtils.Streams.MappedStream;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public class DynamicStream extends MappedStream {
    private boolean _atEof;

    private boolean _autoCommitFooter = true;

    private int[] _blockAllocationTable;

    private final byte[][] _blockBitmaps;

    private final int _blockBitmapSize;

    private final DynamicHeader _dynamicHeader;

    private final Stream _fileStream;

    private byte[] _footerCache;

    private final long _length;

    private boolean _newBlocksAllocated;

    private long _nextBlockStart;

    private final Ownership _ownsParentStream;

    private SparseStream _parentStream;

    private long _position;

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

        _fileStream = fileStream;
        _dynamicHeader = dynamicHeader;
        _length = length;
        _parentStream = parentStream;
        _ownsParentStream = ownsParentStream;
        _blockBitmaps = new byte[_dynamicHeader.MaxTableEntries][];
        _blockBitmapSize = MathUtilities.roundUp(MathUtilities.ceil(_dynamicHeader.BlockSize, Sizes.Sector * 8), Sizes.Sector);
        readBlockAllocationTable();
        // Detect where next block should go (cope if the footer is missing)
        _fileStream.setPosition(MathUtilities.roundDown(_fileStream.getLength(), Sizes.Sector) - Sizes.Sector);
        byte[] footerBytes = StreamUtilities.readExact(_fileStream, Sizes.Sector);
        Footer footer = Footer.fromBytes(footerBytes, 0);
        _nextBlockStart = _fileStream.getPosition() - (footer.isValid() ? Sizes.Sector : 0);
    }

    public boolean getAutoCommitFooter() {
        return _autoCommitFooter;
    }

    public void setAutoCommitFooter(boolean value) {
        _autoCommitFooter = value;
        if (_autoCommitFooter) {
            updateFooter();
        }

    }

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
        return _length;
    }

    public long getPosition() {
        checkDisposed();
        return _position;
    }

    public void setPosition(long value) {
        checkDisposed();
        _atEof = false;
        _position = value;
    }

    public void flush() {
        checkDisposed();
    }

    public List<StreamExtent> mapContent(long start, long length) {
        List<StreamExtent> result = new ArrayList<>();
        long position = start;
        int maxToRead = (int) Math.min(length, _length - position);
        int numRead = 0;
        while (numRead < maxToRead) {
            long block = position / _dynamicHeader.BlockSize;
            int offsetInBlock = (int) (position % _dynamicHeader.BlockSize);
            if (populateBlockBitmap(block)) {
                int sectorInBlock = offsetInBlock / Sizes.Sector;
                int offsetInSector = offsetInBlock % Sizes.Sector;
                int toRead = Math.min(maxToRead - numRead, _dynamicHeader.BlockSize - offsetInBlock);
                // 512 - offsetInSector);
                if (offsetInSector != 0 || toRead < Sizes.Sector) {
                    byte mask = (byte) (1 << (7 - sectorInBlock % 8));
                    if ((_blockBitmaps[(int) block][sectorInBlock / 8] & mask) != 0) {
                        long extentStart = (_blockAllocationTable[(int) block] + sectorInBlock) * Sizes.Sector +
                                           _blockBitmapSize + offsetInSector;
                        result.add(new StreamExtent(extentStart, toRead));
                    }

                    numRead += toRead;
                    position += toRead;
                } else {
                    // Processing at least one whole sector, read as many as possible
                    int toReadSectors = toRead / Sizes.Sector;
                    byte mask = (byte) (1 << (7 - sectorInBlock % 8));
                    boolean readFromParent = (_blockBitmaps[(int) block][sectorInBlock / 8] & mask) == 0;
                    int numSectors = 1;
                    while (numSectors < toReadSectors) {
                        mask = (byte) (1 << (7 - (sectorInBlock + numSectors) % 8));
                        if ((_blockBitmaps[(int) block][(sectorInBlock + numSectors) / 8] & mask) == 0 != readFromParent) {
                            break;
                        }

                        ++numSectors;
                    }
                    toRead = numSectors * Sizes.Sector;
                    if (!readFromParent) {
                        long extentStart = (_blockAllocationTable[(int) block] + sectorInBlock) * Sizes.Sector +
                                           _blockBitmapSize;
                        result.add(new StreamExtent(extentStart, toRead));
                    }

                    numRead += toRead;
                    position += toRead;
                }
            } else {
                int toRead = Math.min(maxToRead - numRead, _dynamicHeader.BlockSize - offsetInBlock);
                numRead += toRead;
                position += toRead;
            }
        }
        return result;
    }

    public int read(byte[] buffer, int offset, int count) {
        checkDisposed();
        if (_atEof || _position > _length) {
            _atEof = true;
            throw new dotnet4j.io.IOException("Attempt to read beyond end of file");
        }

        if (_position == _length) {
            _atEof = true;
            return 0;
        }

        int maxToRead = (int) Math.min(count, _length - _position);
        int numRead = 0;
        while (numRead < maxToRead) {
            long block = _position / _dynamicHeader.BlockSize;
            int offsetInBlock = (int) (_position % _dynamicHeader.BlockSize);
            if (populateBlockBitmap(block)) {
                int sectorInBlock = offsetInBlock / Sizes.Sector;
                int offsetInSector = offsetInBlock % Sizes.Sector;
                int toRead = Math.min(maxToRead - numRead, _dynamicHeader.BlockSize - offsetInBlock);
                // 512 - offsetInSector);
                if (offsetInSector != 0 || toRead < Sizes.Sector) {
                    byte mask = (byte) (1 << (7 - sectorInBlock % 8));
                    if ((_blockBitmaps[(int) block][sectorInBlock / 8] & mask) != 0) {
                        _fileStream.setPosition((_blockAllocationTable[(int) block] + sectorInBlock) * Sizes.Sector +
                                         _blockBitmapSize + offsetInSector);
                        StreamUtilities.readExact(_fileStream, buffer, offset + numRead, toRead);
                    } else {
                        _parentStream.setPosition(_position);
                        StreamUtilities.readExact(_parentStream, buffer, offset + numRead, toRead);
                    }
                    numRead += toRead;
                    _position += toRead;
                } else {
                    // Processing at least one whole sector, read as many as possible
                    int toReadSectors = toRead / Sizes.Sector;
                    byte mask = (byte) (1 << (7 - sectorInBlock % 8));
                    boolean readFromParent = (_blockBitmaps[(int) block][sectorInBlock / 8] & mask) == 0;
                    int numSectors = 1;
                    while (numSectors < toReadSectors) {
                        mask = (byte) (1 << (7 - (sectorInBlock + numSectors) % 8));
                        if ((_blockBitmaps[(int) block][(sectorInBlock + numSectors) / 8] & mask) == 0 != readFromParent) {
                            break;
                        }

                        ++numSectors;
                    }
                    toRead = numSectors * Sizes.Sector;
                    if (readFromParent) {
                        _parentStream.setPosition(_position);
                        StreamUtilities.readExact(_parentStream, buffer, offset + numRead, toRead);
                    } else {
                        _fileStream
                                .setPosition((_blockAllocationTable[(int) block] + sectorInBlock) * Sizes.Sector + _blockBitmapSize);
                        StreamUtilities.readExact(_fileStream, buffer, offset + numRead, toRead);
                    }
                    numRead += toRead;
                    _position += toRead;
                }
            } else {
                int toRead = Math.min(maxToRead - numRead, _dynamicHeader.BlockSize - offsetInBlock);
                _parentStream.setPosition(_position);
                StreamUtilities.readExact(_parentStream, buffer, offset + numRead, toRead);
                numRead += toRead;
                _position += toRead;
            }
        }
        return numRead;
    }

    public long seek(long offset, SeekOrigin origin) {
        checkDisposed();
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += _position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += _length;
        }

        _atEof = false;
        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of disk");
        }

        _position = effectiveOffset;
        return _position;
    }

    public void setLength(long value) {
        checkDisposed();
        throw new UnsupportedOperationException();
    }

    public void write(byte[] buffer, int offset, int count) {
        checkDisposed();
        if (!canWrite()) {
            throw new dotnet4j.io.IOException("Attempt to write to read-only stream");
        }

        if (_position + count > _length) {
            throw new dotnet4j.io.IOException("Attempt to write beyond end of the stream");
        }

        int numWritten = 0;
        while (numWritten < count) {
            long block = _position / _dynamicHeader.BlockSize;
            int offsetInBlock = (int) (_position % _dynamicHeader.BlockSize);
            if (!populateBlockBitmap(block)) {
                allocateBlock(block);
            }

            int sectorInBlock = offsetInBlock / Sizes.Sector;
            int offsetInSector = offsetInBlock % Sizes.Sector;
            int toWrite = Math.min(count - numWritten, _dynamicHeader.BlockSize - offsetInBlock);
            boolean blockBitmapDirty = false;
            // Need to read - we're not handling a full sector
            if (offsetInSector != 0 || toWrite < Sizes.Sector) {
                // Reduce the write to just the end of the current sector
                toWrite = Math.min(count - numWritten, Sizes.Sector - offsetInSector);
                byte sectorMask = (byte) (1 << (7 - sectorInBlock % 8));
                long sectorStart = (_blockAllocationTable[(int) block] + sectorInBlock) * Sizes.Sector + _blockBitmapSize;
                // Get the existing sector data (if any), or otherwise the parent's content
                byte[] sectorBuffer;
                if ((_blockBitmaps[(int) block][sectorInBlock / 8] & sectorMask) != 0) {
                    _fileStream.setPosition(sectorStart);
                    sectorBuffer = StreamUtilities.readExact(_fileStream, Sizes.Sector);
                } else {
                    _parentStream.setPosition(_position / Sizes.Sector * Sizes.Sector);
                    sectorBuffer = StreamUtilities.readExact(_parentStream, Sizes.Sector);
                }
                // Overlay as much data as we have for this sector
                System.arraycopy(buffer, offset + numWritten, sectorBuffer, offsetInSector, toWrite);
                // Write the sector back
                _fileStream.setPosition(sectorStart);
                _fileStream.write(sectorBuffer, 0, Sizes.Sector);
                // Update the in-memory block bitmap
                if ((_blockBitmaps[(int) block][sectorInBlock / 8] & sectorMask) == 0) {
                    _blockBitmaps[(int) block][sectorInBlock / 8] |= sectorMask;
                    blockBitmapDirty = true;
                }

            } else {
                // Processing at least one whole sector, just write (after making sure to trim any partial sectors from the end)...
                toWrite = toWrite / Sizes.Sector * Sizes.Sector;
                _fileStream.setPosition((_blockAllocationTable[(int) block] + sectorInBlock) * Sizes.Sector + _blockBitmapSize);
                _fileStream.write(buffer, offset + numWritten, toWrite);
                for (int i = offset; i < offset + toWrite; i += Sizes.Sector) {
                    // Update all of the bits in the block bitmap
                    byte sectorMask = (byte) (1 << (7 - sectorInBlock % 8));
                    if ((_blockBitmaps[(int) block][sectorInBlock / 8] & sectorMask) == 0) {
                        _blockBitmaps[(int) block][sectorInBlock / 8] |= sectorMask;
                        blockBitmapDirty = true;
                    }

                    sectorInBlock++;
                }
            }
            if (blockBitmapDirty) {
                writeBlockBitmap(block);
            }

            numWritten += toWrite;
            _position += toWrite;
        }
        _atEof = false;
    }

    public List<StreamExtent> getExtentsInRange(long start, long count) {
        checkDisposed();
        long maxCount = Math.min(getLength(), start + count) - start;
        if (maxCount < 0) {
            return Arrays.asList();
        }

        List<StreamExtent> parentExtents = _parentStream.getExtentsInRange(start, maxCount);
        List<StreamExtent> result = StreamExtent.union(layerExtents(start, maxCount), parentExtents);
        result = StreamExtent.intersect(result);
        return result;
    }

    public void close() throws IOException {
        updateFooter();
        if (_ownsParentStream == Ownership.Dispose && _parentStream != null) {
            _parentStream.close();
            _parentStream = null;
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
            long block = pos / _dynamicHeader.BlockSize;
            if (!populateBlockBitmap(block)) {
                pos += _dynamicHeader.BlockSize;
            } else {
                int offsetInBlock = (int) (pos % _dynamicHeader.BlockSize);
                int sectorInBlock = offsetInBlock / Sizes.Sector;
                if (_blockBitmaps[(int) block][sectorInBlock / 8] == 0) {
                    pos += (8 - sectorInBlock % 8) * Sizes.Sector;
                } else {
                    byte mask = (byte) (1 << (7 - sectorInBlock % 8));
                    if ((_blockBitmaps[(int) block][sectorInBlock / 8] & mask) != 0) {
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
            long block = pos / _dynamicHeader.BlockSize;
            if (!populateBlockBitmap(block)) {
                foundEnd = true;
            } else {
                int offsetInBlock = (int) (pos % _dynamicHeader.BlockSize);
                int sectorInBlock = offsetInBlock / Sizes.Sector;
                if ((_blockBitmaps[(int) block][sectorInBlock / 8] & 0xff) == 0xFF) {
                    pos += (8 - sectorInBlock % 8) * Sizes.Sector;
                } else {
                    byte mask = (byte) (1 << (7 - sectorInBlock % 8));
                    if ((_blockBitmaps[(int) block][sectorInBlock / 8] & mask) == 0) {
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
        _fileStream.setPosition(_dynamicHeader.TableOffset);
        byte[] data = StreamUtilities.readExact(_fileStream, _dynamicHeader.MaxTableEntries * 4);
        int[] bat = new int[_dynamicHeader.MaxTableEntries];
        for (int i = 0; i < _dynamicHeader.MaxTableEntries; ++i) {
            bat[i] = EndianUtilities.toUInt32BigEndian(data, i * 4);
        }
        _blockAllocationTable = bat;
    }

    private boolean populateBlockBitmap(long block) {
        if (_blockBitmaps[(int) block] != null) {
            return true;
        }

        // Nothing to do...
        if (_blockAllocationTable[(int) block] == 0xffffffff) { // uint.MAX_VALUE
            return false;
        }

        // No such block stored...
        // Read in bitmap
        _fileStream.setPosition((long) _blockAllocationTable[(int) block] * Sizes.Sector);
        _blockBitmaps[(int) block] = StreamUtilities.readExact(_fileStream, _blockBitmapSize);
        return true;
    }

    private void allocateBlock(long block) {
        if (_blockAllocationTable[(int) block] != 0xffffffff) { // uint.MAX_VALUE
            throw new IllegalArgumentException("Attempt to allocate existing block");
        }

        _newBlocksAllocated = true;
        long newBlockStart = _nextBlockStart;
        // Create and write new sector bitmap
        byte[] bitmap = new byte[_blockBitmapSize];
        _fileStream.setPosition(newBlockStart);
        _fileStream.write(bitmap, 0, _blockBitmapSize);
        _blockBitmaps[(int) block] = bitmap;
        _nextBlockStart += _blockBitmapSize + _dynamicHeader.BlockSize;
        if (_fileStream.getLength() < _nextBlockStart) {
            _fileStream.setLength(_nextBlockStart);
        }

        // Update the BAT entry for the new block
        byte[] entryBuffer = new byte[4];
        EndianUtilities.writeBytesBigEndian((int) (newBlockStart / 512), entryBuffer, 0);
        _fileStream.setPosition(_dynamicHeader.TableOffset + block * 4);
        _fileStream.write(entryBuffer, 0, 4);
        _blockAllocationTable[(int) block] = (int) (newBlockStart / 512);
        if (_autoCommitFooter) {
            updateFooter();
        }
    }

    private void writeBlockBitmap(long block) {
        _fileStream.setPosition((long) _blockAllocationTable[(int) block] * Sizes.Sector);
        _fileStream.write(_blockBitmaps[(int) block], 0, _blockBitmapSize);
    }

    private void checkDisposed() {
        if (_parentStream == null) {
            throw new dotnet4j.io.IOException("DynamicStream: Attempt to use closed stream");
        }
    }

    private void updateFooter() {
        if (_newBlocksAllocated) {
            // Update the footer at the end of the file (if we allocated new blocks).
            if (_footerCache == null) {
                _fileStream.setPosition(0);
                _footerCache = StreamUtilities.readExact(_fileStream, Sizes.Sector);
            }

            _fileStream.setPosition(_nextBlockStart);
            _fileStream.write(_footerCache, 0, _footerCache.length);
        }
    }
}
