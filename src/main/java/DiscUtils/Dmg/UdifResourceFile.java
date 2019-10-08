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

package DiscUtils.Dmg;

import java.util.UUID;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class UdifResourceFile implements IByteArraySerializable {
    public UdifChecksum DataForkChecksum;

    public long DataForkLength;

    public long DataForkOffset;

    public int Flags;

    public int HeaderSize;

    public int ImageVariant;

    public UdifChecksum MasterChecksum;

    public long RsrcForkLength;

    public long RsrcForkOffset;

    public long RunningDataForkOffset;

    public long SectorCount;

    public int SegmentCount;

    public UUID SegmentGuid;

    public int SegmentNumber;

    public int Signature;

    public int Version;

    public long XmlLength;

    public long XmlOffset;

    public boolean getSignatureValid() {
        return Signature == 0x6B6F6C79;
    }

    public long getSize() {
        return 512;
    }

    public int readFrom(byte[] buffer, int offset) {
        Signature = EndianUtilities.toUInt32BigEndian(buffer, offset + 0);
        Version = EndianUtilities.toUInt32BigEndian(buffer, offset + 4);
        HeaderSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 8);
        Flags = EndianUtilities.toUInt32BigEndian(buffer, offset + 12);
        RunningDataForkOffset = EndianUtilities.toUInt64BigEndian(buffer, offset + 16);
        DataForkOffset = EndianUtilities.toUInt64BigEndian(buffer, offset + 24);
        DataForkLength = EndianUtilities.toUInt64BigEndian(buffer, offset + 32);
        RsrcForkOffset = EndianUtilities.toUInt64BigEndian(buffer, offset + 40);
        RsrcForkLength = EndianUtilities.toUInt64BigEndian(buffer, offset + 48);
        SegmentNumber = EndianUtilities.toUInt32BigEndian(buffer, offset + 56);
        SegmentCount = EndianUtilities.toUInt32BigEndian(buffer, offset + 60);
        SegmentGuid = EndianUtilities.toGuidBigEndian(buffer, offset + 64);
        DataForkChecksum = EndianUtilities.<UdifChecksum> toStruct(UdifChecksum.class, buffer, offset + 80);
        XmlOffset = EndianUtilities.toUInt64BigEndian(buffer, offset + 216);
        XmlLength = EndianUtilities.toUInt64BigEndian(buffer, offset + 224);
        MasterChecksum = EndianUtilities.<UdifChecksum> toStruct(UdifChecksum.class, buffer, offset + 352);
        ImageVariant = EndianUtilities.toUInt32BigEndian(buffer, offset + 488);
        SectorCount = EndianUtilities.toInt64BigEndian(buffer, offset + 492);
        return (int) getSize();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
