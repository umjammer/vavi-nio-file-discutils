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

package discUtils.udf;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.StreamUtilities;


public class UdfUtilities {
    private static final int[] CrcTable = {
        0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50A5, 0x60C6, 0x70E7, 0x8108, 0x9129, 0xA14A, 0xB16B, 0xC18C, 0xD1AD, 0xE1CE,
        0xF1EF, 0x1231, 0x0210, 0x3273, 0x2252, 0x52B5, 0x4294, 0x72F7, 0x62D6, 0x9339, 0x8318, 0xB37B, 0xA35A, 0xD3BD, 0xC39C,
        0xF3FF, 0xE3DE, 0x2462, 0x3443, 0x0420, 0x1401, 0x64E6, 0x74C7, 0x44A4, 0x5485, 0xA56A, 0xB54B, 0x8528, 0x9509, 0xE5EE,
        0xF5CF, 0xC5AC, 0xD58D, 0x3653, 0x2672, 0x1611, 0x0630, 0x76D7, 0x66F6, 0x5695, 0x46B4, 0xB75B, 0xA77A, 0x9719, 0x8738,
        0xF7DF, 0xE7FE, 0xD79D, 0xC7BC, 0x48C4, 0x58E5, 0x6886, 0x78A7, 0x0840, 0x1861, 0x2802, 0x3823, 0xC9CC, 0xD9ED, 0xE98E,
        0xF9AF, 0x8948, 0x9969, 0xA90A, 0xB92B, 0x5AF5, 0x4AD4, 0x7AB7, 0x6A96, 0x1A71, 0x0A50, 0x3A33, 0x2A12, 0xDBFD, 0xCBDC,
        0xFBBF, 0xEB9E, 0x9B79, 0x8B58, 0xBB3B, 0xAB1A, 0x6CA6, 0x7C87, 0x4CE4, 0x5CC5, 0x2C22, 0x3C03, 0x0C60, 0x1C41, 0xEDAE,
        0xFD8F, 0xCDEC, 0xDDCD, 0xAD2A, 0xBD0B, 0x8D68, 0x9D49, 0x7E97, 0x6EB6, 0x5ED5, 0x4EF4, 0x3E13, 0x2E32, 0x1E51, 0x0E70,
        0xFF9F, 0xEFBE, 0xDFDD, 0xCFFC, 0xBF1B, 0xAF3A, 0x9F59, 0x8F78, 0x9188, 0x81A9, 0xB1CA, 0xA1EB, 0xD10C, 0xC12D, 0xF14E,
        0xE16F, 0x1080, 0x00A1, 0x30C2, 0x20E3, 0x5004, 0x4025, 0x7046, 0x6067, 0x83B9, 0x9398, 0xA3FB, 0xB3DA, 0xC33D, 0xD31C,
        0xE37F, 0xF35E, 0x02B1, 0x1290, 0x22F3, 0x32D2, 0x4235, 0x5214, 0x6277, 0x7256, 0xB5EA, 0xA5CB, 0x95A8, 0x8589, 0xF56E,
        0xE54F, 0xD52C, 0xC50D, 0x34E2, 0x24C3, 0x14A0, 0x0481, 0x7466, 0x6447, 0x5424, 0x4405, 0xA7DB, 0xB7FA, 0x8799, 0x97B8,
        0xE75F, 0xF77E, 0xC71D, 0xD73C, 0x26D3, 0x36F2, 0x0691, 0x16B0, 0x6657, 0x7676, 0x4615, 0x5634, 0xD94C, 0xC96D, 0xF90E,
        0xE92F, 0x99C8, 0x89E9, 0xB98A, 0xA9AB, 0x5844, 0x4865, 0x7806, 0x6827, 0x18C0, 0x08E1, 0x3882, 0x28A3, 0xCB7D, 0xDB5C,
        0xEB3F, 0xFB1E, 0x8BF9, 0x9BD8, 0xABBB, 0xBB9A, 0x4A75, 0x5A54, 0x6A37, 0x7A16, 0x0AF1, 0x1AD0, 0x2AB3, 0x3A92, 0xFD2E,
        0xED0F, 0xDD6C, 0xCD4D, 0xBDAA, 0xAD8B, 0x9DE8, 0x8DC9, 0x7C26, 0x6C07, 0x5C64, 0x4C45, 0x3CA2, 0x2C83, 0x1CE0, 0x0CC1,
        0xEF1F, 0xFF3E, 0xCF5D, 0xDF7C, 0xAF9B, 0xBFBA, 0x8FD9, 0x9FF8, 0x6E17, 0x7E36, 0x4E55, 0x5E74, 0x2E93, 0x3EB2, 0x0ED1,
        0x1EF0
    };

