//
// Copyright (c) 2017, Bianco Veigel
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

package discUtils.btrfs.base.items;

import java.util.EnumSet;

import discUtils.btrfs.base.InodeFlag;
import discUtils.btrfs.base.Key;
import discUtils.btrfs.base.TimeSpec;
import discUtils.streams.util.EndianUtilities;


/**
 * define the location and parameters of the root of a btree
 */
public class InodeItem extends BaseItem {
    public InodeItem(Key key) {
        super(key);
    }

    public InodeItem() {
        this(null);
    }

    public static final int Length = 160;

    private long _generation;

    public long getGeneration() {
        return _generation;
    }

    public void setGeneration(long value) {
        _generation = value;
    }

    private long _transId;

    public long getTransId() {
        return _transId;
    }

    public void setTransId(long value) {
        _transId = value;
    }

    /**
     * Size of the file in bytes.
     */
    private long _fileSize;

    public long getFileSize() {
        return _fileSize;
    }

    public void setFileSize(long value) {
        _fileSize = value;
    }

    /**
     * Size allocated to this file, in bytes;
     * Sum of the offset fields of all EXTENT_DATA items for this inode. For
     * directories: 0.
     */
    private long _nBytes;

    public long getNBytes() {
        return _nBytes;
    }

    public void setNBytes(long value) {
        _nBytes = value;
    }

    /**
     * Unused for normal inodes. Contains byte offset of block group when used
     * as a free space inode.
     */
    private long _blockGroup;

    public long getBlockGroup() {
        return _blockGroup;
    }

    public void setBlockGroup(long value) {
        _blockGroup = value;
    }

    /**
     * Count of INODE_REF entries for the inode. When used outside of a file
     * tree, 1.
     */
    private int _linkCount;

    public int getLinkCount() {
        return _linkCount;
    }

    public void setLinkCount(int value) {
        _linkCount = value;
    }

    /**
     * stat.st_uid
     */
    private int _uid;

    public int getUid() {
        return _uid;
    }

    public void setUid(int value) {
        _uid = value;
    }

    /**
     * stat.st_gid
     */
    private int _gid;

    public int getGid() {
        return _gid;
    }

    public void setGid(int value) {
        _gid = value;
    }

    /**
     * stat.st_mode
     */
    private int _mode;

    public int getMode() {
        return _mode;
    }

    public void setMode(int value) {
        _mode = value;
    }

    /**
     * stat.st_rdev
     */
    private long _rDev;

    public long getRDev() {
        return _rDev;
    }

    public void setRDev(long value) {
        _rDev = value;
    }

    /**
     * Inode flags
     */
    private EnumSet<InodeFlag> _flags;

    public EnumSet<InodeFlag> getFlags() {
        return _flags;
    }

    public void setFlags(EnumSet<InodeFlag> value) {
        _flags = value;
    }

    /**
     * Sequence number used for NFS compatibility. Initialized to 0 and
     * incremented each time mtime value is changed.
     */
    private long _sequence;

    public long getSequence() {
        return _sequence;
    }

    public void setSequence(long value) {
        _sequence = value;
    }

    /**
     * stat.st_atime
     */
    private TimeSpec _aTime;

    public TimeSpec getATime() {
        return _aTime;
    }

    public void setATime(TimeSpec value) {
        _aTime = value;
    }

    /**
     * stat.st_ctime
     */
    private TimeSpec _cTime;

    public TimeSpec getCTime() {
        return _cTime;
    }

    public void setCTime(TimeSpec value) {
        _cTime = value;
    }

    /**
     * stat.st_mtime
     */
    private TimeSpec _mTime;

    public TimeSpec getMTime() {
        return _mTime;
    }

    public void setMTime(TimeSpec value) {
        _mTime = value;
    }

    /**
     * Timestamp of inode creation
     */
    private TimeSpec _oTime;

    public TimeSpec getOTime() {
        return _oTime;
    }

    public void setOTime(TimeSpec value) {
        _oTime = value;
    }

    public int size() {
        return Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        setGeneration(EndianUtilities.toUInt64LittleEndian(buffer, offset));
        setTransId(EndianUtilities.toUInt64LittleEndian(buffer, offset + 8));
        setFileSize(EndianUtilities.toUInt64LittleEndian(buffer, offset + 16));
        setNBytes(EndianUtilities.toUInt64LittleEndian(buffer, offset + 24));
        setBlockGroup(EndianUtilities.toUInt64LittleEndian(buffer, offset + 32));
        setLinkCount(EndianUtilities.toUInt32LittleEndian(buffer, offset + 40));
        setUid(EndianUtilities.toUInt32LittleEndian(buffer, offset + 44));
        setGid(EndianUtilities.toUInt32LittleEndian(buffer, offset + 48));
        setMode(EndianUtilities.toUInt32LittleEndian(buffer, offset + 52));
        setRDev(EndianUtilities.toUInt64LittleEndian(buffer, offset + 56));
        setFlags(InodeFlag.valueOf((int) EndianUtilities.toUInt64LittleEndian(buffer, offset + 64)));
        setSequence(EndianUtilities.toUInt64LittleEndian(buffer, offset + 72));
        setATime(EndianUtilities.toStruct(TimeSpec.class, buffer, offset + 112));
        setCTime(EndianUtilities.toStruct(TimeSpec.class, buffer, offset + 124));
        setMTime(EndianUtilities.toStruct(TimeSpec.class, buffer, offset + 136));
        setOTime(EndianUtilities.toStruct(TimeSpec.class, buffer, offset + 148));
        return size();
    }
}
