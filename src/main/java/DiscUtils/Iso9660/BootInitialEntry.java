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

package DiscUtils.Iso9660;

import java.util.Arrays;

import DiscUtils.Streams.Util.EndianUtilities;


public class BootInitialEntry {
    public byte BootIndicator;

    public BootDeviceEmulation BootMediaType = BootDeviceEmulation.NoEmulation;

    public int ImageStart;

    public short LoadSegment;

    public short SectorCount;

    public byte SystemType;

    public BootInitialEntry() {
    }

    public BootInitialEntry(byte[] buffer, int offset) {
        BootIndicator = buffer[offset + 0x00];
        BootMediaType = BootDeviceEmulation.valueOf(buffer[offset + 0x01]);
        LoadSegment = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x02);
        SystemType = buffer[offset + 0x04];
        SectorCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x06);
        ImageStart = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x08);
    }

    public void writeTo(byte[] buffer, int offset) {
        Arrays.fill(buffer, offset, offset + 0x20, (byte) 0);
        buffer[offset + 0x00] = BootIndicator;
        buffer[offset + 0x01] = (byte) BootMediaType.ordinal();
        EndianUtilities.writeBytesLittleEndian(LoadSegment, buffer, offset + 0x02);
        buffer[offset + 0x04] = SystemType;
        EndianUtilities.writeBytesLittleEndian(SectorCount, buffer, offset + 0x06);
        EndianUtilities.writeBytesLittleEndian(ImageStart, buffer, offset + 0x08);
    }
}