    public static short computeCrc(byte[] buffer, int offset, int length) {
        short result = 0;
        for (int i = 0; i < length; ++i) {
            result = (short) (CrcTable[(result >> 8 ^ buffer[offset + i]) & 0xFF] ^ (result << 8));
        }
        return result;
    }

    public static long parseTimestamp(byte[] buffer, int offset) {
        boolean allZero = true;
        for (int i = 0; i < 12; ++i) {
            if (buffer[i] != 0) {
                allZero = false;
                break;
            }

        }
        if (allZero) {
            return Long.MIN_VALUE;
        }

        short typeAndZone = EndianUtilities.toUInt16LittleEndian(buffer, offset);
        int type = (typeAndZone >>> 12) & 0x0F;
        int minutesWest = typeAndZone & 0xFFF;
        if ((minutesWest & 0x800) != 0) {
            minutesWest = (-1 & ~0xFFF) | minutesWest;
        }

        int year = forceRange((short) 1, (short) 9999, EndianUtilities.toInt16LittleEndian(buffer, offset + 2));
        int month = forceRange((short) 1, (short) 12, buffer[offset + 4]);
        int day = forceRange((short) 1, (short) 31, buffer[offset + 5]);
        int hour = forceRange((short) 0, (short) 23, buffer[offset + 6]);
        int min = forceRange((short) 0, (short) 59, buffer[offset + 7]);
        int sec = forceRange((short) 0, (short) 59, buffer[offset + 8]);
        int csec = forceRange((short) 0, (short) 99, buffer[offset + 9]);
        int hmsec = forceRange((short) 0, (short) 99, buffer[offset + 10]);
        int msec = forceRange((short) 0, (short) 99, buffer[offset + 11]);
        try {
            ZonedDateTime baseTime = ZonedDateTime.of(year, month, day, hour, min, sec, 10 * csec + hmsec / 10, ZoneId.of("UTC"));
            return baseTime.minus(Duration.ofMinutes(minutesWest)).toInstant().toEpochMilli();
        } catch (IndexOutOfBoundsException e) {
            return Long.MIN_VALUE;
        }
    }

    public static String readDString(byte[] buffer, int offset, int count) {
        int byteLen = buffer[offset + count - 1];
        return readDCharacters(buffer, offset, byteLen);
    }

    public static String readDCharacters(byte[] buffer, int offset, int count) {
        if (count == 0) {
            return "";
        }

        byte alg = buffer[offset];
        if (alg != 8 && alg != 16) {
            throw new IllegalArgumentException("Corrupt compressed unicode string");
        }

        StringBuilder result = new StringBuilder(count);
        int pos = 1;
        while (pos < count) {
            char ch = '\0';
            if (alg == 16) {
                ch = (char) (buffer[offset + pos] << 8);
                pos++;
            }

            if (pos < count) {
                ch |= (char) buffer[offset + pos];
                pos++;
            }

            result.append(ch);
        }
        return result.toString();
    }

    public static byte[] readExtent(UdfContext context, LongAllocationDescriptor extent) {
        LogicalPartition partition = context.logicalPartitions.get(extent.extentLocation.getPartition());
        long pos = extent.extentLocation.logicalBlock * partition.getLogicalBlockSize();
        return StreamUtilities.readExact(partition.getContent(), pos, extent.extentLength);
    }

    private static short forceRange(short min, short max, short val) {
        if (val < min) {
            return min;
        }

        if (val > max) {
            return max;
        }

        return val;
    }

    private static byte forceRange(byte min, byte max, byte val) {
        if (val < min) {
            return min;
        }

        if (val > max) {
            return max;
        }

        return val;
    }
}
