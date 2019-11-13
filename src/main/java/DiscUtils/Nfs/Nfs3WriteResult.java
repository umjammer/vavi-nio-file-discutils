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

import dotnet4j.io.compat.Utilities;

public final class Nfs3WriteResult extends Nfs3CallResult {
    public Nfs3WriteResult(XdrDataReader reader) {
        setStatus(Nfs3Status.valueOf(reader.readInt32()));
        setCacheConsistency(new Nfs3WeakCacheConsistency(reader));
        if (getStatus() == Nfs3Status.Ok) {
            setCount(reader.readInt32());
            setHowCommitted(Nfs3StableHow.values()[reader.readInt32()]);
            setWriteVerifier(reader.readUInt64());
        }
    }

    public Nfs3WriteResult() {
    }

    private Nfs3WeakCacheConsistency _cacheConsistency;

    public Nfs3WeakCacheConsistency getCacheConsistency() {
        return _cacheConsistency;
    }

    public void setCacheConsistency(Nfs3WeakCacheConsistency value) {
        _cacheConsistency = value;
    }

    private int _count;

    public int getCount() {
        return _count;
    }

    public void setCount(int value) {
        _count = value;
    }

    private Nfs3StableHow _howCommitted = Nfs3StableHow.Unstable;

    public Nfs3StableHow getHowCommitted() {
        return _howCommitted;
    }

    public void setHowCommitted(Nfs3StableHow value) {
        _howCommitted = value;
    }

    private long _writeVerifier;

    public long getWriteVerifier() {
        return _writeVerifier;
    }

    public void setWriteVerifier(long value) {
        _writeVerifier = value;
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

        return other._status == _status && other.getCacheConsistency().equals(getCacheConsistency()) &&
               other.getCount() == getCount() && other.getWriteVerifier() == getWriteVerifier() &&
               other.getHowCommitted() == getHowCommitted();
    }

    public int hashCode() {
        return Utilities.getCombinedHashCode(_status, _cacheConsistency, _count, _writeVerifier, _howCommitted);
    }
}
