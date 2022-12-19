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
import discUtils.streams.util.EndianUtilities;
import vavi.util.ByteUtil;


public final class ForkData implements IByteArraySerializable {

    public static final int StructSize = 80;

    public int clumpSize;

    public ExtentDescriptor[] extents;

    public long logicalSize;

    public int totalBlocks;

    public int size() {
        return StructSize;
    }

    public int readFrom(byte[] buffer, int offset) {
        logicalSize = ByteUtil.readBeLong(buffer, offset + 0);
        clumpSize = ByteUtil.readBeInt(buffer, offset + 8);
        totalBlocks = ByteUtil.readBeInt(buffer, offset + 12);

        extents = new ExtentDescriptor[8];
        for (int i = 0; i < 8; ++i) {
            extents[i] = EndianUtilities.toStruct(ExtentDescriptor.class, buffer, offset + 16 + i * 8);
        }

        return StructSize;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
