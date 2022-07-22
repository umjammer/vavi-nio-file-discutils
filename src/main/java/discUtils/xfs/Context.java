//
// Copyright (c) 2008-2011, Kenneth Bell
// Copyright (c) 2016, Bianco Veigel
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

import discUtils.core.vfs.VfsContext;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


public class Context extends VfsContext {

    private Stream rawStream;

    public Stream getRawStream() {
        return rawStream;
    }

    public void setRawStream(Stream value) {
        rawStream = value;
    }

    private SuperBlock superblock;

    public SuperBlock getSuperBlock() {
        return superblock;
    }

    public void setSuperBlock(SuperBlock value) {
        superblock = value;
    }

    private AllocationGroup[] allocationGroups;

    public AllocationGroup[] getAllocationGroups() {
        return allocationGroups;
    }

    public void setAllocationGroups(AllocationGroup[] value) {
        allocationGroups = value;
    }

    private XfsFileSystemOptions options;

    public XfsFileSystemOptions getOptions() {
        return options;
    }

    public void setOptions(XfsFileSystemOptions value) {
        options = value;
    }

    public Inode getInode(long number) {
        Inode inode = new Inode(number, this);
        AllocationGroup group = allocationGroups[inode.getAllocationGroup()];
        group.loadInode(inode);
        if (inode.getMagic() != Inode.InodeMagic)
            throw new IOException("invalid inode magic");

        return inode;
    }
}
