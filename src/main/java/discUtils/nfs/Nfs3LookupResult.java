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

public final class Nfs3LookupResult extends Nfs3CallResult {

    public Nfs3LookupResult() {
    }

    public Nfs3LookupResult(XdrDataReader reader) {
        status = Nfs3Status.valueOf(reader.readInt32());
        if (status == Nfs3Status.Ok) {
            objectHandle = new Nfs3FileHandle(reader);
            if (reader.readBool()) {
                objectAttributes = new Nfs3FileAttributes(reader);
            }
        }

        if (reader.readBool()) {
            dirAttributes = new Nfs3FileAttributes(reader);
        }
    }

    private Nfs3FileAttributes dirAttributes;

    public Nfs3FileAttributes getDirAttributes() {
        return dirAttributes;
    }

    public void setDirAttributes(Nfs3FileAttributes value) {
        dirAttributes = value;
    }

    private Nfs3FileAttributes objectAttributes;

    public Nfs3FileAttributes getObjectAttributes() {
        return objectAttributes;
    }

    public void setObjectAttributes(Nfs3FileAttributes value) {
        objectAttributes = value;
    }

    private Nfs3FileHandle objectHandle;

    public Nfs3FileHandle getObjectHandle() {
        return objectHandle;
    }

    public void setObjectHandle(Nfs3FileHandle value) {
        objectHandle = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(this.status.getValue());
        if (status == Nfs3Status.Ok) {
            objectHandle.write(writer);
            writer.write(objectAttributes != null);
            if (objectAttributes != null) {
                objectAttributes.write(writer);
            }
        }

        writer.write(dirAttributes != null);
        if (dirAttributes != null) {
            dirAttributes.write(writer);
        }
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3LookupResult ? (Nfs3LookupResult) obj : null);
    }

    public boolean equals(Nfs3LookupResult other) {
        if (other == null) {
            return false;
        }

        return other.status == status && other.objectHandle.equals(objectHandle) &&
               other.objectAttributes.equals(objectAttributes) && other.dirAttributes.equals(dirAttributes);
    }

    public int hashCode() {
        return dotnet4j.util.compat.Utilities.getCombinedHashCode(status, objectHandle, objectAttributes, dirAttributes);
    }
}
