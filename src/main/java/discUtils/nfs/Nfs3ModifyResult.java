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

public final class Nfs3ModifyResult extends Nfs3CallResult {
    public Nfs3ModifyResult() {
    }

    public Nfs3ModifyResult(XdrDataReader reader) {
        setStatus(Nfs3Status.valueOf(reader.readInt32()));
        setCacheConsistency(new Nfs3WeakCacheConsistency(reader));
    }

    private Nfs3WeakCacheConsistency cacheConsistency;

    public Nfs3WeakCacheConsistency getCacheConsistency() {
        return cacheConsistency;
    }

    public void setCacheConsistency(Nfs3WeakCacheConsistency value) {
        cacheConsistency = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(status.getValue());
        getCacheConsistency().write(writer);
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3ModifyResult ? (Nfs3ModifyResult) obj : null);
    }

    public boolean equals(Nfs3ModifyResult other) {
        if (other == null) {
            return false;
        }

        return other.getStatus() == getStatus() && other.getCacheConsistency().equals(getCacheConsistency());
    }

    public int hashCode() {
        return dotnet4j.util.compat.Utilities.getCombinedHashCode(getStatus(), getCacheConsistency());
    }
}
