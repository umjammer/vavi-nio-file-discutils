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

import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;


public class AttributeListRecord implements IDiagnosticTraceable, IByteArraySerializable, Comparable<AttributeListRecord> {
    public short AttributeId;

    public FileRecordReference BaseFileReference = new FileRecordReference();

    public String Name;

    public byte NameLength;

    public byte NameOffset;

    public short RecordLength;

    public long StartVcn;

    public AttributeType Type = AttributeType.None;

    public long getSize() {
        return MathUtilities.roundUp(0x20 + (Name == null || Name.isEmpty() ? 0 : Name.getBytes(Charset.forName("Unicode")).length), 8);
    }

    public int readFrom(byte[] data, int offset) {
        Type = AttributeType.valueOf(EndianUtilities.toUInt32LittleEndian(data, offset + 0x00));
        RecordLength = (short) EndianUtilities.toUInt16LittleEndian(data, offset + 0x04);
        NameLength = data[offset + 0x06];
        NameOffset = data[offset + 0x07];
        StartVcn = EndianUtilities.toUInt64LittleEndian(data, offset + 0x08);
        BaseFileReference = new FileRecordReference(EndianUtilities.toUInt64LittleEndian(data, offset + 0x10));
        AttributeId = (short) EndianUtilities.toUInt16LittleEndian(data, offset + 0x18);
        if (NameLength > 0) {
            Name = new String(data, offset + NameOffset, NameLength * 2, Charset.forName("Unicode"));
        } else {
            Name = null;
        }
        if (RecordLength < 0x18) {
            throw new IllegalArgumentException("Malformed AttributeList record");
        }

        return RecordLength;
    }

    public void writeTo(byte[] buffer, int offset) {
        NameOffset = 0x20;
        if (Name == null || Name.isEmpty()) {
            NameLength = 0;
        } else {
            byte[] bytes = Name.getBytes(Charset.forName("Unicode"));
            System.arraycopy(bytes, 0, buffer, offset + NameOffset, bytes.length);
            NameLength = (byte) bytes.length;
        }
        RecordLength = (short) MathUtilities.roundUp(NameOffset + NameLength * 2, 8);
        EndianUtilities.writeBytesLittleEndian(Type.ordinal(), buffer, offset);
        EndianUtilities.writeBytesLittleEndian(RecordLength, buffer, offset + 0x04);
        buffer[offset + 0x06] = NameLength;
        buffer[offset + 0x07] = NameOffset;
        EndianUtilities.writeBytesLittleEndian(StartVcn, buffer, offset + 0x08);
        EndianUtilities.writeBytesLittleEndian(BaseFileReference.getValue(), buffer, offset + 0x10);
        EndianUtilities.writeBytesLittleEndian(AttributeId, buffer, offset + 0x18);
    }

    public int compareTo(AttributeListRecord other) {
        int val = Type.ordinal() - other.Type.ordinal();
        if (val != 0) {
            return val;
        }

        val = Name.compareTo(other.Name);
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
        if (attr.getIsNonResident()) {
            newRecord.StartVcn = ((NonResidentAttributeRecord) attr).getStartVcn();
        }

        return newRecord;
    }

}