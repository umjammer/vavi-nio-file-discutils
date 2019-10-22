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
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Range;
import moe.yo3explorer.dotnetio4j.IOException;


public abstract class AttributeRecord implements Comparable<AttributeRecord> {
    protected short _attributeId;

    protected EnumSet<AttributeFlags> _flags;

    protected String _name;

    protected byte _nonResidentFlag;

    protected AttributeType _type;

    public AttributeRecord() {
    }

    public AttributeRecord(AttributeType type, String name, short id, EnumSet<AttributeFlags> flags) {
        _type = type;
        _name = name;
        _attributeId = id;
        _flags = flags;
    }

    public abstract long getAllocatedLength();

    public abstract void setAllocatedLength(long value);

    public short getAttributeId() {
        return _attributeId;
    }

    public void setAttributeId(short value) {
        _attributeId = value;
    }

    public AttributeType getAttributeType() {
        return _type;
    }

    public abstract long getDataLength();

    public abstract void setDataLength(long value);

    public EnumSet<AttributeFlags> getFlags() {
        return _flags;
    }

    public void setFlags(EnumSet<AttributeFlags> value) {
        _flags = value;
    }

    public abstract long getInitializedDataLength();

    public abstract void setInitializedDataLength(long value);

    public boolean isNonResident() {
        return _nonResidentFlag != 0;
    }

    public String getName() {
        return _name;
    }

    public abstract long getSize();

    public abstract long getStartVcn();

    public int compareTo(AttributeRecord other) {
        int val = Enum.class.cast(_type).ordinal() - Enum.class.cast(other._type).ordinal();
        if (val != 0) {
            return val;
        }

        val = Utilities.compareTo(_name, other._name, true);
        if (val != 0) {
            return val;
        }

        return _attributeId - other._attributeId;
    }

    public static AttributeRecord fromBytes(byte[] buffer, int offset, int[] length) {
        if (EndianUtilities.toUInt32LittleEndian(buffer, offset) == 0xFFFFFFFF) {
            length[0] = 0;
            return null;
        }

        if (buffer[offset + 0x08] != 0x00) {
            return new NonResidentAttributeRecord(buffer, offset, length);
        }

        ResidentAttributeRecord result = new ResidentAttributeRecord(buffer, offset, length);
        return result;
    }

    public static Comparator<AttributeRecord> compareStartVcns = new Comparator<AttributeRecord>() {
        @Override
        public int compare(AttributeRecord x, AttributeRecord y) {
            if (x.getStartVcn() < y.getStartVcn()) {
                return -1;
            }

            if (x.getStartVcn() == y.getStartVcn()) {
                return 0;
            }

            return 1;
        }
    };

    public abstract List<Range> getClusters();

    public abstract IBuffer getReadOnlyDataBuffer(INtfsContext context);

    public abstract int write(byte[] buffer, int offset);

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "ATTRIBUTE RECORD");
        writer.println(indent + "            Type: " + _type);
        writer.println(indent + "    Non-Resident: " + _nonResidentFlag);
        writer.println(indent + "            Name: " + _name);
        writer.println(indent + "           Flags: " + _flags);
        writer.println(indent + "     AttributeId: " + _attributeId);
    }

    protected void read(byte[] buffer, int offset, int[] length) {
        _type = AttributeType.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x00));
        length[0] = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x04);
        _nonResidentFlag = buffer[offset + 0x08];
        byte nameLength = buffer[offset + 0x09];
        short nameOffset = (short) EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x0A);
        _flags = AttributeFlags.valueOf(EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x0C));
        _attributeId = (short) EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x0E);
        if (nameLength != 0x00) {
            if (nameLength + nameOffset > length[0]) {
                throw new IOException("Corrupt attribute, name outside of attribute");
            }

            _name = new String(buffer, offset + nameOffset, nameLength * 2, Charset.forName("UTF-16LE"));
        }
    }
}
