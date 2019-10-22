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

package DiscUtils.Nfs;

import DiscUtils.Core.Internal.Utilities;


public final class Nfs3FileSystemStat {
    public Nfs3FileSystemStat() {
    }

    public Nfs3FileSystemStat(XdrDataReader reader) {
        setTotalSizeBytes(reader.readUInt64());
        setFreeSpaceBytes(reader.readUInt64());
        setAvailableFreeSpaceBytes(reader.readUInt64());
        setFileSlotCount(reader.readUInt64());
        setFreeFileSlotCount(reader.readUInt64());
        setAvailableFreeFileSlotCount(reader.readUInt64());
        int invarsec = reader.readUInt32();
        if (invarsec == 0xffffffff) {
            setInvariant(0xffffffffffffffffl);
            setInvariantUntil(0xffffffffffffffffl);
        } else {
            setInvariant(invarsec);
            setInvariantUntil(System.currentTimeMillis() + getInvariant());
        }
    }

    /**
     * The total size, in bytes, of the file system.
     */
    private long __TotalSizeBytes;

    public long getTotalSizeBytes() {
        return __TotalSizeBytes;
    }

    public void setTotalSizeBytes(long value) {
        __TotalSizeBytes = value;
    }

    /**
     * The amount of free space, in bytes, in the file system.
     */
    private long __FreeSpaceBytes;

    public long getFreeSpaceBytes() {
        return __FreeSpaceBytes;
    }

    public void setFreeSpaceBytes(long value) {
        __FreeSpaceBytes = value;
    }

    /**
     * The amount of free space, in bytes, available to the user identified by the
     * authentication information in the RPC. (This reflects space that is reserved
     * by the file system; it does not reflect any quota system implemented by the
     * server.)
     */
    private long __AvailableFreeSpaceBytes;

    public long getAvailableFreeSpaceBytes() {
        return __AvailableFreeSpaceBytes;
    }

    public void setAvailableFreeSpaceBytes(long value) {
        __AvailableFreeSpaceBytes = value;
    }

    /**
     * The total number of file slots in the file system. (On a UNIX server, this
     * often corresponds to the number of inodes configured.)
     */
    private long __FileSlotCount;

    public long getFileSlotCount() {
        return __FileSlotCount;
    }

    public void setFileSlotCount(long value) {
        __FileSlotCount = value;
    }

    /**
     * The number of free file slots in the file system.
     */
    private long __FreeFileSlotCount;

    public long getFreeFileSlotCount() {
        return __FreeFileSlotCount;
    }

    public void setFreeFileSlotCount(long value) {
        __FreeFileSlotCount = value;
    }

    /**
     * The number of free file slots that are available to the user corresponding to
     * the authentication information in the RPC. (This reflects slots that are
     * reserved by the file system; it does not reflect any quota system implemented
     * by the server.)
     */
    private long __AvailableFreeFileSlotCount;

    public long getAvailableFreeFileSlotCount() {
        return __AvailableFreeFileSlotCount;
    }

    public void setAvailableFreeFileSlotCount(long value) {
        __AvailableFreeFileSlotCount = value;
    }

    /**
     * A measure of file system volatility: this is the number of seconds for which
     * the file system is not expected to change.For a volatile, frequently updated
     * file system, this will be 0. For an immutable file system, such as a CD-ROM,
     * this would be the largest unsigned integer.For file systems that are
     * infrequently modified, for example, one containing local executable programs
     * and on-line documentation, a value corresponding to a few hours or days might
     * be used. The client may use this as a hint in tuning its cache management.
     * Note however, this measure is assumed to be dynamic and may change at any
     * time.
     */
    private long __Invariant;

    public long getInvariant() {
        return __Invariant;
    }

    public void setInvariant(long value) {
        __Invariant = value;
    }

    private long __InvariantUntil;

    public long getInvariantUntil() {
        return __InvariantUntil;
    }

    public void setInvariantUntil(long value) {
        __InvariantUntil = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(getTotalSizeBytes());
        writer.write(getFreeSpaceBytes());
        writer.write(getAvailableFreeSpaceBytes());
        writer.write(getFileSlotCount());
        writer.write(getFreeFileSlotCount());
        writer.write(getAvailableFreeFileSlotCount());
        if (getInvariant() == 0xffffffffffffffffl) {
            writer.write(0xffffffff);
        } else {
            writer.write((int) getInvariant());
        }
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3FileSystemStat ? (Nfs3FileSystemStat) obj : (Nfs3FileSystemStat) null);
    }

    public boolean equals(Nfs3FileSystemStat other) {
        if (other == null) {
            return false;
        }

        return other.getTotalSizeBytes() == getTotalSizeBytes() && other.getFreeSpaceBytes() == getFreeSpaceBytes()
                && other.getAvailableFreeSpaceBytes() == getAvailableFreeSpaceBytes()
                && other.getFileSlotCount() == getFileSlotCount() && other.getFreeFileSlotCount() == getFreeFileSlotCount()
                && other.getAvailableFreeFileSlotCount() == getAvailableFreeFileSlotCount()
                && other.getInvariant() == getInvariant();
    }

    public int hashCode() {
        return Utilities.getCombinedHashCode(getTotalSizeBytes(),
                                             getFreeSpaceBytes(),
                                             getAvailableFreeSpaceBytes(),
                                             getFileSlotCount(),
                                             getFreeFileSlotCount(),
                                             getAvailableFreeFileSlotCount(),
                                             getInvariant());
    }
}
