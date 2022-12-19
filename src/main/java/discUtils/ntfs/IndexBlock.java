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

import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;
import vavi.util.ByteUtil;


public class IndexBlock extends FixupRecordBase {

    /**
     * Size of meta-data placed at start of a block.
     */
    private static final int FieldSize = 0x18;

    private final Index index;

    // Virtual Cluster Number (maybe in sectors sometimes...?)
    private long indexBlockVcn;

    private final boolean isRoot;

    private long logSequenceNumber;

    private final long streamPosition;

    public IndexBlock(Index index, boolean isRoot, IndexEntry parentEntry, BiosParameterBlock bpb) {
        super("INDX", bpb.getBytesPerSector());
        this.index = index;
        this.isRoot = isRoot;
        Stream stream = index.getAllocationStream();
        streamPosition = index.indexBlockVcnToPosition(parentEntry.getChildrenVirtualCluster());
        stream.position(streamPosition);
        byte[] buffer = StreamUtilities.readExact(stream, index.getIndexBufferSize());
        fromBytes(buffer, 0);
    }

    private IndexBlock(Index index, boolean isRoot, long vcn, BiosParameterBlock bpb) {
        super("INDX", bpb.getBytesPerSector(), bpb.getIndexBufferSize());
        this.index = index;
        this.isRoot = isRoot;
        indexBlockVcn = vcn;
        streamPosition = vcn * bpb.getBytesPerSector() * bpb.getSectorsPerCluster();
        setNode(new IndexNode(this::writeToDisk, getUpdateSequenceSize(), this.index, isRoot, bpb.getIndexBufferSize() - FieldSize));
        writeToDisk();
    }

    private IndexNode node;

    public IndexNode getNode() {
        return node;
    }

    public void setNode(IndexNode value) {
        node = value;
    }

    public static IndexBlock initialize(Index index, boolean isRoot, IndexEntry parentEntry, BiosParameterBlock bpb) {
        return new IndexBlock(index, isRoot, parentEntry.getChildrenVirtualCluster(), bpb);
    }

    public void writeToDisk() {
        byte[] buffer = new byte[index.getIndexBufferSize()];
        toBytes(buffer, 0);
        Stream stream = index.getAllocationStream();
        stream.position(streamPosition);
        stream.write(buffer, 0, buffer.length);
        stream.flush();
    }

    protected void read(byte[] buffer, int offset) {
        // Skip FixupRecord fields...
        logSequenceNumber = ByteUtil.readLeLong(buffer, offset + 0x08);
        indexBlockVcn = ByteUtil.readLeLong(buffer, offset + 0x10);
        setNode(new IndexNode(this::writeToDisk, getUpdateSequenceSize(), index, isRoot, buffer, offset + FieldSize));
    }

    protected short write(byte[] buffer, int offset) {
        ByteUtil.writeLeLong(logSequenceNumber, buffer, offset + 0x08);
        ByteUtil.writeLeLong(indexBlockVcn, buffer, offset + 0x10);
        return (short) (FieldSize + getNode().writeTo(buffer, offset + FieldSize));
    }

    protected int calcSize() {
        throw new UnsupportedOperationException();
    }
}
