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

package discUtils.wim;

import java.io.IOException;

import discUtils.core.compression.HuffmanTree;
import dotnet4j.io.BufferedStream;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


/**
 * Implements the XPRESS decompression algorithm.
 * This class is optimized for the case where the entire stream contents
 * fit into memory, it is not suitable for unbounded streams.
 */
public class XpressStream extends Stream {

    private final byte[] buffer;

    private final Stream compressedStream;

    private long position;

    /**
     * Initializes a new instance of the XpressStream class.
     *
     * @param compressed The stream of compressed data.
     * @param count The length of this stream (in uncompressed bytes).
     */
    public XpressStream(Stream compressed, int count) {
        compressedStream = new BufferedStream(compressed);
        buffer = buffer(count);
    }

    @Override public boolean canRead() {
        return true;
    }

    @Override public boolean canSeek() {
        return false;
    }

    @Override public boolean canWrite() {
        return false;
    }

    @Override public long getLength() {
        return buffer.length;
    }

    @Override public long position() {
        return position;
    }

    @Override public void position(long value) {
        position = value;
    }

    @Override public void flush() {
    }

    @Override public int read(byte[] buffer, int offset, int count) {
        if (position > getLength()) {
            return 0;
        }

        int numToRead = (int) Math.min(count, this.buffer.length - position);
        System.arraycopy(this.buffer, (int) position, buffer, offset, numToRead);
        position += numToRead;
        return numToRead;
    }

    @Override public long seek(long offset, SeekOrigin origin) {
        throw new UnsupportedOperationException();
    }

    @Override public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public void write(byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    private HuffmanTree readHuffmanTree() {
        int[] lengths = new int[256 + 16 * 16];
        for (int i = 0; i < lengths.length; i += 2) {
            int b = readCompressedByte();
            lengths[i] = b & 0xF;
            lengths[i + 1] = b >>> 4;
        }
        return new HuffmanTree(lengths);
    }

    private byte[] buffer(int count) {
        byte[] buffer = new byte[count];
        int numRead = 0;
        HuffmanTree tree = readHuffmanTree();
        XpressBitStream bitStream = new XpressBitStream(compressedStream);
        while (numRead < count) {
            int symbol = tree.nextSymbol(bitStream);
            if (symbol < 256) {
                // The first 256 symbols are literal byte values
                buffer[numRead] = (byte) symbol;
                numRead++;
            } else {
                // The next 256 symbols are 4 bits each for offset and length.
                int offsetBits = (symbol - 256) / 16;
                int len = (symbol - 256) % 16;
                // The actual offset
                int offset = (1 << offsetBits) - 1 + bitStream.read(offsetBits);
                // Lengths up to 15 bytes are stored directly in the symbol bits, beyond that
                // the length is stored in the compression stream.
                if (len == 15) {
                    // Note this access is directly to the underlying stream - we're not going
                    // through the bit stream.  This makes the precise behaviour of the bit stream,
                    // in terms of read-ahead critical.
                    int b = readCompressedByte();
                    if (b == 0xFF) {
                        // Again, note this access is directly to the underlying stream - we're not going
                        // through the bit stream.
                        len = readCompressedUShort();
                    } else {
                        len += b;
                    }
                }

                // Minimum length for a match is 3 bytes, so all lengths are stored as an offset
                // from 3.
                len += 3;
                for (int i = 0; i < len; ++i) {
                    // Simply do the copy
                    buffer[numRead] = buffer[numRead - offset - 1];
                    numRead++;
                }
            }
        }
        return buffer;
    }

    private int readCompressedByte() {
        int b = compressedStream.readByte();
        if (b < 0) {
            throw new IllegalStateException("Truncated stream");
        }

        return b;
    }

    private int readCompressedUShort() {
        int result = readCompressedByte();
        return result | readCompressedByte() << 8;
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
    }
}
