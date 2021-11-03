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

package DiscUtils.Ntfs;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import vavi.util.Debug;

import DiscUtils.Core.Internal.ObjectCache;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.Tuple;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;


class Index implements Closeable {
    private final ObjectCache<Long, IndexBlock> _blockCache;

    protected BiosParameterBlock _bpb;

    private final Comparator<byte[]> _comparer;

    protected File _file;

    private Bitmap _indexBitmap;

    protected String _name;

    private final IndexRoot _root;

    private final IndexNode _rootNode;

    public Index(File file, String name, BiosParameterBlock bpb, UpperCase upCase) {
        _file = file;
        _name = name;
        _bpb = bpb;
        _isFileIndex = name.equals("$I30");

        _blockCache = new ObjectCache<>();

        _root = _file.getStream(AttributeType.IndexRoot, _name).getContent(IndexRoot.class);
        _comparer = _root.getCollator(upCase);

        try (Stream s = _file.openStream(AttributeType.IndexRoot, _name, FileAccess.Read)) {
            byte[] buffer = StreamUtilities.readExact(s, (int) s.getLength());
            _rootNode = new IndexNode(this::writeRootNodeToDisk, 0, this, true, buffer, IndexRoot.HeaderOffset);
            // Give the attribute some room to breathe, so long as it doesn't
            // squeeze others out BROKEN, BROKEN, BROKEN - how to figure this
            // out? Query at the point of adding entries to the root node?
            _rootNode.setTotalSpaceAvailable(_rootNode.getTotalSpaceAvailable() +
                                             (_file.mftRecordFreeSpace(AttributeType.IndexRoot, _name) - 100));
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }

        if (_file.streamExists(AttributeType.IndexAllocation, _name)) {
            setAllocationStream(_file.openStream(AttributeType.IndexAllocation, _name, FileAccess.ReadWrite));
        }

        if (_file.streamExists(AttributeType.Bitmap, _name)) {
            _indexBitmap = new Bitmap(_file.openStream(AttributeType.Bitmap, _name, FileAccess.ReadWrite), Long.MAX_VALUE);
        }
    }

    private Index(AttributeType attrType,
            AttributeCollationRule collationRule,
            File file,
            String name,
            BiosParameterBlock bpb,
            UpperCase upCase) {
        _file = file;
        _name = name;
        _bpb = bpb;
        _isFileIndex = name.equals("$I30");

        _blockCache = new ObjectCache<>();

        _file.createStream(AttributeType.IndexRoot, _name);

        _root = new IndexRoot();
        _root.setAttributeType(attrType != null ? attrType.ordinal() : 0);
        _root.setCollationRule(collationRule);
        _root.setIndexAllocationSize(bpb.getIndexBufferSize());
        _root.setRawClustersPerIndexRecord(bpb._rawIndexBufferSize);

        _comparer = _root.getCollator(upCase);

        _rootNode = new IndexNode(this::writeRootNodeToDisk, 0, this, true, 32);
    }

    private Stream _allocationStream;

    Stream getAllocationStream() {
        return _allocationStream;
    }

    void setAllocationStream(Stream value) {
        _allocationStream = value;
    }

    public int getCount() {
        return getEntries().size();
    }

    public List<Tuple<byte[], byte[]>> getEntries() {
        List<Tuple<byte[], byte[]>> result = new ArrayList<>();
        for (IndexEntry entry : enumerate(_rootNode)) {
            result.add(new Tuple<>(entry.getKeyBuffer(), entry.getDataBuffer()));
        }
        return result;
    }

    int getIndexBufferSize() {
        return _root.getIndexAllocationSize();
    }

    private boolean _isFileIndex;

    boolean isFileIndex() {
        return _isFileIndex;
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
        _rootNode.setTotalSpaceAvailable(_rootNode.calcSize() + _file.mftRecordFreeSpace(AttributeType.IndexRoot, _name));

        if (_rootNode.tryFindEntry(key, oldEntry, node)) {
            node[0].updateEntry(key, value);
        } else {
            _rootNode.addEntry(key, value);
        }
    }

