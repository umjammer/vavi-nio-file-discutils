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

import com.github.fge.filesystem.driver.CachedFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.util.Debug;

import static vavi.nio.file.Util.toPathString;

import DiscUtils.Core.DiscDirectoryInfo;
import DiscUtils.Core.DiscFileInfo;
import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.DiscFileSystemInfo;
import DiscUtils.Core.CoreCompat.FileAttributes;
import dotnet4j.io.FileMode;
import dotnet4j.io.compat.StreamInputStream;
import dotnet4j.io.compat.StreamOutputStream;


/**
 * DuFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/11/17 umjammer initial version <br>
 */
public final class DuFileSystemDriver extends CachedFileSystemDriverBase<DiscFileSystemInfo> {

    private DiscFileSystem fileSystem;

    public DuFileSystemDriver(final FileStore fileStore,
            FileSystemFactoryProvider provider,
            DiscFileSystem fileSystem,
            Map<String, ?> env) {
        super(fileStore, provider);
        this.fileSystem = fileSystem;
        setEnv(env);
    }

    private static String toDuPathString(Path path) throws IOException {
        return toPathString(path).replace(File.separator, "\\").substring(1);
    }

    private static String toJavaPathString(String path) {
        return File.separator + path.replace("\\", File.separator);
    }

    @Override
    protected String getFilenameString(DiscFileSystemInfo entry) throws IOException {
    	return toJavaPathString(entry.getFullName());
    }

    @Override
    protected boolean isFolder(DiscFileSystemInfo entry) {
        return DiscDirectoryInfo.class.isInstance(entry);
    }

    @Override
    protected DiscFileSystemInfo getRootEntry(Path root) throws IOException {
Debug.println("path: " + toDuPathString(root));
Debug.println("root: " + fileSystem.getDirectoryInfo(toDuPathString(root)));
    	return fileSystem.getDirectoryInfo(toDuPathString(root));
    }

    @Override
    protected DiscFileSystemInfo getEntry(DiscFileSystemInfo parentEntry, Path path)throws IOException {
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

    /**
     * fuse からだと
     * <ol>
     * <li>create -> newByteChannel
     * <li>flush -> n/a
     * <li>lock -> n/a
     * <li>release -> byteChannel.close
     * </ol>
     * と呼ばれる <br/>
     * 元のファイルが取れない... <br/>
     * 書き込みの指示もない...
     * <p>
     * nio.file からだと
     * 
     * <pre>
     * newOutputStream -> write(2)
     * </pre>
     */
    @Override
    protected OutputStream uploadEntry(DiscFileSystemInfo parentEntry, Path path, Set<? extends OpenOption> options) throws IOException {
        return new StreamOutputStream(fileSystem.openFile(toDuPathString(path), FileMode.OpenOrCreate));
        // TODO cache.addEntry(path, newEntry);
    }

    @Override
    protected DiscFileSystemInfo createDirectoryEntry(DiscFileSystemInfo parentEntry, Path dir) throws IOException {
        // TODO: how to diagnose?
        fileSystem.createDirectory(dir.toString());
        return getEntry(null, dir);
    }

    @Override
    public void close() throws IOException {
        fileSystem.close();
    }

    @Override
    protected List<DiscFileSystemInfo> getDirectoryEntries(DiscFileSystemInfo dirEntry, Path dir) throws IOException {
    	List<DiscFileSystemInfo> list = new ArrayList<>();
    	List<DiscDirectoryInfo> folders = DiscDirectoryInfo.class.cast(dirEntry).getDirectories();
    	list.addAll(folders);
    	List<DiscFileInfo> files = DiscDirectoryInfo.class.cast(dirEntry).getFiles();
    	list.addAll(files);
        return list;
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
        return getEntry(null, target);
    }

    @Override
    protected DiscFileSystemInfo moveEntry(DiscFileSystemInfo sourceEntry, DiscFileSystemInfo targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        fileSystem.moveFile(toDuPathString(source), toDuPathString(target));
        if (targetIsParent) {
        	return getEntry(null, target.resolve(source.getFileName()));
        } else {
            return getEntry(null, target);
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
        return getEntry(null, target);
    }
}
