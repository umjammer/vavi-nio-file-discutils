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

package DiscUtils.Vhd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.Builder.BuilderBufferExtent;
import DiscUtils.Streams.Builder.BuilderExtent;
import DiscUtils.Streams.Builder.StreamBuilder;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Range;
import DiscUtils.Streams.Util.Sizes;
import moe.yo3explorer.dotnetio4j.MemoryStream;


public final class DynamicDiskBuilder extends StreamBuilder {
    private final int _blockSize;

    private final SparseStream _content;

    private final Footer _footer;

    public DynamicDiskBuilder(SparseStream content, Footer footer, int blockSize) {
        _content = content;
        _footer = footer;
        _blockSize = blockSize;
    }

    protected List<BuilderExtent> fixExtents(long[] totalLength) {
        final int FooterSize = 512;
        final int DynHeaderSize = 1024;
        List<BuilderExtent> extents = new ArrayList<>();
        _footer.DataOffset = FooterSize;
        DynamicHeader dynHeader = new DynamicHeader(-1, FooterSize + DynHeaderSize, _blockSize, _footer.CurrentSize);
        BlockAllocationTableExtent batExtent = new BlockAllocationTableExtent(FooterSize + DynHeaderSize,
                                                                              dynHeader.MaxTableEntries);
        long streamPos = batExtent.getStart() + batExtent.getLength();
        for (Range blockRange : StreamExtent.blocks(_content.getExtents(), _blockSize)) {
            for (int i = 0; i < blockRange.getCount(); ++i) {
                long block = blockRange.getOffset() + i;
                long blockStart = block * _blockSize;
                DataBlockExtent dataExtent = new DataBlockExtent(streamPos,
                                                                 new SubStream(_content,
                                                                               blockStart,
                                                                               Math.min(_blockSize,
                                                                                        _content.getLength() - blockStart)));
                extents.add(dataExtent);
                batExtent.setEntry((int) block, (int) (streamPos / Sizes.Sector));
                streamPos += dataExtent.getLength();
            }
        }
        _footer.updateChecksum();
        dynHeader.updateChecksum();
        byte[] footerBuffer = new byte[FooterSize];
        _footer.toBytes(footerBuffer, 0);
        byte[] dynHeaderBuffer = new byte[DynHeaderSize];
        dynHeader.toBytes(dynHeaderBuffer, 0);
        // Add footer (to end)
        extents.add(new BuilderBufferExtent(streamPos, footerBuffer));
        totalLength[0] = streamPos + FooterSize;
        extents.add(0, batExtent);
        extents.add(0, new BuilderBufferExtent(FooterSize, dynHeaderBuffer));
        extents.add(0, new BuilderBufferExtent(0, footerBuffer));
        return extents;
    }

    private static class BlockAllocationTableExtent extends BuilderExtent {
        private MemoryStream _dataStream;

        private final int[] _entries;

        public BlockAllocationTableExtent(long start, int maxEntries) {
            super(start, MathUtilities.roundUp(maxEntries * 4, 512));
            _entries = new int[(int) (getLength() / 4)];
            for (int i = 0; i < _entries.length; ++i) {
                _entries[i] = 0xFFFFFFFF;
            }
        }

        public void close() throws IOException {
            if (_dataStream != null) {
                _dataStream.close();
                _dataStream = null;
            }

        }

        public void setEntry(int index, int fileSector) {
            _entries[index] = fileSector;
        }

        public void prepareForRead() {
            byte[] buffer = new byte[(int) getLength()];
            for (int i = 0; i < _entries.length; ++i) {
                EndianUtilities.writeBytesBigEndian(_entries[i], buffer, i * 4);
            }
            _dataStream = new MemoryStream(buffer, false);
        }

        public int read(long diskOffset, byte[] block, int offset, int count) {
            _dataStream.setPosition(diskOffset - getStart());
            return _dataStream.read(block, offset, count);
        }

        public void disposeReadState() {
            if (_dataStream != null) {
                try {
                    _dataStream.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
                _dataStream = null;
            }
        }
    }

    private static class DataBlockExtent extends BuilderExtent {
        private MemoryStream _bitmapStream;

        private SparseStream _content;

        private final Ownership _ownership;

        public DataBlockExtent(long start, SparseStream content) {
            this(start, content, Ownership.None);
        }

        public DataBlockExtent(long start, SparseStream content, Ownership ownership) {
            super(start,
                  MathUtilities.roundUp(MathUtilities.ceil(content.getLength(), Sizes.Sector) / 8, Sizes.Sector) +
                         MathUtilities.roundUp(content.getLength(), Sizes.Sector));
            _content = content;
            _ownership = ownership;
        }

        public void close() throws IOException {
            if (_content != null && _ownership == Ownership.Dispose) {
                _content.close();
                _content = null;
            }

            if (_bitmapStream != null) {
                _bitmapStream.close();
                _bitmapStream = null;
            }

        }

        public void prepareForRead() {
            byte[] bitmap = new byte[(int) MathUtilities.roundUp(MathUtilities.ceil(_content.getLength(), Sizes.Sector) / 8,
                                                                 Sizes.Sector)];
            for (Range range: StreamExtent.blocks(_content.getExtents(), Sizes.Sector)) {
                for (int i = 0; i < range.getCount(); ++i) {
                    byte mask = (byte) (1 << (7 - (int) (range.getOffset() + i) % 8));
                    bitmap[(int) (range.getOffset() + i) / 8] |= mask;
                }
            }
            _bitmapStream = new MemoryStream(bitmap, false);
        }

        public int read(long diskOffset, byte[] block, int offset, int count) {
            long position = diskOffset - getStart();
            if (position < _bitmapStream.getLength()) {
                _bitmapStream.setPosition(position);
                return _bitmapStream.read(block, offset, count);
            }

            _content.setPosition(position - _bitmapStream.getLength());
            return _content.read(block, offset, count);
        }

        public void disposeReadState() {
            if (_bitmapStream != null) {
                try {
                    _bitmapStream.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
                _bitmapStream = null;
            }
        }
    }
}
