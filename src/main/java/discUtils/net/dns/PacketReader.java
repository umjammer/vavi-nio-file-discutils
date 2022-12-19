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

package discUtils.net.dns;

import java.nio.charset.StandardCharsets;

import vavi.util.ByteUtil;


public final class PacketReader {

    private final byte[] data;

    public PacketReader(byte[] data) {
        this.data = data;
    }

    private int position;

    public int getPosition() {
        return position;
    }

    public void setPosition(int value) {
        position = value;
    }

    public String readName() {
        StringBuilder sb = new StringBuilder();

        boolean hasIndirected = false;
        int readPos = getPosition();

        while (data[readPos] != 0) {
            byte len = data[readPos];
            switch (len & 0xC0) {
            case 0x00:
                sb.append(new String(data, readPos + 1, len, StandardCharsets.UTF_8));
                sb.append(".");
                readPos += 1 + len;
                if (!hasIndirected) {
                    position = readPos;
                }

                break;

            case 0xC0:
                if (!hasIndirected) {
                    position += 2;
                }

                hasIndirected = true;
                readPos = ByteUtil.readBeShort(data, readPos) & 0x3FFF;
                break;

            default:
                throw new UnsupportedOperationException("Unknown control flags reading label");
            }
        }

        if (!hasIndirected) {
            position++;
        }

        return sb.toString();
    }

    public short readUShort() {
        short result = ByteUtil.readBeShort(data, getPosition());
        position += 2;
        return result;
    }

    public int readInt() {
        int result = ByteUtil.readBeShort(data, getPosition());
        position += 4;
        return result;
    }

    public byte readByte() {
        byte result = data[getPosition()];
        position++;
        return result;
    }

    public byte[] readBytes(int count) {
        byte[] result = new byte[count];
        System.arraycopy(data, position, result, 0, count);
        position += count;
        return result;
    }
}
