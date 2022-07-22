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

package discUtils.registry;

import java.util.EnumSet;

import discUtils.streams.util.EndianUtilities;


public final class ValueCell extends Cell {

    private EnumSet<ValueFlags> flags = EnumSet.noneOf(ValueFlags.class);

    public ValueCell(String name) {
        this(-1);
        setName(name);
    }

    public ValueCell(int index) {
        super(index);
        setDataIndex(-1);
    }

    private int dataIndex;

    public int getDataIndex() {
        return dataIndex;
    }

    public void setDataIndex(int value) {
        dataIndex = value;
    }

    private int dataLength;

    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int value) {
        dataLength = value;
    }

    private RegistryValueType dataType = RegistryValueType.None;

    public RegistryValueType getDataType() {
        return dataType;
    }

    public void setDataType(RegistryValueType value) {
        dataType = value;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    public int size() {
        return 0x14 + (getName() == null || getName().isEmpty() ? 0 : getName().length());
    }

    public int readFrom(byte[] buffer, int offset) {
        int nameLen = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x02);
        dataLength = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x04);
        dataIndex = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x08);
        dataType = RegistryValueType.values()[EndianUtilities.toInt32LittleEndian(buffer, offset + 0x0C)];
        flags = ValueFlags.valueOf(EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x10));
        if (flags.contains(ValueFlags.Named)) {
            name = EndianUtilities.bytesToString(buffer, offset + 0x14, nameLen).replaceAll("(^\0*|\0*$)", "");
        }

        return 0x14 + nameLen;
    }

    public void writeTo(byte[] buffer, int offset) {
        int nameLen;
        if (name == null || name.isEmpty()) {
            flags.remove(ValueFlags.Named);
            nameLen = 0;
        } else {
            flags.add(ValueFlags.Named);
            nameLen = name.length();
        }
        EndianUtilities.stringToBytes("vk", buffer, offset, 2);
        EndianUtilities.writeBytesLittleEndian((short) nameLen, buffer, offset + 0x02);
        EndianUtilities.writeBytesLittleEndian(dataLength, buffer, offset + 0x04);
        EndianUtilities.writeBytesLittleEndian(dataIndex, buffer, offset + 0x08);
        EndianUtilities.writeBytesLittleEndian(dataType.ordinal(), buffer, offset + 0x0C);
        EndianUtilities.writeBytesLittleEndian((short) ValueFlags.valueOf(flags), buffer, offset + 0x10);
        if (nameLen != 0) {
            EndianUtilities.stringToBytes(getName(), buffer, offset + 0x14, nameLen);
        }
    }

    public String toString() {
        return "ValueCell{" + name + ", " + dataType + "}";
    }
}
