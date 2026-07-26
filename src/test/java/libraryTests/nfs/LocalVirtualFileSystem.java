/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libraryTests.nfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.security.auth.Subject;

import org.dcache.nfs.status.ExistException;
import org.dcache.nfs.status.NoEntException;
import org.dcache.nfs.status.NotDirException;
import org.dcache.nfs.v4.NfsIdMapping;
import org.dcache.nfs.v4.SimpleIdMap;
import org.dcache.nfs.v4.xdr.nfsace4;
import org.dcache.nfs.vfs.AclCheckable;
import org.dcache.nfs.vfs.DirectoryEntry;
import org.dcache.nfs.vfs.DirectoryStream;
import org.dcache.nfs.vfs.FsStat;
import org.dcache.nfs.vfs.Inode;
import org.dcache.nfs.vfs.Stat;
import org.dcache.nfs.vfs.VirtualFileSystem;


/**
 * An nfs4j {@link VirtualFileSystem} serving a directory of the local file
 * system through {@code java.nio.file}, enough of one to exercise an NFS v3
 * client.
 * <p>
 * Inodes are handed out as small opaque ids, remembered in a map, so no
 * assumption is made about what the local file system can encode in a file
 * handle. That also means handles only stay valid for the lifetime of an
 * instance, which is all a test needs.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @see NfsTestServer
 */
class LocalVirtualFileSystem implements VirtualFileSystem {

    /**
     * Everything is reported as world writable, and owned by root.
     * <p>
     * nfs4j decides on its own whether a request may proceed, from these
     * permissions and the uid the client sends, and the local permissions of a
     * temporary directory say nothing about the client of a test. So this
     * export lets everybody do everything, and the local file system remains
     * the only real guard.
     */
    private static final int PERMISSIONS = 0777;

    private final Path root;

    private final NfsIdMapping idMapping = new SimpleIdMap();

    /** inode id to path, and back */
    private final Map<Long, Path> paths = new HashMap<>();

    private final Map<Path, Long> ids = new HashMap<>();

    private final AtomicLong nextId = new AtomicLong(1);

    LocalVirtualFileSystem(Path root) {
        this.root = root.toAbsolutePath().normalize();
        idOf(this.root);
    }

    private synchronized long idOf(Path path) {
        return ids.computeIfAbsent(path, p -> {
            long id = nextId.getAndIncrement();
            paths.put(id, p);
            return id;
        });
    }

    private synchronized Path pathOf(Inode inode) throws IOException {
        long id = ByteBuffer.wrap(inode.getFileId()).getLong();
        Path path = paths.get(id);
        if (path == null) {
            throw new NoEntException("stale inode: " + id);
        }
        return path;
    }

    private Inode inodeOf(Path path) {
        return Inode.forFile(ByteBuffer.allocate(Long.BYTES).putLong(idOf(path)).array());
    }

    /** rejects anything that would leave the exported tree */
    private Path resolve(Inode parent, String name) throws IOException {
        Path path = pathOf(parent).resolve(name).normalize();
        if (!path.startsWith(root)) {
            throw new NoEntException("outside of the export: " + name);
        }
        return path;
    }

    @Override
    public Inode getRootInode() {
        return inodeOf(root);
    }

