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

package DiscUtils.Vdi;

import DiscUtils.Core.Geometry;
import DiscUtils.Streams.Util.EndianUtilities;


public class GeometryRecord {
    public int Cylinders;

    public int Heads;

    public int Sectors;

    public int SectorSize;

    public GeometryRecord() {
        SectorSize = 512;
    }

    public static GeometryRecord fromCapacity(long capacity) {
        GeometryRecord result = new GeometryRecord();
        long totalSectors = capacity / 512;
        if (totalSectors / (16 * 63) <= 1024) {
            result.Cylinders = (int) Math.max(totalSectors / (16 * 63), 1);
            result.Heads = 16;
        } else if (totalSectors / (32 * 63) <= 1024) {
            result.Cylinders = (int) Math.max(totalSectors / (32 * 63), 1);
            result.Heads = 32;
        } else if (totalSectors / (64 * 63) <= 1024) {
            result.Cylinders = (int) (totalSectors / (64 * 63));
            result.Heads = 64;
        } else if (totalSectors / (128 * 63) <= 1024) {
            result.Cylinders = (int) (totalSectors / (128 * 63));
            result.Heads = 128;
        } else {
            result.Cylinders = (int) Math.min(totalSectors / (255 * 63), 1024);
            result.Heads = 255;
        }
        result.Sectors = 63;
        return result;
    }

    public void read(byte[] buffer, int offset) {
        Cylinders = EndianUtilities.toInt32LittleEndian(buffer, offset + 0);
        Heads = EndianUtilities.toInt32LittleEndian(buffer, offset + 4);
        Sectors = EndianUtilities.toInt32LittleEndian(buffer, offset + 8);
        SectorSize = EndianUtilities.toInt32LittleEndian(buffer, offset + 12);
    }

    public void write(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(Cylinders, buffer, offset + 0);
        EndianUtilities.writeBytesLittleEndian(Heads, buffer, offset + 4);
        EndianUtilities.writeBytesLittleEndian(Sectors, buffer, offset + 8);
        EndianUtilities.writeBytesLittleEndian(SectorSize, buffer, offset + 12);
    }

    public Geometry toGeometry(long actualCapacity) {
        long cylinderCapacity = SectorSize * (long) Sectors * Heads;
        return new Geometry((int) (actualCapacity / cylinderCapacity), Heads, Sectors, SectorSize);
    }

}
