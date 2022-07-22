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

package discUtils.ext;

import java.util.EnumSet;

import discUtils.core.UnixFileType;
import discUtils.streams.IByteArraySerializable;
import discUtils.streams.StreamBuffer;
import discUtils.streams.buffer.IBuffer;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.Ownership;
import dotnet4j.io.MemoryStream;


public class Inode implements IByteArraySerializable {

    public int accessTime;

    public int blocksCount;

    public int creationTime;

    public int deletionTime;

    public int dirAcl;

    public int[] directBlocks;

    public int doubleIndirectBlock;

    public ExtentBlock extents;

    public byte[] fastSymlink;

    public int fileAcl;

    public int fileSize;

    public int fileVersion;

    public EnumSet<InodeFlags> flags = EnumSet.noneOf(InodeFlags.class);

    public int fragAddress;

    public byte fragment;

    public byte fragmentSize;

    public short groupIdHigh;

    public short groupIdLow;

    public int indirectBlock;

    private short linksCount;

    public int getLinksCount() {
        return linksCount & 0xffff;
    }

    public short mode;

    public int modificationTime;

    public int tripleIndirectBlock;

    public short userIdHigh;

    public short userIdLow;

    public UnixFileType getFileType() {
        return UnixFileType.values()[((mode & 0xffff) >>> 12) & 0xff];
    }

    public int size() {
        throw new UnsupportedOperationException();
    }

    public int readFrom(byte[] buffer, int offset) {
        mode = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0);
        userIdLow = EndianUtilities.toUInt16LittleEndian(buffer, offset + 2);
        fileSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 4);
        accessTime = EndianUtilities.toUInt32LittleEndian(buffer, offset + 8);
        creationTime = EndianUtilities.toUInt32LittleEndian(buffer, offset + 12);
        modificationTime = EndianUtilities.toUInt32LittleEndian(buffer, offset + 16);
        deletionTime = EndianUtilities.toUInt32LittleEndian(buffer, offset + 20);
        groupIdLow = EndianUtilities.toUInt16LittleEndian(buffer, offset + 24);
        linksCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 26);
        blocksCount = EndianUtilities.toUInt32LittleEndian(buffer, offset + 28);
        flags = InodeFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 32));

        fastSymlink = null;
        extents = null;
        directBlocks = null;
        if (getFileType() == UnixFileType.Link && blocksCount == 0) {
            fastSymlink = new byte[60];
            System.arraycopy(buffer, offset + 40, fastSymlink, 0, 60);
        } else if (flags.contains(InodeFlags.ExtentsUsed)) {
            extents = EndianUtilities.toStruct(ExtentBlock.class, buffer, offset + 40);
        } else {
            directBlocks = new int[12];
            for (int i = 0; i < 12; ++i) {
                directBlocks[i] = EndianUtilities.toUInt32LittleEndian(buffer, offset + 40 + i * 4);
            }

            indirectBlock = EndianUtilities.toUInt32LittleEndian(buffer, offset + 88);
            doubleIndirectBlock = EndianUtilities.toUInt32LittleEndian(buffer, offset + 92);
            tripleIndirectBlock = EndianUtilities.toUInt32LittleEndian(buffer, offset + 96);
        }

        fileVersion = EndianUtilities.toUInt32LittleEndian(buffer, offset + 100);
        fileAcl = EndianUtilities.toUInt32LittleEndian(buffer, offset + 104);
        dirAcl = EndianUtilities.toUInt32LittleEndian(buffer, offset + 108);
        fragAddress = EndianUtilities.toUInt32LittleEndian(buffer, offset + 112);
        fragment = buffer[offset + 116];
        fragmentSize = buffer[offset + 117];
        userIdHigh = EndianUtilities.toUInt16LittleEndian(buffer, offset + 120);
        groupIdHigh = EndianUtilities.toUInt16LittleEndian(buffer, offset + 122);

        return 128;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public IBuffer getContentBuffer(Context context) {
        if (fastSymlink != null) {
            return new StreamBuffer(new MemoryStream(fastSymlink, false), Ownership.Dispose);
        }
        if (flags.contains(InodeFlags.ExtentsUsed)) {
            return new ExtentsFileBuffer(context, this);
        }
        return new FileBuffer(context, this);
    }
}
