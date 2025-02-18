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

package discUtils.streams.util;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.UUID;

import discUtils.streams.IByteArraySerializable;


public class EndianUtilities {

    @Deprecated
    public static void writeBytesLittleEndian(short val, byte[] buffer, int offset) {
        buffer[offset] = (byte) (val & 0xFF);
        buffer[offset + 1] = (byte) ((val >>> 8) & 0xFF);
    }

    @Deprecated
    public static void writeBytesLittleEndian(int val, byte[] buffer, int offset) {
        buffer[offset] = (byte) (val & 0xFF);
        buffer[offset + 1] = (byte) ((val >>> 8) & 0xFF);
        buffer[offset + 2] = (byte) ((val >>> 16) & 0xFF);
        buffer[offset + 3] = (byte) ((val >>> 24) & 0xFF);
    }

    @Deprecated
    public static void writeBytesLittleEndian(long val, byte[] buffer, int offset) {
        buffer[offset] = (byte) (val & 0xFF);
        buffer[offset + 1] = (byte) ((val >>> 8) & 0xFF);
        buffer[offset + 2] = (byte) ((val >>> 16) & 0xFF);
        buffer[offset + 3] = (byte) ((val >>> 24) & 0xFF);
        buffer[offset + 4] = (byte) ((val >>> 32) & 0xFF);
        buffer[offset + 5] = (byte) ((val >>> 40) & 0xFF);
        buffer[offset + 6] = (byte) ((val >>> 48) & 0xFF);
        buffer[offset + 7] = (byte) ((val >>> 56) & 0xFF);
    }

