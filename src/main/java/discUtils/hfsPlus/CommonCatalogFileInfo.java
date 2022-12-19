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

import discUtils.core.UnixFileSystemInfo;
import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;
import vavi.util.ByteUtil;


abstract class CommonCatalogFileInfo implements IByteArraySerializable {

    public long accessTime;

    public long attributeModifyTime;

    public long backupTime;

    public long contentModifyTime;

    public long createTime;

    public CatalogNodeId fileId;

    public UnixFileSystemInfo fileSystemInfo;

    public CatalogRecordType recordType = CatalogRecordType.None;

    public int unixSpecialField;

    public abstract int size();

    public int readFrom(byte[] buffer, int offset) {
        recordType = CatalogRecordType.values()[ByteUtil.readBeShort(buffer, offset + 0)];
        fileId = new CatalogNodeId(ByteUtil.readBeInt(buffer, offset + 8));
        createTime = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 12);
        contentModifyTime = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 16);
        attributeModifyTime = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 20);
        accessTime = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 24);
        backupTime = HfsPlusUtilities.readHFSPlusDate(ZoneId.of("UTC"), buffer, offset + 28);

        int[] special = new int[1];
        fileSystemInfo = HfsPlusUtilities.readBsdInfo(buffer, offset + 32, special);
        unixSpecialField = special[0];

        return 0;
    }

    public abstract void writeTo(byte[] buffer, int offset);
}
