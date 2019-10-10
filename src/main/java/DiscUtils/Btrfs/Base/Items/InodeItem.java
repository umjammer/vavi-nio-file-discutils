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

import java.util.EnumSet;

import DiscUtils.Btrfs.Base.InodeFlag;
import DiscUtils.Btrfs.Base.Key;
import DiscUtils.Btrfs.Base.TimeSpec;
import DiscUtils.Streams.Util.EndianUtilities;


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

    private long __Generation;

    public long getGeneration() {
        return __Generation;
    }

    public void setGeneration(long value) {
        __Generation = value;
    }

    private long __TransId;

    public long getTransId() {
        return __TransId;
    }

    public void setTransId(long value) {
        __TransId = value;
    }

    /**
     * Size of the file in bytes.
     */
    private long __FileSize;

    public long getFileSize() {
        return __FileSize;
    }

    public void setFileSize(long value) {
        __FileSize = value;
    }

    /**
     * Size allocated to this file, in bytes;
     * Sum of the offset fields of all EXTENT_DATA items for this inode. For
     * directories: 0.
     */
    private long __NBytes;

    public long getNBytes() {
        return __NBytes;
    }

    public void setNBytes(long value) {
        __NBytes = value;
    }

    /**
     * Unused for normal inodes. Contains byte offset of block group when used
     * as a free space inode.
     */
    private long __BlockGroup;

    public long getBlockGroup() {
        return __BlockGroup;
    }

    public void setBlockGroup(long value) {
        __BlockGroup = value;
    }

    /**
     * Count of INODE_REF entries for the inode. When used outside of a file
     * tree, 1.
     */
    private int __LinkCount;

    public int getLinkCount() {
        return __LinkCount;
    }

    public void setLinkCount(int value) {
        __LinkCount = value;
    }

    /**
     * stat.st_uid
     */
    private int __Uid;

    public int getUid() {
        return __Uid;
    }

    public void setUid(int value) {
        __Uid = value;
    }

    /**
     * stat.st_gid
     */
    private int __Gid;

    public int getGid() {
        return __Gid;
    }

    public void setGid(int value) {
        __Gid = value;
    }

    /**
     * stat.st_mode
     */
    private int __Mode;

    public int getMode() {
        return __Mode;
    }

    public void setMode(int value) {
        __Mode = value;
    }

    /**
     * stat.st_rdev
     */
    private long __RDev;

    public long getRDev() {
        return __RDev;
    }

    public void setRDev(long value) {
        __RDev = value;
    }

    /**
     * Inode flags
     */
    private EnumSet<InodeFlag> __Flags;

    public EnumSet<InodeFlag> getFlags() {
        return __Flags;
    }

    public void setFlags(EnumSet<InodeFlag> value) {
        __Flags = value;
    }

    /**
     * Sequence number used for NFS compatibility. Initialized to 0 and
     * incremented each time mtime value is changed.
     */
    private long __Sequence;

    public long getSequence() {
        return __Sequence;
    }

    public void setSequence(long value) {
        __Sequence = value;
    }

    /**
     * stat.st_atime
     */
    private TimeSpec __ATime;

    public TimeSpec getATime() {
        return __ATime;
    }

    public void setATime(TimeSpec value) {
        __ATime = value;
    }

    /**
     * stat.st_ctime
     */
    private TimeSpec __CTime;

    public TimeSpec getCTime() {
        return __CTime;
    }

    public void setCTime(TimeSpec value) {
        __CTime = value;
    }

    /**
     * stat.st_mtime
     */
    private TimeSpec __MTime;

    public TimeSpec getMTime() {
        return __MTime;
    }

    public void setMTime(TimeSpec value) {
        __MTime = value;
    }

    /**
     * Timestamp of inode creation
     */
    private TimeSpec __OTime;

    public TimeSpec getOTime() {
        return __OTime;
    }

    public void setOTime(TimeSpec value) {
        __OTime = value;
    }

    public long getSize() {
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
        setATime(EndianUtilities.<TimeSpec> toStruct(TimeSpec.class, buffer, offset + 112));
        setCTime(EndianUtilities.<TimeSpec> toStruct(TimeSpec.class, buffer, offset + 124));
        setMTime(EndianUtilities.<TimeSpec> toStruct(TimeSpec.class, buffer, offset + 136));
        setOTime(EndianUtilities.<TimeSpec> toStruct(TimeSpec.class, buffer, offset + 148));
        return (int) getSize();
    }
}
