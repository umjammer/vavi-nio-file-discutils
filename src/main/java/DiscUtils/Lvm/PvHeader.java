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

package DiscUtils.Lvm;

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class PvHeader implements IByteArraySerializable {
    public String Uuid;

    public long DeviceSize;

    public List<DiskArea> DiskAreas;

    public List<DiskArea> MetadataDiskAreas;

    /**
     *
     */
    public long getSize() {
        return PhysicalVolume.SECTOR_SIZE;
    }

    /**
     *
     */
    public int readFrom(byte[] buffer, int offset) {
        Uuid = readUuid(buffer, offset);
        DeviceSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x20);
        List<DiskArea> areas = new ArrayList<>();
        long areaOffset = offset + 0x28;
        while (true) {
            DiskArea area = new DiskArea();
            areaOffset += area.readFrom(buffer, (int) areaOffset);
            if (area.Offset == 0 && area.Length == 0)
                break;

            areas.add(area);
        }
        DiskAreas = areas;
        areas = new ArrayList<>();
        while (true) {
            DiskArea area = new DiskArea();
            areaOffset += area.readFrom(buffer, (int) areaOffset);
            if (area.Offset == 0 && area.Length == 0)
                break;

            areas.add(area);
        }
        MetadataDiskAreas = areas;
        return (int) getSize();
    }

    /**
     *
     */
    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    private static String readUuid(byte[] buffer, int offset) {
        StringBuilder sb = new StringBuilder();
        sb.append(EndianUtilities.bytesToString(buffer, offset, 0x6)).append('-');
        sb.append(EndianUtilities.bytesToString(buffer, offset + 0x6, 0x4)).append('-');
        sb.append(EndianUtilities.bytesToString(buffer, offset + 0xA, 0x4)).append('-');
        sb.append(EndianUtilities.bytesToString(buffer, offset + 0xE, 0x4)).append('-');
        sb.append(EndianUtilities.bytesToString(buffer, offset + 0x12, 0x4)).append('-');
        sb.append(EndianUtilities.bytesToString(buffer, offset + 0x16, 0x4)).append('-');
        sb.append(EndianUtilities.bytesToString(buffer, offset + 0x1A, 0x6));
        return sb.toString();
    }
}
