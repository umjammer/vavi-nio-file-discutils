//
// Copyright (c) 2017, Bianco Veigel
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

package discUtils.btrfs.base;

import java.util.UUID;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


/**
 * Maps logical address to physical
 */
public class Stripe implements IByteArraySerializable {
    public static final int Length = 0x20;

    /**
     * device id
     */
    private long deviceId;

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long value) {
        deviceId = value;
    }

    /**
     * offset
     */
    private long offset;

    public long getOffset() {
        return offset;
    }

    public void setOffset(long value) {
        offset = value;
    }

    /**
     * device UUID
     */
    private UUID deviceUuid;

    public UUID getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(UUID value) {
        deviceUuid = value;
    }

    @Override public int size() {
        return Length;
    }

    public Key getDevItemKey() {
        return new Key(getDeviceId(), ItemType.DevItem, getOffset());
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        setDeviceId(ByteUtil.readLeLong(buffer, offset));
        setOffset(ByteUtil.readLeLong(buffer, offset + 0x8));
        setDeviceUuid(ByteUtil.readLeUUID(buffer, offset + 0x10));
        return size();
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
