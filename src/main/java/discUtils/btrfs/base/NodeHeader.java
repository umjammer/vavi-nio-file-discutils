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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import discUtils.btrfs.Context;
import discUtils.btrfs.base.items.BaseItem;
import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;
import vavi.util.ByteUtil;


public abstract class NodeHeader implements IByteArraySerializable {

    public static final int Length = 0x65;

    /**
     * Checksum of everything past this field (from 20 to the end of the node)
     */
    private byte[] checksum;

    public byte[] getChecksum() {
        return checksum;
    }

    public void setChecksum(byte[] value) {
        checksum = value;
    }

    /**
     * FS UUID
     */
    private UUID fsUuid;

    public UUID getFsUuid() {
        return fsUuid;
    }

    public void setFsUuid(UUID value) {
        fsUuid = value;
    }

    /**
     * Logical address of this node
     */
    private long logicalAddress;

    public long getLogicalAddress() {
        return logicalAddress;
    }

    public void setLogicalAddress(long value) {
        logicalAddress = value;
    }

    /**
     * flags
     */
    private long flags;

    public long getFlags() {
        return flags;
    }

    public void setFlags(long value) {
        flags = value;
    }

    /**
     * Backref. Rev.: always 1 (MIXED) for new filesystems; 0 (OLD) indicates an
     * old filesystem.
     */
    private byte backrefRevision;

    public byte getBackrefRevision() {
        return backrefRevision;
    }

    public void setBackrefRevision(byte value) {
        backrefRevision = value;
    }

    /**
     * Chunk tree UUID
     */
    private UUID chunkTreeUuid;

    public UUID getChunkTreeUuid() {
        return chunkTreeUuid;
    }

    public void setChunkTreeUuid(UUID value) {
        chunkTreeUuid = value;
    }

    /**
     * Logical address of this node
     */
    private long generation;

    public long getGeneration() {
        return generation;
    }

    public void setGeneration(long value) {
        generation = value;
    }

    /**
     * The ID of the tree that contains this node
     */
    private long treeId;

    public long getTreeId() {
        return treeId;
    }

    public void setTreeId(long value) {
        treeId = value;
    }

    /**
     * Number of items
     */
    private int itemCount;

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int value) {
        itemCount = value;
    }

    /**
     * Level (0 for leaf nodes)
     */
    private byte level;

    public int getLevel() {
        return level & 0xff;
    }

    public void setLevel(byte value) {
        level = value;
    }

    public int size() {
        return Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        checksum = EndianUtilities.toByteArray(buffer, offset, 0x20);
        fsUuid = ByteUtil.readLeUUID(buffer, offset + 0x20);
        logicalAddress = ByteUtil.readLeLong(buffer, offset + 0x30);
        // TODO: validate shift
        flags = ByteUtil.readLeLong(buffer, offset + 0x38) >>> 8;
        backrefRevision = buffer[offset + 0x3f];
        chunkTreeUuid = ByteUtil.readLeUUID(buffer, offset + 0x40);
        generation = ByteUtil.readLeLong(buffer, offset + 0x50);
        treeId = ByteUtil.readLeLong(buffer, offset + 0x58);
        itemCount = ByteUtil.readLeInt(buffer, offset + 0x60);
        level = buffer[offset + 0x64];
        return Length;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public static NodeHeader create(byte[] buffer, int offset) {
        int level = buffer[offset + 0x64];
        NodeHeader result;
        if (level == 0)
            result = new LeafNode();
        else
            result = new InternalNode();
        result.readFrom(buffer, offset);
        return result;
    }

    public abstract List<BaseItem> find(Key key, Context context);

    public BaseItem findFirst(Key key, Context context) {
        for (BaseItem item : find(key, context)) {
            return item;
        }
        return null;
    }

    public <T extends BaseItem> T findFirst(Class<T> clazz, Key key, Context context) {
        for (T item : find(clazz, key, context)) {
            return item;
        }
        return null;
    }

    public <T extends BaseItem> List<T> find(Class<T> clazz, Key key, Context context) {
        List<T> result = new ArrayList<>();
        for (BaseItem item : find(key, context)) {
            if (clazz.isInstance(item))
                result.add(clazz.cast(item));
        }
        return result;
    }
}
