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

import DiscUtils.Core.UnixFileSystemInfo;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public abstract class CommonCatalogFileInfo implements IByteArraySerializable {
    public long AccessTime;

    public long AttributeModifyTime;

    public long BackupTime;

    public long ContentModifyTime;

    public long CreateTime;

    public CatalogNodeId FileId;

    public UnixFileSystemInfo FileSystemInfo;

    public CatalogRecordType RecordType = CatalogRecordType.None;

    public int UnixSpecialField;

    public abstract int sizeOf();

    public int readFrom(byte[] buffer, int offset) {
        RecordType = CatalogRecordType.valueOf(EndianUtilities.toInt16BigEndian(buffer, offset + 0));
        FileId = new CatalogNodeId(EndianUtilities.toUInt32BigEndian(buffer, offset + 8));
        CreateTime = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 12);
        ContentModifyTime = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 16);
        AttributeModifyTime = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 20);
        AccessTime = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 24);
        BackupTime = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 28);
        int[] special = new int[1];
        FileSystemInfo = HfsPlusUtilities.readBsdInfo(buffer, offset + 32, special);
        UnixSpecialField = special[0];
        return 0;
    }

    public abstract void writeTo(byte[] buffer, int offset);
}
