//
// Copyright (c) 2008-2011, Kenneth Bell
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

package discUtils.ntfs;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import discUtils.core.internal.ObjectCache;
import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;
import dotnet4j.util.compat.Tuple;

import static java.lang.System.getLogger;


class Index implements Closeable {

    private static final Logger logger = getLogger(Index.class.getName());

    private final ObjectCache<Long, IndexBlock> blockCache;

    protected BiosParameterBlock bpb;

    private final Comparator<byte[]> comparer;

    protected File file;

    private Bitmap indexBitmap;

    protected String name;

    private final IndexRoot root;

    private final IndexNode rootNode;

    public Index(File file, String name, BiosParameterBlock bpb, UpperCase upCase) {
        this.file = file;
        this.name = name;
        this.bpb = bpb;
        isFileIndex = name.equals("$I30");

        blockCache = new ObjectCache<>();

        root = this.file.getStream(AttributeType.IndexRoot, this.name).getContent(IndexRoot.class);
        comparer = root.getCollator(upCase);

        try (Stream s = this.file.openStream(AttributeType.IndexRoot, this.name, FileAccess.Read)) {
            byte[] buffer = StreamUtilities.readExact(s, (int) s.getLength());
            rootNode = new IndexNode(this::writeRootNodeToDisk, 0, this, true, buffer, IndexRoot.HeaderOffset);
            // Give the attribute some room to breathe, so long as it doesn't
            // squeeze others out BROKEN, BROKEN, BROKEN - how to figure this
            // out? Query at the point of adding entries to the root node?
            rootNode.setTotalSpaceAvailable(rootNode.getTotalSpaceAvailable() +
                                             (this.file.mftRecordFreeSpace(AttributeType.IndexRoot, this.name) - 100));
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }

        if (this.file.streamExists(AttributeType.IndexAllocation, this.name)) {
            setAllocationStream(this.file.openStream(AttributeType.IndexAllocation, this.name, FileAccess.ReadWrite));
        }

        if (this.file.streamExists(AttributeType.Bitmap, this.name)) {
            indexBitmap = new Bitmap(this.file.openStream(AttributeType.Bitmap, this.name, FileAccess.ReadWrite), Long.MAX_VALUE);
        }
    }

    private Index(AttributeType attrType,
            AttributeCollationRule collationRule,
            File file,
            String name,
            BiosParameterBlock bpb,
            UpperCase upCase) {
        this.file = file;
        this.name = name;
        this.bpb = bpb;
        isFileIndex = name.equals("$I30");

        blockCache = new ObjectCache<>();

        this.file.createStream(AttributeType.IndexRoot, this.name);

        root = new IndexRoot();
        root.setAttributeType(attrType != null ? attrType.ordinal() : 0);
        root.setCollationRule(collationRule);
        root.setIndexAllocationSize(bpb.getIndexBufferSize());
        root.setRawClustersPerIndexRecord(bpb.rawIndexBufferSize);

        comparer = root.getCollator(upCase);

        rootNode = new IndexNode(this::writeRootNodeToDisk, 0, this, true, 32);
    }

    private Stream allocationStream;

    Stream getAllocationStream() {
        return allocationStream;
    }

    void setAllocationStream(Stream value) {
        allocationStream = value;
    }

    public int getCount() {
        return getEntries().size();
    }

    public List<Tuple<byte[], byte[]>> getEntries() {
        List<Tuple<byte[], byte[]>> result = new ArrayList<>();
        for (IndexEntry entry : enumerate(rootNode)) {
            result.add(new Tuple<>(entry.getKeyBuffer(), entry.getDataBuffer()));
        }
        return result;
    }

    int getIndexBufferSize() {
        return root.getIndexAllocationSize();
    }

    private boolean isFileIndex;

    boolean isFileIndex() {
        return isFileIndex;
    }

    public byte[] get(byte[] key) {
        byte[][] value = new byte[1][];
        if (tryGetValue(key, value)) {
            return value[0];
        }
        throw new NoSuchElementException(Arrays.toString(key));
    }

    public void put(byte[] key, byte[] value) {
        IndexEntry[] oldEntry = new IndexEntry[1];
        IndexNode[] node = new IndexNode[1];
        rootNode.setTotalSpaceAvailable(rootNode.calcSize() + file.mftRecordFreeSpace(AttributeType.IndexRoot, name));

        if (rootNode.tryFindEntry(key, oldEntry, node)) {
            node[0].updateEntry(key, value);
        } else {
            rootNode.addEntry(key, value);
        }
    }

    @Override
    public void close() throws IOException {
        if (indexBitmap != null) {
            indexBitmap.close();
            indexBitmap = null;
        }
    }

    public static void create(AttributeType attrType, AttributeCollationRule collationRule, File file, String name) {
        Index idx = new Index(attrType,
                              collationRule,
                              file,
                              name,
                              file.getContext().getBiosParameterBlock(),
                              file.getContext().getUpperCase());
        idx.writeRootNodeToDisk();
    }

