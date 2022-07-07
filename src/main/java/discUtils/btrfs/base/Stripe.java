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
import discUtils.streams.util.EndianUtilities;


/**
 * Maps logical address to physical
 */
public class Stripe implements IByteArraySerializable {
    public static final int Length = 0x20;

    /**
     * device id
     */
    private long __DeviceId;

    public long getDeviceId() {
        return __DeviceId;
    }

    public void setDeviceId(long value) {
        __DeviceId = value;
    }

    /**
     * offset
     */
    private long __Offset;

    public long getOffset() {
        return __Offset;
    }

    public void setOffset(long value) {
        __Offset = value;
    }

    /**
     * device UUID
     */
    private UUID __DeviceUuid;

    public UUID getDeviceUuid() {
        return __DeviceUuid;
    }

    public void setDeviceUuid(UUID value) {
        __DeviceUuid = value;
    }

    public int size() {
        return Length;
    }

    public Key getDevItemKey() {
        return new Key(getDeviceId(), ItemType.DevItem, getOffset());
    }

    public int readFrom(byte[] buffer, int offset) {
        setDeviceId(EndianUtilities.toUInt64LittleEndian(buffer, offset));
        setOffset(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x8));
        setDeviceUuid(EndianUtilities.toGuidLittleEndian(buffer, offset + 0x10));
        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
