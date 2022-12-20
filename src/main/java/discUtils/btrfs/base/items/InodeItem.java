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
import vavi.util.ByteUtil;


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

    private long generation;

    public long getGeneration() {
        return generation;
    }

    public void setGeneration(long value) {
        generation = value;
    }

    private long transId;

    public long getTransId() {
        return transId;
    }

    public void setTransId(long value) {
        transId = value;
    }

    /**
     * Size of the file in bytes.
     */
    private long fileSize;

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long value) {
        fileSize = value;
    }

    /**
     * Size allocated to this file, in bytes;
     * Sum of the offset fields of all EXTENT_DATA items for this inode. For
     * directories: 0.
     */
    private long nBytes;

    public long getNBytes() {
        return nBytes;
    }

    public void setNBytes(long value) {
        nBytes = value;
    }

    /**
     * Unused for normal inodes. Contains byte offset of block group when used
     * as a free space inode.
     */
    private long blockGroup;

    public long getBlockGroup() {
        return blockGroup;
    }

    public void setBlockGroup(long value) {
        blockGroup = value;
    }

    /**
     * Count of INODE_REF entries for the inode. When used outside of a file
     * tree, 1.
     */
    private int linkCount;

    public int getLinkCount() {
        return linkCount;
    }

    public void setLinkCount(int value) {
        linkCount = value;
    }

    /**
     * stat.st_uid
     */
    private int uid;

    public int getUid() {
        return uid;
    }

    public void setUid(int value) {
        uid = value;
    }

    /**
     * stat.st_gid
     */
    private int gid;

    public int getGid() {
        return gid;
    }

    public void setGid(int value) {
        gid = value;
    }

    /**
     * stat.st_mode
     */
    private int mode;

    public int getMode() {
        return mode;
    }

    public void setMode(int value) {
        mode = value;
    }

    /**
     * stat.st_rdev
     */
    private long rDev;

    public long getRDev() {
        return rDev;
    }

    public void setRDev(long value) {
        rDev = value;
    }

    /**
     * Inode flags
     */
    private EnumSet<InodeFlag> flags;

    public EnumSet<InodeFlag> getFlags() {
        return flags;
    }

    public void setFlags(EnumSet<InodeFlag> value) {
        flags = value;
    }

    /**
     * Sequence number used for NFS compatibility. Initialized to 0 and
     * incremented each time mtime value is changed.
     */
    private long sequence;

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long value) {
        sequence = value;
    }

    /**
     * stat.st_atime
     */
    private TimeSpec aTime;

    public TimeSpec getATime() {
        return aTime;
    }

    public void setATime(TimeSpec value) {
        aTime = value;
    }

    /**
     * stat.st_ctime
     */
    private TimeSpec cTime;

    public TimeSpec getCTime() {
        return cTime;
    }

    public void setCTime(TimeSpec value) {
        cTime = value;
    }

    /**
     * stat.st_mtime
     */
    private TimeSpec mTime;

    public TimeSpec getMTime() {
        return mTime;
    }

    public void setMTime(TimeSpec value) {
        mTime = value;
    }

    /**
     * Timestamp of inode creation
     */
    private TimeSpec oTime;

    public TimeSpec getOTime() {
        return oTime;
    }

    public void setOTime(TimeSpec value) {
        oTime = value;
    }

    @Override public int size() {
        return Length;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        generation = ByteUtil.readLeLong(buffer, offset);
        transId = ByteUtil.readLeLong(buffer, offset + 8);
        fileSize = ByteUtil.readLeLong(buffer, offset + 16);
        nBytes = ByteUtil.readLeLong(buffer, offset + 24);
        blockGroup = ByteUtil.readLeLong(buffer, offset + 32);
        linkCount = ByteUtil.readLeInt(buffer, offset + 40);
        uid = ByteUtil.readLeInt(buffer, offset + 44);
        gid = ByteUtil.readLeInt(buffer, offset + 48);
        mode = ByteUtil.readLeInt(buffer, offset + 52);
        rDev = ByteUtil.readLeLong(buffer, offset + 56);
        flags = InodeFlag.valueOf((int) ByteUtil.readLeLong(buffer, offset + 64));
        sequence = ByteUtil.readLeLong(buffer, offset + 72);
        aTime = EndianUtilities.toStruct(TimeSpec.class, buffer, offset + 112);
        cTime = EndianUtilities.toStruct(TimeSpec.class, buffer, offset + 124);
        mTime = EndianUtilities.toStruct(TimeSpec.class, buffer, offset + 136);
        oTime = EndianUtilities.toStruct(TimeSpec.class, buffer, offset + 148);
        return size();
    }
}
