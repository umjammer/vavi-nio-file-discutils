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


public class AllocationGroupFreeBlockInfo implements IByteArraySerializable {
    public static final int AgfMagic = 0x58414746;

    public static final int BtreeMagicOffset = 0x41425442;

    public static final int BtreeMagicCount = 0x41425443;

    /**
     * Specifies the magic number for the AGF sector: "XAGF" (0x58414746).
     */
    private int __Magic;

    public int getMagic() {
        return __Magic;
    }

    public void setMagic(int value) {
        __Magic = value;
    }

    /**
     * Set to XFS_AGF_VERSION which is currently 1.
     */
    private int __Version;

    public int getVersion() {
        return __Version;
    }

    public void setVersion(int value) {
        __Version = value;
    }

    /**
     * Specifies the AG number for the sector.
     */
    private int __SequenceNumber;

    public int getSequenceNumber() {
        return __SequenceNumber;
    }

    public void setSequenceNumber(int value) {
        __SequenceNumber = value;
    }

    /**
     * Specifies the size of the AG in filesystem blocks. For all AGs except the
     * last, this must be equal
     * to the superblock's
     * {@link #SuperBlock.AgBlocks}
     * value. For the last AG, this could be less than the
     *
     * {@link #SuperBlock.AgBlocks}
     * value. It is this value that should be used to determine the size of the
     * AG.
     */
    private int __Length;

    public int getLength() {
        return __Length;
    }

    public void setLength(int value) {
        __Length = value;
    }

    /**
     * Specifies the block number for the root of the two free space B+trees.
     */
    private int[] __RootBlockNumbers;

    public int[] getRootBlockNumbers() {
        return __RootBlockNumbers;
    }

    public void setRootBlockNumbers(int[] value) {
        __RootBlockNumbers = value;
    }

    private int __Spare0;

    public int getSpare0() {
        return __Spare0;
    }

    public void setSpare0(int value) {
        __Spare0 = value;
    }

    /**
     * Specifies the level or depth of the two free space B+trees. For a fresh
     * AG, this will be one, and
     * the "roots" will point to a single leaf of level 0.
     */
    public int[] Levels;

    private int __Spare1;

    public int getSpare1() {
        return __Spare1;
    }

    public void setSpare1(int value) {
        __Spare1 = value;
    }

    /**
     * Specifies the index of the first "free list" block.
     */
    private int __FreeListFirst;

    public int getFreeListFirst() {
        return __FreeListFirst;
    }

    public void setFreeListFirst(int value) {
        __FreeListFirst = value;
    }

    /**
     * Specifies the index of the last "free list" block.
     */
    private int __FreeListLast;

    public int getFreeListLast() {
        return __FreeListLast;
    }

    public void setFreeListLast(int value) {
        __FreeListLast = value;
    }

    /**
     * Specifies the number of blocks in the "free list".
     */
    private int __FreeListCount;

    public int getFreeListCount() {
        return __FreeListCount;
    }

    public void setFreeListCount(int value) {
        __FreeListCount = value;
    }

    /**
     * Specifies the current number of free blocks in the AG.
     */
    private int __FreeBlocks;

    public int getFreeBlocks() {
        return __FreeBlocks;
    }

    public void setFreeBlocks(int value) {
        __FreeBlocks = value;
    }

    /**
     * Specifies the number of blocks of longest contiguous free space in the
     * AG.
     */
    private int __Longest;

    public int getLongest() {
        return __Longest;
    }

    public void setLongest(int value) {
        __Longest = value;
    }

    /**
     * Specifies the number of blocks used for the free space B+trees. This is
     * only used if the
     * XFS_SB_VERSION2_LAZYSBCOUNTBIT bit is set in
     * {@link #SuperBlock.Features2}
     * .
     */
    private int __BTreeBlocks;

    public int getBTreeBlocks() {
        return __BTreeBlocks;
    }

    public void setBTreeBlocks(int value) {
        __BTreeBlocks = value;
    }

    /**
     * stores a sorted array of block offset and block counts in the leaves of
     * the B+tree, sorted by the offset
     */
    private BtreeHeader __FreeSpaceOffset;

    public BtreeHeader getFreeSpaceOffset() {
        return __FreeSpaceOffset;
    }

    public void setFreeSpaceOffset(BtreeHeader value) {
        __FreeSpaceOffset = value;
    }

    /**
     * stores a sorted array of block offset and block counts in the leaves of
     * the B+tree, sorted by the count or size
     */
    private BtreeHeader __FreeSpaceCount;

    public BtreeHeader getFreeSpaceCount() {
        return __FreeSpaceCount;
    }

    public void setFreeSpaceCount(BtreeHeader value) {
        __FreeSpaceCount = value;
    }

    private UUID __UniqueId;

    public UUID getUniqueId() {
        return __UniqueId;
    }

    public void setUniqueId(UUID value) {
        __UniqueId = value;
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

    private int __Crc;

    public int getCrc() {
        return __Crc;
    }

    public void setCrc(int value) {
        __Crc = value;
    }

    private int __Size;

    public long getSize() {
        return __Size;
    }

    private int __SbVersion;

    private int getSbVersion() {
        return __SbVersion;
    }

    public AllocationGroupFreeBlockInfo(SuperBlock superBlock) {
        __SbVersion = superBlock.getSbVersion();
        __Size = getSbVersion() >= 5 ? 92 : 64;
    }

    public int readFrom(byte[] buffer, int offset) {
        setMagic(EndianUtilities.toUInt32BigEndian(buffer, offset));
        setVersion(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x4));
        setSequenceNumber(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x8));
        setLength(EndianUtilities.toUInt32BigEndian(buffer, offset + 0xC));
        setRootBlockNumbers(new int[2]);
        getRootBlockNumbers()[0] = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x10);
        getRootBlockNumbers()[1] = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x14);
        setSpare0(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x18));
        Levels = new int[2];
        Levels[0] = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x1C);
        Levels[1] = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x20);
        setSpare1(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x24));
        setFreeListFirst(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x28));
        setFreeListLast(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x2C));
        setFreeListCount(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x30));
        setFreeBlocks(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x34));
        setLongest(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x38));
        setBTreeBlocks(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x3C));
        if (getSbVersion() >= 5) {
            setUniqueId(EndianUtilities.toGuidBigEndian(buffer, offset + 0x40));
            setLsn(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x50));
            setCrc(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x58));
        }

        return (int) getSize();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
