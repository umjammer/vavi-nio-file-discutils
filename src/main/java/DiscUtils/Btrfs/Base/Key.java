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

package DiscUtils.Btrfs.Base;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class Key implements IByteArraySerializable {
    public static final int Length = 0x11;

    public Key() {
    }

    public Key(long objectId, ItemType type, long offset) {
        this();
        __ObjectId = objectId;
        __ItemType = type;
        __Offset = offset;
    }

    public Key(long objectId, ItemType type) {
        this(objectId, type, 0L);
    }

    public Key(ReservedObjectId objectId, ItemType type) {
        this(objectId.ordinal(), type);
    }

    /**
     * Object ID. Each tree has its own set of Object IDs.
     */
    private long __ObjectId;

    public long getObjectId() {
        return __ObjectId;
    }

    public void setObjectId(long value) {
        __ObjectId = value;
    }

    public ReservedObjectId getReservedObjectId() {
        return ReservedObjectId.valueOf((int) getObjectId());
    }

    /**
     * Item type.
     */
    private ItemType __ItemType = ItemType.InodeItem;

    public ItemType getItemType() {
        return __ItemType;
    }

    public void setItemType(ItemType value) {
        __ItemType = value;
    }

    /**
     * Offset. The meaning depends on the item type.
     */
    private long __Offset;

    public long getOffset() {
        return __Offset;
    }

    public void setOffset(long value) {
        __Offset = value;
    }

    public long getSize() {
        return Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        setObjectId(EndianUtilities.toUInt64LittleEndian(buffer, offset));
        setItemType(ItemType.valueOf(buffer[offset + 0x8]));
        setOffset(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x9));
        return (int) getSize();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        try {
            ReservedObjectId.valueOf((int) getObjectId());
            return String.format("%d|%s|%d", __ObjectId, __ItemType, __Offset);
        } catch (Exception e) {
            return String.format("%d|%s|%d", __ObjectId, __ItemType, __Offset);
        }
    }
}
