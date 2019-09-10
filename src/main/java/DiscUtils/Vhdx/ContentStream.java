//
// Copyright (c) 2008-2012, Kenneth Bell
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

package DiscUtils.Vhdx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import DiscUtils.Core.Internal.ObjectCache;
import DiscUtils.Streams.MappedStream;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.SeekOrigin;
import moe.yo3explorer.dotnetio4j.Stream;


public final class ContentStream extends MappedStream {
    private boolean _atEof;

    private final Stream _batStream;

    private final boolean _canWrite;

    private final ObjectCache<Integer, Chunk> _chunks;

    private final FileParameters _fileParameters;

    private final SparseStream _fileStream;

    private final FreeSpaceTable _freeSpaceTable;

    private final long _length;

    private final Metadata _metadata;

    private final Ownership _ownsParent;

    private SparseStream _parentStream;

    private long _position;

    public ContentStream(SparseStream fileStream,
            boolean canWrite,
            Stream batStream,
            FreeSpaceTable freeSpaceTable,
            Metadata metadata,
            long length,
            SparseStream parentStream,
            Ownership ownsParent) {
        _fileStream = fileStream;
        _canWrite = canWrite;
        _batStream = batStream;
        _freeSpaceTable = freeSpaceTable;
        _metadata = metadata;
        _fileParameters = _metadata.getFileParameters();
        _length = length;
        _parentStream = parentStream;
        _ownsParent = ownsParent;
        _chunks = new ObjectCache<>();
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
        return _canWrite ? _canWrite : _fileStream.canWrite();
    }

    public List<StreamExtent> getExtents() {
        checkDisposed();
        return getExtentsInRange(0, getLength());
    }

