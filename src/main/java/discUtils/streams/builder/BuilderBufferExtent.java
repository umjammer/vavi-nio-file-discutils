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

package discUtils.streams.builder;

public class BuilderBufferExtent extends BuilderExtent {

    private byte[] buffer;

    private final boolean fixedBuffer;

    public BuilderBufferExtent(long start, long length)  {
        super(start, length);
        fixedBuffer = false;
    }

    public BuilderBufferExtent(long start, byte[] buffer)  {
        super(start, buffer.length);
        fixedBuffer = true;
        this.buffer = buffer;
    }

    @Override
    public void close() {
    }

    @Override
    public void prepareForRead() {
        if (!fixedBuffer) {
            buffer = getBuffer();
        }
    }

    @Override
    public int read(long diskOffset, byte[] block, int offset, int count) {
        int startOffset = (int) (diskOffset - start);
        int numBytes = (int) Math.min(length - startOffset, count);
        System.arraycopy(buffer, startOffset, block, offset, numBytes);
        return numBytes;
    }

    @Override
    public void disposeReadState() {
        if (!fixedBuffer) {
            buffer = null;
        }
    }

    protected byte[] getBuffer() {
        throw new UnsupportedOperationException("Derived class should implement");
    }
}
