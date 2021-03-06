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

package DiscUtils.Core.Archives;

import java.util.Arrays;
import java.util.EnumSet;

import DiscUtils.Core.UnixFilePermissions;
import DiscUtils.Streams.Util.EndianUtilities;


public final class TarHeader {
    public static final int Length = 512;

    public long FileLength;

    public EnumSet<UnixFilePermissions> FileMode;

    public String FileName;

    public int GroupId;

    public long ModificationTime;

    public int OwnerId;

    public void readFrom(byte[] buffer, int offset) {
        FileName = readNullTerminatedString(buffer, offset + 0, 100);
        FileMode = UnixFilePermissions.valueOf((int) octalToLong(readNullTerminatedString(buffer, offset + 100, 8)));
        OwnerId = (int) octalToLong(readNullTerminatedString(buffer, offset + 108, 8));
        GroupId = (int) octalToLong(readNullTerminatedString(buffer, offset + 116, 8));
        FileLength = octalToLong(readNullTerminatedString(buffer, offset + 124, 12));
        ModificationTime = octalToLong(readNullTerminatedString(buffer, offset + 136, 12));
    }

    public void writeTo(byte[] buffer, int offset) {
        Arrays.fill(buffer, offset, offset + Length, (byte) 0);
        EndianUtilities.stringToBytes(FileName, buffer, offset, 99);
        EndianUtilities.stringToBytes(longToOctal(UnixFilePermissions.valueOf(FileMode), 7), buffer, offset + 100, 7);
        EndianUtilities.stringToBytes(longToOctal(OwnerId, 7), buffer, offset + 108, 7);
        EndianUtilities.stringToBytes(longToOctal(GroupId, 7), buffer, offset + 116, 7);
        EndianUtilities.stringToBytes(longToOctal(FileLength, 11), buffer, offset + 124, 11);
        EndianUtilities.stringToBytes(longToOctal(ModificationTime, 11), buffer, offset + 136, 11);
        // Checksum
        EndianUtilities.stringToBytes("        ", buffer, offset + 148, 8);
        long checkSum = 0;
        for (int i = 0; i < 512; ++i) {
            checkSum += buffer[offset + i];
        }
        EndianUtilities.stringToBytes(longToOctal(checkSum, 7), buffer, offset + 148, 7);
        buffer[155] = 0;
    }

    private static String readNullTerminatedString(byte[] buffer, int offset, int length) {
        return EndianUtilities.bytesToString(buffer, offset, length).replaceFirst("\0*$", "");
    }

    private static long octalToLong(String value) {
        long result = 0;
        for (int i = 0; i < value.length(); ++i) {
            result = result * 8 + (value.charAt(i) - '0');
        }
        return result;
    }

    private static String longToOctal(long value, int length) {
        String result = "";
        while (value > 0) {
            result = (char) ('0' + value % 8) + result;
            value = value / 8;
        }
        return new String(new char[length - result.length()]).replace('\0', '0')  + result;
    }

}
