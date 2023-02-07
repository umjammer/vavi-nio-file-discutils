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

package discUtils.squashFs;

import java.time.Instant;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public class SuperBlock implements IByteArraySerializable {

    public static final int SquashFsMagic = 0x73717368;

    public int blockSize;

    public short blockSizeLog2;

    public long bytesUsed;

    public short compression;

    public long creationTime;

    public long directoryTableStart;

    public long extendedAttrsTableStart;

    public short flags;

    public int fragmentsCount;

    public long fragmentTableStart;

    public int inodesCount;

    public long inodeTableStart;

    public long lookupTableStart;

    public int magic;

    public short majorVersion;

    public short minorVersion;

    public MetadataRef rootInode;

    private short uidGidCount;

    public int getUidGidCount() {
        return uidGidCount & 0xffff;
    }

    public void setUidGidCount(short value) {
        uidGidCount = value;
    }

    public long uidGidTableStart;

    @Override public int size() {
        return 96;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        magic = ByteUtil.readLeInt(buffer, offset + 0);
        if (magic != SquashFsMagic)
            return size();

        inodesCount = ByteUtil.readLeInt(buffer, offset + 4);
        creationTime = Instant.ofEpochSecond(ByteUtil.readLeInt(buffer, offset + 8)).toEpochMilli();
        blockSize = ByteUtil.readLeInt(buffer, offset + 12);
        fragmentsCount = ByteUtil.readLeInt(buffer, offset + 16);
        compression = ByteUtil.readLeShort(buffer, offset + 20);
        blockSizeLog2 = ByteUtil.readLeShort(buffer, offset + 22);
        flags = ByteUtil.readLeShort(buffer, offset + 24);
        uidGidCount = ByteUtil.readLeShort(buffer, offset + 26);
        majorVersion = ByteUtil.readLeShort(buffer, offset + 28);
        minorVersion = ByteUtil.readLeShort(buffer, offset + 30);
        rootInode = new MetadataRef(ByteUtil.readLeLong(buffer, offset + 32));
        bytesUsed = ByteUtil.readLeLong(buffer, offset + 40);
        uidGidTableStart = ByteUtil.readLeLong(buffer, offset + 48);
        extendedAttrsTableStart = ByteUtil.readLeLong(buffer, offset + 56);
        inodeTableStart = ByteUtil.readLeLong(buffer, offset + 64);
        directoryTableStart = ByteUtil.readLeLong(buffer, offset + 72);
        fragmentTableStart = ByteUtil.readLeLong(buffer, offset + 80);
        lookupTableStart = ByteUtil.readLeLong(buffer, offset + 88);
        return size();
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        ByteUtil.writeLeInt(magic, buffer, offset + 0);
        ByteUtil.writeLeInt(inodesCount, buffer, offset + 4);
        ByteUtil.writeLeInt((int) Instant.ofEpochMilli(creationTime).getEpochSecond(), buffer, offset + 8);
        ByteUtil.writeLeInt(blockSize, buffer, offset + 12);
        ByteUtil.writeLeInt(fragmentsCount, buffer, offset + 16);
        ByteUtil.writeLeShort(compression, buffer, offset + 20);
        ByteUtil.writeLeShort(blockSizeLog2, buffer, offset + 22);
        ByteUtil.writeLeShort(flags, buffer, offset + 24);
        ByteUtil.writeLeShort(uidGidCount, buffer, offset + 26);
        ByteUtil.writeLeShort(majorVersion, buffer, offset + 28);
        ByteUtil.writeLeShort(minorVersion, buffer, offset + 30);
        ByteUtil.writeLeLong(rootInode.getValue(), buffer, offset + 32);
        ByteUtil.writeLeLong(bytesUsed, buffer, offset + 40);
        ByteUtil.writeLeLong(uidGidTableStart, buffer, offset + 48);
        ByteUtil.writeLeLong(extendedAttrsTableStart, buffer, offset + 56);
        ByteUtil.writeLeLong(inodeTableStart, buffer, offset + 64);
        ByteUtil.writeLeLong(directoryTableStart, buffer, offset + 72);
        ByteUtil.writeLeLong(fragmentTableStart, buffer, offset + 80);
        ByteUtil.writeLeLong(lookupTableStart, buffer, offset + 88);
    }
}
