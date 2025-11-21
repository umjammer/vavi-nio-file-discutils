
package externalFileSystem;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;

import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.vfs.VfsDirEntry;

class MyDirEntry extends VfsDirEntry {

    private static long nextId;

    private final boolean isDir;

    private final String name;

    private final long id;

    public MyDirEntry(String name, boolean isDir) {
        this.name = name;
        this.isDir = isDir;
        id = nextId++;
    }

    @Override public boolean isDirectory() {
        return isDir;
    }

    @Override public boolean isSymlink() {
        return false;
    }

    @Override public String getFileName() {
        return name;
    }

    @Override public boolean hasVfsTimeInfo() {
        return true;
    }

    @Override public long getLastAccessTimeUtc() {
        return ZonedDateTime.of(1980, 10, 21, 11, 4, 22, 0, ZoneId.of("UTC")).toInstant().toEpochMilli();
    }

    @Override public long getLastWriteTimeUtc() {
        return ZonedDateTime.of(1980, 10, 21, 11, 4, 22, 0, ZoneId.of("UTC")).toInstant().toEpochMilli();
    }

    @Override public long getCreationTimeUtc() {
        return ZonedDateTime.of(1980, 10, 21, 11, 4, 22, 0, ZoneId.of("UTC")).toInstant().toEpochMilli();
    }

    @Override public boolean hasVfsFileAttributes() {
        return true;
    }

    @Override public EnumSet<FileAttributes> getFileAttributes() {
        return EnumSet.of(isDirectory() ? FileAttributes.Directory : FileAttributes.Normal);
    }

    @Override public long getUniqueCacheId() {
        return id;
    }
}