    @Deprecated
    public static void writeBytesLittleEndian(UUID val, byte[] buffer, int offset) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]).order(ByteOrder.BIG_ENDIAN);
        bb.putLong(val.getMostSignificantBits());
        bb.putLong(val.getLeastSignificantBits());
        byte[] le = bb.array();
        writeBytesLittleEndian(toInt32BigEndian(le, 0), buffer, offset + 0);
        writeBytesLittleEndian(toInt16BigEndian(le, 4), buffer, offset + 4);
        writeBytesLittleEndian(toInt16BigEndian(le, 6), buffer, offset + 6);
        System.arraycopy(le, 8, buffer, offset + 8, 8);
    }

    @Deprecated
    public static void writeBytesBigEndian(short val, byte[] buffer, int offset) {
        buffer[offset] = (byte) (val >>> 8);
        buffer[offset + 1] = (byte) (val & 0xFF);
    }

    @Deprecated
    public static void writeBytesBigEndian(int val, byte[] buffer, int offset) {
        buffer[offset] = (byte) ((val >>> 24) & 0xFF);
        buffer[offset + 1] = (byte) ((val >>> 16) & 0xFF);
        buffer[offset + 2] = (byte) ((val >>> 8) & 0xFF);
        buffer[offset + 3] = (byte) (val & 0xFF);
    }

    @Deprecated
    public static void writeBytesBigEndian(long val, byte[] buffer, int offset) {
        buffer[offset] = (byte) ((val >>> 56) & 0xFF);
        buffer[offset + 1] = (byte) ((val >>> 48) & 0xFF);
        buffer[offset + 2] = (byte) ((val >>> 40) & 0xFF);
        buffer[offset + 3] = (byte) ((val >>> 32) & 0xFF);
        buffer[offset + 4] = (byte) ((val >>> 24) & 0xFF);
        buffer[offset + 5] = (byte) ((val >>> 16) & 0xFF);
        buffer[offset + 6] = (byte) ((val >>> 8) & 0xFF);
        buffer[offset + 7] = (byte) (val & 0xFF);
    }

    @Deprecated
    public static void writeBytesBigEndian(UUID val, byte[] buffer, int offset) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]).order(ByteOrder.BIG_ENDIAN);
        bb.putLong(val.getMostSignificantBits());
        bb.putLong(val.getLeastSignificantBits());
        byte[] be = bb.array();
        System.arraycopy(be, 0, buffer, offset, 16);
    }

    @Deprecated
    public static short toUInt16LittleEndian(byte[] buffer, int offset) {
        return (short) (((buffer[offset + 1] << 8) & 0xFF00) | ((buffer[offset + 0] << 0) & 0x00FF));
    }

    @Deprecated
    public static int toUInt32LittleEndian(byte[] buffer, int offset) {
        return ((buffer[offset + 3] << 24) & 0xFF000000) | ((buffer[offset + 2] << 16) & 0x00FF0000) |
               ((buffer[offset + 1] << 8) & 0x0000FF00) | ((buffer[offset + 0] << 0) & 0x000000FF);
    }

    @Deprecated
    public static long toUInt64LittleEndian(byte[] buffer, int offset) {
        return ((toUInt32LittleEndian(buffer, offset + 4) & 0xffffffffL) << 32) |
               (toUInt32LittleEndian(buffer, offset + 0) & 0xffffffffL);
    }

    @Deprecated
    public static short toInt16LittleEndian(byte[] buffer, int offset) {
        return toUInt16LittleEndian(buffer, offset);
    }

    @Deprecated
    public static int toInt32LittleEndian(byte[] buffer, int offset) {
        return toUInt32LittleEndian(buffer, offset);
    }

    @Deprecated
    public static long toInt64LittleEndian(byte[] buffer, int offset) {
        return toUInt64LittleEndian(buffer, offset);
    }

    @Deprecated
    public static short toUInt16BigEndian(byte[] buffer, int offset) {
        short val = (short) (((buffer[offset] << 8) & 0xFF00) | ((buffer[offset + 1] << 0) & 0x00FF));
        return val;
    }

    @Deprecated
    public static int toUInt32BigEndian(byte[] buffer, int offset) {
        int val = ((buffer[offset + 0] << 24) & 0xFF000000) | ((buffer[offset + 1] << 16) & 0x00FF0000) |
                  ((buffer[offset + 2] << 8) & 0x0000FF00) | ((buffer[offset + 3] << 0) & 0x000000FF);
        return val;
    }

    @Deprecated
    public static long toUInt64BigEndian(byte[] buffer, int offset) {
        return ((toUInt32BigEndian(buffer, offset + 0) & 0xffffffffL) << 32) |
               (toUInt32BigEndian(buffer, offset + 4) & 0xffffffffL);
    }

    @Deprecated
    public static short toInt16BigEndian(byte[] buffer, int offset) {
        return toUInt16BigEndian(buffer, offset);
    }

    @Deprecated
    public static int toInt32BigEndian(byte[] buffer, int offset) {
        return toUInt32BigEndian(buffer, offset);
    }

    @Deprecated
    public static long toInt64BigEndian(byte[] buffer, int offset) {
        return toUInt64BigEndian(buffer, offset);
    }

    @Deprecated
    public static UUID toGuidLittleEndian(byte[] buffer, int offset) {
        byte[] temp = new byte[16];
        writeBytesBigEndian(toInt32LittleEndian(buffer, offset + 0), temp, 0);
        writeBytesBigEndian(toInt16LittleEndian(buffer, offset + 4), temp, 4);
        writeBytesBigEndian(toInt16LittleEndian(buffer, offset + 6), temp, 6);
        System.arraycopy(buffer, offset + 8, temp, 8, 8);
        ByteBuffer bb = ByteBuffer.wrap(temp).order(ByteOrder.BIG_ENDIAN);
        long msb = bb.getLong();
        long lsb = bb.getLong();
        return new UUID(msb, lsb);
    }

    @Deprecated
    public static UUID toGuidBigEndian(byte[] buffer, int offset) {
        byte[] temp = new byte[16];
        System.arraycopy(buffer, offset, temp, 0, 16);
        ByteBuffer bb = ByteBuffer.wrap(temp).order(ByteOrder.BIG_ENDIAN);
        long msb = bb.getLong();
        long lsb = bb.getLong();
        return new UUID(msb, lsb);
    }

    public static byte[] toByteArray(byte[] buffer, int offset, int length) {
        byte[] result = new byte[length];
        System.arraycopy(buffer, offset, result, 0, length);
        return result;
    }

    public static <T extends IByteArraySerializable> T toStruct(Class<T> c, byte[] buffer, int offset) {
        try {
            T result = c.getDeclaredConstructor().newInstance();
            result.readFrom(buffer, offset);
            return result;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Primitive conversion from Unicode to ASCII that preserves special
     * characters.
     *
     * The built-in ASCIIEncoding converts characters of codepoint > 127 to ?,
     * this preserves those code points by removing the top 16 bits of each
     * character.
     *
     * @param value The string to convert.
     * @param dest The buffer to fill.
     * @param offset The start of the string in the buffer.
     * @param count The number of characters to convert.
     */
    public static void stringToBytes(String value, byte[] dest, int offset, int count) {
        char[] chars = value.substring(0, Math.min(value.length(), count)).toCharArray();

        int i = 0;
        while (i < chars.length && i < count) {
            dest[i + offset] = (byte) chars[i];
            ++i;
        }

        while (i < count) {
            dest[i + offset] = 0;
            ++i;
        }
    }

    /**
     * Primitive conversion from ASCII to Unicode that preserves special
     * characters.
     *
     * The built-in ASCIIEncoding converts characters of codepoint > 127 to ?,
     * this preserves those code points.
     *
     * @param data The data to convert.
     * @param offset The first byte to convert.
     * @param count The number of bytes to convert.
     * @return The string.
     */
    @Deprecated
    public static String bytesToString(byte[] data, int offset, int count) {
        char[] result = new char[count];

        for (int i = 0; i < count; ++i) {
            result[i] = (char) (data[i + offset] & 0xff);
        }

        return new String(result);
    }

    /**
     * Primitive conversion from ASCII to Unicode that stops at a
     * null-terminator.
     *
     * The built-in ASCIIEncoding converts characters of codepoint > 127 to ?,
     * this preserves those code points.
     *
     * @param data The data to convert.
     * @param offset The first byte to convert.
     * @param count The number of bytes to convert.
     * @return The string.
     */
    public static String bytesToZString(byte[] data, int offset, int count) {
        char[] result = new char[count];

        for (int i = 0; i < count; ++i) {
            byte ch = data[i + offset];
            if (ch == 0) {
                return new String(result, 0, i);
            }

            result[i] = (char) (ch & 0xff);
        }

        return new String(result);
    }

    /**
     * Primitive conversion from ASCII to {@code encoding} that stops at a
     * null-terminator.
     *
     * @param data The data to convert.
     * @param offset The first byte to convert.
     * @param count The number of bytes to convert.
     * @param encoding encoding.
     * @return The string.
     */
    public static String bytesToZString(byte[] data, int offset, int count, Charset encoding) {
        byte[] result = new byte[count];

        for (int i = 0; i < count; ++i) {
            byte ch = data[i + offset];
            if (ch == 0) {
                return new String(result, 0, i, encoding);
            }

            result[i] = ch;
        }

        return new String(result, encoding);
    }
}
