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

package discUtils.core.logicalDiskManager;

import java.nio.charset.StandardCharsets;

import vavi.util.ByteUtil;


public class PrivateHeader {

    public int checksum;

    // 00 00 2f 96
    public long configSizeLba;

    public long configurationSizeLba;

    // 08 00
    public long configurationStartLba;

    // 03 FF F8 00
    public long dataSizeLba;

    // 03 FF F7 C1
    public long dataStartLba;

    // 3F
    public String diskGroupId;

    // GUID string
    public String diskGroupName;

    // MAX_COMPUTER_NAME_LENGTH?
    public String diskId;

    // GUID string
    public String hostId;

    // GUID string
    public long logSizeLba;

    public long nextTocLba;

    public long numberOfConfigs;

    public long numberOfLogs;

    public String signature;

    // PRIVHEAD
    public long timestamp;

    public long tocSizeLba;

    public long unknown2;

    // Active TOC? 00 .. 00 01
    public long unknown3;

    // 00 .. 07 ff  // 1 sector less than 2MB
    public long unknown4;

    // 00 .. 07 40
    public int unknown5;

    // Sector Size?
    public int version;

    // 2.12
    public void readFrom(byte[] buffer, int offset) {
        signature = new String(buffer, offset + 0x00, 8, StandardCharsets.US_ASCII);
        checksum = ByteUtil.readBeInt(buffer, offset + 0x08);
        version = ByteUtil.readBeInt(buffer, offset + 0x0C);
        timestamp = ByteUtil.readBeLong(buffer, offset + 0x10);
        unknown2 = ByteUtil.readBeLong(buffer, offset + 0x18);
        unknown3 = ByteUtil.readBeLong(buffer, offset + 0x20);
        unknown4 = ByteUtil.readBeLong(buffer, offset + 0x28);
        diskId = new String(buffer, offset + 0x30, 0x40, StandardCharsets.US_ASCII).replaceFirst("\0*$", "");
        hostId = new String(buffer, offset + 0x70, 0x40, StandardCharsets.US_ASCII).replaceFirst("\0*$", "");
        diskGroupId = new String(buffer, offset + 0xB0, 0x40, StandardCharsets.US_ASCII).replaceFirst("\0*$", "");
        diskGroupName = new String(buffer, offset + 0xF0, 31, StandardCharsets.US_ASCII).replaceFirst("\0*$", "");
        unknown5 = ByteUtil.readBeInt(buffer, offset + 0x10F);
        dataStartLba = ByteUtil.readBeLong(buffer, offset + 0x11B);
        dataSizeLba = ByteUtil.readBeLong(buffer, offset + 0x123);
        configurationStartLba = ByteUtil.readBeLong(buffer, offset + 0x12B);
        configurationSizeLba = ByteUtil.readBeLong(buffer, offset + 0x133);
        tocSizeLba = ByteUtil.readBeLong(buffer, offset + 0x13B);
        nextTocLba = ByteUtil.readBeLong(buffer, offset + 0x143);
        // These two may be reversed
        numberOfConfigs = ByteUtil.readBeInt(buffer, offset + 0x14B);
        numberOfLogs = ByteUtil.readBeInt(buffer, offset + 0x14F);
        configSizeLba = ByteUtil.readBeLong(buffer, offset + 0x153);
        logSizeLba = ByteUtil.readBeLong(buffer, offset + 0x15B);
    }

}
