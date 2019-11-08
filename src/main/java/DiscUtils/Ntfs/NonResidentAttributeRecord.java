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

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;

import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Range;


public final class NonResidentAttributeRecord extends AttributeRecord {
    private static final short DefaultCompressionUnitSize = 4;

    private long _compressedSize;

    private short _compressionUnitSize;

    private long _dataAllocatedSize;

    private long _dataRealSize;

    private short _dataRunsOffset;

    private long _initializedDataSize;

    private long _lastVCN;

    private long _startingVCN;

    public NonResidentAttributeRecord(byte[] buffer, int offset, int[] length) {
        read(buffer, offset, length);
    }

    public NonResidentAttributeRecord(AttributeType type,
            String name,
            short id,
            EnumSet<AttributeFlags> flags,
            long firstCluster,
            long numClusters,
            int bytesPerCluster) {
        super(type, name, id, flags);

        _nonResidentFlag = 1;
        _dataRuns = new ArrayList<>();
        _dataRuns.add(new DataRun(firstCluster, numClusters, false));
        _lastVCN = numClusters - 1;
        _dataAllocatedSize = bytesPerCluster * numClusters;
        _dataRealSize = bytesPerCluster * numClusters;
        _initializedDataSize = bytesPerCluster * numClusters;

        if (!Collections.disjoint(flags, EnumSet.of(AttributeFlags.Compressed, AttributeFlags.Sparse))) {
            _compressionUnitSize = DefaultCompressionUnitSize;
        }
    }

    public NonResidentAttributeRecord(AttributeType type,
            String name,
            short id,
            EnumSet<AttributeFlags> flags,
            long startVcn,
            List<DataRun> dataRuns) {
        super(type, name, id, flags);

        _nonResidentFlag = 1;
        _dataRuns = dataRuns;
        _startingVCN = startVcn;

        if (!Collections.disjoint(flags, EnumSet.of(AttributeFlags.Compressed, AttributeFlags.Sparse))) {
            _compressionUnitSize = DefaultCompressionUnitSize;
        }

        if (dataRuns != null && dataRuns.size() != 0) {
            _lastVCN = _startingVCN;
            for (DataRun run : dataRuns) {
                _lastVCN += run.getRunLength();
            }

            _lastVCN -= 1;
        }
    }

    /**
     * The amount of space occupied by the attribute (in bytes).
     */
    public long getAllocatedLength() {
        return _dataAllocatedSize;
    }

    public void setAllocatedLength(long value) {
        _dataAllocatedSize = value;
    }

    public long getCompressedDataSize() {
        return _compressedSize;
    }

    public void setCompressedDataSize(long value) {
        _compressedSize = value;
    }

    /**
     * Gets or sets the size of a compression unit (in clusters).
     */
    public int getCompressionUnitSize() {
        return 1 << (_compressionUnitSize & 0xffff);
    }

    public void setCompressionUnitSize(int value) {
        _compressionUnitSize = (short) MathUtilities.log2(value);
    }

    /**
     * The amount of data in the attribute (in bytes).
     */
    public long getDataLength() {
        return _dataRealSize;
    }

    public void setDataLength(long value) {
        _dataRealSize = value;
    }

    private List<DataRun> _dataRuns;

    public List<DataRun> getDataRuns() {
        return _dataRuns;
    }

    public void setDataRuns(List<DataRun> value) {
        _dataRuns = value;
    }

    /**
     * The amount of initialized data in the attribute (in bytes).
     */
    public long getInitializedDataLength() {
        return _initializedDataSize;
    }

    public void setInitializedDataLength(long value) {
        _initializedDataSize = value;
    }

    public long getLastVcn() {
        return _lastVCN;
    }

    public void setLastVcn(long value) {
        _lastVCN = value;
    }

    public int getSize() {
        int nameLength = 0;
        int nameOffset = !Collections.disjoint(_flags, EnumSet.of(AttributeFlags.Compressed, AttributeFlags.Sparse)) ? 0x48 : 0x40;
        if (_name != null) {
            nameLength = _name.length();
        }

        int dataOffset = MathUtilities.roundUp(nameOffset + nameLength * 2, 8);

        // Write out data first, since we know where it goes...
        int dataLen = 0;
        for (DataRun run : _dataRuns) {
            dataLen += run.getSize();
        }

        dataLen++; // NULL terminator

        return MathUtilities.roundUp(dataOffset + dataLen, 8);
    }

