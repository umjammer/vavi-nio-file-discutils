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

package DiscUtils.Core.Partitions;

import DiscUtils.Streams.Util.EndianUtilities;


public class BiosPartitionRecord implements Comparable<BiosPartitionRecord> {
    private int _lbaOffset;

    public BiosPartitionRecord() {
    }

    public BiosPartitionRecord(byte[] data, int offset, int lbaOffset, int index) {
        _lbaOffset = lbaOffset;
        setStatus(data[offset]);
        setStartHead(data[offset + 1]);
        setStartSector((byte) (data[offset + 2] & 0x3F));
        setStartCylinder((short) (data[offset + 3] | ((data[offset + 2] & 0xC0) << 2)));
        setPartitionType(data[offset + 4]);
        setEndHead(data[offset + 5]);
        setEndSector((byte) (data[offset + 6] & 0x3F));
        setEndCylinder((short) (data[offset + 7] | ((data[offset + 6] & 0xC0) << 2)));
        setLBAStart(EndianUtilities.toUInt32LittleEndian(data, offset + 8));
        setLBALength(EndianUtilities.toUInt32LittleEndian(data, offset + 12));
        __Index = index;
    }

    private short __EndCylinder;

    public short getEndCylinder() {
        return __EndCylinder;
    }

    public void setEndCylinder(short value) {
        __EndCylinder = value;
    }

    private byte __EndHead;

    public byte getEndHead() {
        return __EndHead;
    }

    public void setEndHead(byte value) {
        __EndHead = value;
    }

    private byte __EndSector;

    public byte getEndSector() {
        return __EndSector;
    }

    public void setEndSector(byte value) {
        __EndSector = value;
    }

    public String getFriendlyPartitionType() {
        return BiosPartitionTypes.toString(getPartitionType());
    }

    private int __Index;

    public int getIndex() {
        return __Index;
    }

    public boolean isValid() {
        return getEndHead() != 0 || getEndSector() != 0 || getEndCylinder() != 0 || getLBALength() != 0;
    }

    private int __LBALength;

    public int getLBALength() {
        return __LBALength;
    }

    public void setLBALength(int value) {
        __LBALength = value;
    }

    private int __LBAStart;

    public int getLBAStart() {
        return __LBAStart;
    }

    public void setLBAStart(int value) {
        __LBAStart = value;
    }

    public int getLBAStartAbsolute() {
        return getLBAStart() + _lbaOffset;
    }

    private byte __PartitionType;

    public byte getPartitionType() {
        return __PartitionType;
    }

    public void setPartitionType(byte value) {
        __PartitionType = value;
    }

    private short __StartCylinder;

    public short getStartCylinder() {
        return __StartCylinder;
    }

    public void setStartCylinder(short value) {
        __StartCylinder = value;
    }

    private byte __StartHead;

    public byte getStartHead() {
        return __StartHead;
    }

    public void setStartHead(byte value) {
        __StartHead = value;
    }

    private byte __StartSector;

    public byte getStartSector() {
        return __StartSector;
    }

    public void setStartSector(byte value) {
        __StartSector = value;
    }

    private byte __Status;

    public byte getStatus() {
        return __Status;
    }

    public void setStatus(byte value) {
        __Status = value;
    }

    public int compareTo(BiosPartitionRecord other) {
        return getLBAStartAbsolute() - other.getLBAStartAbsolute();
    }

    public void writeTo(byte[] buffer, int offset) {
        buffer[offset] = getStatus();
        buffer[offset + 1] = getStartHead();
        buffer[offset + 2] = (byte) ((getStartSector() & 0x3F) | ((getStartCylinder() >> 2) & 0xC0));
        buffer[offset + 3] = (byte) getStartCylinder();
        buffer[offset + 4] = getPartitionType();
        buffer[offset + 5] = getEndHead();
        buffer[offset + 6] = (byte) ((getEndSector() & 0x3F) | ((getEndCylinder() >> 2) & 0xC0));
        buffer[offset + 7] = (byte) getEndCylinder();
        EndianUtilities.writeBytesLittleEndian(getLBAStart(), buffer, offset + 8);
        EndianUtilities.writeBytesLittleEndian(getLBALength(), buffer, offset + 12);
    }

}
