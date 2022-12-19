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

package discUtils.vhdx;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public class BatEntry implements IByteArraySerializable {

    public BatEntry() {
    }

    private long value;

    public BatEntry(byte[] buffer, int offset) {
        value = ByteUtil.readLeLong(buffer, offset);
    }

    public PayloadBlockStatus getPayloadBlockStatus() {
        return PayloadBlockStatus.values()[(int) value & 0x7];
    }

    public void setPayloadBlockStatus(PayloadBlockStatus value) {
        this.value = (this.value & ~0x7L) | value.ordinal();
    }

    public boolean getBitmapBlockPresent() {
        return (value & 0x7) == 6;
    }

    public void setBitmapBlockPresent(boolean value) {
        this.value = (this.value & ~0x7L) | (value ? 6 : 0);
    }

    public long getFileOffsetMB() {
        return value >>> 20 & 0xFFFFFFFFFFFL;
    }

    public void setFileOffsetMB(long value) {
        this.value = (this.value & 0xFFFFF) | value << 20;
    }

    public int size() {
        return 8;
    }

    public int readFrom(byte[] buffer, int offset) {
        value = ByteUtil.readLeLong(buffer, offset);
        return 8;
    }

    public void writeTo(byte[] buffer, int offset) {
        ByteUtil.writeLeLong(value, buffer, offset);
    }
}
