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

package DiscUtils.Xva;

import java.io.IOException;
import java.security.MessageDigest;

import DiscUtils.Streams.Util.Ownership;
import moe.yo3explorer.dotnetio4j.SeekOrigin;
import moe.yo3explorer.dotnetio4j.Stream;


public class HashStreamDotnet extends Stream {
    private Stream _wrapped;

    private Ownership _ownWrapped;

    private MessageDigest _hashAlg;

    private long _hashPos;

    public HashStreamDotnet(Stream wrapped, Ownership ownsWrapped, MessageDigest hashAlg) {
        _wrapped = wrapped;
        _ownWrapped = ownsWrapped;
        _hashAlg = hashAlg;
    }

    public boolean canRead() {
        return _wrapped.canRead();
    }

    public boolean canSeek() {
        return _wrapped.canSeek();
    }

    public boolean canWrite() {
        return _wrapped.canWrite();
    }

    public long getLength() {
        return _wrapped.getLength();
    }

    public long getPosition() {
        return _wrapped.getPosition();
    }

    public void setPosition(long value) {
        _wrapped.setPosition(value);
    }

    public void flush() {
        _wrapped.flush();
    }

    public int read(byte[] buffer, int offset, int count) {
        if (getPosition() != _hashPos) {
            throw new UnsupportedOperationException("Reads must be contiguous");
        }

        int numRead = _wrapped.read(buffer, offset, count);
        _hashAlg.update(buffer, offset, numRead);
        _hashPos += numRead;
        return numRead;
    }

    public long seek(long offset, SeekOrigin origin) {
        return _wrapped.seek(offset, origin);
    }

    public void setLength(long value) {
        _wrapped.setLength(value);
    }

    public void write(byte[] buffer, int offset, int count) {
        _wrapped.write(buffer, offset, count);
    }

    public void close() throws IOException {
        if (_ownWrapped == Ownership.Dispose && _wrapped != null) {
            _wrapped.close();
            _wrapped = null;
        }
    }
}
