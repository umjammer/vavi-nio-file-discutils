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
    private byte[] _checksum;

    public byte[] getChecksum() {
        return _checksum;
    }

    public void setChecksum(byte[] value) {
        _checksum = value;
    }

    /**
     * FS UUID
     */
    private UUID _fsUuid;

    public UUID getFsUuid() {
        return _fsUuid;
    }

    public void setFsUuid(UUID value) {
        _fsUuid = value;
    }

    /**
     * Logical address of this node
     */
    private long _logicalAddress;

    public long getLogicalAddress() {
        return _logicalAddress;
    }

    public void setLogicalAddress(long value) {
        _logicalAddress = value;
    }

    /**
     * Flags
     */
    private long _flags;

    public long getFlags() {
        return _flags;
    }

    public void setFlags(long value) {
        _flags = value;
    }

    /**
     * Backref. Rev.: always 1 (MIXED) for new filesystems; 0 (OLD) indicates an
     * old filesystem.
     */
    private byte _backrefRevision;

    public byte getBackrefRevision() {
        return _backrefRevision;
    }

    public void setBackrefRevision(byte value) {
        _backrefRevision = value;
    }

    /**
     * Chunk tree UUID
     */
    private UUID _chunkTreeUuid;

    public UUID getChunkTreeUuid() {
        return _chunkTreeUuid;
    }

    public void setChunkTreeUuid(UUID value) {
        _chunkTreeUuid = value;
    }

    /**
     * Logical address of this node
     */
    private long _generation;

    public long getGeneration() {
        return _generation;
    }

    public void setGeneration(long value) {
        _generation = value;
    }

    /**
     * The ID of the tree that contains this node
     */
    private long _treeId;

    public long getTreeId() {
        return _treeId;
    }

    public void setTreeId(long value) {
        _treeId = value;
    }

    /**
     * Number of items
     */
    private int _itemCount;

    public int getItemCount() {
        return _itemCount;
    }

    public void setItemCount(int value) {
        _itemCount = value;
    }

    /**
     * Level (0 for leaf nodes)
     */
    private byte _level;

    public byte getLevel() {
        return _level;
    }

    public void setLevel(byte value) {
        _level = value;
    }

    public int size() {
        return Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        setChecksum(EndianUtilities.toByteArray(buffer, offset, 0x20));
        setFsUuid(EndianUtilities.toGuidLittleEndian(buffer, offset + 0x20));
        setLogicalAddress(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x30));
        // TODO: validate shift
        setFlags(EndianUtilities.toInt64LittleEndian(buffer, offset + 0x38) >>> 8);
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
            if (clazz.isInstance(item))
                result.add(clazz.cast(item));
        }
        return result;
    }
}
