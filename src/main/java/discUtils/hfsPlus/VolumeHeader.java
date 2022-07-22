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

package discUtils.hfsPlus;

import java.time.ZoneId;
import java.util.EnumSet;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public final class VolumeHeader implements IByteArraySerializable {

    public static final short HfsPlusSignature = 0x482b;

    public ForkData allocationFile;

    public EnumSet<VolumeAttributes> attributes/* = VolumeAttributes.None*/;

    public ForkData attributesFile;

    public long backupDate;

    public int blockSize;

    public ForkData catalogFile;

    public long checkedDate;

    public long createDate;

    public int dataClumpSize;

    public long encodingsBitmap;

    public ForkData extentsFile;

    public int fileCount;

    public int[] finderInfo;

    public int folderCount;

    public int freeBlocks;

    public int journalInfoBlock;

    public int lastMountedVersion;

    public long modifyDate;

    public int nextAllocation;

    public CatalogNodeId nextCatalogId;

    public int resourceClumpSize;

    public short signature;

    public ForkData startupFile;

    public int totalBlocks;

    public short version;

    public int writeCount;

    public boolean isValid() {
        return signature == HfsPlusSignature;
    }

    public int size() {
        return 512;
    }

    public int readFrom(byte[] buffer, int offset) {
        signature = EndianUtilities.toUInt16BigEndian(buffer, offset + 0);
        if (!isValid())
            return size();

        version = EndianUtilities.toUInt16BigEndian(buffer, offset + 2);
        attributes = VolumeAttributes.valueOf(EndianUtilities.toUInt32BigEndian(buffer, offset + 4));
        lastMountedVersion = EndianUtilities.toUInt32BigEndian(buffer, offset + 8);
        journalInfoBlock = EndianUtilities.toUInt32BigEndian(buffer, offset + 12);

        createDate = HfsPlusUtilities.readHFSPlusDate(ZoneId.systemDefault(), buffer, offset + 16);
        modifyDate = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 20);
        backupDate = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 24);
        checkedDate = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 28);

        fileCount = EndianUtilities.toUInt32BigEndian(buffer, offset + 32);
        folderCount = EndianUtilities.toUInt32BigEndian(buffer, offset + 36);

        blockSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 40);
        totalBlocks = EndianUtilities.toUInt32BigEndian(buffer, offset + 44);
        freeBlocks = EndianUtilities.toUInt32BigEndian(buffer, offset + 48);

        nextAllocation = EndianUtilities.toUInt32BigEndian(buffer, offset + 52);
        resourceClumpSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 56);
        dataClumpSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 60);
        nextCatalogId = new CatalogNodeId(EndianUtilities.toUInt32BigEndian(buffer, offset + 64));

        writeCount = EndianUtilities.toUInt32BigEndian(buffer, offset + 68);
        encodingsBitmap = EndianUtilities.toUInt64BigEndian(buffer, offset + 72);

        finderInfo = new int[8];
        for (int i = 0; i < 8; ++i) {
            finderInfo[i] = EndianUtilities.toUInt32BigEndian(buffer, offset + 80 + i * 4);
        }

        allocationFile = EndianUtilities.toStruct(ForkData.class, buffer, offset + 112);
        extentsFile = EndianUtilities.toStruct(ForkData.class, buffer, offset + 192);
        catalogFile = EndianUtilities.toStruct(ForkData.class, buffer, offset + 272);
        attributesFile = EndianUtilities.toStruct(ForkData.class, buffer, offset + 352);
        startupFile = EndianUtilities.toStruct(ForkData.class, buffer, offset + 432);

        return 512;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
