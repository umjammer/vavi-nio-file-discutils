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

package DiscUtils.Wim;

import java.util.UUID;

import DiscUtils.Streams.Util.EndianUtilities;


public class FileHeader {
    public int BootIndex;

    public ShortResourceHeader BootMetaData;

    public int CompressionSize;

    public FileFlags Flags = FileFlags.Compression;

    public int HeaderSize;

    public int ImageCount;

    public ShortResourceHeader IntegrityHeader;

    public ShortResourceHeader OffsetTableHeader;

    public short PartNumber;

    public String Tag;

    public short TotalParts;

    public int Version;

    public UUID WimGuid;

    public ShortResourceHeader XmlDataHeader;

    public void read(byte[] buffer, int offset) {
        Tag = EndianUtilities.bytesToString(buffer, offset, 8);
        HeaderSize = EndianUtilities.toUInt32LittleEndian(buffer, 8);
        Version = EndianUtilities.toUInt32LittleEndian(buffer, 12);
        Flags = FileFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, 16));
        CompressionSize = EndianUtilities.toInt32LittleEndian(buffer, 20);
        WimGuid = EndianUtilities.toGuidLittleEndian(buffer, 24);
        PartNumber = EndianUtilities.toUInt16LittleEndian(buffer, 40);
        TotalParts = EndianUtilities.toUInt16LittleEndian(buffer, 42);
        ImageCount = EndianUtilities.toUInt32LittleEndian(buffer, 44);
        OffsetTableHeader = new ShortResourceHeader();
        OffsetTableHeader.read(buffer, 48);
        XmlDataHeader = new ShortResourceHeader();
        XmlDataHeader.read(buffer, 72);
        BootMetaData = new ShortResourceHeader();
        BootMetaData.read(buffer, 96);
        BootIndex = EndianUtilities.toUInt32LittleEndian(buffer, 120);
        IntegrityHeader = new ShortResourceHeader();
        IntegrityHeader.read(buffer, 124);
    }

    public boolean isValid() {
        return Tag.equals("MSWIM\0\0\0") && HeaderSize >= 148;
    }
}
