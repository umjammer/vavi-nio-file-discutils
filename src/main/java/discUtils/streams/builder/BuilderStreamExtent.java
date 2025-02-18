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

import discUtils.streams.util.Ownership;
import dotnet4j.io.Stream;


public class BuilderStreamExtent extends BuilderExtent {

    private final Ownership ownership;

    private Stream source;

    public BuilderStreamExtent(long start, Stream source) {
        this(start, source, Ownership.None);
    }

    public BuilderStreamExtent(long start, Stream source, Ownership ownership) {
        super(start, source.getLength());
        this.source = source;
        this.ownership = ownership;
    }

    @Override public void close() throws IOException {
        if (source != null && ownership == Ownership.Dispose) {
            source.close();
            source = null;
        }
    }

    @Override public void prepareForRead() {
    }

    @Override public int read(long diskOffset, byte[] block, int offset, int count) {
        source.position(diskOffset - start);
        return source.read(block, offset, count);
    }

    @Override public void disposeReadState() {
    }
}
