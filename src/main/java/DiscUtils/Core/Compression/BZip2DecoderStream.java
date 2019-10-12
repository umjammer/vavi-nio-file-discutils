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

package DiscUtils.Core.Compression;

import java.io.IOException;

import DiscUtils.Core.Internal.Crc32;
import DiscUtils.Core.Internal.Crc32Algorithm;
import DiscUtils.Core.Internal.Crc32BigEndian;
import DiscUtils.Streams.Util.Ownership;
import moe.yo3explorer.dotnetio4j.BufferedStream;
import moe.yo3explorer.dotnetio4j.SeekOrigin;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * Implementation of a BZip2 decoder.
 */
public final class BZip2DecoderStream extends Stream {
    private final BitStream _bitstream;

    private final byte[] _blockBuffer;

    private int _blockCrc;

    private final BZip2BlockDecoder _blockDecoder;

    private Crc32 _calcBlockCrc;

    private int _calcCompoundCrc;

    private int _compoundCrc;

    private Stream _compressedStream;

    private boolean _eof;

    private final Ownership _ownsCompressed;

    private long _position;

    private BZip2RleStream _rleStream;

    /**
     * Initializes a new instance of the BZip2DecoderStream class.
     *
     * @param stream The compressed input stream.
     * @param ownsStream Whether ownership of stream passes to the new instance.
     */
    public BZip2DecoderStream(Stream stream, Ownership ownsStream) {
        _compressedStream = stream;
        _ownsCompressed = ownsStream;
        _bitstream = new BigEndianBitStream(new BufferedStream(stream));
        // The Magic BZh
        byte[] magic = new byte[3];
        magic[0] = (byte) _bitstream.read(8);
        magic[1] = (byte) _bitstream.read(8);
        magic[2] = (byte) _bitstream.read(8);
        if (magic[0] != 0x42 || magic[1] != 0x5A || magic[2] != 0x68) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Bad magic at start of stream");
        }

        // The size of the decompression blocks in multiples of 100,000
        int blockSize = _bitstream.read(8) - 0x30;
        if (blockSize < 1 || blockSize > 9) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Unexpected block size in header: " + blockSize);
        }

        blockSize *= 100000;
        _rleStream = new BZip2RleStream();
        _blockDecoder = new BZip2BlockDecoder(blockSize);
        _blockBuffer = new byte[blockSize];
        if (readBlock() == 0) {
            _eof = true;
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
        return _position;
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
            throw new IllegalArgumentException("Buffer smaller than declared");
        }

        if (offset < 0) {
            throw new IllegalArgumentException("Offset less than zero");
        }

        if (count < 0) {
            throw new IllegalArgumentException("Count less than zero");
        }

        if (_eof) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to read beyond end of stream");
        }

        if (count == 0) {
            return 0;
        }

        int numRead = _rleStream.read(buffer, offset, count);
        if (numRead == 0) {
            // If there was an existing block, check it's crc.
            if (_calcBlockCrc != null) {
                if (_blockCrc != _calcBlockCrc.getValue()) {
                    throw new moe.yo3explorer.dotnetio4j.IOException("Decompression failed - block CRC mismatch");
                }

                _calcCompoundCrc = ((_calcCompoundCrc << 1) | (_calcCompoundCrc >>> 31)) ^ _blockCrc;
            }

            // Read a new block (if any), if none - check the overall CRC before returning
            if (readBlock() == 0) {
                _eof = true;
                if (_calcCompoundCrc != _compoundCrc) {
                    throw new moe.yo3explorer.dotnetio4j.IOException("Decompression failed - compound CRC");
                }

                return 0;
            }

            numRead = _rleStream.read(buffer, offset, count);
        }

        _calcBlockCrc.process(buffer, offset, numRead);
        // Pre-read next block, so a client that knows the decompressed length will still
        // have the overall CRC calculated.
        if (_rleStream.getAtEof()) {
            // If there was an existing block, check it's crc.
            if (_calcBlockCrc != null) {
                if (_blockCrc != _calcBlockCrc.getValue()) {
                    throw new moe.yo3explorer.dotnetio4j.IOException("Decompression failed - block CRC mismatch");
                }

            }

            _calcCompoundCrc = ((_calcCompoundCrc << 1) | (_calcCompoundCrc >>> 31)) ^ _blockCrc;
            if (readBlock() == 0) {
                _eof = true;
                if (_calcCompoundCrc != _compoundCrc) {
                    throw new moe.yo3explorer.dotnetio4j.IOException("Decompression failed - compound CRC mismatch");
                }

                return numRead;
            }

        }

        _position += numRead;
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
        if (_compressedStream != null && _ownsCompressed == Ownership.Dispose) {
            _compressedStream.close();
        }

        _compressedStream = null;
        if (_rleStream != null) {
            _rleStream.close();
            _rleStream = null;
        }
    }

    private int readBlock() {
        long marker = readMarker();
        if (marker == 0x314159265359l) {
            int blockSize = _blockDecoder.process(_bitstream, _blockBuffer, 0);
            _rleStream.reset(_blockBuffer, 0, blockSize);
            _blockCrc = _blockDecoder.getCrc();
            _calcBlockCrc = new Crc32BigEndian(Crc32Algorithm.Common);
            return blockSize;
        }

        if (marker == 0x177245385090l) {
            _compoundCrc = readUint();
            return 0;
        }

        throw new moe.yo3explorer.dotnetio4j.IOException("Found invalid marker in stream");
    }

    private int readUint() {
        int val = 0;
        for (int i = 0; i < 4; ++i) {
            val = (val << 8) | _bitstream.read(8);
        }
        return val;
    }

    private long readMarker() {
        long marker = 0;
        for (int i = 0; i < 6; ++i) {
            marker = (marker << 8) | _bitstream.read(8);
        }
        return marker;
    }

}
