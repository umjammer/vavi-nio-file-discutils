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
        _status = data[offset];
        _startHead = data[offset + 1];
        _startSector = (byte) (data[offset + 2] & 0x3F);
        _startCylinder = (data[offset + 3] & 0xff) | ((data[offset + 2] & 0xC0) << 2);
        _partitionType = data[offset + 4];
        _endHead = data[offset + 5];
        _endSector = (byte) (data[offset + 6] & 0x3F);
        _endCylinder = (data[offset + 7] & 0xff)  | ((data[offset + 6] & 0xC0) << 2);
        _lbaStart = EndianUtilities.toUInt32LittleEndian(data, offset + 8);
        _lbaLength = EndianUtilities.toUInt32LittleEndian(data, offset + 12);
        _index = index;
    }

    private int _endCylinder;

    public int getEndCylinder() {
        return _endCylinder;
    }

    public void setEndCylinder(short value) {
        _endCylinder = value;
    }

    private byte _endHead;

    public int getEndHead() {
        return _endHead & 0xff;
    }

    public void setEndHead(byte value) {
        _endHead = value;
    }

    private byte _endSector;

    public int getEndSector() {
        return _endSector & 0xff;
    }

    public void setEndSector(byte value) {
        _endSector = value;
    }

    public String getFriendlyPartitionType() {
        return BiosPartitionTypes.toString(getPartitionType());
    }

    private int _index;

    public int getIndex() {
        return _index;
    }

    public boolean isValid() {
        return _endHead != 0 || _endSector != 0 || _endCylinder != 0 || _lbaLength != 0;
    }

    private int _lbaLength;

    public long getLBALength() {
        return _lbaLength & 0xffffffffL;
    }

    public void setLBALength(int value) {
        _lbaLength = value;
    }

    private int _lbaStart;

    public long getLBAStart() {
        return _lbaStart & 0xffffffffL;
    }

    public void setLBAStart(int value) {
        _lbaStart = value;
    }

    public long getLBAStartAbsolute() {
        return getLBAStart() + _lbaOffset;
    }

    private byte _partitionType;

    public byte getPartitionType() {
        return _partitionType;
    }

    public void setPartitionType(byte value) {
        _partitionType = value;
    }

    private int _startCylinder;

    public int getStartCylinder() {
        return _startCylinder;
    }

    public void setStartCylinder(short value) {
        _startCylinder = value;
    }

    private byte _startHead;

    public int getStartHead() {
        return _startHead & 0xff;
    }

    public void setStartHead(byte value) {
        _startHead = value;
    }

    private byte _startSector;

    public int getStartSector() {
        return _startSector & 0xff;
    }

    public void setStartSector(byte value) {
        _startSector = value;
    }

    private byte _status;

    public byte getStatus() {
        return _status;
    }

    public void setStatus(byte value) {
        _status = value;
    }

    public int compareTo(BiosPartitionRecord other) {
        return Long.compare(getLBAStartAbsolute(), other.getLBAStartAbsolute());
    }

    public void writeTo(byte[] buffer, int offset) {
        buffer[offset] = _status;
        buffer[offset + 1] = _startHead;
        buffer[offset + 2] = (byte) ((_startSector & 0x3F) | ((_startCylinder >>> 2) & 0xC0));
        buffer[offset + 3] = (byte) _startCylinder;
        buffer[offset + 4] = _partitionType;
        buffer[offset + 5] = _endHead;
        buffer[offset + 6] = (byte) ((_endSector & 0x3F) | ((_endCylinder >>> 2) & 0xC0));
        buffer[offset + 7] = (byte) _endCylinder;
        EndianUtilities.writeBytesLittleEndian(_lbaStart, buffer, offset + 8);
        EndianUtilities.writeBytesLittleEndian(_lbaLength, buffer, offset + 12);
    }
}
