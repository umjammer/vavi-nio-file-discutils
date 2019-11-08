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

public final class Nfs3WriteResult extends Nfs3CallResult {
    public Nfs3WriteResult(XdrDataReader reader) {
        setStatus(Nfs3Status.valueOf(reader.readInt32()));
        setCacheConsistency(new Nfs3WeakCacheConsistency(reader));
        if (getStatus() == Nfs3Status.Ok) {
            setCount(reader.readInt32());
            setHowCommitted(Nfs3StableHow.valueOf(reader.readInt32()));
            setWriteVerifier(reader.readUInt64());
        }
    }

    public Nfs3WriteResult() {
    }

    private Nfs3WeakCacheConsistency __CacheConsistency;

    public Nfs3WeakCacheConsistency getCacheConsistency() {
        return __CacheConsistency;
    }

    public void setCacheConsistency(Nfs3WeakCacheConsistency value) {
        __CacheConsistency = value;
    }

    private int __Count;

    public int getCount() {
        return __Count;
    }

    public void setCount(int value) {
        __Count = value;
    }

    private Nfs3StableHow __HowCommitted = Nfs3StableHow.Unstable;

    public Nfs3StableHow getHowCommitted() {
        return __HowCommitted;
    }

    public void setHowCommitted(Nfs3StableHow value) {
        __HowCommitted = value;
    }

    private long __WriteVerifier;

    public long getWriteVerifier() {
        return __WriteVerifier;
    }

    public void setWriteVerifier(long value) {
        __WriteVerifier = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(_status.getValue());
        getCacheConsistency().write(writer);
        if (getStatus() == Nfs3Status.Ok) {
            writer.write(getCount());
            writer.write(getHowCommitted().ordinal());
            writer.write(getWriteVerifier());
        }

    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3WriteResult ? (Nfs3WriteResult) obj : (Nfs3WriteResult) null);
    }

    public boolean equals(Nfs3WriteResult other) {
        if (other == null) {
            return false;
        }

        return other.getStatus() == getStatus() && other.getCacheConsistency().equals(getCacheConsistency()) &&
               other.getCount() == getCount() && other.getWriteVerifier() == getWriteVerifier() &&
               other.getHowCommitted() == getHowCommitted();
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(getStatus(), getCacheConsistency(), getCount(), getWriteVerifier(), getHowCommitted());
    }
}
