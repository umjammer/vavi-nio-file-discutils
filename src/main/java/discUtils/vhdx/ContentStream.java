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

    private boolean atEof;

    private final Stream batStream;

    private final boolean canWrite;

    private final ObjectCache<Integer, Chunk> chunks;

    private final FileParameters fileParameters;

    private final SparseStream fileStream;

    private final FreeSpaceTable freeSpaceTable;

    private final long length;

    private final Metadata metadata;

    private final Ownership ownsParent;

    private SparseStream parentStream;

    private long position;

    public ContentStream(SparseStream fileStream,
            boolean canWrite,
            Stream batStream,
            FreeSpaceTable freeSpaceTable,
            Metadata metadata,
            long length,
            SparseStream parentStream,
            Ownership ownsParent) {
        this.fileStream = fileStream;
        this.canWrite = canWrite;
        this.batStream = batStream;
        this.freeSpaceTable = freeSpaceTable;
        this.metadata = metadata;
        fileParameters = this.metadata.getFileParameters();
        this.length = length;
        this.parentStream = parentStream;
        this.ownsParent = ownsParent;

        chunks = new ObjectCache<>();
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

        return canWrite /* ?? fileStream.canWrite() */;
    }

    public List<StreamExtent> getExtents() {
        checkDisposed();

        // For now, report the complete file contents
        return getExtentsInRange(0, getLength());
    }

    public long getLength() {
        checkDisposed();
        return length;
    }

    @Override public long position() {
        checkDisposed();
        return position;
    }

    @Override public void position(long value) {
        checkDisposed();
        atEof = false;
        position = value;
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
                .intersect(StreamExtent.union(getExtentsRaw(start, count), parentStream.getExtentsInRange(start, count)),
                           new StreamExtent(start, count));
    }

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

        if (position % metadata.getLogicalSectorSize() != 0 || count % metadata.getLogicalSectorSize() != 0) {
            throw new dotnet4j.io.IOException("Unaligned read");
        }

        int totalToRead = (int) Math.min(length - position, count);
        int totalRead = 0;

        while (totalRead < totalToRead) {
            int[] chunkIndex = new int[1];
            int[] blockIndex = new int[1];
            int[] sectorIndex = new int[1];
            Chunk chunk = getChunk(position + totalRead, chunkIndex, blockIndex, sectorIndex);

            int blockOffset = sectorIndex[0] * metadata.getLogicalSectorSize();
            int blockBytesRemaining = fileParameters.blockSize - blockOffset;

            PayloadBlockStatus blockStatus = chunk.getBlockStatus(blockIndex[0]);
            if (blockStatus == PayloadBlockStatus.FullyPresent) {
                fileStream.position(chunk.getBlockPosition(blockIndex[0]) + blockOffset);
                int read = StreamUtilities.readMaximum(fileStream,
                                                       buffer,
                                                       offset + totalRead,
                                                       Math.min(blockBytesRemaining, totalToRead - totalRead));

                totalRead += read;
            } else if (blockStatus == PayloadBlockStatus.PartiallyPresent) {
                BlockBitmap bitmap = chunk.getBlockBitmap(blockIndex[0]);

                boolean[] present = new boolean[1];
                int numSectors = bitmap.contiguousSectors(sectorIndex[0], present);
                int toRead = Math.min(numSectors * metadata.getLogicalSectorSize(), totalToRead - totalRead);
                int read;

                if (present[0]) {
                    fileStream.position(chunk.getBlockPosition(blockIndex[0]) + blockOffset);
                    read = StreamUtilities.readMaximum(fileStream, buffer, offset + totalRead, toRead);
                } else {
                    parentStream.position(position + totalRead);
                    read = StreamUtilities.readMaximum(parentStream, buffer, offset + totalRead, toRead);
                }
                totalRead += read;
            } else if (blockStatus == PayloadBlockStatus.NotPresent) {
                parentStream.position(position + totalRead);
                int read = StreamUtilities.readMaximum(parentStream,
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

        position += totalRead;
        return totalRead;
    }

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

    public void setLength(long value) {
        checkDisposed();

        throw new UnsupportedOperationException();
    }

    public void write(byte[] buffer, int offset, int count) {
        checkDisposed();

        if (!canWrite()) {
            throw new dotnet4j.io.IOException("Attempt to write to read-only VHDX");
        }

        if (position % metadata.getLogicalSectorSize() != 0 || count % metadata.getLogicalSectorSize() != 0) {
            throw new dotnet4j.io.IOException("Unaligned read");
        }

        int totalWritten = 0;

        while (totalWritten < count) {
            int[] chunkIndex = new int[1];
            int[] blockIndex = new int[1];
            int[] sectorIndex = new int[1];
            Chunk chunk = getChunk(position + totalWritten, chunkIndex, blockIndex, sectorIndex);

            int blockOffset = sectorIndex[0] * metadata.getLogicalSectorSize();
            int blockBytesRemaining = fileParameters.blockSize - blockOffset;

            PayloadBlockStatus blockStatus = chunk.getBlockStatus(blockIndex[0]);
            if (blockStatus != PayloadBlockStatus.FullyPresent && blockStatus != PayloadBlockStatus.PartiallyPresent) {
                blockStatus = chunk.allocateSpaceForBlock(blockIndex[0]);
            }

            int toWrite = Math.min(blockBytesRemaining, count - totalWritten);
            fileStream.position(chunk.getBlockPosition(blockIndex[0]) + blockOffset);
            fileStream.write(buffer, offset + totalWritten, toWrite);

            if (blockStatus == PayloadBlockStatus.PartiallyPresent) {
                BlockBitmap bitmap = chunk.getBlockBitmap(blockIndex[0]);
                boolean changed = bitmap.markSectorsPresent(sectorIndex[0], toWrite / metadata.getLogicalSectorSize());

                if (changed) {
                    chunk.writeBlockBitmap(blockIndex[0]);
                }
            }

            totalWritten += toWrite;
        }

        position += totalWritten;
    }

    public void close() throws IOException {
        if (parentStream != null) {
            if (ownsParent == Ownership.Dispose) {
                parentStream.close();
            }

            parentStream = null;
        }
    }

    private List<StreamExtent> getExtentsRaw(long start, long count) {
        List<StreamExtent> result = new ArrayList<>();
        long chunkSize = (1L << 23) * metadata.getLogicalSectorSize();
        int chunkRatio = (int) (chunkSize / metadata.getFileParameters().blockSize);

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
                    result.add(new StreamExtent(pos + (long) i * metadata.getFileParameters().blockSize,
                                                metadata.getFileParameters().blockSize));
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
        long chunkSize = (1L << 23) * metadata.getLogicalSectorSize();
        int chunkRatio = (int) (chunkSize / metadata.getFileParameters().blockSize);

        chunk[0] = (int) (position / chunkSize);
        long chunkOffset = position % chunkSize;

        block[0] = (int) (chunkOffset / fileParameters.blockSize);
        int blockOffset = (int) (chunkOffset % fileParameters.blockSize);

        sector[0] = blockOffset / metadata.getLogicalSectorSize();

        Chunk result = chunks.get(chunk[0]);
        if (result == null) {
            result = new Chunk(batStream, fileStream, freeSpaceTable, fileParameters, chunk[0], chunkRatio);
            chunks.put(chunk[0], result);
        }

        return result;
    }

    private void checkDisposed() {
        if (parentStream == null) {
            throw new dotnet4j.io.IOException("ContentStream: Attempt to use closed stream");
        }
    }
}
