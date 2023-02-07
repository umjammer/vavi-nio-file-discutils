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

public class Nfs3CreateResult extends Nfs3CallResult {

    public Nfs3CreateResult(XdrDataReader reader) {
        setStatus(Nfs3Status.valueOf(reader.readInt32()));
        if (status == Nfs3Status.Ok) {
            if (reader.readBool()) {
                fileHandle = new Nfs3FileHandle(reader);
            }

            if (reader.readBool()) {
                fileAttributes = new Nfs3FileAttributes(reader);
            }
        }

        cacheConsistency = new Nfs3WeakCacheConsistency(reader);
    }

    public Nfs3CreateResult() {
    }

    private Nfs3WeakCacheConsistency cacheConsistency;

    public Nfs3WeakCacheConsistency getCacheConsistency() {
        return cacheConsistency;
    }

    public void setCacheConsistency(Nfs3WeakCacheConsistency value) {
        cacheConsistency = value;
    }

    private Nfs3FileAttributes fileAttributes;

    public Nfs3FileAttributes getFileAttributes() {
        return fileAttributes;
    }

    public void setFileAttributes(Nfs3FileAttributes value) {
        fileAttributes = value;
    }

    private Nfs3FileHandle fileHandle;

    public Nfs3FileHandle getFileHandle() {
        return fileHandle;
    }

    public void setFileHandle(Nfs3FileHandle value) {
        fileHandle = value;
    }

    @Override public void write(XdrDataWriter writer) {
        writer.write(status.getValue());
        if (status == Nfs3Status.Ok) {
            writer.write(fileHandle != null);
            if (fileHandle != null) {
                fileHandle.write(writer);
            }

            writer.write(fileAttributes != null);
            if (fileAttributes != null) {
                fileAttributes.write(writer);
            }
        }

        getCacheConsistency().write(writer);
    }

    @Override public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3CreateResult ? (Nfs3CreateResult) obj : null);
    }

    public boolean equals(Nfs3CreateResult other) {
        if (other == null) {
            return false;
        }

        return other.status == status && other.fileHandle.equals(fileHandle) &&
               dotnet4j.util.compat.Utilities.equals(other.fileAttributes, fileAttributes) &&
               other.cacheConsistency.equals(cacheConsistency);
    }

    @Override public int hashCode() {
        return dotnet4j.util.compat.Utilities
                .getCombinedHashCode(status, fileHandle, fileAttributes, cacheConsistency);
    }
}
