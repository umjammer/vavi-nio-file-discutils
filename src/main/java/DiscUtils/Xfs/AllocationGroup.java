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

import vavi.util.Debug;

import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


public class AllocationGroup {
    public static final int IbtMagic = 0x49414254;

    public static final int IbtCrcMagic = 0x49414233;

    private long _offset;

    public long getOffset() {
        return _offset;
    }

    public void setOffset(long value) {
        _offset = value;
    }

    private AllocationGroupFreeBlockInfo _freeBlockInfo;

    public AllocationGroupFreeBlockInfo getFreeBlockInfo() {
        return _freeBlockInfo;
    }

    public void setFreeBlockInfo(AllocationGroupFreeBlockInfo value) {
        _freeBlockInfo = value;
    }

    private AllocationGroupInodeBtreeInfo _inodeBtreeInfo;

    public AllocationGroupInodeBtreeInfo getInodeBtreeInfo() {
        return _inodeBtreeInfo;
    }

    public void setInodeBtreeInfo(AllocationGroupInodeBtreeInfo value) {
        _inodeBtreeInfo = value;
    }

    private Context _context;

    public Context getContext() {
        return _context;
    }

    public void setContext(Context value) {
        _context = value;
    }

    public AllocationGroup(Context context, long offset) {
        _offset = offset;
        _context = context;
        Stream data = context.getRawStream();
        SuperBlock superblock = _context.getSuperBlock();
        _freeBlockInfo = new AllocationGroupFreeBlockInfo(superblock);
        data.setPosition(offset + superblock.getSectorSize());
        byte[] agfData = StreamUtilities.readExact(data, _freeBlockInfo.size());
        _freeBlockInfo.readFrom(agfData, 0);
        if (_freeBlockInfo.getMagic() != AllocationGroupFreeBlockInfo.AgfMagic) {
            throw new IOException("Invalid AGF magic - probably not an xfs file system");
        }

        _inodeBtreeInfo = new AllocationGroupInodeBtreeInfo(superblock);
        data.setPosition(offset + superblock.getSectorSize() * 2);
        byte[] agiData = StreamUtilities.readExact(data, getInodeBtreeInfo().size());
        _inodeBtreeInfo.readFrom(agiData, 0);
        if (_inodeBtreeInfo.getMagic() != AllocationGroupInodeBtreeInfo.AgiMagic) {
            throw new IOException("Invalid AGI magic - probably not an xfs file system");
        }

        _inodeBtreeInfo.loadBtree(context, offset);
        if (superblock.getSbVersion() < 5 && _inodeBtreeInfo.getRootInodeBtree().getMagic() != IbtMagic ||
            superblock.getSbVersion() >= 5 && _inodeBtreeInfo.getRootInodeBtree().getMagic() != IbtCrcMagic) {
Debug.printf("%d, %x\n", superblock.getSbVersion(), _inodeBtreeInfo.getRootInodeBtree().getMagic());
            throw new IOException("Invalid IBT magic - probably not an xfs file system");
        }

        if (_inodeBtreeInfo.getSequenceNumber() != _freeBlockInfo.getSequenceNumber()) {
            throw new IOException("inconsistent AG sequence numbers");
        }
    }

    public void loadInode(Inode inode) {
        long offset = _offset + ((long) inode.getAgBlock() * _context.getSuperBlock().getBlocksize()) +
                      ((long) inode.getBlockOffset() * _context.getSuperBlock().getInodeSize());
        _context.getRawStream().setPosition(offset);
        byte[] data = StreamUtilities.readExact(_context.getRawStream(), _context.getSuperBlock().getInodeSize());
        inode.readFrom(data, 0);
    }
}