    public void close() throws IOException {
        if (_indexBitmap != null) {
            _indexBitmap.close();
            _indexBitmap = null;
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
        for (IndexEntry entry : findAllIn(query, _rootNode)) {
            result.add(new Tuple<>(entry.getKeyBuffer(), entry.getDataBuffer()));
        }
        return result;
    }

    public boolean containsKey(byte[] key) {
        return tryGetValue(key, new byte[1][]);
    }

    public boolean remove(byte[] key) {
        _rootNode.setTotalSpaceAvailable(_rootNode.calcSize() + _file.mftRecordFreeSpace(AttributeType.IndexRoot, _name));
        IndexEntry[] overflowEntry = new IndexEntry[1];
        boolean found = _rootNode.removeEntry(key, overflowEntry);
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

        if (_rootNode.tryFindEntry(key, entry, node)) {
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
            keyValue = new DiscUtils.Ntfs.ObjectIds.IndexKey();
            dataValue = new ObjectIdRecord();
        } else if (fileName.equals("$Reparse") && indexName.equals("$R")) {
            keyValue = new DiscUtils.Ntfs.ReparsePoints.Key();
            dataValue = new DiscUtils.Ntfs.ReparsePoints.Data();
        } else if (fileName.equals("$Quota")) {
            if (indexName.equals("$O")) {
                keyValue = new DiscUtils.Ntfs.Quotas.OwnerKey();
                dataValue = new DiscUtils.Ntfs.Quotas.OwnerRecord();
            } else if (indexName.equals("$Q")) {
                keyValue = new DiscUtils.Ntfs.Quotas.OwnerRecord();
                dataValue = new DiscUtils.Ntfs.Quotas.QuotaRecord();
            }
        } else if (fileName.equals("$Secure")) {
            if (indexName.equals("$SII")) {
                keyValue = new DiscUtils.Ntfs.SecurityDescriptors.IdIndexKey();
                dataValue = new DiscUtils.Ntfs.SecurityDescriptors.IdIndexData();
            } else if (indexName.equals("$SDH")) {
                keyValue = new DiscUtils.Ntfs.SecurityDescriptors.HashIndexKey();
                dataValue = new DiscUtils.Ntfs.SecurityDescriptors.IdIndexData();
            }
        }

        try {
            if (keyValue != null && dataValue != null) {
                keyValue.readFrom(entry.getKeyBuffer(), 0);
                dataValue.readFrom(entry.getDataBuffer(), 0);
                return "{" + keyValue + "-->" + dataValue + "}";
            }
        } catch (Exception e) {
            Debug.printStackTrace(e);
            return "{Parsing-Error}";
        }

        Debug.println(Level.WARNING, indexName);
        return "{Unknown-Index-Type}";
    }

    long indexBlockVcnToPosition(long vcn) {
        if (vcn % _root.getRawClustersPerIndexRecord() != 0) {
            throw new UnsupportedOperationException("Unexpected vcn (not a multiple of clusters-per-index-record): vcn=" + vcn +
                                                    " rcpir=" + _root.getRawClustersPerIndexRecord());
        }

        if (_bpb.getBytesPerCluster() <= _root.getIndexAllocationSize()) {
            return vcn * _bpb.getBytesPerCluster();
        }

        if (_root.getRawClustersPerIndexRecord() != 8) {
            throw new UnsupportedOperationException("Unexpected RawClustersPerIndexRecord (multiple index blocks per cluster): " +
                                                    _root.getRawClustersPerIndexRecord());
        }

        return vcn / _root.getRawClustersPerIndexRecord() * _root.getIndexAllocationSize();
    }

    boolean shrinkRoot() {
        if (_rootNode.depose()) {
            writeRootNodeToDisk();
            _rootNode.setTotalSpaceAvailable(_rootNode.calcSize() + _file.mftRecordFreeSpace(AttributeType.IndexRoot, _name));
            return true;
        }

        return false;
    }

    IndexBlock getSubBlock(IndexEntry parentEntry) {
        IndexBlock block = _blockCache.get(parentEntry.getChildrenVirtualCluster());
        if (block == null) {
            block = new IndexBlock(this, false, parentEntry, _bpb);
            _blockCache.put(parentEntry.getChildrenVirtualCluster(), block);
        }

        return block;
    }

    IndexBlock allocateBlock(IndexEntry parentEntry) {
        if (getAllocationStream() == null) {
            _file.createStream(AttributeType.IndexAllocation, _name);
            setAllocationStream(_file.openStream(AttributeType.IndexAllocation, _name, FileAccess.ReadWrite));
        }

        if (_indexBitmap == null) {
            _file.createStream(AttributeType.Bitmap, _name);
            _indexBitmap = new Bitmap(_file.openStream(AttributeType.Bitmap, _name, FileAccess.ReadWrite), Long.MAX_VALUE);
        }

        long idx = _indexBitmap.allocateFirstAvailable(0);
        parentEntry.setChildrenVirtualCluster(idx * MathUtilities.ceil(_bpb.getIndexBufferSize(),
                                                                       _bpb.getSectorsPerCluster() * _bpb.getBytesPerSector()));
        parentEntry.getFlags().add(IndexEntryFlags.Node);
        IndexBlock block = IndexBlock.initialize(this, false, parentEntry, _bpb);
        _blockCache.put(parentEntry.getChildrenVirtualCluster(), block);
        return block;
    }

    void freeBlock(long vcn) {
        long idx = vcn / MathUtilities.ceil(_bpb.getIndexBufferSize(), _bpb.getSectorsPerCluster() * _bpb.getBytesPerSector());
        _indexBitmap.markAbsent(idx);
        _blockCache.remove(vcn);
    }

    int compare(byte[] x, byte[] y) {
//Debug.println(_comparer.getClass() + ": " + _comparer.compare(x, y));
        return _comparer.compare(x, y);
    }

    void dump(PrintWriter writer, String prefix) {
        nodeAsString(writer, prefix, _rootNode, "R");
    }

    protected List<IndexEntry> enumerate(IndexNode node) {
        List<IndexEntry> result = new ArrayList<>();
        for (IndexEntry focus : node.getEntries()) {
            if (focus.getFlags().contains(IndexEntryFlags.Node)) {
                IndexBlock block = getSubBlock(focus);
                for (IndexEntry subEntry : enumerate(block.getNode())) {
                    result.add(subEntry);
                }
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
                for (IndexEntry entry : findAllIn(query, block.getNode())) {
                    result.add(entry);
                }
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
        _rootNode.getHeader()._allocatedSizeOfEntries = _rootNode.calcSize();
        byte[] buffer = new byte[_rootNode.getHeader()._allocatedSizeOfEntries + _root.size()];
        _root.writeTo(buffer, 0);
        _rootNode.writeTo(buffer, _root.size());
        try (Stream s = _file.openStream(AttributeType.IndexRoot, _name, FileAccess.Write)) {
            s.setPosition(0);
            s.write(buffer, 0, buffer.length);
            s.setLength(s.getPosition());
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
                writer.println(prefix + "      " + entryAsString(entry, _file.getBestName(), _name));
            }
            if (entry.getFlags().contains(IndexEntryFlags.Node)) {
                nodeAsString(writer,
                             prefix + "        ",
                             getSubBlock(entry).getNode(),
                             ":i" + entry.getChildrenVirtualCluster());
            }
        }
    }

    public String toString() {
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
