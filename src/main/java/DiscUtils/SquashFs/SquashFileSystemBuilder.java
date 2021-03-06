//
// Copyright (c) 2008-2011, Kenneth Bell
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.
//

package DiscUtils.SquashFs;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Random;

import DiscUtils.Core.UnixFilePermissions;
import DiscUtils.Core.Compression.ZlibStream;
import DiscUtils.Core.Internal.LocalFileLocator;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Sizes;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileNotFoundException;
import dotnet4j.io.FileOptions;
import dotnet4j.io.FileShare;
import dotnet4j.io.FileStream;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import dotnet4j.io.compat.StringUtilities;
import dotnet4j.io.compression.CompressionMode;


/**
 * Class that creates SquashFs file systems.
 */
public final class SquashFileSystemBuilder {
    private static final int DefaultBlockSize = 131072;

    private BuilderContext _context;

    private int _nextInode;

    private BuilderDirectory _rootDir;

    /**
     * Initializes a new instance of the SquashFileSystemBuilder class.
     */
    public SquashFileSystemBuilder() {
        setDefaultFilePermissions(EnumSet.of(UnixFilePermissions.OwnerRead,
                                             UnixFilePermissions.OwnerWrite,
                                             UnixFilePermissions.GroupRead,
                                             UnixFilePermissions.GroupWrite));
        EnumSet<UnixFilePermissions> flags = UnixFilePermissions.OwnerAll;
        flags.addAll(EnumSet.of(UnixFilePermissions.GroupRead,
                                UnixFilePermissions.GroupExecute,
                                UnixFilePermissions.OthersRead,
                                UnixFilePermissions.OthersExecute));
        setDefaultDirectoryPermissions(flags);
        setDefaultUser(0);
        setDefaultGroup(0);
    }

    private EnumSet<UnixFilePermissions> _defaultDirectoryPermissions;

    /**
     * Gets or sets the default permissions used for new directories.
     */
    public EnumSet<UnixFilePermissions> getDefaultDirectoryPermissions() {
        return _defaultDirectoryPermissions;
    }

    public void setDefaultDirectoryPermissions(EnumSet<UnixFilePermissions> value) {
        _defaultDirectoryPermissions = value;
    }

    private EnumSet<UnixFilePermissions> _defaultFilePermissions;

    /**
     * Gets or sets the default permissions used for new files.
     */
    public EnumSet<UnixFilePermissions> getDefaultFilePermissions() {
        return _defaultFilePermissions;
    }

    public void setDefaultFilePermissions(EnumSet<UnixFilePermissions> value) {
        _defaultFilePermissions = value;
    }

    private int _defaultGroup;

    /**
     * Gets the default group id used for new files and directories.
     */
    public int getDefaultGroup() {
        return _defaultGroup;
    }

    /**
     * Sets the default group id used for new files and directories.
     */
    public void setDefaultGroup(int value) {
        _defaultGroup = value;
    }

    /**
     * Gets or sets the default user id used for new files and directories.
     */
    private int _defaultUser;

    public int getDefaultUser() {
        return _defaultUser;
    }

    public void setDefaultUser(int value) {
        _defaultUser = value;
    }

    /**
     * Adds a file to the file system.
     *
     * The created file with have the default owner, group, permissions and the
     * current time as it's modification time. Any missing parent directories
     * will be created, with default owner, group and directory permissions.
     *
     * @param path The full path to the file.
     * @param content The content of the file.
     */
    public void addFile(String path, Stream content) {
        addFile(path, content, getDefaultUser(), getDefaultGroup(), getDefaultFilePermissions(), System.currentTimeMillis());
    }

    /**
     * Adds a file to the file system.
     *
     * The created file with have the default owner, group, permissions and the
     * current time as it's modification time. Any missing parent directories
     * will be created with default owner, group and directory permissions.
     *
     * @param path The full path to the file.
     * @param contentPath Local file system path to the file to add.
     */
    public void addFile(String path, String contentPath) {
        addFile(path,
                contentPath,
                getDefaultUser(),
                getDefaultGroup(),
                getDefaultFilePermissions(),
                System.currentTimeMillis());
    }

    /**
     * Adds a file to the file system.
     *
     * Any missing parent directories will be created with the specified owner
     * and group, default directory permissions and the current time as the
     * modification time.
     *
     * @param path The full path to the file.
     * @param content The content of the file.
     * @param user The owner of the file.
     * @param group The group of the file.
     * @param permissions The access permission of the file.
     * @param modificationTime The modification time of the file.
     */
    public void addFile(String path,
                        Stream content,
                        int user,
                        int group,
                        EnumSet<UnixFilePermissions> permissions,
                        long modificationTime) {
        BuilderFile file = new BuilderFile(content);
        file.setUserId(user);
        file.setGroupId(group);
        file.setMode(permissions);
        file.setModificationTime(modificationTime);
        BuilderDirectory dirNode = createDirectory(Utilities.getDirectoryFromPath(path),
                                                   user,
                                                   group,
                                                   getDefaultDirectoryPermissions());
        dirNode.addChild(Utilities.getFileFromPath(path), file);
    }

