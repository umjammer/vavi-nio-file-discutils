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
//
// Based on "libbzip2", Copyright (C) 1996-2007 Julian R Seward.
//

package discUtils.core.compression;

import java.io.IOException;

import discUtils.core.internal.Crc32;
import discUtils.core.internal.Crc32Algorithm;
import discUtils.core.internal.Crc32BigEndian;
import discUtils.streams.util.Ownership;
import dotnet4j.io.BufferedStream;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


/**
 * Implementation of a BZip2 decoder.
 */
public final class BZip2DecoderStream extends Stream {

    private final BitStream bitStream;

    private final byte[] blockBuffer;

    private int blockCrc;

    private final BZip2BlockDecoder blockDecoder;

    private Crc32 calcBlockCrc;

    private int calcCompoundCrc;

    private int compoundCrc;

    private Stream compressedStream;

    private boolean eof;

    private final Ownership ownsCompressed;

    private long position;

    private BZip2RleStream rleStream;

    /**
     * Initializes a new instance of the BZip2DecoderStream class.
     *
     * @param stream The compressed input stream.
     * @param ownsStream Whether ownership of stream passes to the new instance.
     */
    public BZip2DecoderStream(Stream stream, Ownership ownsStream) {
        compressedStream = stream;
        ownsCompressed = ownsStream;

        bitStream = new BigEndianBitStream(new BufferedStream(stream));

        // The Magic BZh
        byte[] magic = new byte[3];
        magic[0] = (byte) bitStream.read(8);
        magic[1] = (byte) bitStream.read(8);
        magic[2] = (byte) bitStream.read(8);
        if (magic[0] != 0x42 || magic[1] != 0x5A || magic[2] != 0x68) {
            throw new dotnet4j.io.IOException("Bad magic at start of stream");
        }

        // The size of the decompression blocks in multiples of 100,000
        int blockSize = bitStream.read(8) - 0x30;
        if (blockSize < 1 || blockSize > 9) {
            throw new dotnet4j.io.IOException("Unexpected block size in header: " + blockSize);
        }

        blockSize *= 100000;

        rleStream = new BZip2RleStream();
        blockDecoder = new BZip2BlockDecoder(blockSize);
        blockBuffer = new byte[blockSize];

        if (readBlock() == 0) {
            eof = true;
        }
    }

    /**
     * Gets an indication of whether read access is permitted.
     */
    public boolean canRead() {
        return true;
    }

    /**
     * Gets an indication of whether seeking is permitted.
     */
    public boolean canSeek() {
        return false;
    }

    /**
     * Gets an indication of whether write access is permitted.
     */
    public boolean canWrite() {
        return false;
    }

    /**
     * Gets the length of the stream (the capacity of the underlying buffer).
     */
    public long getLength() {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets and sets the current position within the stream.
     */
    public long getPosition() {
        return position;
    }

    public void setPosition(long value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Flushes all data to the underlying storage.
     */
    public void flush() {
        throw new UnsupportedOperationException();
    }

    /**
     * Reads a number of bytes from the stream.
     *
     * @param buffer The destination buffer.
     * @param offset The start offset within the destination buffer.
     * @param count The number of bytes to read.
     * @return The number of bytes read.
     */
    public int read(byte[] buffer, int offset, int count) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer");
        }

        if (buffer.length < offset + count) {
            throw new IllegalArgumentException("buffer smaller than declared");
        }

        if (offset < 0) {
            throw new IllegalArgumentException("Offset less than zero");
        }

        if (count < 0) {
            throw new IllegalArgumentException("Count less than zero");
        }

        if (eof) {
            return 0;
        }

        if (count == 0) {
            return 0;
        }

        int numRead = rleStream.read(buffer, offset, count);
        if (numRead == 0) {
            // If there was an existing block, check it's crc.
            if (calcBlockCrc != null) {
                if (blockCrc != calcBlockCrc.getValue()) {
                    throw new dotnet4j.io.IOException("Decompression failed - block CRC mismatch");
                }

                calcCompoundCrc = ((calcCompoundCrc << 1) | (calcCompoundCrc >>> 31)) ^ blockCrc;
            }

            // Read a new block (if any), if none - check the overall CRC before returning
            if (readBlock() == 0) {
                eof = true;
                if (calcCompoundCrc != compoundCrc) {
                    throw new dotnet4j.io.IOException("Decompression failed - compound CRC");
                }

                return 0;
            }

            numRead = rleStream.read(buffer, offset, count);
        }

        calcBlockCrc.process(buffer, offset, numRead);
        // Pre-read next block, so a client that knows the decompressed length will still
        // have the overall CRC calculated.
        if (rleStream.getAtEof()) {
            // If there was an existing block, check it's crc.
            if (calcBlockCrc != null) {
                if (blockCrc != calcBlockCrc.getValue()) {
                    throw new dotnet4j.io.IOException("Decompression failed - block CRC mismatch");
                }
            }

            calcCompoundCrc = ((calcCompoundCrc << 1) | (calcCompoundCrc >>> 31)) ^ blockCrc;
            if (readBlock() == 0) {
                eof = true;
                if (calcCompoundCrc != compoundCrc) {
                    throw new dotnet4j.io.IOException("Decompression failed - compound CRC mismatch");
                }

                return numRead;
            }
        }

        position += numRead;
        return numRead;
    }

    /**
     * Changes the current stream position.
     *
     * @param offset The origin-relative stream position.
     * @param origin The origin for the stream position.
     * @return The new stream position.
     */
    public long seek(long offset, SeekOrigin origin) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the length of the stream (the underlying buffer's capacity).
     *
     * @param value The new length of the stream.
     */
    public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes a buffer to the stream.
     *
     * @param buffer The buffer to write.
     * @param offset The starting offset within buffer.
     * @param count The number of bytes to write.
     */
    public void write(byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    /**
     * Releases underlying resources.
     */
    public void close() throws IOException {
        if (compressedStream != null && ownsCompressed == Ownership.Dispose) {
            compressedStream.close();
        }

        compressedStream = null;

        if (rleStream != null) {
            rleStream.close();
            rleStream = null;
        }
    }

    private int readBlock() {
        long marker = readMarker();
        if (marker == 0x314159265359L) {
            int blockSize = blockDecoder.process(bitStream, blockBuffer, 0);
            rleStream.reset(blockBuffer, 0, blockSize);
            blockCrc = blockDecoder.getCrc();
            calcBlockCrc = new Crc32BigEndian(Crc32Algorithm.Common);
            return blockSize;
        }
        if (marker == 0x177245385090L) {
            compoundCrc = readUint();
            return 0;
        }
        throw new dotnet4j.io.IOException("Found invalid marker in stream");
    }

    private int readUint() {
        int val = 0;

        for (int i = 0; i < 4; ++i) {
            val = (val << 8) | bitStream.read(8);
        }

        return val;
    }

    private long readMarker() {
        long marker = 0;

        for (int i = 0; i < 6; ++i) {
            marker = (marker << 8) | bitStream.read(8);
        }

        return marker;
    }

}
