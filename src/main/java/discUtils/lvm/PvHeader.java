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
import java.util.ArrayList;
import java.util.List;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public class PvHeader implements IByteArraySerializable {

    public String uuid;

    public long deviceSize;

    public List<DiskArea> diskAreas;

    public List<DiskArea> metadataDiskAreas;

    /**
     *
     */
    public int size() {
        return PhysicalVolume.SECTOR_SIZE;
    }

    /**
     *
     */
    public int readFrom(byte[] buffer, int offset) {
        uuid = readUuid(buffer, offset);
        deviceSize = ByteUtil.readLeLong(buffer, offset + 0x20);
        List<DiskArea> areas = new ArrayList<>();
        long areaOffset = offset + 0x28;
        while (true) {
            DiskArea area = new DiskArea();
            areaOffset += area.readFrom(buffer, (int) areaOffset);
            if (area.offset == 0 && area.length == 0)
                break;

            areas.add(area);
        }
        diskAreas = areas;
        areas = new ArrayList<>();
        while (true) {
            DiskArea area = new DiskArea();
            areaOffset += area.readFrom(buffer, (int) areaOffset);
            if (area.offset == 0 && area.length == 0)
                break;

            areas.add(area);
        }
        metadataDiskAreas = areas;
        return size();
    }

    /**
     *
     */
    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    private static String readUuid(byte[] buffer, int offset) {
        String sb = new String(buffer, offset, 0x6, StandardCharsets.US_ASCII) + '-' +
                new String(buffer, offset + 0x6, 0x4, StandardCharsets.US_ASCII) + '-' +
                new String(buffer, offset + 0xA, 0x4, StandardCharsets.US_ASCII) + '-' +
                new String(buffer, offset + 0xE, 0x4, StandardCharsets.US_ASCII) + '-' +
                new String(buffer, offset + 0x12, 0x4, StandardCharsets.US_ASCII) + '-' +
                new String(buffer, offset + 0x16, 0x4, StandardCharsets.US_ASCII) + '-' +
                new String(buffer, offset + 0x1A, 0x6, StandardCharsets.US_ASCII);
        return sb;
    }
}
