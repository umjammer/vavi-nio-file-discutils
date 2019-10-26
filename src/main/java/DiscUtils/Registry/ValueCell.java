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

package DiscUtils.Registry;

import java.util.EnumSet;

import DiscUtils.Streams.Util.EndianUtilities;


public final class ValueCell extends Cell {
    private EnumSet<ValueFlags> _flags;

    public ValueCell(String name) {
        this(-1);
        setName(name);
    }

    public ValueCell(int index) {
        super(index);
        setDataIndex(-1);
    }

    private int __DataIndex;

    public int getDataIndex() {
        return __DataIndex;
    }

    public void setDataIndex(int value) {
        __DataIndex = value;
    }

    private int __DataLength;

    public int getDataLength() {
        return __DataLength;
    }

    public void setDataLength(int value) {
        __DataLength = value;
    }

    private RegistryValueType __DataType = RegistryValueType.None;

    public RegistryValueType getDataType() {
        return __DataType;
    }

    public void setDataType(RegistryValueType value) {
        __DataType = value;
    }

    private String __Name;

    public String getName() {
        return __Name;
    }

    public void setName(String value) {
        __Name = value;
    }

    public int size() {
        return 0x14 + (getName() == null || getName().isEmpty() ? 0 : getName().length());
    }

    public int readFrom(byte[] buffer, int offset) {
        int nameLen = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x02);
        setDataLength(EndianUtilities.toInt32LittleEndian(buffer, offset + 0x04));
        setDataIndex(EndianUtilities.toInt32LittleEndian(buffer, offset + 0x08));
        setDataType(RegistryValueType.valueOf(EndianUtilities.toInt32LittleEndian(buffer, offset + 0x0C)));
        _flags = ValueFlags.valueOf(EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x10));
        if (_flags.contains(ValueFlags.Named)) {
            setName(EndianUtilities.bytesToString(buffer, offset + 0x14, nameLen).replaceAll("(^\0*|\0*$)", ""));
        }

        return 0x14 + nameLen;
    }

    public void writeTo(byte[] buffer, int offset) {
        int nameLen;
        if (_flags == null) { // TODO no initializer
            _flags = EnumSet.noneOf(ValueFlags.class);
        }
        if (getName() == null || getName().isEmpty()) {
            _flags.remove(ValueFlags.Named);
            nameLen = 0;
        } else {
            _flags.add(ValueFlags.Named);
            nameLen = getName().length();
        }
        EndianUtilities.stringToBytes("vk", buffer, offset, 2);
        EndianUtilities.writeBytesLittleEndian(nameLen, buffer, offset + 0x02);
        EndianUtilities.writeBytesLittleEndian(getDataLength(), buffer, offset + 0x04);
        EndianUtilities.writeBytesLittleEndian(getDataIndex(), buffer, offset + 0x08);
        EndianUtilities.writeBytesLittleEndian(getDataType().ordinal(), buffer, offset + 0x0C);
        EndianUtilities.writeBytesLittleEndian((short) ValueFlags.valueOf(_flags), buffer, offset + 0x10);
        if (nameLen != 0) {
            EndianUtilities.stringToBytes(getName(), buffer, offset + 0x14, nameLen);
        }
    }

    public String toString() {
        return "ValueCell{" + __Name + ", " + __DataType + "}";
    }
}
