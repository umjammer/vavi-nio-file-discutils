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

import discUtils.core.internal.Utilities;


public class PathTableRecord {

    public PathTableRecord() {
    }

//    public byte extendedAttributeRecordLength;

    public int locationOfExtent;

    public short parentDirectoryNumber;

    public String directoryIdentifier;

//    public static int readFrom(byte[] src, int offset, boolean byteSwap, Charset enc, PathTableRecord[] record) {
//        byte directoryIdentifierLength = src[offset + 0];
//        record[0].extendedAttributeRecordLength = src[offset + 1];
//        record[0].locationOfExtent = ByteUtil.readLeInt(src, offset + 2);
//        record[0].parentDirectoryNumber = ByteUtil.readLeShort(src, offset + 6);
//        record[0].directoryIdentifier = IsoUtilities.readChars(src, offset + 8, directoryIdentifierLength, enc);
//
//        if (byteSwap) {
//            record[0].locationOfExtent = utilities.bitSwap(record.locationOfExtent);
//            record[0].parentDirectoryNumber = utilities.bitSwap(record.parentDirectoryNumber);
//        }
//
//        return directoryIdentifierLength + 8 + (((directoryIdentifierLength & 1) == 1) ? 1 : 0);
//    }

    public int write(boolean byteSwap, Charset enc, byte[] buffer, int offset) {
        int nameBytes = directoryIdentifier.getBytes(enc).length;
        buffer[offset + 0] = (byte) nameBytes;
        buffer[offset + 1] = 0; // extendedAttributeRecordLength;
        IsoUtilities.toBytesFromUInt32(buffer, offset + 2, byteSwap ? Utilities.bitSwap(locationOfExtent) : locationOfExtent);
        IsoUtilities.toBytesFromUInt16(buffer,
                                       offset + 6,
                                       byteSwap ? Utilities.bitSwap(parentDirectoryNumber) : parentDirectoryNumber);
        IsoUtilities.writeString(buffer, offset + 8, nameBytes, false, directoryIdentifier, enc);
        if ((nameBytes & 1) == 1) {
            buffer[offset + 8 + nameBytes] = 0;
        }

        return 8 + nameBytes + ((nameBytes & 0x1) == 1 ? 1 : 0);
    }
}
