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

package DiscUtils.Registry;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;
import dotnet4j.io.IOException;


public final class BinHeader implements IByteArraySerializable {
    public static final int HeaderSize = 0x20;

    private static final int Signature = 0x6E696268;

    public int BinSize;

    public int FileOffset;

    public int sizeOf() {
        return HeaderSize;
    }

    public int readFrom(byte[] buffer, int offset) {
        int sig = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
        if (sig != Signature) {
System.err.printf("%x\n", sig);
            throw new IOException("Invalid signature for registry bin");
        }

        FileOffset = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x04);
        BinSize = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x08);
        long unknown = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x0C);
        long unknown1 = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x14);
        int unknown2 = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x1C);
        return HeaderSize;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(Signature, buffer, offset + 0x00);
        EndianUtilities.writeBytesLittleEndian(FileOffset, buffer, offset + 0x04);
        EndianUtilities.writeBytesLittleEndian(BinSize, buffer, offset + 0x08);
    }
}
