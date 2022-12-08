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

package discUtils.xva;

import java.io.IOException;
import java.security.MessageDigest;

import discUtils.streams.util.Ownership;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public class HashStreamDotnet extends Stream {

    private Stream wrapped;

    private Ownership ownWrapped;

    private MessageDigest hashAlg;

    private long hashPos;

    public HashStreamDotnet(Stream wrapped, Ownership ownsWrapped, MessageDigest hashAlg) {
        this.wrapped = wrapped;
        ownWrapped = ownsWrapped;
        this.hashAlg = hashAlg;
    }

    public boolean canRead() {
        return wrapped.canRead();
    }

    public boolean canSeek() {
        return wrapped.canSeek();
    }

    public boolean canWrite() {
        return wrapped.canWrite();
    }

    public long getLength() {
        return wrapped.getLength();
    }

    @Override public long position() {
        return wrapped.position();
    }

    @Override public void position(long value) {
        wrapped.position(value);
    }

    public void flush() {
        wrapped.flush();
    }

    public int read(byte[] buffer, int offset, int count) {
        if (position() != hashPos) {
            throw new UnsupportedOperationException("Reads must be contiguous");
        }

        int numRead = wrapped.read(buffer, offset, count);

        hashAlg.update(buffer, offset, numRead);
        hashPos += numRead;

        return numRead;
    }

    public long seek(long offset, SeekOrigin origin) {
        return wrapped.seek(offset, origin);
    }

    public void setLength(long value) {
        wrapped.setLength(value);
    }

    public void write(byte[] buffer, int offset, int count) {
        wrapped.write(buffer, offset, count);
    }

    public void close() throws IOException {
        if (ownWrapped == Ownership.Dispose && wrapped != null) {
            wrapped.close();
            wrapped = null;
        }
    }
}
