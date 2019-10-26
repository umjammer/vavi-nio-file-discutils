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

package DiscUtils.Xfs;

import java.util.UUID;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class SuperBlock implements IByteArraySerializable {
    public static final int XfsMagic = 0x58465342;

    /**
     * magic number == XFS_SB_MAGIC
     */
    private int __Magic;

    public int getMagic() {
        return __Magic;
    }

    public void setMagic(int value) {
        __Magic = value;
    }

    /**
     * logical block size, bytes
     */
    private int __Blocksize;

    public int getBlocksize() {
        return __Blocksize;
    }

    public void setBlocksize(int value) {
        __Blocksize = value;
    }

    /**
     * number of data blocks
     */
    private long __DataBlocks;

    public long getDataBlocks() {
        return __DataBlocks;
    }

    public void setDataBlocks(long value) {
        __DataBlocks = value;
    }

    /**
     * number of realtime blocks
     */
    private long __RealtimeBlocks;

    public long getRealtimeBlocks() {
        return __RealtimeBlocks;
    }

    public void setRealtimeBlocks(long value) {
        __RealtimeBlocks = value;
    }

    /**
     * number of realtime extents
     */
    private long __RealtimeExtents;

    public long getRealtimeExtents() {
        return __RealtimeExtents;
    }

    public void setRealtimeExtents(long value) {
        __RealtimeExtents = value;
    }

    /**
     * user-visible file system unique id
     */
    private UUID __UniqueId;

    public UUID getUniqueId() {
        return __UniqueId;
    }

    public void setUniqueId(UUID value) {
        __UniqueId = value;
    }

    /**
     * starting block of log if internal
     */
    private long __Logstart;

    public long getLogstart() {
        return __Logstart;
    }

    public void setLogstart(long value) {
        __Logstart = value;
    }

    /**
     * root inode number
     */
    private long __RootInode;

    public long getRootInode() {
        return __RootInode;
    }

    public void setRootInode(long value) {
        __RootInode = value;
    }

    /**
     * bitmap inode for realtime extents
     */
    private long __RealtimeBitmapInode;

    public long getRealtimeBitmapInode() {
        return __RealtimeBitmapInode;
    }

    public void setRealtimeBitmapInode(long value) {
        __RealtimeBitmapInode = value;
    }

    /**
     * summary inode for rt bitmap
     */
    private long __RealtimeSummaryInode;

    public long getRealtimeSummaryInode() {
        return __RealtimeSummaryInode;
    }

    public void setRealtimeSummaryInode(long value) {
        __RealtimeSummaryInode = value;
    }

    /**
     * realtime extent size, blocks
     */
    private int __RealtimeExtentSize;

    public int getRealtimeExtentSize() {
        return __RealtimeExtentSize;
    }

    public void setRealtimeExtentSize(int value) {
        __RealtimeExtentSize = value;
    }

    /**
     * size of an allocation group
     */
    private int __AgBlocks;

    public int getAgBlocks() {
        return __AgBlocks;
    }

    public void setAgBlocks(int value) {
        __AgBlocks = value;
    }

    /**
     * number of allocation groups
     */
    private int __AgCount;

    public int getAgCount() {
        return __AgCount;
    }

    public void setAgCount(int value) {
        __AgCount = value;
    }

    /**
     * number of rt bitmap blocks
     */
    private int __RealtimeBitmapBlocks;

    public int getRealtimeBitmapBlocks() {
        return __RealtimeBitmapBlocks;
    }

    public void setRealtimeBitmapBlocks(int value) {
        __RealtimeBitmapBlocks = value;
    }

    /**
     * number of log blocks
     */
    private int __LogBlocks;

    public int getLogBlocks() {
        return __LogBlocks;
    }

    public void setLogBlocks(int value) {
        __LogBlocks = value;
    }

    /**
     * header version == XFS_SB_VERSION
     */
    private VersionFlags __Version = VersionFlags.None;

    public VersionFlags getVersion() {
        return __Version;
    }

    public void setVersion(VersionFlags value) {
        __Version = value;
    }

    /**
     * volume sector size, bytes
     */
    private short __SectorSize;

    public short getSectorSize() {
        return __SectorSize;
    }

    public void setSectorSize(short value) {
        __SectorSize = value;
    }

    /**
     * inode size, bytes
     */
    private short __InodeSize;

    public short getInodeSize() {
        return __InodeSize;
    }

    public void setInodeSize(short value) {
        __InodeSize = value;
    }

    /**
     * inodes per block
     */
    private short __InodesPerBlock;

    public short getInodesPerBlock() {
        return __InodesPerBlock;
    }

    public void setInodesPerBlock(short value) {
        __InodesPerBlock = value;
    }

    /**
     * file system name
     */
    private String __FilesystemName;

    public String getFilesystemName() {
        return __FilesystemName;
    }

    public void setFilesystemName(String value) {
        __FilesystemName = value;
    }

    /**
     * log2 of
     * {@link #Blocksize}
     */
    private byte __BlocksizeLog2;

    public byte getBlocksizeLog2() {
        return __BlocksizeLog2;
    }

    public void setBlocksizeLog2(byte value) {
        __BlocksizeLog2 = value;
    }

    /**
     * log2 of
     * {@link #SectorSize}
     */
    private byte __SectorSizeLog2;

    public byte getSectorSizeLog2() {
        return __SectorSizeLog2;
    }

    public void setSectorSizeLog2(byte value) {
        __SectorSizeLog2 = value;
    }

    /**
     * log2 of
     * {@link #InodeSize}
     */
    private byte __InodeSizeLog2;

    public byte getInodeSizeLog2() {
        return __InodeSizeLog2;
    }

    public void setInodeSizeLog2(byte value) {
        __InodeSizeLog2 = value;
    }

    /**
     * log2 of
     * {@link #InodesPerBlock}
     */
    private byte __InodesPerBlockLog2;

    public byte getInodesPerBlockLog2() {
        return __InodesPerBlockLog2;
    }

    public void setInodesPerBlockLog2(byte value) {
        __InodesPerBlockLog2 = value;
    }

    /**
     * log2 of
     * {@link #AgBlocks}
     * (rounded up)
     */
    private byte __AgBlocksLog2;

    public byte getAgBlocksLog2() {
        return __AgBlocksLog2;
    }

    public void setAgBlocksLog2(byte value) {
        __AgBlocksLog2 = value;
    }

    /**
     * log2 of
     * {@link #RealtimeExtents}
     */
    private byte __RealtimeExtentsLog2;

    public byte getRealtimeExtentsLog2() {
        return __RealtimeExtentsLog2;
    }

    public void setRealtimeExtentsLog2(byte value) {
        __RealtimeExtentsLog2 = value;
    }

    /**
     * mkfs is in progress, don't mount
     */
    private byte __InProgress;

    public byte getInProgress() {
        return __InProgress;
    }

    public void setInProgress(byte value) {
        __InProgress = value;
    }

    /**
     * max % of fs for inode space
     */
    private byte __InodesMaxPercent;

    public byte getInodesMaxPercent() {
        return __InodesMaxPercent;
    }

    public void setInodesMaxPercent(byte value) {
        __InodesMaxPercent = value;
    }

    /* These fields must remain contiguous. If you really
     * want to change their layout, make sure you fix the
     * code in xfs_trans_apply_sb_deltas(). */
    /**
     * allocated inodes
     */
    private long __AllocatedInodes;

    public long getAllocatedInodes() {
        return __AllocatedInodes;
    }

    public void setAllocatedInodes(long value) {
        __AllocatedInodes = value;
    }

    /**
     * free inodes
     */
    private long __FreeInodes;

    public long getFreeInodes() {
        return __FreeInodes;
    }

    public void setFreeInodes(long value) {
        __FreeInodes = value;
    }

    /**
     * free data blocks
     */
    private long __FreeDataBlocks;

    public long getFreeDataBlocks() {
        return __FreeDataBlocks;
    }

    public void setFreeDataBlocks(long value) {
        __FreeDataBlocks = value;
    }

    /**
     * free realtime extents
     */
    private long __FreeRealtimeExtents;

    public long getFreeRealtimeExtents() {
        return __FreeRealtimeExtents;
    }

    public void setFreeRealtimeExtents(long value) {
        __FreeRealtimeExtents = value;
    }

    /**
     * user quota inode
     */
    private long __UserQuotaInode;

    public long getUserQuotaInode() {
        return __UserQuotaInode;
    }

    public void setUserQuotaInode(long value) {
        __UserQuotaInode = value;
    }

    /**
     * group quota inode
     */
    private long __GroupQuotaInode;

    public long getGroupQuotaInode() {
        return __GroupQuotaInode;
    }

    public void setGroupQuotaInode(long value) {
        __GroupQuotaInode = value;
    }

    /**
     * quota flags
     */
    private short __QuotaFlags;

    public short getQuotaFlags() {
        return __QuotaFlags;
    }

    public void setQuotaFlags(short value) {
        __QuotaFlags = value;
    }

    /**
     * misc. flags
     */
    private byte __Flags;

    public byte getFlags() {
        return __Flags;
    }

    public void setFlags(byte value) {
        __Flags = value;
    }

    /**
     * shared version number
     */
    private byte __SharedVersionNumber;

    public byte getSharedVersionNumber() {
        return __SharedVersionNumber;
    }

    public void setSharedVersionNumber(byte value) {
        __SharedVersionNumber = value;
    }

    /**
     * inode chunk alignment, fsblocks
     */
    private int __InodeChunkAlignment;

    public int getInodeChunkAlignment() {
        return __InodeChunkAlignment;
    }

    public void setInodeChunkAlignment(int value) {
        __InodeChunkAlignment = value;
    }

    /**
     * stripe or raid unit
     */
    private int __Unit;

    public int getUnit() {
        return __Unit;
    }

    public void setUnit(int value) {
        __Unit = value;
    }

    /**
     * stripe or raid width
     */
    private int __Width;

    public int getWidth() {
        return __Width;
    }

    public void setWidth(int value) {
        __Width = value;
    }

    /**
     * log2 of dir block size (fsbs)
     */
    private byte __DirBlockLog2;

    public byte getDirBlockLog2() {
        return __DirBlockLog2;
    }

    public void setDirBlockLog2(byte value) {
        __DirBlockLog2 = value;
    }

    /**
     * log2 of the log sector size
     */
    private byte __LogSectorSizeLog2;

    public byte getLogSectorSizeLog2() {
        return __LogSectorSizeLog2;
    }

    public void setLogSectorSizeLog2(byte value) {
        __LogSectorSizeLog2 = value;
    }

    /**
     * sector size for the log, bytes
     */
    private short __LogSectorSize;

    public short getLogSectorSize() {
        return __LogSectorSize;
    }

    public void setLogSectorSize(short value) {
        __LogSectorSize = value;
    }

    /**
     * stripe unit size for the log
     */
    private int __LogUnitSize;

    public int getLogUnitSize() {
        return __LogUnitSize;
    }

    public void setLogUnitSize(int value) {
        __LogUnitSize = value;
    }

    /**
     * additional feature bits
     */
    private Version2Features __Features2 = Version2Features.Reserved1;

    public Version2Features getFeatures2() {
        return __Features2;
    }

    public void setFeatures2(Version2Features value) {
        __Features2 = value;
    }

    /* bad features2 field as a result of failing to pad the sb structure to
     * 64 bits. Some machines will be using this field for features2 bits.
     * Easiest just to mark it bad and not use it for anything else.
     * This is not kept up to date in memory; it is always overwritten by
     * the value in sb_features2 when formatting the incore superblock to
     * the disk buffer. */
    /**
     * bad features2 field as a result of failing to pad the sb structure to
     * 64 bits. Some machines will be using this field for features2 bits.
     * Easiest just to mark it bad and not use it for anything else.
     *
     * This is not kept up to date in memory; it is always overwritten by
     * the value in sb_features2 when formatting the incore superblock to
     * the disk buffer.
     */
    private int __BadFeatures2;

    public int getBadFeatures2() {
        return __BadFeatures2;
    }

    public void setBadFeatures2(int value) {
        __BadFeatures2 = value;
    }

    /* version 5 superblock fields start here */
    /* feature masks */
    private int __CompatibleFeatures;

    public int getCompatibleFeatures() {
        return __CompatibleFeatures;
    }

    public void setCompatibleFeatures(int value) {
        __CompatibleFeatures = value;
    }

    private ReadOnlyCompatibleFeatures __ReadOnlyCompatibleFeatures = ReadOnlyCompatibleFeatures.FINOBT;

    public ReadOnlyCompatibleFeatures getReadOnlyCompatibleFeatures() {
        return __ReadOnlyCompatibleFeatures;
    }

    public void setReadOnlyCompatibleFeatures(ReadOnlyCompatibleFeatures value) {
        __ReadOnlyCompatibleFeatures = value;
    }

    private IncompatibleFeatures __IncompatibleFeatures = IncompatibleFeatures.None;

    public IncompatibleFeatures getIncompatibleFeatures() {
        return __IncompatibleFeatures;
    }

    public void setIncompatibleFeatures(IncompatibleFeatures value) {
        __IncompatibleFeatures = value;
    }

    private int __LogIncompatibleFeatures;

    public int getLogIncompatibleFeatures() {
        return __LogIncompatibleFeatures;
    }

    public void setLogIncompatibleFeatures(int value) {
        __LogIncompatibleFeatures = value;
    }

    /**
     * superblock crc
     */
    private int __Crc;

    public int getCrc() {
        return __Crc;
    }

    public void setCrc(int value) {
        __Crc = value;
    }

    /**
     * sparse inode chunk alignment
     */
    private int __SparseInodeAlignment;

    public int getSparseInodeAlignment() {
        return __SparseInodeAlignment;
    }

    public void setSparseInodeAlignment(int value) {
        __SparseInodeAlignment = value;
    }

    /**
     * project quota inode
     */
    private long __ProjectQuotaInode;

    public long getProjectQuotaInode() {
        return __ProjectQuotaInode;
    }

    public void setProjectQuotaInode(long value) {
        __ProjectQuotaInode = value;
    }

    /**
     * last write sequence
     */
    private long __Lsn;

    public long getLsn() {
        return __Lsn;
    }

    public void setLsn(long value) {
        __Lsn = value;
    }

    /**
     * metadata file system unique id
     */
    private UUID __MetaUuid;

    public UUID getMetaUuid() {
        return __MetaUuid;
    }

    public void setMetaUuid(UUID value) {
        __MetaUuid = value;
    }

    /* must be padded to 64 bit alignment */
    private int __RelativeInodeMask;

    public int getRelativeInodeMask() {
        return __RelativeInodeMask;
    }

    public void setRelativeInodeMask(int value) {
        __RelativeInodeMask = value;
    }

    private int __AgInodeMask;

    public int getAgInodeMask() {
        return __AgInodeMask;
    }

    public void setAgInodeMask(int value) {
        __AgInodeMask = value;
    }

    private int __DirBlockSize;

    public int getDirBlockSize() {
        return __DirBlockSize;
    }

    public void setDirBlockSize(int value) {
        __DirBlockSize = value;
    }

    public int size() {
        if (getSbVersion() >= 5) {
            return 264;
        }

        return 208;
    }

    public short getSbVersion() {
        return (short) (getVersion().ordinal() & VersionFlags.NumberFlag.ordinal());
    }

    public boolean getSbVersionHasMoreBits() {
        return (getVersion().ordinal() & VersionFlags.Features2.ordinal()) == VersionFlags.Features2.ordinal();
    }

    public boolean getHasFType() {
        getIncompatibleFeatures();
        getIncompatibleFeatures();
        return getSbVersion() == 5 &&
               ((getIncompatibleFeatures().ordinal() & IncompatibleFeatures.FType.ordinal()) == IncompatibleFeatures.FType
                       .ordinal()) ||
               getSbVersionHasMoreBits() && ((getFeatures2().ordinal() &
                                              Version2Features.FType.ordinal()) == Version2Features.FType.ordinal());
    }

    public int readFrom(byte[] buffer, int offset) {
        setMagic(EndianUtilities.toUInt32BigEndian(buffer, offset));
        if (getMagic() != XfsMagic)
            return size();

        setBlocksize(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x4));
        setDataBlocks(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x8));
        setRealtimeBlocks(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x10));
        setRealtimeExtents(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x18));
        setUniqueId(EndianUtilities.toGuidBigEndian(buffer, offset + 0x20));
        setLogstart(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x30));
        setRootInode(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x38));
        setRealtimeBitmapInode(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x40));
        setRealtimeSummaryInode(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x48));
        setRealtimeExtentSize(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x50));
        setAgBlocks(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x54));
        setAgCount(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x58));
        setRealtimeBitmapBlocks(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x5C));
        setLogBlocks(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x60));
        setVersion(VersionFlags.valueOf(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x64)));
        setSectorSize(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x66));
        setInodeSize(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x68));
        setInodesPerBlock(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x6A));
        setFilesystemName(EndianUtilities.bytesToZString(buffer, offset + 0x6C, 12));
        setBlocksizeLog2(buffer[offset + 0x78]);
        setSectorSizeLog2(buffer[offset + 0x79]);
        setInodeSizeLog2(buffer[offset + 0x7A]);
        setInodesPerBlockLog2(buffer[offset + 0x7B]);
        setAgBlocksLog2(buffer[offset + 0x7C]);
        setRealtimeExtentsLog2(buffer[offset + 0x7D]);
        setInProgress(buffer[offset + 0x7E]);
        setInodesMaxPercent(buffer[offset + 0x7F]);
        setAllocatedInodes(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x80));
        setFreeInodes(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x88));
        setFreeDataBlocks(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x90));
        setFreeRealtimeExtents(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x98));
        setUserQuotaInode(EndianUtilities.toUInt64BigEndian(buffer, offset + 0xA0));
        setGroupQuotaInode(EndianUtilities.toUInt64BigEndian(buffer, offset + 0xA8));
        setQuotaFlags(EndianUtilities.toUInt16BigEndian(buffer, offset + 0xB0));
        setFlags(buffer[offset + 0xB2]);
        setSharedVersionNumber(buffer[offset + 0xB3]);
        setInodeChunkAlignment(EndianUtilities.toUInt32BigEndian(buffer, offset + 0xB4));
        setUnit(EndianUtilities.toUInt32BigEndian(buffer, offset + 0xB8));
        setWidth(EndianUtilities.toUInt32BigEndian(buffer, offset + 0xBC));
        setDirBlockLog2(buffer[offset + 0xC0]);
        setLogSectorSizeLog2(buffer[offset + 0xC1]);
        setLogSectorSize(EndianUtilities.toUInt16BigEndian(buffer, offset + 0xC2));
        setLogUnitSize(EndianUtilities.toUInt32BigEndian(buffer, offset + 0xC4));
        setFeatures2(Version2Features.valueOf(EndianUtilities.toUInt32BigEndian(buffer, offset + 0xC8)));
        setBadFeatures2(EndianUtilities.toUInt32BigEndian(buffer, offset + 0xCC));
        if (getSbVersion() >= (short) VersionFlags.Version5.ordinal()) {
            setCompatibleFeatures(EndianUtilities.toUInt32BigEndian(buffer, offset + 0xD0));
            setReadOnlyCompatibleFeatures(ReadOnlyCompatibleFeatures
                    .valueOf(EndianUtilities.toUInt32BigEndian(buffer, offset + 0xD4)));
            setIncompatibleFeatures(IncompatibleFeatures.valueOf(EndianUtilities.toUInt32BigEndian(buffer, offset + 0xD8)));
            setLogIncompatibleFeatures(EndianUtilities.toUInt32BigEndian(buffer, offset + 0xDC));
            setCrc(EndianUtilities.toUInt32BigEndian(buffer, offset + 0xE0));
            setSparseInodeAlignment(EndianUtilities.toUInt32BigEndian(buffer, offset + 0xE4));
            setProjectQuotaInode(EndianUtilities.toUInt64BigEndian(buffer, offset + 0xE8));
            setLsn(EndianUtilities.toInt64BigEndian(buffer, offset + 0xF0));
            setMetaUuid(EndianUtilities.toGuidBigEndian(buffer, offset + 0xF8));
            getIncompatibleFeatures();
            getIncompatibleFeatures();
            if ((getIncompatibleFeatures().ordinal() &
                 IncompatibleFeatures.Supported.ordinal()) != IncompatibleFeatures.Supported.ordinal())
                throw new UnsupportedOperationException("XFS Features not supported");
        }

        long agOffset = getAgBlocksLog2() + getInodesPerBlockLog2();
        setRelativeInodeMask(0xffffffff >>> (32 - agOffset));
        setAgInodeMask(~getRelativeInodeMask());
        setDirBlockSize(getBlocksize() << getDirBlockLog2());
        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public int xfs_btree_compute_maxlevels() {
        int len = getAgBlocks();
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
        int blocklen = getBlocksize() - 56;
        if (leaf)
            return blocklen / 24;

        return blocklen / (2 * 20 + 4);
    }
}
