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

package DiscUtils.Core.Compression;

import java.io.IOException;

import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;
import dotnet4j.io.compression.CompressionMode;
import dotnet4j.io.compression.DeflateStream;


/**
 * Implementation of the Zlib compression algorithm.
 * Only decompression is currently implemented.
 */
public class ZlibStream extends Stream {
    private final Adler32 _adler32;

    private final DeflateStream _deflateStream;

    private final CompressionMode _mode;

    private final Stream _stream;

    /**
     * Initializes a new instance of the ZlibStream class.
     *
     * @param stream The stream to compress of decompress.
     * @param mode Whether to compress or decompress.
     * @param leaveOpen Whether closing this stream should leave
     *            {@code stream}
     *            open.
     */
    public ZlibStream(Stream stream, CompressionMode mode, boolean leaveOpen) {
        _stream = stream;
        _mode = mode;
        if (mode == CompressionMode.Decompress) {
            // We just sanity check against expected header values...
            byte[] headerBuffer = StreamUtilities.readExact(stream, 2);
            short header = EndianUtilities.toUInt16BigEndian(headerBuffer, 0);
            if (header % 31 != 0) {
                throw new dotnet4j.io.IOException("Invalid Zlib header found");
            }

            if ((header & 0x0F00) != 8 << 8) {
                throw new UnsupportedOperationException("Zlib compression not using DEFLATE algorithm");
            }

            if ((header & 0x0020) != 0) {
                throw new UnsupportedOperationException("Zlib compression using preset dictionary");
            }

        } else {
            short header = (8 << 8) | (7 << 12) | 0x80;
            // DEFLATE
            // 32K window size
            // Default algorithm
            header |= (short) (31 - header % 31);
            byte[] headerBuffer = new byte[2];
            EndianUtilities.writeBytesBigEndian(header, headerBuffer, 0);
            stream.write(headerBuffer, 0, 2);
        }
        _deflateStream = new DeflateStream(stream, mode, leaveOpen);
        _adler32 = new Adler32();
    }

    /**
     * Gets whether the stream can be read.
     */
    public boolean canRead() {
        return _deflateStream.canRead();
    }

    /**
     * Gets whether the stream pointer can be changed.
     */
    public boolean canSeek() {
        return false;
    }

    /**
     * Gets whether the stream can be written to.
     */
    public boolean canWrite() {
        return _deflateStream.canWrite();
    }

    /**
     * Gets the length of the stream.
     */
    public long getLength() {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets and sets the stream position.
     */
    public long getPosition() {
        throw new UnsupportedOperationException();
    }

    public void setPosition(long value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Closes the stream.
     */
    public void close() throws IOException {
        if (_mode == CompressionMode.Decompress) {
            // Can only check Adler checksum on seekable streams.  Since DeflateStream
            // aggresively caches input, it normally has already consumed the footer.
            if (_stream.canSeek()) {
                _stream.seek(-4, SeekOrigin.End);
                byte[] footerBuffer = StreamUtilities.readExact(_stream, 4);
                if (EndianUtilities.toInt32BigEndian(footerBuffer, 0) != _adler32.getValue()) {
//Debug.printf("%x, %x\n", EndianUtilities.toInt32BigEndian(footerBuffer, 0), _adler32.getValue());
                    throw new IOException("Corrupt decompressed data detected");
                }
            }

            _deflateStream.close();
        } else {
            _deflateStream.close();
            byte[] footerBuffer = new byte[4];
            EndianUtilities.writeBytesBigEndian(_adler32.getValue(), footerBuffer, 0);
            _stream.write(footerBuffer, 0, 4);
        }
    }

    /**
     * Flushes the stream.
     */
    public void flush() {
        _deflateStream.flush();
    }

    /**
     * Reads data from the stream.
     *
     * @param buffer The buffer to populate.
     * @param offset The first byte to write.
     * @param count The number of bytes requested.
     * @return The number of bytes read.
     */
    public int read(byte[] buffer, int offset, int count) {
        checkParams(buffer, offset, count);
        int numRead = _deflateStream.read(buffer, offset, count);
        _adler32.process(buffer, offset, numRead);
        return numRead;
    }

    /**
     * Seeks to a new position.
     *
     * @param offset Relative position to seek to.
     * @param origin The origin of the seek.
     * @return The new position.
     */
    public long seek(long offset, SeekOrigin origin) {
        throw new UnsupportedOperationException();
    }

    /**
     * Changes the length of the stream.
     *
     * @param value The new desired length of the stream.
     */
    public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes data to the stream.
     *
     * @param buffer Buffer containing the data to write.
     * @param offset Offset of the first byte to write.
     * @param count Number of bytes to write.
     */
    public void write(byte[] buffer, int offset, int count) {
        checkParams(buffer, offset, count);
        _adler32.process(buffer, offset, count);
        _deflateStream.write(buffer, offset, count);
    }

    private static void checkParams(byte[] buffer, int offset, int count) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }

        if (offset < 0 || offset > buffer.length) {
            throw new IllegalArgumentException("Offset outside of array bounds");
        }

        if (count < 0 || offset + count > buffer.length) {
            throw new IllegalArgumentException("Array index out of bounds");
        }
    }
}
