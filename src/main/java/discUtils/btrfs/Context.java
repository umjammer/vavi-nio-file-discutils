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

package discUtils.btrfs;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import discUtils.btrfs.base.BlockGroupFlag;
import discUtils.btrfs.base.ChecksumType;
import discUtils.btrfs.base.ItemType;
import discUtils.btrfs.base.Key;
import discUtils.btrfs.base.NodeHeader;
import discUtils.btrfs.base.ReservedObjectId;
import discUtils.btrfs.base.Stripe;
import discUtils.btrfs.base.items.BaseItem;
import discUtils.btrfs.base.items.ChunkItem;
import discUtils.btrfs.base.items.ExtentData;
import discUtils.btrfs.base.items.RootItem;
import discUtils.core.internal.Crc32Algorithm;
import discUtils.core.internal.Crc32LittleEndian;
import discUtils.core.vfs.VfsContext;
import dotnet4j.io.IOException;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;
import vavi.util.ByteUtil;


public class Context extends VfsContext {

    public Context(BtrfsFileSystemOptions options) {
        fsTrees = new HashMap<>();
        this.options = options;
    }

    private BtrfsFileSystemOptions options;

    public BtrfsFileSystemOptions getOptions() {
        return options;
    }

    public void setOptions(BtrfsFileSystemOptions value) {
        options = value;
    }

    private Stream rawStream;

    public Stream getRawStream() {
        return rawStream;
    }

    public void setRawStream(Stream value) {
        rawStream = value;
    }

    private SuperBlock superBlock;

    public SuperBlock getSuperBlock() {
        return superBlock;
    }

    public void setSuperBlock(SuperBlock value) {
        superBlock = value;
    }

    private NodeHeader chunkTreeRoot;

    NodeHeader getChunkTreeRoot() {
        return chunkTreeRoot;
    }

    void setChunkTreeRoot(NodeHeader value) {
        chunkTreeRoot = value;
    }

    private NodeHeader rootTreeRoot;

    NodeHeader getRootTreeRoot() {
        return rootTreeRoot;
    }

    void setRootTreeRoot(NodeHeader value) {
        rootTreeRoot = value;
    }

    private Map<Long, NodeHeader> fsTrees;

    Map<Long, NodeHeader> getFsTrees() {
        return fsTrees;
    }

    NodeHeader getFsTree(long treeId) {
        NodeHeader tree;
        if (fsTrees.containsKey(treeId)) {
            return fsTrees.get(treeId);
        }
        RootItem rootItem = getRootTreeRoot().findFirst(RootItem.class, new Key(treeId, ItemType.RootItem), this);
        if (rootItem == null)
            return null;
        tree = readTree(rootItem.getByteNr(), rootItem.getLevel());
        fsTrees.put(treeId, tree);
        return tree;
    }

    public long mapToPhysical(long logical) {
        if (getChunkTreeRoot() != null) {
            List<ChunkItem> nodes = getChunkTreeRoot()
                    .find(ChunkItem.class, new Key(ReservedObjectId.FirstChunkTree, ItemType.ChunkItem), this);
            for (ChunkItem chunk : nodes) {
                if (chunk.getKey().getItemType() != ItemType.ChunkItem)
                    continue;
                if (chunk.getKey().getOffset() > logical)
                    continue;
                if (chunk.getKey().getOffset() + chunk.getChunkSize() < logical)
                    continue;
                checkStriping(chunk.getType());
                if (chunk.getStripeCount() < 1)
                    throw new IOException("Invalid stripe count in ChunkItem");
                Stripe stripe = chunk.getStripes()[0];
                return stripe.getOffset() + (logical - chunk.getKey().getOffset());
            }
        }
        for (ChunkItem chunk : superBlock.getSystemChunkArray()) {
            if (chunk.getKey().getItemType() != ItemType.ChunkItem)
                continue;
            if (chunk.getKey().getOffset() > logical)
                continue;
            if (chunk.getKey().getOffset() + chunk.getChunkSize() < logical)
                continue;

            checkStriping(chunk.getType());
            if (chunk.getStripeCount() < 1)
                throw new IOException("Invalid stripe count in ChunkItem");
            Stripe stripe = chunk.getStripes()[0];
            return stripe.getOffset() + (logical - chunk.getKey().getOffset());
        }
        throw new IOException("no matching ChunkItem found");
    }

    public NodeHeader readTree(long logical, int level) {
        long physical = mapToPhysical(logical);
        rawStream.seek(physical, SeekOrigin.Begin);
        int dataSize = level > 0 ? superBlock.getNodeSize() : superBlock.getLeafSize();
        byte[] buffer = new byte[dataSize];
        rawStream.read(buffer, 0, buffer.length);
        NodeHeader result = NodeHeader.create(buffer, 0);
        verifyChecksum(result.getChecksum(), buffer, 0x20, dataSize - 0x20);
        return result;
    }

    void verifyChecksum(byte[] checksum, byte[] data, int offset, int count) {
        if (!getOptions().verifyChecksums())
            return;
        if (superBlock.getChecksumType() != ChecksumType.Crc32C)
            throw new IllegalArgumentException("Unsupported ChecksumType {SuperBlock.ChecksumType}");
        Crc32LittleEndian crc = new Crc32LittleEndian(Crc32Algorithm.Castagnoli);
        crc.process(data, offset, count);
        byte[] calculated = new byte[4];
        ByteUtil.writeLeInt(crc.getValue(), calculated, 0);
        for (int i = 0; i < calculated.length; i++) {
            if (calculated[i] != checksum[i])
                throw new IOException("Invalid checksum");
        }
    }

    private void checkStriping(EnumSet<BlockGroupFlag> flags) {
        if (flags.contains(BlockGroupFlag.Raid0))
            throw new IOException("Raid0 not supported");

        if (flags.contains(BlockGroupFlag.Raid10))
            throw new IOException("Raid10 not supported");

        if (flags.contains(BlockGroupFlag.Raid5))
            throw new IOException("Raid5 not supported");

        if (flags.contains(BlockGroupFlag.Raid6))
            throw new IOException("Raid6 not supported");
    }

    BaseItem findKey(ReservedObjectId objectId, ItemType type) {
        return findKey(objectId.getValue(), type);
    }

    BaseItem findKey(long objectId, ItemType type) {
        Key key = new Key(objectId, type);
        return findKey(key);
    }

    BaseItem findKey(Key key) {
        return switch (key.getItemType()) {
            case RootItem, DirItem -> rootTreeRoot.findFirst(key, this);
            default -> throw new UnsupportedOperationException();
        };
    }

    List<BaseItem> findKey_(long treeId, Key key) {
        NodeHeader tree = getFsTree(treeId);
        return switch (key.getItemType()) {
            case DirItem -> tree.find(key, this);
            default -> throw new UnsupportedOperationException();
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    <T extends BaseItem> List<T> findKey(long treeId, Key key) {
        NodeHeader tree = getFsTree(treeId);
        return switch (key.getItemType()) {
            case DirItem, ExtentData -> (List) tree.find(ExtentData.class, key, this);
            default -> throw new UnsupportedOperationException();
        };
    }
}
