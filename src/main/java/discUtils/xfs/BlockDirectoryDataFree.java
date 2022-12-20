//
// Copyright (c) 2016, Bianco Veigel
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

package discUtils.xfs;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public class BlockDirectoryDataFree implements IByteArraySerializable {

    private short offset;

    public int getOffset() {
        return offset & 0xffff;
    }

    public void setOffset(short value) {
        offset = value;
    }

    private short length;

    public int getLength() {
        return length & 0xffff;
    }

    public void setLength(short value) {
        length = value;
    }

    @Override public int size() {
        return 0x4;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        this.offset = ByteUtil.readBeShort(buffer, offset);
        length = ByteUtil.readBeShort(buffer, offset + 0x2);
        return size();
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
