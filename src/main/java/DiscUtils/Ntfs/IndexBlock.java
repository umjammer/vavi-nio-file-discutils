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

import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.Stream;


public class IndexBlock extends FixupRecordBase {
    /**
     * Size of meta-data placed at start of a block.
     */
    private static final int FieldSize = 0x18;

    private final Index _index;

    // Virtual Cluster Number (maybe in sectors sometimes...?)
    private long _indexBlockVcn;

    private final boolean _isRoot;

    private long _logSequenceNumber;

    private final long _streamPosition;

    public IndexBlock(Index index, boolean isRoot, IndexEntry parentEntry, BiosParameterBlock bpb) {
        super("INDX", bpb._bytesPerSector);
        _index = index;
        _isRoot = isRoot;
        Stream stream = index.getAllocationStream();
        _streamPosition = index.indexBlockVcnToPosition(parentEntry.getChildrenVirtualCluster());
        stream.setPosition(_streamPosition);
        byte[] buffer = StreamUtilities.readExact(stream, index.getIndexBufferSize());
        fromBytes(buffer, 0);
    }

    private IndexBlock(Index index, boolean isRoot, long vcn, BiosParameterBlock bpb) {
        super("INDX", bpb._bytesPerSector, bpb.getIndexBufferSize());
        _index = index;
        _isRoot = isRoot;
        _indexBlockVcn = vcn;
        _streamPosition = vcn * bpb._bytesPerSector * bpb._sectorsPerCluster;
        setNode(new IndexNode(this::writeToDisk, getUpdateSequenceSize(), _index, isRoot, bpb.getIndexBufferSize() - FieldSize));
        writeToDisk();
    }

    private IndexNode _node;

    public IndexNode getNode() {
        return _node;
    }

    public void setNode(IndexNode value) {
        _node = value;
    }

    public static IndexBlock initialize(Index index, boolean isRoot, IndexEntry parentEntry, BiosParameterBlock bpb) {
        return new IndexBlock(index, isRoot, parentEntry.getChildrenVirtualCluster(), bpb);
    }

    public void writeToDisk() {
        byte[] buffer = new byte[_index.getIndexBufferSize()];
        toBytes(buffer, 0);
        Stream stream = _index.getAllocationStream();
        stream.setPosition(_streamPosition);
        stream.write(buffer, 0, buffer.length);
        stream.flush();
    }

    protected void read(byte[] buffer, int offset) {
        // Skip FixupRecord fields...
        _logSequenceNumber = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x08);
        _indexBlockVcn = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x10);
        setNode(new IndexNode(this::writeToDisk, getUpdateSequenceSize(), _index, _isRoot, buffer, offset + FieldSize));
    }

    protected short write(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(_logSequenceNumber, buffer, offset + 0x08);
        EndianUtilities.writeBytesLittleEndian(_indexBlockVcn, buffer, offset + 0x10);
        return (short) (FieldSize + getNode().writeTo(buffer, offset + FieldSize));
    }

    protected int calcSize() {
        throw new UnsupportedOperationException();
    }
}
