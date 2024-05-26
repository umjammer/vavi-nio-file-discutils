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

import java.io.IOException;

import discUtils.core.DiscFileSystemOptions;
import discUtils.core.IUnixFileSystem;
import discUtils.core.UnixFilePermissions;
import discUtils.core.UnixFileSystemInfo;
import discUtils.core.UnixFileType;
import discUtils.core.compression.ZlibStream;
import discUtils.core.vfs.VfsReadOnlyFileSystem;
import discUtils.streams.block.Block;
import discUtils.streams.block.BlockCache;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import dotnet4j.io.compression.CompressionMode;
import vavi.util.ByteUtil;


class VfsSquashFileSystemReader extends VfsReadOnlyFileSystem<DirectoryEntry, File, Directory, Context>
    implements IUnixFileSystem {

    public static final int MetadataBufferSize = 8 * 1024;

    private final BlockCache<Block> blockCache;

    private final Context context;

    private byte[] ioBuffer;

    private final BlockCache<Metablock> metablockCache;

    public VfsSquashFileSystemReader(Stream stream) {
        super(new DiscFileSystemOptions());

        context = new Context();
        context.setSuperBlock(new SuperBlock());
        context.setRawStream(stream);

        // Read superblock
        stream.position(0);
        byte[] buffer = StreamUtilities.readExact(stream, context.getSuperBlock().size());
        context.getSuperBlock().readFrom(buffer, 0);

        if (context.getSuperBlock().magic != SuperBlock.SquashFsMagic) {
            throw new dotnet4j.io.IOException("Invalid SquashFS filesystem - magic mismatch");
        }

        if (context.getSuperBlock().compression != 1) {
            throw new dotnet4j.io.IOException("Unsupported compression used");
        }

        if (context.getSuperBlock().extendedAttrsTableStart != -1) {
            throw new dotnet4j.io.IOException("Unsupported extended attributes present");
        }

        if (context.getSuperBlock().majorVersion != 4) {
            throw new dotnet4j.io.IOException("Unsupported file system version: " +
                context.getSuperBlock().majorVersion + "." + context.getSuperBlock().minorVersion);
        }

        // Create block caches, used to reduce the amount of I/O and decompression
        // activity.
        blockCache = new BlockCache<>(context.getSuperBlock().blockSize, 20);
        metablockCache = new BlockCache<>(MetadataBufferSize, 20);
        context.setReadBlock(this::readBlock);
        context.setReadMetaBlock(this::readMetaBlock);

        context.setInodeReader(new MetablockReader(context, context.getSuperBlock().inodeTableStart));
        context.setDirectoryReader(new MetablockReader(context, context.getSuperBlock().directoryTableStart));

        if (context.getSuperBlock().fragmentTableStart != -1) {
            context.setFragmentTableReaders(loadIndirectReaders(context.getSuperBlock().fragmentTableStart,
                                                                 context.getSuperBlock().fragmentsCount,
                                                                 FragmentRecord.RecordSize));
        }

        if (context.getSuperBlock().uidGidTableStart != -1) {
            context.setUidGidTableReaders(loadIndirectReaders(context.getSuperBlock().uidGidTableStart,
                                                               context.getSuperBlock().getUidGidCount(),
                                                               4));
        }

        // Bootstrap the root directory
        context.getInodeReader().setPosition(context.getSuperBlock().rootInode);
        DirectoryInode dirInode = (DirectoryInode) Inode.read(context.getInodeReader());
        setRootDirectory(new Directory(context, dirInode, context.getSuperBlock().rootInode));
    }

    @Override public String getFriendlyName() {
        return "squashFs";
    }

    @Override public String getVolumeLabel() {
        return "";
    }

    @Override public UnixFileSystemInfo getUnixFileInfo(String path) {
        File file = getFile(path);
        Inode inode = file.getInode();
        DeviceInode devInod = inode instanceof DeviceInode ? (DeviceInode) inode : null;

        UnixFileSystemInfo info = new UnixFileSystemInfo();
        info.setFileType(fileTypeFromInodeType(inode.type));
        info.setUserId(getId(inode.uidKey));
        info.setGroupId(getId(inode.gidKey));
        info.setPermissions(UnixFilePermissions.valueOf(inode.mode & 0xffff));
        info.setInode(inode.inodeNumber);
        info.setLinkCount(inode.numLinks);
        info.setDeviceId(devInod == null ? 0 : devInod.getDeviceId());

        return info;
    }

    /**
     * Size of the Filesystem in bytes
     */
    @Override public long getSize() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }

    /**
     * Used space of the Filesystem in bytes
     */
    @Override public long getUsedSpace() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }

    /**
     * Available space of the Filesystem in bytes
     */
    @Override public long getAvailableSpace() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }

    static UnixFileType fileTypeFromInodeType(InodeType inodeType) {
        return switch (inodeType) {
            case BlockDevice, ExtendedBlockDevice -> UnixFileType.Block;
            case CharacterDevice, ExtendedCharacterDevice -> UnixFileType.Character;
            case Directory, ExtendedDirectory -> UnixFileType.Directory;
            case Fifo, ExtendedFifo -> UnixFileType.Fifo;
            case File, ExtendedFile -> UnixFileType.Regular;
            case Socket, ExtendedSocket -> UnixFileType.Socket;
            case Symlink, ExtendedSymlink -> UnixFileType.Link;
            default -> throw new IllegalArgumentException("Unrecognized inode type: " + inodeType);
        };
    }

    @Override protected File convertDirEntryToFile(DirectoryEntry dirEntry) {
        MetadataRef inodeRef = dirEntry.getInodeReference();
        context.getInodeReader().setPosition(inodeRef);
        Inode inode = Inode.read(context.getInodeReader());

        if (dirEntry.isSymlink()) {
            return new Symlink(context, inode, inodeRef);
        }
        if (dirEntry.isDirectory()) {
            return new Directory(context, inode, inodeRef);
        }

        return new File(context, inode, inodeRef);
    }

    private MetablockReader[] loadIndirectReaders(long pos, int count, int recordSize) {
        context.getRawStream().position(pos);
        int numBlocks = MathUtilities.ceil(count * recordSize, MetadataBufferSize);

        byte[] tableBytes = StreamUtilities.readExact(context.getRawStream(), numBlocks * 8);
        MetablockReader[] result = new MetablockReader[numBlocks];
        for (int i = 0; i < numBlocks; ++i) {
            long block = ByteUtil.readLeLong(tableBytes, i * 8);
            result[i] = new MetablockReader(context, block);
        }

        return result;
    }

    private int getId(short idKey) {
        int recordsPerBlock = MetadataBufferSize / 4;
        int block = (idKey & 0xffff) / recordsPerBlock;
        int offset = (idKey & 0xffff) % recordsPerBlock;

        MetablockReader reader = context.getUidGidTableReaders()[block];
        reader.setPosition(0, offset * 4);
        return reader.readInt();
    }

    private Block readBlock(long pos, int diskLen) {
        Block block = blockCache.getBlock(pos, Block.class);
        if (block.getAvailable() >= 0) {
            return block;
        }

        Stream stream = context.getRawStream();
        stream.position(pos);

        int readLen = diskLen & 0x00FFFFFF;
        boolean isCompressed = (diskLen & 0x01000000) == 0;

        if (isCompressed) {
            if (ioBuffer == null || readLen > ioBuffer.length) {
                ioBuffer = new byte[readLen];
            }

            StreamUtilities.readExact(stream, ioBuffer, 0, readLen);

            try (ZlibStream zlibStream = new ZlibStream(new MemoryStream(ioBuffer, 0, readLen, false),
                                                        CompressionMode.Decompress,
                                                        true)) {
                block.setAvailable(StreamUtilities
                        .readMaximum(zlibStream, block.getData(), 0, context.getSuperBlock().blockSize));
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        } else {
            StreamUtilities.readExact(stream, block.getData(), 0, readLen);
            block.setAvailable(readLen);
        }

        return block;
    }

    private Metablock readMetaBlock(long pos) {
        Metablock block = metablockCache.getBlock(pos, Metablock.class);
        if (block.getAvailable() >= 0) {
            return block;
        }

        Stream stream = context.getRawStream();
        stream.position(pos);

        byte[] buffer = StreamUtilities.readExact(stream, 2);

        int readLen = ByteUtil.readLeShort(buffer, 0);
        boolean isCompressed = (readLen & 0x8000) == 0;
        readLen &= 0x7FFF;
        if (readLen == 0) {
            readLen = 0x8000;
        }

        block.setNextBlockStart(pos + readLen + 2);

        if (isCompressed) {
            if (ioBuffer == null || readLen > ioBuffer.length) {
                ioBuffer = new byte[readLen];
            }

            StreamUtilities.readExact(stream, ioBuffer, 0, readLen);

            try (ZlibStream zlibStream = new ZlibStream(new MemoryStream(ioBuffer, 0, readLen, false),
                                                        CompressionMode.Decompress,
                                                        true)) {
                block.setAvailable(StreamUtilities.readMaximum(zlibStream, block.getData(), 0, MetadataBufferSize));
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        } else {
            block.setAvailable(StreamUtilities.readMaximum(stream, block.getData(), 0, readLen));
        }

        return block;
    }
}
