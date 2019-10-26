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

package DiscUtils.Ext;

import DiscUtils.Core.UnixFileType;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.StreamBuffer;
import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Ownership;
import dotnet4j.io.MemoryStream;


public class Inode implements IByteArraySerializable {
    public int AccessTime;

    public int BlocksCount;

    public int CreationTime;

    public int DeletionTime;

    public int DirAcl;

    public int[] DirectBlocks;

    public int DoubleIndirectBlock;

    public ExtentBlock Extents;

    public byte[] FastSymlink;

    public int FileAcl;

    public int FileSize;

    public int FileVersion;

    public InodeFlags Flags = InodeFlags.SecureDelete;

    public int FragAddress;

    public byte Fragment;

    public byte FragmentSize;

    public short GroupIdHigh;

    public short GroupIdLow;

    public int IndirectBlock;

    public short LinksCount;

    public short Mode;

    public int ModificationTime;

    public int TripleIndirectBlock;

    public short UserIdHigh;

    public short UserIdLow;

    public UnixFileType getFileType() {
        return UnixFileType.valueOf((Mode >>> 12) & 0xff);
    }

    public int size() {
        throw new UnsupportedOperationException();
    }

    public int readFrom(byte[] buffer, int offset) {
        Mode = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0);
        UserIdLow = EndianUtilities.toUInt16LittleEndian(buffer, offset + 2);
        FileSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 4);
        AccessTime = EndianUtilities.toUInt32LittleEndian(buffer, offset + 8);
        CreationTime = EndianUtilities.toUInt32LittleEndian(buffer, offset + 12);
        ModificationTime = EndianUtilities.toUInt32LittleEndian(buffer, offset + 16);
        DeletionTime = EndianUtilities.toUInt32LittleEndian(buffer, offset + 20);
        GroupIdLow = EndianUtilities.toUInt16LittleEndian(buffer, offset + 24);
        LinksCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 26);
        BlocksCount = EndianUtilities.toUInt32LittleEndian(buffer, offset + 28);
        Flags = InodeFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 32));
        FastSymlink = null;
        Extents = null;
        DirectBlocks = null;
        if (getFileType() == UnixFileType.Link && BlocksCount == 0) {
            FastSymlink = new byte[60];
            System.arraycopy(buffer, offset + 40, FastSymlink, 0, 60);
        } else if ((Flags.ordinal() & InodeFlags.ExtentsUsed.ordinal()) != 0) {
            Extents = EndianUtilities.<ExtentBlock> toStruct(ExtentBlock.class, buffer, offset + 40);
        } else {
            DirectBlocks = new int[12];
            for (int i = 0; i < 12; ++i) {
                DirectBlocks[i] = EndianUtilities.toUInt32LittleEndian(buffer, offset + 40 + i * 4);
            }
            IndirectBlock = EndianUtilities.toUInt32LittleEndian(buffer, offset + 88);
            DoubleIndirectBlock = EndianUtilities.toUInt32LittleEndian(buffer, offset + 92);
            TripleIndirectBlock = EndianUtilities.toUInt32LittleEndian(buffer, offset + 96);
        }
        FileVersion = EndianUtilities.toUInt32LittleEndian(buffer, offset + 100);
        FileAcl = EndianUtilities.toUInt32LittleEndian(buffer, offset + 104);
        DirAcl = EndianUtilities.toUInt32LittleEndian(buffer, offset + 108);
        FragAddress = EndianUtilities.toUInt32LittleEndian(buffer, offset + 112);
        Fragment = buffer[offset + 116];
        FragmentSize = buffer[offset + 117];
        UserIdHigh = EndianUtilities.toUInt16LittleEndian(buffer, offset + 120);
        GroupIdHigh = EndianUtilities.toUInt16LittleEndian(buffer, offset + 122);
        return 128;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public IBuffer getContentBuffer(Context context) {
        if (FastSymlink != null) {
            return new StreamBuffer(new MemoryStream(FastSymlink, false), Ownership.Dispose);
        }

        if ((Flags.ordinal() & InodeFlags.ExtentsUsed.ordinal()) != 0) {
            return new ExtentsFileBuffer(context, this);
        }

        return new FileBuffer(context, this);
    }
}