    // For now, report the complete file contents
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
        throw new UnsupportedOperationException();
    }

    public List<StreamExtent> mapContent(long start, long length) {
        checkDisposed();
        throw new UnsupportedOperationException();
    }

    public List<StreamExtent> getExtentsInRange(long start, long count) {
        checkDisposed();
        return StreamExtent
                .intersect(StreamExtent.union(getExtentsRaw(start, count), _parentStream.getExtentsInRange(start, count)),
                           new StreamExtent(start, count));
    }

    public int read(byte[] buffer, int offset, int count) {
        checkDisposed();
        if (_atEof || _position > _length) {
            _atEof = true;
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to read beyond end of file");
        }

        if (_position == _length) {
            _atEof = true;
            return 0;
        }

        if (_position % _metadata.getLogicalSectorSize() != 0 || count % _metadata.getLogicalSectorSize() != 0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Unaligned read");
        }

        int totalToRead = (int) Math.min(_length - _position, count);
        int totalRead = 0;
        while (totalRead < totalToRead) {
            int chunkIndex;
            int blockIndex;
            int sectorIndex;
            int[] refVar___0 = new int[] {};
            int[] refVar___1 = new int[] {};
            int[] refVar___2 = new int[] {};
            Chunk chunk = getChunk(_position + totalRead, refVar___0, refVar___1, refVar___2);
            chunkIndex = refVar___0[0];
            blockIndex = refVar___1[0];
            sectorIndex = refVar___2[0];
            int blockOffset = sectorIndex * _metadata.getLogicalSectorSize();
            int blockBytesRemaining = _fileParameters.BlockSize - blockOffset;
            PayloadBlockStatus blockStatus = chunk.getBlockStatus(blockIndex);
            if (blockStatus == PayloadBlockStatus.FullyPresent) {
                _fileStream.setPosition(chunk.getBlockPosition(blockIndex) + blockOffset);
                int read = StreamUtilities.readMaximum(_fileStream,
                                                       buffer,
                                                       offset + totalRead,
                                                       Math.min(blockBytesRemaining, totalToRead - totalRead));
                totalRead += read;
            } else if (blockStatus == PayloadBlockStatus.PartiallyPresent) {
                BlockBitmap bitmap = chunk.getBlockBitmap(blockIndex);
                boolean[] present = new boolean[1];
                int numSectors = bitmap.contiguousSectors(sectorIndex, present);
                int toRead = Math.min(numSectors * _metadata.getLogicalSectorSize(), totalToRead - totalRead);
                int read;
                if (present[0]) {
                    _fileStream.setPosition(chunk.getBlockPosition(blockIndex) + blockOffset);
                    read = StreamUtilities.readMaximum(_fileStream, buffer, offset + totalRead, toRead);
                } else {
                    _parentStream.setPosition(_position + totalRead);
                    read = StreamUtilities.readMaximum(_parentStream, buffer, offset + totalRead, toRead);
                }
                totalRead += read;
            } else if (blockStatus == PayloadBlockStatus.NotPresent) {
                _parentStream.setPosition(_position + totalRead);
                int read = StreamUtilities.readMaximum(_parentStream,
                                                       buffer,
                                                       offset + totalRead,
                                                       Math.min(blockBytesRemaining, totalToRead - totalRead));
                totalRead += read;
            } else {
                int zeroed = Math.min(blockBytesRemaining, totalToRead - totalRead);
                Arrays.fill(buffer, offset + totalRead, zeroed, (byte) 0);
                totalRead += zeroed;
            }
        }
        _position += totalRead;
        return totalRead;
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
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to move before beginning of disk");
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
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to write to read-only VHDX");
        }

        if (_position % _metadata.getLogicalSectorSize() != 0 || count % _metadata.getLogicalSectorSize() != 0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Unaligned read");
        }

        int totalWritten = 0;
        while (totalWritten < count) {
            int chunkIndex;
            int blockIndex;
            int sectorIndex;
            int[] refVar___4 = new int[] {};
            int[] refVar___5 = new int[] {};
            int[] refVar___6 = new int[] {};
            Chunk chunk = getChunk(_position + totalWritten, refVar___4, refVar___5, refVar___6);
            chunkIndex = refVar___4[0];
            blockIndex = refVar___5[0];
            sectorIndex = refVar___6[0];
            int blockOffset = sectorIndex * _metadata.getLogicalSectorSize();
            int blockBytesRemaining = _fileParameters.BlockSize - blockOffset;
            PayloadBlockStatus blockStatus = chunk.getBlockStatus(blockIndex);
            if (blockStatus != PayloadBlockStatus.FullyPresent && blockStatus != PayloadBlockStatus.PartiallyPresent) {
                blockStatus = chunk.allocateSpaceForBlock(blockIndex);
            }

            int toWrite = Math.min(blockBytesRemaining, count - totalWritten);
            _fileStream.setPosition(chunk.getBlockPosition(blockIndex) + blockOffset);
            _fileStream.write(buffer, offset + totalWritten, toWrite);
            if (blockStatus == PayloadBlockStatus.PartiallyPresent) {
                BlockBitmap bitmap = chunk.getBlockBitmap(blockIndex);
                boolean changed = bitmap.markSectorsPresent(sectorIndex, toWrite / _metadata.getLogicalSectorSize());
                if (changed) {
                    chunk.writeBlockBitmap(blockIndex);
                }

            }

            totalWritten += toWrite;
        }
        _position += totalWritten;
    }

    public void close() throws IOException {
        if (_parentStream != null) {
            if (_ownsParent == Ownership.Dispose) {
                _parentStream.close();
            }

            _parentStream = null;
        }
    }

    private List<StreamExtent> getExtentsRaw(long start, long count) {
        List<StreamExtent> result = new ArrayList<>();
        long chunkSize = (1L << 23) * _metadata.getLogicalSectorSize();
        int chunkRatio = (int) (chunkSize / _metadata.getFileParameters().BlockSize);
        long pos = MathUtilities.roundDown(start, chunkSize);
        while (pos < start + count) {
            int[] chunkIndex = new int[1];
            int[] blockIndex = new int[1];
            int[] sectorIndex = new int[1];
            Chunk chunk = getChunk(pos, chunkIndex, blockIndex, sectorIndex);
            for (int i = 0; i < chunkRatio; ++i) {
                switch (chunk.getBlockStatus(i)) {
                case NotPresent:
                case Undefined:
                case Unmapped:
                case Zero:
                    break;
                default:
                    result.add(new StreamExtent(pos + i * _metadata.getFileParameters().BlockSize,
                                                _metadata.getFileParameters().BlockSize));
                    break;

                }
            }
            pos += chunkSize;
        }
        return result;
    }

    private Chunk getChunk(long position, int[] chunk, int[] block, int[] sector) {
        long chunkSize = (1L << 23) * _metadata.getLogicalSectorSize();
        int chunkRatio = (int) (chunkSize / _metadata.getFileParameters().BlockSize);
        chunk[0] = (int) (position / chunkSize);
        long chunkOffset = position % chunkSize;
        block[0] = (int) (chunkOffset / _fileParameters.BlockSize);
        int blockOffset = (int) (chunkOffset % _fileParameters.BlockSize);
        sector[0] = blockOffset / _metadata.getLogicalSectorSize();
        Chunk result = _chunks.get___idx(chunk[0]);
        if (result == null) {
            result = new Chunk(_batStream, _fileStream, _freeSpaceTable, _fileParameters, chunk[0], chunkRatio);
            _chunks.set___idx(chunk[0], result);
        }

        return result;
    }

    private void checkDisposed() {
        if (_parentStream == null) {
            throw new moe.yo3explorer.dotnetio4j.IOException("ContentStream: Attempt to use closed stream");
        }

    }

}
