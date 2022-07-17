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

public class Nfs3GetAttributesResult extends Nfs3CallResult {
    public Nfs3GetAttributesResult() {
    }

    public Nfs3GetAttributesResult(XdrDataReader reader) {
        _status = Nfs3Status.valueOf(reader.readInt32());
        _attributes = new Nfs3FileAttributes(reader);
    }

    private Nfs3FileAttributes _attributes;

    public Nfs3FileAttributes getAttributes() {
        return _attributes;
    }

    public void setAttributes(Nfs3FileAttributes value) {
        _attributes = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(_status.getValue());
        if (_status == Nfs3Status.Ok) {
            _attributes.write(writer);
        }
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3GetAttributesResult ? (Nfs3GetAttributesResult) obj : null);
    }

    public boolean equals(Nfs3GetAttributesResult other) {
        if (other == null) {
            return false;
        }

        return other._status == _status && dotnet4j.util.compat.Utilities.equals(other._attributes, _attributes);
    }

    public int hashCode() {
        return dotnet4j.util.compat.Utilities.getCombinedHashCode(_status, _attributes);
    }
}
