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

import java.util.EnumSet;

import discUtils.core.UnixFileType;
import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.vfs.VfsDirEntry;
import discUtils.streams.util.EndianUtilities;
import vavi.util.ByteUtil;


final class DirEntry extends VfsDirEntry {

    public DirEntry(String name, byte[] dirEntryData) {
        fileName = name;
        catalogFileInfo = parseDirEntryData(dirEntryData);
    }

    private CommonCatalogFileInfo catalogFileInfo;

    public CommonCatalogFileInfo getCatalogFileInfo() {
        return catalogFileInfo;
    }

    public long getCreationTimeUtc() {
        return getCatalogFileInfo().createTime;
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        return UnixFileType.toFileAttributes(getCatalogFileInfo().fileSystemInfo.getFileType());
    }

    private String fileName;

    public String getFileName() {
        return fileName;
    }

    public boolean hasVfsFileAttributes() {
        return true;
    }

    public boolean hasVfsTimeInfo() {
        return true;
    }

    public boolean isDirectory() {
        return getCatalogFileInfo().recordType == CatalogRecordType.FolderRecord;
    }

    public boolean isSymlink() {
        return !isDirectory() &&
               ((CatalogFileInfo) getCatalogFileInfo()).fileInfo.fileType == FileTypeFlags.SymLinkFileType.getValue();
    }

    public long getLastAccessTimeUtc() {
        return getCatalogFileInfo().accessTime;
    }

    public long getLastWriteTimeUtc() {
        return getCatalogFileInfo().contentModifyTime;
    }

    public CatalogNodeId getNodeId() {
        return getCatalogFileInfo().fileId;
    }

    public long getUniqueCacheId() {
        return getCatalogFileInfo().fileId.getId();
    }

    static boolean isFileOrDirectory(byte[] dirEntryData) {
        CatalogRecordType type = CatalogRecordType.values()[ByteUtil.readBeShort(dirEntryData, 0)];
        return type == CatalogRecordType.FolderRecord || type == CatalogRecordType.FileRecord;
    }

    private static CommonCatalogFileInfo parseDirEntryData(byte[] dirEntryData) {
        CatalogRecordType type = CatalogRecordType.values()[ByteUtil.readBeShort(dirEntryData, 0)];
        CommonCatalogFileInfo result = null;
        switch (type) {
        case FolderRecord:
            result = new CatalogDirInfo();
            break;
        case FileRecord:
            result = new CatalogFileInfo();
            break;
        default:
            throw new UnsupportedOperationException("Unknown catalog record type: " + type);

        }
        result.readFrom(dirEntryData, 0);
        return result;
    }
}
