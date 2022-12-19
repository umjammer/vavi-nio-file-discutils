//
// Copyright (c) 2016, Bianco Veigel
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

package discUtils.lvm;

import java.nio.charset.StandardCharsets;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public class PhysicalVolumeLabel implements IByteArraySerializable {

    public static final String LABEL_ID = "LABELONE";

    public static final String LVM2_LABEL = "LVM2 001";

    public String label;

    public long sector;

    public long crc;

    public long calculatedCrc;

    public long offset;

    public String label2;

    /* */
    public int size() {
        return PhysicalVolume.SECTOR_SIZE;
    }

    /* */
    public int readFrom(byte[] buffer, int offset) {
        label = new String(buffer, offset, 0x8, StandardCharsets.US_ASCII);
        sector = ByteUtil.readLeLong(buffer, offset + 0x8);
        crc = ByteUtil.readLeInt(buffer, offset + 0x10);
        calculatedCrc = PhysicalVolume.calcCrc(buffer, offset + 0x14, PhysicalVolume.SECTOR_SIZE - 0x14);
        this.offset = ByteUtil.readLeInt(buffer, offset + 0x14);
        label2 = new String(buffer, offset + 0x18, 0x8, StandardCharsets.US_ASCII);
        return size();
    }

    /* */
    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
