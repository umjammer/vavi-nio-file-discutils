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

import discUtils.core.IDiagnosticTraceable;
import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.MathUtilities;
import dotnet4j.util.compat.StringUtilities;


public class AttributeListRecord implements IDiagnosticTraceable, IByteArraySerializable, Comparable<AttributeListRecord> {

    public short attributeId;

    public FileRecordReference baseFileReference;

    public String name;

    private byte nameLength;

    public int getNameLength() {
        return nameLength & 0xff;
    }

    private byte nameOffset;

    public int getNameOffset() {
        return nameOffset & 0xff;
    }

    public short recordLength;

    public long startVcn;

    public AttributeType type = AttributeType.None;

    public int size() {
        return MathUtilities.roundUp(0x20 + (name == null || name.isEmpty() ? 0 : name.getBytes(StandardCharsets.UTF_16LE).length), 8);
    }

    public int readFrom(byte[] data, int offset) {
        type = AttributeType.valueOf(EndianUtilities.toUInt32LittleEndian(data, offset + 0x00));
        recordLength = EndianUtilities.toUInt16LittleEndian(data, offset + 0x04);
        nameLength = data[offset + 0x06];
        nameOffset = data[offset + 0x07];
        startVcn = EndianUtilities.toUInt64LittleEndian(data, offset + 0x08);
        baseFileReference = new FileRecordReference(EndianUtilities.toUInt64LittleEndian(data, offset + 0x10));
        attributeId = EndianUtilities.toUInt16LittleEndian(data, offset + 0x18);
        if (getNameLength() > 0) {
            name = new String(data, offset + getNameOffset(), getNameLength() * 2, StandardCharsets.UTF_16LE);
        } else {
            name = null;
        }
        if (recordLength < 0x18) {
            throw new IllegalArgumentException("Malformed AttributeList record");
        }

        return recordLength;
    }

    public void writeTo(byte[] buffer, int offset) {
        nameOffset = 0x20;
        if (name == null || name.isEmpty()) {
            nameLength = 0;
        } else {
            byte[] bytes = name.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(bytes, 0, buffer, offset + getNameOffset(), bytes.length);
            nameLength = (byte) bytes.length;
        }
        recordLength = (short) MathUtilities.roundUp(getNameOffset() + getNameLength() * 2, 8);
        EndianUtilities.writeBytesLittleEndian(type.getValue(), buffer, offset);
        EndianUtilities.writeBytesLittleEndian(recordLength, buffer, offset + 0x04);
        buffer[offset + 0x06] = nameLength;
        buffer[offset + 0x07] = nameOffset;
        EndianUtilities.writeBytesLittleEndian(startVcn, buffer, offset + 0x08);
        EndianUtilities.writeBytesLittleEndian(baseFileReference.getValue(), buffer, offset + 0x10);
        EndianUtilities.writeBytesLittleEndian(attributeId, buffer, offset + 0x18);
    }

    public int compareTo(AttributeListRecord other) {
        int val = type.ordinal() - other.type.ordinal();
        if (val != 0) {
            return val;
        }

        val = StringUtilities.compare(name, other.name, true);
        if (val != 0) {
            return val;
        }

        return (int) startVcn - (int) other.startVcn;
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "ATTRIBUTE LIST RECORD");
        writer.println(indent + "                 Type: " + type);
        writer.println(indent + "        Record Length: " + recordLength);
        writer.println(indent + "                 Name: " + name);
        writer.println(indent + "            Start VCN: " + startVcn);
        writer.println(indent + "  base File Reference: " + baseFileReference);
        writer.println(indent + "         Attribute ID: " + attributeId);
    }

    public static AttributeListRecord fromAttribute(AttributeRecord attr, FileRecordReference mftRecord) {
        AttributeListRecord newRecord = new AttributeListRecord();
        newRecord.type = attr.getAttributeType();
        newRecord.name = attr.getName();
        newRecord.startVcn = 0;
        newRecord.baseFileReference = mftRecord;
        newRecord.attributeId = attr.getAttributeId();

        if (attr.isNonResident()) {
            newRecord.startVcn = attr.getStartVcn();
        }

        return newRecord;
    }
}
