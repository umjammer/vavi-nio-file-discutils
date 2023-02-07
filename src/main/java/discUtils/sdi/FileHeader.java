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

package discUtils.sdi;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import vavi.util.ByteUtil;


public class FileHeader {

    public long bootCodeOffset;

    public long bootCodeSize;

    public long checksum;

    public long deviceId;

    public UUID deviceModel;

    public long deviceRole;

    /**
     * /Reserved long
     */
    public long pageAlignment;

    /**
     * /Reserved long
     */
    public UUID runtimeGuid;

    public long runtimeOEMRev;

    public String tag;

    public long type;

    public long vendorId;

    public void readFrom(byte[] buffer, int offset) {
        tag = new String(buffer, offset, 8, StandardCharsets.US_ASCII);
        if (!tag.equals("$SDI0001")) {
            throw new IllegalArgumentException("SDI format marker not found");
        }

        type = ByteUtil.readLeLong(buffer, offset + 0x08);
        bootCodeOffset = ByteUtil.readLeLong(buffer, offset + 0x10);
        bootCodeSize = ByteUtil.readLeLong(buffer, offset + 0x18);
        vendorId = ByteUtil.readLeLong(buffer, offset + 0x20);
        deviceId = ByteUtil.readLeLong(buffer, offset + 0x28);
        deviceModel = ByteUtil.readLeUUID(buffer, offset + 0x30);
        deviceRole = ByteUtil.readLeLong(buffer, offset + 0x40);
        runtimeGuid = ByteUtil.readLeUUID(buffer, offset + 0x50);
        runtimeOEMRev = ByteUtil.readLeLong(buffer, offset + 0x60);
        pageAlignment = ByteUtil.readLeLong(buffer, offset + 0x70);
        checksum = ByteUtil.readLeLong(buffer, offset + 0x1F8);
    }
}
