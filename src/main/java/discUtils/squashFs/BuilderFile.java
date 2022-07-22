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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import discUtils.core.internal.LocalFileLocator;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.Stream;


public final class BuilderFile extends BuilderNode {

    private RegularInode inode;

    private List<Integer> lengths;

    private Stream source;

    private String sourcePath;

    public BuilderFile(Stream source) {
        this.source = source;
        setNumLinks(1);
    }

    public BuilderFile(String source) {
        sourcePath = source;
        setNumLinks(1);
    }

    public Inode getInode() {
        return inode;
    }

    public void reset() {
        inode = new RegularInode();
        lengths = null;
    }

    public void write(BuilderContext context) {
        if (!written) {
            writeFileData(context);
            writeInode(context);
            written = true;
        }
    }

    private void writeFileData(BuilderContext context) {
        Stream outStream = context.getRawStream();
        boolean disposeSource = false;
        try {
            if (source == null) {
                LocalFileLocator locator = new LocalFileLocator("");
                source = locator.open(sourcePath, FileMode.Open, FileAccess.Read, FileShare.Read);
                disposeSource = true;
            }

            if (source.getPosition() != 0) {
                source.setPosition(0);
            }

            long startPos = outStream.getPosition();
            int bufferedBytes = StreamUtilities.readMaximum(source, context.getIoBuffer(), 0, context.getDataBlockSize());
            if (bufferedBytes < context.getDataBlockSize()) {
                // Fragment - less than one complete block of data
                inode.startBlock = 0xFFFF_FFFF;
                int[] offset = new int[1];
                inode.fragmentKey = context.getWriteFragment().invoke(bufferedBytes, offset);
                inode.fragmentOffset = offset[0];
                inode.setFileSize(bufferedBytes);
            } else {
                // At least one full block, no fragments used
                inode.fragmentKey = 0xFFFF_FFFF;
                lengths = new ArrayList<>();
                inode.startBlock = (int) startPos;
                inode.setFileSize(bufferedBytes);
                while (bufferedBytes > 0) {
                    lengths.add(context.getWriteDataBlock().invoke(context.getIoBuffer(), 0, bufferedBytes));
                    bufferedBytes = StreamUtilities.readMaximum(source, context.getIoBuffer(), 0, context.getDataBlockSize());
                    inode.setFileSize(inode.getFileSize() + bufferedBytes);
                }
            }
        } finally {
            if (disposeSource) {
                try {
                    source.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void writeInode(BuilderContext context) {
        if (getNumLinks() != 1) {
            throw new dotnet4j.io.IOException("Extended file records (with multiple hard links) not supported");
        }

        fillCommonInodeData(context);
        inode.type = InodeType.File;
        setInodeRef(context.getInodeWriter().getPosition());
        int totalSize = inode.size();
        inode.writeTo(context.getIoBuffer(), 0);
        if (lengths != null && lengths.size() > 0) {
            for (int i = 0; i < lengths.size(); ++i) {
                EndianUtilities.writeBytesLittleEndian(lengths.get(i), context.getIoBuffer(), inode.size() + i * 4);
            }
            totalSize += lengths.size() * 4;
        }

        context.getInodeWriter().write(context.getIoBuffer(), 0, totalSize);
    }
}
