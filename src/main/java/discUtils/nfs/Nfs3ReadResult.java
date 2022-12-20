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

    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(int value) {
        count = value;
    }

    private byte[] data;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] value) {
        data = value;
    }

    private boolean eof;

    public boolean getEof() {
        return eof;
    }

    public void setEof(boolean value) {
        eof = value;
    }

    private Nfs3FileAttributes fileAttributes;

    public Nfs3FileAttributes getFileAttributes() {
        return fileAttributes;
    }

    public void setFileAttributes(Nfs3FileAttributes value) {
        fileAttributes = value;
    }

    @Override public void write(XdrDataWriter writer) {
        writer.write(status.getValue());
        writer.write(fileAttributes != null);
        if (fileAttributes != null) {
            fileAttributes.write(writer);
        }

        if (status == Nfs3Status.Ok) {
            writer.write(count);
            writer.write(eof);
            writer.writeBuffer(data);
        }
    }

    @Override public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3ReadResult ? (Nfs3ReadResult) obj : null);
    }

    public boolean equals(Nfs3ReadResult other) {
        if (other == null) {
            return false;
        }

        return other.status == status && dotnet4j.util.compat.Utilities.equals(other.fileAttributes, fileAttributes)
                && other.count == count && Arrays.equals(other.data, data) && other.eof == eof;
    }

    @Override public int hashCode() {
        return dotnet4j.util.compat.Utilities.getCombinedHashCode(status, fileAttributes, count, eof, data);
    }
}
