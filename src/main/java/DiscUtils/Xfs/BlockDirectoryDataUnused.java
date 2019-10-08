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

package DiscUtils.Xfs;

import DiscUtils.Streams.Util.EndianUtilities;


public class BlockDirectoryDataUnused extends BlockDirectoryData {
    private short __Freetag;

    public short getFreetag() {
        return __Freetag;
    }

    public void setFreetag(short value) {
        __Freetag = value;
    }

    private short __Length;

    public short getLength() {
        return __Length;
    }

    public void setLength(short value) {
        __Length = value;
    }

    private short __Tag;

    public short getTag() {
        return __Tag;
    }

    public void setTag(short value) {
        __Tag = value;
    }

    public long getSize() {
        return getLength();
    }

    public int readFrom(byte[] buffer, int offset) {
        setFreetag(EndianUtilities.toUInt16BigEndian(buffer, offset));
        setLength(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x2));
        setTag(EndianUtilities.toUInt16BigEndian(buffer, offset + getLength() - 0x2));
        return (int) getSize();
    }
}