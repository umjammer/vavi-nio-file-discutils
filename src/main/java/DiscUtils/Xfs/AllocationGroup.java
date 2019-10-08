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

package DiscUtils.Xfs;

import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.IOException;
import moe.yo3explorer.dotnetio4j.Stream;


public class AllocationGroup {
    public static final int IbtMagic = 0x49414254;

    public static final int IbtCrcMagic = 0x49414233;

    private long __Offset;

    public long getOffset() {
        return __Offset;
    }

    public void setOffset(long value) {
        __Offset = value;
    }

    private AllocationGroupFreeBlockInfo __FreeBlockInfo;

    public AllocationGroupFreeBlockInfo getFreeBlockInfo() {
        return __FreeBlockInfo;
    }

    public void setFreeBlockInfo(AllocationGroupFreeBlockInfo value) {
        __FreeBlockInfo = value;
    }

    private AllocationGroupInodeBtreeInfo __InodeBtreeInfo;

    public AllocationGroupInodeBtreeInfo getInodeBtreeInfo() {
        return __InodeBtreeInfo;
    }

    public void setInodeBtreeInfo(AllocationGroupInodeBtreeInfo value) {
        __InodeBtreeInfo = value;
    }

    private Context __Context;

    public Context getContext() {
        return __Context;
    }

    public void setContext(Context value) {
        __Context = value;
    }

    public AllocationGroup(Context context, long offset) {
        setOffset(offset);
        setContext(context);
        Stream data = context.getRawStream();
        SuperBlock superblock = context.getSuperBlock();
        setFreeBlockInfo(new AllocationGroupFreeBlockInfo(superblock));
        data.setPosition(offset + superblock.getSectorSize());
        byte[] agfData = StreamUtilities.readExact(data, (int) getFreeBlockInfo().getSize());
        getFreeBlockInfo().readFrom(agfData, 0);
        if (getFreeBlockInfo().getMagic() != AllocationGroupFreeBlockInfo.AgfMagic) {
            throw new IOException("Invalid AGF magic - probably not an xfs file system");
        }

        setInodeBtreeInfo(new AllocationGroupInodeBtreeInfo(superblock));
        data.setPosition(offset + superblock.getSectorSize() * 2);
        byte[] agiData = StreamUtilities.readExact(data, (int) getInodeBtreeInfo().getSize());
        getInodeBtreeInfo().readFrom(agiData, 0);
        if (getInodeBtreeInfo().getMagic() != AllocationGroupInodeBtreeInfo.AgiMagic) {
            throw new IOException("Invalid AGI magic - probably not an xfs file system");
        }

        getInodeBtreeInfo().loadBtree(context, offset);
        if (superblock.getSbVersion() < 5 && getInodeBtreeInfo().getRootInodeBtree().getMagic() != IbtMagic ||
            superblock.getSbVersion() >= 5 && getInodeBtreeInfo().getRootInodeBtree().getMagic() != IbtCrcMagic) {
            throw new IOException("Invalid IBT magic - probably not an xfs file system");
        }

        if (getInodeBtreeInfo().getSequenceNumber() != getFreeBlockInfo().getSequenceNumber()) {
            throw new IOException("inconsistent AG sequence numbers");
        }

    }

    public void loadInode(Inode inode) {
        long offset = getOffset() + ((long) inode.getAgBlock() * getContext().getSuperBlock().getBlocksize()) +
                      ((long) inode.getBlockOffset() * getContext().getSuperBlock().getInodeSize());
        getContext().getRawStream().setPosition(offset);
        byte[] data = StreamUtilities.readExact(getContext().getRawStream(), getContext().getSuperBlock().getInodeSize());
        inode.readFrom(data, 0);
    }
}
