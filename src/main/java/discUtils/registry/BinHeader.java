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

package discUtils.registry;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import discUtils.streams.IByteArraySerializable;
import dotnet4j.io.IOException;
import vavi.util.ByteUtil;

import static java.lang.System.getLogger;


public final class BinHeader implements IByteArraySerializable {

    private static final Logger logger = getLogger(BinHeader.class.getName());

    public static final int HeaderSize = 0x20;

    private static final int Signature = 0x6E696268;

    public int binSize;

    public int fileOffset;

    @Override public int size() {
        return HeaderSize;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        int sig = ByteUtil.readLeInt(buffer, offset + 0);
        if (sig != Signature) {
logger.log(Level.ERROR, "%x".formatted(sig));
            throw new IOException("Invalid signature for registry bin");
        }

        fileOffset = ByteUtil.readLeInt(buffer, offset + 0x04);
        binSize = ByteUtil.readLeInt(buffer, offset + 0x08);
        long unknown = ByteUtil.readLeLong(buffer, offset + 0x0C);
        long unknown1 = ByteUtil.readLeLong(buffer, offset + 0x14);
        int unknown2 = ByteUtil.readLeInt(buffer, offset + 0x1C);
        return HeaderSize;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        ByteUtil.writeLeInt(Signature, buffer, offset + 0x00);
        ByteUtil.writeLeInt(fileOffset, buffer, offset + 0x04);
        ByteUtil.writeLeInt(binSize, buffer, offset + 0x08);
    }
}
