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

    private int __AllocationGroup;

    public int getAllocationGroup() {
        return __AllocationGroup;
    }

    public void setAllocationGroup(int value) {
        __AllocationGroup = value;
    }

    private int __RelativeInodeNumber;

    public int getRelativeInodeNumber() {
        return __RelativeInodeNumber;
    }

    public void setRelativeInodeNumber(int value) {
        __RelativeInodeNumber = value;
    }

    private int __AgBlock;

    public int getAgBlock() {
        return __AgBlock;
    }

    public void setAgBlock(int value) {
        __AgBlock = value;
    }

    private int __BlockOffset;

    public int getBlockOffset() {
        return __BlockOffset;
    }

    public void setBlockOffset(int value) {
        __BlockOffset = value;
    }

    public static final short InodeMagic = 0x494e;

    /**
     * The inode signature where these two bytes are 0x494e, or "IN" in ASCII.
     */
    private short __Magic;

    public short getMagic() {
        return __Magic;
    }

    public void setMagic(short value) {
        __Magic = value;
    }

    /**
     * Specifies the mode access bits and type of file using the standard S_Ixxx
     * values defined in stat.h.
     */
    private short __Mode;

    public short getMode() {
        return __Mode;
    }

    public void setMode(short value) {
        __Mode = value;
    }

    /**
     * Specifies the inode version which currently can only be 1 or 2. The inode
     * version specifies the
     * usage of the di_onlink, di_nlink and di_projid values in the inode
     * core.Initially, inodes
     * are created as v1 but can be converted on the fly to v2 when required.
     */
    private byte __Version;

    public byte getVersion() {
        return __Version;
    }

    public void setVersion(byte value) {
        __Version = value;
    }

    /**
     * Specifies the format of the data fork in conjunction with the di_mode
     * type. This can be one of
     * several values. For directories and links, it can be "local" where all
     * metadata associated with the
     * file is within the inode, "extents" where the inode contains an array of
     * extents to other filesystem
     * blocks which contain the associated metadata or data or "btree" where the
     * inode contains a
     * B+tree root node which points to filesystem blocks containing the
     * metadata or data. Migration
     * between the formats depends on the amount of metadata associated with the
     * inode. "dev" is
     * used for character and block devices while "uuid" is currently not used.
     */
    private InodeFormat __Format = InodeFormat.Dev;

    public InodeFormat getFormat() {
        return __Format;
    }

    public void setFormat(InodeFormat value) {
        __Format = value;
    }

    /**
     * In v1 inodes, this specifies the number of links to the inode from
     * directories. When the number
     * exceeds 65535, the inode is converted to v2 and the link count is stored
     * in di_nlink.
     */
    private short __Onlink;

    public short getOnlink() {
        return __Onlink;
    }

    public void setOnlink(short value) {
        __Onlink = value;
    }

    /**
     * Specifies the owner's UID of the inode.
     */
    private int __UserId;

    public int getUserId() {
        return __UserId;
    }

    public void setUserId(int value) {
        __UserId = value;
    }

    /**
     * Specifies the owner's GID of the inode.
     */
    private int __GroupId;

    public int getGroupId() {
        return __GroupId;
    }

    public void setGroupId(int value) {
        __GroupId = value;
    }

    /**
     * Specifies the number of links to the inode from directories. This is
     * maintained for both inode
     * versions for current versions of XFS.Old versions of XFS did not support
     * v2 inodes, and
     * therefore this value was never updated and was classed as reserved space
     * (part of
     * {@link #Padding}
     * ).
     */
    private int __Nlink;

    public int getNlink() {
        return __Nlink;
    }

    public void setNlink(int value) {
        __Nlink = value;
    }

    /**
     * Specifies the owner's project ID in v2 inodes. An inode is converted to
     * v2 if the project ID is set.
     * This value must be zero for v1 inodes.
     */
    private short __ProjectId;

    public short getProjectId() {
        return __ProjectId;
    }

    public void setProjectId(short value) {
        __ProjectId = value;
    }

    /**
     * Reserved, must be zero.
     */
    private byte[] __Padding;

    public byte[] getPadding() {
        return __Padding;
    }

    public void setPadding(byte[] value) {
        __Padding = value;
    }

    /**
     * Incremented on flush.
     */
    private short __FlushIterator;

    public short getFlushIterator() {
        return __FlushIterator;
    }

    public void setFlushIterator(short value) {
        __FlushIterator = value;
    }

    /**
     * Specifies the last access time of the files using UNIX time conventions
     * the following structure.
     * This value maybe undefined if the filesystem is mounted with the
     * "noatime" option.
     */
    private long __AccessTime;

    public long getAccessTime() {
        return __AccessTime;
    }

    public void setAccessTime(long value) {
        __AccessTime = value;
    }

    /**
     * Specifies the last time the file was modified.
     */
    private long __ModificationTime;

    public long getModificationTime() {
        return __ModificationTime;
    }

    public void setModificationTime(long value) {
        __ModificationTime = value;
    }

    /**
     * Specifies when the inode's status was last changed.
     */
    private long __CreationTime;

    public long getCreationTime() {
        return __CreationTime;
    }

    public void setCreationTime(long value) {
        __CreationTime = value;
    }

    /**
     * Specifies the EOF of the inode in bytes. This can be larger or smaller
     * than the extent space
     * (therefore actual disk space) used for the inode.For regular files, this
     * is the filesize in bytes,
     * directories, the space taken by directory entries and for links, the
     * length of the symlink.
     */
    private long __Length;

    public long getLength() {
        return __Length;
    }

    public void setLength(long value) {
        __Length = value;
    }

    /**
     * Specifies the number of filesystem blocks used to store the inode's data
     * including relevant
     * metadata like B+trees.This does not include blocks used for extended
     * attributes.
     */
    private long __BlockCount;

    public long getBlockCount() {
        return __BlockCount;
    }

    public void setBlockCount(long value) {
        __BlockCount = value;
    }

    /**
     * Specifies the extent size for filesystems with real-time devices and an
     * extent size hint for
     * standard filesystems. For normal filesystems, and with directories, the
     * XFS_DIFLAG_EXTSZINHERIT flag must be set in di_flags if this field is
     * used.Inodes
     * created in these directories will inherit the di_extsize value and have
     * XFS_DIFLAG_EXTSIZE set in their di_flags. When a file is written to
     * beyond allocated
     * space, XFS will attempt to allocate additional disk space based on this
     * value.
     */
    private int __ExtentSize;

    public int getExtentSize() {
        return __ExtentSize;
    }

    public void setExtentSize(int value) {
        __ExtentSize = value;
    }

    /**
     * Specifies the number of data extents associated with this inode.
     */
    private int __Extents;

    public int getExtents() {
        return __Extents;
    }

    public void setExtents(int value) {
        __Extents = value;
    }

    /**
     * Specifies the number of extended attribute extents associated with this
     * inode.
     */
    private short __AttributeExtents;

    public short getAttributeExtents() {
        return __AttributeExtents;
    }

    public void setAttributeExtents(short value) {
        __AttributeExtents = value;
    }

    /**
     * Specifies the offset into the inode's literal area where the extended
     * attribute fork starts. This is
     * an 8-bit value that is multiplied by 8 to determine the actual offset in
     * bytes(ie.attribute data is
     * 64-bit aligned). This also limits the maximum size of the inode to 2048
     * bytes.This value is
     * initially zero until an extended attribute is created.When in attribute
     * is added, the nature of
     * di_forkoff depends on the XFS_SB_VERSION2_ATTR2BIT flag in the
     * superblock.
     */
    private byte __Forkoff;

    public byte getForkoff() {
        return __Forkoff;
    }

    public void setForkoff(byte value) {
        __Forkoff = value;
    }

    /**
     * Specifies the format of the attribute fork. This uses the same values as
     * di_format, but
     * restricted to "local", "extents" and "btree" formats for extended
     * attribute data.
     */
    private byte __AttributeFormat;

    public byte getAttributeFormat() {
        return __AttributeFormat;
    }

    public void setAttributeFormat(byte value) {
        __AttributeFormat = value;
    }

    /**
     * DMAPI event mask.
     */
    private int __DmApiEventMask;

    public int getDmApiEventMask() {
        return __DmApiEventMask;
    }

    public void setDmApiEventMask(int value) {
        __DmApiEventMask = value;
    }

    /**
     * DMAPI state.
     */
    private short __DmState;

    public short getDmState() {
        return __DmState;
    }

    public void setDmState(short value) {
        __DmState = value;
    }

    /**
     * Specifies flags associated with the inode.
     */
    private InodeFlags __Flags = InodeFlags.None;

    public InodeFlags getFlags() {
        return __Flags;
    }

    public void setFlags(InodeFlags value) {
        __Flags = value;
    }

    /**
     * A generation number used for inode identification. This is used by tools
     * that do inode scanning
     * such as backup tools and xfsdump. An inode's generation number can change
     * by unlinking and
     * creating a new file that reuses the inode.
     */
    private int __Generation;

    public int getGeneration() {
        return __Generation;
    }

    public void setGeneration(int value) {
        __Generation = value;
    }

    public UnixFileType getFileType() {
        return UnixFileType.valueOf((getMode() >>> 12) & 0xF);
    }

    private byte[] __DataFork;

    public byte[] getDataFork() {
        return __DataFork;
    }

    public void setDataFork(byte[] value) {
        __DataFork = value;
    }

    public int sizeOf() {
        return 96;
    }

    public int readFrom(byte[] buffer, int offset) {
        setMagic(EndianUtilities.toUInt16BigEndian(buffer, offset));
        setMode(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x2));
        setVersion(buffer[offset + 0x4]);
        setFormat(InodeFormat.valueOf(buffer[offset + 0x5]));
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
        return sizeOf();
    }

    private long readTimestamp(byte[] buffer, int offset) {
        long seconds = EndianUtilities.toUInt32BigEndian(buffer, offset);
        long nanoSeconds = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x4);
        return Instant.ofEpochSecond(seconds, nanoSeconds / 100).toEpochMilli();
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
            result.add(i, extent);
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
}