    public long getStartVcn() {
        return _startingVCN;
    }

    public void replaceRun(DataRun oldRun, DataRun newRun) {
        int idx = _dataRuns.indexOf(oldRun);
        if (idx < 0) {
            throw new NoSuchElementException("Attempt to replace non-existant run: " + oldRun);
        }

        _dataRuns.set(idx, newRun);
//Debug.println("~~[" + idx + "]: " + newRun + " / " + StringUtil.paramString(getDataRuns()));
    }

    public int removeRun(DataRun run) {
        int idx = _dataRuns.indexOf(run);
        if (idx < 0) {
//Debug.println("-x: " + run + " / " + StringUtil.paramString(getDataRuns()));
            throw new NoSuchElementException("Attempt to remove non-existant run: " + run);
        }

        _dataRuns.remove(idx);
//Debug.println("--[" + idx + "]: " + run + " / " + StringUtil.paramString(getDataRuns()));
        return idx;
    }

    public void insertRun(DataRun existingRun, DataRun newRun) {
        int idx = _dataRuns.indexOf(existingRun);
        if (idx < 0) {
            throw new NoSuchElementException("Attempt to replace non-existant run: " + existingRun);
        }

        _dataRuns.add(idx + 1, newRun);
//Debug.println("+2[" + (idx + 1) + "]: " + newRun + " / " + StringUtil.paramString(getDataRuns()));
    }

    public void insertRun(int index, DataRun newRun) {
        _dataRuns.add(index, newRun);
//Debug.println("+1[" + index + "]: " + newRun + " / " + StringUtil.paramString(getDataRuns()));
    }

    public List<Range> getClusters() {
        List<DataRun> cookedRuns = _dataRuns;

        long start = 0;
        List<Range> result = new ArrayList<>(_dataRuns.size());
        for (DataRun run : cookedRuns) {
            if (!run.isSparse()) {
                start += run.getRunOffset();
                result.add(new Range(start, run.getRunLength()));
            }
        }

        return result;
    }

    public IBuffer getReadOnlyDataBuffer(INtfsContext context) {
        return new NonResidentDataBuffer(context, this);
    }

    public int write(byte[] buffer, int offset) {
        int headerLength = 0x40;
        if (!Collections.disjoint(getFlags(), EnumSet.of(AttributeFlags.Compressed, AttributeFlags.Sparse))) {
            headerLength += 0x08;
        }

        int nameLength = 0;
        int nameOffset = headerLength;
        if (getName() != null) {
            nameLength = getName().length();
        }

        int dataOffset = MathUtilities.roundUp(headerLength + nameLength * 2, 8);

        // Write out data first, since we know where it goes...
        int dataLen = 0;
        for (DataRun run : getDataRuns()) {
            dataLen += run.write(buffer, offset + dataOffset + dataLen);
        }

        buffer[offset + dataOffset + dataLen] = 0; // NULL terminator
        dataLen++;

        int length = MathUtilities.roundUp(dataOffset + dataLen, 8);

        EndianUtilities.writeBytesLittleEndian(_type.getValue(), buffer, offset + 0x00);
        EndianUtilities.writeBytesLittleEndian(length, buffer, offset + 0x04);
        buffer[offset + 0x08] = _nonResidentFlag;
        buffer[offset + 0x09] = (byte) nameLength;
        EndianUtilities.writeBytesLittleEndian((short) nameOffset, buffer, offset + 0x0A);
        EndianUtilities.writeBytesLittleEndian((short) AttributeFlags.valueOf(_flags), buffer, offset + 0x0C);
        EndianUtilities.writeBytesLittleEndian(_attributeId, buffer, offset + 0x0E);

        EndianUtilities.writeBytesLittleEndian(_startingVCN, buffer, offset + 0x10);
        EndianUtilities.writeBytesLittleEndian(_lastVCN, buffer, offset + 0x18);
        EndianUtilities.writeBytesLittleEndian((short) dataOffset, buffer, offset + 0x20);
        EndianUtilities.writeBytesLittleEndian(_compressionUnitSize, buffer, offset + 0x22);
        EndianUtilities.writeBytesLittleEndian(0, buffer, offset + 0x24); // Padding
        EndianUtilities.writeBytesLittleEndian(_dataAllocatedSize, buffer, offset + 0x28);
        EndianUtilities.writeBytesLittleEndian(_dataRealSize, buffer, offset + 0x30);
        EndianUtilities.writeBytesLittleEndian(_initializedDataSize, buffer, offset + 0x38);
        if (!Collections.disjoint(getFlags(), EnumSet.of(AttributeFlags.Compressed, AttributeFlags.Sparse))) {
            EndianUtilities.writeBytesLittleEndian(_compressedSize, buffer, offset + 0x40);
        }

        if (getName() != null) {
            System.arraycopy(getName().getBytes(Charset.forName("UTF-16LE")), 0, buffer, offset + nameOffset, nameLength * 2);
        }

        return length;
    }

