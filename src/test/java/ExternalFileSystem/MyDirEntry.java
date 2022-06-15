
package ExternalFileSystem;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;

import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Core.Vfs.VfsDirEntry;

class MyDirEntry extends VfsDirEntry {
    private static long _nextId;

    private boolean _isDir;

    private String _name;

    private long _id;

    public MyDirEntry(String name, boolean isDir) {
        _name = name;
        _isDir = isDir;
        _id = _nextId++;
    }

    public boolean isDirectory() {
        return _isDir;
    }

    public boolean isSymlink() {
        return false;
    }

    public String getFileName() {
        return _name;
    }

    public boolean hasVfsTimeInfo() {
        return true;
    }

    public long getLastAccessTimeUtc() {
        return ZonedDateTime.of(1980, 10, 21, 11, 4, 22, 0, ZoneId.of("UTC")).toInstant().toEpochMilli();
    }

    public long getLastWriteTimeUtc() {
        return ZonedDateTime.of(1980, 10, 21, 11, 4, 22, 0, ZoneId.of("UTC")).toInstant().toEpochMilli();
    }

    public long getCreationTimeUtc() {
        return ZonedDateTime.of(1980, 10, 21, 11, 4, 22, 0, ZoneId.of("UTC")).toInstant().toEpochMilli();
    }

    public boolean hasVfsFileAttributes() {
        return true;
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        return EnumSet.of(isDirectory() ? FileAttributes.Directory : FileAttributes.Normal);
    }

    public long getUniqueCacheId() {
        return _id;
    }

}
