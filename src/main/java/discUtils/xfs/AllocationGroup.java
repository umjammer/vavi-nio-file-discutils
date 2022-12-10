//
// Copyright (c) 2008-2011, Kenneth Bell
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

import vavi.util.Debug;

import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


public class AllocationGroup {

    public static final int IbtMagic = 0x49414254;

    public static final int IbtCrcMagic = 0x49414233;

    private long offset;

    public long getOffset() {
        return offset;
    }

    public void setOffset(long value) {
        offset = value;
    }

    private AllocationGroupFreeBlockInfo freeBlockInfo;

    public AllocationGroupFreeBlockInfo getFreeBlockInfo() {
        return freeBlockInfo;
    }

    public void setFreeBlockInfo(AllocationGroupFreeBlockInfo value) {
        freeBlockInfo = value;
    }

    private AllocationGroupInodeBtreeInfo inodeBtreeInfo;

    public AllocationGroupInodeBtreeInfo getInodeBtreeInfo() {
        return inodeBtreeInfo;
    }

    public void setInodeBtreeInfo(AllocationGroupInodeBtreeInfo value) {
        inodeBtreeInfo = value;
    }

    private Context context;

    public Context getContext() {
        return context;
    }

    public void setContext(Context value) {
        context = value;
    }

    public AllocationGroup(Context context, long offset) {
        this.offset = offset;
        this.context = context;
        Stream data = context.getRawStream();
        SuperBlock superblock = this.context.getSuperBlock();
        freeBlockInfo = new AllocationGroupFreeBlockInfo(superblock);
        data.position(offset + superblock.getSectorSize());
        byte[] agfData = StreamUtilities.readExact(data, freeBlockInfo.size());
        freeBlockInfo.readFrom(agfData, 0);
        if (freeBlockInfo.getMagic() != AllocationGroupFreeBlockInfo.AgfMagic) {
            throw new IOException("Invalid AGF magic - probably not an xfs file system");
        }

        inodeBtreeInfo = new AllocationGroupInodeBtreeInfo(superblock);
        data.position(offset + superblock.getSectorSize() * 2L);
        byte[] agiData = StreamUtilities.readExact(data, getInodeBtreeInfo().size());
        inodeBtreeInfo.readFrom(agiData, 0);
        if (inodeBtreeInfo.getMagic() != AllocationGroupInodeBtreeInfo.AgiMagic) {
            throw new IOException("Invalid AGI magic - probably not an xfs file system");
        }

        inodeBtreeInfo.loadBtree(context, offset);
        if (superblock.getSbVersion() < 5 && inodeBtreeInfo.getRootInodeBtree().getMagic() != IbtMagic ||
            superblock.getSbVersion() >= 5 && inodeBtreeInfo.getRootInodeBtree().getMagic() != IbtCrcMagic) {
Debug.printf("%d, %x\n", superblock.getSbVersion(), inodeBtreeInfo.getRootInodeBtree().getMagic());
            throw new IOException("Invalid IBT magic - probably not an xfs file system");
        }

        if (inodeBtreeInfo.getSequenceNumber() != freeBlockInfo.getSequenceNumber()) {
            throw new IOException("inconsistent AG sequence numbers");
        }
    }

    public void loadInode(Inode inode) {
        long offset = this.offset + ((long) inode.getAgBlock() * context.getSuperBlock().getBlocksize()) +
                      ((long) inode.getBlockOffset() * context.getSuperBlock().getInodeSize());
        context.getRawStream().position(offset);
        byte[] data = StreamUtilities.readExact(context.getRawStream(), context.getSuperBlock().getInodeSize());
        inode.readFrom(data, 0);
    }
}
