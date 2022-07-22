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
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import discUtils.streams.buffer.IBuffer;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.Range;
import dotnet4j.io.IOException;
import dotnet4j.util.compat.StringUtilities;


public abstract class AttributeRecord implements Comparable<AttributeRecord> {

    protected short attributeId;

    protected EnumSet<AttributeFlags> flags;

    protected String name;

    protected byte nonResidentFlag;

    protected AttributeType type;

    public AttributeRecord() {
    }

    public AttributeRecord(AttributeType type, String name, short id, EnumSet<AttributeFlags> flags) {
        this.type = type;
        this.name = name;
        attributeId = id;
        this.flags = flags;
    }

    public abstract long getAllocatedLength();

    public abstract void setAllocatedLength(long value);

    public short getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(short value) {
        attributeId = value;
    }

    public AttributeType getAttributeType() {
        return type;
    }

    public abstract long getDataLength();

    public abstract void setDataLength(long value);

    public EnumSet<AttributeFlags> getFlags() {
        return flags;
    }

    public void setFlags(EnumSet<AttributeFlags> value) {
        flags = value;
    }

    public abstract long getInitializedDataLength();

    public abstract void setInitializedDataLength(long value);

    public boolean isNonResident() {
        return nonResidentFlag != 0;
    }

    public String getName() {
        return name;
    }

    public abstract int getSize();

    public abstract long getStartVcn();

    public int compareTo(AttributeRecord other) {
        int val = type.getValue() - other.type.getValue();
        if (val != 0) {
            return val;
        }

        val = StringUtilities.compare(name, other.name, true);
        if (val != 0) {
            return val;
        }

        return attributeId - other.attributeId;
    }

    /**
     * @param length {@cs out}
     */
    public static AttributeRecord fromBytes(byte[] buffer, int offset, int[] length) {
        if (EndianUtilities.toUInt32LittleEndian(buffer, offset) == 0xFFFFFFFF) {
            length[0] = 0;
            return null;
        }
        if (buffer[offset + 0x08] != 0x00) {
            return new NonResidentAttributeRecord(buffer, offset, length);
        }
        return new ResidentAttributeRecord(buffer, offset, length);
    }

    public static Comparator<AttributeRecord> compareStartVcns = (x, y) -> {
        if (x.getStartVcn() < y.getStartVcn()) {
            return -1;
        }
        if (x.getStartVcn() == y.getStartVcn()) {
            return 0;
        }
        return 1;
    };

    public abstract List<Range> getClusters();

    public abstract IBuffer getReadOnlyDataBuffer(INtfsContext context);

    public abstract int write(byte[] buffer, int offset);

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "ATTRIBUTE RECORD");
        writer.println(indent + "            Type: " + type);
        writer.println(indent + "    Non-Resident: " + nonResidentFlag);
        writer.println(indent + "            Name: " + name);
        writer.println(indent + "           flags: " + flags);
        writer.println(indent + "     attributeId: " + attributeId);
    }

    /**
     * @param length {@cs out}
     */
    protected void read(byte[] buffer, int offset, int[] length) {
        type = AttributeType.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x00));
        length[0] = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x04);

        nonResidentFlag = buffer[offset + 0x08];
        int nameLength = buffer[offset + 0x09] & 0xff;
        int nameOffset = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x0A) & 0xffff;
        flags = AttributeFlags.valueOf(EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x0C));
        attributeId = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x0E);

        if (nameLength != 0x00) {
            if (nameLength + nameOffset > length[0]) {
                throw new IOException("Corrupt attribute, name outside of attribute");
            }

            name = new String(buffer, offset + nameOffset, nameLength * 2, StandardCharsets.UTF_16LE);
        }
    }
}
