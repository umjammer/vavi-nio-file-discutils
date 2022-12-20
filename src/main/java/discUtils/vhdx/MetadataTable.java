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

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.Sizes;
import vavi.util.ByteUtil;


public final class MetadataTable implements IByteArraySerializable {

    public static final int FixedSize = (int) (64 * Sizes.OneKiB);

    public static final long MetadataTableSignature = 0x617461646174656DL;

    public static final UUID FileParametersGuid = UUID.fromString("CAA16737-FA36-4D43-B3B6-33F0AA44E76B");

    public static final UUID VirtualDiskSizeGuid = UUID.fromString("2FA54224-CD1B-4876-B211-5DBED83BF4B8");

    public static final UUID Page83DataGuid = UUID.fromString("BECA12AB-B2E6-4523-93EF-C309E000C746");

    public static final UUID LogicalSectorSizeGuid = UUID.fromString("8141Bf1D-A96F-4709-BA47-F233A8FAAb5F");

    public static final UUID PhysicalSectorSizeGuid = UUID.fromString("CDA348C7-445D-4471-9CC9-E9885251C556");

    public static final UUID ParentLocatorGuid = UUID.fromString("A8D35F2D-B30B-454D-ABF7-D3D84834AB0C");

    private static final Map<UUID, Object> KnownMetadata = initMetadataTable();

    private final byte[] headerData = new byte[32];

    public Map<MetadataEntryKey, MetadataEntry> entries = new HashMap<>();

    public short entryCount;

    public long signature = MetadataTableSignature;

    public boolean getIsValid() {
        if (signature != MetadataTableSignature) {
            return false;
        }

        if ((entryCount & 0xffff) > 2047) {
            return false;
        }

        for (MetadataEntry entry : entries.values()) {
            if (entry.flags.contains(MetadataEntryFlags.IsRequired)) {
                if (!KnownMetadata.containsKey(entry.itemId)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override public int size() {
        return FixedSize;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        System.arraycopy(buffer, offset, headerData, 0, 32);
        signature = ByteUtil.readLeLong(headerData, 0);
        entryCount = ByteUtil.readLeShort(headerData, 10);
        entries = new HashMap<>();
        if (getIsValid()) {
            for (int i = 0; i < entryCount; ++i) {
                MetadataEntry entry = EndianUtilities.toStruct(MetadataEntry.class, buffer, offset + 32 + i * 32);
                entries.put(MetadataEntryKey.fromEntry(entry), entry);
            }
        }

        return FixedSize;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        entryCount = (short) entries.size();
        ByteUtil.writeLeLong(signature, headerData, 0);
        ByteUtil.writeLeShort(entryCount, headerData, 10);
        System.arraycopy(headerData, 0, buffer, offset, 32);
        int bufferOffset = 32 + offset;
        for (Map.Entry<MetadataEntryKey, MetadataEntry> entry : entries.entrySet()) {
            entry.getValue().writeTo(buffer, bufferOffset);
            bufferOffset += 32;
        }
    }

    private static Map<UUID, Object> initMetadataTable() {
        Map<UUID, Object> knownMetadata = new HashMap<>();
        knownMetadata.put(FileParametersGuid, null);
        knownMetadata.put(VirtualDiskSizeGuid, null);
        knownMetadata.put(Page83DataGuid, null);
        knownMetadata.put(LogicalSectorSizeGuid, null);
        knownMetadata.put(PhysicalSectorSizeGuid, null);
        knownMetadata.put(ParentLocatorGuid, null);
        return knownMetadata;
    }
}
