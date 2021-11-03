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

package DiscUtils.Xfs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import DiscUtils.Core.UnixFileType;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.StreamBuffer;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Builder.BuilderExtent;
import DiscUtils.Streams.Builder.BuilderSparseStreamExtent;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Ownership;
import dotnet4j.io.MemoryStream;


public class Inode implements IByteArraySerializable {
    public Inode(long number, Context context) {
    	_number = number;
        SuperBlock sb = context.getSuperBlock();
        setRelativeInodeNumber((int) (number & sb.getRelativeInodeMask()));
        setAllocationGroup((int) ((number & sb.getAgInodeMask()) >>> (sb.getAgBlocksLog2() + sb.getInodesPerBlockLog2())));
        setAgBlock((int) ((number >>> context.getSuperBlock().getInodesPerBlockLog2()) &
                          xFS_INO_MASK(context.getSuperBlock().getAgBlocksLog2())));
        setBlockOffset((int) (number & xFS_INO_MASK(sb.getInodesPerBlockLog2())));
    }

    private static int xFS_INO_MASK(int k) {
        return (1 << k) - 1;
    }

    public Inode(int allocationGroup, int relInode) {
        setAllocationGroup(allocationGroup);
        setRelativeInodeNumber(relInode);
    }

    private int _allocationGroup;

    public int getAllocationGroup() {
        return _allocationGroup;
    }

    public void setAllocationGroup(int value) {
        _allocationGroup = value;
    }

    private int _relativeInodeNumber;

    public int getRelativeInodeNumber() {
        return _relativeInodeNumber;
    }

    public void setRelativeInodeNumber(int value) {
        _relativeInodeNumber = value;
    }

    private int _agBlock;

    public int getAgBlock() {
        return _agBlock;
    }

    public void setAgBlock(int value) {
        _agBlock = value;
    }

    private int _blockOffset;

    public int getBlockOffset() {
        return _blockOffset;
    }

    public void setBlockOffset(int value) {
        _blockOffset = value;
    }

    private long _number;

	public long getNumber() {
    	return _number;
	}

    public static final short InodeMagic = 0x494e;

    /**
     * The inode signature where these two bytes are 0x494e, or "IN" in ASCII.
     */
    private short _magic;

    public short getMagic() {
        return _magic;
    }

    public void setMagic(short value) {
        _magic = value;
    }

    /**
     * Specifies the mode access bits and type of file using the standard S_Ixxx
     * values defined in stat.h.
     */
    private short _mode;

    public int getMode() {
        return _mode & 0xffff;
    }

    public void setMode(short value) {
        _mode = value;
    }

    /**
     * Specifies the inode version which currently can only be 1 or 2. The inode
     * version specifies the usage of the di_onlink, di_nlink and di_projid
     * values in the inode core.Initially, inodes are created as v1 but can be
     * converted on the fly to v2 when required.
     */
    private byte _version;

    public byte getVersion() {
        return _version;
    }

    public void setVersion(byte value) {
        _version = value;
    }

    /**
     * Specifies the format of the data fork in conjunction with the di_mode
     * type. This can be one of several values. For directories and links, it
     * can be "local" where all metadata associated with the file is within the
     * inode, "extents" where the inode contains an array of extents to other
     * filesystem blocks which contain the associated metadata or data or
     * "btree" where the inode contains a B+tree root node which points to
     * filesystem blocks containing the metadata or data. Migration between the
     * formats depends on the amount of metadata associated with the inode.
     * "dev" is used for character and block devices while "uuid" is currently
     * not used.
     */
    private InodeFormat _format = InodeFormat.Dev;

    public InodeFormat getFormat() {
        return _format;
    }

    public void setFormat(InodeFormat value) {
        _format = value;
    }

    /**
     * In v1 inodes, this specifies the number of links to the inode from
     * directories. When the number exceeds 65535, the inode is converted to v2
     * and the link count is stored in di_nlink.
     */
    private short _onlink;

    public int getOnlink() {
        return _onlink & 0xffff;
    }

    public void setOnlink(short value) {
        _onlink = value;
    }

    /**
     * Specifies the owner's UID of the inode.
     */
    private int _userId;

    public int getUserId() {
        return _userId;
    }

    public void setUserId(int value) {
        _userId = value;
    }

    /**
     * Specifies the owner's GID of the inode.
     */
    private int _groupId;

    public int getGroupId() {
        return _groupId;
    }

    public void setGroupId(int value) {
        _groupId = value;
    }

    /**
     * Specifies the number of links to the inode from directories. This is
     * maintained for both inode versions for current versions of XFS.Old
     * versions of XFS did not support v2 inodes, and therefore this value was
     * never updated and was classed as reserved space (part of
     * {@link #_padding} ).
     */
    private int _nlink;

    public int getNlink() {
        return _nlink;
    }

