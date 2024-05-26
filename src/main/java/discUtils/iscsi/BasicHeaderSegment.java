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

package discUtils.iscsi;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public class BasicHeaderSegment implements IByteArraySerializable {

    public int dataSegmentLength;

    // In bytes!
    public boolean finalPdu;

    public boolean immediate;

    public int initiatorTaskTag;

    public OpCode opCode = OpCode.NopOut;

    public byte totalAhsLength;

    // In 4-byte words!
    @Override public int size() {
        return 48;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        immediate = (buffer[offset] & 0x40) != 0;
        opCode = OpCode.valueOf(buffer[offset] & 0x3F);
//logger.log(Level.DEBUG, "OpCode: " + (buffer[offset] & 0x3F));
        finalPdu = (buffer[offset + 1] & 0x80) != 0;
        totalAhsLength = buffer[offset + 4];
        dataSegmentLength = ByteUtil.readBeInt(buffer, offset + 4) & 0x00FF_FFFF;
        initiatorTaskTag = ByteUtil.readBeInt(buffer, offset + 16);
        return 48;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        buffer[offset] = (byte) ((immediate ? 0x40 : 0x00) | (opCode.ordinal() & 0x3F));
        buffer[offset + 1] |= (byte) (finalPdu ? 0x80 : 0x00);
        buffer[offset + 4] = totalAhsLength;
        buffer[offset + 5] = (byte) ((dataSegmentLength >>> 16) & 0xFF);
        buffer[offset + 6] = (byte) ((dataSegmentLength >>> 8) & 0xFF);
        buffer[offset + 7] = (byte) (dataSegmentLength & 0xFF);
        ByteUtil.writeBeInt(initiatorTaskTag, buffer, offset + 16);
    }
}
