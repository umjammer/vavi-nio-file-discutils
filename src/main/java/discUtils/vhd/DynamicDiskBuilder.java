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

package discUtils.vhd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.SubStream;
import discUtils.streams.builder.BuilderBufferExtent;
import discUtils.streams.builder.BuilderExtent;
import discUtils.streams.builder.StreamBuilder;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Range;
import discUtils.streams.util.Sizes;
import dotnet4j.io.MemoryStream;
import vavi.util.ByteUtil;


public final class DynamicDiskBuilder extends StreamBuilder {

    private final int blockSize;

    private final SparseStream content;

    private final Footer footer;

    public DynamicDiskBuilder(SparseStream content, Footer footer, int blockSize) {
        this.content = content;
        this.footer = footer;
        this.blockSize = blockSize;
    }

    /**
     * @param totalLength {@cs out}
     */
    @Override protected List<BuilderExtent> fixExtents(long[] totalLength) {
        final int FooterSize = 512;
        final int DynHeaderSize = 1024;

        List<BuilderExtent> extents = new ArrayList<>();

        footer.dataOffset = FooterSize;

        DynamicHeader dynHeader = new DynamicHeader(-1, FooterSize + DynHeaderSize, blockSize, footer.currentSize);

        BlockAllocationTableExtent batExtent = new BlockAllocationTableExtent(FooterSize + DynHeaderSize,
                                                                              dynHeader.maxTableEntries);
        long streamPos = batExtent.getStart() + batExtent.getLength();

        for (Range blockRange : StreamExtent.blocks(content.getExtents(), blockSize)) {
            for (int i = 0; i < blockRange.getCount(); ++i) {
                long block = blockRange.getOffset() + i;
                long blockStart = block * blockSize;
                DataBlockExtent dataExtent = new DataBlockExtent(streamPos,
                        new SubStream(content, blockStart, Math.min(blockSize, content.getLength() - blockStart)));
                extents.add(dataExtent);

                batExtent.setEntry((int) block, (int) (streamPos / Sizes.Sector));

                streamPos += dataExtent.getLength();
            }
        }

        footer.updateChecksum();
        dynHeader.updateChecksum();

        byte[] footerBuffer = new byte[FooterSize];
        footer.toBytes(footerBuffer, 0);

        byte[] dynHeaderBuffer = new byte[DynHeaderSize];
        dynHeader.toBytes(dynHeaderBuffer, 0);

        // Add footer (to end)
        extents.add(new BuilderBufferExtent(streamPos, footerBuffer));
        totalLength[0] = streamPos + FooterSize;

        extents.add(0, batExtent);
        extents.add(0, new BuilderBufferExtent(FooterSize, dynHeaderBuffer));
        extents.add(0, new BuilderBufferExtent(0, footerBuffer));
//logger.log(Level.DEBUG, extents);
//extents.forEach(e -> {
//    e.prepareForRead();
//    byte[] b = new byte[128];
//    int r = e.read(e.getStart(), b, 0, 64);
//    logger.log(Level.DEBUG, e + "\n" + StringUtil.getDump(b, 0, r));
//});
        return extents;
    }

    private static class BlockAllocationTableExtent extends BuilderExtent {

        private MemoryStream dataStream;

        private final int[] entries;

        public BlockAllocationTableExtent(long start, int maxEntries) {
            super(start, MathUtilities.roundUp(maxEntries * 4, 512));

            entries = new int[(int) (getLength() / 4)];
            Arrays.fill(entries, 0xFFFFFFFF);
        }

        @Override public void close() throws IOException {
            if (dataStream != null) {
                dataStream.close();
                dataStream = null;
            }
        }

        public void setEntry(int index, int fileSector) {
            entries[index] = fileSector;
        }

        @Override public void prepareForRead() {
            byte[] buffer = new byte[(int) getLength()];

            for (int i = 0; i < entries.length; ++i) {
                ByteUtil.writeBeInt(entries[i], buffer, i * 4);
            }

            dataStream = new MemoryStream(buffer, false);
        }

        @Override public int read(long diskOffset, byte[] block, int offset, int count) {
            dataStream.position(diskOffset - getStart());
            return dataStream.read(block, offset, count);
        }

        @Override public void disposeReadState() {
            if (dataStream != null) {
                dataStream.close();
                dataStream = null;
            }
        }
    }

    private static class DataBlockExtent extends BuilderExtent {

        private MemoryStream bitmapStream;

        private SparseStream content;

        private final Ownership ownership;

        public DataBlockExtent(long start, SparseStream content) {
            this(start, content, Ownership.None);
        }

        public DataBlockExtent(long start, SparseStream content, Ownership ownership) {
            super(start,
                  MathUtilities.roundUp(MathUtilities.ceil(content.getLength(), Sizes.Sector) / 8, Sizes.Sector) +
                         MathUtilities.roundUp(content.getLength(), Sizes.Sector));
            this.content = content;
            this.ownership = ownership;
        }

        @Override public void close() throws IOException {
            if (content != null && ownership == Ownership.Dispose) {
                content.close();
                content = null;
            }

            if (bitmapStream != null) {
                bitmapStream.close();
                bitmapStream = null;
            }
        }

        @Override public void prepareForRead() {
            byte[] bitmap = new byte[(int) MathUtilities.roundUp(MathUtilities.ceil(content.getLength(), Sizes.Sector) / 8,
                                                                 Sizes.Sector)];

            for (Range range: StreamExtent.blocks(content.getExtents(), Sizes.Sector)) {
                for (int i = 0; i < range.getCount(); ++i) {
                    byte mask = (byte) (1 << (7 - (int) ((range.getOffset() + i) % 8)));
                    bitmap[(int) ((range.getOffset() + i) / 8)] |= mask;
                }
            }

            bitmapStream = new MemoryStream(bitmap, false);
        }

        @Override public int read(long diskOffset, byte[] block, int offset, int count) {
            long position = diskOffset - getStart();
            if (position < bitmapStream.getLength()) {
                bitmapStream.position(position);
                return bitmapStream.read(block, offset, count);
            }
            content.position(position - bitmapStream.getLength());
            return content.read(block, offset, count);
        }

        @Override public void disposeReadState() {
            if (bitmapStream != null) {
                bitmapStream.close();
                bitmapStream = null;
            }
        }
    }
}
