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
import moe.yo3explorer.dotnetio4j.DeflateStream.CompressionMode;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.FileNotFoundException;
import moe.yo3explorer.dotnetio4j.FileOptions;
import moe.yo3explorer.dotnetio4j.FileShare;
import moe.yo3explorer.dotnetio4j.FileStream;
import moe.yo3explorer.dotnetio4j.MemoryStream;
import moe.yo3explorer.dotnetio4j.Stream;


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
        setDefaultDirectoryPermissions(EnumSet.of(UnixFilePermissions.OwnerAll,
                                                  UnixFilePermissions.GroupRead,
                                                  UnixFilePermissions.GroupExecute,
                                                  UnixFilePermissions.OthersRead,
                                                  UnixFilePermissions.OthersExecute));
        setDefaultUser(0);
        setDefaultGroup(0);
    }

    /**
     * Gets or sets the default permissions used for new directories.
     */
    private EnumSet<UnixFilePermissions> __DefaultDirectoryPermissions;

    public EnumSet<UnixFilePermissions> getDefaultDirectoryPermissions() {
        return __DefaultDirectoryPermissions;
    }

    public void setDefaultDirectoryPermissions(EnumSet<UnixFilePermissions> value) {
        __DefaultDirectoryPermissions = value;
    }

    /**
     * Gets or sets the default permissions used for new files.
     */
    private EnumSet<UnixFilePermissions> __DefaultFilePermissions;

    public EnumSet<UnixFilePermissions> getDefaultFilePermissions() {
        return __DefaultFilePermissions;
    }

    public void setDefaultFilePermissions(EnumSet<UnixFilePermissions> value) {
        __DefaultFilePermissions = value;
    }

    /**
     * Gets or sets the default group id used for new files and directories.
     */
    private int __DefaultGroup;

    public int getDefaultGroup() {
        return __DefaultGroup;
    }

    public void setDefaultGroup(int value) {
        __DefaultGroup = value;
    }

    /**
     * Gets or sets the default user id used for new files and directories.
     */
    private int __DefaultUser;

    public int getDefaultUser() {
        return __DefaultUser;
    }

    public void setDefaultUser(int value) {
        __DefaultUser = value;
    }

    /**
     * Adds a file to the file system.
     *
     * @param path The full path to the file.
     * @param content The content of the file.The created file with have the
     *            default owner, group, permissions and the
     *            current time as it's modification time. Any missing parent
     *            directories will be
     *            created, with default owner, group and directory permissions.
     */
    public void addFile(String path, Stream content) {
        addFile(path, content, getDefaultUser(), getDefaultGroup(), getDefaultFilePermissions(), System.currentTimeMillis());
    }

    /**
     * Adds a file to the file system.
     *
     * @param path The full path to the file.
     * @param contentPath Local file system path to the file to add.The created
     *            file with have the default owner, group, permissions and the
     *            current time as it's modification time. Any missing parent
     *            directories will be
     *            created with default owner, group and directory permissions.
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
     * @param path The full path to the file.
     * @param content The content of the file.
     * @param user The owner of the file.
     * @param group The group of the file.
     * @param permissions The access permission of the file.
     * @param modificationTime The modification time of the file.Any missing
     *            parent directories will be created with the specified owner
     *            and group,
     *            default directory permissions and the current time as the
     *            modification time.
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
     * @param path The full path to the file.
     * @param contentPath Local file system path to the file to add.
     * @param user The owner of the file.
     * @param group The group of the file.
     * @param permissions The access permission of the file.
     * @param modificationTime The modification time of the file.Any missing
     *            parent directories will be created with the specified owner
     *            and group,
     *            default directory permissions and the current time as the
     *            modification time.
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
     * @param path The full path to the directory.The created directory with
     *            have the default owner, group, permissions and the
     *            current time as it's modification time. Any missing parent
     *            directories will be
     *            created with default owner, group and directory permissions.
     */
    public void addDirectory(String path) {
        addDirectory(path, getDefaultUser(), getDefaultGroup(), getDefaultDirectoryPermissions(), System.currentTimeMillis());
    }

    /**
     * Adds a directory to the file system.
     *
     * @param path The full path to the directory.
     * @param user The owner of the directory.
     * @param group The group of the directory.
     * @param permissions The access permission of the directory.
     * @param modificationTime The modification time of the directory.The
     *            created directory with have the default owner, group,
     *            permissions and the
     *            current time as it's modification time. Any missing parent
     *            directories will be
     *            created with the specified owner, group, and directory
     *            permissions. The current time
     *            will be used as the modification time.
     */
    public void addDirectory(String path, int user, int group, EnumSet<UnixFilePermissions> permissions, long modificationTime) {
        BuilderDirectory dir = new BuilderDirectory();
        dir.setUserId(user);
        dir.setGroupId(group);
        dir.setMode(permissions);
        dir.setModificationTime(modificationTime);
        BuilderDirectory parentDir = createDirectory(Utilities.getDirectoryFromPath(path), user, group, permissions);
        parentDir.addChild(Utilities.getFileFromPath(path), dir);
    }

    /**
     * Builds the file system, returning a new stream.
     *
     * @return The stream containing the file system.
     *         This method uses a temporary file to construct the file system,
     *         use of
     *         the
     *         {@code Build(Stream)}
     *         or
     *         {@code Build(string)}
     *         variant is recommended
     *         when the file system will be written to a file.
     */
    public Stream build() {
        Stream stream = new FileStream(getClass().getSimpleName() + new Random().nextInt(),
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
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
            }
        }
    }

    /**
     * Writes the file system to an existing stream.
     *
     * @param output The stream to write to.The
     *            {@code output}
     *            stream must support seeking and writing.
     */
    public void build(Stream output) {
        if (output == null) {
            throw new NullPointerException("output");
        }

        if (!output.canWrite()) {
            throw new IllegalArgumentException("Output stream must be writable");
        }

        if (!output.canSeek()) {
            throw new IllegalArgumentException("Output stream must support seeking");
        }

        _context = new BuilderContext();
        MetablockWriter inodeWriter = new MetablockWriter();
        MetablockWriter dirWriter = new MetablockWriter();
        FragmentWriter fragWriter = new FragmentWriter(_context);
        IdTableWriter idWriter = new IdTableWriter(_context);
        _context.setAllocateInode(this::allocateInode);
        _context.setAllocateId(id -> {
            return idWriter.allocateId(id);
        });
        _context.setWriteDataBlock(this::writeDataBlock);
        _context.setWriteFragment((length, offset) -> {
            return fragWriter.writeFragment(length, offset);
        });
        _context.setInodeWriter(inodeWriter);
        _context.setDirectoryWriter(dirWriter);
        _nextInode = 1;
        SuperBlock superBlock = new SuperBlock();
        superBlock.Magic = SuperBlock.SquashFsMagic;
        superBlock.CreationTime = System.currentTimeMillis();
        superBlock.BlockSize = _context.getDataBlockSize();
        superBlock.Compression = 1;
        // DEFLATE
        superBlock.BlockSizeLog2 = (short) MathUtilities.log2(superBlock.BlockSize);
        superBlock.MajorVersion = 4;
        superBlock.MinorVersion = 0;
        output.setPosition(superBlock.getSize());
        getRoot().reset();
        getRoot().write(_context);
        fragWriter.flush();
        superBlock.RootInode = getRoot().getInodeRef();
        superBlock.InodesCount = _nextInode - 1;
        superBlock.FragmentsCount = fragWriter.getFragmentCount();
        superBlock.UidGidCount = (short) idWriter.getIdCount();
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
        byte[] buffer = new byte[(int) superBlock.getSize()];
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
        Stream destStream = locator.open(outputFile, FileMode.Create, FileAccess.Write, FileShare.None);
        try {
            build(destStream);
        } finally {
            if (destStream != null)
                try {
                    destStream.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
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
     * @return
     *         The 'length' of the (possibly compressed) data written, including
     *         a flag indicating compression (or not).
     */
    private int writeDataBlock(byte[] buffer, int offset, int count) {
        MemoryStream compressed = new MemoryStream();
        ZlibStream compStream = new ZlibStream(compressed, CompressionMode.Compress, true);
        try {
            compStream.write(buffer, offset, count);
        } finally {
            if (compStream != null)
                try {
                    compStream.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
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
     * to be
     * set before root is created.
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
        String[] elems = path.split("\\");
        for (int i = 0; i < elems.length; ++i) {
            BuilderNode nextDirAsNode = currentDir.getChild(elems[i]);
            BuilderDirectory nextDir = nextDirAsNode instanceof BuilderDirectory ? (BuilderDirectory) nextDirAsNode
                                                                                 : (BuilderDirectory) null;
            if (nextDirAsNode == null) {
                nextDir = new BuilderDirectory();
                currentDir.addChild(elems[i], nextDir);
            } else if (nextDir == null) {
                throw new FileNotFoundException("Found " + nextDirAsNode.getInode().Type + ", expecting Directory " +
                                                String.join("\\", Arrays.copyOfRange(elems, 0, i + 1)));
            }

            currentDir = nextDir;
        }
        return currentDir;
    }
}
