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

package discUtils.vdi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import discUtils.core.internal.Utilities;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public class DiskStream extends SparseStream {

    private static final int BlockFree = 0xffffffff;

    private static final int BlockZero = 0xfffffffe;

    private boolean atEof;

    private int[] blockTable;

    private final HeaderRecord fileHeader;

    private Stream fileStream;

    private boolean isDisposed;

    private final Ownership ownsStream;

    private long position;

    private boolean writeNotified;

    public DiskStream(Stream fileStream, Ownership ownsStream, HeaderRecord fileHeader) {
        this.fileStream = fileStream;
        this.fileHeader = fileHeader;

        this.ownsStream = ownsStream;

        readBlockTable();
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
        return fileStream.canWrite();
    }

    public List<StreamExtent> getExtents() {
        List<StreamExtent> extents = new ArrayList<>();

        long blockSize = fileHeader.blockSize;
        int i = 0;
        while (i < blockTable.length) {
            // Find next stored block
            while (i < blockTable.length && (blockTable[i] == BlockZero || blockTable[i] == BlockFree)) {
                ++i;
            }

            int start = i;

            // Find next absent block
            while (i < blockTable.length && blockTable[i] != BlockZero && blockTable[i] != BlockFree) {
                ++i;
            }

            if (start != i) {
                extents.add(new StreamExtent(start * blockSize, (i - start) * blockSize));
            }
        }

        return extents;
    }

    public long getLength() {
        checkDisposed();
        return fileHeader.diskSize;
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

    public BiConsumer<Object, Object[]> writeOccurred;

    public void flush() {
        checkDisposed();
    }

    public int read(byte[] buffer, int offset, int count) {
        checkDisposed();

        if (atEof || position > fileHeader.diskSize) {
            atEof = true;
            throw new dotnet4j.io.IOException("Attempt to read beyond end of file");
        }

        if (position == fileHeader.diskSize) {
            atEof = true;
            return 0;
        }

        int maxToRead = (int) Math.min(count, fileHeader.diskSize - position);
        int numRead = 0;

        while (numRead < maxToRead) {
            int block = (int) (position / fileHeader.blockSize);
            int offsetInBlock = (int) (position % fileHeader.blockSize);

            int toRead = Math.min(maxToRead - numRead, fileHeader.blockSize - offsetInBlock);

            if (blockTable[block] == BlockFree) {
                // TODO: Use parent
                Arrays.fill(buffer, offset + numRead, offset + numRead + toRead, (byte) 0);
            } else if (blockTable[block] == BlockZero) {
                Arrays.fill(buffer, offset + numRead, offset + numRead + toRead, (byte) 0);
            } else {
                long blockOffset = (blockTable[block] & 0xffff_ffffL) * (fileHeader.blockSize + fileHeader.blockExtraSize);
                long filePos = fileHeader.dataOffset + fileHeader.blockExtraSize + blockOffset + offsetInBlock;
                fileStream.setPosition(filePos);
                StreamUtilities.readExact(fileStream, buffer, offset + numRead, toRead);
            }

            position += toRead;
            numRead += toRead;
        }

        return numRead;
    }

    public long seek(long offset, SeekOrigin origin) {
        checkDisposed();
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += fileHeader.diskSize;
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
            throw new dotnet4j.io.IOException("Attempt to write to read-only stream");
        }

        if (count < 0) {
            throw new IndexOutOfBoundsException("Attempt to write negative number of bytes (count)");
        }

        if (atEof || position + count > fileHeader.diskSize) {
            atEof = true;
            throw new dotnet4j.io.IOException("Attempt to write beyond end of file");
        }

        // On first write, notify event listeners - they just get to find out that some
        // write occurred, not about each write.
        if (!writeNotified) {
            onWriteOccurred();
            writeNotified = true;
        }

        int numWritten = 0;
        while (numWritten < count) {
            int block = (int) (position / fileHeader.blockSize);
            int offsetInBlock = (int) (position % fileHeader.blockSize);

            int toWrite = Math.min(count - numWritten, fileHeader.blockSize - offsetInBlock);

            // Optimize away zero-writes
            if (blockTable[block] == BlockZero || (blockTable[block] == BlockFree && toWrite == fileHeader.blockSize)) {
                if (Utilities.isAllZeros(buffer, offset + numWritten, toWrite)) {
                    numWritten += toWrite;
                    position += toWrite;
                    continue;
                }
            }

            if (blockTable[block] == BlockFree || blockTable[block] == BlockZero) {
                byte[] writeBuffer = buffer;
                int writeBufferOffset = offset + numWritten;

                if (toWrite != fileHeader.blockSize) {
                    writeBuffer = new byte[fileHeader.blockSize];
                    if (blockTable[block] == BlockFree) {
                        // TODO: Use parent stream data...
                    }

                    // Copy actual data into temporary buffer, then this is a full block write.
                    System.arraycopy(buffer, offset + numWritten, writeBuffer, offsetInBlock, toWrite);
                    writeBufferOffset = 0;
                }

                long blockOffset = (long) fileHeader.blocksAllocated * (fileHeader.blockSize + fileHeader.blockExtraSize);
                long filePos = fileHeader.dataOffset + fileHeader.blockExtraSize + blockOffset;

                fileStream.setPosition(filePos);
                fileStream.write(writeBuffer, writeBufferOffset, fileHeader.blockSize);

                blockTable[block] = fileHeader.blocksAllocated;

                // Update the file header on disk, to indicate where the next free block is
                fileHeader.blocksAllocated++;
                fileStream.setPosition(PreHeaderRecord.Size);
                fileHeader.write(fileStream);

                // Update the block table on disk, to indicate where this block is
                writeBlockTableEntry(block);
            } else {
                // Existing block, simply overwrite the existing data
                long blockOffset = (blockTable[block] & 0xfff_ffffL) * (fileHeader.blockSize + fileHeader.blockExtraSize);
                long filePos = fileHeader.dataOffset + fileHeader.blockExtraSize + blockOffset + offsetInBlock;
                fileStream.setPosition(filePos);
                fileStream.write(buffer, offset + numWritten, toWrite);
            }

            numWritten += toWrite;
            position += toWrite;
        }
    }

    public void close() throws IOException {
        isDisposed = true;
        if (ownsStream == Ownership.Dispose && fileStream != null) {
            fileStream.close();
            fileStream = null;
        }
    }

    protected void onWriteOccurred() {
        if (writeOccurred != null) {
            writeOccurred.accept(this, null);
        }
    }

    private void readBlockTable() {
        fileStream.setPosition(fileHeader.blocksOffset);

        byte[] buffer = StreamUtilities.readExact(fileStream, fileHeader.blockCount * 4);

        blockTable = new int[fileHeader.blockCount];
        for (int i = 0; i < fileHeader.blockCount; ++i) {
            blockTable[i] = EndianUtilities.toUInt32LittleEndian(buffer, i * 4);
        }
    }

    private void writeBlockTableEntry(int block) {
        byte[] buffer = new byte[4];
        EndianUtilities.writeBytesLittleEndian(blockTable[block], buffer, 0);

        fileStream.setPosition(fileHeader.blocksOffset + block * 4L);
        fileStream.write(buffer, 0, 4);
    }

    private void checkDisposed() {
        if (isDisposed) {
            throw new dotnet4j.io.IOException("DiskStream: Attempt to use disposed stream");
        }
    }
}
