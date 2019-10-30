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

public final class Nfs3WeakCacheConsistencyAttr {
    public Nfs3WeakCacheConsistencyAttr(XdrDataReader reader) {
        setSize(reader.readInt64());
        setModifyTime(new Nfs3FileTime(reader));
        setChangeTime(new Nfs3FileTime(reader));
    }

    public Nfs3WeakCacheConsistencyAttr() {
    }

    private Nfs3FileTime __ChangeTime;

    public Nfs3FileTime getChangeTime() {
        return __ChangeTime;
    }

    public void setChangeTime(Nfs3FileTime value) {
        __ChangeTime = value;
    }

    private Nfs3FileTime __ModifyTime;

    public Nfs3FileTime getModifyTime() {
        return __ModifyTime;
    }

    public void setModifyTime(Nfs3FileTime value) {
        __ModifyTime = value;
    }

    private long __Size;

    public long getSize() {
        return __Size;
    }

    public void setSize(long value) {
        __Size = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(getSize());
        getModifyTime().write(writer);
        getChangeTime().write(writer);
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3WeakCacheConsistencyAttr ? (Nfs3WeakCacheConsistencyAttr) obj
                                                                  : (Nfs3WeakCacheConsistencyAttr) null);
    }

    public boolean equals(Nfs3WeakCacheConsistencyAttr other) {
        if (other == null) {
            return false;
        }

        return other.getSize() == getSize() && other.getModifyTime().equals(getModifyTime()) &&
               other.getChangeTime().equals(getChangeTime());
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(getSize(), getModifyTime(), getChangeTime());
    }
}
