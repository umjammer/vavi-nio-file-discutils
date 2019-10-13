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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import DiscUtils.Streams.IByteArraySerializable;


public class EndianUtilities {
    public static void writeBytesLittleEndian(short val, byte[] buffer, int offset) {
        buffer[offset] = (byte) (val & 0xFF);
        buffer[offset + 1] = (byte) ((val >>> 8) & 0xFF);
    }

    public static void writeBytesLittleEndian(int val, byte[] buffer, int offset) {
        buffer[offset] = (byte) (val & 0xFF);
        buffer[offset + 1] = (byte) ((val >>> 8) & 0xFF);
        buffer[offset + 2] = (byte) ((val >>> 16) & 0xFF);
        buffer[offset + 3] = (byte) ((val >>> 24) & 0xFF);
    }

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

    public static void writeBytesLittleEndian(UUID val, byte[] buffer, int offset) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]).order(ByteOrder.LITTLE_ENDIAN);
        bb.putLong(val.getMostSignificantBits());
        bb.putLong(val.getLeastSignificantBits());
        byte[] le = bb.array();
        System.arraycopy(le, 0, buffer, offset, 16);
    }

    public static void writeBytesBigEndian(short val, byte[] buffer, int offset) {
        buffer[offset] = (byte) (val >>> 8);
        buffer[offset + 1] = (byte) (val & 0xFF);
    }

    public static void writeBytesBigEndian(int val, byte[] buffer, int offset) {
        buffer[offset] = (byte) ((val >>> 24) & 0xFF);
        buffer[offset + 1] = (byte) ((val >>> 16) & 0xFF);
        buffer[offset + 2] = (byte) ((val >>> 8) & 0xFF);
        buffer[offset + 3] = (byte) (val & 0xFF);
    }

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

    public static void writeBytesBigEndian(UUID val, byte[] buffer, int offset) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]).order(ByteOrder.LITTLE_ENDIAN);
        bb.putLong(val.getMostSignificantBits());
        bb.putLong(val.getLeastSignificantBits());
        byte[] le = bb.array();
        writeBytesBigEndian(toInt32LittleEndian(le, 0), buffer, offset + 0);
        writeBytesBigEndian(toInt16LittleEndian(le, 4), buffer, offset + 4);
        writeBytesBigEndian(toInt16LittleEndian(le, 6), buffer, offset + 6);
        System.arraycopy(le, 8, buffer, offset + 8, 8);