    public List<Tuple<byte[], byte[]>> findAll(Comparable<byte[]> query) {
        List<Tuple<byte[], byte[]>> result = new ArrayList<>();
        for (IndexEntry entry : findAllIn(query, rootNode)) {
            result.add(new Tuple<>(entry.getKeyBuffer(), entry.getDataBuffer()));
        }
        return result;
    }

    public boolean containsKey(byte[] key) {
        return tryGetValue(key, new byte[1][]);
    }

    public boolean remove(byte[] key) {
        rootNode.setTotalSpaceAvailable(rootNode.calcSize() + file.mftRecordFreeSpace(AttributeType.IndexRoot, name));
        IndexEntry[] overflowEntry = new IndexEntry[1];
        boolean found = rootNode.removeEntry(key, overflowEntry);
        if (overflowEntry[0] != null) {
            throw new dotnet4j.io.IOException("Error removing entry, root overflowed");
        }

        return found;
    }

    /**
     * @param value {@cs out}
     */
    public boolean tryGetValue(byte[] key, byte[][] value) {
        IndexEntry[] entry = new IndexEntry[1];
        IndexNode[] node = new IndexNode[1];

        if (rootNode.tryFindEntry(key, entry, node)) {
            value[0] = entry[0].getDataBuffer();
            return true;
        }

        value[0] = null;
        return false;
    }

    static String entryAsString(IndexEntry entry, String fileName, String indexName) {
        IByteArraySerializable keyValue = null;
        IByteArraySerializable dataValue = null;
        // Try to guess the type of data in the key and data fields from the
        // filename
        // and index name
        if (indexName.equals("$I30")) {
            keyValue = new FileNameRecord();
            dataValue = new FileRecordReference();
        } else if (fileName.equals("$ObjId") && indexName.equals("$O")) {
            keyValue = new discUtils.ntfs.ObjectIds.IndexKey();
            dataValue = new ObjectIdRecord();
        } else if (fileName.equals("$Reparse") && indexName.equals("$R")) {
            keyValue = new discUtils.ntfs.ReparsePoints.Key();
            dataValue = new discUtils.ntfs.ReparsePoints.Data();
        } else if (fileName.equals("$Quota")) {
            if (indexName.equals("$O")) {
                keyValue = new discUtils.ntfs.Quotas.OwnerKey();
                dataValue = new discUtils.ntfs.Quotas.OwnerRecord();
            } else if (indexName.equals("$Q")) {
                keyValue = new discUtils.ntfs.Quotas.OwnerRecord();
                dataValue = new discUtils.ntfs.Quotas.QuotaRecord();
            }
        } else if (fileName.equals("$Secure")) {
            if (indexName.equals("$SII")) {
                keyValue = new discUtils.ntfs.SecurityDescriptors.IdIndexKey();
                dataValue = new discUtils.ntfs.SecurityDescriptors.IdIndexData();
            } else if (indexName.equals("$SDH")) {
                keyValue = new discUtils.ntfs.SecurityDescriptors.HashIndexKey();
                dataValue = new discUtils.ntfs.SecurityDescriptors.IdIndexData();
            }
        }

        try {
            if (keyValue != null && dataValue != null) {
                keyValue.readFrom(entry.getKeyBuffer(), 0);
                dataValue.readFrom(entry.getDataBuffer(), 0);
                return "{" + keyValue + "-->" + dataValue + "}";
            }
        } catch (Exception e) {
            logger.log(Level.DEBUG, e.getMessage(), e);
            return "{Parsing-Error}";
        }

        logger.log(Level.WARNING, indexName);
        return "{Unknown-Index-Type}";
    }

    long indexBlockVcnToPosition(long vcn) {
        if (vcn % root.getRawClustersPerIndexRecord() != 0) {
            throw new UnsupportedOperationException("Unexpected vcn (not a multiple of clusters-per-index-record): vcn=" + vcn +
                                                    " rcpir=" + root.getRawClustersPerIndexRecord());
        }

        if (bpb.getBytesPerCluster() <= root.getIndexAllocationSize()) {
            return vcn * bpb.getBytesPerCluster();
        }

        if (root.getRawClustersPerIndexRecord() != 8) {
            throw new UnsupportedOperationException("Unexpected RawClustersPerIndexRecord (multiple index blocks per cluster): " +
                                                    root.getRawClustersPerIndexRecord());
        }

        return vcn / root.getRawClustersPerIndexRecord() * root.getIndexAllocationSize();
    }

    boolean shrinkRoot() {
        if (rootNode.depose()) {
            writeRootNodeToDisk();
            rootNode.setTotalSpaceAvailable(rootNode.calcSize() + file.mftRecordFreeSpace(AttributeType.IndexRoot, name));
            return true;
        }

        return false;
    }

