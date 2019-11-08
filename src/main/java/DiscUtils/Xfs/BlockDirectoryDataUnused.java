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
    private short _freetag;

    public short getFreetag() {
        return _freetag;
    }

    public void setFreetag(short value) {
        _freetag = value;
    }

    private short _length;

    public int getLength() {
        return _length & 0xffff;
    }

    public void setLength(short value) {
        _length = value;
    }

    private short _tag;

    public short getTag() {
        return _tag;
    }

    public void setTag(short value) {
        _tag = value;
    }

    public int size() {
        return getLength();
    }

    public int readFrom(byte[] buffer, int offset) {
        _freetag = EndianUtilities.toUInt16BigEndian(buffer, offset);
        _length = EndianUtilities.toUInt16BigEndian(buffer, offset + 0x2);
        _tag = EndianUtilities.toUInt16BigEndian(buffer, offset + getLength() - 0x2);
        return size();
    }
}
