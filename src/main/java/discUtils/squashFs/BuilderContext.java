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

package discUtils.squashFs;

import dotnet4j.io.Stream;

public final class BuilderContext {
    private AllocateId __AllocateId;

    public AllocateId getAllocateId() {
        return __AllocateId;
    }

    public void setAllocateId(AllocateId value) {
        __AllocateId = value;
    }

    private AllocateInode __AllocateInode;

    public AllocateInode getAllocateInode() {
        return __AllocateInode;
    }

    public void setAllocateInode(AllocateInode value) {
        __AllocateInode = value;
    }

    private int __DataBlockSize;

    public int getDataBlockSize() {
        return __DataBlockSize;
    }

    public void setDataBlockSize(int value) {
        __DataBlockSize = value;
    }

    private MetablockWriter __DirectoryWriter;

    public MetablockWriter getDirectoryWriter() {
        return __DirectoryWriter;
    }

    public void setDirectoryWriter(MetablockWriter value) {
        __DirectoryWriter = value;
    }

    private MetablockWriter __InodeWriter;

    public MetablockWriter getInodeWriter() {
        return __InodeWriter;
    }

    public void setInodeWriter(MetablockWriter value) {
        __InodeWriter = value;
    }

    private byte[] __IoBuffer;

    public byte[] getIoBuffer() {
        return __IoBuffer;
    }

    public void setIoBuffer(byte[] value) {
        __IoBuffer = value;
    }

    private Stream __RawStream;

    public Stream getRawStream() {
        return __RawStream;
    }

    public void setRawStream(Stream value) {
        __RawStream = value;
    }

    private WriteDataBlock __WriteDataBlock;

    public WriteDataBlock getWriteDataBlock() {
        return __WriteDataBlock;
    }

    public void setWriteDataBlock(WriteDataBlock value) {
        __WriteDataBlock = value;
    }

    private WriteFragment __WriteFragment;

    public WriteFragment getWriteFragment() {
        return __WriteFragment;
    }

    public void setWriteFragment(WriteFragment value) {
        __WriteFragment = value;
    }
}
