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

package discUtils.iso9660;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.logging.Logger;

import discUtils.streams.util.EndianUtilities;
import dotnet4j.io.IOException;


public class IsoUtilities {
    private static Logger logger = Logger.getLogger(IsoUtilities.class.getName());

    public static final int SectorSize = 2048;

    public static int toUInt32FromBoth(byte[] data, int offset) {
        return EndianUtilities.toUInt32LittleEndian(data, offset);
    }

    static short toUInt16FromBoth(byte[] data, int offset) {
        return EndianUtilities.toUInt16LittleEndian(data, offset);
    }

    static void toBothFromUInt32(byte[] buffer, int offset, int value) {
        EndianUtilities.writeBytesLittleEndian(value, buffer, offset);
        EndianUtilities.writeBytesBigEndian(value, buffer, offset + 4);
    }

    static void toBothFromUInt16(byte[] buffer, int offset, short value) {
        EndianUtilities.writeBytesLittleEndian(value, buffer, offset);
        EndianUtilities.writeBytesBigEndian(value, buffer, offset + 2);
    }

    static void toBytesFromUInt32(byte[] buffer, int offset, int value) {
        EndianUtilities.writeBytesLittleEndian(value, buffer, offset);
    }

    static void toBytesFromUInt16(byte[] buffer, int offset, short value) {
        EndianUtilities.writeBytesLittleEndian(value, buffer, offset);
    }

    static void writeAChars(byte[] buffer, int offset, int numBytes, String str) {
        // Validate string
        if (!isValidAString(str)) {
            throw new IOException("Attempt to write string with invalid a-characters");
        }

        /// WriteASCII(buffer, offset, numBytes, true, str);
        writeString(buffer, offset, numBytes, true, str, StandardCharsets.US_ASCII);
    }

    static void writeDChars(byte[] buffer, int offset, int numBytes, String str) {
        // Validate string
        if (!isValidDString(str)) {
            throw new IOException("Attempt to write string with invalid d-characters");
        }

        // WriteASCII(buffer, offset, numBytes, true, str);
        writeString(buffer, offset, numBytes, true, str, StandardCharsets.US_ASCII);
    }

    static void writeA1Chars(byte[] buffer, int offset, int numBytes, String str, Charset enc) {
        // Validate string
        if (!isValidAString(str)) {
            throw new IOException("Attempt to write string with invalid a-characters");
        }

        writeString(buffer, offset, numBytes, true, str, enc);
    }

    static void writeD1Chars(byte[] buffer, int offset, int numBytes, String str, Charset enc) {
        // Validate string
        if (!isValidDString(str)) {
            throw new IOException("Attempt to write string with invalid d-characters");
        }

        writeString(buffer, offset, numBytes, true, str, enc);
    }

    public static String readChars(byte[] buffer, int offset, int numBytes, Charset enc) {
        char[] chars;

        // Special handling for 'magic' names '\00' and '\01', which indicate
        // root and parent, respectively
        if (numBytes == 1) {
            chars = new char[1];
            chars[0] = (char) (buffer[offset] & 0xff);
        } else {
            chars = new String(buffer, offset, numBytes, enc).toCharArray();
        }
        return new String(chars).replaceFirst(" *$", "");
    }

    static int writeString(byte[] buffer, int offset, int numBytes, boolean pad, String str, Charset enc) {
        return writeString(buffer, offset, numBytes, pad, str, enc, false);
    }

    static int writeString(byte[] buffer,
                                  int offset,
                                  int numBytes,
                                  boolean pad,
                                  String str,
                                  Charset enc,
                                  boolean canTruncate) {
        String paddedString = pad ? str + new String(new char[numBytes]).replace('\0', ' ') : str;

        // Assumption: never less than one byte per character

        byte[] bytes = paddedString.substring(0, str.length()).getBytes(enc);
        if (!canTruncate && numBytes < bytes.length) {
            throw new IOException("Failed to write entire string");
        }
        System.arraycopy(bytes, 0, buffer, offset, bytes.length);

        return bytes.length;
    }

