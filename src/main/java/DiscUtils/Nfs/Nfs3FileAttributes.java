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

import java.util.EnumSet;

import DiscUtils.Core.UnixFilePermissions;
import DiscUtils.Core.Internal.Utilities;


public final class Nfs3FileAttributes {
    public Nfs3FileTime AccessTime;

    public long BytesUsed;

    public Nfs3FileTime ChangeTime;

    public long FileId;

    public long FileSystemId;

    public int Gid;

    public int LinkCount;

    public EnumSet<UnixFilePermissions> Mode = EnumSet.noneOf(UnixFilePermissions.class);

    public Nfs3FileTime ModifyTime;

    public int RdevMajor;

    public int RdevMinor;

    public long Size;

    public Nfs3FileType Type = Nfs3FileType.None;

    public int Uid;

    public Nfs3FileAttributes() {
    }

    public Nfs3FileAttributes(XdrDataReader reader) {
        Type = Nfs3FileType.valueOf(reader.readInt32());
        Mode = UnixFilePermissions.valueOf(reader.readInt32());
        LinkCount = reader.readUInt32();
        Uid = reader.readUInt32();
        Gid = reader.readUInt32();
        Size = reader.readInt64();
        BytesUsed = reader.readInt64();
        RdevMajor = reader.readUInt32();
        RdevMinor = reader.readUInt32();
        FileSystemId = reader.readUInt64();
        FileId = reader.readUInt64();
        AccessTime = new Nfs3FileTime(reader);
        ModifyTime = new Nfs3FileTime(reader);
        ChangeTime = new Nfs3FileTime(reader);
    }

    public void write(XdrDataWriter writer) {
        writer.write(Type.ordinal());
        writer.write((int) UnixFilePermissions.valueOf(Mode));
        writer.write(LinkCount);
        writer.write(Uid);
        writer.write(Gid);
        writer.write(Size);
        writer.write(BytesUsed);
        writer.write(RdevMajor);
        writer.write(RdevMinor);
        writer.write(FileSystemId);
        writer.write(FileId);
        AccessTime.write(writer);
        ModifyTime.write(writer);
        ChangeTime.write(writer);
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3FileAttributes ? (Nfs3FileAttributes) obj : (Nfs3FileAttributes) null);
    }

    public boolean equals(Nfs3FileAttributes other) {
        if (other == null) {
            return false;
        }
        return other.Type == Type && other.Mode.equals(Mode) && other.LinkCount == LinkCount && other.Uid == Uid &&
               other.Gid == Gid && other.Size == Size && other.BytesUsed == BytesUsed && other.RdevMajor == RdevMajor &&
               other.RdevMinor == RdevMinor && other.FileSystemId == FileSystemId && other.FileId == FileId &&
               other.AccessTime.equals(AccessTime) && other.ModifyTime.equals(ModifyTime) &&
               other.ChangeTime.equals(ChangeTime);
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities
                .getCombinedHashCode(dotnet4j.io.compat.Utilities.getCombinedHashCode(Type, Mode, LinkCount, Uid, Gid, Size, BytesUsed, RdevMajor),
                                  RdevMinor,
                                  FileSystemId,
                                  FileId,
                                  AccessTime,
                                  ModifyTime,
                                  ChangeTime);
    }
}
