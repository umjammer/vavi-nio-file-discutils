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
import vavi.util.ByteUtil;


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
    private InodeItem inode;

    public InodeItem getInode() {
        return inode;
    }

    public void setInode(InodeItem value) {
        inode = value;
    }

    /**
     * transid of the transaction that created this root.
     */
    private long generation;

    public long getGeneration() {
        return generation;
    }

    public void setGeneration(long value) {
        generation = value;
    }

    /**
     * For file trees, the objectid of the root directory in this tree (always
     * 256). Otherwise, 0.
     */
    private long rootDirId;

    public long getRootDirId() {
        return rootDirId;
    }

    public void setRootDirId(long value) {
        rootDirId = value;
    }

    /**
     * The disk offset in bytes for the root node of this tree.
     */
    private long byteNr;

    public long getByteNr() {
        return byteNr;
    }

    public void setByteNr(long value) {
        byteNr = value;
    }

    /**
     * Unused. Always 0.
     */
    private long byteLimit;

    public long getByteLimit() {
        return byteLimit;
    }

    public void setByteLimit(long value) {
        byteLimit = value;
    }

    /**
     * Unused
     */
    private long bytesUsed;

    public long getBytesUsed() {
        return bytesUsed;
    }

    public void setBytesUsed(long value) {
        bytesUsed = value;
    }

    /**
     * The last transid of the transaction that created a snapshot of this root.
     */
    private long lastSnapshot;

    public long getLastSnapshot() {
        return lastSnapshot;
    }

    public void setLastSnapshot(long value) {
        lastSnapshot = value;
    }

    /**
     * flags
     */
    private long flags;

    public long getFlags() {
        return flags;
    }

    public void setFlags(long value) {
        flags = value;
    }

    /**
     * Originally indicated a reference count. In modern usage, it is only 0 or
     * 1.
     */
    private int refs;

    public int getRefs() {
        return refs;
    }

    public void setRefs(int value) {
        refs = value;
    }

    /**
     * Contains key of last dropped item during subvolume removal or relocation.
     * Zeroed otherwise.
     */
    private Key dropProgress;

    public Key getDropProgress() {
        return dropProgress;
    }

    public void setDropProgress(Key value) {
        dropProgress = value;
    }

    /**
     * The tree level of the node described in drop_progress.
     */
    private byte dropLevel;

    public byte getDropLevel() {
        return dropLevel;
    }

    public void setDropLevel(byte value) {
        dropLevel = value;
    }

    /**
     * The height of the tree rooted at bytenr.
     */
    private byte level;

    public int getLevel() {
        return level & 0xff;
    }

    public void setLevel(byte value) {
        level = value;
    }

    public int size() {
        return Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        inode = EndianUtilities.toStruct(InodeItem.class, buffer, offset);
        generation = ByteUtil.readLeLong(buffer, offset + 160);
        rootDirId = ByteUtil.readLeLong(buffer, offset + 168);
        byteNr = ByteUtil.readLeLong(buffer, offset + 176);
        byteLimit = ByteUtil.readLeLong(buffer, offset + 184);
        bytesUsed = ByteUtil.readLeLong(buffer, offset + 192);
        lastSnapshot = ByteUtil.readLeLong(buffer, offset + 200);
        flags = ByteUtil.readLeLong(buffer, offset + 208);
        refs = ByteUtil.readLeInt(buffer, offset + 216);
        dropProgress = EndianUtilities.toStruct(Key.class, buffer, offset + 220);
        dropLevel = buffer[offset + 237];
        level = buffer[offset + 238];
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
