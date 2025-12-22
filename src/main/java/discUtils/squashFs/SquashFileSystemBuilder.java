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

package discUtils.squashFs;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Random;

import discUtils.core.UnixFilePermissions;
import discUtils.core.compression.ZlibStream;
import discUtils.core.internal.LocalFileLocator;
import discUtils.core.internal.Utilities;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Sizes;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileNotFoundException;
import dotnet4j.io.FileOptions;
import dotnet4j.io.FileShare;
import dotnet4j.io.FileStream;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import dotnet4j.io.compression.CompressionMode;
import dotnet4j.util.compat.StringUtilities;

import static java.lang.System.getLogger;


/**
 * Class that creates squashFs file systems.
 */
public final class SquashFileSystemBuilder {

    private static final Logger logger = getLogger(SquashFileSystemBuilder.class.getName());

    private static final int DefaultBlockSize = 131072;

    private BuilderContext context;

    private int nextInode;

    private BuilderDirectory rootDir;

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

    private EnumSet<UnixFilePermissions> defaultDirectoryPermissions;

    /**
     * Gets or sets the default permissions used for new directories.
     */
    public EnumSet<UnixFilePermissions> getDefaultDirectoryPermissions() {
        return defaultDirectoryPermissions;
    }

    public void setDefaultDirectoryPermissions(EnumSet<UnixFilePermissions> value) {
        defaultDirectoryPermissions = value;
    }

    private EnumSet<UnixFilePermissions> defaultFilePermissions;

    /**
     * Gets or sets the default permissions used for new files.
     */
    public EnumSet<UnixFilePermissions> getDefaultFilePermissions() {
        return defaultFilePermissions;
    }

    public void setDefaultFilePermissions(EnumSet<UnixFilePermissions> value) {
        defaultFilePermissions = value;
    }

    private int defaultGroup;

    /**
     * Gets the default group id used for new files and directories.
     */
    public int getDefaultGroup() {
        return defaultGroup;
    }

    /**
     * Sets the default group id used for new files and directories.
     */
    public void setDefaultGroup(int value) {
        defaultGroup = value;
    }

    /**
     * Gets or sets the default user id used for new files and directories.
     */
    private int defaultUser;

    public int getDefaultUser() {
        return defaultUser;
    }

    public void setDefaultUser(int value) {
        defaultUser = value;
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

    private static final Random random = new Random();

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
                    logger.log(Level.DEBUG, e.getMessage(), e);
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

        context = new BuilderContext();
        context.setRawStream(output);
        context.setDataBlockSize(DefaultBlockSize);
        context.setIoBuffer(new byte[DefaultBlockSize]);

        MetablockWriter inodeWriter = new MetablockWriter();
        MetablockWriter dirWriter = new MetablockWriter();
        FragmentWriter fragWriter = new FragmentWriter(context);
        IdTableWriter idWriter = new IdTableWriter(context);

        context.setAllocateInode(this::allocateInode);
        context.setAllocateId(idWriter::allocateId);
        context.setWriteDataBlock(this::writeDataBlock);
        context.setWriteFragment(fragWriter::writeFragment);
        context.setInodeWriter(inodeWriter);
        context.setDirectoryWriter(dirWriter);

        nextInode = 1;

        SuperBlock superBlock = new SuperBlock();
        superBlock.magic = SuperBlock.SquashFsMagic;
        superBlock.creationTime = System.currentTimeMillis();
        superBlock.blockSize = context.getDataBlockSize();
        superBlock.compression = 1; // DEFLATE
        superBlock.blockSizeLog2 = (short) MathUtilities.log2(superBlock.blockSize);
        superBlock.majorVersion = 4;
        superBlock.minorVersion = 0;

        output.position(superBlock.size());

        getRoot().reset();
        getRoot().write(context);
        fragWriter.flush();
        superBlock.rootInode = getRoot().getInodeRef();
        superBlock.inodesCount = nextInode - 1;
        superBlock.fragmentsCount = fragWriter.getFragmentCount();
        superBlock.setUidGidCount((short) idWriter.getIdCount());

        superBlock.inodeTableStart = output.position();
        inodeWriter.persist(output);

        superBlock.directoryTableStart = output.position();
        dirWriter.persist(output);

        superBlock.fragmentTableStart = fragWriter.persist();
        superBlock.lookupTableStart = -1;
        superBlock.uidGidTableStart = idWriter.persist();
        superBlock.extendedAttrsTableStart = -1;
        superBlock.bytesUsed = output.position();

        // Pad to 4KB
        long end = MathUtilities.roundUp(output.position(), 4 * Sizes.OneKiB);
        if (end != output.position()) {
            byte[] padding = new byte[(int) (end - output.position())];
            output.write(padding, 0, padding.length);
        }

        // Go back and write the superblock
        output.position(0);
        byte[] buffer = new byte[superBlock.size()];
        superBlock.writeTo(buffer, 0);
        output.write(buffer, 0, buffer.length);
        output.position(end);
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
        return nextInode++;
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
        context.getRawStream().write(writeData, writeOffset, writeLen & 0xFFFFFF);
        return writeLen;
    }

    /**
     * Delayed root construction, to permit default permissions / identity info
     * to be set before root is created.
     *
     * @return The root directory.
     */
    private BuilderDirectory getRoot() {
        if (rootDir == null) {
            rootDir = new BuilderDirectory();
            rootDir.setMode(getDefaultDirectoryPermissions());
        }

        return rootDir;
    }

    private BuilderDirectory createDirectory(String path, int user, int group, EnumSet<UnixFilePermissions> permissions) {
        BuilderDirectory currentDir = getRoot();
        String[] elems = Arrays.stream(path.split(StringUtilities.escapeForRegex(File.separator)))
                .filter(e -> !e.isEmpty())
                .toArray(String[]::new);

        for (int i = 0; i < elems.length; ++i) {
            BuilderNode nextDirAsNode = currentDir.getChild(elems[i]);
            BuilderDirectory nextDir = nextDirAsNode instanceof BuilderDirectory ? (BuilderDirectory) nextDirAsNode
                                                                                 : null;
            if (nextDirAsNode == null) {
                nextDir = new BuilderDirectory();
                nextDir.setUserId(user);
                nextDir.setGroupId(group);
                nextDir.setMode(permissions);
                nextDir.setModificationTime(System.currentTimeMillis());

                currentDir.addChild(elems[i], nextDir);
            } else if (nextDir == null) {
                throw new FileNotFoundException("Found " + nextDirAsNode.getInode().type + ", expecting Directory " +
                                                String.join(File.separator, Arrays.copyOfRange(elems, 0, i + 1)));
            }

            currentDir = nextDir;
        }
        return currentDir;
    }
}
