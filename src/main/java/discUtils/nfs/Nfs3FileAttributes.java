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

import java.util.EnumSet;

import discUtils.core.UnixFilePermissions;


public final class Nfs3FileAttributes {

    public Nfs3FileTime accessTime;

    public long bytesUsed;

    public Nfs3FileTime changeTime;

    public long fileId;

    public long fileSystemId;

    public int gid;

    public int linkCount;

    public EnumSet<UnixFilePermissions> mode = EnumSet.noneOf(UnixFilePermissions.class);

    public Nfs3FileTime modifyTime;

    public int rdevMajor;

    public int rdevMinor;

    public long size;

    public Nfs3FileType type = Nfs3FileType.None;

    public int uid;

    public Nfs3FileAttributes() {
    }

    public Nfs3FileAttributes(XdrDataReader reader) {
        type = Nfs3FileType.valueOf(reader.readInt32());
        mode = UnixFilePermissions.valueOf(reader.readInt32());
        linkCount = reader.readUInt32();
        uid = reader.readUInt32();
        gid = reader.readUInt32();
        size = reader.readInt64();
        bytesUsed = reader.readInt64();
        rdevMajor = reader.readUInt32();
        rdevMinor = reader.readUInt32();
        fileSystemId = reader.readUInt64();
        fileId = reader.readUInt64();
        accessTime = new Nfs3FileTime(reader);
        modifyTime = new Nfs3FileTime(reader);
        changeTime = new Nfs3FileTime(reader);
    }

    public void write(XdrDataWriter writer) {
        writer.write(type.ordinal());
        writer.write((int) UnixFilePermissions.valueOf(mode));
        writer.write(linkCount);
        writer.write(uid);
        writer.write(gid);
        writer.write(size);
        writer.write(bytesUsed);
        writer.write(rdevMajor);
        writer.write(rdevMinor);
        writer.write(fileSystemId);
        writer.write(fileId);
        accessTime.write(writer);
        modifyTime.write(writer);
        changeTime.write(writer);
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3FileAttributes ? (Nfs3FileAttributes) obj : null);
    }

    public boolean equals(Nfs3FileAttributes other) {
        if (other == null) {
            return false;
        }
        return other.type == type && other.mode.equals(mode) && other.linkCount == linkCount && other.uid == uid &&
               other.gid == gid && other.size == size && other.bytesUsed == bytesUsed && other.rdevMajor == rdevMajor &&
               other.rdevMinor == rdevMinor && other.fileSystemId == fileSystemId && other.fileId == fileId &&
               other.accessTime.equals(accessTime) && other.modifyTime.equals(modifyTime) &&
               other.changeTime.equals(changeTime);
    }

    public int hashCode() {
        return dotnet4j.util.compat.Utilities
                .getCombinedHashCode(dotnet4j.util.compat.Utilities.getCombinedHashCode(type, mode, linkCount, uid, gid, size, bytesUsed, rdevMajor),
                        rdevMinor,
                        fileSystemId,
                        fileId,
                        accessTime,
                        modifyTime,
                        changeTime);
    }
}