    /**
     * Adds a file to the file system.
     *
     * Any missing parent directories will be created with the specified owner
     * and group, default directory permissions and the current time as the
     * modification time.
     *
     * @param path The full path to the file.
     * @param contentPath Local file system path to the file to add.
     * @param user The owner of the file.
     * @param group The group of the file.
     * @param permissions The access permission of the file.
     * @param modificationTime The modification time of the file.
     */
    public void addFile(String path,
                        String contentPath,
                        int user,
                        int group,
                        EnumSet<UnixFilePermissions> permissions,
                        long modificationTime) {
        BuilderFile file = new BuilderFile(contentPath);
        file.setUserId(user);
        file.setGroupId(group);
        file.setMode(permissions);
        file.setModificationTime(modificationTime);
        BuilderDirectory dirNode = createDirectory(Utilities.getDirectoryFromPath(path),
                                                   user,
                                                   group,
                                                   getDefaultDirectoryPermissions());
        dirNode.addChild(Utilities.getFileFromPath(path), file);
    }

    /**
     * Adds a directory to the file system.
     *
     * The created directory with have the default owner, group, permissions and
     * the current time as it's modification time. Any missing parent
     * directories will be created with default owner, group and directory
     * permissions.
     *
     * @param path The full path to the directory.
     */
    public void addDirectory(String path) {
        addDirectory(path, getDefaultUser(), getDefaultGroup(), getDefaultDirectoryPermissions(), System.currentTimeMillis());
    }

    /**
     * Adds a directory to the file system.
     *
     * The created directory with have the default owner, group, permissions and
     * the current time as it's modification time. Any missing parent
     * directories will be created with the specified owner, group, and
     * directory permissions. The current time will be used as the modification
     * time.
     *
     * @param path The full path to the directory.
     * @param user The owner of the directory.
     * @param group The group of the directory.
     * @param permissions The access permission of the directory.
     * @param modificationTime The modification time of the directory.
     */
    public void addDirectory(String path,
                             int user,
                             int group,
                             EnumSet<UnixFilePermissions> permissions,
                             long modificationTime) {
        BuilderDirectory dir = new BuilderDirectory();
        dir.setUserId(user);
        dir.setGroupId(group);
        dir.setMode(permissions);
        dir.setModificationTime(modificationTime);
        BuilderDirectory parentDir = createDirectory(Utilities.getDirectoryFromPath(path), user, group, permissions);
        parentDir.addChild(Utilities.getFileFromPath(path), dir);
    }

    private static Random random = new Random();

