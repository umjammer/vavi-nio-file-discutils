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

package discUtils.wim;

import vavi.util.ByteUtil;


public class ResourceInfo {

    public static final int Size = ShortResourceHeader.Size + 26;

    public byte[] hash;

    public ShortResourceHeader header;

    public short partNumber;

    public int refCount;

    public void read(byte[] buffer, int offset) {
        header = new ShortResourceHeader();
        header.read(buffer, offset);
        partNumber = ByteUtil.readLeShort(buffer, offset + ShortResourceHeader.Size);
        refCount = ByteUtil.readLeInt(buffer, offset + ShortResourceHeader.Size + 2);
        hash = new byte[20];
        System.arraycopy(buffer, offset + ShortResourceHeader.Size + 6, hash, 0, 20);
    }
}
