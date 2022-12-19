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


public class DirectoryHeader implements IByteArraySerializable {

    public int count;

    public int inodeNumber;

    public int startBlock;

    public int size() {
        return 12;
    }

    public int readFrom(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public void writeTo(byte[] buffer, int offset) {
        ByteUtil.writeLeInt(count, buffer, offset + 0);
        ByteUtil.writeLeInt(startBlock, buffer, offset + 4);
        ByteUtil.writeLeInt(inodeNumber, buffer, offset + 8);
    }

    public static DirectoryHeader readFrom(MetablockReader reader) {
        DirectoryHeader result = new DirectoryHeader();
        result.count = reader.readInt();
        result.startBlock = reader.readInt();
        result.inodeNumber = reader.readInt();
        return result;
    }
}
