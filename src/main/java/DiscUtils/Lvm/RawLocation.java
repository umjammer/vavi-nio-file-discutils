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

package DiscUtils.Lvm;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class RawLocation implements IByteArraySerializable {
    public long Offset;

    public long Length;

    public int Checksum;

    public RawLocationFlags Flags = RawLocationFlags.Ignored;

    /**
     *
     */
    public long getSize() {
        return 0x18;
    }

    /**
     *
     */
    public int readFrom(byte[] buffer, int offset) {
        Offset = EndianUtilities.toUInt64LittleEndian(buffer, offset);
        Length = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x8);
        Checksum = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x10);
        Flags = RawLocationFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x14));
        return (int) getSize();
    }

    /**
     *
     */
    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}