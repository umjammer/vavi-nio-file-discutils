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
    private int _magic;

    public int getMagic() {
        return _magic;
    }

    public void setMagic(int value) {
        _magic = value;
    }

    /**
     * Set to XFS_AGF_VERSION which is currently 1.
     */
    private int _version;

    public int getVersion() {
        return _version;
    }

    public void setVersion(int value) {
        _version = value;
    }

    /**
     * Specifies the AG number for the sector.
     */
    private int _sequenceNumber;

    public int getSequenceNumber() {
        return _sequenceNumber;
    }

    public void setSequenceNumber(int value) {
        _sequenceNumber = value;
    }

    /**
     * Specifies the size of the AG in filesystem blocks. For all AGs except the
     * last, this must be equal
     * to the superblock's
     * {@link SuperBlock#_agBlocks}
     * value. For the last AG, this could be less than the
     *
     * {@link SuperBlock#_agBlocks}
     * value. It is this value that should be used to determine the size of the
     * AG.
     */
    private int _length;

    public int getLength() {
        return _length;
    }

    public void setLength(int value) {
        _length = value;
    }

    /**
     * Specifies the block number for the root of the two free space B+trees.
     */
    private int[] _rootBlockNumbers;

    public int[] getRootBlockNumbers() {
        return _rootBlockNumbers;
    }

    public void setRootBlockNumbers(int[] value) {
        _rootBlockNumbers = value;
    }

    private int _spare0;

    public int getSpare0() {
        return _spare0;
    }

    public void setSpare0(int value) {
        _spare0 = value;
    }

    /**
     * Specifies the level or depth of the two free space B+trees. For a fresh
     * AG, this will be one, and
     * the "roots" will point to a single leaf of level 0.
     */
    public int[] Levels;

    private int _spare1;

    public int getSpare1() {
        return _spare1;
    }

    public void setSpare1(int value) {
        _spare1 = value;
    }

    /**
     * Specifies the index of the first "free list" block.
     */
    private int _freeListFirst;

    public int getFreeListFirst() {
        return _freeListFirst;
    }

    public void setFreeListFirst(int value) {
        _freeListFirst = value;
    }

    /**
     * Specifies the index of the last "free list" block.
     */
    private int _freeListLast;

    public int getFreeListLast() {
        return _freeListLast;
    }

    public void setFreeListLast(int value) {
        _freeListLast = value;
    }

    /**
     * Specifies the number of blocks in the "free list".
     */
    private int _freeListCount;

    public int getFreeListCount() {
        return _freeListCount;
    }

    public void setFreeListCount(int value) {
        _freeListCount = value;
    }

    /**
     * Specifies the current number of free blocks in the AG.
     */
    private int _freeBlocks;

    public int getFreeBlocks() {
        return _freeBlocks;
    }

    public void setFreeBlocks(int value) {
        _freeBlocks = value;
    }

    /**
     * Specifies the number of blocks of longest contiguous free space in the
     * AG.
     */
    private int _longest;

    public int getLongest() {
        return _longest;
    }

    public void setLongest(int value) {
        _longest = value;
    }

    /**
     * Specifies the number of blocks used for the free space B+trees. This is
     * only used if the
     * XFS_SB_VERSION2_LAZYSBCOUNTBIT bit is set in
     * {@link SuperBlock#_features2}
     * .
     */
    private int _bTreeBlocks;

    public int getBTreeBlocks() {
        return _bTreeBlocks;
    }

    public void setBTreeBlocks(int value) {
        _bTreeBlocks = value;
    }

    /**
     * stores a sorted array of block offset and block counts in the leaves of
     * the B+tree, sorted by the offset
     */
    private BtreeHeader _freeSpaceOffset;

    public BtreeHeader getFreeSpaceOffset() {
        return _freeSpaceOffset;
    }

    public void setFreeSpaceOffset(BtreeHeader value) {
        _freeSpaceOffset = value;
    }

    /**
     * stores a sorted array of block offset and block counts in the leaves of
     * the B+tree, sorted by the count or size
     */
    private BtreeHeader _freeSpaceCount;

    public BtreeHeader getFreeSpaceCount() {
        return _freeSpaceCount;
    }

    public void setFreeSpaceCount(BtreeHeader value) {
        _freeSpaceCount = value;
    }

    private UUID _uniqueId;

    public UUID getUniqueId() {
        return _uniqueId;
    }

    public void setUniqueId(UUID value) {
        _uniqueId = value;
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

    private int _crc;

    public int getCrc() {
        return _crc;
    }

    public void setCrc(int value) {
        _crc = value;
    }

    private int _size;

    public int size() {
        return _size;
    }

    private int _sbVersion;

    private int getSbVersion() {
        return _sbVersion;
    }

    public AllocationGroupFreeBlockInfo(SuperBlock superBlock) {
        _sbVersion = superBlock.getSbVersion();
        _size = getSbVersion() >= 5 ? 92 : 64;
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

        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
