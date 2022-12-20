//
// Copyright (c) 2016, Bianco Veigel
// Copyright (c) 2017, Timo Walter
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

package discUtils.xfs;

import discUtils.core.FileSystemParameters;
import discUtils.core.IUnixFileSystem;
import discUtils.core.UnixFileSystemInfo;
import discUtils.core.UnixFileType;
import discUtils.core.vfs.VfsReadOnlyFileSystem;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


public final class VfsXfsFileSystem extends VfsReadOnlyFileSystem<DirEntry, File, Directory, Context>
    implements IUnixFileSystem {

    private static final int XFS_ALLOC_AGFL_RESERVE = 4;

    private static final int BBSHIFT = 9;

    public VfsXfsFileSystem(Stream stream, FileSystemParameters parameters) {
        super(new XfsFileSystemOptions(parameters));

        stream.position(0);
        byte[] superblockData = StreamUtilities.readExact(stream, 264);

        SuperBlock superblock = new SuperBlock();
        superblock.readFrom(superblockData, 0);

        if (superblock.getMagic() != SuperBlock.XfsMagic) {
            throw new IOException("Invalid superblock magic - probably not an xfs file system");
        }

        Context context = new Context();
        context.setRawStream(stream);
        context.setSuperBlock(superblock);
        context.setOptions((XfsFileSystemOptions) getOptions());
        setContext(context);

        AllocationGroup[] allocationGroups = new AllocationGroup[superblock.getAgCount()];
        long offset = 0;
        for (int i = 0; i < allocationGroups.length; i++) {
            AllocationGroup ag = new AllocationGroup(context, offset);
            allocationGroups[ag.getInodeBtreeInfo().getSequenceNumber()] = ag;
            offset = (xFS_AG_DADDR(context.getSuperBlock(), i + 1, xFS_AGF_DADDR(context.getSuperBlock())) << BBSHIFT) -
                superblock.getSectorSize();
        }
        context.setAllocationGroups(allocationGroups);

        setRootDirectory(new Directory(context, context.getInode(superblock.getRootInode())));
    }

    @Override public String getFriendlyName() {
        return "XFS";
    }

    @Override public String getVolumeLabel() {
        return getContext().getSuperBlock().getFilesystemName();
    }

    @Override protected File convertDirEntryToFile(DirEntry dirEntry) {
        if (dirEntry.isDirectory()) {
            if (dirEntry.getCachedDirectory() != null) {
                return dirEntry.getCachedDirectory();
            } else {
                dirEntry.setCachedDirectory(new Directory(getContext(), dirEntry.getInode()));
                return dirEntry.getCachedDirectory();
            }
        } else if (dirEntry.isSymlink()) {
            return new Symlink(getContext(), dirEntry.getInode());
        } else if (dirEntry.getInode().getFileType() == UnixFileType.Regular) {
            return new File(getContext(), dirEntry.getInode());
        } else {
            throw new UnsupportedOperationException(String.format("Type %s is not supported in XFS",
                                                                  dirEntry.getInode().getFileType()));
        }
    }

    /**
     * Size of the Filesystem in bytes
     */
    @Override public long getSize() {
        SuperBlock superblock = getContext().getSuperBlock();
        long lsize = superblock.getLogstart() != 0 ? superblock.getLogBlocks() : 0;
        return (superblock.getDataBlocks() - lsize) * superblock.getBlocksize();
    }

    /**
     * Used space of the Filesystem in bytes
     */
    @Override public long getUsedSpace() {
        return getSize() - getAvailableSpace();
    }

    /**
     * Available space of the Filesystem in bytes
     */
    @Override public long getAvailableSpace() {
        SuperBlock superblock = getContext().getSuperBlock();
        long fdblocks = 0;
        for (AllocationGroup agf : getContext().getAllocationGroups()) {
            fdblocks += agf.getFreeBlockInfo().getFreeBlocks();
        }
        long alloc_set_aside;

        alloc_set_aside = 4 + ((long) superblock.getAgCount() * XFS_ALLOC_AGFL_RESERVE);

        if (superblock.getReadOnlyCompatibleFeatures().contains(ReadOnlyCompatibleFeatures.RMAPBT)) {
            int rmapMaxlevels = 9;
            if (superblock.getReadOnlyCompatibleFeatures().contains(ReadOnlyCompatibleFeatures.REFLINK)) {
                rmapMaxlevels = superblock.computeBtreeMaxlevels();
            }

            alloc_set_aside += (long) superblock.getAgCount() * rmapMaxlevels;
        }
        return (fdblocks - alloc_set_aside) * superblock.getBlocksize();
    }

    @Override public UnixFileSystemInfo getUnixFileInfo(String path) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see "https://github.com/torvalds/linux/blob/2a610b8aa8e5bd449ba270e517b0e72295d62c9c/fs/xfs/libxfs/xfs_format.h#L832"
     * XFS_AG_DADDR
     */
    private long xFS_AG_DADDR(SuperBlock sb, int agno, long d) {
        return xFS_AGB_TO_DADDR(sb, agno, 0) + d;
    }

    /**
     * @see "https://github.com/torvalds/linux/blob/2a610b8aa8e5bd449ba270e517b0e72295d62c9c/fs/xfs/libxfs/xfs_format.h#L829"
     * XFS_AGB_TO_DADDR
     */
    private long xFS_AGB_TO_DADDR(SuperBlock sb, int agno, int agbno) {
        return fsbToBb(sb, (long) agno * sb.getAgBlocks()) + agbno;
    }

    /**
     * @see "https://github.com/torvalds/linux/blob/2a610b8aa8e5bd449ba270e517b0e72295d62c9c/fs/xfs/libxfs/xfs_format.h#L587"
     * XFS_FSB_TO_BB
     */
    private long fsbToBb(SuperBlock sb, long fsbno) {
        return fsbno << (sb.getBlocksizeLog2() - BBSHIFT);
    }

    /**
     * @see "https://github.com/torvalds/linux/blob/2a610b8aa8e5bd449ba270e517b0e72295d62c9c/fs/xfs/libxfs/xfs_format.h#L716"
     * XFS_AGF_DADDR
     */
    private long xFS_AGF_DADDR(SuperBlock sb) {
        return 1L << (sb.getSectorSizeLog2() - BBSHIFT);
    }
}
