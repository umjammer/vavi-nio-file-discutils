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

import java.io.IOException;
import java.util.List;

import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.util.Ownership;


public class BuilderSparseStreamExtent extends BuilderExtent {

    private final Ownership ownership;

    private SparseStream stream;

    public BuilderSparseStreamExtent(long start, SparseStream stream) {
        this(start, stream, Ownership.None);
    }

    public BuilderSparseStreamExtent(long start, SparseStream stream, Ownership ownership) {
        super(start, stream.getLength());
        this.stream = stream;
        this.ownership = ownership;
    }

    public List<StreamExtent> getStreamExtents() {
        return StreamExtent.offset(stream.getExtents(), getStart());
    }

    public void close() throws IOException {
        if (stream != null && ownership == Ownership.Dispose) {
            stream.close();
            stream = null;
        }
    }

    public void prepareForRead() {
    }

    public int read(long diskOffset, byte[] block, int offset, int count) {
        stream.position(diskOffset - getStart());
        return stream.read(block, offset, count);
    }

    public void disposeReadState() {
    }
}
