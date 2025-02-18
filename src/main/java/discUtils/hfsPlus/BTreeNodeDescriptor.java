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

package discUtils.hfsPlus;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public final class BTreeNodeDescriptor implements IByteArraySerializable {

    public int backwardLink;

    public int forwardLink;

    public byte height;

    public BTreeNodeKind kind = BTreeNodeKind.LeafNode;

    private short numRecords;

    public int getNumRecords() {
        return numRecords & 0xffff;
    }

    public short reserved;

    @Override public int size() {
        return 14;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        forwardLink = ByteUtil.readBeInt(buffer, offset + 0);
        backwardLink = ByteUtil.readBeInt(buffer, offset + 4);
        kind = BTreeNodeKind.valueOf(buffer[offset + 8]);
        height = buffer[offset + 9];
        numRecords = ByteUtil.readBeShort(buffer, offset + 10);
        reserved = ByteUtil.readBeShort(buffer, offset + 12);

        return 14;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
