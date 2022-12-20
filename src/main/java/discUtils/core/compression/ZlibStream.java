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

package discUtils.core.compression;

import java.io.IOException;

import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;
import dotnet4j.io.compression.CompressionMode;
import dotnet4j.io.compression.DeflateStream;
import vavi.util.ByteUtil;


/**
 * Implementation of the Zlib compression algorithm. Only decompression is
 * currently implemented.
 */
public class ZlibStream extends Stream {

    private final Adler32 adler32;

    private final DeflateStream deflateStream;

    private final CompressionMode mode;

    private final Stream stream;

    /**
     * Initializes a new instance of the ZlibStream class.
     *
     * @param stream The stream to compress of decompress.
     * @param mode Whether to compress or decompress.
     * @param leaveOpen Whether closing this stream should leave {@code stream}
     *            open.
     */
    public ZlibStream(Stream stream, CompressionMode mode, boolean leaveOpen) {
        this.stream = stream;
        this.mode = mode;
        //
        // *** WARNING ***
        // {@link DeflateStream} needs zip header (0x78, 0x9c)
        // so commented out below
        //
//        if (mode == CompressionMode.Decompress) {
//            // We just sanity check against expected header values...
//            byte[] headerBuffer = StreamUtilities.readExact(stream, 2);
//            short header = ByteUtil.readBeShort(headerBuffer, 0);
//            if (header % 31 != 0) {
//                throw new dotnet4j.io.IOException("Invalid Zlib header found");
//            }
//
//            if ((header & 0x0F00) != 8 << 8) {
//                throw new UnsupportedOperationException("Zlib compression not using DEFLATE algorithm");
//            }
//
//            if ((header & 0x0020) != 0) {
//                throw new UnsupportedOperationException("Zlib compression using preset dictionary");
//            }
//        } else {
//            short header = (8 << 8) | // DEFLATE
//                           (7 << 12) | // 32K window size
//                           0x80; // Default algorithm
//            header |= (short) (31 - header % 31);
//
//            byte[] headerBuffer = new byte[2];
//            ByteUtil.writeBeShort(header, headerBuffer, 0);
//            stream.write(headerBuffer, 0, 2);
//        }

        deflateStream = new DeflateStream(stream, mode, leaveOpen);
        adler32 = new Adler32();
    }

    /**
     * Gets whether the stream can be read.
     */
    @Override public boolean canRead() {
        return deflateStream.canRead();
    }

    /**
     * Gets whether the stream pointer can be changed.
     */
    @Override public boolean canSeek() {
        return false;
    }

    /**
     * Gets whether the stream can be written to.
     */
    @Override public boolean canWrite() {
        return deflateStream.canWrite();
    }

    /**
     * Gets the length of the stream.
     */
    @Override public long getLength() {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets and sets the stream position.
     */
    @Override public long position() {
        throw new UnsupportedOperationException();
    }

    @Override public void position(long value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Closes the stream.
     */
    @Override public void close() throws IOException {
        if (mode == CompressionMode.Decompress) {
            // Can only check Adler checksum on seekable streams. Since
            // DeflateStream aggresively caches input, it normally has already
            // consumed the footer.
            if (stream.canSeek()) {
                stream.seek(-4, SeekOrigin.End);
                byte[] footerBuffer = StreamUtilities.readExact(stream, 4);
                if (ByteUtil.readBeInt(footerBuffer, 0) != adler32.getValue()) {
//Debug.printf("R: %08x, %08x\n", ByteUtil.readBeInt(footerBuffer, 0), adler32.getValue());
                    throw new dotnet4j.io.IOException("Corrupt decompressed data detected");
                }
            }

            deflateStream.close();
        } else {
            deflateStream.close();

            byte[] footerBuffer = new byte[4];
            ByteUtil.writeBeInt(adler32.getValue(), footerBuffer, 0);
//Debug.printf("W: %08x, %s\n", adler32.getValue(), stream);
            stream.write(footerBuffer, 0, 4);
        }
    }

    /**
     * Flushes the stream.
     */
    @Override public void flush() {
        deflateStream.flush();
    }

    /**
     * Reads data from the stream.
     *
     * @param buffer The buffer to populate.
     * @param offset The first byte to write.
     * @param count The number of bytes requested.
     * @return The number of bytes read.
     */
    @Override public int read(byte[] buffer, int offset, int count) {
        checkParams(buffer, offset, count);

        int numRead = deflateStream.read(buffer, offset, count);
//Debug.printf("r: %d\n", numRead);
        adler32.process(buffer, offset, numRead);
        return numRead;
    }

    /**
     * Seeks to a new position.
     *
     * @param offset Relative position to seek to.
     * @param origin The origin of the seek.
     * @return The new position.
     */
    @Override public long seek(long offset, SeekOrigin origin) {
        throw new UnsupportedOperationException();
    }

    /**
     * Changes the length of the stream.
     *
     * @param value The new desired length of the stream.
     */
    @Override public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes data to the stream.
     *
     * @param buffer buffer containing the data to write.
     * @param offset Offset of the first byte to write.
     * @param count Number of bytes to write.
     */
    @Override public void write(byte[] buffer, int offset, int count) {
        checkParams(buffer, offset, count);

        adler32.process(buffer, offset, count);
//Debug.println("w: " + count + ", " + deflateStream);
        deflateStream.write(buffer, offset, count);
        deflateStream.flush();
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
