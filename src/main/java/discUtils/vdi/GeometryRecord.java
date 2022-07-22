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

package discUtils.vdi;

import discUtils.core.Geometry;
import discUtils.streams.util.EndianUtilities;


public class GeometryRecord {

    public int cylinders;

    public int heads;

    public int sectors;

    public int sectorSize;

    public GeometryRecord() {
        sectorSize = 512;
    }

    public static GeometryRecord fromCapacity(long capacity) {
        GeometryRecord result = new GeometryRecord();

        long totalSectors = capacity / 512;
        if (totalSectors / (16 * 63) <= 1024) {
            result.cylinders = (int) Math.max(totalSectors / (16 * 63), 1);
            result.heads = 16;
        } else if (totalSectors / (32 * 63) <= 1024) {
            result.cylinders = (int) Math.max(totalSectors / (32 * 63), 1);
            result.heads = 32;
        } else if (totalSectors / (64 * 63) <= 1024) {
            result.cylinders = (int) (totalSectors / (64 * 63));
            result.heads = 64;
        } else if (totalSectors / (128 * 63) <= 1024) {
            result.cylinders = (int) (totalSectors / (128 * 63));
            result.heads = 128;
        } else {
            result.cylinders = (int) Math.min(totalSectors / (255 * 63), 1024);
            result.heads = 255;
        }

        result.sectors = 63;

        return result;
    }

    public void read(byte[] buffer, int offset) {
        cylinders = EndianUtilities.toInt32LittleEndian(buffer, offset + 0);
        heads = EndianUtilities.toInt32LittleEndian(buffer, offset + 4);
        sectors = EndianUtilities.toInt32LittleEndian(buffer, offset + 8);
        sectorSize = EndianUtilities.toInt32LittleEndian(buffer, offset + 12);
    }

    public void write(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(cylinders, buffer, offset + 0);
        EndianUtilities.writeBytesLittleEndian(heads, buffer, offset + 4);
        EndianUtilities.writeBytesLittleEndian(sectors, buffer, offset + 8);
        EndianUtilities.writeBytesLittleEndian(sectorSize, buffer, offset + 12);
    }

    public Geometry toGeometry(long actualCapacity) {
        long cylinderCapacity = sectorSize * (long) sectors * heads;
        return new Geometry((int) (actualCapacity / cylinderCapacity), heads, sectors, sectorSize);
    }
}
