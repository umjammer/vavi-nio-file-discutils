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

import java.util.EnumSet;

import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;


public class IndexEntry {
    public static final int EndNodeSize = 0x18;

    protected byte[] _dataBuffer;

    protected EnumSet<IndexEntryFlags> _flags;

    protected byte[] _keyBuffer;

    // Only valid if Node flag set
    protected long _vcn;

    public IndexEntry(boolean isFileIndexEntry) {
        _isFileIndexEntry = isFileIndexEntry;
        _flags = EnumSet.noneOf(IndexEntryFlags.class);
    }

    public IndexEntry(IndexEntry toCopy, byte[] newKey, byte[] newData) {
        _isFileIndexEntry = toCopy.isFileIndexEntry();
        _flags = toCopy._flags;
        _vcn = toCopy._vcn;
        _keyBuffer = newKey;
        _dataBuffer = newData;
    }

    public IndexEntry(byte[] key, byte[] data, boolean isFileIndexEntry) {
        _isFileIndexEntry = isFileIndexEntry;
        _flags = EnumSet.noneOf(IndexEntryFlags.class);
        _keyBuffer = key;
        _dataBuffer = data;
    }

    public long getChildrenVirtualCluster() {
        return _vcn;
    }

    public void setChildrenVirtualCluster(long value) {
        _vcn = value;
    }

    public byte[] getDataBuffer() {
        return _dataBuffer;
    }

    public void setDataBuffer(byte[] value) {
        _dataBuffer = value;
    }

    public EnumSet<IndexEntryFlags> getFlags() {
        return _flags;
    }

    public void setFlags(EnumSet<IndexEntryFlags> value) {
        _flags = value;
    }

    private boolean _isFileIndexEntry;

    protected boolean isFileIndexEntry() {
        return _isFileIndexEntry;
    }

    public byte[] getKeyBuffer() {
        return _keyBuffer;
    }

    public void setKeyBuffer(byte[] value) {
        _keyBuffer = value;
    }

    public int getSize() {
        int size = 0x10; // start of variable data

        if (!_flags.contains(IndexEntryFlags.End)) {
            size += _keyBuffer.length;
            size += isFileIndexEntry() ? 0 : _dataBuffer.length;
        }

        size = MathUtilities.roundUp(size, 8);

        if (_flags.contains(IndexEntryFlags.Node)) {
            size += 8;
        }

        return size;
    }

    public void read(byte[] buffer, int offset) {
        @SuppressWarnings("unused")
        short dataOffset = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x00);
        short dataLength = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x02);
        short length = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x08);
        short keyLength = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x0A);
        assert dataLength >= 0 && length >=0 && keyLength >= 0;
        _flags = IndexEntryFlags.valueOf(EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x0C));

        if (!_flags.contains(IndexEntryFlags.End)) {
            _keyBuffer = new byte[keyLength];
            System.arraycopy(buffer, offset + 0x10, _keyBuffer, 0, keyLength);

            if (isFileIndexEntry()) {
                // Special case, for file indexes, the MFT ref is held where the data offset & length go
                _dataBuffer = new byte[8];
                System.arraycopy(buffer, offset + 0x00, _dataBuffer, 0, 8);
            } else {
                _dataBuffer = new byte[dataLength];
                System.arraycopy(buffer, offset + 0x10 + keyLength, _dataBuffer, 0, dataLength);
            }
        }

        if (_flags.contains(IndexEntryFlags.Node)) {
            _vcn = EndianUtilities.toInt64LittleEndian(buffer, offset + length - 8);
        }
    }

    public void writeTo(byte[] buffer, int offset) {
        assert buffer.length > offset : buffer.length + ", " + offset;

        short length = (short) getSize();

        if (!_flags.contains(IndexEntryFlags.End)) {
            short keyLength = (short) _keyBuffer.length;

            if (isFileIndexEntry()) {
                System.arraycopy(_dataBuffer, 0, buffer, offset + 0x00, 8);
            } else {
                short dataOffset = (short) (isFileIndexEntry() ? 0 : 0x10 + keyLength);
                short dataLength = (short) _dataBuffer.length;

                EndianUtilities.writeBytesLittleEndian(dataOffset, buffer, offset + 0x00);
                EndianUtilities.writeBytesLittleEndian(dataLength, buffer, offset + 0x02);
//Debug.println(_dataBuffer.length + ", " + buffer.length + ", " + offset + ", " + dataOffset);
                System.arraycopy(_dataBuffer, 0, buffer, offset + dataOffset, _dataBuffer.length);
            }
            EndianUtilities.writeBytesLittleEndian(keyLength, buffer, offset + 0x0A);
            System.arraycopy(_keyBuffer, 0, buffer, offset + 0x10, _keyBuffer.length);
        } else {
            EndianUtilities.writeBytesLittleEndian((short) 0, buffer, offset + 0x00); // dataOffset
            EndianUtilities.writeBytesLittleEndian((short) 0, buffer, offset + 0x02); // dataLength
            EndianUtilities.writeBytesLittleEndian((short) 0, buffer, offset + 0x0A); // keyLength
        }

        EndianUtilities.writeBytesLittleEndian(length, buffer, offset + 0x08);
        EndianUtilities.writeBytesLittleEndian((short) IndexEntryFlags.valueOf(_flags), buffer, offset + 0x0C);
        if (_flags.contains(IndexEntryFlags.Node)) {
            EndianUtilities.writeBytesLittleEndian(_vcn, buffer, offset + length - 8);
        }
    }
}
