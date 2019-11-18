/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.du;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Cache;
import vavi.nio.file.Util;
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
@ParametersAreNonnullByDefault
public final class DuFileSystemDriver extends UnixLikeFileSystemDriverBase {

    private final DiscFileSystem fileSystem;

    private boolean ignoreAppleDouble = false;

    @SuppressWarnings("unchecked")
    public DuFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final DiscFileSystem fileSystem,
            final Map<String, ?> env) {
        super(fileStore, provider);
        this.fileSystem = fileSystem;
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault("ignoreAppleDouble", Boolean.FALSE);
//Debug.println("ignoreAppleDouble: " + ignoreAppleDouble);
    }

    /** */
    private boolean isDirectory(DiscFileSystemInfo file) {
        return DiscDirectoryInfo.class.isInstance(file);
    }

    /** */
    private boolean isFile(DiscFileSystemInfo file) {
        return DiscFileInfo.class.isInstance(file);
    }

    private String toDuPathString(Path path) throws IOException {
        return toPathString(path).replace(File.separator, "\\").substring(1);
    }

    private String toJavaPathString(String path) {
        return File.separator + path.replace("\\", File.separator);
    }

    /** */
    private Cache<DiscFileSystemInfo> cache = new Cache<DiscFileSystemInfo>() {
        /**
         * TODO when the parent is not cached
         * 
         * @see #ignoreAppleDouble
         * @throws NoSuchFileException must be thrown when the path is not found
         *             in this cache
         */
        public DiscFileSystemInfo getEntry(Path path) throws IOException {
            if (cache.containsFile(path)) {
                return cache.getFile(path);
            } else {
                if (ignoreAppleDouble && path.getFileName() != null && Util.isAppleDouble(path)) {
                    throw new NoSuchFileException("ignore apple double file: " + path);
                }

                String pathString = toDuPathString(path);
//Debug.println("path: " + pathString);
                DiscFileSystemInfo entry = fileSystem.getFileSystemInfo(pathString);
                if (entry.getAttributes().contains(FileAttributes.Directory)) {
                    entry = fileSystem.getDirectoryInfo(pathString);
                } else {
                    entry = fileSystem.getFileInfo(pathString);
                }
                cache.putFile(path, entry);
                return entry;
            }
        }
    };

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final DiscFileSystemInfo entry = cache.getEntry(path);

        // TODO: metadata driver
        if (isDirectory(entry)) {
            throw new IsDirectoryException("path: " + path);
        }

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
    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        try {
            DiscFileSystemInfo entry = cache.getEntry(path);

            if (isDirectory(entry)) {
                throw new IsDirectoryException("path: " + path);
            } else {
                throw new FileAlreadyExistsException("path: " + path);
            }
        } catch (NoSuchFileException e) {
Debug.println("newOutputStream: " + e.getMessage());
//new Exception("*** DUMMY ***").printStackTrace();
        }

        return new StreamOutputStream(fileSystem.openFile(toDuPathString(path), FileMode.OpenOrCreate));
