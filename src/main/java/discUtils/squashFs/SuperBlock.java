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

    public int BlockSize;

    public short BlockSizeLog2;

    public long BytesUsed;

    public short Compression;

    public long CreationTime;

    public long DirectoryTableStart;

    public long ExtendedAttrsTableStart;

    public short Flags;

    public int FragmentsCount;

    public long FragmentTableStart;

    public int InodesCount;

    public long InodeTableStart;

    public long LookupTableStart;

    public int Magic;

    public short MajorVersion;

    public short MinorVersion;

    public MetadataRef RootInode;

    private short uidGidCount;

    public int getUidGidCount() {
        return uidGidCount & 0xffff;
    }

    public void setUidGidCount(short value) {
        uidGidCount = value;
    }

    public long UidGidTableStart;

    public int size() {
        return 96;
    }

    public int readFrom(byte[] buffer, int offset) {
        Magic = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
        if (Magic != SquashFsMagic)
            return size();

        InodesCount = EndianUtilities.toUInt32LittleEndian(buffer, offset + 4);
        CreationTime = Instant.ofEpochSecond(EndianUtilities.toUInt32LittleEndian(buffer, offset + 8)).toEpochMilli();
        BlockSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 12);
        FragmentsCount = EndianUtilities.toUInt32LittleEndian(buffer, offset + 16);
        Compression = EndianUtilities.toUInt16LittleEndian(buffer, offset + 20);
        BlockSizeLog2 = EndianUtilities.toUInt16LittleEndian(buffer, offset + 22);
        Flags = EndianUtilities.toUInt16LittleEndian(buffer, offset + 24);
        uidGidCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 26);
        MajorVersion = EndianUtilities.toUInt16LittleEndian(buffer, offset + 28);
        MinorVersion = EndianUtilities.toUInt16LittleEndian(buffer, offset + 30);
        RootInode = new MetadataRef(EndianUtilities.toInt64LittleEndian(buffer, offset + 32));
        BytesUsed = EndianUtilities.toInt64LittleEndian(buffer, offset + 40);
        UidGidTableStart = EndianUtilities.toInt64LittleEndian(buffer, offset + 48);
        ExtendedAttrsTableStart = EndianUtilities.toInt64LittleEndian(buffer, offset + 56);
        InodeTableStart = EndianUtilities.toInt64LittleEndian(buffer, offset + 64);
        DirectoryTableStart = EndianUtilities.toInt64LittleEndian(buffer, offset + 72);
        FragmentTableStart = EndianUtilities.toInt64LittleEndian(buffer, offset + 80);
        LookupTableStart = EndianUtilities.toInt64LittleEndian(buffer, offset + 88);
        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(Magic, buffer, offset + 0);
        EndianUtilities.writeBytesLittleEndian(InodesCount, buffer, offset + 4);
        EndianUtilities.writeBytesLittleEndian(Instant.ofEpochMilli(CreationTime).getEpochSecond(), buffer, offset + 8);
        EndianUtilities.writeBytesLittleEndian(BlockSize, buffer, offset + 12);
        EndianUtilities.writeBytesLittleEndian(FragmentsCount, buffer, offset + 16);
        EndianUtilities.writeBytesLittleEndian(Compression, buffer, offset + 20);
        EndianUtilities.writeBytesLittleEndian(BlockSizeLog2, buffer, offset + 22);
        EndianUtilities.writeBytesLittleEndian(Flags, buffer, offset + 24);
        EndianUtilities.writeBytesLittleEndian(uidGidCount, buffer, offset + 26);
        EndianUtilities.writeBytesLittleEndian(MajorVersion, buffer, offset + 28);
        EndianUtilities.writeBytesLittleEndian(MinorVersion, buffer, offset + 30);
        EndianUtilities.writeBytesLittleEndian(RootInode.getValue(), buffer, offset + 32);
        EndianUtilities.writeBytesLittleEndian(BytesUsed, buffer, offset + 40);
        EndianUtilities.writeBytesLittleEndian(UidGidTableStart, buffer, offset + 48);
        EndianUtilities.writeBytesLittleEndian(ExtendedAttrsTableStart, buffer, offset + 56);
        EndianUtilities.writeBytesLittleEndian(InodeTableStart, buffer, offset + 64);
        EndianUtilities.writeBytesLittleEndian(DirectoryTableStart, buffer, offset + 72);
        EndianUtilities.writeBytesLittleEndian(FragmentTableStart, buffer, offset + 80);
        EndianUtilities.writeBytesLittleEndian(LookupTableStart, buffer, offset + 88);
    }
}