    public AttributeRecord split(int suggestedSplitIdx) {
        int splitIdx;
        if (suggestedSplitIdx <= 0 || suggestedSplitIdx >= _dataRuns.size()) {
            splitIdx = _dataRuns.size() / 2;
        } else {
            splitIdx = suggestedSplitIdx;
        }

        long splitVcn = _startingVCN;
        long splitLcn = 0;
        for (int i = 0; i < splitIdx; ++i) {
            splitVcn += getDataRuns().get(i).getRunLength();
            splitLcn += getDataRuns().get(i).getRunOffset();
        }

        List<DataRun> newRecordRuns = new ArrayList<>();
        while (getDataRuns().size() > splitIdx) {
            DataRun run = getDataRuns().get(splitIdx);

            getDataRuns().remove(splitIdx);
            newRecordRuns.add(run);
        }

        // Each extent has implicit start LCN=0, so have to make stored runs match reality.
        // However, take care not to stomp on 'sparse' runs that may be at the start of the
        // new extent (indicated by Zero run offset).
        for (int i = 0; i < newRecordRuns.size(); ++i) {
            if (!newRecordRuns.get(i).isSparse()) {
                newRecordRuns.get(i).setRunOffset(newRecordRuns.get(i).getRunOffset() + splitLcn);;
                break;
            }
        }

        _lastVCN = splitVcn - 1;

        return new NonResidentAttributeRecord(_type, _name, (short) 0, _flags, splitVcn, newRecordRuns);
    }

    public void dump(PrintWriter writer, String indent) {
        super.dump(writer, indent);
        writer.println(indent + "     Starting VCN: " + _startingVCN);
        writer.println(indent + "         Last VCN: " + _lastVCN);
        writer.println(indent + "   Comp Unit Size: " + _compressionUnitSize);
        writer.println(indent + "   Allocated Size: " + _dataAllocatedSize);
        writer.println(indent + "        Real Size: " + _dataRealSize);
        writer.println(indent + "   Init Data Size: " + _initializedDataSize);
        if (!Collections.disjoint(getFlags(), EnumSet.of(AttributeFlags.Compressed, AttributeFlags.Sparse))) {
            writer.println(indent + "  Compressed Size: " + _compressedSize);
        }

        String runStr = "";

        for (DataRun run : _dataRuns) {
            runStr += " " + run;
        }

        writer.println(indent + "        Data Runs:" + runStr);
    }

    protected void read(byte[] buffer, int offset, int[] length) {
        _dataRuns = null;

        super.read(buffer, offset, length);

        _startingVCN = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x10);
        _lastVCN = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x18);
        _dataRunsOffset = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x20);
        _compressionUnitSize = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x22);
        _dataAllocatedSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x28);
        _dataRealSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x30);
        _initializedDataSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x38);
        if (!Collections.disjoint(_flags, EnumSet.of(AttributeFlags.Compressed, AttributeFlags.Sparse)) &&
            (_dataRunsOffset & 0xffff) > 0x40) {
            _compressedSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x40);
        }

        _dataRuns = new ArrayList<>();
        int pos = _dataRunsOffset & 0xffff;
        while (pos < length[0]) {
            DataRun run = new DataRun();
            int len = run.read(buffer, offset + pos);

            // Length 1 means there was only a header byte (i.e. terminator)
            if (len == 1) {
                break;
            }

            _dataRuns.add(run);
            pos += len;
        }
    }
}
