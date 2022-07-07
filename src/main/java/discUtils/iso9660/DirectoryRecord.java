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
import java.util.EnumSet;


public class DirectoryRecord {
    public int DataLength;

    public byte ExtendedAttributeRecordLength;

    public String FileIdentifier;

    public byte FileUnitSize;

    public EnumSet<FileFlags> Flags;

    public byte InterleaveGapSize;

    public int LocationOfExtent;

    public long RecordingDateAndTime;

    public byte[] SystemUseData;

    public short VolumeSequenceNumber;

    /**
     * @param record {@cs out}
     */
    public static int readFrom(byte[] src, int offset, Charset enc, DirectoryRecord[] record) {
        int length = src[offset + 0] & 0xff;
        record[0] = new DirectoryRecord();
        record[0].ExtendedAttributeRecordLength = src[offset + 1];
        record[0].LocationOfExtent = IsoUtilities.toUInt32FromBoth(src, offset + 2);
        record[0].DataLength = IsoUtilities.toUInt32FromBoth(src, offset + 10);
        record[0].RecordingDateAndTime = IsoUtilities.toUTCDateTimeFromDirectoryTime(src, offset + 18);
        record[0].Flags = FileFlags.valueOf(src[offset + 25]);
        record[0].FileUnitSize = src[offset + 26];
        record[0].InterleaveGapSize = src[offset + 27];
        record[0].VolumeSequenceNumber = IsoUtilities.toUInt16FromBoth(src, offset + 28);
        byte lengthOfFileIdentifier = src[offset + 32];
        record[0].FileIdentifier = IsoUtilities.readChars(src, offset + 33, lengthOfFileIdentifier & 0xff, enc);
        int padding = (lengthOfFileIdentifier & 1) == 0 ? 1 : 0;
        int startSystemArea = (lengthOfFileIdentifier & 0xff) + padding + 33;
        int lenSystemArea = length - startSystemArea;
        if (lenSystemArea > 0) {
            record[0].SystemUseData = new byte[lenSystemArea];
            System.arraycopy(src, offset + startSystemArea, record[0].SystemUseData, 0, lenSystemArea);
        }

        return length;
    }

    public static int calcLength(String name, Charset enc) {
        int nameBytes;
        if (name.length() == 1 && name.charAt(0) <= 1) {
            nameBytes = 1;
        } else {
            nameBytes = name.getBytes(enc).length;
        }
        return 33 + nameBytes + ((nameBytes & 0x1) == 0 ? 1 : 0);
    }

    public int writeTo(byte[] buffer, int offset, Charset enc) {
        int length = calcLength(FileIdentifier, enc);
        buffer[offset] = (byte) length;
        buffer[offset + 1] = ExtendedAttributeRecordLength;
        IsoUtilities.toBothFromUInt32(buffer, offset + 2, LocationOfExtent);
        IsoUtilities.toBothFromUInt32(buffer, offset + 10, DataLength);
        IsoUtilities.toDirectoryTimeFromUTC(buffer, offset + 18, RecordingDateAndTime);
        buffer[offset + 25] = (byte) FileFlags.valueOf(Flags);
        buffer[offset + 26] = FileUnitSize;
        buffer[offset + 27] = InterleaveGapSize;
        IsoUtilities.toBothFromUInt16(buffer, offset + 28, VolumeSequenceNumber);
        byte lengthOfFileIdentifier;
        if (FileIdentifier.length() == 1 && FileIdentifier.charAt(0) <= 1) {
            buffer[offset + 33] = (byte) FileIdentifier.charAt(0);
            lengthOfFileIdentifier = 1;
        } else {
            lengthOfFileIdentifier = (byte) IsoUtilities
                    .writeString(buffer, offset + 33, length - 33, false, FileIdentifier, enc);
        }
        buffer[offset + 32] = lengthOfFileIdentifier;
        return length;
    }
}
