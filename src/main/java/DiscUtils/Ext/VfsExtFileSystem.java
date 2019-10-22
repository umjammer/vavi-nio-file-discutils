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

package DiscUtils.Ext;

import java.util.EnumSet;

import DiscUtils.Core.FileSystemParameters;
import DiscUtils.Core.IUnixFileSystem;
import DiscUtils.Core.UnixFilePermissions;
import DiscUtils.Core.UnixFileSystemInfo;
import DiscUtils.Core.UnixFileType;
import DiscUtils.Core.Vfs.VfsReadOnlyFileSystem;
import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.IOException;
import moe.yo3explorer.dotnetio4j.Stream;


public final class VfsExtFileSystem extends VfsReadOnlyFileSystem<DirEntry, File, Directory, Context>
    implements IUnixFileSystem {
    public static final EnumSet<IncompatibleFeatures> SupportedIncompatibleFeatures = EnumSet
            .of(IncompatibleFeatures.FileType,
                IncompatibleFeatures.FlexBlockGroups,
                IncompatibleFeatures.Extents,
                IncompatibleFeatures.NeedsRecovery,
                IncompatibleFeatures.SixtyFourBit);

    private final BlockGroup[] _blockGroups;

    public VfsExtFileSystem(Stream stream, FileSystemParameters parameters) {
        super(new ExtFileSystemOptions(parameters));
        stream.setPosition(1024);
        byte[] superblockData = StreamUtilities.readExact(stream, 1024);

        SuperBlock superblock = new SuperBlock();
        superblock.readFrom(superblockData, 0);

        if (superblock.Magic != SuperBlock.Ext2Magic) {
            throw new IOException("Invalid superblock magic - probably not an Ext file system");
        }

        if (superblock.RevisionLevel == SuperBlock.OldRevision) {
            throw new IOException("Old ext revision - not supported");
        }

        if (!superblock._IncompatibleFeatures.containsAll(SupportedIncompatibleFeatures)) {
            throw new IOException("Incompatible ext features present: " + superblock._IncompatibleFeatures);
        }

        Context context = new Context();
        context.setRawStream(stream);
        context.setSuperBlock(superblock);
        context.setOptions((ExtFileSystemOptions) getOptions());
        setContext(context);

        int numGroups = MathUtilities.ceil(superblock.BlocksCount, superblock.BlocksPerGroup);
        long blockDescStart = (superblock.FirstDataBlock + 1) * (long) superblock.getBlockSize();

        stream.setPosition(blockDescStart);
        int bgDescSize = superblock.getHas64Bit() ? superblock.DescriptorSize : BlockGroup.DescriptorSize;
        byte[] blockDescData = StreamUtilities.readExact(stream, numGroups * bgDescSize);

        _blockGroups = new BlockGroup[numGroups];
        for (int i = 0; i < numGroups; ++i) {
            BlockGroup bg = superblock.getHas64Bit() ? new BlockGroup64(bgDescSize) : new BlockGroup();
            bg.readFrom(blockDescData, i * bgDescSize);
            _blockGroups[i] = bg;
        }

        JournalSuperBlock journalSuperBlock = new JournalSuperBlock();
        if (superblock.JournalInode != 0) {
            Inode journalInode = getInode(superblock.JournalInode);
            IBuffer journalDataStream = journalInode.getContentBuffer(getContext());
            byte[] journalData = StreamUtilities.readExact(journalDataStream, 0, 1024 + 12);
            journalSuperBlock.readFrom(journalData, 0);
            getContext().setJournalSuperblock(journalSuperBlock);
        }

        setRootDirectory(new Directory(getContext(), 2, getInode(2)));
    }

    public String getFriendlyName() {
        return "EXT-family";
    }

    public String getVolumeLabel() {
        return getContext().getSuperBlock().VolumeName;
    }

    public UnixFileSystemInfo getUnixFileInfo(String path) {
        File file = getFile(path);
        Inode inode = file.getInode();

        UnixFileType fileType = UnixFileType.valueOf((inode.Mode >>> 12) & 0xff);

        int deviceId = 0;
        if (fileType == UnixFileType.Character || fileType == UnixFileType.Block) {
            if (inode.DirectBlocks[0] != 0) {
                deviceId = inode.DirectBlocks[0];
            } else {
                deviceId = inode.DirectBlocks[1];
            }
        }

        UnixFileSystemInfo fileInfo = new UnixFileSystemInfo();
        fileInfo.setFileType(fileType);
        fileInfo.setPermissions(UnixFilePermissions.valueOf(inode.Mode & 0xfff));
        fileInfo.setUserId((inode.UserIdHigh << 16) | inode.UserIdLow);
        fileInfo.setGroupId((inode.GroupIdHigh << 16) | inode.GroupIdLow);
        fileInfo.setInode(file.getInodeNumber());
        fileInfo.setLinkCount(inode.LinksCount);
        fileInfo.setDeviceId(deviceId);
        return fileInfo;
    }

    protected File convertDirEntryToFile(DirEntry dirEntry) {
        Inode inode = getInode(dirEntry.getRecord().Inode);
        if (dirEntry.getRecord().FileType == DirectoryRecord.FileTypeDirectory) {
            return new Directory(getContext(), dirEntry.getRecord().Inode, inode);
        }

        if (dirEntry.getRecord().FileType == DirectoryRecord.FileTypeSymlink) {
            return new Symlink(getContext(), dirEntry.getRecord().Inode, inode);
        }
        return new File(getContext(), dirEntry.getRecord().Inode, inode);
    }

    private Inode getInode(int inodeNum) {
        int index = inodeNum - 1;

        SuperBlock superBlock = getContext().getSuperBlock();

        int group = index / superBlock.InodesPerGroup;
        int groupOffset = index - group * superBlock.InodesPerGroup;
        BlockGroup inodeBlockGroup = getBlockGroup(group);

        int inodesPerBlock = superBlock.getBlockSize() / superBlock.InodeSize;
        int block = groupOffset / inodesPerBlock;
        int blockOffset = groupOffset - block * inodesPerBlock;

        getContext().getRawStream()
                .setPosition((inodeBlockGroup.InodeTableBlock + block) * (long) superBlock.getBlockSize() +
                    blockOffset * superBlock.InodeSize);
        byte[] inodeData = StreamUtilities.readExact(getContext().getRawStream(), superBlock.InodeSize);

        return EndianUtilities.<Inode> toStruct(Inode.class, inodeData, 0);
    }

    private BlockGroup getBlockGroup(int index) {
        return _blockGroups[index];
    }

    /**
     * Size of the Filesystem in bytes
     */
    public long getSize() {
        SuperBlock superBlock = getContext().getSuperBlock();
        long blockCount = ((long) superBlock.BlocksCountHigh << 32) | superBlock.BlocksCount;
        long inodeSize = superBlock.InodesCount * superBlock.InodeSize;
        long overhead = 0;
        long journalSize = 0;
        if (superBlock.OverheadBlocksCount != 0) {
            overhead = superBlock.OverheadBlocksCount * superBlock.getBlockSize();
        }
        if (getContext().getJournalSuperblock() != null) {
            journalSize = getContext().getJournalSuperblock().MaxLength * getContext().getJournalSuperblock().BlockSize;
        }
        return superBlock.getBlockSize() * blockCount - (inodeSize + overhead + journalSize);
    }

    /**
     * Used space of the Filesystem in bytes
     */
    public long getUsedSpace() {
        return getSize() - getAvailableSpace();
    }

    /**
     * Available space of the Filesystem in bytes
     */
    public long getAvailableSpace() {
        SuperBlock superBlock = getContext().getSuperBlock();
        if (superBlock.getHas64Bit()) {
            long free = 0;
            for (BlockGroup blockGroup : _blockGroups) {
                // ext4 64Bit Feature
                free += BlockGroup64.class.cast(blockGroup).FreeBlocksCountHigh << 16 | blockGroup.FreeBlocksCount;
            }
            return superBlock.getBlockSize() * free;
        } else {
            long free = 0;
            for (BlockGroup blockGroup : _blockGroups) {
                // ext4 64Bit Feature
                free += blockGroup.FreeBlocksCount;
            }
            return superBlock.getBlockSize() * free;
        }
    }
}
