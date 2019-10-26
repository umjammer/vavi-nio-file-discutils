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

package DiscUtils.Vhdx;

import java.util.EnumSet;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Sizes;


public final class FileParameters implements IByteArraySerializable {
    public static final int DefaultBlockSize = 32 * (int) Sizes.OneMiB;

    public static final int DefaultDifferencingBlockSize = 2 * (int) Sizes.OneMiB;

    public static final int DefaultDynamicBlockSize = 32 * (int) Sizes.OneMiB;

    public int BlockSize;

    public EnumSet<FileParametersFlags> Flags;

    public int size() {
        return 8;
    }

    public int readFrom(byte[] buffer, int offset) {
        BlockSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
        Flags = FileParametersFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 4));
        return 8;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(BlockSize, buffer, offset + 0);
        EndianUtilities.writeBytesLittleEndian((int) FileParametersFlags.valueOf(Flags), buffer, offset + 4);
    }
}
