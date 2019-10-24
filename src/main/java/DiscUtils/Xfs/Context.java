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

package DiscUtils.Xfs;

import DiscUtils.Core.Vfs.VfsContext;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


public class Context extends VfsContext {
    private Stream __RawStream;

    public Stream getRawStream() {
        return __RawStream;
    }

    public void setRawStream(Stream value) {
        __RawStream = value;
    }

    private SuperBlock __SuperBlock;

    public SuperBlock getSuperBlock() {
        return __SuperBlock;
    }

    public void setSuperBlock(SuperBlock value) {
        __SuperBlock = value;
    }

    private AllocationGroup[] __AllocationGroups;

    public AllocationGroup[] getAllocationGroups() {
        return __AllocationGroups;
    }

    public void setAllocationGroups(AllocationGroup[] value) {
        __AllocationGroups = value;
    }

    private XfsFileSystemOptions __Options;

    public XfsFileSystemOptions getOptions() {
        return __Options;
    }

    public void setOptions(XfsFileSystemOptions value) {
        __Options = value;
    }

    public Inode getInode(long number) {
        Inode inode = new Inode(number, this);
        AllocationGroup group = getAllocationGroups()[inode.getAllocationGroup()];
        group.loadInode(inode);
        if (inode.getMagic() != Inode.InodeMagic)
            throw new IOException("invalid inode magic");

        return inode;
    }
}
