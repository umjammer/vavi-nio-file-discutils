//
// Copyright (c) 2017, Bianco Veigel
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

public class Nfs3FileSystemStatResult extends Nfs3CallResult {
    public Nfs3FileSystemStatResult() {
    }

    public Nfs3FileSystemStatResult(XdrDataReader reader) {
        setStatus(Nfs3Status.valueOf(reader.readInt32()));
        if (reader.readBool()) {
            _postOpAttributes = new Nfs3FileAttributes(reader);
        }

        if (getStatus() == Nfs3Status.Ok) {
            _fileSystemStat = new Nfs3FileSystemStat(reader);
        }
    }

    private Nfs3FileAttributes _postOpAttributes;

    public Nfs3FileAttributes getPostOpAttributes() {
        return _postOpAttributes;
    }

    public void setPostOpAttributes(Nfs3FileAttributes value) {
        _postOpAttributes = value;
    }

    private Nfs3FileSystemStat _fileSystemStat;

    public Nfs3FileSystemStat getFileSystemStat() {
        return _fileSystemStat;
    }

    public void setFileSystemStat(Nfs3FileSystemStat value) {
        _fileSystemStat = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(_status.getValue());
        writer.write(getPostOpAttributes() != null);
        if (getPostOpAttributes() != null) {
            getPostOpAttributes().write(writer);
        }

        if (getStatus() == Nfs3Status.Ok) {
            getFileSystemStat().write(writer);
        }
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3FileSystemStatResult ? (Nfs3FileSystemStatResult) obj
                                                              : null);
    }

    public boolean equals(Nfs3FileSystemStatResult other) {
        if (other == null) {
            return false;
        }

        return other.getStatus() == getStatus() && dotnet4j.io.compat.Utilities.equals(other.getPostOpAttributes(), getPostOpAttributes()) &&
               dotnet4j.io.compat.Utilities.equals(other.getFileSystemStat(), getFileSystemStat());
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(getStatus(), getPostOpAttributes(), getFileSystemStat());
    }
}