// TODO               cache.addEntry(path, newEntry);
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        return Util.newDirectoryStream(getDirectoryEntries(dir));
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path,
                                              Set<? extends OpenOption> options,
                                              FileAttribute<?>... attrs) throws IOException {
        if (options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND)) {
            return new Util.SeekableByteChannelForWriting(newOutputStream(path, options)) {
                @Override
                protected long getLeftOver() throws IOException {
                    long leftover = 0;
                    if (options.contains(StandardOpenOption.APPEND)) {
                        DiscFileSystemInfo entry = cache.getEntry(path);
                        if (entry != null && DiscFileInfo.class.cast(entry).getLength() >= 0) {
                            leftover = DiscFileInfo.class.cast(entry).getLength();
                        }
                    }
                    return leftover;
                }

                @Override
                public void close() throws IOException {
                    System.out.println("SeekableByteChannelForWriting::close");
                    if (written == 0) {
                        // TODO no mean
                        System.out.println("SeekableByteChannelForWriting::close: scpecial: " + path);
                        java.io.File file = new java.io.File(toPathString(path));
                        FileInputStream fis = new FileInputStream(file);
                        FileChannel fc = fis.getChannel();
                        fc.transferTo(0, file.length(), this);
                        fis.close();
                    }
                    super.close();
                }
            };
        } else {
            DiscFileSystemInfo entry = cache.getEntry(path);
            if (isDirectory(entry)) {
                throw new IsDirectoryException(path.toString());
            }
            return new Util.SeekableByteChannelForReading(newInputStream(path, null)) {
                @Override
                protected long getSize() {
                    return DiscFileInfo.class.cast(entry).getLength();
                }
            };
        }
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        // TODO: how to diagnose?
        fileSystem.createDirectory(dir.toString());
        DiscFileSystemInfo newEntry = cache.getEntry(dir);
        cache.addEntry(dir, newEntry);
    }

    @Override
    public void delete(final Path path) throws IOException {
        removeEntry(path);
    }

    @Override
    public void copy(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        if (cache.existsEntry(target)) {
            if (options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
                removeEntry(target);
            } else {
                throw new FileAlreadyExistsException(target.toString());
            }
        }
        copyEntry(source, target);
    }

    @Override
    public void move(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        if (cache.existsEntry(target)) {
            if (isDirectory(cache.getEntry(target))) {
                if (options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
                    // replace the target
                    if (cache.getChildCount(target) > 0) {
                        throw new DirectoryNotEmptyException(target.toString());
                    } else {
                        removeEntry(target);
                        moveEntry(source, target, false);
                    }
                } else {
                    // move into the target
                    moveEntry(source, target, true);
                }
            } else {
                if (options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
                    removeEntry(target);
                    moveEntry(source, target, false);
                } else {
                    throw new FileAlreadyExistsException(target.toString());
                }
            }
        } else {
            if (source.getParent().equals(target.getParent())) {
                // rename
                renameEntry(source, target);
            } else {
                moveEntry(source, target, false);
            }
        }
    }

    /**
     * Check access modes for a path on this filesystem
     * <p>
     * If no modes are provided to check for, this simply checks for the
     * existence of the path.
     * </p>
     *
     * @param path the path to check
     * @param modes the modes to check for, if any
     * @throws IOException filesystem level error, or a plain I/O error if you
     *             use this with javafs (jnr-fuse), you should throw
     *             {@link NoSuchFileException} when the file not found.
     * @see FileSystemProvider#checkAccess(Path, AccessMode...)
     */
    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        final DiscFileSystemInfo entry = cache.getEntry(path);

        if (!isFile(entry)) {
            return;
        }

        // TODO: assumed; not a file == directory
        for (final AccessMode mode : modes) {
            if (mode == AccessMode.EXECUTE) {
                throw new AccessDeniedException(path.toString());
            }
        }
    }

    @Override
    public void close() throws IOException {
        fileSystem.close();
    }

    /**
     * @throws IOException you should throw {@link NoSuchFileException} when the
     *             file not found.
     */
    @Nonnull
    @Override
    public Object getPathMetadata(final Path path) throws IOException {
        return cache.getEntry(path);
    }

    /** */
    private List<Path> getDirectoryEntries(Path dir) throws IOException {
        final DiscFileSystemInfo entry = cache.getEntry(dir);

        if (!isDirectory(entry)) {
//Debug.println(entry.name + ", " + entry.id + ", " + entry.hashCode());
            throw new NotDirectoryException(dir.toString());
        }

        List<Path> list = null;
        if (cache.containsFolder(dir)) {
            list = cache.getFolder(dir);
        } else {
            list = new ArrayList<>();

            for (final DiscFileSystemInfo child : DiscDirectoryInfo.class.cast(entry).getDirectories()) {
                Path childPath = dir.resolve(toJavaPathString(child.getFullName()));
                list.add(childPath);
//Debug.println("child: " + childPath.toRealPath().toString());

                cache.putFile(childPath, child);
            }
            for (final DiscFileSystemInfo child : DiscDirectoryInfo.class.cast(entry).getFiles()) {
                Path childPath = dir.resolve(toJavaPathString(child.getFullName()));
                list.add(childPath);
//Debug.println("child: " + childPath.toRealPath().toString());

                cache.putFile(childPath, child);
            }
            cache.putFolder(dir, list);
        }

        return list;
    }

    /** */
    private void removeEntry(Path path) throws IOException {
        DiscFileSystemInfo entry = cache.getEntry(path);
        if (isDirectory(entry)) {
            if (cache.getChildCount(path) > 0) {
                throw new DirectoryNotEmptyException(path.toString());
            }
        }

        // TODO: unknown what happens when a move operation is performed
        // and the target already exists
        fileSystem.deleteFile(toDuPathString(path));
        cache.removeEntry(path);
    }

    /** */
    private void copyEntry(final Path source, final Path target) throws IOException {
        DiscFileSystemInfo sourceEntry = cache.getEntry(source);
        DiscFileSystemInfo targetParentEntry = cache.getEntry(target.getParent());
        if (isFile(sourceEntry)) {
            fileSystem.copyFile(toDuPathString(source), toDuPathString(target));
            cache.getEntry(target);
        } else if (isDirectory(sourceEntry)) {
            // TODO java spec. allows empty folder
            throw new IsDirectoryException("source can not be a folder: " + source);
        }
    }

    /**
     * @param targetIsParent if the target is folder
     */
    private void moveEntry(final Path source, final Path target, boolean targetIsParent) throws IOException {
        DiscFileSystemInfo sourceEntry = cache.getEntry(source);
        DiscFileSystemInfo targetParentEntry = cache.getEntry(targetIsParent ? target : target.getParent());
        if (isFile(sourceEntry)) {
            DiscFileSystemInfo preEntry;
            if (targetIsParent) {
                preEntry = targetParentEntry;
            } else {
                //preEntry = toPathString(target);
            }
            fileSystem.moveFile(toDuPathString(source), toDuPathString(target));
            cache.removeEntry(source);
            if (targetIsParent) {
                cache.getEntry(target.resolve(source.getFileName()));
            } else {
                cache.getEntry(target);
            }
        } else if (isDirectory(sourceEntry)) {
            // TODO java spec. allows empty folder
            throw new IsDirectoryException("source can not be a folder: " + source);
        }
    }

    /** */
    private void renameEntry(final Path source, final Path target) throws IOException {
        fileSystem.moveFile(toDuPathString(source), toDuPathString(target));
        cache.removeEntry(source);
        cache.getEntry(target);
    }
}
