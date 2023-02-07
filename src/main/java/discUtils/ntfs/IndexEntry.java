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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;

import discUtils.streams.util.MathUtilities;
import vavi.util.ByteUtil;


public class IndexEntry {

    public static final int EndNodeSize = 0x18;

    protected byte[] dataBuffer;

    protected EnumSet<IndexEntryFlags> flags;

    protected byte[] keyBuffer;

    // Only valid if Node flag set
    protected long vcn;

    public IndexEntry(boolean isFileIndexEntry) {
        this.isFileIndexEntry = isFileIndexEntry;
        flags = EnumSet.noneOf(IndexEntryFlags.class);
    }

    public IndexEntry(IndexEntry toCopy, byte[] newKey, byte[] newData) {
        isFileIndexEntry = toCopy.isFileIndexEntry();
        flags = toCopy.flags;
        vcn = toCopy.vcn;
        keyBuffer = newKey;
        dataBuffer = newData;
    }

    public IndexEntry(byte[] key, byte[] data, boolean isFileIndexEntry) {
        this.isFileIndexEntry = isFileIndexEntry;
        flags = EnumSet.noneOf(IndexEntryFlags.class);
        keyBuffer = key;
        dataBuffer = data;
    }

    public long getChildrenVirtualCluster() {
        return vcn;
    }

    public void setChildrenVirtualCluster(long value) {
        vcn = value;
    }

    public byte[] getDataBuffer() {
        return dataBuffer;
    }

    public void setDataBuffer(byte[] value) {
        dataBuffer = value;
    }

    public EnumSet<IndexEntryFlags> getFlags() {
        return flags;
    }

    public void setFlags(EnumSet<IndexEntryFlags> value) {
        flags = value;
    }

    private boolean isFileIndexEntry;

    protected boolean isFileIndexEntry() {
        return isFileIndexEntry;
    }

    public byte[] getKeyBuffer() {
        return keyBuffer;
    }

    public void setKeyBuffer(byte[] value) {
        keyBuffer = value;
    }

    public int getSize() {
        int size = 0x10; // start of variable data

        if (!flags.contains(IndexEntryFlags.End)) {
            size += keyBuffer.length;
            size += isFileIndexEntry() ? 0 : dataBuffer.length;
        }

        size = MathUtilities.roundUp(size, 8);

        if (flags.contains(IndexEntryFlags.Node)) {
            size += 8;
        }

        return size;
    }

    public void read(byte[] buffer, int offset) {
        @SuppressWarnings("unused")
        short dataOffset = ByteUtil.readLeShort(buffer, offset + 0x00);
        short dataLength = ByteUtil.readLeShort(buffer, offset + 0x02);
        short length = ByteUtil.readLeShort(buffer, offset + 0x08);
        short keyLength = ByteUtil.readLeShort(buffer, offset + 0x0A);
//        assert dataLength >= 0 && length >=0 && keyLength >= 0;
        flags = IndexEntryFlags.valueOf(ByteUtil.readLeShort(buffer, offset + 0x0C));

        if (!flags.contains(IndexEntryFlags.End)) {
            keyBuffer = new byte[keyLength];
            System.arraycopy(buffer, offset + 0x10, keyBuffer, 0, keyLength);

            if (isFileIndexEntry()) {
                // Special case, for file indexes, the MFT ref is held where the data offset & length go
                dataBuffer = new byte[8];
                System.arraycopy(buffer, offset + 0x00, dataBuffer, 0, 8);
            } else {
                dataBuffer = new byte[dataLength];
                System.arraycopy(buffer, offset + 0x10 + keyLength, dataBuffer, 0, dataLength);
            }
        }

        if (flags.contains(IndexEntryFlags.Node)) {
            vcn = ByteUtil.readLeLong(buffer, offset + length - 8);
        }
    }

    public void writeTo(byte[] buffer, int offset) {
        int length = getSize();

        if (!flags.contains(IndexEntryFlags.End)) {
            int keyLength = keyBuffer.length;

            if (isFileIndexEntry()) {
                System.arraycopy(dataBuffer, 0, buffer, offset + 0x00, 8);
            } else {
                int dataOffset = isFileIndexEntry() ? 0 : 0x10 + keyLength;
                int dataLength = dataBuffer.length;

                ByteUtil.writeLeShort((short) dataOffset, buffer, offset + 0x00);
                ByteUtil.writeLeShort((short) dataLength, buffer, offset + 0x02);
                System.arraycopy(dataBuffer, 0, buffer, offset + dataOffset, dataBuffer.length);
            }
            ByteUtil.writeLeShort((short) keyLength, buffer, offset + 0x0A);
//Debug.println(keyBuffer.length + " | " + buffer.length + ", " + (offset + 0x10));
            assert buffer.length > offset  + 0x10 + keyBuffer.length : buffer.length + ", " + (offset + 0x10) + ", " + keyBuffer.length;
            System.arraycopy(keyBuffer, 0, buffer, offset + 0x10, keyBuffer.length);
        } else {
            ByteUtil.writeLeShort((short) 0, buffer, offset + 0x00); // dataOffset
            ByteUtil.writeLeShort((short) 0, buffer, offset + 0x02); // dataLength
            ByteUtil.writeLeShort((short) 0, buffer, offset + 0x0A); // keyLength
        }

        ByteUtil.writeLeShort((short) length, buffer, offset + 0x08);
        ByteUtil.writeLeShort((short) IndexEntryFlags.valueOf(flags), buffer, offset + 0x0C);
        if (flags.contains(IndexEntryFlags.Node)) {
            ByteUtil.writeLeLong(vcn, buffer, offset + length - 8);
        }
    }

    public String toString() {
        try {
            return (keyBuffer != null ? new String(keyBuffer, StandardCharsets.US_ASCII) : "null") + ": " +
                   (dataBuffer != null ? Arrays.toString(dataBuffer) : "null");
        } catch (Exception e) {
            e.printStackTrace();
            return super.toString();
        }
    }
}
