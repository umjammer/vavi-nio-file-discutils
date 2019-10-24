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

package DiscUtils.Btrfs.Base.Items;

import DiscUtils.Btrfs.Base.Key;
import DiscUtils.Streams.Util.EndianUtilities;


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
    private InodeItem __Inode;

    public InodeItem getInode() {
        return __Inode;
    }

    public void setInode(InodeItem value) {
        __Inode = value;
    }

    /**
     * transid of the transaction that created this root.
     */
    private long __Generation;

    public long getGeneration() {
        return __Generation;
    }

    public void setGeneration(long value) {
        __Generation = value;
    }

    /**
     * For file trees, the objectid of the root directory in this tree (always
     * 256). Otherwise, 0.
     */
    private long __RootDirId;

    public long getRootDirId() {
        return __RootDirId;
    }

    public void setRootDirId(long value) {
        __RootDirId = value;
    }

    /**
     * The disk offset in bytes for the root node of this tree.
     */
    private long __ByteNr;

    public long getByteNr() {
        return __ByteNr;
    }

    public void setByteNr(long value) {
        __ByteNr = value;
    }

    /**
     * Unused. Always 0.
     */
    private long __ByteLimit;

    public long getByteLimit() {
        return __ByteLimit;
    }

    public void setByteLimit(long value) {
        __ByteLimit = value;
    }

    /**
     * Unused
     */
    private long __BytesUsed;

    public long getBytesUsed() {
        return __BytesUsed;
    }

    public void setBytesUsed(long value) {
        __BytesUsed = value;
    }

    /**
     * The last transid of the transaction that created a snapshot of this root.
     */
    private long __LastSnapshot;

    public long getLastSnapshot() {
        return __LastSnapshot;
    }

    public void setLastSnapshot(long value) {
        __LastSnapshot = value;
    }

    /**
     * flags
     */
    private long __Flags;

    public long getFlags() {
        return __Flags;
    }

    public void setFlags(long value) {
        __Flags = value;
    }

    /**
     * Originally indicated a reference count. In modern usage, it is only 0 or
     * 1.
     */
    private int __Refs;

    public int getRefs() {
        return __Refs;
    }

    public void setRefs(int value) {
        __Refs = value;
    }

    /**
     * Contains key of last dropped item during subvolume removal or relocation.
     * Zeroed otherwise.
     */
    private Key __DropProgress;

    public Key getDropProgress() {
        return __DropProgress;
    }

    public void setDropProgress(Key value) {
        __DropProgress = value;
    }

    /**
     * The tree level of the node described in drop_progress.
     */
    private byte __DropLevel;

    public byte getDropLevel() {
        return __DropLevel;
    }

    public void setDropLevel(byte value) {
        __DropLevel = value;
    }

    /**
     * The height of the tree rooted at bytenr.
     */
    private byte __Level;

    public byte getLevel() {
        return __Level;
    }

    public void setLevel(byte value) {
        __Level = value;
    }

    public int sizeOf() {
        return Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        setInode(EndianUtilities.<InodeItem> toStruct(InodeItem.class, buffer, offset));
        setGeneration(EndianUtilities.toUInt64LittleEndian(buffer, offset + 160));
        setRootDirId(EndianUtilities.toUInt64LittleEndian(buffer, offset + 168));
        setByteNr(EndianUtilities.toUInt64LittleEndian(buffer, offset + 176));
        setByteLimit(EndianUtilities.toUInt64LittleEndian(buffer, offset + 184));
        setBytesUsed(EndianUtilities.toUInt64LittleEndian(buffer, offset + 192));
        setLastSnapshot(EndianUtilities.toUInt64LittleEndian(buffer, offset + 200));
        setFlags(EndianUtilities.toUInt64LittleEndian(buffer, offset + 208));
        setRefs(EndianUtilities.toUInt32LittleEndian(buffer, offset + 216));
        setDropProgress(EndianUtilities.<Key> toStruct(Key.class, buffer, offset + 220));
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

        return sizeOf();
    }
}
