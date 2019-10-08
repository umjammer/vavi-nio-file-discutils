//
// Copyright (c) 2008-2011, Kenneth Bell
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


public final class Nfs3FileSystemInfo {
    public Nfs3FileSystemInfo(XdrDataReader reader) {
        setReadMaxBytes(reader.readUInt32());
        setReadPreferredBytes(reader.readUInt32());
        setReadMultipleSize(reader.readUInt32());
        setWriteMaxBytes(reader.readUInt32());
        setWritePreferredBytes(reader.readUInt32());
        setWriteMultipleSize(reader.readUInt32());
        setDirectoryPreferredBytes(reader.readUInt32());
        setMaxFileSize(reader.readInt64());
        setTimePrecision(new Nfs3FileTime(reader));
        setFileSystemProperties(Nfs3FileSystemProperties.valueOf(reader.readInt32()));
    }

    public Nfs3FileSystemInfo() {
    }

    /**
     * The preferred size of a READDIR request.
     */
    private int __DirectoryPreferredBytes;

    public int getDirectoryPreferredBytes() {
        return __DirectoryPreferredBytes;
    }

    public void setDirectoryPreferredBytes(int value) {
        __DirectoryPreferredBytes = value;
    }

    /**
     * A bit mask of file system properties.
     */
    private Nfs3FileSystemProperties __FileSystemProperties = Nfs3FileSystemProperties.None;

    public Nfs3FileSystemProperties getFileSystemProperties() {
        return __FileSystemProperties;
    }

    public void setFileSystemProperties(Nfs3FileSystemProperties value) {
        __FileSystemProperties = value;
    }

    /**
     * The maximum size of a file on the file system.
     */
    private long __MaxFileSize;

    public long getMaxFileSize() {
        return __MaxFileSize;
    }

    public void setMaxFileSize(long value) {
        __MaxFileSize = value;
    }

    /**
     * The maximum size in bytes of a READ request supported
     * by the server. Any READ with a number greater than
     * rtmax will result in a short read of rtmax bytes or
     * less.
     */
    private int __ReadMaxBytes;

    public int getReadMaxBytes() {
        return __ReadMaxBytes;
    }

    public void setReadMaxBytes(int value) {
        __ReadMaxBytes = value;
    }

    /**
     * The suggested multiple for the size of a READ request.
     */
    private int __ReadMultipleSize;

    public int getReadMultipleSize() {
        return __ReadMultipleSize;
    }

    public void setReadMultipleSize(int value) {
        __ReadMultipleSize = value;
    }

    /**
     * The preferred size of a READ request. This should be
     * the same as rtmax unless there is a clear benefit in
     * performance or efficiency.
     */
    private int __ReadPreferredBytes;

    public int getReadPreferredBytes() {
        return __ReadPreferredBytes;
    }

    public void setReadPreferredBytes(int value) {
        __ReadPreferredBytes = value;
    }

    /**
     * The server time granularity. When setting a file time
     * using SETATTR, the server guarantees only to preserve
     * times to this accuracy. If this is {0, 1}, the server
     * can support nanosecond times, {0, 1000000}
     * denotes millisecond precision, and {1, 0} indicates that times
     * are accurate only to the nearest second.
     */
    private Nfs3FileTime __TimePrecision;

    public Nfs3FileTime getTimePrecision() {
        return __TimePrecision;
    }

    public void setTimePrecision(Nfs3FileTime value) {
        __TimePrecision = value;
    }

    /**
     * The maximum size of a WRITE request supported by the
     * server. In general, the client is limited by wtmax
     * since there is no guarantee that a server can handle a
     * larger write. Any WRITE with a count greater than wtmax
     * will result in a short write of at most wtmax bytes.
     */
    private int __WriteMaxBytes;

    public int getWriteMaxBytes() {
        return __WriteMaxBytes;
    }

    public void setWriteMaxBytes(int value) {
        __WriteMaxBytes = value;
    }

    /**
     * The suggested multiple for the size of a WRITE
     * request.
     */
    private int __WriteMultipleSize;

    public int getWriteMultipleSize() {
        return __WriteMultipleSize;
    }

    public void setWriteMultipleSize(int value) {
        __WriteMultipleSize = value;
    }

    /**
     * The preferred size of a WRITE request. This should be
     * the same as wtmax unless there is a clear benefit in
     * performance or efficiency.
     */
    private int __WritePreferredBytes;

    public int getWritePreferredBytes() {
        return __WritePreferredBytes;
    }

    public void setWritePreferredBytes(int value) {
        __WritePreferredBytes = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(getReadMaxBytes());
        writer.write(getReadPreferredBytes());
        writer.write(getReadMultipleSize());
        writer.write(getWriteMaxBytes());
        writer.write(getWritePreferredBytes());
        writer.write(getWriteMultipleSize());
        writer.write(getDirectoryPreferredBytes());
        writer.write(getMaxFileSize());
        getTimePrecision().write(writer);
        writer.write(getFileSystemProperties().ordinal());
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3FileSystemInfo ? (Nfs3FileSystemInfo) obj : (Nfs3FileSystemInfo) null);
    }

    public boolean equals(Nfs3FileSystemInfo other) {
        if (other == null) {
            return false;
        }

        return other.getReadMaxBytes() == getReadMaxBytes() && other.getReadPreferredBytes() == getReadPreferredBytes() &&
               other.getReadMultipleSize() == getReadMultipleSize() && other.getWriteMaxBytes() == getWriteMaxBytes() &&
               other.getWritePreferredBytes() == getWritePreferredBytes() &&
               other.getWriteMultipleSize() == getWriteMultipleSize() &&
               other.getDirectoryPreferredBytes() == getDirectoryPreferredBytes() &&
               other.getMaxFileSize() == getMaxFileSize() && other.getTimePrecision().equals(getTimePrecision()) &&
               other.getFileSystemProperties() == getFileSystemProperties();
    }

    public int hashCode() {
        return Utilities.getCombinedHashCode(
                                          Utilities.getCombinedHashCode(getReadMaxBytes(),
                                                                     getReadPreferredBytes(),
                                                                     getReadMultipleSize(),
                                                                     getWriteMaxBytes(),
                                                                     getWritePreferredBytes(),
                                                                     getWriteMultipleSize(),
                                                                     getDirectoryPreferredBytes(),
                                                                     getMaxFileSize()),
                                          getTimePrecision(),
                                          getFileSystemProperties());
    }
}
