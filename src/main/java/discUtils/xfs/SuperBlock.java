//
// Copyright (c) 2008-2011, Kenneth Bell
// Copyright (c) 2016, Bianco Veigel
// Copyright (c) 2017, Timo Walter
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

package discUtils.xfs;

import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public class SuperBlock implements IByteArraySerializable {

    public static final int XfsMagic = 0x58465342;

    /**
     * magic number == XFS_SB_MAGIC
     */
    private int magic;

    public int getMagic() {
        return magic;
    }

    public void setMagic(int value) {
        magic = value;
    }

    /**
     * logical block size, bytes
     */
    private int blocksize;

    public int getBlocksize() {
        return blocksize;
    }

    public void setBlocksize(int value) {
        blocksize = value;
    }

    /**
     * number of data blocks
     */
    private long dataBlocks;

    public long getDataBlocks() {
        return dataBlocks;
    }

    public void setDataBlocks(long value) {
        dataBlocks = value;
    }

    /**
     * number of realtime blocks
     */
    private long realtimeBlocks;

    public long getRealtimeBlocks() {
        return realtimeBlocks;
    }

    public void setRealtimeBlocks(long value) {
        realtimeBlocks = value;
    }

    /**
     * number of realtime extents
     */
    private long realtimeExtents;

    public long getRealtimeExtents() {
        return realtimeExtents;
    }

    public void setRealtimeExtents(long value) {
        realtimeExtents = value;
    }

    /**
     * user-visible file system unique id
     */
    private UUID uniqueId;

    public UUID getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(UUID value) {
        uniqueId = value;
    }

    /**
     * starting block of log if internal
     */
    private long logstart;

    public long getLogstart() {
        return logstart;
    }

    public void setLogstart(long value) {
        logstart = value;
    }

    /**
     * root inode number
     */
    private long rootInode;

    public long getRootInode() {
        return rootInode;
    }

    public void setRootInode(long value) {
        rootInode = value;
    }

    /**
     * bitmap inode for realtime extents
     */
    private long realtimeBitmapInode;

    public long getRealtimeBitmapInode() {
        return realtimeBitmapInode;
    }

    public void setRealtimeBitmapInode(long value) {
        realtimeBitmapInode = value;
    }

    /**
     * summary inode for rt bitmap
     */
    private long realtimeSummaryInode;

    public long getRealtimeSummaryInode() {
        return realtimeSummaryInode;
    }

    public void setRealtimeSummaryInode(long value) {
        realtimeSummaryInode = value;
    }

    /**
     * realtime extent size, blocks
     */
    private int realtimeExtentSize;

    public int getRealtimeExtentSize() {
        return realtimeExtentSize;
    }

    public void setRealtimeExtentSize(int value) {
        realtimeExtentSize = value;
    }

    /**
     * size of an allocation group
     */
    private int agBlocks;

    public int getAgBlocks() {
        return agBlocks;
    }

    public void setAgBlocks(int value) {
        agBlocks = value;
    }

    /**
     * number of allocation groups
     */
    private int agCount;

    public int getAgCount() {
        return agCount;
    }

    public void setAgCount(int value) {
        agCount = value;
    }

    /**
     * number of rt bitmap blocks
     */
    private int realtimeBitmapBlocks;

    public int getRealtimeBitmapBlocks() {
        return realtimeBitmapBlocks;
    }

    public void setRealtimeBitmapBlocks(int value) {
        realtimeBitmapBlocks = value;
    }

    /**
     * number of log blocks
     */
    private int logBlocks;

    public int getLogBlocks() {
        return logBlocks;
    }

    public void setLogBlocks(int value) {
        logBlocks = value;
    }

    private int version;

    /**
     * header version == XFS_SB_VERSION
     */
    private EnumSet<VersionFlags> versionFlags;

    public EnumSet<VersionFlags> getVersion() {
        return versionFlags;
    }

    public void setVersion(EnumSet<VersionFlags> value) {
        versionFlags = value;
    }

    /**
     * volume sector size, bytes
     */
    private short sectorSize;

    public int getSectorSize() {
        return sectorSize & 0xffff;
    }

    public void setSectorSize(short value) {
        sectorSize = value;
    }

    /**
     * inode size, bytes
     */
    private short inodeSize;

    public int getInodeSize() {
        return inodeSize & 0xffff;
    }

    public void setInodeSize(short value) {
        inodeSize = value;
    }

    /**
     * inodes per block
     */
    private short inodesPerBlock;

    public int getInodesPerBlock() {
        return inodesPerBlock & 0xffff;
    }

    public void setInodesPerBlock(short value) {
        inodesPerBlock = value;
    }

    /**
     * file system name
     */
    private String filesystemName;

    public String getFilesystemName() {
        return filesystemName;
    }

    public void setFilesystemName(String value) {
        filesystemName = value;
    }

    /**
     * log2 of {@link #blocksize}
     */
    private byte blocksizeLog2;

    public int getBlocksizeLog2() {
        return blocksizeLog2 & 0xff;
    }

    public void setBlocksizeLog2(byte value) {
        blocksizeLog2 = value;
    }

    /**
     * log2 of {@link #sectorSize}
     */
    private byte sectorSizeLog2;

    public int getSectorSizeLog2() {
        return sectorSizeLog2 & 0xff;
    }

    public void setSectorSizeLog2(byte value) {
        sectorSizeLog2 = value;
    }

    /**
     * log2 of {@link #inodeSize}
     */
    private byte inodeSizeLog2;

    public int getInodeSizeLog2() {
        return inodeSizeLog2 & 0xff;
    }

    public void setInodeSizeLog2(byte value) {
        inodeSizeLog2 = value;
    }

    /**
     * log2 of {@link #inodesPerBlock}
     */
    private byte inodesPerBlockLog2;

    public int getInodesPerBlockLog2() {
        return inodesPerBlockLog2 & 0xff;
    }

    public void setInodesPerBlockLog2(byte value) {
        inodesPerBlockLog2 = value;
    }

    /**
     * log2 of {@link #agBlocks} (rounded up)
     */
    private byte agBlocksLog2;

    public int getAgBlocksLog2() {
        return agBlocksLog2 & 0xff;
    }

    public void setAgBlocksLog2(byte value) {
        agBlocksLog2 = value;
    }

    /**
     * log2 of {@link #realtimeExtents}
     */
    private byte realtimeExtentsLog2;

    public int getRealtimeExtentsLog2() {
        return realtimeExtentsLog2 & 0xff;
    }

    public void setRealtimeExtentsLog2(byte value) {
        realtimeExtentsLog2 = value;
    }

    /**
     * mkfs is in progress, don't mount
     */
    private byte inProgress;

    public int getInProgress() {
        return inProgress & 0xff;
    }

    public void setInProgress(byte value) {
        inProgress = value;
    }

    /**
     * max % of fs for inode space
     */
    private byte inodesMaxPercent;

    public int getInodesMaxPercent() {
        return inodesMaxPercent & 0xff;
    }

    public void setInodesMaxPercent(byte value) {
        inodesMaxPercent = value;
    }

    // These fields must remain contiguous. If you really
    // want to change their layout, make sure you fix the
    // code in xfs_trans_apply_sb_deltas().

    /**
     * allocated inodes
     */
    private long allocatedInodes;

    public long getAllocatedInodes() {
        return allocatedInodes;
    }

    public void setAllocatedInodes(long value) {
        allocatedInodes = value;
    }

    /**
     * free inodes
     */
    private long freeInodes;

    public long getFreeInodes() {
        return freeInodes;
    }

    public void setFreeInodes(long value) {
        freeInodes = value;
    }

    /**
     * free data blocks
     */
    private long freeDataBlocks;

    public long getFreeDataBlocks() {
        return freeDataBlocks;
    }

    public void setFreeDataBlocks(long value) {
        freeDataBlocks = value;
    }

    /**
     * free realtime extents
     */
    private long freeRealtimeExtents;

    public long getFreeRealtimeExtents() {
        return freeRealtimeExtents;
    }

    public void setFreeRealtimeExtents(long value) {
        freeRealtimeExtents = value;
    }

    /**
     * user quota inode
     */
    private long userQuotaInode;

    public long getUserQuotaInode() {
        return userQuotaInode;
    }

    public void setUserQuotaInode(long value) {
        userQuotaInode = value;
    }

    /**
     * group quota inode
     */
    private long groupQuotaInode;

    public long getGroupQuotaInode() {
        return groupQuotaInode;
    }

    public void setGroupQuotaInode(long value) {
        groupQuotaInode = value;
    }

    /**
     * quota flags
     */
    private short quotaFlags;

    public short getQuotaFlags() {
        return quotaFlags;
    }

    public void setQuotaFlags(short value) {
        quotaFlags = value;
    }

    /**
     * misc. flags
     */
    private byte flags;

    public byte getFlags() {
        return flags;
    }

    public void setFlags(byte value) {
        flags = value;
    }

    /**
     * shared version number
     */
    private byte sharedVersionNumber;

    public int getSharedVersionNumber() {
        return sharedVersionNumber & 0xff;
    }

    public void setSharedVersionNumber(byte value) {
        sharedVersionNumber = value;
    }

    /**
     * inode chunk alignment, fsblocks
     */
    private int inodeChunkAlignment;

    public int getInodeChunkAlignment() {
        return inodeChunkAlignment;
    }

    public void setInodeChunkAlignment(int value) {
        inodeChunkAlignment = value;
    }

    /**
     * stripe or raid unit
     */
    private int unit;

    public int getUnit() {
        return unit;
    }

    public void setUnit(int value) {
        unit = value;
    }

    /**
     * stripe or raid width
     */
    private int width;

    public int getWidth() {
        return width;
    }

    public void setWidth(int value) {
        width = value;
    }

    /**
     * log2 of dir block size (fsbs)
     */
    private byte dirBlockLog2;

    public int getDirBlockLog2() {
        return dirBlockLog2 & 0xff;
    }

    public void setDirBlockLog2(byte value) {
        dirBlockLog2 = value;
    }

    /**
     * log2 of the log sector size
     */
    private byte logSectorSizeLog2;

    public int getLogSectorSizeLog2() {
        return logSectorSizeLog2 & 0xff;
    }

    public void setLogSectorSizeLog2(byte value) {
        logSectorSizeLog2 = value;
    }

    /**
     * sector size for the log, bytes
     */
    private short logSectorSize;

    public int getLogSectorSize() {
        return logSectorSize & 0xffff;
    }

    public void setLogSectorSize(short value) {
        logSectorSize = value;
    }

    /**
     * stripe unit size for the log
     */
    private int logUnitSize;

    public int getLogUnitSize() {
        return logUnitSize;
    }

    public void setLogUnitSize(int value) {
        logUnitSize = value;
    }

    /**
     * additional feature bits
     */
    private EnumSet<Version2Features> features2;

    public EnumSet<Version2Features> getFeatures2() {
        return features2;
    }

    public void setFeatures2(EnumSet<Version2Features> value) {
        features2 = value;
    }

    // bad features2 field as a result of failing to pad the sb structure to
    // 64 bits. Some machines will be using this field for features2 bits.
    // Easiest just to mark it bad and not use it for anything else.
    // This is not kept up to date in memory; it is always overwritten by
    // the value in sb_features2 when formatting the incore superblock to
    // the disk buffer.

    /**
     * bad features2 field as a result of failing to pad the sb structure to 64
     * bits. Some machines will be using this field for features2 bits. Easiest
     * just to mark it bad and not use it for anything else.
     *
     * This is not kept up to date in memory; it is always overwritten by the
     * value in sb_features2 when formatting the incore superblock to the disk
     * buffer.
     */
    private int badFeatures2;

    public int getBadFeatures2() {
        return badFeatures2;
    }

    public void setBadFeatures2(int value) {
        badFeatures2 = value;
    }

    // version 5 superblock fields start here

    /* feature masks */
    private int compatibleFeatures;

    public int getCompatibleFeatures() {
        return compatibleFeatures;
    }

    public void setCompatibleFeatures(int value) {
        compatibleFeatures = value;
    }

    private EnumSet<ReadOnlyCompatibleFeatures> readOnlyCompatibleFeatures = EnumSet.noneOf(ReadOnlyCompatibleFeatures.class);

    public EnumSet<ReadOnlyCompatibleFeatures> getReadOnlyCompatibleFeatures() {
        return readOnlyCompatibleFeatures;
    }

    public void setReadOnlyCompatibleFeatures(EnumSet<ReadOnlyCompatibleFeatures> value) {
        readOnlyCompatibleFeatures = value;
    }

    private EnumSet<IncompatibleFeatures> incompatibleFeatures = EnumSet.noneOf(IncompatibleFeatures.class);

    public EnumSet<IncompatibleFeatures> getIncompatibleFeatures() {
        return incompatibleFeatures;
    }

    public void setIncompatibleFeatures(EnumSet<IncompatibleFeatures> value) {
        incompatibleFeatures = value;
    }

    private int logIncompatibleFeatures;

    public int getLogIncompatibleFeatures() {
        return logIncompatibleFeatures;
    }

    public void setLogIncompatibleFeatures(int value) {
        logIncompatibleFeatures = value;
    }

    /**
     * superblock crc
     */
    private int crc;

    public int getCrc() {
        return crc;
    }

    public void setCrc(int value) {
        crc = value;
    }

    /**
     * sparse inode chunk alignment
     */
    private int sparseInodeAlignment;

    public int getSparseInodeAlignment() {
        return sparseInodeAlignment;
    }

    public void setSparseInodeAlignment(int value) {
        sparseInodeAlignment = value;
    }

    /**
     * project quota inode
     */
    private long projectQuotaInode;

    public long getProjectQuotaInode() {
        return projectQuotaInode;
    }

    public void setProjectQuotaInode(long value) {
        projectQuotaInode = value;
    }

    /**
     * last write sequence
     */
    private long lsn;

    public long getLsn() {
        return lsn;
    }

    public void setLsn(long value) {
        lsn = value;
    }

    /**
     * metadata file system unique id
     */
    private UUID metaUuid;

    public UUID getMetaUuid() {
        return metaUuid;
    }

    public void setMetaUuid(UUID value) {
        metaUuid = value;
    }

    /* must be padded to 64 bit alignment */
    private int relativeInodeMask;

    public int getRelativeInodeMask() {
        return relativeInodeMask;
    }

    public void setRelativeInodeMask(int value) {
        relativeInodeMask = value;
    }

    private int agInodeMask;

    public int getAgInodeMask() {
        return agInodeMask;
    }

    public void setAgInodeMask(int value) {
        agInodeMask = value;
    }

    private int dirBlockSize;

    public int getDirBlockSize() {
        return dirBlockSize;
    }

    public void setDirBlockSize(int value) {
        dirBlockSize = value;
    }

    public int size() {
        if (getSbVersion() >= 5) {
            return 264;
        }

        return 208;
    }

    public int getSbVersion() {
        return version;
    }

    public boolean getSbVersionHasMoreBits() {
        return versionFlags.contains(VersionFlags.Features2);
    }

    public boolean hasFType() {
        return getSbVersion() == 5 && incompatibleFeatures.contains(IncompatibleFeatures.FType) ||
               getSbVersionHasMoreBits() && features2.contains(Version2Features.FType);
    }

    public int readFrom(byte[] buffer, int offset) {
        magic = EndianUtilities.toUInt32BigEndian(buffer, offset);
        if (magic != XfsMagic)
            return size();

        blocksize = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x4);
        dataBlocks = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x8);
        realtimeBlocks = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x10);
        realtimeExtents = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x18);
        uniqueId = EndianUtilities.toGuidBigEndian(buffer, offset + 0x20);
        logstart = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x30);
        rootInode = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x38);
        realtimeBitmapInode = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x40);
        realtimeSummaryInode = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x48);
        realtimeExtentSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x50);
        agBlocks = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x54);
        agCount = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x58);
        realtimeBitmapBlocks = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x5C);
        logBlocks = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x60);
        short versionFlags = EndianUtilities.toUInt16BigEndian(buffer, offset + 0x64);
        version = versionFlags & VersionFlags.NumberFlag;
        this.versionFlags = VersionFlags.valueOf(versionFlags & ~VersionFlags.NumberFlag);
        sectorSize = EndianUtilities.toUInt16BigEndian(buffer, offset + 0x66);
        inodeSize = EndianUtilities.toUInt16BigEndian(buffer, offset + 0x68);
        inodesPerBlock = EndianUtilities.toUInt16BigEndian(buffer, offset + 0x6A);
        filesystemName = EndianUtilities.bytesToZString(buffer, offset + 0x6C, 12);
        blocksizeLog2 = buffer[offset + 0x78];
        sectorSizeLog2 = buffer[offset + 0x79];
        inodeSizeLog2 = buffer[offset + 0x7A];
        inodesPerBlockLog2 = buffer[offset + 0x7B];
        agBlocksLog2 = buffer[offset + 0x7C];
        realtimeExtentsLog2 = buffer[offset + 0x7D];
        inProgress = buffer[offset + 0x7E];
        inodesMaxPercent = buffer[offset + 0x7F];
        allocatedInodes = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x80);
        freeInodes = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x88);
        freeDataBlocks = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x90);
        freeRealtimeExtents = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x98);
        userQuotaInode = EndianUtilities.toUInt64BigEndian(buffer, offset + 0xA0);
        groupQuotaInode = EndianUtilities.toUInt64BigEndian(buffer, offset + 0xA8);
        quotaFlags = EndianUtilities.toUInt16BigEndian(buffer, offset + 0xB0);
        flags = buffer[offset + 0xB2];
        sharedVersionNumber = buffer[offset + 0xB3];
        inodeChunkAlignment = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xB4);
        unit = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xB8);
        width = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xBC);
        dirBlockLog2 = buffer[offset + 0xC0];
        logSectorSizeLog2 = buffer[offset + 0xC1];
        logSectorSize = EndianUtilities.toUInt16BigEndian(buffer, offset + 0xC2);
        logUnitSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xC4);
        features2 = Version2Features.valueOf(EndianUtilities.toUInt32BigEndian(buffer, offset + 0xC8));
        badFeatures2 = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xCC);

        if (version >= (short) VersionFlags.Version5) {
            compatibleFeatures = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xD0);
            readOnlyCompatibleFeatures = ReadOnlyCompatibleFeatures
                        .valueOf(EndianUtilities.toUInt32BigEndian(buffer, offset + 0xD4));
            incompatibleFeatures = IncompatibleFeatures.valueOf(EndianUtilities.toUInt32BigEndian(buffer, offset + 0xD8));
            logIncompatibleFeatures = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xDC);
            crc = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xE0);
            sparseInodeAlignment = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xE4);
            projectQuotaInode = EndianUtilities.toUInt64BigEndian(buffer, offset + 0xE8);
            lsn = EndianUtilities.toInt64BigEndian(buffer, offset + 0xF0);
            metaUuid = EndianUtilities.toGuidBigEndian(buffer, offset + 0xF8);
            if (Collections.disjoint(incompatibleFeatures, IncompatibleFeatures.Supported))
                throw new UnsupportedOperationException("XFS features not supported");
        }

        long agOffset = getAgBlocksLog2() + getInodesPerBlockLog2();
        relativeInodeMask = 0xffff_ffff >>> (32 - agOffset);
        agInodeMask = ~relativeInodeMask;

        dirBlockSize = blocksize << getDirBlockLog2();
        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    // xfs_btree_compute_maxlevels
    public int computeBtreeMaxlevels() {
        int len = agBlocks;
        int level;
        int[] limits = new int[] {
            xfs_rmapbt_maxrecs(false), xfs_rmapbt_maxrecs(true)
        };
        long maxblocks = (len + limits[0] - 1) / limits[0];
        for (level = 1; maxblocks > 1; level++)
            maxblocks = (maxblocks + limits[1] - 1) / limits[1];
        return level;
    }

    private int xfs_rmapbt_maxrecs(boolean leaf) {
        int blocklen = blocksize - 56;
        if (leaf)
            return blocklen / 24;

        return blocklen / (2 * 20 + 4);
    }
}
