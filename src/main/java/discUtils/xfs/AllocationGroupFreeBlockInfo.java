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
import vavi.util.ByteUtil;


public class AllocationGroupFreeBlockInfo implements IByteArraySerializable {

    public static final int AgfMagic = 0x58414746;

    public static final int BtreeMagicOffset = 0x41425442;

    public static final int BtreeMagicCount = 0x41425443;

    /**
     * Specifies the magic number for the AGF sector: "XAGF" (0x58414746).
     */
    private int magic;

    public int getMagic() {
        return magic;
    }

    public void setMagic(int value) {
        magic = value;
    }

    /**
     * Set to XFS_AGF_VERSION which is currently 1.
     */
    private int version;

    public int getVersion() {
        return version;
    }

    public void setVersion(int value) {
        version = value;
    }

    /**
     * Specifies the AG number for the sector.
     */
    private int sequenceNumber;

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int value) {
        sequenceNumber = value;
    }

    /**
     * Specifies the size of the AG in filesystem blocks. For all AGs except the
     * last, this must be equal
     * to the superblock's {@link SuperBlock#getAgBlocks()}
     * value. For the last AG, this could be less than the
     *
     * {@link SuperBlock#getAgBlocks()}
     * value. It is this value that should be used to determine the size of the
     * AG.
     */
    private int length;

    public int getLength() {
        return length;
    }

    public void setLength(int value) {
        length = value;
    }

    /**
     * Specifies the block number for the root of the two free space B+trees.
     */
    private int[] rootBlockNumbers;

    public int[] getRootBlockNumbers() {
        return rootBlockNumbers;
    }

    public void setRootBlockNumbers(int[] value) {
        rootBlockNumbers = value;
    }

    private int spare0;

    public int getSpare0() {
        return spare0;
    }

    public void setSpare0(int value) {
        spare0 = value;
    }

    /**
     * Specifies the level or depth of the two free space B+trees. For a fresh
     * AG, this will be one, and
     * the "roots" will point to a single leaf of level 0.
     */
    public int[] levels;

    private int spare1;

    public int getSpare1() {
        return spare1;
    }

    public void setSpare1(int value) {
        spare1 = value;
    }

    /**
     * Specifies the index of the first "free list" block.
     */
    private int freeListFirst;

    public int getFreeListFirst() {
        return freeListFirst;
    }

    public void setFreeListFirst(int value) {
        freeListFirst = value;
    }

    /**
     * Specifies the index of the last "free list" block.
     */
    private int freeListLast;

    public int getFreeListLast() {
        return freeListLast;
    }

    public void setFreeListLast(int value) {
        freeListLast = value;
    }

    /**
     * Specifies the number of blocks in the "free list".
     */
    private int freeListCount;

    public int getFreeListCount() {
        return freeListCount;
    }

    public void setFreeListCount(int value) {
        freeListCount = value;
    }

    /**
     * Specifies the current number of free blocks in the AG.
     */
    private int freeBlocks;

    public int getFreeBlocks() {
        return freeBlocks;
    }

    public void setFreeBlocks(int value) {
        freeBlocks = value;
    }

    /**
     * Specifies the number of blocks of longest contiguous free space in the
     * AG.
     */
    private int longest;

    public int getLongest() {
        return longest;
    }

    public void setLongest(int value) {
        longest = value;
    }

    /**
     * Specifies the number of blocks used for the free space B+trees. This is
     * only used if the XFS_SB_VERSION2_LAZYSBCOUNTBIT bit is set in
     * {@link SuperBlock#getFeatures2}.
     */
    private int bTreeBlocks;

    public int getBTreeBlocks() {
        return bTreeBlocks;
    }

    public void setBTreeBlocks(int value) {
        bTreeBlocks = value;
    }

    /**
     * stores a sorted array of block offset and block counts in the leaves of
     * the B+tree, sorted by the offset
     */
    private BtreeHeader freeSpaceOffset;

    public BtreeHeader getFreeSpaceOffset() {
        return freeSpaceOffset;
    }

    public void setFreeSpaceOffset(BtreeHeader value) {
        freeSpaceOffset = value;
    }

    /**
     * stores a sorted array of block offset and block counts in the leaves of
     * the B+tree, sorted by the count or size
     */
    private BtreeHeader freeSpaceCount;

    public BtreeHeader getFreeSpaceCount() {
        return freeSpaceCount;
    }

    public void setFreeSpaceCount(BtreeHeader value) {
        freeSpaceCount = value;
    }

    private UUID uniqueId;

    public UUID getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(UUID value) {
        uniqueId = value;
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

    private int crc;

    public int getCrc() {
        return crc;
    }

    public void setCrc(int value) {
        crc = value;
    }

    private final int size;

    @Override public int size() {
        return size;
    }

    private final int sbVersion;

    private int getSbVersion() {
        return sbVersion;
    }

    public AllocationGroupFreeBlockInfo(SuperBlock superBlock) {
        sbVersion = superBlock.getSbVersion();
        size = sbVersion >= 5 ? 92 : 64;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        magic = ByteUtil.readBeInt(buffer, offset);
        version = ByteUtil.readBeInt(buffer, offset + 0x4);
        sequenceNumber = ByteUtil.readBeInt(buffer, offset + 0x8);
        length = ByteUtil.readBeInt(buffer, offset + 0xC);
        rootBlockNumbers = new int[2];
        rootBlockNumbers[0] = ByteUtil.readBeInt(buffer, offset + 0x10);
        rootBlockNumbers[1] = ByteUtil.readBeInt(buffer, offset + 0x14);
        spare0 = ByteUtil.readBeInt(buffer, offset + 0x18);
        levels = new int[2];
        levels[0] = ByteUtil.readBeInt(buffer, offset + 0x1C);
        levels[1] = ByteUtil.readBeInt(buffer, offset + 0x20);
        spare1 = ByteUtil.readBeInt(buffer, offset + 0x24);
        freeListFirst = ByteUtil.readBeInt(buffer, offset + 0x28);
        freeListLast = ByteUtil.readBeInt(buffer, offset + 0x2C);
        freeListCount = ByteUtil.readBeInt(buffer, offset + 0x30);
        freeBlocks = ByteUtil.readBeInt(buffer, offset + 0x34);
        longest = ByteUtil.readBeInt(buffer, offset + 0x38);
        bTreeBlocks = ByteUtil.readBeInt(buffer, offset + 0x3C);
        if (sbVersion >= 5) {
            uniqueId = ByteUtil.readBeUUID(buffer, offset + 0x40);
            lsn = ByteUtil.readBeLong(buffer, offset + 0x50);
            crc = ByteUtil.readBeInt(buffer, offset + 0x58);
        }

        return size();
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
