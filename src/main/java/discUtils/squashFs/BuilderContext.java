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

    private AllocateId allocateId;

    public AllocateId getAllocateId() {
        return allocateId;
    }

    public void setAllocateId(AllocateId value) {
        allocateId = value;
    }

    private AllocateInode allocateInode;

    public AllocateInode getAllocateInode() {
        return allocateInode;
    }

    public void setAllocateInode(AllocateInode value) {
        allocateInode = value;
    }

    private int dataBlockSize;

    public int getDataBlockSize() {
        return dataBlockSize;
    }

    public void setDataBlockSize(int value) {
        dataBlockSize = value;
    }

    private MetablockWriter directoryWriter;

    public MetablockWriter getDirectoryWriter() {
        return directoryWriter;
    }

    public void setDirectoryWriter(MetablockWriter value) {
        directoryWriter = value;
    }

    private MetablockWriter inodeWriter;

    public MetablockWriter getInodeWriter() {
        return inodeWriter;
    }

    public void setInodeWriter(MetablockWriter value) {
        inodeWriter = value;
    }

    private byte[] ioBuffer;

    public byte[] getIoBuffer() {
        return ioBuffer;
    }

    public void setIoBuffer(byte[] value) {
        ioBuffer = value;
    }

    private Stream rawStream;

    public Stream getRawStream() {
        return rawStream;
    }

    public void setRawStream(Stream value) {
        rawStream = value;
    }

    private WriteDataBlock writeDataBlock;

    public WriteDataBlock getWriteDataBlock() {
        return writeDataBlock;
    }

    public void setWriteDataBlock(WriteDataBlock value) {
        writeDataBlock = value;
    }

    private WriteFragment writeFragment;

    public WriteFragment getWriteFragment() {
        return writeFragment;
    }

    public void setWriteFragment(WriteFragment value) {
        writeFragment = value;
    }
}
