/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.chd;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.nio.file.CopyOption;
import java.nio.file.FileStore;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.ExtendedFileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import vavi.nio.file.chd.ChdFileSystemProvider.Entity;

import static java.lang.System.getLogger;
import static java.util.function.Predicate.not;
import static vavi.nio.file.Util.isAppleDouble;


/**
 * ChdFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2025/09/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class ChdFileSystemDriver extends ExtendedFileSystemDriver<Entity> {

    private static final Logger logger = getLogger(ChdFileSystemDriver.class.getName());

    private final UmdIsoReader manager;

    /** */
    public ChdFileSystemDriver(FileStore fileStore,
                               FileSystemFactoryProvider provider,
                               UmdIsoReader manager,
                               Map<String, ?> env) throws IOException {
        super(fileStore, provider);

        this.manager = manager;
        setEnv(env);
    }

    @Override
    protected String getFilenameString(Entity entry) {
        return entry.name;
    }

    @Override
    protected boolean isFolder(Entity entry) throws IOException {
        return entry.isDirectory;
    }

    @Override
    protected boolean exists(Entity entry) throws IOException {
        return manager.hasFile(entry.name);
    }

    static String toPathString(Path path) throws IOException {
        String pathString = path.toString();
        if (pathString.charAt(0) == '/') return pathString.substring(1);
        return pathString;
    }

    /** */
    private Entity root;
    private List<Entity> rootDir;

    @Override
    protected Entity getEntry(Path path) throws IOException {
//logger.log(Level.TRACE, "path: " + path + ", " + path.getNameCount());
        if (ignoreAppleDouble && path.getFileName() != null && isAppleDouble(path)) {
            throw new NoSuchFileException("ignore apple double file: " + path);
        }

        // TODO
        //  just do
        //  manager.hasFile(toPathString(path));
        try {
            Entity entry = null;
            List<Entity> prevDir;
            if (root == null) {
                root = new Entity(manager, null, "", true);
            }
            if (rootDir == null) {
                rootDir = getRootEntries();
            }
            if (path.getNameCount() == 0) {
                entry = root;
            }
            prevDir = rootDir;
            for (int i = 0; i < path.getNameCount(); i++) {
                Path currentPath = path.getName(i);
                Path parentPath = path;
                for (int j = 0; j < path.getNameCount() - i; j++)
                    parentPath = parentPath.getParent();
//logger.log(Level.TRACE, i + ": " + parentPath + ", " + currentPath + ", " + path);
                if (i == 0) {
                    entry = prevDir.stream().filter(e -> e.name.equals(currentPath.toString())).findFirst().orElseThrow();
                    if (i != path.getNameCount() - 1) { // dir
                        prevDir = getDirectoryEntries(null, currentPath);
                    }
                } else if (i == path.getNameCount() - 1) { // leaf
                    entry = prevDir.stream().filter(e -> e.name.equals(currentPath.toString())).findFirst().orElseThrow();
                } else { // dir
                    prevDir = getDirectoryEntries(null, parentPath != null ? parentPath.resolve(currentPath) : currentPath);
                }
            }
//logger.log(Level.TRACE, "entry: " + entry + ", " + entry.exists());
            if (entry != null) {
                return entry;
            } else {
                throw new NoSuchFileException(path.toString());
            }
        } catch (NoSuchElementException e) {
            throw (NoSuchFileException) new NoSuchFileException(path.toString()).initCause(e);
        }
    }

    // gross
    private List<Entity> getRootEntries() throws IOException {
        return Arrays.stream(manager.listDirectory(""))
                .filter(not("."::equals))
                .filter(not("\u0001"::equals))
                .map(f -> new Entity(manager, null, f))
                .collect(Collectors.toList());
    }

    @Override
    protected InputStream downloadEntry(Entity entry, Path path, Set<? extends OpenOption> options) throws IOException {
        return new FilterInputStream(manager.getFile(entry.name)) {
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int r = in.read(b, off, len);
                return r == 0 ? -1 : r; // UmdIsoFile#read is not compliant with standard.
            }
        };
    }

    @Override
    protected OutputStream uploadEntry(Entity parentEntry, Path path, Set<? extends OpenOption> options) throws IOException {
        throw new UnsupportedOperationException("filesystem is readonly");
    }

    @Override
    protected List<Entity> getDirectoryEntries(Entity dirEntry, Path dir) throws IOException {
//logger.log(Level.TRACE, "dir: " + dir);
        List<Entity> result = Arrays.stream(manager.listDirectory(toPathString(dir)))
                .filter(not("."::equals))
                .filter(not("\u0001"::equals))
                .map(f -> new Entity(manager, dir, f))
                .collect(Collectors.toList());
//logger.log(Level.TRACE, "dir: " + dir + ", " + dirEntry + ", " + result);
        return result;
    }

    @Override
    protected Entity createDirectoryEntry(Entity parentEntry, Path dir) throws IOException {
        throw new UnsupportedOperationException("filesystem is readonly");
    }

    @Override
    protected boolean hasChildren(Entity dirEntry, Path dir) throws IOException {
        return !getDirectoryEntries(dirEntry, dir).isEmpty();
    }

    @Override
    protected void removeEntry(Entity entry, Path path) throws IOException {
        throw new UnsupportedOperationException("filesystem is readonly");
    }

    @Override
    protected Entity copyEntry(Entity sourceEntry, Entity targetParentEntry, Path source, Path target, Set<CopyOption> options) throws IOException {
        throw new UnsupportedOperationException("filesystem is readonly");
    }

    @Override
    protected Entity moveEntry(Entity sourceEntry, Entity targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        throw new UnsupportedOperationException("filesystem is readonly");
    }

    @Override
    protected Entity moveFolderEntry(Entity sourceEntry, Entity targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        throw new UnsupportedOperationException("filesystem is readonly");
    }

    @Override
    protected Entity renameEntry(Entity sourceEntry, Entity targetParentEntry, Path source, Path target) throws IOException {
        throw new UnsupportedOperationException("filesystem is readonly");
    }
}
