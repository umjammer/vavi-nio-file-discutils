//
// Copyright (c) 2008-2012, Kenneth Bell
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

package discUtils.vhdx;

import java.util.EnumSet;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.Sizes;
import vavi.util.ByteUtil;


public final class FileParameters implements IByteArraySerializable {

    public static final int DefaultBlockSize = 32 * (int) Sizes.OneMiB;

    public static final int DefaultDifferencingBlockSize = 2 * (int) Sizes.OneMiB;

    public static final int DefaultDynamicBlockSize = 32 * (int) Sizes.OneMiB;

    public int blockSize;

    public EnumSet<FileParametersFlags> flags;

    @Override public int size() {
        return 8;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        blockSize = ByteUtil.readLeInt(buffer, offset + 0);
        flags = FileParametersFlags.valueOf(ByteUtil.readLeInt(buffer, offset + 4));
        return 8;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        ByteUtil.writeLeInt(blockSize, buffer, offset + 0);
        ByteUtil.writeLeInt((int) FileParametersFlags.valueOf(flags), buffer, offset + 4);
    }
}
