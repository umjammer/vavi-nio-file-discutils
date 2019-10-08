/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.dus;

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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import com.google.api.client.util.IOUtils;
import com.google.api.services.drive.Drive.Files.Copy;

import vavi.nio.file.Cache;
import vavi.nio.file.Util;
import vavi.nio.file.Util.OutputStreamForUploading;
import vavi.util.Debug;

import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.Util.toPathString;


/**
 * DusFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class DusFileSystemDriver extends UnixLikeFileSystemDriverBase {

    private final DiskFileSystem session;
    private boolean ignoreAppleDouble = false;

    @SuppressWarnings("unchecked")
    public DusFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final DiskFileSystem session,
            final Map<String, ?> env) {
        super(fileStore, provider);
        this.session = session;
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault("ignoreAppleDouble", Boolean.FALSE);
//System.err.println("ignoreAppleDouble: " + ignoreAppleDouble);
    }

    /** */
    private Cache<DusEntry> cache = new Cache<DusEntry>() {
        /**
         * TODO when the parent is not cached
         * @see #ignoreAppleDouble
         * @throws NoSuchFileException must be thrown when the path is not found in this cache
         */
        public DusEntry getEntry(Path path) {
            if (cache.containsFile(path)) {
                return cache.getFile(path);
            } else {
                if (ignoreAppleDouble && path.getFileName() != null && Util.isAppleDouble(path)) {
                    throw new NoSuchFileException("ignore apple double file: " + path);
                }

                    String pathString = toPathString(path);
//Debug.println("path: " + pathString);
                    Search search = session._getFeature(Search.class);
                    DusEntry entry;
                    if (path.getNameCount() == 0) {
                        entry = new DefaultHomeFinderService(session).find();
                    } else {
                        DusEntry parentEntry = getEntry(path.getParent());
                        AttributedList<DusEntry> entries = search.search(parentEntry, new SearchFilter(toFilenameString(path)), new DisabledListProgressListener());
                        if (!entries.isEmpty()) {
Debug.println("entries: " + entries.size());
                            entry = entries.get(0);
                        } else {
                            if (cache.containsFile(path)) {
                                cache.removeEntry(path);
                            }
                            throw new NoSuchFileException(pathString);
                        }
                    }
                    cache.putFile(path, entry);
                    return entry;
            }
        }
    };

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) {
        final DusEntry entry = cache.getEntry(path);

        // TODO: metadata driver
        if (entry.isDirectory()) {
            throw new IsDirectoryException("path: " + path);
        }

            final Read read = session._getFeature(Read.class);
            return read.read(entry, new TransferStatus(), new DisabledConnectionCallback());
    }

    /**
     * fuse からだと
     * <ol>
     *  <li>create -> newByteChannel
     *  <li>flush -> n/a
     *  <li>lock -> n/a
     *  <li>release -> byteChannel.close
     * </ol>
     * と呼ばれる <br/>
     * 元のファイルが取れない... <br/>
     * 書き込みの指示もない...
     * <p>
     * nio.file からだと
     * <pre>
     *  newOutputStream -> write(2)
     * </pre>
     */
    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) {
        try {
            DusEntry entry = cache.getEntry(path);

            if (entry.isDirectory()) {
                throw new IsDirectoryException("path: " + path);
            } else {
                throw new FileAlreadyExistsException("path: " + path);
            }
        } catch (NoSuchFileException e) {
Debug.println("newOutputStream: " + e.getMessage());
//new Exception("*** DUMMY ***").printStackTrace();
        }

        return new OutputStreamForUploading() {
            @Override
            protected void upload(InputStream is) {
                    final Write<?> write = session._getFeature(Write.class);
                    TransferStatus status =  new TransferStatus();
                    DusEntry newEntry = new DusEntry(toPathString(path), EnumSet.of(DusEntry.Type.file));
                    StatusOutputStream<?> out = write.write(newEntry, status, new DisabledConnectionCallback());
                    IOUtils.copy(is, out);
                    cache.addEntry(path, newEntry);
            }
        };
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) {
        return Util.newDirectoryStream(getDirectoryEntries(dir));
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path,
                                              Set<? extends OpenOption> options,
                                              FileAttribute<?>... attrs) {
        if (options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND)) {
            return new Util.SeekableByteChannelForWriting(newOutputStream(path, options)) {
                @Override
                protected long getLeftOver() {
                    long leftover = 0;
                    if (options.contains(StandardOpenOption.APPEND)) {
                        DusEntry entry = cache.getEntry(path);
                        if (entry != null && entry.attributes().getSize() >= 0) {
                            leftover = entry.attributes().getSize();
                        }
                    }
                    return leftover;
                }

                @Override
                public void close() {
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
            DusEntry entry = cache.getEntry(path);
            if (entry.isDirectory()) {
                throw new IsDirectoryException(path.toString());
            }
            return new Util.SeekableByteChannelForReading(newInputStream(path, null)) {
                @Override
                protected long getSize() {
                    return entry.attributes().getSize();
                }
            };
        }
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) {
        DusEntry parentEntry = cache.getEntry(dir.getParent());

        // TODO: how to diagnose?
            final Directory<?> directory = session._getFeature(Directory.class);
            DusEntry preEntry = new DusEntry(parentEntry, toFilenameString(dir), EnumSet.of(DusEntry.Type.directory));
            DusEntry newEntry = directory.mkdir(preEntry, null, new TransferStatus());
            cache.addEntry(dir, newEntry);
    }

    @Override
    public void delete(final Path path) {
        removeEntry(path);
    }

    @Override
    public void copy(final Path source, final Path target, final Set<CopyOption> options) {
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
    public void move(final Path source, final Path target, final Set<CopyOption> options) {
        if (cache.existsEntry(target)) {
            if (cache.getEntry(target).isDirectory()) {
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
     * @throws IOException filesystem level error, or a plain I/O error
     *                     if you use this with javafs (jnr-fuse), you should throw {@link NoSuchFileException} when the file not found.
     * @see FileSystemProvider#checkAccess(Path, AccessMode...)
     */
    @Override
    public void checkAccess(final Path path, final AccessMode... modes) {
        final DusEntry entry = cache.getEntry(path);

        if (!entry.isFile()) {
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
    public void close() {
            session.close();
    }

    /**
     * @throws IOException you should throw {@link NoSuchFileException} when the file not found.
     */
    @Nonnull
    @Override
    public Object getPathMetadata(final Path path) {
        return cache.getEntry(path);
    }

    /** */
    private List<Path> getDirectoryEntries(Path dir) {
        final DusEntry entry = cache.getEntry(dir);

        if (!entry.isDirectory()) {
//System.err.println(entry.name + ", " + entry.id + ", " + entry.hashCode());
            throw new NotDirectoryException(dir.toString());
        }

        List<Path> list = null;
        if (cache.containsFolder(dir)) {
            list = cache.getFolder(dir);
        } else {
            list = new ArrayList<>();

                final ListService listService = session._getFeature(ListService.class);
                AttributedList<DusEntry> children = listService.list(entry, new DisabledListProgressListener());

                for (final DusEntry child : children) {
                    Path childPath = dir.resolve(child.getAbsolute());
                    list.add(childPath);
//System.err.println("child: " + childPath.toRealPath().toString());

                    cache.putFile(childPath, child);
                }
                cache.putFolder(dir, list);
        }

        return list;
    }

    /** */
    private void removeEntry(Path path) {
        DusEntry entry = cache.getEntry(path);
        if (entry.isDirectory()) {
            if (cache.getChildCount(path) > 0) {
                throw new DirectoryNotEmptyException(path.toString());
            }
        }

        // TODO: unknown what happens when a move operation is performed
        // and the target already exists
            final Delete delete = session._getFeature(Delete.class);
            delete.delete(Arrays.asList(entry), new DisabledConnectionCallback(), new Delete.DisabledCallback());

            cache.removeEntry(path);
    }

    /** */
    private void copyEntry(final Path source, final Path target) {
        DusEntry sourceEntry = cache.getEntry(source);
        DusEntry targetParentEntry = cache.getEntry(target.getParent());
        if (sourceEntry.isFile()) {
                final Copy copy = session._getFeature(Copy.class);
                DusEntry newEntry = copy.copy(sourceEntry, targetParentEntry, new TransferStatus(), new DisabledConnectionCallback());

                cache.addEntry(target, newEntry);
        } else if (sourceEntry.isDirectory()) {
            // TODO java spec. allows empty folder
            throw new IsDirectoryException("source can not be a folder: " + source);
        }
    }

    /**
     * @param targetIsParent if the target is folder
     */
    private void moveEntry(final Path source, final Path target, boolean targetIsParent) {
        DusEntry sourceEntry = cache.getEntry(source);
        DusEntry targetParentEntry = cache.getEntry(targetIsParent ? target : target.getParent());
        if (sourceEntry.isFile()) {
                DusEntry preEntry;
                if (targetIsParent) {
                    preEntry = new DusEntry(targetParentEntry, toFilenameString(source), EnumSet.of(DusEntry.Type.file));
                } else {
                    preEntry = new DusEntry(toPathString(target), EnumSet.of(DusEntry.Type.file));
                }
                final Move move = session._getFeature(Move.class);
                DusEntry patchedEntry = move.move(sourceEntry, preEntry, new TransferStatus(), new Delete.DisabledCallback(), new DisabledConnectionCallback());
                cache.removeEntry(source);
                if (targetIsParent) {
                    cache.addEntry(target.resolve(source.getFileName()), patchedEntry);
                } else {
                    cache.addEntry(target, patchedEntry);
                }
        } else if (sourceEntry.isDirectory()) {
            // TODO java spec. allows empty folder
            throw new IsDirectoryException("source can not be a folder: " + source);
        }
    }

    /** */
    private void renameEntry(final Path source, final Path target) {
        DusEntry sourceEntry = cache.getEntry(source);
//Debug.println(sourceEntry.id + ", " + sourceEntry.name);

            DusEntry preEntry = new DusEntry(toPathString(target), EnumSet.of(DusEntry.Type.file));
            final Move move = session._getFeature(Move.class);
            DusEntry patchedEntry = move.move(sourceEntry, preEntry, new TransferStatus(), new Delete.DisabledCallback(), new DisabledConnectionCallback());
            cache.removeEntry(source);
            cache.addEntry(target, patchedEntry);
    }
}
