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


public final class Nfs3SetAttributes {
    public Nfs3SetAttributes() {
    }

    public Nfs3SetAttributes(XdrDataReader reader) {
        setSetMode(reader.readBool());
        if (getSetMode()) {
            setMode(UnixFilePermissions.valueOf(reader.readInt32()));
        }

        setSetUid(reader.readBool());
        if (getSetUid()) {
            setUid(reader.readUInt32());
        }

        setSetGid(reader.readBool());
        if (getSetGid()) {
            setGid(reader.readUInt32());
        }

        setSetSize(reader.readBool());
        if (getSetSize()) {
            setSize(reader.readInt64());
        }

        setSetAccessTime(Nfs3SetTimeMethod.values()[reader.readInt32()]);
        if (getSetAccessTime() == Nfs3SetTimeMethod.ClientTime) {
            setAccessTime(new Nfs3FileTime(reader));
        }

        setSetModifyTime(Nfs3SetTimeMethod.values()[reader.readInt32()]);
        if (getSetModifyTime() == Nfs3SetTimeMethod.ClientTime) {
            setModifyTime(new Nfs3FileTime(reader));
        }
    }

    private Nfs3FileTime _accessTime;

    public Nfs3FileTime getAccessTime() {
        return _accessTime;
    }

    public void setAccessTime(Nfs3FileTime value) {
        _accessTime = value;
    }

    private int _gid;

    public int getGid() {
        return _gid;
    }

    public void setGid(int value) {
        _gid = value;
    }

    private EnumSet<UnixFilePermissions> _mode;

    public EnumSet<UnixFilePermissions> getMode() {
        return _mode;
    }

    public void setMode(EnumSet<UnixFilePermissions> value) {
        _mode = value;
    }

    private Nfs3FileTime _modifyTime;

    public Nfs3FileTime getModifyTime() {
        return _modifyTime;
    }

    public void setModifyTime(Nfs3FileTime value) {
        _modifyTime = value;
    }

    private Nfs3SetTimeMethod _setAccessTime = Nfs3SetTimeMethod.NoChange;

    public Nfs3SetTimeMethod getSetAccessTime() {
        return _setAccessTime;
    }

    public void setSetAccessTime(Nfs3SetTimeMethod value) {
        _setAccessTime = value;
    }

    private boolean _setGid;

    public boolean getSetGid() {
        return _setGid;
    }

    public void setSetGid(boolean value) {
        _setGid = value;
    }

    private boolean _setMode;

    public boolean getSetMode() {
        return _setMode;
    }

    public void setSetMode(boolean value) {
        _setMode = value;
    }

    private Nfs3SetTimeMethod _setModifyTime = Nfs3SetTimeMethod.NoChange;

    public Nfs3SetTimeMethod getSetModifyTime() {
        return _setModifyTime;
    }

    public void setSetModifyTime(Nfs3SetTimeMethod value) {
        _setModifyTime = value;
    }

    private boolean _setSize;

    public boolean getSetSize() {
        return _setSize;
    }

    public void setSetSize(boolean value) {
        _setSize = value;
    }

    private boolean _setUid;

    public boolean getSetUid() {
        return _setUid;
    }

    public void setSetUid(boolean value) {
        _setUid = value;
    }

    private long _size;

    public long getSize() {
        return _size;
    }

    public void setSize(long value) {
        _size = value;
    }

    private int _uid;

    public int getUid() {
        return _uid;
    }

    public void setUid(int value) {
        _uid = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(getSetMode());
        if (getSetMode()) {
            writer.write((int) UnixFilePermissions.valueOf(getMode()));
        }

        writer.write(getSetUid());
        if (getSetUid()) {
            writer.write(getUid());
        }

        writer.write(getSetGid());
        if (getSetGid()) {
            writer.write(getGid());
        }

        writer.write(getSetSize());
        if (getSetSize()) {
            writer.write(getSize());
        }

        writer.write(getSetAccessTime().ordinal());
        if (getSetAccessTime() == Nfs3SetTimeMethod.ClientTime) {
            getAccessTime().write(writer);
        }

        writer.write(getSetModifyTime().ordinal());
        if (getSetModifyTime() == Nfs3SetTimeMethod.ClientTime) {
            getModifyTime().write(writer);
        }

    }

    public boolean equals(Object obj) {
        try {
            return equal(obj instanceof Nfs3SetAttributes ? (Nfs3SetAttributes) obj : (Nfs3SetAttributes) null);
        } catch (RuntimeException __dummyCatchVar0) {
            throw __dummyCatchVar0;
        } catch (Exception __dummyCatchVar0) {
            throw new RuntimeException(__dummyCatchVar0);
        }

    }

    public boolean equal(Nfs3SetAttributes other) {
        if (other == null) {
            return false;
        }

        return other.getSetMode() == getSetMode() && other.getMode().equals(getMode()) && other.getSetUid() == getSetUid() &&
               other.getUid() == getUid() && other.getSetGid() == getSetGid() && other.getGid() == getGid() &&
               other.getSetSize() == getSetSize() && other.getSize() == getSize() &&
               other.getSetAccessTime() == getSetAccessTime() && other.getAccessTime().equals(getAccessTime()) &&
               other.getSetModifyTime() == getSetModifyTime() && other.getModifyTime().equals(getModifyTime());
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(
                                          dotnet4j.io.compat.Utilities.getCombinedHashCode(getSetMode(),
                                                                     getModifyTime(),
                                                                     getSetUid(),
                                                                     getUid(),
                                                                     getSetGid(),
                                                                     getGid(),
                                                                     getSetSize(),
                                                                     getSize()),
                                          getSetAccessTime(),
                                          getAccessTime(),
                                          getSetModifyTime(),
                                          getModifyTime());
    }
}
