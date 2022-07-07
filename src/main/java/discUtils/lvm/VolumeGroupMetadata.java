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

import java.util.ArrayList;
import java.util.List;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public class VolumeGroupMetadata implements IByteArraySerializable {
    public static final String VgMetadataMagic = " LVM2 x[5A%r0N*>";

    public static final int VgMetadataVersion = 1;

    public int Crc;

    public long CalculatedCrc;

    public String Magic;

    public int Version;

    public long Start;

    public long Length;

    public List<RawLocation> RawLocations;

    public String _Metadata;

    public Metadata ParsedMetadata;

    /**
     *
     */
    public int size() {
        return (int) Length;
    }

    /**
     *
     */
    public int readFrom(byte[] buffer, int offset) {
        Crc = EndianUtilities.toUInt32LittleEndian(buffer, offset);
        CalculatedCrc = PhysicalVolume.calcCrc(buffer, offset + 0x4, PhysicalVolume.SECTOR_SIZE - 0x4);
        Magic = EndianUtilities.bytesToString(buffer, offset + 0x4, 0x10);
        Version = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x14);
        Start = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x18);
        Length = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x20);
        List<RawLocation> locations = new ArrayList<>();
        int locationOffset = offset + 0x28;
        while (true) {
            RawLocation location = new RawLocation();
            locationOffset += location.readFrom(buffer, locationOffset);
            if (location.Offset == 0 && location.Length == 0 && location.Checksum == 0 && location.Flags.ordinal() == 0)
                break;

            locations.add(location);
        }
        RawLocations = locations;
        for (RawLocation location : RawLocations) {
            if (location.Flags == RawLocationFlags.Ignored)
                continue;

            int checksum = PhysicalVolume.calcCrc(buffer, (int) location.Offset, (int) location.Length);
            if (location.Checksum != checksum)
                throw new dotnet4j.io.IOException("invalid metadata checksum");

            _Metadata = EndianUtilities.bytesToString(buffer, (int) location.Offset, (int) location.Length);
            ParsedMetadata = Metadata.parse(_Metadata);
            break;
        }
        return size();
    }

    /**
     *
     */
    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
