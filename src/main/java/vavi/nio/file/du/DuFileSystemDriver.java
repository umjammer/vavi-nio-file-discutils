/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.du;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileStore;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.fge.filesystem.driver.ExtendedFileSystemDriver;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import discUtils.core.DiscDirectoryInfo;
import discUtils.core.DiscFileInfo;
import discUtils.core.DiscFileSystem;
import discUtils.core.DiscFileSystemInfo;
import discUtils.core.coreCompat.FileAttributes;
import dotnet4j.io.FileMode;
import dotnet4j.io.compat.StreamInputStream;
import dotnet4j.io.compat.StreamOutputStream;

import static vavi.nio.file.Util.toPathString;


/**
 * DuFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/11/17 umjammer initial version <br>
 */
public final class DuFileSystemDriver extends ExtendedFileSystemDriver<DiscFileSystemInfo> {

    private DiscFileSystem fileSystem;

    public DuFileSystemDriver(final FileStore fileStore,
            FileSystemFactoryProvider provider,
            DiscFileSystem fileSystem,
            Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.fileSystem = fileSystem;
        setEnv(env);
    }

    private static String toDuPathString(Path path) throws IOException {
        return toPathString(path).substring(1);
    }

    private static String toJavaPathString(String path) {
        return File.separator + path;
    }

    @Override
    protected String getFilenameString(DiscFileSystemInfo entry) {
        return toJavaPathString(entry.getFullName());
    }

    @Override
    protected boolean isFolder(DiscFileSystemInfo entry) {
        return entry instanceof DiscDirectoryInfo;
    }

    @Override
    protected boolean exists(DiscFileSystemInfo entry) throws IOException {
        return fileSystem.exists(entry.getFullName());
    }

    @Override
    protected DiscFileSystemInfo getEntry(Path path)throws IOException {
        String pathString = toDuPathString(path);
        DiscFileSystemInfo entry = fileSystem.getFileSystemInfo(pathString);
        if (entry.getAttributes().contains(FileAttributes.Directory)) {
            return fileSystem.getDirectoryInfo(pathString);
        } else {
            return fileSystem.getFileInfo(pathString);
        }
    }

    @Override
    protected InputStream downloadEntry(DiscFileSystemInfo entry, Path path, Set<? extends OpenOption> options) throws IOException {
        return new StreamInputStream(fileSystem.openFile(toDuPathString(path), FileMode.Open));
    }

    @Override
    protected OutputStream uploadEntry(DiscFileSystemInfo parentEntry, Path path, Set<? extends OpenOption> options) throws IOException {
        return new StreamOutputStream(fileSystem.openFile(toDuPathString(path), FileMode.OpenOrCreate));
    }

    @Override
    protected List<DiscFileSystemInfo> getDirectoryEntries(DiscFileSystemInfo dirEntry, Path dir) throws IOException {
        List<DiscDirectoryInfo> folders = ((DiscDirectoryInfo) dirEntry).getDirectories();
        List<DiscFileSystemInfo> list = new ArrayList<>(folders);
        List<DiscFileInfo> files = ((DiscDirectoryInfo) dirEntry).getFiles();
        list.addAll(files);
        return list;
    }

    @Override
    protected DiscFileSystemInfo createDirectoryEntry(DiscFileSystemInfo parentEntry, Path dir) throws IOException {
        // TODO: how to diagnose?
        fileSystem.createDirectory(dir.toString());
        return getEntry(dir);
    }

    @Override
    protected boolean hasChildren(DiscFileSystemInfo dirEntry, Path dir) throws IOException {
        return getDirectoryEntries(dirEntry, dir).size() > 0;
    }

    @Override
    protected void removeEntry(DiscFileSystemInfo entry, Path path) throws IOException {
        // TODO: unknown what happens when a move operation is performed
        // and the target already exists
        fileSystem.deleteFile(toDuPathString(path));
    }

    @Override
    protected DiscFileSystemInfo copyEntry(DiscFileSystemInfo sourceEntry, DiscFileSystemInfo targetParentEntry, Path source, Path target, Set<CopyOption> options) throws IOException {
        fileSystem.copyFile(toDuPathString(source), toDuPathString(target));
        return getEntry(target);
    }

    @Override
    protected DiscFileSystemInfo moveEntry(DiscFileSystemInfo sourceEntry, DiscFileSystemInfo targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        fileSystem.moveFile(toDuPathString(source), toDuPathString(target));
        if (targetIsParent) {
            return getEntry(target.resolve(source.getFileName()));
        } else {
            return getEntry(target);
        }
    }

    @Override
    protected DiscFileSystemInfo moveFolderEntry(DiscFileSystemInfo sourceEntry, DiscFileSystemInfo targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        // TODO java spec. allows empty folder
        throw new IsDirectoryException("source can not be a folder: " + source);
    }

    @Override
    protected DiscFileSystemInfo renameEntry(DiscFileSystemInfo sourceEntry, DiscFileSystemInfo targetParentEntry, Path source, Path target) throws IOException {
        fileSystem.moveFile(toDuPathString(source), toDuPathString(target));
        return getEntry(target);
    }

    @Override
    public void close() throws IOException {
        fileSystem.close();
    }
}
