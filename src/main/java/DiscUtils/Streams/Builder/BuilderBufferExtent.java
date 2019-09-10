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

package DiscUtils.Streams.Builder;

public class BuilderBufferExtent extends BuilderExtent {
    private byte[] _buffer;

    private final boolean _fixedBuffer;

    public BuilderBufferExtent(long start, long length)  {
        super(start, length);
        _fixedBuffer = false;
    }

    public BuilderBufferExtent(long start, byte[] buffer)  {
        super(start, buffer.length);
        _fixedBuffer = true;
        _buffer = buffer;
    }

    public void close() {
    }

    public void prepareForRead() {
        if (!_fixedBuffer) {
            _buffer = getBuffer();
        }

    }

    public int read(long diskOffset, byte[] block, int offset, int count) {
        int startOffset = (int) (diskOffset - getStart());
        int numBytes = (int) Math.min(getLength() - startOffset, count);
        System.arraycopy(_buffer, startOffset, block, offset, numBytes);
        return numBytes;
    }

    public void disposeReadState() {
        if (!_fixedBuffer) {
            _buffer = null;
        }

    }

    protected byte[] getBuffer() {
        throw new UnsupportedOperationException("Derived class should implement");
    }

}
