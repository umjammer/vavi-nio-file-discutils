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

package discUtils.dmg;

import java.util.UUID;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


/**
 * Represents UDIF header structure. usually locates at the last of a dmg file. 
 */
public class UdifResourceFile implements IByteArraySerializable {
    public UdifChecksum dataForkChecksum;

    public long dataForkLength;

    public long dataForkOffset;

    public int flags;

    public int headerSize;

    public int imageVariant;

    public UdifChecksum masterChecksum;

    public long rsrcForkLength;

    public long rsrcForkOffset;

    public long runningDataForkOffset;

    public long sectorCount;

    public int segmentCount;

    public UUID segmentGuid;

    public int segmentNumber;

    public int signature;

    public int version;

    public long xmlLength;

    public long xmlOffset;

    /** "koly" */
    public boolean getSignatureValid() {
        return signature == 0x6B6F6C79;
    }

    public int size() {
        return 512;
    }

    public int readFrom(byte[] buffer, int offset) {
        signature = EndianUtilities.toUInt32BigEndian(buffer, offset + 0);
        version = EndianUtilities.toUInt32BigEndian(buffer, offset + 4);
        headerSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 8);
        flags = EndianUtilities.toUInt32BigEndian(buffer, offset + 12);
        runningDataForkOffset = EndianUtilities.toUInt64BigEndian(buffer, offset + 16);
        dataForkOffset = EndianUtilities.toUInt64BigEndian(buffer, offset + 24);
        dataForkLength = EndianUtilities.toUInt64BigEndian(buffer, offset + 32);
        rsrcForkOffset = EndianUtilities.toUInt64BigEndian(buffer, offset + 40);
        rsrcForkLength = EndianUtilities.toUInt64BigEndian(buffer, offset + 48);
        segmentNumber = EndianUtilities.toUInt32BigEndian(buffer, offset + 56);
        segmentCount = EndianUtilities.toUInt32BigEndian(buffer, offset + 60);
        segmentGuid = EndianUtilities.toGuidBigEndian(buffer, offset + 64);
        dataForkChecksum = EndianUtilities.toStruct(UdifChecksum.class, buffer, offset + 80);
        xmlOffset = EndianUtilities.toUInt64BigEndian(buffer, offset + 216);
        xmlLength = EndianUtilities.toUInt64BigEndian(buffer, offset + 224);
        masterChecksum = EndianUtilities.toStruct(UdifChecksum.class, buffer, offset + 352);
        imageVariant = EndianUtilities.toUInt32BigEndian(buffer, offset + 488);
        sectorCount = EndianUtilities.toInt64BigEndian(buffer, offset + 492);
        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
