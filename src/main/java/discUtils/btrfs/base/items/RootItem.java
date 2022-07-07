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

import discUtils.btrfs.base.Key;
import discUtils.streams.util.EndianUtilities;


/**
 * Contains the stat information for an inode
 */
public class RootItem extends BaseItem {
    public static final int Length = 375;

    public RootItem(Key key) {
        super(key);
    }

    /**
     * Several fields are initialized but only flags is interpreted at runtime.
     * generation=1, size=3,nlink=1, nbytes=leafsize, mode=040755
     * flags depends on kernel version.
     */
    private InodeItem _inode;

    public InodeItem getInode() {
        return _inode;
    }

    public void setInode(InodeItem value) {
        _inode = value;
    }

    /**
     * transid of the transaction that created this root.
     */
    private long _generation;

    public long getGeneration() {
        return _generation;
    }

    public void setGeneration(long value) {
        _generation = value;
    }

    /**
     * For file trees, the objectid of the root directory in this tree (always
     * 256). Otherwise, 0.
     */
    private long _rootDirId;

    public long getRootDirId() {
        return _rootDirId;
    }

    public void setRootDirId(long value) {
        _rootDirId = value;
    }

    /**
     * The disk offset in bytes for the root node of this tree.
     */
    private long _byteNr;

    public long getByteNr() {
        return _byteNr;
    }

    public void setByteNr(long value) {
        _byteNr = value;
    }

    /**
     * Unused. Always 0.
     */
    private long _byteLimit;

    public long getByteLimit() {
        return _byteLimit;
    }

    public void setByteLimit(long value) {
        _byteLimit = value;
    }

    /**
     * Unused
     */
    private long _bytesUsed;

    public long getBytesUsed() {
        return _bytesUsed;
    }

    public void setBytesUsed(long value) {
        _bytesUsed = value;
    }

    /**
     * The last transid of the transaction that created a snapshot of this root.
     */
    private long _lastSnapshot;

    public long getLastSnapshot() {
        return _lastSnapshot;
    }

    public void setLastSnapshot(long value) {
        _lastSnapshot = value;
    }

    /**
     * flags
     */
    private long _flags;

    public long getFlags() {
        return _flags;
    }

    public void setFlags(long value) {
        _flags = value;
    }

    /**
     * Originally indicated a reference count. In modern usage, it is only 0 or
     * 1.
     */
    private int _refs;

    public int getRefs() {
        return _refs;
    }

    public void setRefs(int value) {
        _refs = value;
    }

    /**
     * Contains key of last dropped item during subvolume removal or relocation.
     * Zeroed otherwise.
     */
    private Key _dropProgress;

    public Key getDropProgress() {
        return _dropProgress;
    }

    public void setDropProgress(Key value) {
        _dropProgress = value;
    }

    /**
     * The tree level of the node described in drop_progress.
     */
    private byte _dropLevel;

    public byte getDropLevel() {
        return _dropLevel;
    }

    public void setDropLevel(byte value) {
        _dropLevel = value;
    }

    /**
     * The height of the tree rooted at bytenr.
     */
    private byte _level;

    public int getLevel() {
        return _level & 0xff;
    }

    public void setLevel(byte value) {
        _level = value;
    }

    public int size() {
        return Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        setInode(EndianUtilities.toStruct(InodeItem.class, buffer, offset));
        setGeneration(EndianUtilities.toUInt64LittleEndian(buffer, offset + 160));
        setRootDirId(EndianUtilities.toUInt64LittleEndian(buffer, offset + 168));
        setByteNr(EndianUtilities.toUInt64LittleEndian(buffer, offset + 176));
        setByteLimit(EndianUtilities.toUInt64LittleEndian(buffer, offset + 184));
        setBytesUsed(EndianUtilities.toUInt64LittleEndian(buffer, offset + 192));
        setLastSnapshot(EndianUtilities.toUInt64LittleEndian(buffer, offset + 200));
        setFlags(EndianUtilities.toUInt64LittleEndian(buffer, offset + 208));
        setRefs(EndianUtilities.toUInt32LittleEndian(buffer, offset + 216));
        setDropProgress(EndianUtilities.toStruct(Key.class, buffer, offset + 220));
        setDropLevel(buffer[offset + 237]);
        setLevel(buffer[offset + 238]);
        //The following fields depend on the subvol_uuids+subvol_times features
        //239   __le64  generation_v2   If equal to generation, indicates validity of the following fields.
        //If the root is modified using an older kernel, this field and generation will become out of sync. This is normal and recoverable.
        //247   u8[16]  uuid    This subvolume's UUID.
        //263   u8[16]  parent_uuid     The parent's UUID (for use with send/receive).
        //279   u8[16]  received_uuid   The received UUID (for used with send/receive).
        //295   __le64  ctransid    The transid of the last transaction that modified this tree, with some exceptions (like the internal caches or relocation).
        //303   __le64  otransid    The transid of the transaction that created this tree.
        //311   __le64  stransid    The transid for the transaction that sent this subvolume. Nonzero for received subvolume.
        //319   __le64  rtransid    The transid for the transaction that received this subvolume. Nonzero for received subvolume.
        //327   struct btrfs_timespec   ctime   Timestamp for ctransid.
        //339   struct btrfs_timespec   otime   Timestamp for otransid.
        //351   struct btrfs_timespec   stime   Timestamp for stransid.
        //363   struct btrfs_timespec   rtime   Timestamp for rtransid.
        //375   __le64[8]   reserved    Reserved for future use.

        return size();
    }
}
