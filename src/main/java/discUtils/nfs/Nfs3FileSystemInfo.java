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

package discUtils.nfs;


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
    private int directoryPreferredBytes;

    public int getDirectoryPreferredBytes() {
        return directoryPreferredBytes;
    }

    public void setDirectoryPreferredBytes(int value) {
        directoryPreferredBytes = value;
    }

    /**
     * A bit mask of file system properties.
     */
    private Nfs3FileSystemProperties fileSystemProperties = Nfs3FileSystemProperties.None;

    public Nfs3FileSystemProperties getFileSystemProperties() {
        return fileSystemProperties;
    }

    public void setFileSystemProperties(Nfs3FileSystemProperties value) {
        fileSystemProperties = value;
    }

    /**
     * The maximum size of a file on the file system.
     */
    private long maxFileSize;

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long value) {
        maxFileSize = value;
    }

    /**
     * The maximum size in bytes of a READ request supported
     * by the server. Any READ with a number greater than
     * rtmax will result in a short read of rtmax bytes or
     * less.
     */
    private int readMaxBytes;

    public int getReadMaxBytes() {
        return readMaxBytes;
    }

    public void setReadMaxBytes(int value) {
        readMaxBytes = value;
    }

    /**
     * The suggested multiple for the size of a READ request.
     */
    private int readMultipleSize;

    public int getReadMultipleSize() {
        return readMultipleSize;
    }

    public void setReadMultipleSize(int value) {
        readMultipleSize = value;
    }

    /**
     * The preferred size of a READ request. This should be
     * the same as rtmax unless there is a clear benefit in
     * performance or efficiency.
     */
    private int readPreferredBytes;

    public int getReadPreferredBytes() {
        return readPreferredBytes;
    }

    public void setReadPreferredBytes(int value) {
        readPreferredBytes = value;
    }

    /**
     * The server time granularity. When setting a file time
     * using SETATTR, the server guarantees only to preserve
     * times to this accuracy. If this is {0, 1}, the server
     * can support nanosecond times, {0, 1000000}
     * denotes millisecond precision, and {1, 0} indicates that times
     * are accurate only to the nearest second.
     */
    private Nfs3FileTime timePrecision;

    public Nfs3FileTime getTimePrecision() {
        return timePrecision;
    }

    public void setTimePrecision(Nfs3FileTime value) {
        timePrecision = value;
    }

    /**
     * The maximum size of a WRITE request supported by the
     * server. In general, the client is limited by wtmax
     * since there is no guarantee that a server can handle a
     * larger write. Any WRITE with a count greater than wtmax
     * will result in a short write of at most wtmax bytes.
     */
    private int writeMaxBytes;

    public int getWriteMaxBytes() {
        return writeMaxBytes;
    }

    public void setWriteMaxBytes(int value) {
        writeMaxBytes = value;
    }

    /**
     * The suggested multiple for the size of a WRITE
     * request.
     */
    private int writeMultipleSize;

    public int getWriteMultipleSize() {
        return writeMultipleSize;
    }

    public void setWriteMultipleSize(int value) {
        writeMultipleSize = value;
    }

    /**
     * The preferred size of a WRITE request. This should be
     * the same as wtmax unless there is a clear benefit in
     * performance or efficiency.
     */
    private int writePreferredBytes;

    public int getWritePreferredBytes() {
        return writePreferredBytes;
    }

    public void setWritePreferredBytes(int value) {
        writePreferredBytes = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(readMaxBytes);
        writer.write(readPreferredBytes);
        writer.write(readMultipleSize);
        writer.write(writeMaxBytes);
        writer.write(writePreferredBytes);
        writer.write(writeMultipleSize);
        writer.write(directoryPreferredBytes);
        writer.write(maxFileSize);
        timePrecision.write(writer);
        writer.write(fileSystemProperties.ordinal());
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3FileSystemInfo ? (Nfs3FileSystemInfo) obj : null);
    }

    public boolean equals(Nfs3FileSystemInfo other) {
        if (other == null) {
            return false;
        }

        return other.readMaxBytes == readMaxBytes && other.readPreferredBytes == readPreferredBytes &&
               other.readMultipleSize == readMultipleSize && other.writeMaxBytes == writeMaxBytes &&
               other.writePreferredBytes == writePreferredBytes &&
               other.writeMultipleSize == writeMultipleSize &&
               other.directoryPreferredBytes == directoryPreferredBytes &&
               other.maxFileSize == maxFileSize && other.timePrecision.equals(timePrecision) &&
               other.fileSystemProperties == fileSystemProperties;
    }

    public int hashCode() {
        return dotnet4j.util.compat.Utilities.getCombinedHashCode(
                dotnet4j.util.compat.Utilities.getCombinedHashCode(
                        readMaxBytes,
                        readPreferredBytes,
                        readMultipleSize,
                        writeMaxBytes,
                        writePreferredBytes,
                        writeMultipleSize,
                        directoryPreferredBytes,
                        maxFileSize),
                timePrecision,
                fileSystemProperties);
    }
}
