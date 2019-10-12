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

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class BatEntry implements IByteArraySerializable {
    public BatEntry() {
    }

    private long _value;

    public BatEntry(byte[] buffer, int offset) {
        _value = EndianUtilities.toUInt64LittleEndian(buffer, offset);
    }

    public PayloadBlockStatus getPayloadBlockStatus() {
        return PayloadBlockStatus.valueOf((int) _value & 0x7);
    }

    public void setPayloadBlockStatus(PayloadBlockStatus value) {
        _value = (_value & ~0x7l) | value.ordinal();
    }

    public boolean getBitmapBlockPresent() {
        return (_value & 0x7) == 6;
    }

    public void setBitmapBlockPresent(boolean value) {
        _value = (_value & ~0x7l) | (value ? 6 : 0);
    }

    public long getFileOffsetMB() {
        return _value >>> 20 & 0xFFFFFFFFFFFL;
    }

    public void setFileOffsetMB(long value) {
        _value = (_value & 0xFFFFF) | value << 20;
    }

    public long getSize() {
        return 8;
    }

    public int readFrom(byte[] buffer, int offset) {
        _value = EndianUtilities.toUInt64LittleEndian(buffer, offset);
        return 8;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(_value, buffer, offset);
    }
}
