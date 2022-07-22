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
import discUtils.streams.util.EndianUtilities;


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

    public int size() {
        return 96;
    }

    public int readFrom(byte[] buffer, int offset) {
        magic = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
        if (magic != SquashFsMagic)
            return size();

        inodesCount = EndianUtilities.toUInt32LittleEndian(buffer, offset + 4);
        creationTime = Instant.ofEpochSecond(EndianUtilities.toUInt32LittleEndian(buffer, offset + 8)).toEpochMilli();
        blockSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 12);
        fragmentsCount = EndianUtilities.toUInt32LittleEndian(buffer, offset + 16);
        compression = EndianUtilities.toUInt16LittleEndian(buffer, offset + 20);
        blockSizeLog2 = EndianUtilities.toUInt16LittleEndian(buffer, offset + 22);
        flags = EndianUtilities.toUInt16LittleEndian(buffer, offset + 24);
        uidGidCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 26);
        majorVersion = EndianUtilities.toUInt16LittleEndian(buffer, offset + 28);
        minorVersion = EndianUtilities.toUInt16LittleEndian(buffer, offset + 30);
        rootInode = new MetadataRef(EndianUtilities.toInt64LittleEndian(buffer, offset + 32));
        bytesUsed = EndianUtilities.toInt64LittleEndian(buffer, offset + 40);
        uidGidTableStart = EndianUtilities.toInt64LittleEndian(buffer, offset + 48);
        extendedAttrsTableStart = EndianUtilities.toInt64LittleEndian(buffer, offset + 56);
        inodeTableStart = EndianUtilities.toInt64LittleEndian(buffer, offset + 64);
        directoryTableStart = EndianUtilities.toInt64LittleEndian(buffer, offset + 72);
        fragmentTableStart = EndianUtilities.toInt64LittleEndian(buffer, offset + 80);
        lookupTableStart = EndianUtilities.toInt64LittleEndian(buffer, offset + 88);
        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(magic, buffer, offset + 0);
        EndianUtilities.writeBytesLittleEndian(inodesCount, buffer, offset + 4);
        EndianUtilities.writeBytesLittleEndian(Instant.ofEpochMilli(creationTime).getEpochSecond(), buffer, offset + 8);
        EndianUtilities.writeBytesLittleEndian(blockSize, buffer, offset + 12);
        EndianUtilities.writeBytesLittleEndian(fragmentsCount, buffer, offset + 16);
        EndianUtilities.writeBytesLittleEndian(compression, buffer, offset + 20);
        EndianUtilities.writeBytesLittleEndian(blockSizeLog2, buffer, offset + 22);
        EndianUtilities.writeBytesLittleEndian(flags, buffer, offset + 24);
        EndianUtilities.writeBytesLittleEndian(uidGidCount, buffer, offset + 26);
        EndianUtilities.writeBytesLittleEndian(majorVersion, buffer, offset + 28);
        EndianUtilities.writeBytesLittleEndian(minorVersion, buffer, offset + 30);
        EndianUtilities.writeBytesLittleEndian(rootInode.getValue(), buffer, offset + 32);
        EndianUtilities.writeBytesLittleEndian(bytesUsed, buffer, offset + 40);
        EndianUtilities.writeBytesLittleEndian(uidGidTableStart, buffer, offset + 48);
        EndianUtilities.writeBytesLittleEndian(extendedAttrsTableStart, buffer, offset + 56);
        EndianUtilities.writeBytesLittleEndian(inodeTableStart, buffer, offset + 64);
        EndianUtilities.writeBytesLittleEndian(directoryTableStart, buffer, offset + 72);
        EndianUtilities.writeBytesLittleEndian(fragmentTableStart, buffer, offset + 80);
        EndianUtilities.writeBytesLittleEndian(lookupTableStart, buffer, offset + 88);
    }
}
