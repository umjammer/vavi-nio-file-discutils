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

import java.util.Arrays;


public final class Nfs3ReadResult extends Nfs3CallResult {
    public Nfs3ReadResult(XdrDataReader reader) {
        setStatus(Nfs3Status.valueOf(reader.readInt32()));
        if (reader.readBool()) {
            setFileAttributes(new Nfs3FileAttributes(reader));
        }

        if (getStatus() == Nfs3Status.Ok) {
            setCount(reader.readInt32());
            setEof(reader.readBool());
            setData(reader.readBuffer());
        }
    }

    public Nfs3ReadResult() {
    }

    private int _count;

    public int getCount() {
        return _count;
    }

    public void setCount(int value) {
        _count = value;
    }

    private byte[] _data;

    public byte[] getData() {
        return _data;
    }

    public void setData(byte[] value) {
        _data = value;
    }

    private boolean _eof;

    public boolean getEof() {
        return _eof;
    }

    public void setEof(boolean value) {
        _eof = value;
    }

    private Nfs3FileAttributes _fileAttributes;

    public Nfs3FileAttributes getFileAttributes() {
        return _fileAttributes;
    }

    public void setFileAttributes(Nfs3FileAttributes value) {
        _fileAttributes = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(_status.getValue());
        writer.write(getFileAttributes() != null);
        if (getFileAttributes() != null) {
            getFileAttributes().write(writer);
        }

        if (getStatus() == Nfs3Status.Ok) {
            writer.write(getCount());
            writer.write(getEof());
            writer.writeBuffer(getData());
        }
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3ReadResult ? (Nfs3ReadResult) obj : null);
    }

    public boolean equals(Nfs3ReadResult other) {
        if (other == null) {
            return false;
        }

        return other.getStatus() == getStatus() && dotnet4j.io.compat.Utilities.equals(other.getFileAttributes(), getFileAttributes())
                && other.getCount() == getCount() && Arrays.equals(other.getData(), getData()) && other.getEof() == getEof();
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(getStatus(), getFileAttributes(), getCount(), getEof(), getData());
    }
}
