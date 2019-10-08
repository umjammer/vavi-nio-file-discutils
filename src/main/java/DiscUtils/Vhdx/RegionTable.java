//
// Copyright (c) 2008-2012, Kenneth Bell
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

package DiscUtils.Vhdx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import DiscUtils.Core.Internal.Crc32Algorithm;
import DiscUtils.Core.Internal.Crc32LittleEndian;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Sizes;


public final class RegionTable implements IByteArraySerializable {
    public static final int RegionTableSignature = 0x69676572;

    public static final int FixedSize = (int) (64 * Sizes.OneKiB);

    public static final UUID BatGuid = UUID.fromString("2DC27766-F623-4200-9D64-115E9BFD4A08");

    public static final UUID MetadataRegionGuid = UUID.fromString("8B7CA206-4790-4B9A-B8FE-575F050F886E");

    private final byte[] _data = new byte[FixedSize];

    public int Checksum;

    public int EntryCount;

    public Map<UUID, RegionEntry> Regions;

    public int Reserved;

    public int Signature = RegionTableSignature;

    public boolean isValid() {
        if (Signature != RegionTableSignature) {
            return false;
        }

        if (EntryCount > 2047) {
            return false;
        }

        byte[] checkData = new byte[FixedSize];
        System.arraycopy(_data, 0, checkData, 0, FixedSize);
        EndianUtilities.writeBytesLittleEndian(0, checkData, 4);
        return Checksum == Crc32LittleEndian.compute(Crc32Algorithm.Castagnoli, checkData, 0, FixedSize);
    }

    public long getSize() {
        return FixedSize;
    }

    public int readFrom(byte[] buffer, int offset) {
        System.arraycopy(buffer, offset, _data, 0, FixedSize);
        Signature = EndianUtilities.toUInt32LittleEndian(_data, 0);
        Checksum = EndianUtilities.toUInt32LittleEndian(_data, 4);
        EntryCount = EndianUtilities.toUInt32LittleEndian(_data, 8);
        Reserved = EndianUtilities.toUInt32LittleEndian(_data, 12);
        Regions = new HashMap<>();
        if (isValid()) {
            for (int i = 0; i < EntryCount; ++i) {
                RegionEntry entry = EndianUtilities.toStruct(RegionEntry.class, _data, 16 + 32 * i);
                Regions.put(entry.Guid, entry);
            }
        }

        return (int) getSize();
    }

    public void writeTo(byte[] buffer, int offset) {
        EntryCount = Regions.size();
        Checksum = 0;
        EndianUtilities.writeBytesLittleEndian(Signature, _data, 0);
        EndianUtilities.writeBytesLittleEndian(Checksum, _data, 4);
        EndianUtilities.writeBytesLittleEndian(EntryCount, _data, 8);
        int dataOffset = 16;
        for (Map.Entry<UUID, RegionEntry> region : Regions.entrySet()) {
            region.getValue().writeTo(_data, dataOffset);
            dataOffset += 32;
        }
        Checksum = Crc32LittleEndian.compute(Crc32Algorithm.Castagnoli, _data, 0, FixedSize);
        EndianUtilities.writeBytesLittleEndian(Checksum, _data, 4);
        System.arraycopy(_data, 0, buffer, offset, FixedSize);
    }
}
