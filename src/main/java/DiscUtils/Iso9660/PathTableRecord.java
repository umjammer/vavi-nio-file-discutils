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

package DiscUtils.Iso9660;

import java.nio.charset.Charset;

import DiscUtils.Core.Internal.Utilities;


public class PathTableRecord {
    public PathTableRecord() {
    }

    /**
     * /public byte ExtendedAttributeRecordLength;
     */
    public int LocationOfExtent;

    public short ParentDirectoryNumber;

    public String DirectoryIdentifier;

//    public static int ReadFrom(byte[] src, int offset, boolean byteSwap, Charset enc, /* out */ PathTableRecord record) {
//        byte directoryIdentifierLength = src[offset + 0];
//        record.ExtendedAttributeRecordLength = src[offset + 1];
//        record.LocationOfExtent = EndianUtilities.toUInt32LittleEndian(src, offset + 2);
//        record.ParentDirectoryNumber = EndianUtilities.toUInt16LittleEndian(src, offset + 6);
//        record.DirectoryIdentifier = IsoUtilities.readChars(src, offset + 8, directoryIdentifierLength, enc);
//
//        if (byteSwap) {
//            record.LocationOfExtent = Utilities.bitSwap(record.LocationOfExtent);
//            record.ParentDirectoryNumber = Utilities.bitSwap(record.ParentDirectoryNumber);
//        }
//
//        return directoryIdentifierLength + 8 + (((directoryIdentifierLength & 1) == 1) ? 1 : 0);
//    }

    public int write(boolean byteSwap, Charset enc, byte[] buffer, int offset) {
        int nameBytes = DirectoryIdentifier.getBytes(enc).length;
        buffer[offset + 0] = (byte) nameBytes;
        buffer[offset + 1] = 0;
        // ExtendedAttributeRecordLength;
        IsoUtilities.toBytesFromUInt32(buffer, offset + 2, byteSwap ? Utilities.bitSwap(LocationOfExtent) : LocationOfExtent);
        IsoUtilities.toBytesFromUInt16(buffer,
                                       offset + 6,
                                       byteSwap ? Utilities.bitSwap(ParentDirectoryNumber) : ParentDirectoryNumber);
        IsoUtilities.writeString(buffer, offset + 8, nameBytes, false, DirectoryIdentifier, enc);
        if ((nameBytes & 1) == 1) {
            buffer[offset + 8 + nameBytes] = 0;
        }

        return 8 + nameBytes + ((nameBytes & 0x1) == 1 ? 1 : 0);
    }
}
