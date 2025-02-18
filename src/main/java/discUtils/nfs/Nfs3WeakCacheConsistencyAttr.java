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

public final class Nfs3WeakCacheConsistencyAttr {

    public Nfs3WeakCacheConsistencyAttr(XdrDataReader reader) {
        setSize(reader.readInt64());
        setModifyTime(new Nfs3FileTime(reader));
        setChangeTime(new Nfs3FileTime(reader));
    }

    public Nfs3WeakCacheConsistencyAttr() {
    }

    private Nfs3FileTime changeTime;

    public Nfs3FileTime getChangeTime() {
        return changeTime;
    }

    public void setChangeTime(Nfs3FileTime value) {
        changeTime = value;
    }

    private Nfs3FileTime modifyTime;

    public Nfs3FileTime getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(Nfs3FileTime value) {
        modifyTime = value;
    }

    private long size;

    public long getSize() {
        return size;
    }

    public void setSize(long value) {
        size = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(size);
        modifyTime.write(writer);
        changeTime.write(writer);
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3WeakCacheConsistencyAttr ? (Nfs3WeakCacheConsistencyAttr) obj
                                                                  : null);
    }

    public boolean equals(Nfs3WeakCacheConsistencyAttr other) {
        if (other == null) {
            return false;
        }

        return other.size == size && other.modifyTime.equals(modifyTime) &&
               other.changeTime.equals(changeTime);
    }

    public int hashCode() {
        return dotnet4j.util.compat.Utilities.getCombinedHashCode(size, modifyTime, changeTime);
    }
}
