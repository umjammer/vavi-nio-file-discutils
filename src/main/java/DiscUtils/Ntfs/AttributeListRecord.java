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

import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import dotnet4j.io.compat.StringUtilities;


public class AttributeListRecord implements IDiagnosticTraceable, IByteArraySerializable, Comparable<AttributeListRecord> {
    public short AttributeId;

    public FileRecordReference BaseFileReference;

    public String Name;

    private byte nameLength;

    public int getNameLength() {
        return nameLength & 0xff;
    }

    private byte nameOffset;

    public int getNameOffset() {
        return nameOffset & 0xff;
    }

    public short RecordLength;

    public long StartVcn;

    public AttributeType Type = AttributeType.None;

    public int size() {
        return MathUtilities.roundUp(0x20 + (Name == null || Name.isEmpty() ? 0 : Name.getBytes(StandardCharsets.UTF_16LE).length), 8);
    }

    public int readFrom(byte[] data, int offset) {
        Type = AttributeType.valueOf(EndianUtilities.toUInt32LittleEndian(data, offset + 0x00));
        RecordLength = EndianUtilities.toUInt16LittleEndian(data, offset + 0x04);
        nameLength = data[offset + 0x06];
        nameOffset = data[offset + 0x07];
        StartVcn = EndianUtilities.toUInt64LittleEndian(data, offset + 0x08);
        BaseFileReference = new FileRecordReference(EndianUtilities.toUInt64LittleEndian(data, offset + 0x10));
        AttributeId = EndianUtilities.toUInt16LittleEndian(data, offset + 0x18);
        if (getNameLength() > 0) {
            Name = new String(data, offset + getNameOffset(), getNameLength() * 2, StandardCharsets.UTF_16LE);
        } else {
            Name = null;
        }
        if (RecordLength < 0x18) {
            throw new IllegalArgumentException("Malformed AttributeList record");
        }

        return RecordLength;
    }

    public void writeTo(byte[] buffer, int offset) {
        nameOffset = 0x20;
        if (Name == null || Name.isEmpty()) {
            nameLength = 0;
        } else {
            byte[] bytes = Name.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(bytes, 0, buffer, offset + getNameOffset(), bytes.length);
            nameLength = (byte) bytes.length;
        }
        RecordLength = (short) MathUtilities.roundUp(getNameOffset() + getNameLength() * 2, 8);
        EndianUtilities.writeBytesLittleEndian(Type.getValue(), buffer, offset);
        EndianUtilities.writeBytesLittleEndian(RecordLength, buffer, offset + 0x04);
        buffer[offset + 0x06] = nameLength;
        buffer[offset + 0x07] = nameOffset;
        EndianUtilities.writeBytesLittleEndian(StartVcn, buffer, offset + 0x08);
        EndianUtilities.writeBytesLittleEndian(BaseFileReference.getValue(), buffer, offset + 0x10);
        EndianUtilities.writeBytesLittleEndian(AttributeId, buffer, offset + 0x18);
    }

    public int compareTo(AttributeListRecord other) {
        int val = Type.ordinal() - other.Type.ordinal();
        if (val != 0) {
            return val;
        }

        val = StringUtilities.compare(Name, other.Name, true);
        if (val != 0) {
            return val;
        }

        return (int) StartVcn - (int) other.StartVcn;
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "ATTRIBUTE LIST RECORD");
        writer.println(indent + "                 Type: " + Type);
        writer.println(indent + "        Record Length: " + RecordLength);
        writer.println(indent + "                 Name: " + Name);
        writer.println(indent + "            Start VCN: " + StartVcn);
        writer.println(indent + "  Base File Reference: " + BaseFileReference);
        writer.println(indent + "         Attribute ID: " + AttributeId);
    }

    public static AttributeListRecord fromAttribute(AttributeRecord attr, FileRecordReference mftRecord) {
        AttributeListRecord newRecord = new AttributeListRecord();
        newRecord.Type = attr.getAttributeType();
        newRecord.Name = attr.getName();
        newRecord.StartVcn = 0;
        newRecord.BaseFileReference = mftRecord;
        newRecord.AttributeId = attr.getAttributeId();

        if (attr.isNonResident()) {
            newRecord.StartVcn = attr.getStartVcn();
        }

        return newRecord;
    }
}
