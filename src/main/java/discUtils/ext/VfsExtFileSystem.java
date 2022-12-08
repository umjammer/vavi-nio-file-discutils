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

package discUtils.ext;

import java.util.Collections;
import java.util.EnumSet;

import discUtils.core.FileSystemParameters;
import discUtils.core.IUnixFileSystem;
import discUtils.core.UnixFilePermissions;
import discUtils.core.UnixFileSystemInfo;
import discUtils.core.UnixFileType;
import discUtils.core.vfs.VfsReadOnlyFileSystem;
import discUtils.streams.buffer.IBuffer;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


final class VfsExtFileSystem extends VfsReadOnlyFileSystem<DirEntry, File, Directory, Context>
    implements IUnixFileSystem {

    static final EnumSet<IncompatibleFeatures> SupportedIncompatibleFeatures = EnumSet
            .of(IncompatibleFeatures.FileType,
                IncompatibleFeatures.FlexBlockGroups,
                IncompatibleFeatures.Extents,
                IncompatibleFeatures.NeedsRecovery,
                IncompatibleFeatures.SixtyFourBit);

    private final BlockGroup[] blockGroups;

    public VfsExtFileSystem(Stream stream, FileSystemParameters parameters) {
        super(new ExtFileSystemOptions(parameters));

        stream.position(1024);
        byte[] superblockData = StreamUtilities.readExact(stream, 1024);

        SuperBlock superblock = new SuperBlock();
        superblock.readFrom(superblockData, 0);

        if (superblock.magic != SuperBlock.Ext2Magic) {
            throw new IOException("Invalid superblock magic - probably not an ext file system");
        }

        if (superblock.revisionLevel == SuperBlock.OldRevision) {
            throw new IOException("Old ext revision - not supported");
        }

        if (Collections.disjoint(superblock.incompatibleFeatures, SupportedIncompatibleFeatures)) {
            throw new IOException("Incompatible ext features present: " + superblock.incompatibleFeatures);
        }

        Context context = new Context();
        context.setRawStream(stream);
        context.setSuperBlock(superblock);
        context.setOptions((ExtFileSystemOptions) getOptions());
        setContext(context);

        int numGroups = MathUtilities.ceil(superblock.blocksCount, superblock.blocksPerGroup);
        long blockDescStart = (superblock.firstDataBlock + 1) * (long) superblock.getBlockSize();

        stream.position(blockDescStart);
        int bgDescSize = superblock.has64Bit() ? superblock.descriptorSize : BlockGroup.DescriptorSize;
        byte[] blockDescData = StreamUtilities.readExact(stream, numGroups * bgDescSize);

        blockGroups = new BlockGroup[numGroups];
        for (int i = 0; i < numGroups; ++i) {
            BlockGroup bg = superblock.has64Bit() ? new BlockGroup64(bgDescSize) : new BlockGroup();
            bg.readFrom(blockDescData, i * bgDescSize);
            blockGroups[i] = bg;
        }

        JournalSuperBlock journalSuperBlock = new JournalSuperBlock();
        if (superblock.journalInode != 0) {
            Inode journalInode = getInode(superblock.journalInode);
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
        return getContext().getSuperBlock().volumeName;
    }

    public UnixFileSystemInfo getUnixFileInfo(String path) {
        File file = getFile(path);
        Inode inode = file.getInode();

        UnixFileType fileType = UnixFileType.values()[(inode.mode >>> 12) & 0xff];

        int deviceId = 0;
        if (fileType == UnixFileType.Character || fileType == UnixFileType.Block) {
            if (inode.directBlocks[0] != 0) {
                deviceId = inode.directBlocks[0];
            } else {
                deviceId = inode.directBlocks[1];
            }
        }

        UnixFileSystemInfo fileInfo = new UnixFileSystemInfo();
        fileInfo.setFileType(fileType);
        fileInfo.setPermissions(UnixFilePermissions.valueOf(inode.mode & 0xfff));
        fileInfo.setUserId(((inode.userIdHigh & 0xffff) << 16) | (inode.userIdLow & 0xffff));
        fileInfo.setGroupId(((inode.groupIdHigh & 0xffff)  << 16) | (inode.groupIdLow & 0xffff));
        fileInfo.setInode(file.getInodeNumber());
        fileInfo.setLinkCount(inode.getLinksCount());
        fileInfo.setDeviceId(deviceId);
        return fileInfo;
    }

    protected File convertDirEntryToFile(DirEntry dirEntry) {
        Inode inode = getInode(dirEntry.getRecord().inode);
        if (dirEntry.getRecord().fileType == DirectoryRecord.FileTypeDirectory) {
            return new Directory(getContext(), dirEntry.getRecord().inode, inode);
        }
        if (dirEntry.getRecord().fileType == DirectoryRecord.FileTypeSymlink) {
            return new Symlink(getContext(), dirEntry.getRecord().inode, inode);
        }
        return new File(getContext(), dirEntry.getRecord().inode, inode);
    }

    private Inode getInode(int inodeNum) {
        int index = inodeNum - 1;

        SuperBlock superBlock = getContext().getSuperBlock();

        int group = index / superBlock.inodesPerGroup;
        int groupOffset = index - group * superBlock.inodesPerGroup;
        BlockGroup inodeBlockGroup = getBlockGroup(group);

        int inodesPerBlock = superBlock.getBlockSize() / superBlock.getInodeSize();
        int block = groupOffset / inodesPerBlock;
        int blockOffset = groupOffset - block * inodesPerBlock;

        getContext().getRawStream()
                .position((inodeBlockGroup.inodeTableBlock + block) * (long) superBlock.getBlockSize() +
                        (long) blockOffset * superBlock.getInodeSize());
        byte[] inodeData = StreamUtilities.readExact(getContext().getRawStream(), superBlock.getInodeSize());

        return EndianUtilities.toStruct(Inode.class, inodeData, 0);
    }

    private BlockGroup getBlockGroup(int index) {
        return blockGroups[index];
    }

    /**
     * Size of the Filesystem in bytes
     */
    public long getSize() {
        SuperBlock superBlock = getContext().getSuperBlock();
        long blockCount = ((long) superBlock.blocksCountHigh << 32) | superBlock.blocksCount;
        long inodeSize = (long) superBlock.inodesCount * superBlock.getInodeSize();
        long overhead = 0;
        long journalSize = 0;
        if (superBlock.overheadBlocksCount != 0) {
            overhead = (long) superBlock.overheadBlocksCount * superBlock.getBlockSize();
        }
        if (getContext().getJournalSuperblock() != null) {
            journalSize = (long) getContext().getJournalSuperblock().maxLength * getContext().getJournalSuperblock().blockSize;
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
        if (superBlock.has64Bit()) {
            long free = 0;
            // ext4 64Bit Feature
            for (BlockGroup blockGroup : blockGroups) {
                free += (long) ((BlockGroup64) blockGroup).getFreeBlocksCountHigh() << 16 | blockGroup.getFreeBlocksCount();
            }
            return superBlock.getBlockSize() * free;
        } else {
            long free = 0;
            // ext4 64Bit Feature
            for (BlockGroup blockGroup : blockGroups) {
                free += blockGroup.getFreeBlocksCount();
            }
            return superBlock.getBlockSize() * free;
        }
    }
}
