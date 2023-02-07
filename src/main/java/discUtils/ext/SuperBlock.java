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

package discUtils.ext;

import java.util.EnumSet;
import java.util.UUID;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;
import vavi.util.ByteUtil;


public class SuperBlock implements IByteArraySerializable {
    public static final short Ext2Magic = (short) 0xEF53;

    /**
     * Old revision, not supported by DiscUtils.
     */
    public static final int OldRevision = 0;

    public short blockGroupNumber;

    public int blocksCount;

    public int blocksCountHigh;

    public int blocksPerGroup;

    public int checkInterval;

    public EnumSet<CompatibleFeatures> compatibleFeatures = EnumSet.noneOf(CompatibleFeatures.class);

    public int compressionAlgorithmUsageBitmap;

    public int creatorOS;

    public byte defaultHashVersion;

    public int defaultMountOptions;

    public short defaultReservedBlockGid;

    public short defaultReservedBlockUid;

    public short descriptorSize;

    public byte dirPreallocateBlockCount;

    public int reservedGDTBlocks;

    public short errors;

    public int firstDataBlock;

    public int firstInode;

    public int firstMetablockBlockGroup;

    public int flags;

    public int fragsPerGroup;

    public int freeBlocksCount;

    public int freeBlocksCountHigh;

    public int freeInodesCount;

    public int[] hashSeed;

    public EnumSet<IncompatibleFeatures> incompatibleFeatures = EnumSet.noneOf(IncompatibleFeatures.class);

    public int inodesCount;

    private short inodeSize;

    public int getInodeSize() {
        return inodeSize & 0xffff;
    }

    public int inodesPerGroup;

    public int[] journalBackup;

    public int journalDevice;

    public int journalInode;

    public UUID journalSuperBlockUniqueId;

    public int lastCheckTime;

    public String lastMountPoint;

    public int lastOrphan;

    public int logBlockSize;

    public int logFragSize;

    public byte logGroupsPerFlex;

    public int overheadBlocksCount;

    public short magic;

    public short maxMountCount;

    public short minimumExtraInodeSize;

    public short minorRevisionLevel;

    public int mkfsTime;

    public short mountCount;

    public int mountTime;

    public long multiMountProtectionBlock;

    public short multiMountProtectionInterval;

    public byte preallocateBlockCount;

    public short raidStride;

    public int raidStripeWidth;

    public EnumSet<ReadOnlyCompatibleFeatures> readOnlyCompatibleFeatures = EnumSet.noneOf(ReadOnlyCompatibleFeatures.class);

    public int reservedBlocksCount;

    public int reservedBlocksCountHigh;

    public int revisionLevel;

    public short state;

    public UUID uniqueId;

    public String volumeName;

    public short wantExtraInodeSize;

    public int writeTime;

    public boolean has64Bit() {
        return incompatibleFeatures.contains(discUtils.ext.IncompatibleFeatures.SixtyFourBit) && descriptorSize >= 64;
    }

    public int getBlockSize() {
        return 1024 << logBlockSize;
    }

