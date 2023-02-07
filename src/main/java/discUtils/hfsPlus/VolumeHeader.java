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
import vavi.util.ByteUtil;


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

    @Override public int size() {
        return 512;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        signature = ByteUtil.readBeShort(buffer, offset + 0);
        if (!isValid())
            return size();

        version = ByteUtil.readBeShort(buffer, offset + 2);
        attributes = VolumeAttributes.valueOf(ByteUtil.readBeInt(buffer, offset + 4));
        lastMountedVersion = ByteUtil.readBeInt(buffer, offset + 8);
        journalInfoBlock = ByteUtil.readBeInt(buffer, offset + 12);

        createDate = HfsPlusUtilities.readHFSPlusDate(ZoneId.systemDefault(), buffer, offset + 16);
        modifyDate = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 20);
        backupDate = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 24);
        checkedDate = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 28);

        fileCount = ByteUtil.readBeInt(buffer, offset + 32);
        folderCount = ByteUtil.readBeInt(buffer, offset + 36);

        blockSize = ByteUtil.readBeInt(buffer, offset + 40);
        totalBlocks = ByteUtil.readBeInt(buffer, offset + 44);
        freeBlocks = ByteUtil.readBeInt(buffer, offset + 48);

        nextAllocation = ByteUtil.readBeInt(buffer, offset + 52);
        resourceClumpSize = ByteUtil.readBeInt(buffer, offset + 56);
        dataClumpSize = ByteUtil.readBeInt(buffer, offset + 60);
        nextCatalogId = new CatalogNodeId(ByteUtil.readBeInt(buffer, offset + 64));

        writeCount = ByteUtil.readBeInt(buffer, offset + 68);
        encodingsBitmap = ByteUtil.readBeLong(buffer, offset + 72);

        finderInfo = new int[8];
        for (int i = 0; i < 8; ++i) {
            finderInfo[i] = ByteUtil.readBeInt(buffer, offset + 80 + i * 4);
        }

        allocationFile = EndianUtilities.toStruct(ForkData.class, buffer, offset + 112);
        extentsFile = EndianUtilities.toStruct(ForkData.class, buffer, offset + 192);
        catalogFile = EndianUtilities.toStruct(ForkData.class, buffer, offset + 272);
        attributesFile = EndianUtilities.toStruct(ForkData.class, buffer, offset + 352);
        startupFile = EndianUtilities.toStruct(ForkData.class, buffer, offset + 432);

        return 512;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
