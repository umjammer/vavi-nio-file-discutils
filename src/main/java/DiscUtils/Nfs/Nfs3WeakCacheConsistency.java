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

public final class Nfs3WeakCacheConsistency {
    public Nfs3WeakCacheConsistency(XdrDataReader reader) {
        if (reader.readBool()) {
            setBefore(new Nfs3WeakCacheConsistencyAttr(reader));
        }

        if (reader.readBool()) {
            setAfter(new Nfs3FileAttributes(reader));
        }
    }

    public Nfs3WeakCacheConsistency() {
    }

    private Nfs3FileAttributes after;

    public Nfs3FileAttributes getAfter() {
        return after;
    }

    public void setAfter(Nfs3FileAttributes value) {
        after = value;
    }

    private Nfs3WeakCacheConsistencyAttr before;

    public Nfs3WeakCacheConsistencyAttr getBefore() {
        return before;
    }

    public void setBefore(Nfs3WeakCacheConsistencyAttr value) {
        before = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(before != null);
        if (before != null) {
            before.write(writer);
        }

        writer.write(after != null);
        if (after != null) {
            after.write(writer);
        }
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3WeakCacheConsistency ? (Nfs3WeakCacheConsistency) obj
                                                              : null);
    }

    public boolean equals(Nfs3WeakCacheConsistency other) {
        if (other == null) {
            return false;
        }

        return dotnet4j.io.compat.Utilities.equals(other.after, after) && dotnet4j.io.compat.Utilities.equals(other.before, before);
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(after, before);
    }
}