    @Override public int size() {
        return 1024;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        magic = ByteUtil.readLeShort(buffer, offset + 56);
        if (magic != Ext2Magic)
            return size();

        inodesCount = ByteUtil.readLeInt(buffer, offset + 0);
        blocksCount = ByteUtil.readLeInt(buffer, offset + 4);
        reservedBlocksCount = ByteUtil.readLeInt(buffer, offset + 8);
        freeBlocksCount = ByteUtil.readLeInt(buffer, offset + 12);
        freeInodesCount = ByteUtil.readLeInt(buffer, offset + 16);
        firstDataBlock = ByteUtil.readLeInt(buffer, offset + 20);
        logBlockSize = ByteUtil.readLeInt(buffer, offset + 24);
        logFragSize = ByteUtil.readLeInt(buffer, offset + 28);
        blocksPerGroup = ByteUtil.readLeInt(buffer, offset + 32);
        fragsPerGroup = ByteUtil.readLeInt(buffer, offset + 36);
        inodesPerGroup = ByteUtil.readLeInt(buffer, offset + 40);
        mountTime = ByteUtil.readLeInt(buffer, offset + 44);
        writeTime = ByteUtil.readLeInt(buffer, offset + 48);
        mountCount = ByteUtil.readLeShort(buffer, offset + 52);
        maxMountCount = ByteUtil.readLeShort(buffer, offset + 54);
        state = ByteUtil.readLeShort(buffer, offset + 58);
        errors = ByteUtil.readLeShort(buffer, offset + 60);
        minorRevisionLevel = ByteUtil.readLeShort(buffer, offset + 62);
        lastCheckTime = ByteUtil.readLeInt(buffer, offset + 64);
        checkInterval = ByteUtil.readLeInt(buffer, offset + 68);
        creatorOS = ByteUtil.readLeInt(buffer, offset + 72);
        revisionLevel = ByteUtil.readLeInt(buffer, offset + 76);
        defaultReservedBlockUid = ByteUtil.readLeShort(buffer, offset + 80);
        defaultReservedBlockGid = ByteUtil.readLeShort(buffer, offset + 82);
        firstInode = ByteUtil.readLeInt(buffer, offset + 84);
        inodeSize = ByteUtil.readLeShort(buffer, offset + 88);
        blockGroupNumber = ByteUtil.readLeShort(buffer, offset + 90);
        compatibleFeatures = CompatibleFeatures.valueOf(ByteUtil.readLeInt(buffer, offset + 92));
        incompatibleFeatures = IncompatibleFeatures.valueOf(ByteUtil.readLeInt(buffer, offset + 96));
        readOnlyCompatibleFeatures = ReadOnlyCompatibleFeatures
                .valueOf(ByteUtil.readLeInt(buffer, offset + 100));
        uniqueId = ByteUtil.readLeUUID(buffer, offset + 104);
        volumeName = EndianUtilities.bytesToZString(buffer, offset + 120, 16);
        lastMountPoint = EndianUtilities.bytesToZString(buffer, offset + 136, 64);
        compressionAlgorithmUsageBitmap = ByteUtil.readLeInt(buffer, offset + 200);
        preallocateBlockCount = buffer[offset + 204];
        dirPreallocateBlockCount = buffer[offset + 205];
        reservedGDTBlocks = ByteUtil.readLeShort(buffer, offset + 206);
        journalSuperBlockUniqueId = ByteUtil.readLeUUID(buffer, offset + 208);
        journalInode = ByteUtil.readLeInt(buffer, offset + 224);
        journalDevice = ByteUtil.readLeInt(buffer, offset + 228);
        lastOrphan = ByteUtil.readLeInt(buffer, offset + 232);
        hashSeed = new int[4];
        hashSeed[0] = ByteUtil.readLeInt(buffer, offset + 236);
        hashSeed[1] = ByteUtil.readLeInt(buffer, offset + 240);
        hashSeed[2] = ByteUtil.readLeInt(buffer, offset + 244);
        hashSeed[3] = ByteUtil.readLeInt(buffer, offset + 248);
        defaultHashVersion = buffer[offset + 252];
        descriptorSize = ByteUtil.readLeShort(buffer, offset + 254);
        defaultMountOptions = ByteUtil.readLeInt(buffer, offset + 256);
        firstMetablockBlockGroup = ByteUtil.readLeInt(buffer, offset + 260);
        mkfsTime = ByteUtil.readLeInt(buffer, offset + 264);
        journalBackup = new int[17];
        for (int i = 0; i < 17; ++i) {
            journalBackup[i] = ByteUtil.readLeInt(buffer, offset + 268 + 4 * i);
        }
        blocksCountHigh = ByteUtil.readLeInt(buffer, offset + 336);
        reservedBlocksCountHigh = ByteUtil.readLeInt(buffer, offset + 340);
        freeBlocksCountHigh = ByteUtil.readLeInt(buffer, offset + 344);
        minimumExtraInodeSize = ByteUtil.readLeShort(buffer, offset + 348);
        wantExtraInodeSize = ByteUtil.readLeShort(buffer, offset + 350);
        flags = ByteUtil.readLeInt(buffer, offset + 352);
        raidStride = ByteUtil.readLeShort(buffer, offset + 356);
        multiMountProtectionInterval = ByteUtil.readLeShort(buffer, offset + 358);
        multiMountProtectionBlock = ByteUtil.readLeLong(buffer, offset + 360);
        raidStripeWidth = ByteUtil.readLeInt(buffer, offset + 368);
        logGroupsPerFlex = buffer[offset + 372];
        overheadBlocksCount = ByteUtil.readLeInt(buffer, offset + 584);
        return 1024;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
