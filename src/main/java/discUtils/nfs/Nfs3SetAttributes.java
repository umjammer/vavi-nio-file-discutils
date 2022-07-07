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
        if (_setMode) {
            _mode = UnixFilePermissions.valueOf(reader.readInt32());
        }

        setSetUid(reader.readBool());
        if (_setUid) {
            _uid = reader.readUInt32();
        }

        setSetGid(reader.readBool());
        if (_setGid) {
            _gid = reader.readUInt32();
        }

        setSetSize(reader.readBool());
        if (_setSize) {
            _size = reader.readInt64();
        }

        setSetAccessTime(Nfs3SetTimeMethod.values()[reader.readInt32()]);
        if (_setAccessTime == Nfs3SetTimeMethod.ClientTime) {
            _accessTime = new Nfs3FileTime(reader);
        }

        setSetModifyTime(Nfs3SetTimeMethod.values()[reader.readInt32()]);
        if (_setModifyTime == Nfs3SetTimeMethod.ClientTime) {
            _modifyTime = new Nfs3FileTime(reader);
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
        writer.write(_setMode);
        if (_setMode) {
            writer.write((int) UnixFilePermissions.valueOf(_mode));
        }

        writer.write(_setUid);
        if (_setUid) {
            writer.write(_uid);
        }

        writer.write(_setGid);
        if (_setGid) {
            writer.write(_gid);
        }

        writer.write(_setSize);
        if (_setSize) {
            writer.write(_size);
        }

        writer.write(_setAccessTime.ordinal());
        if (_setAccessTime == Nfs3SetTimeMethod.ClientTime) {
            _accessTime.write(writer);
        }

        writer.write(_setModifyTime.ordinal());
        if (_setModifyTime == Nfs3SetTimeMethod.ClientTime) {
            _modifyTime.write(writer);
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

        return other._setMode == _setMode && other._mode.equals(_mode) && other._setUid == _setUid &&
               other._uid == _uid && other._setGid == _setGid && other._gid == _gid &&
               other._setSize == _setSize && other._size == _size &&
               other._setAccessTime == _setAccessTime && other._accessTime.equals(_accessTime) &&
               other._setModifyTime == _setModifyTime && other._modifyTime.equals(_modifyTime);
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(
                dotnet4j.io.compat.Utilities.getCombinedHashCode(
                        _setMode,
                        _modifyTime,
                        _setUid,
                        _uid,
                        _setGid,
                        _gid,
                        _setSize,
                        _size),
                _setAccessTime,
                _accessTime,
                _setModifyTime,
                _modifyTime);
    }
}
