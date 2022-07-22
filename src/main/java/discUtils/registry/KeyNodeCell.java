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

import vavi.util.win32.DateUtil;

import discUtils.streams.util.EndianUtilities;


public final class KeyNodeCell extends Cell {

    public int classNameIndex;

    public int classNameLength;

    public EnumSet<RegistryKeyFlags> flags;

    public int indexInParent;

    /**
     * Number of bytes to represent largest subkey name in Unicode - no null
     * terminator.
     */
    public int maxSubKeyNameBytes;

    /**
     * Number of bytes to represent largest value content (strings in Unicode,
     * with null terminator - if stored).
     */
    public int maxValDataBytes;

    /**
     * Number of bytes to represent largest value name in Unicode - no null
     * terminator.
     */
    public int maxValNameBytes;

    public String name;

    public int numSubKeys;

    public int numValues;

    public int parentIndex;

    public int securityIndex;

    public int subKeysIndex;

    public long timestamp;

    public int valueListIndex;

    public KeyNodeCell(String name, int parentCellIndex) {
        this(-1);
        flags = EnumSet.of(RegistryKeyFlags.Normal);
        timestamp = System.currentTimeMillis();
        parentIndex = parentCellIndex;
        subKeysIndex = -1;
        valueListIndex = -1;
        securityIndex = -1;
        classNameIndex = -1;
        this.name = name;
    }

    public KeyNodeCell(int index) {
        super(index);
    }

    public int size() {
        return 0x4C + name.length();
    }

    public int readFrom(byte[] buffer, int offset) {
        flags = RegistryKeyFlags.valueOf(EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x02));
        timestamp = DateUtil.fromFileTime(EndianUtilities.toInt64LittleEndian(buffer, offset + 0x04));
        parentIndex = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x10);
        numSubKeys = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x14);
        subKeysIndex = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x1C);
        numValues = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x24);
        valueListIndex = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x28);
        securityIndex = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x2C);
        classNameIndex = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x30);
        maxSubKeyNameBytes = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x34);
        maxValNameBytes = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x3C);
        maxValDataBytes = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x40);
        indexInParent = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x44);
        int nameLength = EndianUtilities.toInt16LittleEndian(buffer, offset + 0x48);
        classNameLength = EndianUtilities.toInt16LittleEndian(buffer, offset + 0x4A);
        name = EndianUtilities.bytesToString(buffer, offset + 0x4C, nameLength);
        return 0x4C + nameLength;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.stringToBytes("nk", buffer, offset, 2);
        EndianUtilities.writeBytesLittleEndian((short) RegistryKeyFlags.valueOf(flags), buffer, offset + 0x02);
        EndianUtilities.writeBytesLittleEndian(DateUtil.toFileTime(timestamp), buffer, offset + 0x04);
        EndianUtilities.writeBytesLittleEndian(parentIndex, buffer, offset + 0x10);
        EndianUtilities.writeBytesLittleEndian(numSubKeys, buffer, offset + 0x14);
        EndianUtilities.writeBytesLittleEndian(subKeysIndex, buffer, offset + 0x1C);
        EndianUtilities.writeBytesLittleEndian(numValues, buffer, offset + 0x24);
        EndianUtilities.writeBytesLittleEndian(valueListIndex, buffer, offset + 0x28);
        EndianUtilities.writeBytesLittleEndian(securityIndex, buffer, offset + 0x2C);
        EndianUtilities.writeBytesLittleEndian(classNameIndex, buffer, offset + 0x30);
        EndianUtilities.writeBytesLittleEndian(indexInParent, buffer, offset + 0x44);
        EndianUtilities.writeBytesLittleEndian((short) name.length(), buffer, offset + 0x48);
        EndianUtilities.writeBytesLittleEndian(classNameLength, buffer, offset + 0x4A);
        EndianUtilities.stringToBytes(name, buffer, offset + 0x4C, name.length());
    }

    public String toString() {
        return "Key: " + name + " " + flags + " <" + timestamp + ">";
    }
}