    IndexBlock getSubBlock(IndexEntry parentEntry) {
        IndexBlock block = blockCache.get(parentEntry.getChildrenVirtualCluster());
        if (block == null) {
            block = new IndexBlock(this, false, parentEntry, bpb);
            blockCache.put(parentEntry.getChildrenVirtualCluster(), block);
        }

        return block;
    }

    IndexBlock allocateBlock(IndexEntry parentEntry) {
        if (getAllocationStream() == null) {
            file.createStream(AttributeType.IndexAllocation, name);
            setAllocationStream(file.openStream(AttributeType.IndexAllocation, name, FileAccess.ReadWrite));
        }

        if (indexBitmap == null) {
            file.createStream(AttributeType.Bitmap, name);
            indexBitmap = new Bitmap(file.openStream(AttributeType.Bitmap, name, FileAccess.ReadWrite), Long.MAX_VALUE);
        }

        long idx = indexBitmap.allocateFirstAvailable(0);
        parentEntry.setChildrenVirtualCluster(idx * MathUtilities.ceil(bpb.getIndexBufferSize(),
                                                                       bpb.getSectorsPerCluster() * bpb.getBytesPerSector()));
        parentEntry.getFlags().add(IndexEntryFlags.Node);
        IndexBlock block = IndexBlock.initialize(this, false, parentEntry, bpb);
        blockCache.put(parentEntry.getChildrenVirtualCluster(), block);
        return block;
    }

    void freeBlock(long vcn) {
        long idx = vcn / MathUtilities.ceil(bpb.getIndexBufferSize(), bpb.getSectorsPerCluster() * bpb.getBytesPerSector());
        indexBitmap.markAbsent(idx);
        blockCache.remove(vcn);
    }

    int compare(byte[] x, byte[] y) {
//logger.log(Level.DEBUG, comparer.getClass() + ": " + comparer.compare(x, y));
        return comparer.compare(x, y);
    }

    void dump(PrintWriter writer, String prefix) {
        nodeAsString(writer, prefix, rootNode, "R");
    }

    protected List<IndexEntry> enumerate(IndexNode node) {
        List<IndexEntry> result = new ArrayList<>();
        for (IndexEntry focus : node.getEntries()) {
            if (focus.getFlags().contains(IndexEntryFlags.Node)) {
                IndexBlock block = getSubBlock(focus);
                result.addAll(enumerate(block.getNode()));
            }

            if (!focus.getFlags().contains(IndexEntryFlags.End)) {
                result.add(focus);
            }
        }
        return result;
    }

    private List<IndexEntry> findAllIn(Comparable<byte[]> query, IndexNode node) {
        List<IndexEntry> result = new ArrayList<>();
        for (IndexEntry focus : node.getEntries()) {
            boolean searchChildren = true;
            boolean matches = false;
            boolean keepIterating = true;

            if (!focus.getFlags().contains(IndexEntryFlags.End)) {
                int compVal = query.compareTo(focus.getKeyBuffer());
                if (compVal == 0) {
                    matches = true;
                } else if (compVal > 0) {
                    searchChildren = false;
                } else if (compVal < 0) {
                    keepIterating = false;
                }
            }

            if (searchChildren && focus.getFlags().contains(IndexEntryFlags.Node)) {
                IndexBlock block = getSubBlock(focus);
                result.addAll(findAllIn(query, block.getNode()));
            }

            if (matches) {
                result.add(focus);
            }

            if (!keepIterating) {
                break;
            }
        }
        return result;
    }

    private void writeRootNodeToDisk() {
        rootNode.getHeader().allocatedSizeOfEntries = rootNode.calcSize();
        byte[] buffer = new byte[rootNode.getHeader().allocatedSizeOfEntries + root.size()];
        root.writeTo(buffer, 0);
        rootNode.writeTo(buffer, root.size());
        try (Stream s = file.openStream(AttributeType.IndexRoot, name, FileAccess.Write)) {
            s.position(0);
            s.write(buffer, 0, buffer.length);
            s.setLength(s.position());
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    private void nodeAsString(PrintWriter writer, String prefix, IndexNode node, String id) {
        writer.println(prefix + id + ":");
        for (IndexEntry entry : node.getEntries()) {
            if (entry.getFlags().contains(IndexEntryFlags.End)) {
                writer.println(prefix + "      E");
            } else {
                writer.println(prefix + "      " + entryAsString(entry, file.getBestName(), name));
            }
            if (entry.getFlags().contains(IndexEntryFlags.Node)) {
                nodeAsString(writer,
                             prefix + "        ",
                             getSubBlock(entry).getNode(),
                             ":i" + entry.getChildrenVirtualCluster());
            }
        }
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Index: {");
        getEntries().forEach(e -> {
            sb.append('"');
            sb.append(Arrays.toString(e.getKey()));
            sb.append('"');
            sb.append(": ");
            sb.append('"');
            sb.append(Arrays.toString(e.getValue()));
            sb.append('"');
            sb.append(", ");
        });
        sb.setLength(sb.length() - 2);
        sb.append("}");
        return sb.toString();
    }
}
