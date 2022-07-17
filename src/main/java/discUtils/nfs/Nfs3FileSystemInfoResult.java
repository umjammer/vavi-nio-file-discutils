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

public final class Nfs3FileSystemInfoResult extends Nfs3CallResult {
    public Nfs3FileSystemInfoResult(XdrDataReader reader) {
        setStatus(Nfs3Status.valueOf(reader.readInt32()));
        if (reader.readBool()) {
            setPostOpAttributes(new Nfs3FileAttributes(reader));
        }

        if (getStatus() == Nfs3Status.Ok) {
            setFileSystemInfo(new Nfs3FileSystemInfo(reader));
        }
    }

    public Nfs3FileSystemInfoResult() {
    }

    private Nfs3FileSystemInfo _fileSystemInfo;

    public Nfs3FileSystemInfo getFileSystemInfo() {
        return _fileSystemInfo;
    }

    public void setFileSystemInfo(Nfs3FileSystemInfo value) {
        _fileSystemInfo = value;
    }

    private Nfs3FileAttributes _postOpAttributes;

    public Nfs3FileAttributes getPostOpAttributes() {
        return _postOpAttributes;
    }

    public void setPostOpAttributes(Nfs3FileAttributes value) {
        _postOpAttributes = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(_status.getValue());
        writer.write(getPostOpAttributes() != null);
        if (getPostOpAttributes() != null) {
            getPostOpAttributes().write(writer);
        }

        if (getStatus() == Nfs3Status.Ok) {
            getFileSystemInfo().write(writer);
        }
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3FileSystemInfoResult ? (Nfs3FileSystemInfoResult) obj
                                                              : null);
    }

    public boolean equals(Nfs3FileSystemInfoResult other) {
        if (other == null) {
            return false;
        }

        return other.getStatus() == getStatus() && other.getPostOpAttributes().equals(getPostOpAttributes()) &&
               other.getStatus() == getStatus() && other.getFileSystemInfo().equals(getFileSystemInfo());
    }

    public int hashCode() {
        return dotnet4j.util.compat.Utilities.getCombinedHashCode(getStatus(), getPostOpAttributes(), getStatus(), getFileSystemInfo());
    }
}
