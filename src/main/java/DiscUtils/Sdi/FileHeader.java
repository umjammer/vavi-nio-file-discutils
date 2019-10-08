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

package DiscUtils.Sdi;

import java.util.UUID;

import DiscUtils.Streams.Util.EndianUtilities;


public class FileHeader {
    public long BootCodeOffset;

    public long BootCodeSize;

    public long Checksum;

    public long DeviceId;

    public UUID DeviceModel;

    public long DeviceRole;

    /**
     * /Reserved long
     */
    public long PageAlignment;

    /**
     * /Reserved long
     */
    public UUID RuntimeGuid;

    public long RuntimeOEMRev;

    public String Tag;

    public long Type;

    public long VendorId;

    public void readFrom(byte[] buffer, int offset) {
        Tag = EndianUtilities.bytesToString(buffer, offset, 8);
        if (!Tag.equals("$SDI0001")) {
            throw new IllegalArgumentException("SDI format marker not found");
        }

        Type = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x08);
        BootCodeOffset = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x10);
        BootCodeSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x18);
        VendorId = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x20);
        DeviceId = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x28);
        DeviceModel = EndianUtilities.toGuidLittleEndian(buffer, offset + 0x30);
        DeviceRole = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x40);
        RuntimeGuid = EndianUtilities.toGuidLittleEndian(buffer, offset + 0x50);
        RuntimeOEMRev = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x60);
        PageAlignment = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x70);
        Checksum = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x1F8);
    }
}
