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

import DiscUtils.Core.DiscFileSystemOptions;
import DiscUtils.Core.IUnixFileSystem;
import DiscUtils.Core.UnixFileSystemInfo;
import DiscUtils.Core.UnixFileType;
import DiscUtils.Core.Compression.ZlibStream;
import DiscUtils.Core.Vfs.VfsReadOnlyFileSystem;
import DiscUtils.Streams.Block.Block;
import DiscUtils.Streams.Block.BlockCache;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.CompressionMode;
import moe.yo3explorer.dotnetio4j.MemoryStream;
import moe.yo3explorer.dotnetio4j.Stream;


public class VfsSquashFileSystemReader extends VfsReadOnlyFileSystem<DirectoryEntry, File, Directory, Context> implements
                                       IUnixFileSystem {
    public static final int MetadataBufferSize = 8 * 1024;

    private final BlockCache<Block> _blockCache;

    private final Context _context;

    private byte[] _ioBuffer;

    private final BlockCache<Metablock> _metablockCache;

    public VfsSquashFileSystemReader(Stream stream) {
        super(new DiscFileSystemOptions());
        _context = new Context();
        _context.setSuperBlock(new SuperBlock());
        _context.setRawStream(stream);
        // Read superblock
        stream.setPosition(0);
        byte[] buffer = StreamUtilities.readExact(stream, (int) _context.getSuperBlock().getSize());
        _context.getSuperBlock().readFrom(buffer, 0);
        if (_context.getSuperBlock().Magic != SuperBlock.SquashFsMagic) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Invalid SquashFS filesystem - magic mismatch");
        }

        if (_context.getSuperBlock().Compression != 1) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Unsupported compression used");
        }

        if (_context.getSuperBlock().ExtendedAttrsTableStart != -1) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Unsupported extended attributes present");
        }

        if (_context.getSuperBlock().MajorVersion != 4) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Unsupported file system version: "
                    + _context.getSuperBlock().MajorVersion + "." + _context.getSuperBlock().MinorVersion);
        }

        // Create block caches, used to reduce the amount of I/O and decompression
        // activity.
        _blockCache = new BlockCache<>(_context.getSuperBlock().BlockSize, 20);
        _metablockCache = new BlockCache<>(MetadataBufferSize, 20);
        _context.setReadBlock(this::readBlock);
        _context.setReadMetaBlock(this::readMetaBlock);
        _context.setInodeReader(new MetablockReader(_context, _context.getSuperBlock().InodeTableStart));
        _context.setDirectoryReader(new MetablockReader(_context, _context.getSuperBlock().DirectoryTableStart));
        if (_context.getSuperBlock().FragmentTableStart != -1) {
            _context.setFragmentTableReaders(loadIndirectReaders(_context.getSuperBlock().FragmentTableStart,
                                                                 _context.getSuperBlock().FragmentsCount,
                                                                 FragmentRecord.RecordSize));
        }

        if (_context.getSuperBlock().UidGidTableStart != -1) {
            _context.setUidGidTableReaders(loadIndirectReaders(_context.getSuperBlock().UidGidTableStart,
                                                               _context.getSuperBlock().UidGidCount,
                                                               4));
        }

        // Bootstrap the root directory
        _context.getInodeReader().setPosition(_context.getSuperBlock().RootInode);
        DirectoryInode dirInode = (DirectoryInode) Inode.read(_context.getInodeReader());
        setRootDirectory(new Directory(_context, dirInode, _context.getSuperBlock().RootInode));
    }

    public String getFriendlyName() {
        return "SquashFs";
    }

    public String getVolumeLabel() {
        return "";
    }

    public UnixFileSystemInfo getUnixFileInfo(String path) {
        File file = getFile(path);
        Inode inode = file.getInode();
        DeviceInode devInod = inode instanceof DeviceInode ? (DeviceInode) inode : (DeviceInode) null;
        UnixFileSystemInfo info = new UnixFileSystemInfo();
        return info;
    }

    /**
     * Size of the Filesystem in bytes
     */
    public long getSize() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }

    /**
     * Used space of the Filesystem in bytes
     */
    public long getUsedSpace() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }

    /**
     * Available space of the Filesystem in bytes
     */
    public long getAvailableSpace() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }

    public static UnixFileType fileTypeFromInodeType(InodeType inodeType) {
        switch (inodeType) {
        case BlockDevice:
        case ExtendedBlockDevice:
            return UnixFileType.Block;
        case CharacterDevice:
        case ExtendedCharacterDevice:
            return UnixFileType.Character;
        case Directory:
        case ExtendedDirectory:
            return UnixFileType.Directory;
        case Fifo:
        case ExtendedFifo:
            return UnixFileType.Fifo;
        case File:
        case ExtendedFile:
            return UnixFileType.Regular;
        case Socket:
        case ExtendedSocket:
            return UnixFileType.Socket;
        case Symlink:
        case ExtendedSymlink:
            return UnixFileType.Link;
        default:
            throw new UnsupportedOperationException("Unrecognized inode type: " + inodeType);
        }
    }

    protected File convertDirEntryToFile(DirectoryEntry dirEntry) {
        MetadataRef inodeRef = dirEntry.getInodeReference();
        _context.getInodeReader().setPosition(inodeRef);
        Inode inode = Inode.read(_context.getInodeReader());
        if (dirEntry.isSymlink()) {
            return new Symlink(_context, inode, inodeRef);
        }

        if (dirEntry.isDirectory()) {
            return new Directory(_context, inode, inodeRef);
        }

        return new File(_context, inode, inodeRef);
    }

    private MetablockReader[] loadIndirectReaders(long pos, int count, int recordSize) {
        _context.getRawStream().setPosition(pos);
        int numBlocks = MathUtilities.ceil(count * recordSize, MetadataBufferSize);
        byte[] tableBytes = StreamUtilities.readExact(_context.getRawStream(), numBlocks * 8);
        MetablockReader[] result = new MetablockReader[numBlocks];
        for (int i = 0; i < numBlocks; ++i) {
            long block = EndianUtilities.toInt64LittleEndian(tableBytes, i * 8);
            result[i] = new MetablockReader(_context, block);
        }
        return result;
    }

    private int getId(short idKey) {
        int recordsPerBlock = MetadataBufferSize / 4;
        int block = idKey / recordsPerBlock;
        int offset = idKey % recordsPerBlock;
        MetablockReader reader = _context.getUidGidTableReaders()[block];
        reader.setPosition(0, offset * 4);
        return reader.readInt();
    }

    private Block readBlock(long pos, int diskLen) {
        Block block = _blockCache.getBlock(pos, Block.class);
        if (block.getAvailable() >= 0) {
            return block;
        }

        Stream stream = _context.getRawStream();
        stream.setPosition(pos);
        int readLen = diskLen & 0x00FFFFFF;
        boolean isCompressed = (diskLen & 0x01000000) == 0;
        if (isCompressed) {
            if (_ioBuffer == null || readLen > _ioBuffer.length) {
                _ioBuffer = new byte[readLen];
            }

            StreamUtilities.readExact(stream, _ioBuffer, 0, readLen);

            try (ZlibStream zlibStream = new ZlibStream(new MemoryStream(_ioBuffer, 0, readLen, false),
                                                        CompressionMode.Decompress,
                                                        true)) {
                block.setAvailable(StreamUtilities
                        .readMaximum(zlibStream, block.getData(), 0, _context.getSuperBlock().BlockSize));
            } catch (IOException e) {
                throw new moe.yo3explorer.dotnetio4j.IOException(e);
            }
        } else {
            StreamUtilities.readExact(stream, block.getData(), 0, readLen);
            block.setAvailable(readLen);
        }
        return block;
    }

    private Metablock readMetaBlock(long pos) {
        Metablock block = _metablockCache.getBlock(pos, Metablock.class);
        if (block.getAvailable() >= 0) {
            return block;
        }

        Stream stream = _context.getRawStream();
        stream.setPosition(pos);
        byte[] buffer = StreamUtilities.readExact(stream, 2);
        int readLen = EndianUtilities.toUInt16LittleEndian(buffer, 0);
        boolean isCompressed = (readLen & 0x8000) == 0;
        readLen &= 0x7FFF;
        if (readLen == 0) {
            readLen = 0x8000;
        }

        block.setNextBlockStart(pos + readLen + 2);
        if (isCompressed) {
            if (_ioBuffer == null || readLen > _ioBuffer.length) {
                _ioBuffer = new byte[readLen];
            }

            StreamUtilities.readExact(stream, _ioBuffer, 0, readLen);

            try (ZlibStream zlibStream = new ZlibStream(new MemoryStream(_ioBuffer, 0, readLen, false),
                                                        CompressionMode.Decompress,
                                                        true)) {
                block.setAvailable(StreamUtilities.readMaximum(zlibStream, block.getData(), 0, MetadataBufferSize));
            } catch (IOException e) {
                throw new moe.yo3explorer.dotnetio4j.IOException(e);
            }
        } else {
            block.setAvailable(StreamUtilities.readMaximum(stream, block.getData(), 0, readLen));
        }
        return block;
    }
}
