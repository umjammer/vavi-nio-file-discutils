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


public class Nfs3CreateResult extends Nfs3CallResult {
    public Nfs3CreateResult(XdrDataReader reader) {
        setStatus(Nfs3Status.valueOf(reader.readInt32()));
        if (getStatus() == Nfs3Status.Ok) {
            if (reader.readBool()) {
                setFileHandle(new Nfs3FileHandle(reader));
            }

            if (reader.readBool()) {
                setFileAttributes(new Nfs3FileAttributes(reader));
            }
        }

        setCacheConsistency(new Nfs3WeakCacheConsistency(reader));
    }

    public Nfs3CreateResult() {
    }

    private Nfs3WeakCacheConsistency __CacheConsistency;

    public Nfs3WeakCacheConsistency getCacheConsistency() {
        return __CacheConsistency;
    }

    public void setCacheConsistency(Nfs3WeakCacheConsistency value) {
        __CacheConsistency = value;
    }

    private Nfs3FileAttributes __FileAttributes;

    public Nfs3FileAttributes getFileAttributes() {
        return __FileAttributes;
    }

    public void setFileAttributes(Nfs3FileAttributes value) {
        __FileAttributes = value;
    }

    private Nfs3FileHandle __FileHandle;

    public Nfs3FileHandle getFileHandle() {
        return __FileHandle;
    }

    public void setFileHandle(Nfs3FileHandle value) {
        __FileHandle = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(getStatus().ordinal());
        if (getStatus() == Nfs3Status.Ok) {
            writer.write(getFileHandle() != null);
            if (getFileHandle() != null) {
                getFileHandle().write(writer);
            }

            writer.write(getFileAttributes() != null);
            if (getFileAttributes() != null) {
                getFileAttributes().write(writer);
            }
        }

        getCacheConsistency().write(writer);
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3CreateResult ? (Nfs3CreateResult) obj : (Nfs3CreateResult) null);
    }

    public boolean equals(Nfs3CreateResult other) {
        if (other == null) {
            return false;
        }

        return other.getStatus() == getStatus() && other.getFileHandle().equals(getFileHandle()) &&
               dotnet4j.io.compat.Utilities.equals(other.getFileAttributes(), getFileAttributes()) &&
               other.getCacheConsistency().equals(getCacheConsistency());
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(getStatus(),
                                         getFileHandle(),
                                         getFileAttributes(),
                                         getCacheConsistency());
    }
}
