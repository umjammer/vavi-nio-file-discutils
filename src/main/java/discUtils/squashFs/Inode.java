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
import dotnet4j.io.IOException;


abstract class Inode implements IByteArraySerializable {

    public short gidKey;

    public int inodeNumber;

    public short mode;

    public long modificationTime;

    public int numLinks;

    public InodeType type = InodeType.Directory;

    public short uidKey;

    public long getFileSize() {
        return 0;
    }

    public void setFileSize(long value) {
        throw new UnsupportedOperationException();
    }

    public abstract int size();

    public int readFrom(byte[] buffer, int offset) {
        type = InodeType.values()[EndianUtilities.toUInt16LittleEndian(buffer, offset + 0)];
        mode = EndianUtilities.toUInt16LittleEndian(buffer, offset + 2);
        uidKey = EndianUtilities.toUInt16LittleEndian(buffer, offset + 4);
        gidKey = EndianUtilities.toUInt16LittleEndian(buffer, offset + 6);
        modificationTime = Instant.ofEpochSecond(EndianUtilities.toUInt32LittleEndian(buffer, offset + 8)).toEpochMilli();
        inodeNumber = EndianUtilities.toUInt32LittleEndian(buffer, offset + 12);
        return 16;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian((short) type.ordinal(), buffer, offset + 0);
        EndianUtilities.writeBytesLittleEndian(mode, buffer, offset + 2);
        EndianUtilities.writeBytesLittleEndian(uidKey, buffer, offset + 4);
        EndianUtilities.writeBytesLittleEndian(gidKey, buffer, offset + 6);
        EndianUtilities.writeBytesLittleEndian(Instant.ofEpochMilli(modificationTime).getEpochSecond(), buffer, offset + 8);
        EndianUtilities.writeBytesLittleEndian(inodeNumber, buffer, offset + 12);
    }

    public static Inode read(MetablockReader inodeReader) {
        byte[] typeData = new byte[2];
        if (inodeReader.read(typeData, 0, 2) != 2) {
            throw new IOException("Unable to read Inode type");
        }

        InodeType type = InodeType.values()[EndianUtilities.toUInt16LittleEndian(typeData, 0)];
        Inode inode = instantiateType(type);

        byte[] inodeData = new byte[inode.size()];
        inodeData[0] = typeData[0];
        inodeData[1] = typeData[1];

        if (inodeReader.read(inodeData, 2, inode.size() - 2) != inode.size() - 2) {
            throw new IOException("Unable to read whole Inode");
        }

        inode.readFrom(inodeData, 0);

        return inode;
    }

    private static Inode instantiateType(InodeType type) {
        switch (type) {
        case Directory:
            return new DirectoryInode();
        case ExtendedDirectory:
            return new ExtendedDirectoryInode();
        case File:
            return new RegularInode();
        case Symlink:
            return new SymlinkInode();
        case CharacterDevice:
        case BlockDevice:
            return new DeviceInode();
        default:
            throw new UnsupportedOperationException("Inode type not implemented: " + type);
        }
    }
}
