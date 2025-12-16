/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.emu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import discUtils.core.DiscFileSystem;
import discUtils.core.DiscFileSystemOptions;
import discUtils.core.FileSystemParameters;
import discUtils.fat.DirectoryEntry;
import discUtils.fat.FatAttributes;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.Stream;
import vavi.emu.disk.FileEntry;
import vavi.emu.disk.FolderEntry;
import vavi.emu.disk.LogicalDisk;


/**
 * EmuFileSystem.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-12-16 nsano initial version <br>
 */
public class EmuFileSystem extends DiscFileSystem {

    private static final Logger logger = System.getLogger(EmuFileSystem.class.getName());

    private static final String FS = File.separator;

    private final LogicalDisk logicalDisk;

    public EmuFileSystem(Stream stream, LogicalDisk logicalDisk, Ownership ownership, FileSystemParameters parameters) {
        super(new DiscFileSystemOptions());
        this.logicalDisk = logicalDisk;
    }

    private static boolean isRootPath(String path) {
        return (path == null || path.isEmpty()) || path.equals(FS);
    }

    private FileEntry getEntry(String path) throws IOException {
        if (isRootPath(path)) {
            return logicalDisk.getRoot();
        }

        List<? extends FileEntry> current = logicalDisk.getRoot().entries();
        String[] pathElements = path.split(FS);
        for (int i = 0; i < pathElements.length; i++) {
            if (i == pathElements.length - 1) {
                String fileName = pathElements[i];
                return current.stream().filter(fe -> fe.getName().equals(fileName)).findFirst().orElseThrow(() -> new FileNotFoundException("No such file " + path));
            } else {
                String dirName = pathElements[i];
                FileEntry dir = current.stream().filter(fe -> fe.getName().equals(dirName)).findFirst().orElseThrow(() -> new FileNotFoundException("No such file " + path));
                if (!dir.isDirectory()) throw new FileNotFoundException("No such file " + path);
                current = ((FolderEntry) dir).entries();
            }
        }
        throw new FileNotFoundException("No such file " + path);
    }

    @Override
    public String getFriendlyName() {
        return logicalDisk.imageDescText();
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public void copyFile(String sourceFile, String destinationFile, boolean overwrite) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createDirectory(String path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteDirectory(String path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteFile(String path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean directoryExists(String path) throws IOException {
        return false;
    }

    @Override
    public boolean fileExists(String path) throws IOException {
        return false;
    }

    @Override
    public List<String> getDirectories(String path, String searchPattern, String searchOption) throws IOException {
logger.log(Level.TRACE, "path: " + path);
        if (isRootPath(path)) {
            return logicalDisk.getRoot().entries().stream().map(FileEntry::getName).toList();
        }
        return List.of();
    }

    @Override
    public List<String> getFiles(String path, String searchPattern, String searchOption) throws IOException {
logger.log(Level.TRACE, "path: " + path);
        return List.of();
    }

    @Override
    public List<String> getFileSystemEntries(String path) throws IOException {
logger.log(Level.TRACE, "path: " + path);
        return List.of();
    }

    @Override
    public List<String> getFileSystemEntries(String path, String searchPattern) throws IOException {
logger.log(Level.TRACE, "path: " + path);
        return List.of();
    }

    @Override
    public void moveDirectory(String sourceDirectoryName, String destinationDirectoryName) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveFile(String sourceName, String destinationName, boolean overwrite) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SparseStream openFile(String path, FileMode mode, FileAccess access) throws IOException {
        return null;
    }

    @Override
    public Map<String, Object> getAttributes(String path) throws IOException {
logger.log(Level.TRACE, "path: " + path);
        if (isRootPath(path)) {
            return FatAttributes.toMap(EnumSet.of(FatAttributes.Directory));
        }

        return Map.of();
    }

    @Override
    public void setAttributes(String path, Map<String, Object> newValue) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCreationTimeUtc(String path) throws IOException {
        return 0;
    }

    @Override
    public void setCreationTimeUtc(String path, long newTime) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLastAccessTimeUtc(String path) throws IOException {
        return 0;
    }

    @Override
    public void setLastAccessTimeUtc(String path, long newTime) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLastWriteTimeUtc(String path) throws IOException {
        return 0;
    }

    @Override
    public void setLastWriteTimeUtc(String path, long newTime) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getFileLength(String path) throws IOException {
        return 0;
    }

    @Override
    public long getSize() throws IOException {
        return 0;
    }

    @Override
    public long getUsedSpace() throws IOException {
        return 0;
    }

    @Override
    public long getAvailableSpace() throws IOException {
        return 0;
    }
}
