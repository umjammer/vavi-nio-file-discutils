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

    private int _magic;

    /**
     * Specifies the magic number for the AGI sector: "XAGI" (0x58414749)
     */
    public int getMagic() {
        return _magic;
    }

    private int _version;

    /**
     * Set to XFS_AGI_VERSION which is currently 1.
     */
    public int getVersion() {
        return _version;
    }

    private int _sequenceNumber;

    /**
     * Specifies the AG number for the sector.
     */
    public int getSequenceNumber() {
        return _sequenceNumber;
    }

    private int _length;

    /**
     * Specifies the size of the AG in filesystem blocks.
     */
    public int getLength() {
        return _length;
    }

    private int _count;

    /**
     * Specifies the number of inodes allocated for the AG.
     */
    public int getCount() {
        return _count;
    }

    private int _root;

    /**
     * Specifies the block number in the AG containing the root of the inode
     * B+tree.
     */
    public int getRoot() {
        return _root;
    }

    private int _level;

    /**
     * Specifies the number of levels in the inode B+tree.
     */
    public int getLevel() {
        return _level;
    }

    private int _freeCount;

    public void setFreeCount(int freeCount) {
        _freeCount = freeCount;
    }

    /**
     * Specifies the number of free inodes in the AG.
     */
    public int getFreeCount() {
        return _freeCount;
    }

    private int _newInode;

    /**
     * Specifies AG relative inode number most recently allocated.
     */
    public int getNewInode() {
        return _newInode;
    }

    private int _dirInode = -1;

    /**
     * Deprecated and not used, it's always set to NULL (-1).
     *
     * @deprecated
     */
    public int getDirInode() {
        return _dirInode;
    }

    private int[] _unlinked;

    /**
     * Hash table of unlinked (deleted) inodes that are still being referenced.
     */
    public int[] getUnlinked() {
        return _unlinked;
    }

    private BtreeHeader _rootInodeBtree;

    /**
     * root of the inode B+tree
     */
    public BtreeHeader getRootInodeBtree() {
        return _rootInodeBtree;
    }

    private UUID _uniqueId;

    public UUID getUniqueId() {
        return _uniqueId;
    }

    private long _lsn;

    /**
     * last write sequence
     */
    public long getLsn() {
        return _lsn;
    }

    private int _crc;

    public int getCrc() {
        return _crc;
    }

    private int _size;

    public int size() {
        return _size;
    }

    private int _sbVersion;

    public int getSbVersion() {
        return _sbVersion;
    }

    public AllocationGroupInodeBtreeInfo(SuperBlock superBlock) {
        _sbVersion = superBlock.getSbVersion();
        _size = _sbVersion >= 5 ? 334 : 296;
    }

    public int readFrom(byte[] buffer, int offset) {
        _magic = EndianUtilities.toUInt32BigEndian(buffer, offset);
        _version = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x4);
        _sequenceNumber = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x8);
        _length = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xc);
        _count = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x10);
        _root = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x14);
        _level = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x18);
        _freeCount = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x1c);
        _newInode = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x20);
        _unlinked = new int[64];
        for (int i = 0; i < _unlinked.length; i++) {
            _unlinked[i] = EndianUtilities.toInt32BigEndian(buffer, offset + 0x28 + i * 0x4);
        }
        if (_sbVersion >= 5) {
            _uniqueId = EndianUtilities.toGuidBigEndian(buffer, offset + 0x132);
            _lsn = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x142);
            _crc = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x14A);
        }
        return _size;
    }

    public void loadBtree(Context context, long offset) {
        Stream data = context.getRawStream();
        data.setPosition(offset + context.getSuperBlock().getBlocksize() * (long) _root);
        if (_level == 1) {
            _rootInodeBtree = new BTreeInodeLeaf(_sbVersion);
        } else {
            _rootInodeBtree = new BTreeInodeNode(_sbVersion);
        }
        byte[] buffer = StreamUtilities.readExact(data, context.getSuperBlock().getBlocksize());
        _rootInodeBtree.readFrom(buffer, 0);
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