    static boolean isValidAString(String str) {
        for (int i = 0; i < str.length(); ++i) {
            if (!((str.charAt(i) >= ' ' && str.charAt(i) <= '\"') || (str.charAt(i) >= '%' && str.charAt(i) <= '/') ||
                  (str.charAt(i) >= ':' && str.charAt(i) <= '?') || (str.charAt(i) >= '0' && str.charAt(i) <= '9') ||
                  (str.charAt(i) >= 'A' && str.charAt(i) <= 'Z') || (str.charAt(i) == '_'))) {
                return false;
            }
        }

        return true;
    }

    static boolean isValidDString(String str) {
        for (int i = 0; i < str.length(); ++i) {
            if (!isValidDChar(str.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    static boolean isValidDChar(char ch) {
        return (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z') || (ch == '_');
    }

    static boolean isValidFileName(String str) {
        for (int i = 0; i < str.length(); ++i) {
            if (!((str.charAt(i) >= '0' && str.charAt(i) <= '9') || (str.charAt(i) >= 'A' && str.charAt(i) <= 'Z') ||
                  (str.charAt(i) == '_') || (str.charAt(i) == '.') || (str.charAt(i) == ';'))) {
                return false;
            }
        }

        return true;
    }

    static boolean isValidDirectoryName(String str) {
        if (str.length() == 1 && (str.charAt(0) == 0 || str.charAt(0) == 1)) {
            return true;
        }
        return isValidDString(str);
    }

    static String normalizeFileName(String name) {
        String[] parts = splitFileName(name);
//Debug.println("@: " + parts[0] + '.' + parts[1] + ';' + parts[2]);
        return parts[0] + '.' + parts[1] + ';' + parts[2];
    }

    static String[] splitFileName(String name) {
        String[] parts = new String[] {
            name, "", "1"
        };

        if (name.contains(".")) {
            int endOfFilePart = name.indexOf('.');
            parts[0] = name.substring(0, endOfFilePart);
            if (name.contains(";")) {
                int verSep = name.indexOf(';', endOfFilePart + 1);
                parts[1] = name.substring(endOfFilePart + 1, verSep);
                parts[2] = name.substring(verSep + 1);
            } else {
                parts[1] = name.substring(endOfFilePart + 1);
            }
        } else {
            if (name.contains(";")) {
                int verSep = name.indexOf(';');
                parts[0] = name.substring(0, verSep);
                parts[2] = name.substring(verSep + 1);
            }
        }

        short ver;
        try {
            ver = Short.parseShort(parts[2]);
            if (ver > 32767 || ver < 1) {
                ver = 1;
            }
        } catch (NumberFormatException e) {
e.printStackTrace();
            ver = 1;
        }

        parts[2] = String.format("%d", ver);

        return parts;
    }

    /**
     * Converts a DirectoryRecord time to UTC.
     *
     * @param data buffer containing the time data.
     * @param offset Offset in buffer of the time data.
     * @return The time in UTC.
     */
    public static long toUTCDateTimeFromDirectoryTime(byte[] data, int offset) {
        try {
            ZonedDateTime relTime = ZonedDateTime.of(1900 + (data[offset] & 0xff),
                                                     (data[offset + 1] & 0xff),
                                                     (data[offset + 2] & 0xff),
                                                     (data[offset + 3] & 0xff),
                                                     (data[offset + 4] & 0xff),
                                                     (data[offset + 5] & 0xff),
                                                     0,
                                                     ZoneId.of("UTC"));
            return relTime.minus(Duration.ofMinutes(15 * (data[offset + 6] & 0xff))).toInstant().toEpochMilli();
        } catch (DateTimeException e) {
            // In case the ISO has a bad date encoded, we'll just fall back to using a
            // fixed date
            return Long.MIN_VALUE;
        }
    }

    static void toDirectoryTimeFromUTC(byte[] data, int offset, long dateTime_) {
        if (dateTime_ == Long.MIN_VALUE) {
            Arrays.fill(data, offset, offset + 7, (byte) 0);
        } else {
            ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(dateTime_), ZoneId.of("UTC"));
            if (dateTime.getYear() < 1900) {
                throw new IOException("Year is out of range");
            }

            data[offset] = (byte) (dateTime.getYear() - 1900);
            data[offset + 1] = (byte) dateTime.getMonthValue();
            data[offset + 2] = (byte) dateTime.getDayOfMonth();
            data[offset + 3] = (byte) dateTime.getHour();
            data[offset + 4] = (byte) dateTime.getMinute();
            data[offset + 5] = (byte) dateTime.getSecond();
            data[offset + 6] = 0;
        }
    }

    public static long toDateTimeFromVolumeDescriptorTime(byte[] data, int offset) {
        boolean allNull = true;
        for (int i = 0; i < 16; ++i) {
            if (data[offset + i] != (byte) '0' && data[offset + i] != 0) {
                allNull = false;
                break;
            }
        }

        if (allNull) {
            return Long.MIN_VALUE;
        }

        String strForm = new String(data, offset, 16, StandardCharsets.US_ASCII);

        // Work around bugs in burning software that may use zero bytes (rather
        // than '0' characters)
        strForm = strForm.replace('\0', '0');

        int year = safeParseInt(1, 9999, strForm.substring(0, 4));
        int month = safeParseInt(1, 12, strForm.substring(4, 4 + 2));
        int day = safeParseInt(1, 31, strForm.substring(6, 6 + 2));
        int hour = safeParseInt(0, 23, strForm.substring(8, 8 + 2));
        int min = safeParseInt(0, 59, strForm.substring(10, 10 + 2));
        int sec = safeParseInt(0, 59, strForm.substring(12, 12 + 2));
        int hundredths = safeParseInt(0, 99, strForm.substring(14, 14 + 2));

        try {
            ZonedDateTime time = ZonedDateTime.of(year, month, day, hour, min, sec, hundredths * 10, ZoneId.of("UTC"));
            return time.minus(Duration.ofMinutes(15 * (data[offset + 16] & 0xff))).toInstant().toEpochMilli();
        } catch (DateTimeException e) {
            logger.warning(e.getMessage());
            return Long.MIN_VALUE;
        }
    }

    static void toVolumeDescriptorTimeFromUTC(byte[] buffer, int offset, long dateTime) {
        if (dateTime == Long.MIN_VALUE) {
            for (int i = offset; i < offset + 16; ++i) {
                buffer[i] = (byte) '0';
            }

            buffer[offset + 16] = 0;
            return;
        }

        String strForm = new SimpleDateFormat("yyyyMMddHHmmssSS").format(dateTime);
        EndianUtilities.stringToBytes(strForm, buffer, offset, 16);
        buffer[offset + 16] = 0;
    }

    static void encodingToBytes(Charset enc, byte[] data, int offset) {
        Arrays.fill(data, offset, offset + 32, (byte) 0);
        if (enc.equals(StandardCharsets.US_ASCII)) {
            // Nothing to do
        } else if (enc.equals(StandardCharsets.UTF_16BE)) {
            data[offset + 0] = 0x25;
            data[offset + 1] = 0x2F;
            data[offset + 2] = 0x45;
        } else {
            throw new IllegalArgumentException("Unrecognized character encoding");
        }
    }

    static Charset encodingFromBytes(byte[] data, int offset) {
        Charset enc = StandardCharsets.US_ASCII;
        if (data[offset + 0] == 0x25 && data[offset + 1] == 0x2F &&
            (data[offset + 2] == 0x40 || data[offset + 2] == 0x43 || data[offset + 2] == 0x45)) {
            // I.e. this is a joliet disc!
            enc = StandardCharsets.UTF_16BE;
        }

        return enc;
    }

    static boolean isSpecialDirectory(DirectoryRecord r) {
        return r.FileIdentifier.equals("\0") || r.FileIdentifier.equals("\01");
    }

    private static int safeParseInt(int minVal, int maxVal, String str) {
        int val;
        try {
            val = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return minVal;
        }

        if (val < minVal) {
            return minVal;
        }
        return Math.min(val, maxVal);
    }
}