    /**
     * Builds the file system, returning a new stream.
     *
     * This method uses a temporary file to construct the file system, use of
     * the {@code Build(Stream)} or {@code Build(string)} variant is recommended
     * when the file system will be written to a file.
     *
     * @return The stream containing the file system.
     */
    public Stream build() {
        Stream stream = new FileStream(getClass().getSimpleName() + random.nextInt(),
                                       FileMode.CreateNew,
                                       FileAccess.ReadWrite,
                                       FileShare.None,
                                       1024 * 1024,
                                       FileOptions.DeleteOnClose);
        try {
            build(stream);

            Stream tempStream = stream;
            stream = null;
            return tempStream;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
            }
        }
    }

    /**
     * Writes the file system to an existing stream.
     *
     * The {@code output} stream must support seeking and writing.
     *
     * @param output The stream to write to.
     */
    public void build(Stream output) {
        if (output == null) {
            throw new NullPointerException("output");
        }

        if (!output.canWrite()) {
            throw new IllegalArgumentException("Output stream must be writable: " + output.getClass());
        }

        if (!output.canSeek()) {
            throw new IllegalArgumentException("Output stream must support seeking: " + output.getClass());
        }

        _context = new BuilderContext();
        _context.setRawStream(output);
        _context.setDataBlockSize(DefaultBlockSize);
        _context.setIoBuffer(new byte[DefaultBlockSize]);

        MetablockWriter inodeWriter = new MetablockWriter();
        MetablockWriter dirWriter = new MetablockWriter();
        FragmentWriter fragWriter = new FragmentWriter(_context);
        IdTableWriter idWriter = new IdTableWriter(_context);

        _context.setAllocateInode(this::allocateInode);
        _context.setAllocateId(idWriter::allocateId);
        _context.setWriteDataBlock(this::writeDataBlock);
        _context.setWriteFragment(fragWriter::writeFragment);
        _context.setInodeWriter(inodeWriter);
        _context.setDirectoryWriter(dirWriter);

        _nextInode = 1;

        SuperBlock superBlock = new SuperBlock();
        superBlock.Magic = SuperBlock.SquashFsMagic;
        superBlock.CreationTime = System.currentTimeMillis();
        superBlock.BlockSize = _context.getDataBlockSize();
        superBlock.Compression = 1; // DEFLATE
        superBlock.BlockSizeLog2 = (short) MathUtilities.log2(superBlock.BlockSize);
        superBlock.MajorVersion = 4;
        superBlock.MinorVersion = 0;

        output.setPosition(superBlock.size());

        getRoot().reset();
        getRoot().write(_context);
        fragWriter.flush();
        superBlock.RootInode = getRoot().getInodeRef();
        superBlock.InodesCount = _nextInode - 1;
        superBlock.FragmentsCount = fragWriter.getFragmentCount();
        superBlock.setUidGidCount((short) idWriter.getIdCount());

        superBlock.InodeTableStart = output.getPosition();
        inodeWriter.persist(output);

        superBlock.DirectoryTableStart = output.getPosition();
        dirWriter.persist(output);

        superBlock.FragmentTableStart = fragWriter.persist();
        superBlock.LookupTableStart = -1;
        superBlock.UidGidTableStart = idWriter.persist();
        superBlock.ExtendedAttrsTableStart = -1;
        superBlock.BytesUsed = output.getPosition();

        // Pad to 4KB
        long end = MathUtilities.roundUp(output.getPosition(), 4 * Sizes.OneKiB);
        if (end != output.getPosition()) {
            byte[] padding = new byte[(int) (end - output.getPosition())];
            output.write(padding, 0, padding.length);
        }

        // Go back and write the superblock
        output.setPosition(0);
        byte[] buffer = new byte[superBlock.size()];
        superBlock.writeTo(buffer, 0);
        output.write(buffer, 0, buffer.length);
        output.setPosition(end);
    }

    /**
     * Writes the stream contents to a file.
     *
     * @param outputFile The file to write to.
     */
    public void build(String outputFile) {
        LocalFileLocator locator = new LocalFileLocator("");
        try (Stream destStream = locator.open(outputFile, FileMode.Create, FileAccess.Write, FileShare.None)) {
            build(destStream);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Allocates a unique inode identifier.
     *
     * @return The inode identifier.
     */
    private int allocateInode() {
        return _nextInode++;
    }

    /**
     * Writes a block of file data, possibly compressing it.
     *
     * @param buffer The data to write.
     * @param offset Offset of the first byte to write.
     * @param count The number of bytes to write.
     * @return The 'length' of the (possibly compressed) data written, including
     *         a flag indicating compression (or not).
     */
    private int writeDataBlock(byte[] buffer, int offset, int count) {
        MemoryStream compressed = new MemoryStream();

        try (ZlibStream compStream = new ZlibStream(compressed, CompressionMode.Compress, true)) {
            compStream.write(buffer, offset, count);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
        byte[] writeData;
        int writeOffset;
        int writeLen;
        if (compressed.getLength() < count) {
            writeData = compressed.toArray();
            writeOffset = 0;
            writeLen = (int) compressed.getLength();
        } else {
            writeData = buffer;
            writeOffset = offset;
            writeLen = count | 0x01000000;
        }
        _context.getRawStream().write(writeData, writeOffset, writeLen & 0xFFFFFF);
        return writeLen;
    }

    /**
     * Delayed root construction, to permit default permissions / identity info
     * to be set before root is created.
     *
     * @return The root directory.
     */
    private BuilderDirectory getRoot() {
        if (_rootDir == null) {
            _rootDir = new BuilderDirectory();
            _rootDir.setMode(getDefaultDirectoryPermissions());
        }

        return _rootDir;
    }

    private BuilderDirectory createDirectory(String path, int user, int group, EnumSet<UnixFilePermissions> permissions) {
        BuilderDirectory currentDir = getRoot();
        String[] elems = Arrays.stream(path.split(StringUtilities.escapeForRegex("\\")))
                .filter(e -> !e.isEmpty())
                .toArray(String[]::new);

        for (int i = 0; i < elems.length; ++i) {
            BuilderNode nextDirAsNode = currentDir.getChild(elems[i]);
            BuilderDirectory nextDir = nextDirAsNode instanceof BuilderDirectory ? (BuilderDirectory) nextDirAsNode
                                                                                 : (BuilderDirectory) null;
            if (nextDirAsNode == null) {
                nextDir = new BuilderDirectory();
                nextDir.setUserId(user);
                nextDir.setGroupId(group);
                nextDir.setMode(permissions);
                nextDir.setModificationTime(System.currentTimeMillis());

                currentDir.addChild(elems[i], nextDir);
            } else if (nextDir == null) {
                throw new FileNotFoundException("Found " + nextDirAsNode.getInode()._type + ", expecting Directory " +
                                                String.join("\\", Arrays.copyOfRange(elems, 0, i + 1)));
            }

            currentDir = nextDir;
        }
        return currentDir;
    }
}
