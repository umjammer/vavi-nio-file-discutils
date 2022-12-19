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


public class ExtentIndex implements IByteArraySerializable {

    public int firstLogicalBlock;

    private short leafPhysicalBlockHi;

    public int getLeafPhysicalBlockHi() {
        return leafPhysicalBlockHi & 0xffff;
    }

    private int leafPhysicalBlockLo;

    public long getLeafPhysicalBlockLo() {
        return leafPhysicalBlockLo & 0xffff_ffffL;
    }

    public long getLeafPhysicalBlock() {
        return getLeafPhysicalBlockLo() | ((long) getLeafPhysicalBlockHi() << 32);
    }

    public int size() {
        return 12;
    }

    public int readFrom(byte[] buffer, int offset) {
        firstLogicalBlock = ByteUtil.readLeInt(buffer, offset + 0);
        leafPhysicalBlockLo = ByteUtil.readLeInt(buffer, offset + 4);
        leafPhysicalBlockHi = ByteUtil.readLeShort(buffer, offset + 8);
        return 12;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