    @Override
    public Inode lookup(Inode parent, String name) throws IOException {
        Path path = resolve(parent, name);
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new NoEntException(name);
        }
        return inodeOf(path);
    }

    @Override
    public Inode parentOf(Inode inode) throws IOException {
        Path parent = pathOf(inode).getParent();
        if (parent == null || !pathOf(inode).startsWith(root) || pathOf(inode).equals(root)) {
            return getRootInode();
        }
        return inodeOf(parent);
    }

    @Override
    public Inode create(Inode parent, Stat.Type type, String name, Subject subject, int mode) throws IOException {
        Path path = resolve(parent, name);
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new ExistException(name);
        }
        if (type != Stat.Type.REGULAR) {
            throw new UnsupportedOperationException("create " + type);
        }
        Files.createFile(path);
        return inodeOf(path);
    }

    @Override
    public Inode mkdir(Inode parent, String name, Subject subject, int mode) throws IOException {
        Path path = resolve(parent, name);
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new ExistException(name);
        }
        Files.createDirectory(path);
        return inodeOf(path);
    }

    @Override
    public Inode symlink(Inode parent, String name, String link, Subject subject, int mode) throws IOException {
        Path path = resolve(parent, name);
        Files.createSymbolicLink(path, path.getFileSystem().getPath(link));
        return inodeOf(path);
    }

    @Override
    public String readlink(Inode inode) throws IOException {
        return Files.readSymbolicLink(pathOf(inode)).toString();
    }

    @Override
    public Inode link(Inode parent, Inode existing, String name, Subject subject) throws IOException {
        Path path = resolve(parent, name);
        Files.createLink(path, pathOf(existing));
        return inodeOf(path);
    }

    @Override
    public void remove(Inode parent, String name) throws IOException {
        Path path = resolve(parent, name);
        try {
            Files.delete(path);
        } catch (NoSuchFileException e) {
            throw new NoEntException(name);
        }
        forget(path);
    }

    @Override
    public boolean move(Inode source, String oldName, Inode destination, String newName) throws IOException {
        Path from = resolve(source, oldName);
        Path to = resolve(destination, newName);
        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        forget(from);
        return true;
    }

    private synchronized void forget(Path path) {
        Long id = ids.remove(path);
        if (id != null) {
            paths.remove(id);
        }
    }

    @Override
    public DirectoryStream list(Inode inode, byte[] verifier, long cookie) throws IOException {
        Path directory = pathOf(inode);
        if (!Files.isDirectory(directory)) {
            throw new NotDirException(directory.toString());
        }

        List<DirectoryEntry> entries = new ArrayList<>();
        try (java.nio.file.DirectoryStream<Path> children = Files.newDirectoryStream(directory)) {
            long next = 2;
            for (Path child : children) {
                Inode childInode = inodeOf(child);
                entries.add(new DirectoryEntry(child.getFileName().toString(), childInode, statOf(child), next++));
            }
        } catch (DirectoryIteratorException e) {
            throw e.getCause();
        }
        return new DirectoryStream(entries);
    }

    @Override
    public byte[] directoryVerifier(Inode inode) throws IOException {
        return DirectoryStream.ZERO_VERIFIER;
    }

    @Override
    public int read(Inode inode, byte[] data, long offset, int count) throws IOException {
        try (FileChannel channel = FileChannel.open(pathOf(inode), StandardOpenOption.READ)) {
            return channel.read(ByteBuffer.wrap(data, 0, count), offset);
        }
    }

    @Override
    public WriteResult write(Inode inode, byte[] data, long offset, int count, StabilityLevel stabilityLevel)
            throws IOException {
        try (FileChannel channel = FileChannel.open(pathOf(inode), StandardOpenOption.WRITE)) {
            int written = channel.write(ByteBuffer.wrap(data, 0, count), offset);
            channel.force(true);
            return new WriteResult(StabilityLevel.FILE_SYNC, written);
        }
    }

    @Override
    public void commit(Inode inode, long offset, int count) {
        // write() is always FILE_SYNC
    }

    @Override
    public Stat getattr(Inode inode) throws IOException {
        return statOf(pathOf(inode));
    }

    private Stat statOf(Path path) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

        Stat stat = new Stat();
        stat.setIno(idOf(path));
        stat.setDev(1);
        stat.setRdev(0);
        stat.setGeneration(attributes.lastModifiedTime().toMillis());
        stat.setNlink(1);
        stat.setUid(0);
        stat.setGid(0);
        stat.setSize(attributes.isDirectory() ? 512 : attributes.size());
        stat.setATime(attributes.lastAccessTime().toMillis());
        stat.setMTime(attributes.lastModifiedTime().toMillis());
        stat.setCTime(attributes.lastModifiedTime().toMillis());
        stat.setBTime(attributes.creationTime().toMillis());
        stat.setMode(typeOf(attributes) | PERMISSIONS);
        return stat;
    }

    private static int typeOf(BasicFileAttributes attributes) {
        if (attributes.isDirectory()) {
            return Stat.S_IFDIR;
        }
        if (attributes.isSymbolicLink()) {
            return Stat.S_IFLNK;
        }
        return Stat.S_IFREG;
    }

    @Override
    public void setattr(Inode inode, Stat stat) throws IOException {
        Path path = pathOf(inode);
        if (stat.isDefined(Stat.StatAttribute.SIZE)) {
            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
                channel.truncate(stat.getSize());
            }
        }
        if (stat.isDefined(Stat.StatAttribute.MTIME)) {
            Files.setLastModifiedTime(path, java.nio.file.attribute.FileTime.fromMillis(stat.getMTime()));
        }
    }

    @Override
    public int access(Subject subject, Inode inode, int mode) throws IOException {
        // everything the client asks for is granted, this is a test export
        pathOf(inode);
        return mode;
    }

    @Override
    public FsStat getFsStat() throws IOException {
        FileStore store = Files.getFileStore(root);
        long total = store.getTotalSpace();
        return new FsStat(total, Long.MAX_VALUE, total - store.getUsableSpace(), paths.size());
    }

    @Override
    public nfsace4[] getAcl(Inode inode) {
        return new nfsace4[0];
    }

    @Override
    public void setAcl(Inode inode, nfsace4[] acl) {
    }

    @Override
    public AclCheckable getAclCheckable() {
        return AclCheckable.UNDEFINED_ALL;
    }

    @Override
    public NfsIdMapping getIdMapper() {
        return idMapping;
    }

    @Override
    public boolean hasIOLayout(Inode inode) {
        return false;
    }

    @Override
    public boolean getCaseInsensitive() {
        return false;
    }

    @Override
    public boolean getCasePreserving() {
        return true;
    }

    @Override
    public String toString() {
        return "nfs export of " + root;
    }
}
