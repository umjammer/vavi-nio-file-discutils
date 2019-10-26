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

package DiscUtils.Ext;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class ExtentHeader implements IByteArraySerializable {
    public static final short HeaderMagic = (short) 0xf30a;

    public short Depth;

    public short Entries;

    public int Generation;

    public short Magic;

    public short MaxEntries;

    public int size() {
        return 12;
    }

    public int readFrom(byte[] buffer, int offset) {
        Magic = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0);
        Entries = EndianUtilities.toUInt16LittleEndian(buffer, offset + 2);
        MaxEntries = EndianUtilities.toUInt16LittleEndian(buffer, offset + 4);
        Depth = EndianUtilities.toUInt16LittleEndian(buffer, offset + 6);
        Generation = (short) EndianUtilities.toUInt32LittleEndian(buffer, offset + 8);
        return 12;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
