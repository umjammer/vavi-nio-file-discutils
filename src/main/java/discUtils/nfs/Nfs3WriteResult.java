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

import dotnet4j.util.compat.Utilities;

public final class Nfs3WriteResult extends Nfs3CallResult {
    public Nfs3WriteResult(XdrDataReader reader) {
        status = Nfs3Status.valueOf(reader.readInt32());
        cacheConsistency = new Nfs3WeakCacheConsistency(reader);
        if (status == Nfs3Status.Ok) {
            count = reader.readInt32();
            howCommitted = Nfs3StableHow.values()[reader.readInt32()];
            writeVerifier = reader.readUInt64();
        }
    }

    public Nfs3WriteResult() {
    }

    private Nfs3WeakCacheConsistency cacheConsistency;

    public Nfs3WeakCacheConsistency getCacheConsistency() {
        return cacheConsistency;
    }

    public void setCacheConsistency(Nfs3WeakCacheConsistency value) {
        cacheConsistency = value;
    }

    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(int value) {
        count = value;
    }

    private Nfs3StableHow howCommitted = Nfs3StableHow.Unstable;

    public Nfs3StableHow getHowCommitted() {
        return howCommitted;
    }

    public void setHowCommitted(Nfs3StableHow value) {
        howCommitted = value;
    }

    private long writeVerifier;

    public long getWriteVerifier() {
        return writeVerifier;
    }

    public void setWriteVerifier(long value) {
        writeVerifier = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(status.getValue());
        getCacheConsistency().write(writer);
        if (status == Nfs3Status.Ok) {
            writer.write(count);
            writer.write(howCommitted.ordinal());
            writer.write(writeVerifier);
        }
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3WriteResult ? (Nfs3WriteResult) obj : null);
    }

    public boolean equals(Nfs3WriteResult other) {
        if (other == null) {
            return false;
        }

        return other.status == status && other.cacheConsistency.equals(cacheConsistency) &&
               other.count == count && other.writeVerifier == writeVerifier &&
               other.howCommitted == howCommitted;
    }

    public int hashCode() {
        return Utilities.getCombinedHashCode(status, cacheConsistency, count, writeVerifier, howCommitted);
    }
}