    public void setNlink(int value) {
        _nlink = value;
    }

    /**
     * Specifies the owner's project ID in v2 inodes. An inode is converted to
     * v2 if the project ID is set. This value must be zero for v1 inodes.
     */
    private short _projectId;

    public short getProjectId() {
        return _projectId;
    }

    public void setProjectId(short value) {
        _projectId = value;
    }

    /**
     * Reserved, must be zero.
     */
    private byte[] _padding;

    public byte[] getPadding() {
        return _padding;
    }

    public void setPadding(byte[] value) {
        _padding = value;
    }

    /**
     * Incremented on flush.
     */
    private short _flushIterator;

    public int getFlushIterator() {
        return _flushIterator & 0xffff;
    }

    public void setFlushIterator(short value) {
        _flushIterator = value;
    }

    /**
     * Specifies the last access time of the files using UNIX time conventions
     * the following structure. This value maybe undefined if the filesystem is
     * mounted with the "noatime" option.
     */
    private long _accessTime;

    public long getAccessTime() {
        return _accessTime;
    }

    public void setAccessTime(long value) {
        _accessTime = value;
    }

    /**
     * Specifies the last time the file was modified.
     */
    private long _modificationTime;

    public long getModificationTime() {
        return _modificationTime;
    }

    public void setModificationTime(long value) {
        _modificationTime = value;
    }

    /**
     * Specifies when the inode's status was last changed.
     */
    private long _creationTime;

    public long getCreationTime() {
        return _creationTime;
    }

    public void setCreationTime(long value) {
        _creationTime = value;
    }

    /**
     * Specifies the EOF of the inode in bytes. This can be larger or smaller
     * than the extent space (therefore actual disk space) used for the
     * inode.For regular files, this is the filesize in bytes, directories, the
     * space taken by directory entries and for links, the length of the
     * symlink.
     */
    private long _length;

    public long getLength() {
        return _length;
    }

    public void setLength(long value) {
        _length = value;
    }

    /**
     * Specifies the number of filesystem blocks used to store the inode's data
     * including relevant metadata like B+trees.
     *
     * This does not include blocks used for extended attributes.
     */
    private long _blockCount;

    public long getBlockCount() {
        return _blockCount;
    }

    public void setBlockCount(long value) {
        _blockCount = value;
    }

    /**
     * Specifies the extent size for filesystems with real-time devices and an
     * extent size hint for standard filesystems. For normal filesystems, and
     * with directories, the XFS_DIFLAG_EXTSZINHERIT flag must be set in
     * di_flags if this field is used.Inodes created in these directories will
     * inherit the di_extsize value and have XFS_DIFLAG_EXTSIZE set in their
     * di_flags. When a file is written to beyond allocated space, XFS will
     * attempt to allocate additional disk space based on this value.
     */
    private int _extentSize;

    public int getExtentSize() {
        return _extentSize;
    }

    public void setExtentSize(int value) {
        _extentSize = value;
    }

    /**
     * Specifies the number of data extents associated with this inode.
     */
    private int _extents;

    public int getExtents() {
        return _extents;
    }

    public void setExtents(int value) {
        _extents = value;
    }

    /**
     * Specifies the number of extended attribute extents associated with this
     * inode.
     */
    private short _attributeExtents;

    public int getAttributeExtents() {
        return _attributeExtents & 0xffff;
    }

    public void setAttributeExtents(short value) {
        _attributeExtents = value;
    }

    /**
     * Specifies the offset into the inode's literal area where the extended
     * attribute fork starts. This is an 8-bit value that is multiplied by 8 to
     * determine the actual offset in bytes(ie.attribute data is 64-bit
     * aligned). This also limits the maximum size of the inode to 2048
     * bytes. This value is initially zero until an extended attribute is
     * created.When in attribute is added, the nature of di_forkoff depends on
     * the XFS_SB_VERSION2_ATTR2BIT flag in the superblock.
     */
    private byte _forkoff;

    public int getForkoff() {
        return _forkoff & 0xff;
    }

    public void setForkoff(byte value) {
        _forkoff = value;
    }

    /**
     * Specifies the format of the attribute fork. This uses the same values as
     * di_format, but restricted to "local", "extents" and "btree" formats for
     * extended attribute data.
     */
    private byte _attributeFormat;

    public byte getAttributeFormat() {
        return _attributeFormat;
    }

    public void setAttributeFormat(byte value) {
        _attributeFormat = value;
    }

    /**
     * DMAPI event mask.
     */
    private int _dmApiEventMask;

    public int getDmApiEventMask() {
        return _dmApiEventMask;
    }

    public void setDmApiEventMask(int value) {
        _dmApiEventMask = value;
    }

    /**
     * DMAPI state.
     */
    private short _dmState;

    public short getDmState() {
        return _dmState;
    }

