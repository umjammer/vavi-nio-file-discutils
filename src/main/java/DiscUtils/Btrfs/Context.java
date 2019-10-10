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

package DiscUtils.Btrfs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Btrfs.Base.BlockGroupFlag;
import DiscUtils.Btrfs.Base.ChecksumType;
import DiscUtils.Btrfs.Base.ItemType;
import DiscUtils.Btrfs.Base.Key;
import DiscUtils.Btrfs.Base.NodeHeader;
import DiscUtils.Btrfs.Base.ReservedObjectId;
import DiscUtils.Btrfs.Base.Stripe;
import DiscUtils.Btrfs.Base.Items.BaseItem;
import DiscUtils.Btrfs.Base.Items.ChunkItem;
import DiscUtils.Btrfs.Base.Items.ExtentData;
import DiscUtils.Btrfs.Base.Items.RootItem;
import DiscUtils.Core.Internal.Crc32Algorithm;
import DiscUtils.Core.Internal.Crc32LittleEndian;
import DiscUtils.Core.Vfs.VfsContext;
import DiscUtils.Streams.Util.EndianUtilities;
import moe.yo3explorer.dotnetio4j.IOException;
import moe.yo3explorer.dotnetio4j.SeekOrigin;
import moe.yo3explorer.dotnetio4j.Stream;


public class Context extends VfsContext {

    public Context(BtrfsFileSystemOptions options) {
        __FsTrees = new HashMap<>();
        setOptions(options);
    }

    private BtrfsFileSystemOptions __Options;

    public BtrfsFileSystemOptions getOptions() {
        return __Options;
    }

    public void setOptions(BtrfsFileSystemOptions value) {
        __Options = value;
    }

    private Stream __RawStream;

    public Stream getRawStream() {
        return __RawStream;
    }

    public void setRawStream(Stream value) {
        __RawStream = value;
    }

    private SuperBlock __SuperBlock;

    public SuperBlock getSuperBlock() {
        return __SuperBlock;
    }

    public void setSuperBlock(SuperBlock value) {
        __SuperBlock = value;
    }

    private NodeHeader __ChunkTreeRoot;

    public NodeHeader getChunkTreeRoot() {
        return __ChunkTreeRoot;
    }

    public void setChunkTreeRoot(NodeHeader value) {
        __ChunkTreeRoot = value;
    }

    private NodeHeader __RootTreeRoot;

    public NodeHeader getRootTreeRoot() {
        return __RootTreeRoot;
    }

    public void setRootTreeRoot(NodeHeader value) {
        __RootTreeRoot = value;
    }

    private Map<Long, NodeHeader> __FsTrees;

    public Map<Long, NodeHeader> getFsTrees() {
        return __FsTrees;
    }

    public NodeHeader getFsTree(long treeId) {
        NodeHeader tree;
        if (getFsTrees().containsKey(treeId)) {
            return getFsTrees().get(treeId);
        }
        RootItem rootItem = getRootTreeRoot().findFirst(RootItem.class, new Key(treeId, ItemType.RootItem), this);
        if (rootItem == null)
            return null;
        tree = readTree(rootItem.getByteNr(), rootItem.getLevel());
        getFsTrees().put(treeId, tree);
        return tree;
    }

    public long mapToPhysical(long logical) {
        if (getChunkTreeRoot() != null) {
            List<ChunkItem> nodes = getChunkTreeRoot().find(ChunkItem.class, new Key(ReservedObjectId.FirstChunkTree, ItemType.ChunkItem), this);
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

        for (ChunkItem chunk : getSuperBlock().getSystemChunkArray()) {
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

    public NodeHeader readTree(long logical, byte level) {
        long physical = mapToPhysical(logical);
        getRawStream().seek(physical, SeekOrigin.Begin);
        int dataSize = level > 0 ? getSuperBlock().getNodeSize() : getSuperBlock().getLeafSize();
        byte[] buffer = new byte[dataSize];
        getRawStream().read(buffer, 0, buffer.length);
        NodeHeader result = NodeHeader.create(buffer, 0);
        verifyChecksum(result.getChecksum(), buffer, 0x20, dataSize - 0x20);
        return result;
    }

    public void verifyChecksum(byte[] checksum, byte[] data, int offset, int count) {
        if (!getOptions().getVerifyChecksums())
            return;

        if (getSuperBlock().getChecksumType() != ChecksumType.Crc32C)
            throw new IllegalArgumentException("Unsupported ChecksumType {SuperBlock.ChecksumType}");

        Crc32LittleEndian crc = new Crc32LittleEndian(Crc32Algorithm.Castagnoli);
        crc.process(data, offset, count);
        byte[] calculated = new byte[4];
        EndianUtilities.writeBytesLittleEndian(crc.getValue(), calculated, 0);
        for (int i = 0; i < calculated.length; i++) {
            if (calculated[i] != checksum[i])
                throw new IOException("Invalid checksum");
        }
    }

    // TODO
    private void checkStriping(BlockGroupFlag flags) {
        if ((flags.ordinal() & BlockGroupFlag.Raid0.ordinal()) == BlockGroupFlag.Raid0.ordinal())
            throw new IOException("Raid0 not supported");

        if ((flags.ordinal() & BlockGroupFlag.Raid10.ordinal()) == BlockGroupFlag.Raid0.ordinal())
            throw new IOException("Raid10 not supported");

        if ((flags.ordinal() & BlockGroupFlag.Raid5.ordinal()) == BlockGroupFlag.Raid0.ordinal())
            throw new IOException("Raid5 not supported");

        if ((flags.ordinal() & BlockGroupFlag.Raid6.ordinal()) == BlockGroupFlag.Raid0.ordinal())
            throw new IOException("Raid6 not supported");
    }

    public BaseItem findKey(ReservedObjectId objectId, ItemType type) {
        return findKey(objectId.ordinal(), type);
    }

    public BaseItem findKey(long objectId, ItemType type) {
        Key key = new Key(objectId, type);
        return findKey(key);
    }

    public BaseItem findKey(Key key) {
        switch (key.getItemType()) {
        case RootItem:
            return getRootTreeRoot().findFirst(key, this);
        case DirItem:
            return getRootTreeRoot().findFirst(key, this);
        default:
            throw new UnsupportedOperationException();
        }
    }

    public List<BaseItem> findKey_(long treeId, Key key) {
        NodeHeader tree = getFsTree(treeId);
        switch (key.getItemType()) {
        case DirItem:
            return tree.find(key, this);
        default:
            throw new UnsupportedOperationException();
        }
    }

    public <T extends BaseItem> List<T> findKey__(long treeId, Key key) {
        NodeHeader tree = getFsTree(treeId);
        switch (key.getItemType()) {
        case DirItem:
        case ExtentData:
            return List.class.cast(tree.find(ExtentData.class, key, this));
        default:
            throw new UnsupportedOperationException();
        }
    }
}
