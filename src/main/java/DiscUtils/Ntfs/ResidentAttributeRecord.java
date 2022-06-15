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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import DiscUtils.Streams.SparseMemoryBuffer;
import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Range;


public final class ResidentAttributeRecord extends AttributeRecord {
    private byte _indexedFlag;

    private SparseMemoryBuffer _memoryBuffer;

    public ResidentAttributeRecord(byte[] buffer, int offset, int[] length) {
        read(buffer, offset, length);
    }

    public ResidentAttributeRecord(AttributeType type, String name, short id, boolean indexed, EnumSet<AttributeFlags> flags) {
        super(type, name, id, flags);
        _nonResidentFlag = 0;
        _indexedFlag = (byte) (indexed ? 1 : 0);
        _memoryBuffer = new SparseMemoryBuffer(1024);
    }

    public long getAllocatedLength() {
        return MathUtilities.roundUp(getDataLength(), 8);
    }

    public void setAllocatedLength(long value) {
        throw new UnsupportedOperationException();
    }

    public IBuffer getDataBuffer() {
        return _memoryBuffer;
    }

    public long getDataLength() {
        return _memoryBuffer.getCapacity();
    }

    public void setDataLength(long value) {
        throw new UnsupportedOperationException();
    }

    public int getDataOffset() {
        byte nameLength = 0;
        if (getName() != null) {
            nameLength = (byte) getName().length();
        }

        return MathUtilities.roundUp(0x18 + nameLength * 2, 8);
    }

    /**
     * The amount of initialized data in the attribute (in bytes).
     */
    public long getInitializedDataLength() {
        return getDataLength();
    }

    public void setInitializedDataLength(long value) {
        throw new UnsupportedOperationException();
    }

    public int getSize() {
        byte nameLength = 0;
        short nameOffset = 0x18;
        if (getName() != null) {
            nameLength = (byte) getName().length();
        }

        short dataOffset = (short) MathUtilities.roundUp(nameOffset + nameLength * 2, 8);
        return (int) MathUtilities.roundUp(dataOffset + _memoryBuffer.getCapacity(), 8);
    }

    public long getStartVcn() {
        return 0;
    }

    public IBuffer getReadOnlyDataBuffer(INtfsContext context) {
        return _memoryBuffer;
    }

    public List<Range> getClusters() {
        return Collections.emptyList();
    }

    public int write(byte[] buffer, int offset) {
        byte nameLength = 0;
        short nameOffset = 0;
        if (getName() != null) {
            nameOffset = 0x18;
            nameLength = (byte) getName().length();
        }

        short dataOffset = (short) MathUtilities.roundUp(0x18 + nameLength * 2, 8);
        int length = (int) MathUtilities.roundUp(dataOffset + _memoryBuffer.getCapacity(), 8);

        EndianUtilities.writeBytesLittleEndian(_type.getValue(), buffer, offset + 0x00);
        EndianUtilities.writeBytesLittleEndian(length, buffer, offset + 0x04);
        buffer[offset + 0x08] = _nonResidentFlag;
        buffer[offset + 0x09] = nameLength;
        EndianUtilities.writeBytesLittleEndian(nameOffset, buffer, offset + 0x0A);
        EndianUtilities.writeBytesLittleEndian((short) AttributeFlags.valueOf(_flags), buffer, offset + 0x0C);
        EndianUtilities.writeBytesLittleEndian(_attributeId, buffer, offset + 0x0E);
        EndianUtilities.writeBytesLittleEndian((int) _memoryBuffer.getCapacity(), buffer, offset + 0x10);
        EndianUtilities.writeBytesLittleEndian(dataOffset, buffer, offset + 0x14);
        buffer[offset + 0x16] = _indexedFlag;
        buffer[offset + 0x17] = 0; // Padding

        if (getName() != null) {
            byte[] bytes = getName().getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(bytes, 0, buffer, offset + nameOffset, bytes.length);
        }

        _memoryBuffer.read(0, buffer, offset + dataOffset, (int) _memoryBuffer.getCapacity());

        return length;
    }

    public void dump(PrintWriter writer, String indent) {
        super.dump(writer, indent);
        writer.println(indent + "     Data Length: " + getDataLength());
        writer.println(indent + "         Indexed: " + _indexedFlag);
    }

    protected void read(byte[] buffer, int offset, int[] length) {
        super.read(buffer, offset, length);

        int dataLength = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x10);
        short dataOffset = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x14);
        _indexedFlag = buffer[offset + 0x16];

        if (dataOffset + dataLength > length[0]) {
            throw new dotnet4j.io.IOException("Corrupt attribute, data outside of attribute");
        }

        _memoryBuffer = new SparseMemoryBuffer(1024);
        _memoryBuffer.write(0, buffer, offset + dataOffset, dataLength);
    }
}
