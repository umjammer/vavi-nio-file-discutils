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

package discUtils.vhdx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import discUtils.core.internal.ObjectCache;
import discUtils.streams.MappedStream;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


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

        return _canWrite /* ?? _fileStream.canWrite() */;
    }

    public List<StreamExtent> getExtents() {
        checkDisposed();

        // For now, report the complete file contents
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
            throw new dotnet4j.io.IOException("Attempt to read beyond end of file");
        }

        if (_position == _length) {
            _atEof = true;
            return 0;
        }

        if (_position % _metadata.getLogicalSectorSize() != 0 || count % _metadata.getLogicalSectorSize() != 0) {
            throw new dotnet4j.io.IOException("Unaligned read");
        }

        int totalToRead = (int) Math.min(_length - _position, count);
        int totalRead = 0;

        while (totalRead < totalToRead) {
            int[] chunkIndex = new int[1];
            int[] blockIndex = new int[1];
            int[] sectorIndex = new int[1];
            Chunk chunk = getChunk(_position + totalRead, chunkIndex, blockIndex, sectorIndex);

            int blockOffset = sectorIndex[0] * _metadata.getLogicalSectorSize();
            int blockBytesRemaining = _fileParameters.BlockSize - blockOffset;

            PayloadBlockStatus blockStatus = chunk.getBlockStatus(blockIndex[0]);
            if (blockStatus == PayloadBlockStatus.FullyPresent) {
                _fileStream.setPosition(chunk.getBlockPosition(blockIndex[0]) + blockOffset);
                int read = StreamUtilities.readMaximum(_fileStream,
                                                       buffer,
                                                       offset + totalRead,
                                                       Math.min(blockBytesRemaining, totalToRead - totalRead));

                totalRead += read;
            } else if (blockStatus == PayloadBlockStatus.PartiallyPresent) {
                BlockBitmap bitmap = chunk.getBlockBitmap(blockIndex[0]);

                boolean[] present = new boolean[1];
                int numSectors = bitmap.contiguousSectors(sectorIndex[0], present);
                int toRead = Math.min(numSectors * _metadata.getLogicalSectorSize(), totalToRead - totalRead);
                int read;

                if (present[0]) {
                    _fileStream.setPosition(chunk.getBlockPosition(blockIndex[0]) + blockOffset);
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
                Arrays.fill(buffer, offset + totalRead, offset + totalRead + zeroed, (byte) 0);
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
            throw new dotnet4j.io.IOException("Attempt to write to read-only VHDX");
        }

        if (_position % _metadata.getLogicalSectorSize() != 0 || count % _metadata.getLogicalSectorSize() != 0) {
            throw new dotnet4j.io.IOException("Unaligned read");
        }

        int totalWritten = 0;

        while (totalWritten < count) {
            int[] chunkIndex = new int[1];
            int[] blockIndex = new int[1];
            int[] sectorIndex = new int[1];
            Chunk chunk = getChunk(_position + totalWritten, chunkIndex, blockIndex, sectorIndex);

            int blockOffset = sectorIndex[0] * _metadata.getLogicalSectorSize();
            int blockBytesRemaining = _fileParameters.BlockSize - blockOffset;

            PayloadBlockStatus blockStatus = chunk.getBlockStatus(blockIndex[0]);
            if (blockStatus != PayloadBlockStatus.FullyPresent && blockStatus != PayloadBlockStatus.PartiallyPresent) {
                blockStatus = chunk.allocateSpaceForBlock(blockIndex[0]);
            }

            int toWrite = Math.min(blockBytesRemaining, count - totalWritten);
            _fileStream.setPosition(chunk.getBlockPosition(blockIndex[0]) + blockOffset);
            _fileStream.write(buffer, offset + totalWritten, toWrite);

            if (blockStatus == PayloadBlockStatus.PartiallyPresent) {
                BlockBitmap bitmap = chunk.getBlockBitmap(blockIndex[0]);
                boolean changed = bitmap.markSectorsPresent(sectorIndex[0], toWrite / _metadata.getLogicalSectorSize());

                if (changed) {
                    chunk.writeBlockBitmap(blockIndex[0]);
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
                    result.add(new StreamExtent(pos + (long) i * _metadata.getFileParameters().BlockSize,
                                                _metadata.getFileParameters().BlockSize));
                    break;
                }
            }

            pos += chunkSize;
        }

        return result;
    }

    /**
     * @param chunk {@cs out}
     * @param block {@cs out}
     * @param sector {@cs out}
     */
    private Chunk getChunk(long position, int[] chunk, int[] block, int[] sector) {
        long chunkSize = (1L << 23) * _metadata.getLogicalSectorSize();
        int chunkRatio = (int) (chunkSize / _metadata.getFileParameters().BlockSize);

        chunk[0] = (int) (position / chunkSize);
        long chunkOffset = position % chunkSize;

        block[0] = (int) (chunkOffset / _fileParameters.BlockSize);
        int blockOffset = (int) (chunkOffset % _fileParameters.BlockSize);

        sector[0] = blockOffset / _metadata.getLogicalSectorSize();

        Chunk result = _chunks.get(chunk[0]);
        if (result == null) {
            result = new Chunk(_batStream, _fileStream, _freeSpaceTable, _fileParameters, chunk[0], chunkRatio);
            _chunks.put(chunk[0], result);
        }

        return result;
    }

    private void checkDisposed() {
        if (_parentStream == null) {
            throw new dotnet4j.io.IOException("ContentStream: Attempt to use closed stream");
        }
    }
}
