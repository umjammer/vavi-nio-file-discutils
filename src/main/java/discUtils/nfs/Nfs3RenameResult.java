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

public final class Nfs3RenameResult extends Nfs3CallResult {

    public Nfs3RenameResult(XdrDataReader reader) {
        setStatus(Nfs3Status.valueOf(reader.readInt32()));
        setFromDirCacheConsistency(new Nfs3WeakCacheConsistency(reader));
        setToDirCacheConsistency(new Nfs3WeakCacheConsistency(reader));
    }

    private Nfs3WeakCacheConsistency fromDirCacheConsistency;

    public Nfs3WeakCacheConsistency getFromDirCacheConsistency() {
        return fromDirCacheConsistency;
    }

    public void setFromDirCacheConsistency(Nfs3WeakCacheConsistency value) {
        fromDirCacheConsistency = value;
    }

    private Nfs3WeakCacheConsistency toDirCacheConsistency;

    public Nfs3WeakCacheConsistency getToDirCacheConsistency() {
        return toDirCacheConsistency;
    }

    public void setToDirCacheConsistency(Nfs3WeakCacheConsistency value) {
        toDirCacheConsistency = value;
    }
}
