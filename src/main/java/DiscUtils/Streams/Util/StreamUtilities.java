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

package DiscUtils.Streams.Util;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Buffer.IBuffer;
import dotnet4j.io.Stream;


public class StreamUtilities {
    /**
     * Validates standard buffer, offset, count parameters to a method.
     *
     * @param buffer The byte array to read from / write to.
     * @param offset The starting offset in {@code buffer} .
     * @param count The number of bytes to read / write.
     */
    public static void assertBufferParameters(byte[] buffer, int offset, int count) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }

        if (offset < 0) {
            throw new IllegalArgumentException("Offset is negative");
        }

        if (count < 0) {
            throw new IllegalArgumentException("Count is negative");
        }

        if (buffer.length < offset + count) {
            throw new IllegalArgumentException("buffer is too small");
        }
    }

    /**
     * Read bytes until buffer filled or throw {@link dotnet4j.io.IOException}.
     *
     * @param stream The stream to read.
     * @param buffer The buffer to populate.
     * @param offset Offset in the buffer to start.
     * @param count The number of bytes to read.
     */
    public static void readExact(Stream stream, byte[] buffer, int offset, int count) {
        int originalCount = count;
        while (count > 0) {
            int numRead = stream.read(buffer, offset, count);
            if (numRead == 0) {
                throw new dotnet4j.io.IOException("Unable to complete read of " + originalCount + " bytes");
            }

            offset += numRead;
            count -= numRead;
        }
    }

    /**
     * Read bytes until buffer filled or throw {@link dotnet4j.io.IOException}.
     *
     * @param stream The stream to read.
     * @param count The number of bytes to read.
     * @return The data read from the stream.
     */
    public static byte[] readExact(Stream stream, int count) {
        byte[] buffer = new byte[count];
        readExact(stream, buffer, 0, count);
        return buffer;
    }

    /**
     * Read bytes until buffer filled or throw {@link dotnet4j.io.IOException}.
     *
     * @param buffer The stream to read.
     * @param pos The position in buffer to read from.
     * @param data The buffer to populate.
     * @param offset Offset in the buffer to start.
     * @param count The number of bytes to read.
     */
    public static void readExact(IBuffer buffer, long pos, byte[] data, int offset, int count) {
        int originalCount = count;
        while (count > 0) {
            int numRead = buffer.read(pos, data, offset, count);
            if (numRead == 0) {
                throw new dotnet4j.io.IOException("Unable to complete read of " + originalCount + " bytes");
            }

            pos += numRead;
            offset += numRead;
            count -= numRead;
        }
    }

    /**
     * Read bytes until buffer filled or throw EndOfStreamException.
     *
     * @param buffer The buffer to read.
     * @param pos The position in buffer to read from.
     * @param count The number of bytes to read.
     * @return The data read from the stream.
     */
    public static byte[] readExact(IBuffer buffer, long pos, int count) {
        byte[] result = new byte[count];
        readExact(buffer, pos, result, 0, count);
        return result;
    }

    /**
     * Read bytes until buffer filled or EOF.
     *
     * @param stream The stream to read.
     * @param buffer The buffer to populate.
     * @param offset Offset in the buffer to start.
     * @param count The number of bytes to read.
     * @return The number of bytes actually read.
     */
    public static int readMaximum(Stream stream, byte[] buffer, int offset, int count) {
        int totalRead = 0;
        while (count > 0) {
            int numRead = stream.read(buffer, offset, count);
            if (numRead == 0) {
                return totalRead;
            }

            offset += numRead;
            count -= numRead;
            totalRead += numRead;
        }
        return totalRead;
    }

    /**
     * Read bytes until buffer filled or EOF.
     *
     * @param buffer The stream to read.
     * @param pos The position in buffer to read from.
     * @param data The buffer to populate.
     * @param offset Offset in the buffer to start.
     * @param count The number of bytes to read.
     * @return The number of bytes actually read.
     */
    public static int readMaximum(IBuffer buffer, long pos, byte[] data, int offset, int count) {
        int totalRead = 0;
        while (count > 0) {
            int numRead = buffer.read(pos, data, offset, count);
            if (numRead == 0) {
                return totalRead;
            }

            pos += numRead;
            offset += numRead;
            count -= numRead;
            totalRead += numRead;
        }
        return totalRead;
    }

    /**
     * Read bytes until buffer filled or throw EndOfStreamException.
     *
     * @param buffer The buffer to read.
     * @return The data read from the stream.
     */
    public static byte[] readAll(IBuffer buffer) {
        return readExact(buffer, 0, (int) buffer.getCapacity());
    }

    /**
     * Reads a disk sector (512 bytes).
     *
     * @param stream The stream to read.
     * @return The sector data as a byte array.
     */
    public static byte[] readSector(Stream stream) {
        return readExact(stream, Sizes.Sector);
    }

    /**
     * Reads a structure from a stream. The type of the structure.
     *
     * @param stream The stream to read.
     * @return The structure.
     */
    public static <T extends IByteArraySerializable> T readStruct(Class<T> c, Stream stream) {
        try {
            T result = c.newInstance();
            int size = result.size();
            byte[] buffer = readExact(stream, size);
            result.readFrom(buffer, 0);
            return result;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Reads a structure from a stream. The type of the structure.
     *
     * @param stream The stream to read.
     * @param length The number of bytes to read.
     * @return The structure.
     */
    public static <T extends IByteArraySerializable> T readStruct(Class<T> c, Stream stream, int length) {
        try {
            T result = c.newInstance();
            byte[] buffer = readExact(stream, length);
            result.readFrom(buffer, 0);
            return result;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Writes a structure to a stream. The type of the structure.
     *
     * @param stream The stream to write to.
     * @param obj The structure to write.
     */
    public static <T extends IByteArraySerializable> void writeStruct(Stream stream, T obj) {
        byte[] buffer = new byte[obj.size()];
        obj.writeTo(buffer, 0);
        stream.write(buffer, 0, buffer.length);
    }

    /**
     * Copies the contents of one stream to another.
     *
     * @param source The stream to copy from.
     * @param dest The destination stream.Copying starts at the current stream
     *            positions.
     */
    public static void pumpStreams(Stream source, Stream dest) {
        byte[] buffer = new byte[8192];
        int numRead;
        while ((numRead = source.read(buffer, 0, buffer.length)) > 0) {
            dest.write(buffer, 0, numRead);
        }
    }
}
