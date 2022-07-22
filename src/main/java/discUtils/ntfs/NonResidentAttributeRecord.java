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

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;

import vavi.util.StringUtil;

import discUtils.streams.buffer.IBuffer;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Range;


final class NonResidentAttributeRecord extends AttributeRecord {

    private static final short DefaultCompressionUnitSize = 4;

    private long compressedSize;

    private short compressionUnitSize;

    private long dataAllocatedSize;

    private long dataRealSize;

    private short dataRunsOffset;

    private long initializedDataSize;

    private long lastVCN;

    private long startingVCN;

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

        nonResidentFlag = 1;
        dataRuns = new ArrayList<>();
        dataRuns.add(new DataRun(firstCluster, numClusters, false));
        lastVCN = numClusters - 1;
        dataAllocatedSize = bytesPerCluster * numClusters;
        dataRealSize = bytesPerCluster * numClusters;
        initializedDataSize = bytesPerCluster * numClusters;

        if (!Collections.disjoint(flags, EnumSet.of(AttributeFlags.Compressed, AttributeFlags.Sparse))) {
            compressionUnitSize = DefaultCompressionUnitSize;
        }
    }

    public NonResidentAttributeRecord(AttributeType type,
            String name,
            short id,
            EnumSet<AttributeFlags> flags,
            long startVcn,
            List<DataRun> dataRuns) {
        super(type, name, id, flags);

        nonResidentFlag = 1;
        this.dataRuns = dataRuns;
        startingVCN = startVcn;

        if (!Collections.disjoint(flags, EnumSet.of(AttributeFlags.Compressed, AttributeFlags.Sparse))) {
            compressionUnitSize = DefaultCompressionUnitSize;
        }

        if (dataRuns != null && dataRuns.size() != 0) {
            lastVCN = startingVCN;
            for (DataRun run : dataRuns) {
                lastVCN += run.getRunLength();
            }

            lastVCN -= 1;
        }
    }

    /**
     * The amount of space occupied by the attribute (in bytes).
     */
    public long getAllocatedLength() {
        return dataAllocatedSize;
    }

    public void setAllocatedLength(long value) {
        dataAllocatedSize = value;
    }

    public long getCompressedDataSize() {
        return compressedSize;
    }

    public void setCompressedDataSize(long value) {
        compressedSize = value;
    }

    /**
     * Gets or sets the size of a compression unit (in clusters).
     */
    public int getCompressionUnitSize() {
        return 1 << (compressionUnitSize & 0xffff);
    }

    public void setCompressionUnitSize(int value) {
        compressionUnitSize = (short) MathUtilities.log2(value);
    }

    /**
     * The amount of data in the attribute (in bytes).
     */
    public long getDataLength() {
        return dataRealSize;
    }

    public void setDataLength(long value) {
        dataRealSize = value;
    }

    private List<DataRun> dataRuns;

    public List<DataRun> getDataRuns() {
        return dataRuns;
    }

    /**
     * The amount of initialized data in the attribute (in bytes).
     */
    public long getInitializedDataLength() {
        return initializedDataSize;
    }

    public void setInitializedDataLength(long value) {
        initializedDataSize = value;
    }

    public long getLastVcn() {
        return lastVCN;
    }

    public void setLastVcn(long value) {
        lastVCN = value;
    }

    public int getSize() {
        int nameLength = 0;
        int nameOffset = !Collections.disjoint(flags, EnumSet.of(AttributeFlags.Compressed, AttributeFlags.Sparse)) ? 0x48 : 0x40;
        if (name != null) {
            nameLength = name.length();
        }

        int dataOffset = MathUtilities.roundUp(nameOffset + nameLength * 2, 8);

        // Write out data first, since we know where it goes...
        int dataLen = 0;
        for (DataRun run : dataRuns) {
            dataLen += run.getSize();
        }

        dataLen++; // NULL terminator

        return MathUtilities.roundUp(dataOffset + dataLen, 8);
    }

    public long getStartVcn() {
        return startingVCN;
    }

    public void replaceRun(DataRun oldRun, DataRun newRun) {
        int idx = dataRuns.indexOf(oldRun);
        if (idx < 0) {
            throw new NoSuchElementException("Attempt to replace non-existant run: " + oldRun);
        }

        dataRuns.set(idx, newRun);
//Debug.println("~~[" + idx + "]: " + newRun + " / " + StringUtil.paramString(getDataRuns()));
    }

    public int removeRun(DataRun run) {
        int idx = dataRuns.indexOf(run);
        if (idx < 0) {
//Debug.println("-x: " + run + " / " + StringUtil.paramString(getDataRuns()));
            throw new NoSuchElementException("Attempt to remove non-existant run: " + run);
        }

        dataRuns.remove(idx);
//Debug.println("--[" + idx + "]: " + run + " / " + StringUtil.paramString(getDataRuns()));
        return idx;
    }

    public void insertRun(DataRun existingRun, DataRun newRun) {
        int idx = dataRuns.indexOf(existingRun);
        if (idx < 0) {
            throw new NoSuchElementException("Attempt to replace non-existant run: " + existingRun);
        }

        dataRuns.add(idx + 1, newRun);
//Debug.println("+2[" + (idx + 1) + "]: " + newRun + " / " + StringUtil.paramString(getDataRuns()));
    }

    public void insertRun(int index, DataRun newRun) {
        dataRuns.add(index, newRun);
//Debug.println("+1[" + index + "]: " + newRun + " / " + getDataRuns());
    }

    public List<Range> getClusters() {
        List<DataRun> cookedRuns = dataRuns;

        long start = 0;
        List<Range> result = new ArrayList<>(dataRuns.size());
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

        EndianUtilities.writeBytesLittleEndian(type.getValue(), buffer, offset + 0x00);
        EndianUtilities.writeBytesLittleEndian(length, buffer, offset + 0x04);
        buffer[offset + 0x08] = nonResidentFlag;
        buffer[offset + 0x09] = (byte) nameLength;
        EndianUtilities.writeBytesLittleEndian((short) nameOffset, buffer, offset + 0x0A);
        EndianUtilities.writeBytesLittleEndian((short) AttributeFlags.valueOf(flags), buffer, offset + 0x0C);
        EndianUtilities.writeBytesLittleEndian(attributeId, buffer, offset + 0x0E);

        EndianUtilities.writeBytesLittleEndian(startingVCN, buffer, offset + 0x10);
        EndianUtilities.writeBytesLittleEndian(lastVCN, buffer, offset + 0x18);
        EndianUtilities.writeBytesLittleEndian((short) dataOffset, buffer, offset + 0x20);
        EndianUtilities.writeBytesLittleEndian(compressionUnitSize, buffer, offset + 0x22);
        EndianUtilities.writeBytesLittleEndian(0, buffer, offset + 0x24); // Padding
        EndianUtilities.writeBytesLittleEndian(dataAllocatedSize, buffer, offset + 0x28);
        EndianUtilities.writeBytesLittleEndian(dataRealSize, buffer, offset + 0x30);
        EndianUtilities.writeBytesLittleEndian(initializedDataSize, buffer, offset + 0x38);
        if (!Collections.disjoint(getFlags(), EnumSet.of(AttributeFlags.Compressed, AttributeFlags.Sparse))) {
            EndianUtilities.writeBytesLittleEndian(compressedSize, buffer, offset + 0x40);
        }

        if (getName() != null) {
            System.arraycopy(getName().getBytes(StandardCharsets.UTF_16LE), 0, buffer, offset + nameOffset, nameLength * 2);
        }

        return length;
    }

    public AttributeRecord split(int suggestedSplitIdx) {
        int splitIdx;
        if (suggestedSplitIdx <= 0 || suggestedSplitIdx >= dataRuns.size()) {
            splitIdx = dataRuns.size() / 2;
        } else {
            splitIdx = suggestedSplitIdx;
        }

        long splitVcn = startingVCN;
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
        for (DataRun newRecordRun : newRecordRuns) {
            if (!newRecordRun.isSparse()) {
                newRecordRun.setRunOffset(newRecordRun.getRunOffset() + splitLcn);
                break;
            }
        }

        lastVCN = splitVcn - 1;

        return new NonResidentAttributeRecord(type, name, (short) 0, flags, splitVcn, newRecordRuns);
    }

    public void dump(PrintWriter writer, String indent) {
        super.dump(writer, indent);
        writer.println(indent + "     Starting VCN: " + startingVCN);
        writer.println(indent + "         Last VCN: " + lastVCN);
        writer.println(indent + "   Comp Unit Size: " + compressionUnitSize);
        writer.println(indent + "   Allocated Size: " + dataAllocatedSize);
        writer.println(indent + "        Real Size: " + dataRealSize);
        writer.println(indent + "   Init Data Size: " + initializedDataSize);
        if (!Collections.disjoint(getFlags(), EnumSet.of(AttributeFlags.Compressed, AttributeFlags.Sparse))) {
            writer.println(indent + "  Compressed Size: " + compressedSize);
        }

        StringBuilder runStr = new StringBuilder();

        for (DataRun run : dataRuns) {
            runStr.append(" ").append(run);
        }

        writer.println(indent + "        Data Runs:" + runStr);
    }

    protected void read(byte[] buffer, int offset, int[] length) {
        dataRuns = null;

        super.read(buffer, offset, length);

        startingVCN = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x10);
        lastVCN = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x18);
        dataRunsOffset = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x20);
        compressionUnitSize = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x22);
        dataAllocatedSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x28);
        dataRealSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x30);
        initializedDataSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x38);
        if (!Collections.disjoint(flags, EnumSet.of(AttributeFlags.Compressed, AttributeFlags.Sparse)) &&
            (dataRunsOffset & 0xffff) > 0x40) {
            compressedSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x40);
        }

        dataRuns = new ArrayList<>();
        int pos = dataRunsOffset & 0xffff;
        while (pos < length[0]) {
            DataRun run = new DataRun();
            int len = run.read(buffer, offset + pos);

            // Length 1 means there was only a header byte (i.e. terminator)
            if (len == 1) {
                break;
            }

            dataRuns.add(run);
            pos += len;
        }
    }

    public String toString() {
        return StringUtil.paramString(this);
    }
}
