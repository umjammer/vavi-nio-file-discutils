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

import java.util.UUID;

import discUtils.streams.util.EndianUtilities;


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
        tag = EndianUtilities.bytesToString(buffer, offset, 8);
        if (!tag.equals("$SDI0001")) {
            throw new IllegalArgumentException("SDI format marker not found");
        }

        type = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x08);
        bootCodeOffset = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x10);
        bootCodeSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x18);
        vendorId = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x20);
        deviceId = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x28);
        deviceModel = EndianUtilities.toGuidLittleEndian(buffer, offset + 0x30);
        deviceRole = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x40);
        runtimeGuid = EndianUtilities.toGuidLittleEndian(buffer, offset + 0x50);
        runtimeOEMRev = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x60);
        pageAlignment = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x70);
        checksum = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x1F8);
    }
}
