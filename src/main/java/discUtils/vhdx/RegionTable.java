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

package discUtils.vhdx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import discUtils.core.internal.Crc32Algorithm;
import discUtils.core.internal.Crc32LittleEndian;
import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.Sizes;
import vavi.util.ByteUtil;


public final class RegionTable implements IByteArraySerializable {

    public static final int RegionTableSignature = 0x69676572;

    public static final int FixedSize = (int) (64 * Sizes.OneKiB);

    public static final UUID BatGuid = UUID.fromString("2DC27766-F623-4200-9D64-115E9BFD4A08");

    public static final UUID MetadataRegionGuid = UUID.fromString("8B7CA206-4790-4B9A-B8FE-575F050F886E");

    private final byte[] data = new byte[FixedSize];

    public int checksum;

    public int entryCount;

    public Map<UUID, RegionEntry> regions = new HashMap<>();

    public int reserved;

    public int signature = RegionTableSignature;

    public boolean isValid() {
        if (signature != RegionTableSignature) {
            return false;
        }

        if (entryCount > 2047) {
            return false;
        }

        byte[] checkData = new byte[FixedSize];
        System.arraycopy(data, 0, checkData, 0, FixedSize);
        ByteUtil.writeLeInt(0, checkData, 4);
        return checksum == Crc32LittleEndian.compute(Crc32Algorithm.Castagnoli, checkData, 0, FixedSize);
    }

    public int size() {
        return FixedSize;
    }

    public int readFrom(byte[] buffer, int offset) {
        System.arraycopy(buffer, offset, data, 0, FixedSize);
        signature = ByteUtil.readLeInt(data, 0);
        checksum = ByteUtil.readLeInt(data, 4);
        entryCount = ByteUtil.readLeInt(data, 8);
        reserved = ByteUtil.readLeInt(data, 12);
        regions = new HashMap<>();
        if (isValid()) {
            for (int i = 0; i < entryCount; ++i) {
                RegionEntry entry = EndianUtilities.toStruct(RegionEntry.class, data, 16 + 32 * i);
                regions.put(entry.guid, entry);
            }
        }

        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        entryCount = regions.size();
        checksum = 0;
        ByteUtil.writeLeInt(signature, data, 0);
        ByteUtil.writeLeInt(checksum, data, 4);
        ByteUtil.writeLeInt(entryCount, data, 8);
        int dataOffset = 16;
        for (Map.Entry<UUID, RegionEntry> region : regions.entrySet()) {
            region.getValue().writeTo(data, dataOffset);
            dataOffset += 32;
        }
        checksum = Crc32LittleEndian.compute(Crc32Algorithm.Castagnoli, data, 0, FixedSize);
        ByteUtil.writeLeInt(checksum, data, 4);
        System.arraycopy(data, 0, buffer, offset, FixedSize);
    }
}
