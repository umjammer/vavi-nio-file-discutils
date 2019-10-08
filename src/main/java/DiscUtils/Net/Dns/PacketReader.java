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

package DiscUtils.Net.Dns;

import java.nio.charset.Charset;

import DiscUtils.Streams.Util.EndianUtilities;


public final class PacketReader {
    private final byte[] _data;

    public PacketReader(byte[] data) {
        _data = data;
    }

    private int __Position;

    public int getPosition() {
        return __Position;
    }

    public void setPosition(int value) {
        __Position = value;
    }

    public String readName() {
        StringBuilder sb = new StringBuilder();
        boolean hasIndirected = false;
        int readPos = getPosition();
        while (_data[readPos] != 0) {
            byte len = _data[readPos];
            if ((len & 0xC0) == 0x00) {
                sb.append(new String(_data, readPos + 1, len, Charset.forName("UTF8")));
                sb.append(".");
                readPos += 1 + len;
                if (!hasIndirected) {
                    setPosition(readPos);
                }

            } else if ((len & 0xC0) == 0xC0) {
                if (!hasIndirected) {
                    setPosition(getPosition() + 2);
                }

                hasIndirected = true;
                readPos = EndianUtilities.toUInt16BigEndian(_data, readPos) & 0x3FFF;
            } else {
                throw new UnsupportedOperationException("Unknown control flags reading label");
            }
        }
        if (!hasIndirected) {
            setPosition(getPosition() + 1);
        }

        return sb.toString();
    }

    public short readUShort() {
        short result = EndianUtilities.toUInt16BigEndian(_data, getPosition());
        setPosition(getPosition() + 2);
        return result;
    }

    public int readInt() {
        int result = EndianUtilities.toInt32BigEndian(_data, getPosition());
        setPosition(getPosition() + 4);
        return result;
    }

    public byte readByte() {
        byte result = _data[getPosition()];
        setPosition(getPosition() + 1);
        return result;
    }

    public byte[] readBytes(int count) {
        byte[] result = new byte[count];
        System.arraycopy(_data, getPosition(), result, 0, count);
        setPosition(getPosition() + count);
        return result;
    }
}
