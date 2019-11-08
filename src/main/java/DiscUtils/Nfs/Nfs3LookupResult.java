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

public final class Nfs3LookupResult extends Nfs3CallResult {
    public Nfs3LookupResult() {
    }

    public Nfs3LookupResult(XdrDataReader reader) {
        setStatus(Nfs3Status.valueOf(reader.readInt32()));
        if (getStatus() == Nfs3Status.Ok) {
            setObjectHandle(new Nfs3FileHandle(reader));
            if (reader.readBool()) {
                setObjectAttributes(new Nfs3FileAttributes(reader));
            }
        }

        if (reader.readBool()) {
            setDirAttributes(new Nfs3FileAttributes(reader));
        }
    }

    private Nfs3FileAttributes __DirAttributes;

    public Nfs3FileAttributes getDirAttributes() {
        return __DirAttributes;
    }

    public void setDirAttributes(Nfs3FileAttributes value) {
        __DirAttributes = value;
    }

    private Nfs3FileAttributes __ObjectAttributes;

    public Nfs3FileAttributes getObjectAttributes() {
        return __ObjectAttributes;
    }

    public void setObjectAttributes(Nfs3FileAttributes value) {
        __ObjectAttributes = value;
    }

    private Nfs3FileHandle __ObjectHandle;

    public Nfs3FileHandle getObjectHandle() {
        return __ObjectHandle;
    }

    public void setObjectHandle(Nfs3FileHandle value) {
        __ObjectHandle = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(this._status.getValue());
        if (getStatus() == Nfs3Status.Ok) {
            getObjectHandle().write(writer);
            writer.write(getObjectAttributes() != null);
            if (getObjectAttributes() != null) {
                getObjectAttributes().write(writer);
            }
        }

        writer.write(getDirAttributes() != null);
        if (getDirAttributes() != null) {
            getDirAttributes().write(writer);
        }
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3LookupResult ? (Nfs3LookupResult) obj : (Nfs3LookupResult) null);
    }

    public boolean equals(Nfs3LookupResult other) {
        if (other == null) {
            return false;
        }

        return other.getStatus() == getStatus() && other.getObjectHandle().equals(getObjectHandle()) &&
               other.getObjectAttributes().equals(getObjectAttributes()) && other.getDirAttributes().equals(getDirAttributes());
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(getStatus(), getObjectHandle(), getObjectAttributes(), getDirAttributes());
    }
}
