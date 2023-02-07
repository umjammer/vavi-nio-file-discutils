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
import dotnet4j.io.IOException;
import vavi.util.ByteUtil;


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

    @Override public abstract int size();

    @Override public int readFrom(byte[] buffer, int offset) {
        type = InodeType.values()[ByteUtil.readLeShort(buffer, offset + 0)];
        mode = ByteUtil.readLeShort(buffer, offset + 2);
        uidKey = ByteUtil.readLeShort(buffer, offset + 4);
        gidKey = ByteUtil.readLeShort(buffer, offset + 6);
        modificationTime = Instant.ofEpochSecond(ByteUtil.readLeInt(buffer, offset + 8)).toEpochMilli();
        inodeNumber = ByteUtil.readLeInt(buffer, offset + 12);
        return 16;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        ByteUtil.writeLeShort((short) type.ordinal(), buffer, offset + 0);
        ByteUtil.writeLeShort(mode, buffer, offset + 2);
        ByteUtil.writeLeShort(uidKey, buffer, offset + 4);
        ByteUtil.writeLeShort(gidKey, buffer, offset + 6);
        ByteUtil.writeLeInt((int) Instant.ofEpochMilli(modificationTime).getEpochSecond(), buffer, offset + 8);
        ByteUtil.writeLeInt(inodeNumber, buffer, offset + 12);
    }

    public static Inode read(MetablockReader inodeReader) {
        byte[] typeData = new byte[2];
        if (inodeReader.read(typeData, 0, 2) != 2) {
            throw new IOException("Unable to read Inode type");
        }

        InodeType type = InodeType.values()[ByteUtil.readLeShort(typeData, 0)];
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
