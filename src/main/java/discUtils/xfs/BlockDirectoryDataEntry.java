//
// Copyright (c) 2016, Bianco Veigel
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

package discUtils.xfs;

import java.nio.charset.StandardCharsets;

import discUtils.streams.util.EndianUtilities;
import vavi.util.ByteUtil;


public class BlockDirectoryDataEntry extends BlockDirectoryData implements IDirectoryEntry {

    private final boolean ftype;

    private long inode;

    @Override
    public long getInode() {
        return inode;
    }

    public void setInode(long value) {
        inode = value;
    }

    private byte nameLength;

    public int getNameLength() {
        return nameLength & 0xff;
    }

    public void setNameLength(byte value) {
        nameLength = value;
    }

    private byte[] name;

    @Override
    public byte[] getName() {
        return name;
    }

    public void setName(byte[] value) {
        name = value;
    }

    private short tag;

    public short getTag() {
        return tag;
    }

    public void setTag(short value) {
        tag = value;
    }

    private DirectoryFType fType = DirectoryFType.File;

    public DirectoryFType getFType() {
        return fType;
    }

    public void setFType(DirectoryFType value) {
        fType = value;
    }

    @Override
    public int size() {
        int size = 0xb + getNameLength() + (ftype ? 1 : 0);
        int padding = size % 8;
        if (padding != 0)
            return size + (8 - padding);

        return size;
    }

    public BlockDirectoryDataEntry(Context context) {
        ftype = context.getSuperBlock().hasFType();
    }

    @Override
    public int readFrom(byte[] buffer, int offset) {
        inode = ByteUtil.readBeLong(buffer, offset);
        nameLength = buffer[offset + 0x8];
        name = EndianUtilities.toByteArray(buffer, offset + 0x9, getNameLength());
        offset += 0x9 + getNameLength();
        if (ftype) {
            fType = DirectoryFType.values()[buffer[offset]];
            offset++;
        }

        int padding = 6 - ((getNameLength() + (ftype ? 2 : 1)) % 8);
        if (padding < 0)
            padding += 8;

        offset += padding;
        tag = ByteUtil.readBeShort(buffer, offset);
        return size();
    }

    /**
     *
     */
    public String toString() {
        return String.format("%d : %s", inode, new String(name, 0, nameLength, StandardCharsets.US_ASCII));
    }
}
