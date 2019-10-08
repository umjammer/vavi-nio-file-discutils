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

package DiscUtils.HfsPlus;

import java.time.ZoneId;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public final class VolumeHeader implements IByteArraySerializable {
    public static final short HfsPlusSignature = 0x482b;

    public ForkData AllocationFile;

    public VolumeAttributes Attributes = VolumeAttributes.None;

    public ForkData AttributesFile;

    public long BackupDate;

    public int BlockSize;

    public ForkData CatalogFile;

    public long CheckedDate;

    public long CreateDate;

    public int DataClumpSize;

    public long EncodingsBitmap;

    public ForkData ExtentsFile;

    public int FileCount;

    public int[] FinderInfo;

    public int FolderCount;

    public int FreeBlocks;

    public int JournalInfoBlock;

    public int LastMountedVersion;

    public long ModifyDate;

    public int NextAllocation;

    public CatalogNodeId NextCatalogId;

    public int ResourceClumpSize;

    public short Signature;

    public ForkData StartupFile;

    public int TotalBlocks;

    public short Version;

    public int WriteCount;

    public boolean getIsValid() {
        return Signature == HfsPlusSignature;
    }

    public long getSize() {
        return 512;
    }

    public int readFrom(byte[] buffer, int offset) {
        Signature = EndianUtilities.toUInt16BigEndian(buffer, offset + 0);
        if (!getIsValid())
            return (int) getSize();

        Version = EndianUtilities.toUInt16BigEndian(buffer, offset + 2);
        Attributes = VolumeAttributes.valueOf(EndianUtilities.toUInt32BigEndian(buffer, offset + 4));
        LastMountedVersion = EndianUtilities.toUInt32BigEndian(buffer, offset + 8);
        JournalInfoBlock = EndianUtilities.toUInt32BigEndian(buffer, offset + 12);
        CreateDate = HfsPlusUtilities.readHFSPlusDate(ZoneId.systemDefault(), buffer, offset + 16);
        ModifyDate = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 20);
        BackupDate = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 24);
        CheckedDate = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 28);
        FileCount = EndianUtilities.toUInt32BigEndian(buffer, offset + 32);
        FolderCount = EndianUtilities.toUInt32BigEndian(buffer, offset + 36);
        BlockSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 40);
        TotalBlocks = EndianUtilities.toUInt32BigEndian(buffer, offset + 44);
        FreeBlocks = EndianUtilities.toUInt32BigEndian(buffer, offset + 48);
        NextAllocation = EndianUtilities.toUInt32BigEndian(buffer, offset + 52);
        ResourceClumpSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 56);
        DataClumpSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 60);
        NextCatalogId = new CatalogNodeId(EndianUtilities.toUInt32BigEndian(buffer, offset + 64));
        WriteCount = EndianUtilities.toUInt32BigEndian(buffer, offset + 68);
        EncodingsBitmap = EndianUtilities.toUInt64BigEndian(buffer, offset + 72);
        FinderInfo = new int[8];
        for (int i = 0; i < 8; ++i) {
            FinderInfo[i] = EndianUtilities.toUInt32BigEndian(buffer, offset + 80 + i * 4);
        }
        AllocationFile = EndianUtilities.<ForkData> toStruct(ForkData.class, buffer, offset + 112);
        ExtentsFile = EndianUtilities.<ForkData> toStruct(ForkData.class, buffer, offset + 192);
        CatalogFile = EndianUtilities.<ForkData> toStruct(ForkData.class, buffer, offset + 272);
        AttributesFile = EndianUtilities.<ForkData> toStruct(ForkData.class, buffer, offset + 352);
        StartupFile = EndianUtilities.<ForkData> toStruct(ForkData.class, buffer, offset + 432);
        return 512;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
