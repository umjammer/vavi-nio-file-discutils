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

package discUtils.squashFs;

import java.io.Closeable;
import java.io.IOException;

import discUtils.core.compression.ZlibStream;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import dotnet4j.io.compression.CompressionMode;
import vavi.util.ByteUtil;


final class MetablockWriter implements Closeable {

    private MemoryStream buffer;

    private final byte[] currentBlock;

    private int currentBlockNum;

    private int currentOffset;

    public MetablockWriter() {
        currentBlock = new byte[8 * 1024];
        buffer = new MemoryStream();
    }

    public MetadataRef getPosition() {
        return new MetadataRef(currentBlockNum, currentOffset);
    }

    public void close() throws IOException {
        if (buffer != null) {
            buffer.close();
            buffer = null;
        }
    }

    public void write(byte[] buffer, int offset, int count) {
        int totalStored = 0;
        while (totalStored < count) {
            int toCopy = Math.min(currentBlock.length - currentOffset, count - totalStored);
            System.arraycopy(buffer, offset + totalStored, currentBlock, currentOffset, toCopy);
            currentOffset += toCopy;
            totalStored += toCopy;
            if (currentOffset == currentBlock.length) {
                nextBlock();
                currentOffset = 0;
            }
        }
    }

    void persist(Stream output) {
        if (currentOffset > 0) {
            nextBlock();
        }

        output.write(buffer.toArray(), 0, (int) buffer.getLength());
    }

    long distanceFrom(MetadataRef startPos) {
        return (currentBlockNum - startPos.getBlock()) * VfsSquashFileSystemReader.MetadataBufferSize +
            (currentOffset - startPos.getOffset());
    }

    private void nextBlock() {
        MemoryStream compressed = new MemoryStream();
        try (ZlibStream compStream = new ZlibStream(compressed, CompressionMode.Compress, true)) {
            compStream.write(currentBlock, 0, currentOffset);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }

        byte[] writeData;
        int writeLen;
        if (compressed.getLength() < currentOffset) {
            writeData = compressed.toArray();
            writeLen = (int) (compressed.getLength() & 0xffff);
        } else {
            writeData = currentBlock;
            writeLen = currentOffset | 0x8000;
        }

        byte[] header = new byte[2];
        ByteUtil.writeLeShort((short) writeLen, header, 0);
        buffer.write(header, 0, 2);
        buffer.write(writeData, 0, writeLen & 0x7FFF);

        ++currentBlockNum;
    }
}
