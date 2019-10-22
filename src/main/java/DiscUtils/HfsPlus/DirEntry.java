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

import java.util.EnumSet;

import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Core.Vfs.VfsDirEntry;
import DiscUtils.Streams.Util.EndianUtilities;


public final class DirEntry extends VfsDirEntry {
    public DirEntry(String name, byte[] dirEntryData) {
        __FileName = name;
        __CatalogFileInfo = parseDirEntryData(dirEntryData);
    }

    private CommonCatalogFileInfo __CatalogFileInfo;

    public CommonCatalogFileInfo getCatalogFileInfo() {
        return __CatalogFileInfo;
    }

    public long getCreationTimeUtc() {
        return getCatalogFileInfo().CreateTime;
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        return Utilities.fileAttributesFromUnixFileType(getCatalogFileInfo().FileSystemInfo.getFileType());
    }

    private String __FileName;

    public String getFileName() {
        return __FileName;
    }

    public boolean hasVfsFileAttributes() {
        return true;
    }

    public boolean hasVfsTimeInfo() {
        return true;
    }

    public boolean isDirectory() {
        return getCatalogFileInfo().RecordType == CatalogRecordType.FolderRecord;
    }

    public boolean isSymlink() {
        return !isDirectory() &&
               ((CatalogFileInfo) getCatalogFileInfo()).FileInfo.FileType == FileTypeFlags.SymLinkFileType.getValue();
    }

    public long getLastAccessTimeUtc() {
        return getCatalogFileInfo().AccessTime;
    }

    public long getLastWriteTimeUtc() {
        return getCatalogFileInfo().ContentModifyTime;
    }

    public CatalogNodeId getNodeId() {
        return getCatalogFileInfo().FileId;
    }

    public long getUniqueCacheId() {
        return getCatalogFileInfo().FileId.getId();
    }

    public static boolean isFileOrDirectory(byte[] dirEntryData) {
        CatalogRecordType type = CatalogRecordType.valueOf(EndianUtilities.toInt16BigEndian(dirEntryData, 0));
        return type == CatalogRecordType.FolderRecord || type == CatalogRecordType.FileRecord;
    }

    private static CommonCatalogFileInfo parseDirEntryData(byte[] dirEntryData) {
        CatalogRecordType type = CatalogRecordType.valueOf(EndianUtilities.toInt16BigEndian(dirEntryData, 0));
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
