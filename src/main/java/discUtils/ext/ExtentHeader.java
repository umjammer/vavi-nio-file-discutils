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

package discUtils.ext;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public class ExtentHeader implements IByteArraySerializable {

    public static final short HeaderMagic = (short) 0xf30a;

    public short depth;

    private short entries;

    public int getEntries() {
        return entries & 0xffff;
    }

    public int generation;

    public short magic;

    private short maxEntries;

    public int getMaxEntries() {
        return maxEntries & 0xffff;
    }

    @Override public int size() {
        return 12;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        magic = ByteUtil.readLeShort(buffer, offset + 0);
        entries = ByteUtil.readLeShort(buffer, offset + 2);
        maxEntries = ByteUtil.readLeShort(buffer, offset + 4);
        depth = ByteUtil.readLeShort(buffer, offset + 6);
        generation = (short) ByteUtil.readLeInt(buffer, offset + 8);
        return 12;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
