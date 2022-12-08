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

package discUtils.xfs;

import java.util.UUID;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


class AllocationGroupInodeBtreeInfo implements IByteArraySerializable {

    public static final int AgiMagic = 0x58414749;

    private int magic;

    /**
     * Specifies the magic number for the AGI sector: "XAGI" (0x58414749)
     */
    public int getMagic() {
        return magic;
    }

    private int version;

    /**
     * Set to XFS_AGI_VERSION which is currently 1.
     */
    public int getVersion() {
        return version;
    }

    private int sequenceNumber;

    /**
     * Specifies the AG number for the sector.
     */
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    private int length;

    /**
     * Specifies the size of the AG in filesystem blocks.
     */
    public int getLength() {
        return length;
    }

    private int count;

    /**
     * Specifies the number of inodes allocated for the AG.
     */
    public int getCount() {
        return count;
    }

    private int root;

    /**
     * Specifies the block number in the AG containing the root of the inode
     * B+tree.
     */
    public int getRoot() {
        return root;
    }

    private int level;

    /**
     * Specifies the number of levels in the inode B+tree.
     */
    public int getLevel() {
        return level;
    }

    private int freeCount;

    public void setFreeCount(int freeCount) {
        this.freeCount = freeCount;
    }

    /**
     * Specifies the number of free inodes in the AG.
     */
    public int getFreeCount() {
        return freeCount;
    }

    private int newInode;

    /**
     * Specifies AG relative inode number most recently allocated.
     */
    public int getNewInode() {
        return newInode;
    }

    private int dirInode = -1;

    /**
     * Deprecated and not used, it's always set to NULL (-1).
     *
     * @deprecated
     */
    public int getDirInode() {
        return dirInode;
    }

    private int[] unlinked;

    /**
     * Hash table of unlinked (deleted) inodes that are still being referenced.
     */
    public int[] getUnlinked() {
        return unlinked;
    }

    private BtreeHeader rootInodeBtree;

    /**
     * root of the inode B+tree
     */
    public BtreeHeader getRootInodeBtree() {
        return rootInodeBtree;
    }

    private UUID uniqueId;

    public UUID getUniqueId() {
        return uniqueId;
    }

    private long lsn;

    /**
     * last write sequence
     */
    public long getLsn() {
        return lsn;
    }

    private int crc;

    public int getCrc() {
        return crc;
    }

    private int size;

    public int size() {
        return size;
    }

    private int sbVersion;

    public int getSbVersion() {
        return sbVersion;
    }

    public AllocationGroupInodeBtreeInfo(SuperBlock superBlock) {
        sbVersion = superBlock.getSbVersion();
        size = sbVersion >= 5 ? 334 : 296;
    }

    public int readFrom(byte[] buffer, int offset) {
        magic = EndianUtilities.toUInt32BigEndian(buffer, offset);
        version = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x4);
        sequenceNumber = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x8);
        length = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xc);
        count = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x10);
        root = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x14);
        level = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x18);
        freeCount = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x1c);
        newInode = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x20);
        unlinked = new int[64];
        for (int i = 0; i < unlinked.length; i++) {
            unlinked[i] = EndianUtilities.toInt32BigEndian(buffer, offset + 0x28 + i * 0x4);
        }
        if (sbVersion >= 5) {
            uniqueId = EndianUtilities.toGuidBigEndian(buffer, offset + 0x132);
            lsn = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x142);
            crc = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x14A);
        }
        return size;
    }

    public void loadBtree(Context context, long offset) {
        Stream data = context.getRawStream();
        data.position(offset + context.getSuperBlock().getBlocksize() * (long) root);
        if (level == 1) {
            rootInodeBtree = new BTreeInodeLeaf(sbVersion);
        } else {
            rootInodeBtree = new BTreeInodeNode(sbVersion);
        }
        byte[] buffer = StreamUtilities.readExact(data, context.getSuperBlock().getBlocksize());
        rootInodeBtree.readFrom(buffer, 0);
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
