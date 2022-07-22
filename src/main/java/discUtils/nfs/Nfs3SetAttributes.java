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


public final class Nfs3SetAttributes {

    public Nfs3SetAttributes() {
    }

    public Nfs3SetAttributes(XdrDataReader reader) {
        setSetMode(reader.readBool());
        if (setMode) {
            mode = UnixFilePermissions.valueOf(reader.readInt32());
        }

        setSetUid(reader.readBool());
        if (setUid) {
            uid = reader.readUInt32();
        }

        setSetGid(reader.readBool());
        if (setGid) {
            gid = reader.readUInt32();
        }

        setSetSize(reader.readBool());
        if (setSize) {
            size = reader.readInt64();
        }

        setSetAccessTime(Nfs3SetTimeMethod.values()[reader.readInt32()]);
        if (setAccessTime == Nfs3SetTimeMethod.ClientTime) {
            accessTime = new Nfs3FileTime(reader);
        }

        setSetModifyTime(Nfs3SetTimeMethod.values()[reader.readInt32()]);
        if (setModifyTime == Nfs3SetTimeMethod.ClientTime) {
            modifyTime = new Nfs3FileTime(reader);
        }
    }

    private Nfs3FileTime accessTime;

    public Nfs3FileTime getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(Nfs3FileTime value) {
        accessTime = value;
    }

    private int gid;

    public int getGid() {
        return gid;
    }

    public void setGid(int value) {
        gid = value;
    }

    private EnumSet<UnixFilePermissions> mode;

    public EnumSet<UnixFilePermissions> getMode() {
        return mode;
    }

    public void setMode(EnumSet<UnixFilePermissions> value) {
        mode = value;
    }

    private Nfs3FileTime modifyTime;

    public Nfs3FileTime getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(Nfs3FileTime value) {
        modifyTime = value;
    }

    private Nfs3SetTimeMethod setAccessTime = Nfs3SetTimeMethod.NoChange;

    public Nfs3SetTimeMethod getSetAccessTime() {
        return setAccessTime;
    }

    public void setSetAccessTime(Nfs3SetTimeMethod value) {
        setAccessTime = value;
    }

    private boolean setGid;

    public boolean getSetGid() {
        return setGid;
    }

    public void setSetGid(boolean value) {
        setGid = value;
    }

    private boolean setMode;

    public boolean getSetMode() {
        return setMode;
    }

    public void setSetMode(boolean value) {
        setMode = value;
    }

    private Nfs3SetTimeMethod setModifyTime = Nfs3SetTimeMethod.NoChange;

    public Nfs3SetTimeMethod getSetModifyTime() {
        return setModifyTime;
    }

    public void setSetModifyTime(Nfs3SetTimeMethod value) {
        setModifyTime = value;
    }

    private boolean setSize;

    public boolean getSetSize() {
        return setSize;
    }

    public void setSetSize(boolean value) {
        setSize = value;
    }

    private boolean setUid;

    public boolean getSetUid() {
        return setUid;
    }

    public void setSetUid(boolean value) {
        setUid = value;
    }

    private long size;

    public long getSize() {
        return size;
    }

    public void setSize(long value) {
        size = value;
    }

    private int uid;

    public int getUid() {
        return uid;
    }

    public void setUid(int value) {
        uid = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(setMode);
        if (setMode) {
            writer.write((int) UnixFilePermissions.valueOf(mode));
        }

        writer.write(setUid);
        if (setUid) {
            writer.write(uid);
        }

        writer.write(setGid);
        if (setGid) {
            writer.write(gid);
        }

        writer.write(setSize);
        if (setSize) {
            writer.write(size);
        }

        writer.write(setAccessTime.ordinal());
        if (setAccessTime == Nfs3SetTimeMethod.ClientTime) {
            accessTime.write(writer);
        }

        writer.write(setModifyTime.ordinal());
        if (setModifyTime == Nfs3SetTimeMethod.ClientTime) {
            modifyTime.write(writer);
        }
    }

    public boolean equals(Object obj) {
        try {
            return equal(obj instanceof Nfs3SetAttributes ? (Nfs3SetAttributes) obj : null);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean equal(Nfs3SetAttributes other) {
        if (other == null) {
            return false;
        }

        return other.setMode == setMode && other.mode.equals(mode) && other.setUid == setUid &&
               other.uid == uid && other.setGid == setGid && other.gid == gid &&
               other.setSize == setSize && other.size == size &&
               other.setAccessTime == setAccessTime && other.accessTime.equals(accessTime) &&
               other.setModifyTime == setModifyTime && other.modifyTime.equals(modifyTime);
    }

    public int hashCode() {
        return dotnet4j.util.compat.Utilities.getCombinedHashCode(
                dotnet4j.util.compat.Utilities.getCombinedHashCode(
                        setMode,
                        modifyTime,
                        setUid,
                        uid,
                        setGid,
                        gid,
                        setSize,
                        size),
                setAccessTime,
                accessTime,
                setModifyTime,
                modifyTime);
    }
}
