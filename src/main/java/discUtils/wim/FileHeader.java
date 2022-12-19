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

package discUtils.wim;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.UUID;

import vavi.util.ByteUtil;


public class FileHeader {

    public int bootIndex;

    public ShortResourceHeader bootMetaData;

    public int compressionSize;

    public EnumSet<FileFlags> flags;

    public int headerSize;

    public int imageCount;

    public ShortResourceHeader integrityHeader;

    public ShortResourceHeader offsetTableHeader;

    public short partNumber;

    public String tag;

    public short totalParts;

    public int version;

    public UUID wimGuid;

    public ShortResourceHeader xmlDataHeader;

    public void read(byte[] buffer, int offset) {
        tag = new String(buffer, offset, 8, StandardCharsets.US_ASCII);
        headerSize = ByteUtil.readLeInt(buffer, 8);
        version = ByteUtil.readLeInt(buffer, 12);
        flags = FileFlags.valueOf(ByteUtil.readLeInt(buffer, 16));
        compressionSize = ByteUtil.readLeInt(buffer, 20);
        wimGuid = ByteUtil.readLeUUID(buffer, 24);
        partNumber = ByteUtil.readLeShort(buffer, 40);
        totalParts = ByteUtil.readLeShort(buffer, 42);
        imageCount = ByteUtil.readLeInt(buffer, 44);
        offsetTableHeader = new ShortResourceHeader();
        offsetTableHeader.read(buffer, 48);
        xmlDataHeader = new ShortResourceHeader();
        xmlDataHeader.read(buffer, 72);
        bootMetaData = new ShortResourceHeader();
        bootMetaData.read(buffer, 96);
        bootIndex = ByteUtil.readLeInt(buffer, 120);
        integrityHeader = new ShortResourceHeader();
        integrityHeader.read(buffer, 124);
    }

    public boolean isValid() {
        return tag.equals("MSWIM\0\0\0") && headerSize >= 148;
    }
}
