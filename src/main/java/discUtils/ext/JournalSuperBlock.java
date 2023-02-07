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

package discUtils.ext;

import discUtils.streams.IByteArraySerializable;
import dotnet4j.io.IOException;
import vavi.util.ByteUtil;


public class JournalSuperBlock implements IByteArraySerializable {

    public int blockSize;

    public int maxLength;

    public static final int Magic = 0xC03B3998;

    @Override
    public int size() {
        return 1024;
    }

    @Override
    public int readFrom(byte[] buffer, int offset) {
        int magic = ByteUtil.readBeInt(buffer, offset);
        if (magic != Magic) {
            throw new IOException("Invalid journal magic - probably not an ext file system");
        }

        int blockType = ByteUtil.readBeInt(buffer, offset + 0x4);
        if (blockType != 3 && blockType != 4) {
            throw new IOException("Invalid journal block type - no superblock found");
        }

        blockSize = ByteUtil.readBeInt(buffer, offset + 0xc);
        maxLength = ByteUtil.readBeInt(buffer, offset + 0x10);
        return 1024;
    }

    @Override
    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
