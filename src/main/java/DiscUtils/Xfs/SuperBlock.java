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

import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class SuperBlock implements IByteArraySerializable {
    public static final int XfsMagic = 0x58465342;

    /**
     * magic number == XFS_SB_MAGIC
     */
    private int _magic;

    public int getMagic() {
        return _magic;
    }

    public void setMagic(int value) {
        _magic = value;
    }

    /**
     * logical block size, bytes
     */
    private int _blocksize;

    public int getBlocksize() {
        return _blocksize;
    }

    public void setBlocksize(int value) {
        _blocksize = value;
    }

    /**
     * number of data blocks
     */
    private long _dataBlocks;

    public long getDataBlocks() {
        return _dataBlocks;
    }

    public void setDataBlocks(long value) {
        _dataBlocks = value;
    }

    /**
     * number of realtime blocks
     */
    private long _realtimeBlocks;

    public long getRealtimeBlocks() {
        return _realtimeBlocks;
    }

    public void setRealtimeBlocks(long value) {
        _realtimeBlocks = value;
    }

    /**
     * number of realtime extents
     */
    private long _realtimeExtents;

    public long getRealtimeExtents() {
        return _realtimeExtents;
    }

    public void setRealtimeExtents(long value) {
        _realtimeExtents = value;
    }

    /**
     * user-visible file system unique id
     */
    private UUID _uniqueId;

    public UUID getUniqueId() {
        return _uniqueId;
    }

    public void setUniqueId(UUID value) {
        _uniqueId = value;
    }

    /**
     * starting block of log if internal
     */
    private long _logstart;

    public long getLogstart() {
        return _logstart;
    }

    public void setLogstart(long value) {
        _logstart = value;
    }

    /**
     * root inode number
     */
    private long _rootInode;

    public long getRootInode() {
        return _rootInode;
    }

    public void setRootInode(long value) {
        _rootInode = value;
    }

    /**
     * bitmap inode for realtime extents
     */
    private long _realtimeBitmapInode;

    public long getRealtimeBitmapInode() {
        return _realtimeBitmapInode;
    }

    public void setRealtimeBitmapInode(long value) {
        _realtimeBitmapInode = value;
    }

    /**
     * summary inode for rt bitmap
     */
    private long _realtimeSummaryInode;

    public long getRealtimeSummaryInode() {
        return _realtimeSummaryInode;
    }

    public void setRealtimeSummaryInode(long value) {
        _realtimeSummaryInode = value;
    }

    /**
     * realtime extent size, blocks
     */
    private int _realtimeExtentSize;

    public int getRealtimeExtentSize() {
        return _realtimeExtentSize;
    }

    public void setRealtimeExtentSize(int value) {
        _realtimeExtentSize = value;
    }

    /**
     * size of an allocation group
     */
    private int _agBlocks;

    public int getAgBlocks() {
        return _agBlocks;
    }

    public void setAgBlocks(int value) {
        _agBlocks = value;
    }

    /**
     * number of allocation groups
     */
    private int _agCount;

    public int getAgCount() {
        return _agCount;
    }

    public void setAgCount(int value) {
        _agCount = value;
    }

    /**
     * number of rt bitmap blocks
     */
    private int _realtimeBitmapBlocks;

    public int getRealtimeBitmapBlocks() {
        return _realtimeBitmapBlocks;
    }

    public void setRealtimeBitmapBlocks(int value) {
        _realtimeBitmapBlocks = value;
    }

    /**
     * number of log blocks
     */
    private int _logBlocks;

    public int getLogBlocks() {
        return _logBlocks;
    }

    public void setLogBlocks(int value) {
        _logBlocks = value;
    }

    private int _version;

    /**
     * header version == XFS_SB_VERSION
     */
    private EnumSet<VersionFlags> _versionFlags;

    public EnumSet<VersionFlags> getVersion() {
        return _versionFlags;
    }

    public void setVersion(EnumSet<VersionFlags> value) {
        _versionFlags = value;
    }

    /**
     * volume sector size, bytes
     */
    private short _sectorSize;

    public int getSectorSize() {
        return _sectorSize & 0xffff;
    }

    public void setSectorSize(short value) {
        _sectorSize = value;
    }

    /**
     * inode size, bytes
     */
    private short _inodeSize;

    public int getInodeSize() {
        return _inodeSize & 0xffff;
    }

    public void setInodeSize(short value) {
        _inodeSize = value;
    }

    /**
     * inodes per block
     */
    private short _inodesPerBlock;

    public int getInodesPerBlock() {
        return _inodesPerBlock & 0xffff;
    }

    public void setInodesPerBlock(short value) {
        _inodesPerBlock = value;
    }

    /**
     * file system name
     */
    private String _filesystemName;

    public String getFilesystemName() {
        return _filesystemName;
    }

    public void setFilesystemName(String value) {
        _filesystemName = value;
    }

    /**
     * log2 of {@link #_blocksize}
     */
    private byte _blocksizeLog2;

    public int getBlocksizeLog2() {
        return _blocksizeLog2 & 0xff;
    }

    public void setBlocksizeLog2(byte value) {
        _blocksizeLog2 = value;
    }

    /**
     * log2 of {@link #_sectorSize}
     */
    private byte _sectorSizeLog2;

    public int getSectorSizeLog2() {
        return _sectorSizeLog2 & 0xff;
    }

    public void setSectorSizeLog2(byte value) {
        _sectorSizeLog2 = value;
    }

    /**
     * log2 of {@link #_inodeSize}
     */
    private byte _inodeSizeLog2;

    public int getInodeSizeLog2() {
        return _inodeSizeLog2 & 0xff;
    }

    public void setInodeSizeLog2(byte value) {
        _inodeSizeLog2 = value;
    }

    /**
     * log2 of {@link #_inodesPerBlock}
     */
    private byte _inodesPerBlockLog2;

    public int getInodesPerBlockLog2() {
        return _inodesPerBlockLog2 & 0xff;
    }

    public void setInodesPerBlockLog2(byte value) {
        _inodesPerBlockLog2 = value;
    }

    /**
     * log2 of {@link #_agBlocks} (rounded up)
     */
    private byte _agBlocksLog2;

    public int getAgBlocksLog2() {
        return _agBlocksLog2 & 0xff;
    }

    public void setAgBlocksLog2(byte value) {
        _agBlocksLog2 = value;
    }

    /**
     * log2 of {@link #_realtimeExtents}
     */
    private byte _realtimeExtentsLog2;

    public int getRealtimeExtentsLog2() {
        return _realtimeExtentsLog2 & 0xff;
    }

    public void setRealtimeExtentsLog2(byte value) {
        _realtimeExtentsLog2 = value;
    }

    /**
     * mkfs is in progress, don't mount
     */
    private byte _inProgress;

    public int getInProgress() {
        return _inProgress & 0xff;
    }

    public void setInProgress(byte value) {
        _inProgress = value;
    }

    /**
     * max % of fs for inode space
     */
    private byte _inodesMaxPercent;

    public int getInodesMaxPercent() {
        return _inodesMaxPercent & 0xff;
    }

    public void setInodesMaxPercent(byte value) {
        _inodesMaxPercent = value;
    }

    // These fields must remain contiguous. If you really
    // want to change their layout, make sure you fix the
    // code in xfs_trans_apply_sb_deltas().

    /**
     * allocated inodes
     */
    private long _allocatedInodes;

    public long getAllocatedInodes() {
        return _allocatedInodes;
    }

    public void setAllocatedInodes(long value) {
        _allocatedInodes = value;
    }

    /**
     * free inodes
     */
    private long _freeInodes;

    public long getFreeInodes() {
        return _freeInodes;
    }

    public void setFreeInodes(long value) {
        _freeInodes = value;
    }

    /**
     * free data blocks
     */
    private long _freeDataBlocks;

    public long getFreeDataBlocks() {
        return _freeDataBlocks;
    }

    public void setFreeDataBlocks(long value) {
        _freeDataBlocks = value;
    }

    /**
     * free realtime extents
     */
    private long _freeRealtimeExtents;

    public long getFreeRealtimeExtents() {
        return _freeRealtimeExtents;
    }

    public void setFreeRealtimeExtents(long value) {
        _freeRealtimeExtents = value;
    }

    /**
     * user quota inode
     */
    private long _userQuotaInode;

    public long getUserQuotaInode() {
        return _userQuotaInode;
    }

    public void setUserQuotaInode(long value) {
        _userQuotaInode = value;
    }

    /**
     * group quota inode
     */
    private long _groupQuotaInode;

    public long getGroupQuotaInode() {
        return _groupQuotaInode;
    }

    public void setGroupQuotaInode(long value) {
        _groupQuotaInode = value;
    }

    /**
     * quota flags
     */
    private short _quotaFlags;

    public short getQuotaFlags() {
        return _quotaFlags;
    }

    public void setQuotaFlags(short value) {
        _quotaFlags = value;
    }

    /**
     * misc. flags
     */
    private byte _flags;

    public byte getFlags() {
        return _flags;
    }

    public void setFlags(byte value) {
        _flags = value;
    }

    /**
     * shared version number
     */
    private byte _sharedVersionNumber;

    public int getSharedVersionNumber() {
        return _sharedVersionNumber & 0xff;
    }

    public void setSharedVersionNumber(byte value) {
        _sharedVersionNumber = value;
    }

    /**
     * inode chunk alignment, fsblocks
     */
    private int _inodeChunkAlignment;

    public int getInodeChunkAlignment() {
        return _inodeChunkAlignment;
    }

    public void setInodeChunkAlignment(int value) {
        _inodeChunkAlignment = value;
    }

    /**
     * stripe or raid unit
     */
    private int _unit;

    public int getUnit() {
        return _unit;
    }

    public void setUnit(int value) {
        _unit = value;
    }

    /**
     * stripe or raid width
     */
    private int _width;

    public int getWidth() {
        return _width;
    }

    public void setWidth(int value) {
        _width = value;
    }

    /**
     * log2 of dir block size (fsbs)
     */
    private byte _dirBlockLog2;

    public int getDirBlockLog2() {
        return _dirBlockLog2 & 0xff;
    }

    public void setDirBlockLog2(byte value) {
        _dirBlockLog2 = value;
    }

    /**
     * log2 of the log sector size
     */
    private byte _logSectorSizeLog2;

    public int getLogSectorSizeLog2() {
        return _logSectorSizeLog2 & 0xff;
    }

    public void setLogSectorSizeLog2(byte value) {
        _logSectorSizeLog2 = value;
    }

    /**
     * sector size for the log, bytes
     */
    private short _logSectorSize;

    public int getLogSectorSize() {
        return _logSectorSize & 0xffff;
    }

    public void setLogSectorSize(short value) {
        _logSectorSize = value;
    }

    /**
     * stripe unit size for the log
     */
    private int _logUnitSize;

    public int getLogUnitSize() {
        return _logUnitSize;
    }

    public void setLogUnitSize(int value) {
        _logUnitSize = value;
    }

    /**
     * additional feature bits
     */
    private EnumSet<Version2Features> _features2;

    public EnumSet<Version2Features> getFeatures2() {
        return _features2;
    }

    public void setFeatures2(EnumSet<Version2Features> value) {
        _features2 = value;
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
    private int _badFeatures2;

    public int getBadFeatures2() {
        return _badFeatures2;
    }

    public void setBadFeatures2(int value) {
        _badFeatures2 = value;
    }

    // version 5 superblock fields start here

    /* feature masks */
    private int _compatibleFeatures;

    public int getCompatibleFeatures() {
        return _compatibleFeatures;
    }

    public void setCompatibleFeatures(int value) {
        _compatibleFeatures = value;
    }

    private EnumSet<ReadOnlyCompatibleFeatures> _readOnlyCompatibleFeatures = EnumSet.noneOf(ReadOnlyCompatibleFeatures.class);

    public EnumSet<ReadOnlyCompatibleFeatures> getReadOnlyCompatibleFeatures() {
        return _readOnlyCompatibleFeatures;
    }

    public void setReadOnlyCompatibleFeatures(EnumSet<ReadOnlyCompatibleFeatures> value) {
        _readOnlyCompatibleFeatures = value;
    }

    private EnumSet<IncompatibleFeatures> _incompatibleFeatures = EnumSet.noneOf(IncompatibleFeatures.class);

    public EnumSet<IncompatibleFeatures> getIncompatibleFeatures() {
        return _incompatibleFeatures;
    }

    public void setIncompatibleFeatures(EnumSet<IncompatibleFeatures> value) {
        _incompatibleFeatures = value;
    }

    private int _logIncompatibleFeatures;

    public int getLogIncompatibleFeatures() {
        return _logIncompatibleFeatures;
    }

    public void setLogIncompatibleFeatures(int value) {
        _logIncompatibleFeatures = value;
    }

    /**
     * superblock crc
     */
    private int _crc;

    public int getCrc() {
        return _crc;
    }

    public void setCrc(int value) {
        _crc = value;
    }

    /**
     * sparse inode chunk alignment
     */
    private int _sparseInodeAlignment;

    public int getSparseInodeAlignment() {
        return _sparseInodeAlignment;
    }

    public void setSparseInodeAlignment(int value) {
        _sparseInodeAlignment = value;
    }

    /**
     * project quota inode
     */
    private long _projectQuotaInode;

    public long getProjectQuotaInode() {
        return _projectQuotaInode;
    }

    public void setProjectQuotaInode(long value) {
        _projectQuotaInode = value;
    }

    /**
     * last write sequence
     */
    private long _lsn;

    public long getLsn() {
        return _lsn;
    }

    public void setLsn(long value) {
        _lsn = value;
    }

    /**
     * metadata file system unique id
     */
    private UUID _metaUuid;

    public UUID getMetaUuid() {
        return _metaUuid;
    }

    public void setMetaUuid(UUID value) {
        _metaUuid = value;
    }

    /* must be padded to 64 bit alignment */
    private int _relativeInodeMask;

    public int getRelativeInodeMask() {
        return _relativeInodeMask;
    }

    public void setRelativeInodeMask(int value) {
        _relativeInodeMask = value;
    }

    private int _agInodeMask;

    public int getAgInodeMask() {
        return _agInodeMask;
    }

    public void setAgInodeMask(int value) {
        _agInodeMask = value;
    }

    private int _dirBlockSize;

    public int getDirBlockSize() {
        return _dirBlockSize;
    }

    public void setDirBlockSize(int value) {
        _dirBlockSize = value;
    }

    public int size() {
        if (getSbVersion() >= 5) {
            return 264;
        }

        return 208;
    }

    public int getSbVersion() {
        return _version;
    }

    public boolean getSbVersionHasMoreBits() {
        return _versionFlags.contains(VersionFlags.Features2);
    }

    public boolean hasFType() {
        return getSbVersion() == 5 && _incompatibleFeatures.contains(IncompatibleFeatures.FType) ||
               getSbVersionHasMoreBits() && _features2.contains(Version2Features.FType);
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
        short versionFlags = EndianUtilities.toUInt16BigEndian(buffer, offset + 0x64);
        _version = versionFlags & VersionFlags.NumberFlag;
        setVersion(VersionFlags.valueOf(versionFlags & ~VersionFlags.NumberFlag));
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

        if (getSbVersion() >= (short) VersionFlags.Version5) {
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
            if (Collections.disjoint(_incompatibleFeatures, IncompatibleFeatures.Supported))
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

    // xfs_btree_compute_maxlevels
    public int computeBtreeMaxlevels() {
        int len = _agBlocks;
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
        int blocklen = _blocksize - 56;
        if (leaf)
            return blocklen / 24;

        return blocklen / (2 * 20 + 4);
    }
}
