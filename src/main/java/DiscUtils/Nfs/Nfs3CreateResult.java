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
        if (_status == Nfs3Status.Ok) {
            if (reader.readBool()) {
                _fileHandle = new Nfs3FileHandle(reader);
            }

            if (reader.readBool()) {
                _fileAttributes = new Nfs3FileAttributes(reader);
            }
        }

        _cacheConsistency = new Nfs3WeakCacheConsistency(reader);
    }

    public Nfs3CreateResult() {
    }

    private Nfs3WeakCacheConsistency _cacheConsistency;

    public Nfs3WeakCacheConsistency getCacheConsistency() {
        return _cacheConsistency;
    }

    public void setCacheConsistency(Nfs3WeakCacheConsistency value) {
        _cacheConsistency = value;
    }

    private Nfs3FileAttributes _fileAttributes;

    public Nfs3FileAttributes getFileAttributes() {
        return _fileAttributes;
    }

    public void setFileAttributes(Nfs3FileAttributes value) {
        _fileAttributes = value;
    }

    private Nfs3FileHandle _fileHandle;

    public Nfs3FileHandle getFileHandle() {
        return _fileHandle;
    }

    public void setFileHandle(Nfs3FileHandle value) {
        _fileHandle = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(_status.getValue());
        if (_status == Nfs3Status.Ok) {
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
        return equals(obj instanceof Nfs3CreateResult ? (Nfs3CreateResult) obj : null);
    }

    public boolean equals(Nfs3CreateResult other) {
        if (other == null) {
            return false;
        }

        return other._status == _status && other.getFileHandle().equals(getFileHandle()) &&
               dotnet4j.io.compat.Utilities.equals(other.getFileAttributes(), getFileAttributes()) &&
               other.getCacheConsistency().equals(getCacheConsistency());
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities
                .getCombinedHashCode(_status, getFileHandle(), getFileAttributes(), getCacheConsistency());
    }
}
