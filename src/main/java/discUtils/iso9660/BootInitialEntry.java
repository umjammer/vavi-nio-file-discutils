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

package discUtils.iso9660;

import java.util.Arrays;

import discUtils.streams.util.EndianUtilities;


public class BootInitialEntry {

    public byte bootIndicator;

    public BootDeviceEmulation bootMediaType = BootDeviceEmulation.NoEmulation;

    public int imageStart;

    private short loadSegment;

    public int getLoadSegment() {
        return loadSegment & 0xffff;
    }

    public void setLoadSegment(short value) {
        loadSegment = value;
    }

    private short sectorCount;

    public int getSectorCount() {
        return sectorCount;
    }

    public void setSectorCount(short value) {
        sectorCount = value;
    }

    public byte systemType;

    public BootInitialEntry() {
    }

    public BootInitialEntry(byte[] buffer, int offset) {
        bootIndicator = buffer[offset + 0x00];
        bootMediaType = BootDeviceEmulation.values()[buffer[offset + 0x01]];
        loadSegment = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x02);
        systemType = buffer[offset + 0x04];
        sectorCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x06);
        imageStart = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x08);
    }

    public void writeTo(byte[] buffer, int offset) {
        Arrays.fill(buffer, offset, offset + 0x20, (byte) 0);
        buffer[offset + 0x00] = bootIndicator;
        buffer[offset + 0x01] = (byte) bootMediaType.ordinal();
        EndianUtilities.writeBytesLittleEndian(loadSegment, buffer, offset + 0x02);
        buffer[offset + 0x04] = systemType;
        EndianUtilities.writeBytesLittleEndian(sectorCount, buffer, offset + 0x06);
        EndianUtilities.writeBytesLittleEndian(imageStart, buffer, offset + 0x08);
    }
}
