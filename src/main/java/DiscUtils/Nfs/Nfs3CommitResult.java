//
// Copyright (c) 2017, Quamotion
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

public class Nfs3CommitResult extends Nfs3CallResult {
    public Nfs3CommitResult() {
    }

    private Nfs3WeakCacheConsistency __CacheConsistency;

    public Nfs3WeakCacheConsistency getCacheConsistency() {
        return __CacheConsistency;
    }

    public void setCacheConsistency(Nfs3WeakCacheConsistency value) {
        __CacheConsistency = value;
    }

    private long __WriteVerifier;

    public long getWriteVerifier() {
        return __WriteVerifier;
    }

    public void setWriteVerifier(long value) {
        __WriteVerifier = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(getStatus().ordinal());
        getCacheConsistency().write(writer);
        if (getStatus() == Nfs3Status.Ok) {
            writer.write(getWriteVerifier());
        }
    }
}
