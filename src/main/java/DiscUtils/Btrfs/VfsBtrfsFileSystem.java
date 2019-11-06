//
// Copyright (c) 2017, Bianco Veigel
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

package DiscUtils.Btrfs;

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Btrfs.Base.DirItemChildType;
import DiscUtils.Btrfs.Base.ItemType;
import DiscUtils.Btrfs.Base.Key;
import DiscUtils.Btrfs.Base.ReservedObjectId;
import DiscUtils.Btrfs.Base.Items.DirItem;
import DiscUtils.Btrfs.Base.Items.RootItem;
import DiscUtils.Btrfs.Base.Items.RootRef;
import DiscUtils.Core.IUnixFileSystem;
import DiscUtils.Core.UnixFileSystemInfo;
import DiscUtils.Core.Vfs.VfsReadOnlyFileSystem;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


final class VfsBtrfsFileSystem extends VfsReadOnlyFileSystem<DirEntry, File, Directory, Context> implements
                                      IUnixFileSystem {
    public VfsBtrfsFileSystem(Stream stream) {
        this(stream, new BtrfsFileSystemOptions());
    }

    public VfsBtrfsFileSystem(Stream stream, BtrfsFileSystemOptions options) {
        super(options);

        Context context = new Context(options);
        context.setRawStream(stream);
        setContext(context);
        for (long offset : BtrfsFileSystem.SuperblockOffsets) {
            if (offset + SuperBlock.Length > stream.getLength())
                break;

            stream.setPosition(offset);
            byte[] superblockData = StreamUtilities.readExact(stream, SuperBlock.Length);
            SuperBlock superblock = new SuperBlock();
            superblock.readFrom(superblockData, 0);

            if (superblock.getMagic() != SuperBlock.BtrfsMagic)
                throw new IOException("Invalid Superblock Magic");

            if (getContext().getSuperBlock() == null)
                getContext().setSuperBlock(superblock);
            else if (getContext().getSuperBlock().getGeneration() < superblock.getGeneration())
                getContext().setSuperBlock(superblock);

            getContext().verifyChecksum(superblock.getChecksum(), superblockData, 0x20, 0x1000 - 0x20);
        }
        if (getContext().getSuperBlock() == null)
            throw new IOException("No Superblock detected");
        context.setChunkTreeRoot(context.readTree(context.getSuperBlock().getChunkRoot(),
                                                            context.getSuperBlock().getChunkRootLevel()));
        context.setRootTreeRoot(context.readTree(context.getSuperBlock().getRoot(),
                                                           context.getSuperBlock().getRootLevel()));
        DirItem rootDir = (DirItem) context.findKey(context.getSuperBlock().getRootDirObjectid(), ItemType.DirItem);
        RootItem fsTreeLocation;
        if (!options.useDefaultSubvolume()) {
            fsTreeLocation = (RootItem) context.findKey(options.getSubvolumeId(), ItemType.RootItem);
        } else {
            fsTreeLocation = (RootItem) context.findKey(rootDir.getChildLocation().getObjectId(),
                                                             rootDir.getChildLocation().getItemType());
        }
        long rootDirObjectId = fsTreeLocation.getRootDirId();
        context.getFsTrees()
                .put(rootDir.getChildLocation().getObjectId(),
                     context.readTree(fsTreeLocation.getByteNr(), fsTreeLocation.getLevel()));
        DirEntry dirEntry = new DirEntry(rootDir.getChildLocation().getObjectId(), rootDirObjectId);
        setRootDirectory(new Directory(dirEntry, context));
    }

    public String getFriendlyName() {
        return "Btrfs";
    }

    /**
     * 
     */
    public String getVolumeLabel() {
        return getContext().getSuperBlock().getLabel();
    }

    /**
     * Size of the Filesystem in bytes
     */
    public long getSize() {
        return getContext().getSuperBlock().getTotalBytes();
    }

    /**
     * Used space of the Filesystem in bytes
     */
    public long getUsedSpace() {
        return getContext().getSuperBlock().getBytesUsed();
    }

    /**
     * Available space of the Filesystem in bytes
     */
    public long getAvailableSpace() {
        return getSize() - getUsedSpace();
    }

    public Subvolume[] getSubvolumes() {
        List<RootRef> volumes = getContext().getRootTreeRoot()
                .find(RootRef.class, new Key(ReservedObjectId.FsTree, ItemType.RootRef), getContext());
        List<Subvolume> result = new ArrayList<>();
        for (RootRef volume : volumes) {
            Subvolume subvolume = new Subvolume();
            subvolume.setId(volume.getKey().getOffset());
            subvolume.setName(volume.getName());
            result.add(subvolume);
        }
        return result.toArray(new Subvolume[0]);
    }

    protected File convertDirEntryToFile(DirEntry dirEntry) {
        if (dirEntry.isDirectory()) {
            if (dirEntry.getCachedDirectory() != null) {
                return dirEntry.getCachedDirectory();
            } else {
                dirEntry.setCachedDirectory(new Directory(dirEntry, getContext()));
                return dirEntry.getCachedDirectory();
            }
        } else if (dirEntry.isSymlink()) {
            return new Symlink(dirEntry, getContext());
        } else if (dirEntry.getType() == DirItemChildType.RegularFile) {
            return new File(dirEntry, getContext());
        } else {
            throw new IllegalArgumentException(String.format("Type %s is not supported in btrfs", dirEntry.getType()));
        }
    }

    public UnixFileSystemInfo getUnixFileInfo(String path) {
        throw new UnsupportedOperationException();
    }
}
