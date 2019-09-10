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

import java.io.IOException;
import java.util.List;

import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Util.Ownership;


public class BuilderSparseStreamExtent extends BuilderExtent {
    private final Ownership _ownership;

    private SparseStream _stream;

    public BuilderSparseStreamExtent(long start, SparseStream stream) {
        this(start, stream, Ownership.None);
    }

    public BuilderSparseStreamExtent(long start, SparseStream stream, Ownership ownership) {
        super(start, stream.getLength());
        _stream = stream;
        _ownership = ownership;
    }

    public List<StreamExtent> getStreamExtents() {
        return StreamExtent.offset(_stream.getExtents(), getStart());
    }

    public void close() throws IOException {
        if (_stream != null && _ownership == Ownership.Dispose) {
            _stream.close();
            _stream = null;
        }

    }

    public void prepareForRead() {
    }

    public int read(long diskOffset, byte[] block, int offset, int count) {
        _stream.setPosition(diskOffset - getStart());
        return _stream.read(block, offset, count);
    }

    public void disposeReadState() {
    }

}
