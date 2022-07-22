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

package discUtils.nfs;


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
            setInvariant(0xffff_ffff_ffff_ffffL);
            setInvariantUntil(0xffff_ffff_ffff_ffffL);
        } else {
            setInvariant(invarsec);
            setInvariantUntil(System.currentTimeMillis() + getInvariant());
        }
    }

    /**
     * The total size, in bytes, of the file system.
     */
    private long totalSizeBytes;

    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public void setTotalSizeBytes(long value) {
        totalSizeBytes = value;
    }

    /**
     * The amount of free space, in bytes, in the file system.
     */
    private long freeSpaceBytes;

    public long getFreeSpaceBytes() {
        return freeSpaceBytes;
    }

    public void setFreeSpaceBytes(long value) {
        freeSpaceBytes = value;
    }

    /**
     * The amount of free space, in bytes, available to the user identified by the
     * authentication information in the RPC. (This reflects space that is reserved
     * by the file system; it does not reflect any quota system implemented by the
     * server.)
     */
    private long availableFreeSpaceBytes;

    public long getAvailableFreeSpaceBytes() {
        return availableFreeSpaceBytes;
    }

    public void setAvailableFreeSpaceBytes(long value) {
        availableFreeSpaceBytes = value;
    }

    /**
     * The total number of file slots in the file system. (On a UNIX server, this
     * often corresponds to the number of inodes configured.)
     */
    private long fileSlotCount;

    public long getFileSlotCount() {
        return fileSlotCount;
    }

    public void setFileSlotCount(long value) {
        fileSlotCount = value;
    }

    /**
     * The number of free file slots in the file system.
     */
    private long freeFileSlotCount;

    public long getFreeFileSlotCount() {
        return freeFileSlotCount;
    }

    public void setFreeFileSlotCount(long value) {
        freeFileSlotCount = value;
    }

    /**
     * The number of free file slots that are available to the user corresponding to
     * the authentication information in the RPC. (This reflects slots that are
     * reserved by the file system; it does not reflect any quota system implemented
     * by the server.)
     */
    private long availableFreeFileSlotCount;

    public long getAvailableFreeFileSlotCount() {
        return availableFreeFileSlotCount;
    }

    public void setAvailableFreeFileSlotCount(long value) {
        availableFreeFileSlotCount = value;
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
    private long invariant;

    public long getInvariant() {
        return invariant;
    }

    public void setInvariant(long value) {
        invariant = value;
    }

    private long invariantUntil;

    public long getInvariantUntil() {
        return invariantUntil;
    }

    public void setInvariantUntil(long value) {
        invariantUntil = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(totalSizeBytes);
        writer.write(freeSpaceBytes);
        writer.write(availableFreeSpaceBytes);
        writer.write(fileSlotCount);
        writer.write(freeFileSlotCount);
        writer.write(availableFreeFileSlotCount);
        if (getInvariant() == 0xffff_ffff_ffff_ffffL) {
            writer.write(0xffff_ffff);
        } else {
            writer.write((int) invariant);
        }
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3FileSystemStat ? (Nfs3FileSystemStat) obj : null);
    }

    public boolean equals(Nfs3FileSystemStat other) {
        if (other == null) {
            return false;
        }

        return other.totalSizeBytes == totalSizeBytes && other.freeSpaceBytes == freeSpaceBytes
                && other.availableFreeSpaceBytes == availableFreeSpaceBytes
                && other.fileSlotCount == fileSlotCount && other.freeFileSlotCount == freeFileSlotCount
                && other.availableFreeFileSlotCount == availableFreeFileSlotCount
                && other.invariant == invariant;
    }

    public int hashCode() {
        return dotnet4j.util.compat.Utilities.getCombinedHashCode(totalSizeBytes,
                freeSpaceBytes,
                availableFreeSpaceBytes,
                fileSlotCount,
                freeFileSlotCount,
                availableFreeFileSlotCount,
                invariant);
    }
}
