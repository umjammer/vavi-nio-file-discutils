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

package DiscUtils.Ext;

import java.util.EnumSet;
import java.util.UUID;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class SuperBlock implements IByteArraySerializable {
    public static final short Ext2Magic = (short) 0xEF53;

    /**
     * Old revision, not supported by DiscUtils.
     */
    public static final int OldRevision = 0;

    public short BlockGroupNumber;

    public int BlocksCount;

    public int BlocksCountHigh;

    public int BlocksPerGroup;

    public int CheckInterval;

    public CompatibleFeatures _CompatibleFeatures = CompatibleFeatures.DirectoryPreallocation;

    public int CompressionAlgorithmUsageBitmap;

    public int CreatorOS;

    public byte DefaultHashVersion;

    public int DefaultMountOptions;

    public short DefaultReservedBlockGid;

    public short DefaultReservedBlockUid;

    public short DescriptorSize;

    public byte DirPreallocateBlockCount;

    public int ReservedGDTBlocks;

    public short Errors;

    public int FirstDataBlock;

    public int FirstInode;

    public int FirstMetablockBlockGroup;

    public int Flags;

    public int FragsPerGroup;

    public int FreeBlocksCount;

    public int FreeBlocksCountHigh;

    public int FreeInodesCount;

    public int[] HashSeed;

    public EnumSet<IncompatibleFeatures> _IncompatibleFeatures = EnumSet.of(IncompatibleFeatures.Compression);

    public int InodesCount;

    public short InodeSize;

    public int InodesPerGroup;

    public int[] JournalBackup;

    public int JournalDevice;

    public int JournalInode;

    public UUID JournalSuperBlockUniqueId;

    public int LastCheckTime;

    public String LastMountPoint;

    public int LastOrphan;

    public int LogBlockSize;

    public int LogFragSize;

    public byte LogGroupsPerFlex;

    public int OverheadBlocksCount;

    public short Magic;

    public short MaxMountCount;

    public short MinimumExtraInodeSize;

    public short MinorRevisionLevel;

    public int MkfsTime;

    public short MountCount;

    public int MountTime;

    public long MultiMountProtectionBlock;

    public short MultiMountProtectionInterval;

    public byte PreallocateBlockCount;

    public short RaidStride;

    public int RaidStripeWidth;

    public ReadOnlyCompatibleFeatures _ReadOnlyCompatibleFeatures = ReadOnlyCompatibleFeatures.SparseSuperblock;

    public int ReservedBlocksCount;

    public int ReservedBlocksCountHigh;

    public int RevisionLevel;

    public short State;

    public UUID UniqueId;

    public String VolumeName;

    public short WantExtraInodeSize;

    public int WriteTime;

    public boolean getHas64Bit() {
        return _IncompatibleFeatures.contains(DiscUtils.Ext.IncompatibleFeatures.SixtyFourBit) && DescriptorSize >= 64;
    }

    public int getBlockSize() {
        return 1024 << LogBlockSize;
    }

    public int size() {
        return 1024;
    }

    public int readFrom(byte[] buffer, int offset) {
        Magic = EndianUtilities.toUInt16LittleEndian(buffer, offset + 56);
        if (Magic != Ext2Magic)
            return size();

        InodesCount = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
        BlocksCount = EndianUtilities.toUInt32LittleEndian(buffer, offset + 4);
        ReservedBlocksCount = EndianUtilities.toUInt32LittleEndian(buffer, offset + 8);
        FreeBlocksCount = EndianUtilities.toUInt32LittleEndian(buffer, offset + 12);
        FreeInodesCount = EndianUtilities.toUInt32LittleEndian(buffer, offset + 16);
        FirstDataBlock = EndianUtilities.toUInt32LittleEndian(buffer, offset + 20);
        LogBlockSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 24);
        LogFragSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 28);
        BlocksPerGroup = EndianUtilities.toUInt32LittleEndian(buffer, offset + 32);
        FragsPerGroup = EndianUtilities.toUInt32LittleEndian(buffer, offset + 36);
        InodesPerGroup = EndianUtilities.toUInt32LittleEndian(buffer, offset + 40);
        MountTime = EndianUtilities.toUInt32LittleEndian(buffer, offset + 44);
        WriteTime = EndianUtilities.toUInt32LittleEndian(buffer, offset + 48);
        MountCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 52);
        MaxMountCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 54);
        State = EndianUtilities.toUInt16LittleEndian(buffer, offset + 58);
        Errors = EndianUtilities.toUInt16LittleEndian(buffer, offset + 60);
        MinorRevisionLevel = EndianUtilities.toUInt16LittleEndian(buffer, offset + 62);
        LastCheckTime = EndianUtilities.toUInt32LittleEndian(buffer, offset + 64);
        CheckInterval = EndianUtilities.toUInt32LittleEndian(buffer, offset + 68);
        CreatorOS = EndianUtilities.toUInt32LittleEndian(buffer, offset + 72);
        RevisionLevel = EndianUtilities.toUInt32LittleEndian(buffer, offset + 76);
        DefaultReservedBlockUid = EndianUtilities.toUInt16LittleEndian(buffer, offset + 80);
        DefaultReservedBlockGid = EndianUtilities.toUInt16LittleEndian(buffer, offset + 82);
        FirstInode = EndianUtilities.toUInt32LittleEndian(buffer, offset + 84);
        InodeSize = EndianUtilities.toUInt16LittleEndian(buffer, offset + 88);
        BlockGroupNumber = EndianUtilities.toUInt16LittleEndian(buffer, offset + 90);
        _CompatibleFeatures = CompatibleFeatures.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 92));
        _IncompatibleFeatures = IncompatibleFeatures.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 96));
        _ReadOnlyCompatibleFeatures = ReadOnlyCompatibleFeatures
                .valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 100));
        UniqueId = EndianUtilities.toGuidLittleEndian(buffer, offset + 104);
        VolumeName = EndianUtilities.bytesToZString(buffer, offset + 120, 16);
        LastMountPoint = EndianUtilities.bytesToZString(buffer, offset + 136, 64);
        CompressionAlgorithmUsageBitmap = EndianUtilities.toUInt32LittleEndian(buffer, offset + 200);
        PreallocateBlockCount = buffer[offset + 204];
        DirPreallocateBlockCount = buffer[offset + 205];
        ReservedGDTBlocks = EndianUtilities.toUInt16LittleEndian(buffer, offset + 206);
        JournalSuperBlockUniqueId = EndianUtilities.toGuidLittleEndian(buffer, offset + 208);
        JournalInode = EndianUtilities.toUInt32LittleEndian(buffer, offset + 224);
        JournalDevice = EndianUtilities.toUInt32LittleEndian(buffer, offset + 228);
        LastOrphan = EndianUtilities.toUInt32LittleEndian(buffer, offset + 232);
        HashSeed = new int[4];
        HashSeed[0] = EndianUtilities.toUInt32LittleEndian(buffer, offset + 236);
        HashSeed[1] = EndianUtilities.toUInt32LittleEndian(buffer, offset + 240);
        HashSeed[2] = EndianUtilities.toUInt32LittleEndian(buffer, offset + 244);
        HashSeed[3] = EndianUtilities.toUInt32LittleEndian(buffer, offset + 248);
        DefaultHashVersion = buffer[offset + 252];
        DescriptorSize = EndianUtilities.toUInt16LittleEndian(buffer, offset + 254);
        DefaultMountOptions = EndianUtilities.toUInt32LittleEndian(buffer, offset + 256);
        FirstMetablockBlockGroup = EndianUtilities.toUInt32LittleEndian(buffer, offset + 260);
        MkfsTime = EndianUtilities.toUInt32LittleEndian(buffer, offset + 264);
        JournalBackup = new int[17];
        for (int i = 0; i < 17; ++i) {
            JournalBackup[i] = EndianUtilities.toUInt32LittleEndian(buffer, offset + 268 + 4 * i);
        }
        BlocksCountHigh = EndianUtilities.toUInt32LittleEndian(buffer, offset + 336);
        ReservedBlocksCountHigh = EndianUtilities.toUInt32LittleEndian(buffer, offset + 340);
        FreeBlocksCountHigh = EndianUtilities.toUInt32LittleEndian(buffer, offset + 344);
        MinimumExtraInodeSize = EndianUtilities.toUInt16LittleEndian(buffer, offset + 348);
        WantExtraInodeSize = EndianUtilities.toUInt16LittleEndian(buffer, offset + 350);
        Flags = EndianUtilities.toUInt32LittleEndian(buffer, offset + 352);
        RaidStride = EndianUtilities.toUInt16LittleEndian(buffer, offset + 356);
        MultiMountProtectionInterval = EndianUtilities.toUInt16LittleEndian(buffer, offset + 358);
        MultiMountProtectionBlock = EndianUtilities.toUInt64LittleEndian(buffer, offset + 360);
        RaidStripeWidth = EndianUtilities.toUInt32LittleEndian(buffer, offset + 368);
        LogGroupsPerFlex = buffer[offset + 372];
        OverheadBlocksCount = EndianUtilities.toUInt32LittleEndian(buffer, offset + 584);
        return 1024;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
