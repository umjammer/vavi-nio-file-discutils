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

        setSetAccessTime(Nfs3SetTimeMethod.valueOf(reader.readInt32()));
        if (getSetAccessTime() == Nfs3SetTimeMethod.ClientTime) {
            setAccessTime(new Nfs3FileTime(reader));
        }

        setSetModifyTime(Nfs3SetTimeMethod.valueOf(reader.readInt32()));
        if (getSetModifyTime() == Nfs3SetTimeMethod.ClientTime) {
            setModifyTime(new Nfs3FileTime(reader));
        }
    }

    private Nfs3FileTime __AccessTime;

    public Nfs3FileTime getAccessTime() {
        return __AccessTime;
    }

    public void setAccessTime(Nfs3FileTime value) {
        __AccessTime = value;
    }

    private int __Gid;

    public int getGid() {
        return __Gid;
    }

    public void setGid(int value) {
        __Gid = value;
    }

    private EnumSet<UnixFilePermissions> __Mode;

    public EnumSet<UnixFilePermissions> getMode() {
        return __Mode;
    }

    public void setMode(EnumSet<UnixFilePermissions> value) {
        __Mode = value;
    }

    private Nfs3FileTime __ModifyTime;

    public Nfs3FileTime getModifyTime() {
        return __ModifyTime;
    }

    public void setModifyTime(Nfs3FileTime value) {
        __ModifyTime = value;
    }

    private Nfs3SetTimeMethod __SetAccessTime = Nfs3SetTimeMethod.NoChange;

    public Nfs3SetTimeMethod getSetAccessTime() {
        return __SetAccessTime;
    }

    public void setSetAccessTime(Nfs3SetTimeMethod value) {
        __SetAccessTime = value;
    }

    private boolean __SetGid;

    public boolean getSetGid() {
        return __SetGid;
    }

    public void setSetGid(boolean value) {
        __SetGid = value;
    }

    private boolean __SetMode;

    public boolean getSetMode() {
        return __SetMode;
    }

    public void setSetMode(boolean value) {
        __SetMode = value;
    }

    private Nfs3SetTimeMethod __SetModifyTime = Nfs3SetTimeMethod.NoChange;

    public Nfs3SetTimeMethod getSetModifyTime() {
        return __SetModifyTime;
    }

    public void setSetModifyTime(Nfs3SetTimeMethod value) {
        __SetModifyTime = value;
    }

    private boolean __SetSize;

    public boolean getSetSize() {
        return __SetSize;
    }

    public void setSetSize(boolean value) {
        __SetSize = value;
    }

    private boolean __SetUid;

    public boolean getSetUid() {
        return __SetUid;
    }

    public void setSetUid(boolean value) {
        __SetUid = value;
    }

    private long __Size;

    public long getSize() {
        return __Size;
    }

    public void setSize(long value) {
        __Size = value;
    }

    private int __Uid;

    public int getUid() {
        return __Uid;
    }

    public void setUid(int value) {
        __Uid = value;
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
        return Utilities.getCombinedHashCode(
                                          Utilities.getCombinedHashCode(getSetMode(),
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
