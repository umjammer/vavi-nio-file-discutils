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

import java.nio.charset.Charset;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.MathUtilities;
import vavi.util.ByteUtil;


public class DirectoryRecord implements IByteArraySerializable {

    public static final byte FileTypeUnknown = 0;
    public static final byte FileTypeRegularFile = 1;
    public static final byte FileTypeDirectory = 2;
    public static final byte FileTypeCharacterDevice = 3;
    public static final byte FileTypeBlockDevice = 4;
    public static final byte FileTypeFifo = 5;
    public static final byte FileTypeSocket = 6;
    public static final byte FileTypeSymlink = 7;

    private final Charset nameEncoding;

    public byte fileType;

    public int inode;

    public String name;

    public DirectoryRecord(Charset nameEncoding) {
        this.nameEncoding = nameEncoding;
    }

    @Override public int size() {
        return MathUtilities.roundUp(8 + name.length(), 4);
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        inode = ByteUtil.readLeInt(buffer, offset + 0);
        short recordLen = ByteUtil.readLeShort(buffer, offset + 4);
        int nameLen = buffer[offset + 6];
        fileType = buffer[offset + 7];
        name = new String(buffer, offset + 8, nameLen, nameEncoding);

        return recordLen;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
