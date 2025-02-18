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

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public class Key implements IByteArraySerializable {

    public static final int Length = 0x11;

    public Key() {
    }

    public Key(long objectId, ItemType type, long offset) {
        this();
        this.objectId = objectId;
        itemType = type;
        this.offset = offset;
    }

    public Key(long objectId, ItemType type) {
        this(objectId, type, 0L);
    }

    public Key(ReservedObjectId objectId, ItemType type) {
        this(objectId.getValue(), type);
    }

    /**
     * Object ID. Each tree has its own set of Object IDs.
     */
    private long objectId;

    public long getObjectId() {
        return objectId;
    }

    public void setObjectId(long value) {
        objectId = value;
    }

    public ReservedObjectId getReservedObjectId() {
        return ReservedObjectId.valueOf((int) getObjectId());
    }

    /**
     * Item type.
     */
    private ItemType itemType = ItemType.InodeItem;

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType value) {
        itemType = value;
    }

    /**
     * Offset. The meaning depends on the item type.
     */
    private long offset;

    public long getOffset() {
        return offset;
    }

    public void setOffset(long value) {
        offset = value;
    }

    @Override public int size() {
        return Length;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        objectId = ByteUtil.readLeLong(buffer, offset);
        itemType = ItemType.valueOf(buffer[offset + 0x8] & 0xff);
        this.offset = ByteUtil.readLeLong(buffer, offset + 0x9);
        return size();
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override public String toString() {
        try {
            ReservedObjectId.valueOf((int) getObjectId());
            return String.format("%d|%s|%d", objectId, itemType, offset);
        } catch (Exception e) {
            return String.format("%d|%s|%d", objectId, itemType, offset);
        }
    }
}
