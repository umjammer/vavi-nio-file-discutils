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

package DiscUtils.SquashFs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import DiscUtils.Core.Internal.LocalFileLocator;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.Stream;


public final class BuilderFile extends BuilderNode {
    private RegularInode _inode;

    private List<Integer> _lengths;

    private Stream _source;

    private String _sourcePath;

    public BuilderFile(Stream source) {
        _source = source;
        setNumLinks(1);
    }

    public BuilderFile(String source) {
        _sourcePath = source;
        setNumLinks(1);
    }

    public Inode getInode() {
        return _inode;
    }

    public void reset() {
        _inode = new RegularInode();
        _lengths = null;
    }

    public void write(BuilderContext context) {
        if (!_written) {
            writeFileData(context);
            writeInode(context);
            _written = true;
        }

    }

    private void writeFileData(BuilderContext context) {
        Stream outStream = context.getRawStream();
        boolean disposeSource = false;
        try {
            if (_source == null) {
                LocalFileLocator locator = new LocalFileLocator("");
                _source = locator.open(_sourcePath, FileMode.Open, FileAccess.Read, FileShare.Read);
                disposeSource = true;
            }

            if (_source.getPosition() != 0) {
                _source.setPosition(0);
            }

            long startPos = outStream.getPosition();
            int bufferedBytes = StreamUtilities.readMaximum(_source, context.getIoBuffer(), 0, context.getDataBlockSize());
            if (bufferedBytes < context.getDataBlockSize()) {
                // Fragment - less than one complete block of data
                _inode.StartBlock = 0xFFFFFFFF;
                int[] refVar___0 = new int[1];
                _inode.FragmentKey = context.getWriteFragment().invoke(bufferedBytes, refVar___0);
                _inode.FragmentOffset = refVar___0[0];
                _inode.setFileSize(bufferedBytes);
            } else {
                // At least one full block, no fragments used
                _inode.FragmentKey = 0xFFFFFFFF;
                _lengths = new ArrayList<>();
                _inode.StartBlock = (int) startPos;
                _inode.setFileSize(bufferedBytes);
                while (bufferedBytes > 0) {
                    _lengths.add(context.getWriteDataBlock().invoke(context.getIoBuffer(), 0, bufferedBytes));
                    bufferedBytes = StreamUtilities.readMaximum(_source, context.getIoBuffer(), 0, context.getDataBlockSize());
                    _inode.setFileSize(_inode.getFileSize() + bufferedBytes);
                }
            }
        } finally {
            if (disposeSource) {
                try {
                    _source.close();
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
            }
        }
    }

    private void writeInode(BuilderContext context) {
        if (getNumLinks() != 1) {
            throw new dotnet4j.io.IOException("Extended file records (with multiple hard links) not supported");
        }

        fillCommonInodeData(context);
        _inode.Type = InodeType.File;
        setInodeRef(context.getInodeWriter().getPosition());
        int totalSize =_inode.sizeOf();
        _inode.writeTo(context.getIoBuffer(), 0);
        if (_lengths != null && _lengths.size() > 0) {
            for (int i = 0; i < _lengths.size(); ++i) {
                EndianUtilities.writeBytesLittleEndian(_lengths.get(i), context.getIoBuffer(), _inode.sizeOf() + i * 4);
            }
            totalSize += _lengths.size() * 4;
        }

        context.getInodeWriter().write(context.getIoBuffer(), 0, totalSize);
    }
}
