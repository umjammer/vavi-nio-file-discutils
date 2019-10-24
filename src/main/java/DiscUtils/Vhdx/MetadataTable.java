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

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Sizes;


public final class MetadataTable implements IByteArraySerializable {
    public static final int FixedSize = (int) (64 * Sizes.OneKiB);

    public static final long MetadataTableSignature = 0x617461646174656Dl;

    public static final UUID FileParametersGuid = UUID.fromString("CAA16737-FA36-4D43-B3B6-33F0AA44E76B");

    public static final UUID VirtualDiskSizeGuid = UUID.fromString("2FA54224-CD1B-4876-B211-5DBED83BF4B8");

    public static final UUID Page83DataGuid = UUID.fromString("BECA12AB-B2E6-4523-93EF-C309E000C746");

    public static final UUID LogicalSectorSizeGuid = UUID.fromString("8141Bf1D-A96F-4709-BA47-F233A8FAAb5F");

    public static final UUID PhysicalSectorSizeGuid = UUID.fromString("CDA348C7-445D-4471-9CC9-E9885251C556");

    public static final UUID ParentLocatorGuid = UUID.fromString("A8D35F2D-B30B-454D-ABF7-D3D84834AB0C");

    private static final Map<UUID, Object> KnownMetadata = initMetadataTable();

    private final byte[] _headerData = new byte[32];

    public Map<MetadataEntryKey, MetadataEntry> Entries = new HashMap<>();

    public short EntryCount;

    public long Signature = MetadataTableSignature;

    public boolean getIsValid() {
        if (Signature != MetadataTableSignature) {
            return false;
        }

        if (EntryCount > 2047) {
            return false;
        }

        for (MetadataEntry entry : Entries.values()) {
            if (entry.Flags.contains(MetadataEntryFlags.IsRequired)) {
                if (!KnownMetadata.containsKey(entry.ItemId)) {
                    return false;
                }
            }
        }
        return true;
    }

    public int sizeOf() {
        return FixedSize;
    }

    public int readFrom(byte[] buffer, int offset) {
        System.arraycopy(buffer, offset, _headerData, 0, 32);
        Signature = EndianUtilities.toUInt64LittleEndian(_headerData, 0);
        EntryCount = EndianUtilities.toUInt16LittleEndian(_headerData, 10);
        Entries = new HashMap<>();
        if (getIsValid()) {
            for (int i = 0; i < EntryCount; ++i) {
                MetadataEntry entry = EndianUtilities.toStruct(MetadataEntry.class, buffer, offset + 32 + i * 32);
                Entries.put(MetadataEntryKey.fromEntry(entry), entry);
            }
        }

        return FixedSize;
    }

    public void writeTo(byte[] buffer, int offset) {
        EntryCount = (short) Entries.size();
        EndianUtilities.writeBytesLittleEndian(Signature, _headerData, 0);
        EndianUtilities.writeBytesLittleEndian(EntryCount, _headerData, 10);
        System.arraycopy(_headerData, 0, buffer, offset, 32);
        int bufferOffset = 32 + offset;
        for (Map.Entry<MetadataEntryKey, MetadataEntry> entry : Entries.entrySet()) {
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
