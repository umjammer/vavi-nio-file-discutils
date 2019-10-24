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

import java.time.Instant;
import java.util.EnumSet;

import vavi.util.win32.DateUtil;

import DiscUtils.Streams.Util.EndianUtilities;


public final class KeyNodeCell extends Cell {
    public int ClassNameIndex;

    public int ClassNameLength;

    public EnumSet<RegistryKeyFlags> Flags;

    public int IndexInParent;

    /**
     * Number of bytes to represent largest subkey name in Unicode - no null
     * terminator.
     */
    public int MaxSubKeyNameBytes;

    /**
     * Number of bytes to represent largest value content (strings in Unicode,
     * with null terminator - if stored).
     */
    public int MaxValDataBytes;

    /**
     * Number of bytes to represent largest value name in Unicode - no null
     * terminator.
     */
    public int MaxValNameBytes;

    public String Name;

    public int NumSubKeys;

    public int NumValues;

    public int ParentIndex;

    public int SecurityIndex;

    public int SubKeysIndex;

    public long Timestamp;

    public int ValueListIndex;

    public KeyNodeCell(String name, int parentCellIndex) {
        this(-1);
        Flags = EnumSet.of(RegistryKeyFlags.Normal);
        Timestamp = System.currentTimeMillis();
        ParentIndex = parentCellIndex;
        SubKeysIndex = -1;
        ValueListIndex = -1;
        SecurityIndex = -1;
        ClassNameIndex = -1;
        Name = name;
    }

    public KeyNodeCell(int index) {
        super(index);
    }

    public int sizeOf() {
        return 0x4C + Name.length();
    }

    public int readFrom(byte[] buffer, int offset) {
        Flags = RegistryKeyFlags.valueOf(EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x02));
        Timestamp = DateUtil.filetimeToLong(EndianUtilities.toInt64LittleEndian(buffer, offset + 0x04));
        ParentIndex = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x10);
        NumSubKeys = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x14);
        SubKeysIndex = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x1C);
        NumValues = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x24);
        ValueListIndex = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x28);
        SecurityIndex = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x2C);
        ClassNameIndex = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x30);
        MaxSubKeyNameBytes = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x34);
        MaxValNameBytes = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x3C);
        MaxValDataBytes = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x40);
        IndexInParent = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x44);
        int nameLength = EndianUtilities.toInt16LittleEndian(buffer, offset + 0x48);
        ClassNameLength = EndianUtilities.toInt16LittleEndian(buffer, offset + 0x4A);
        Name = EndianUtilities.bytesToString(buffer, offset + 0x4C, nameLength);
        return 0x4C + nameLength;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.stringToBytes("nk", buffer, offset, 2);
        EndianUtilities.writeBytesLittleEndian((short) RegistryKeyFlags.valueOf(Flags), buffer, offset + 0x02);
        EndianUtilities.writeBytesLittleEndian(DateUtil.toFileTime(Instant.ofEpochMilli(Timestamp)), buffer, offset + 0x04);
        EndianUtilities.writeBytesLittleEndian(ParentIndex, buffer, offset + 0x10);
        EndianUtilities.writeBytesLittleEndian(NumSubKeys, buffer, offset + 0x14);
        EndianUtilities.writeBytesLittleEndian(SubKeysIndex, buffer, offset + 0x1C);
        EndianUtilities.writeBytesLittleEndian(NumValues, buffer, offset + 0x24);
        EndianUtilities.writeBytesLittleEndian(ValueListIndex, buffer, offset + 0x28);
        EndianUtilities.writeBytesLittleEndian(SecurityIndex, buffer, offset + 0x2C);
        EndianUtilities.writeBytesLittleEndian(ClassNameIndex, buffer, offset + 0x30);
        EndianUtilities.writeBytesLittleEndian(IndexInParent, buffer, offset + 0x44);
        EndianUtilities.writeBytesLittleEndian((short) Name.length(), buffer, offset + 0x48);
        EndianUtilities.writeBytesLittleEndian(ClassNameLength, buffer, offset + 0x4A);
        EndianUtilities.stringToBytes(Name, buffer, offset + 0x4C, Name.length());
    }

    public String toString() {
        return "Key:" + Name + "[" + Flags + "] <" + Timestamp + ">";
    }
}
