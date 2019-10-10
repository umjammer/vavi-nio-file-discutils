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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import DiscUtils.Btrfs.Context;
import DiscUtils.Btrfs.Base.Items.BaseItem;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public abstract class NodeHeader implements IByteArraySerializable {
    public static final int Length = 0x65;

    /**
     * Checksum of everything past this field (from 20 to the end of the node)
     */
    private byte[] __Checksum;

    public byte[] getChecksum() {
        return __Checksum;
    }

    public void setChecksum(byte[] value) {
        __Checksum = value;
    }

    /**
     * FS UUID
     */
    private UUID __FsUuid;

    public UUID getFsUuid() {
        return __FsUuid;
    }

    public void setFsUuid(UUID value) {
        __FsUuid = value;
    }

    /**
     * Logical address of this node
     */
    private long __LogicalAddress;

    public long getLogicalAddress() {
        return __LogicalAddress;
    }

    public void setLogicalAddress(long value) {
        __LogicalAddress = value;
    }

    /**
     * Flags
     */
    private long __Flags;

    public long getFlags() {
        return __Flags;
    }

    public void setFlags(long value) {
        __Flags = value;
    }

    /**
     * Backref. Rev.: always 1 (MIXED) for new filesystems; 0 (OLD) indicates an
     * old filesystem.
     */
    private byte __BackrefRevision;

    public byte getBackrefRevision() {
        return __BackrefRevision;
    }

    public void setBackrefRevision(byte value) {
        __BackrefRevision = value;
    }

    /**
     * Chunk tree UUID
     */
    private UUID __ChunkTreeUuid;

    public UUID getChunkTreeUuid() {
        return __ChunkTreeUuid;
    }

    public void setChunkTreeUuid(UUID value) {
        __ChunkTreeUuid = value;
    }

    /**
     * Logical address of this node
     */
    private long __Generation;

    public long getGeneration() {
        return __Generation;
    }

    public void setGeneration(long value) {
        __Generation = value;
    }

    /**
     * The ID of the tree that contains this node
     */
    private long __TreeId;

    public long getTreeId() {
        return __TreeId;
    }

    public void setTreeId(long value) {
        __TreeId = value;
    }

    /**
     * Number of items
     */
    private int __ItemCount;

    public int getItemCount() {
        return __ItemCount;
    }

    public void setItemCount(int value) {
        __ItemCount = value;
    }

    /**
     * Level (0 for leaf nodes)
     */
    private byte __Level;

    public byte getLevel() {
        return __Level;
    }

    public void setLevel(byte value) {
        __Level = value;
    }

    public long getSize() {
        return Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        setChecksum(EndianUtilities.toByteArray(buffer, offset, 0x20));
        setFsUuid(EndianUtilities.toGuidLittleEndian(buffer, offset + 0x20));
        setLogicalAddress(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x30));
        //todo validate shift
        setFlags(EndianUtilities.toInt64LittleEndian(buffer, offset + 0x38) >> 8);
        setBackrefRevision(buffer[offset + 0x3f]);
        setChunkTreeUuid(EndianUtilities.toGuidLittleEndian(buffer, offset + 0x40));
        setGeneration(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x50));
        setTreeId(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x58));
        setItemCount(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x60));
        setLevel(buffer[offset + 0x64]);
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
            T typed = clazz.isInstance(item) ? (T) item : (T) null;
            if (typed != null)
                result.add(typed);
        }
        return result;
    }
}
