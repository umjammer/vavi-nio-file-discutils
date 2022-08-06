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

package discUtils.core.partitions;

import discUtils.streams.util.EndianUtilities;


public class BiosPartitionRecord implements Comparable<BiosPartitionRecord> {

    private int lbaOffset;

    public BiosPartitionRecord() {
    }

    public BiosPartitionRecord(byte[] data, int offset, int lbaOffset, int index) {
        this.lbaOffset = lbaOffset;
        status = data[offset];
        startHead = data[offset + 1];
        startSector = (byte) (data[offset + 2] & 0x3F);
        startCylinder = (data[offset + 3] & 0xff) | ((data[offset + 2] & 0xC0) << 2);
        partitionType = data[offset + 4];
        endHead = data[offset + 5];
        endSector = (byte) (data[offset + 6] & 0x3F);
        endCylinder = (data[offset + 7] & 0xff)  | ((data[offset + 6] & 0xC0) << 2);
        lbaStart = EndianUtilities.toUInt32LittleEndian(data, offset + 8);
        lbaLength = EndianUtilities.toUInt32LittleEndian(data, offset + 12);
        this.index = index;
    }

    private int endCylinder;

    public int getEndCylinder() {
        return endCylinder;
    }

    public void setEndCylinder(short value) {
        endCylinder = value;
    }

    private byte endHead;

    public int getEndHead() {
        return endHead & 0xff;
    }

    public void setEndHead(byte value) {
        endHead = value;
    }

    private byte endSector;

    public int getEndSector() {
        return endSector & 0xff;
    }

    public void setEndSector(byte value) {
        endSector = value;
    }

    public String getFriendlyPartitionType() {
        return BiosPartitionTypes.toString(getPartitionType());
    }

    private int index;

    public int getIndex() {
        return index;
    }

    public boolean isValid() {
        return partitionType != 0 && (endHead != 0 || endSector != 0 || endCylinder != 0 || lbaLength != 0);
    }

    private int lbaLength;

    public long getLBALength() {
        return lbaLength & 0xffffffffL;
    }

    public void setLBALength(int value) {
        lbaLength = value;
    }

    private int lbaStart;

    public long getLBAStart() {
        return lbaStart & 0xffffffffL;
    }

    public void setLBAStart(int value) {
        lbaStart = value;
    }

    public long getLBAStartAbsolute() {
        return getLBAStart() + lbaOffset;
    }

    private byte partitionType;

    public byte getPartitionType() {
        return partitionType;
    }

    public void setPartitionType(byte value) {
        partitionType = value;
    }

    private int startCylinder;

    public int getStartCylinder() {
        return startCylinder;
    }

    public void setStartCylinder(short value) {
        startCylinder = value;
    }

    private byte startHead;

    public int getStartHead() {
        return startHead & 0xff;
    }

    public void setStartHead(byte value) {
        startHead = value;
    }

    private byte startSector;

    public int getStartSector() {
        return startSector & 0xff;
    }

    public void setStartSector(byte value) {
        startSector = value;
    }

    private byte status;

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte value) {
        status = value;
    }

    public int compareTo(BiosPartitionRecord other) {
        return Long.compare(getLBAStartAbsolute(), other.getLBAStartAbsolute());
    }

    public void writeTo(byte[] buffer, int offset) {
        buffer[offset] = status;
        buffer[offset + 1] = startHead;
        buffer[offset + 2] = (byte) ((startSector & 0x3F) | ((startCylinder >>> 2) & 0xC0));
        buffer[offset + 3] = (byte) startCylinder;
        buffer[offset + 4] = partitionType;
        buffer[offset + 5] = endHead;
        buffer[offset + 6] = (byte) ((endSector & 0x3F) | ((endCylinder >>> 2) & 0xC0));
        buffer[offset + 7] = (byte) endCylinder;
        EndianUtilities.writeBytesLittleEndian(lbaStart, buffer, offset + 8);
        EndianUtilities.writeBytesLittleEndian(lbaLength, buffer, offset + 12);
    }

    @Override
    public String toString() {
        return "BiosPartitionRecord{" +
                "lbaOffset=" + lbaOffset +
                ", endCylinder=" + endCylinder +
                ", endHead=" + endHead +
                ", endSector=" + endSector +
                ", index=" + index +
                ", lbaLength=" + lbaLength +
                ", lbaStart=" + lbaStart +
                ", partitionType=" + partitionType +
                ", startCylinder=" + startCylinder +
                ", startHead=" + startHead +
                ", startSector=" + startSector +
                ", status=" + status +
                '}';
    }
}