    public void setDmState(short value) {
        _dmState = value;
    }

    /**
     * Specifies flags associated with the inode.
     */
    private EnumSet<InodeFlags> _flags;

    public EnumSet<InodeFlags> getFlags() {
        return _flags;
    }

    public void setFlags(EnumSet<InodeFlags> value) {
        _flags = value;
    }

    /**
     * A generation number used for inode identification. This is used by tools
     * that do inode scanning such as backup tools and xfsdump. An inode's
     * generation number can change by unlinking and creating a new file that
     * reuses the inode.
     */
    private int _generation;

    public int getGeneration() {
        return _generation;
    }

    public void setGeneration(int value) {
        _generation = value;
    }

    public UnixFileType getFileType() {
        return UnixFileType.values()[(getMode() >>> 12) & 0xF];
    }

    private byte[] _dataFork;

    public byte[] getDataFork() {
        return _dataFork;
    }

    public void setDataFork(byte[] value) {
        _dataFork = value;
    }

    public int size() {
        return 96;
    }

    public int readFrom(byte[] buffer, int offset) {
        setMagic(EndianUtilities.toUInt16BigEndian(buffer, offset));
        setMode(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x2));
        setVersion(buffer[offset + 0x4]);
        setFormat(InodeFormat.values()[buffer[offset + 0x5]]);
        setOnlink(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x6));
        setUserId(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x8));
        setGroupId(EndianUtilities.toUInt32BigEndian(buffer, offset + 0xC));
        setNlink(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x10));
        setProjectId(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x14));
        setPadding(EndianUtilities.toByteArray(buffer, offset + 0x16, 8));
        setFlushIterator(EndianUtilities.toUInt16BigEndian(buffer, 0x1E));
        setAccessTime(readTimestamp(buffer, offset + 0x20));
        setModificationTime(readTimestamp(buffer, offset + 0x28));
        setCreationTime(readTimestamp(buffer, offset + 0x30));
        setLength(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x38));
        setBlockCount(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x40));
        setExtentSize(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x48));
        setExtents(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x4C));
        setAttributeExtents(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x50));
        setForkoff(buffer[offset + 0x52]);
        setAttributeFormat(buffer[offset + 0x53]);
        setDmApiEventMask(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x54));
        setDmState(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x58));
        setFlags(InodeFlags.valueOf(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x5A)));
        setGeneration(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x5C));
        int dfOffset = getVersion() < 3 ? 0x64 : 0xb0;
        int dfLength;
        if (getForkoff() == 0) {
            dfLength = buffer.length - offset - dfOffset;
        } else {
            dfLength = (getForkoff() * 8);
        }
        setDataFork(EndianUtilities.toByteArray(buffer, offset + dfOffset, dfLength));
        return size();
    }

    private long readTimestamp(byte[] buffer, int offset) {
        long seconds = EndianUtilities.toUInt32BigEndian(buffer, offset);
        long nanoSeconds = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x4);
        return Instant.ofEpochSecond(seconds, nanoSeconds).toEpochMilli();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public List<Extent> getExtents_() {
        List<Extent> result = new ArrayList<>(getExtents());
        int offset = 0;
        for (int i = 0; i < getExtents(); i++) {
            Extent extent = new Extent();
            offset += extent.readFrom(getDataFork(), offset);
            result.add(extent);
        }
        return result;
    }

    public IBuffer getContentBuffer(Context context) {
        if (getFormat() == InodeFormat.Local) {
            return new StreamBuffer(new MemoryStream(getDataFork()), Ownership.Dispose);
        } else if (getFormat() == InodeFormat.Extents) {
            List<Extent> extents = getExtents_();
            return bufferFromExtentList(context, extents);
        } else if (getFormat() == InodeFormat.Btree) {
            BTreeExtentRoot tree = new BTreeExtentRoot();
            tree.readFrom(getDataFork(), 0);
            tree.loadBtree(context);
            List<Extent> extents = tree.getExtents();
            return bufferFromExtentList(context, extents);
        }

        throw new UnsupportedOperationException();
    }

    public IBuffer bufferFromExtentList(Context context, List<Extent> extents) {
        List<BuilderExtent> builderExtents = new ArrayList<>(extents.size());
        for (Extent extent : extents) {
            long blockOffset = extent.getOffset(context);
            SubStream substream = new SubStream(context.getRawStream(),
                                                blockOffset,
                                                (long) extent.getBlockCount() * context.getSuperBlock().getBlocksize());
            builderExtents.add(new BuilderSparseStreamExtent(extent.getStartOffset() * context.getSuperBlock().getBlocksize(),
                                                             substream));
        }
        return new StreamBuffer(new ExtentStream(this.getLength(), builderExtents), Ownership.Dispose);
    }

    @Override 
    public String toString() {
        return "inode " + _number;
    }
}