//System.err.printf("%08x, %08x, %s\n", val.getMostSignificantBits(), val.getLeastSignificantBits(), StringUtil.getDump(buffer));
    }

    public static int toUInt16LittleEndian(byte[] buffer, int offset) {
        int val = ((buffer[offset + 1] << 8) & 0xFF00) | ((buffer[offset + 0] << 0) & 0x00FF);
//if (val < 0) new Error(String.valueOf(val)).printStackTrace();
        return val;
    }

    public static int toUInt32LittleEndian(byte[] buffer, int offset) {
        int val = ((buffer[offset + 3] << 24) & 0xFF000000) | ((buffer[offset + 2] << 16) & 0x00FF0000) |
               ((buffer[offset + 1] << 8) & 0x0000FF00) | ((buffer[offset + 0] << 0) & 0x000000FF);
//if (val < 0) new Error(String.valueOf(val)).printStackTrace();
        return val;
    }

    public static long toUInt64LittleEndian(byte[] buffer, int offset) {
        return ((long) toUInt32LittleEndian(buffer, offset + 4) << 32) | toUInt32LittleEndian(buffer, offset + 0);
    }

    public static short toInt16LittleEndian(byte[] buffer, int offset) {
        return (short) toUInt16LittleEndian(buffer, offset);
    }

    public static int toInt32LittleEndian(byte[] buffer, int offset) {
        return toUInt32LittleEndian(buffer, offset);
    }

    public static long toInt64LittleEndian(byte[] buffer, int offset) {
        return toUInt64LittleEndian(buffer, offset);
    }

    public static short toUInt16BigEndian(byte[] buffer, int offset) {
        short val = (short) (((buffer[offset] << 8) & 0xFF00) | ((buffer[offset + 1] << 0) & 0x00FF));
//if (val < 0) new Error(String.valueOf(val)).printStackTrace();
        return val;
    }

    public static int toUInt32BigEndian(byte[] buffer, int offset) {
        int val = ((buffer[offset + 0] << 24) & 0xFF000000) | ((buffer[offset + 1] << 16) & 0x00FF0000) |
                  ((buffer[offset + 2] << 8) & 0x0000FF00) | ((buffer[offset + 3] << 0) & 0x000000FF);
//if (val < 0) new Error(String.valueOf(val)).printStackTrace();
        return val;
    }

    public static long toUInt64BigEndian(byte[] buffer, int offset) {
//System.err.printf("%x\n", ((long) toUInt32BigEndian(buffer, offset + 0) << 32));
        return ((long) toUInt32BigEndian(buffer, offset + 0) << 32) | toUInt32BigEndian(buffer, offset + 4);
    }

    public static short toInt16BigEndian(byte[] buffer, int offset) {
        return toUInt16BigEndian(buffer, offset);
    }

    public static int toInt32BigEndian(byte[] buffer, int offset) {
        return toUInt32BigEndian(buffer, offset);
    }

    public static long toInt64BigEndian(byte[] buffer, int offset) {
        return toUInt64BigEndian(buffer, offset);
    }

    public static UUID toGuidLittleEndian(byte[] buffer, int offset) {
        byte[] temp = new byte[16];
        System.arraycopy(buffer, offset, temp, 0, 16);
        ByteBuffer bb = ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN);
        long msb = bb.getLong();
        long lsb = bb.getLong();
        return new UUID(msb, lsb);
    }

    public static UUID toGuidBigEndian(byte[] buffer, int offset) {
        // TODO incomplete
        byte[] temp = new byte[16];
        writeBytesLittleEndian(toInt32BigEndian(buffer, offset + 0), temp, offset + 0);
        writeBytesLittleEndian(toInt16BigEndian(buffer, offset + 4), temp, offset + 4);
        writeBytesLittleEndian(toInt16BigEndian(buffer, offset + 6), temp, offset + 6);
        System.arraycopy(buffer, 8, temp, offset + 8, 8);
        ByteBuffer bb = ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN);
        long msb = bb.getLong();
        long lsb = bb.getLong();
//System.err.printf("%08x, %08x, %s\n", msb, lsb, StringUtil.getDump(temp));
        return new UUID(msb, lsb);
    }

    public static byte[] toByteArray(byte[] buffer, int offset, int length) {
        byte[] result = new byte[length];
        System.arraycopy(buffer, offset, result, 0, length);
        return result;
    }

    public static <T extends IByteArraySerializable> T toStruct(Class<T> c, byte[] buffer, int offset) {
        try {
            T result = c.newInstance();
            result.readFrom(buffer, offset);
            return result;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Primitive conversion from Unicode to ASCII that preserves special
     * characters.
     *
     * @param value The string to convert.
     * @param dest The buffer to fill.
     * @param offset The start of the string in the buffer.
     * @param count The number of characters to convert.The built-in
     *            ASCIICharset converts characters of codepoint > 127 to ?,
     *            this preserves those code points by removing the top 16 bits
     *            of each character.
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
     * @param data The data to convert.
     * @param offset The first byte to convert.
     * @param count The number of bytes to convert.
     * @return The string.The built-in ASCIICharset converts characters of
     *         codepoint > 127 to ?,
     *         this preserves those code points.
     */
    public static String bytesToString(byte[] data, int offset, int count) {
        char[] result = new char[count];
        for (int i = 0; i < count; ++i) {
            result[i] = (char) data[i + offset];
        }
        return new String(result);
    }

    /**
     * Primitive conversion from ASCII to Unicode that stops at a
     * null-terminator.
     *
     * @param data The data to convert.
     * @param offset The first byte to convert.
     * @param count The number of bytes to convert.
     * @return The string.The built-in ASCIICharset converts characters of
     *         codepoint > 127 to ?,
     *         this preserves those code points.
     */
    public static String bytesToZString(byte[] data, int offset, int count) {
        char[] result = new char[count];
        for (int i = 0; i < count; ++i) {
            byte ch = data[i + offset];
            if (ch == 0) {
                return new String(result, 0, i);
            }

            result[i] = (char) ch;
        }
        return new String(result);
    }
}
