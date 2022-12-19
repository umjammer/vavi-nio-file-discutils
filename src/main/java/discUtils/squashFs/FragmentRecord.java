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

package discUtils.squashFs;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public class FragmentRecord implements IByteArraySerializable {

    public static final int RecordSize = 16;

    public int compressedSize;

    public long startBlock;

    public int size() {
        return RecordSize;
    }

    public int readFrom(byte[] buffer, int offset) {
        startBlock = ByteUtil.readLeLong(buffer, offset + 0);
        compressedSize = ByteUtil.readLeInt(buffer, offset + 8);
        return RecordSize;
    }

    public void writeTo(byte[] buffer, int offset) {
        ByteUtil.writeLeLong(startBlock, buffer, offset + 0);
        ByteUtil.writeLeInt(compressedSize, buffer, offset + 8);
        ByteUtil.writeLeInt(0, buffer, offset + 12);
    }
}
