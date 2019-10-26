//
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
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.Stream;


class AllocationGroupInodeBtreeInfo implements IByteArraySerializable {

    public static final int AgiMagic = 0x58414749;

    private int Magic;

    /**
     * Specifies the magic number for the AGI sector: "XAGI" (0x58414749)
     */
    public int getMagic() {
        return Magic;
    }

    private int Version;

    /**
     * Set to XFS_AGI_VERSION which is currently 1.
     */
    public int getVersion() {
        return Version;
    }

    private int SequenceNumber;

    /**
     * Specifies the AG number for the sector.
     */
    public int getSequenceNumber() {
        return SequenceNumber;
    }

    private int Length;

    /**
     * Specifies the size of the AG in filesystem blocks.
     */
    public int getLength() {
        return Length;
    }

    private int Count;

    /**
     * Specifies the number of inodes allocated for the AG.
     */
    public int getCount() {
        return Count;
    }

    private int Root;

    /**
     * Specifies the block number in the AG containing the root of the inode
     * B+tree.
     */
    public int getRoot() {
        return Root;
    }

    private int Level;

    /**
     * Specifies the number of levels in the inode B+tree.
     */
    public int getLevel() {
        return Level;
    }

    private int FreeCount;

    public void setFreeCount(int freeCount) {
        FreeCount = freeCount;
    }

    /**
     * Specifies the number of free inodes in the AG.
     */
    public int getFreeCount() {
        return FreeCount;
    }

    private int NewInode;

    /**
     * Specifies AG relative inode number most recently allocated.
     */
    public int getNewInode() {
        return NewInode;
    }

    private int DirInode = -1;

    /**
     * Deprecated and not used, it's always set to NULL (-1).
     *
     * @deprecated
     */
    public int getDirInode() {
        return DirInode;
    }

    private int[] Unlinked;

    /**
     * Hash table of unlinked (deleted) inodes that are still being referenced.
     */
    public int[] getUnlinked() {
        return Unlinked;
    }

    private BtreeHeader RootInodeBtree;

    /**
     * root of the inode B+tree
     */
    public BtreeHeader getRootInodeBtree() {
        return RootInodeBtree;
    }

    private UUID UniqueId;

    public UUID getUniqueId() {
        return UniqueId;
    }

    private long Lsn;

    /**
     * last write sequence
     */
    public long getLsn() {
        return Lsn;
    }

    private int Crc;

    public int getCrc() {
        return Crc;
    }

    private int Size;

    public int size() {
        return Size;
    }

    private int SbVersion;

    public int getSbVersion() {
        return SbVersion;
    }

    public AllocationGroupInodeBtreeInfo(SuperBlock superBlock) {
        SbVersion = superBlock.getSbVersion();
        Size = SbVersion >= 5 ? 334 : 296;
    }

    public int readFrom(byte[] buffer, int offset) {
        Magic = EndianUtilities.toUInt32BigEndian(buffer, offset);
        Version = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x4);
        SequenceNumber = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x8);
        Length = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xc);
        Count = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x10);
        Root = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x14);
        Level = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x18);
        FreeCount = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x1c);
        NewInode = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x20);
        Unlinked = new int[64];
        for (int i = 0; i < Unlinked.length; i++) {
            Unlinked[i] = EndianUtilities.toInt32BigEndian(buffer, offset + 0x28 + i * 0x4);
        }
        if (SbVersion >= 5) {
            UniqueId = EndianUtilities.toGuidBigEndian(buffer, offset + 0x132);
            Lsn = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x142);
            Crc = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x14A);
        }
        return Size;
    }

    public void loadBtree(Context context, long offset) {
        Stream data = context.getRawStream();
        data.setPosition(offset + context.getSuperBlock().getBlocksize() * (long) Root);
        if (Level == 1) {
            RootInodeBtree = new BTreeInodeLeaf(SbVersion);
        } else {
            RootInodeBtree = new BTreeInodeNode(SbVersion);
        }
        byte[] buffer = StreamUtilities.readExact(data, context.getSuperBlock().getBlocksize());
        RootInodeBtree.readFrom(buffer, 0);
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
