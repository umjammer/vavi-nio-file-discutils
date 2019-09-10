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

import java.nio.charset.Charset;
import java.util.Arrays;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Sizes;


public final class FileHeader implements IByteArraySerializable {
    public static final long VhdxSignature = 0x656C696678646876l;

    public String Creator;

    public long Signature = VhdxSignature;

    public boolean getIsValid() {
        return Signature == VhdxSignature;
    }

    public int getSize() {
        return (int) (64 * Sizes.OneKiB);
    }

    public int readFrom(byte[] buffer, int offset) {
        Signature = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0);
        Creator = new String(buffer, offset + 8, 256 * 2, Charset.forName("utf-8")).replaceAll("\0*$", "");
        return getSize();
    }

    public void writeTo(byte[] buffer, int offset) {
        Arrays.fill(buffer, offset, getSize(), (byte) 0);
        EndianUtilities.writeBytesLittleEndian(Signature, buffer, offset + 0);
        System.arraycopy(Creator.getBytes(Charset.forName("utf-8")), 0, buffer, offset + 8, Creator.length());
    }

}
