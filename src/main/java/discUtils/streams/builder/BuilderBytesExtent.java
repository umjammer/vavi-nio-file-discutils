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

public class BuilderBytesExtent extends BuilderExtent {
    protected byte[] data;

    public BuilderBytesExtent(long start, byte[] data) {
        super(start, data.length);
        this.data = data;
    }

    protected BuilderBytesExtent(long start, long length) {
        super(start, length);
    }

    public void close() {
    }

    public void prepareForRead() {
    }

    public int read(long diskOffset, byte[] block, int offset, int count) {
        int start = (int) Math.min(diskOffset - getStart(), data.length);
        int numRead = Math.min(count, data.length - start);
        System.arraycopy(data, start, block, offset, numRead);
        return numRead;
    }

    public void disposeReadState() {
    }

}
