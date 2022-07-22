
package externalFileSystem;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;

import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.vfs.VfsDirEntry;

class MyDirEntry extends VfsDirEntry {

    private static long nextId;

    private boolean isDir;

    private String name;

    private long id;

    public MyDirEntry(String name, boolean isDir) {
        this.name = name;
        this.isDir = isDir;
        id = nextId++;
    }

    public boolean isDirectory() {
        return isDir;
    }

    public boolean isSymlink() {
        return false;
    }

    public String getFileName() {
        return name;
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
        return id;
    }

}
