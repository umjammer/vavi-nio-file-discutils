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

package discUtils.vmdk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.SubStream;
import discUtils.streams.builder.BuilderBufferExtent;
import discUtils.streams.builder.BuilderExtent;
import discUtils.streams.builder.StreamBuilder;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Range;
import discUtils.streams.util.Sizes;
import dotnet4j.io.MemoryStream;


public final class VmfsSparseExtentBuilder extends StreamBuilder {

    private final SparseStream content;

    public VmfsSparseExtentBuilder(SparseStream content) {
        this.content = content;
    }

    /**
     * @param totalLength {@cs out}
     */
    protected List<BuilderExtent> fixExtents(long[] totalLength) {
        List<BuilderExtent> extents = new ArrayList<>();

        ServerSparseExtentHeader header = DiskImageFile.createServerSparseExtentHeader(content.getLength());
        GlobalDirectoryExtent gdExtent = new GlobalDirectoryExtent(header);

        long grainTableStart = header.gdOffset * Sizes.Sector + gdExtent.getLength();
        long grainTableCoverage = header.numGTEsPerGT * header.grainSize * Sizes.Sector;

        for (Range grainTableRange : StreamExtent.blocks(content.getExtents(), grainTableCoverage)) {
            for (int i = 0; i < grainTableRange.getCount(); ++i) {
                long grainTable = grainTableRange.getOffset() + i;
                long dataStart = grainTable * grainTableCoverage;
                GrainTableExtent gtExtent = new GrainTableExtent(grainTableStart,
                                                                 new SubStream(content,
                                                                               dataStart,
                                                                               Math.min(grainTableCoverage,
                                                                                        content.getLength() - dataStart)),
                                                                 header);
                extents.add(gtExtent);
                gdExtent.setEntry((int) grainTable, (int) (grainTableStart / Sizes.Sector));

                grainTableStart += gtExtent.getLength();
            }
        }

        extents.add(0, gdExtent);

        header.freeSector = (int) (grainTableStart / Sizes.Sector);

        byte[] buffer = header.getBytes();
        extents.add(0, new BuilderBufferExtent(0, buffer));

        totalLength[0] = grainTableStart;

        return extents;
    }

    private static class GlobalDirectoryExtent extends BuilderExtent {

        private final byte[] buffer;

        private MemoryStream streamView;

        public GlobalDirectoryExtent(ServerSparseExtentHeader header) {
            super(header.gdOffset * Sizes.Sector, MathUtilities.roundUp(header.numGdEntries * 4, Sizes.Sector));
            buffer = new byte[(int) getLength()];
        }

        public void close() throws IOException {
            if (streamView != null) {
                streamView.close();
                streamView = null;
            }

        }

        public void setEntry(int index, int grainTableSector) {
            EndianUtilities.writeBytesLittleEndian(grainTableSector, buffer, index * 4);
        }

        public void prepareForRead() {
            streamView = new MemoryStream(buffer, false);
        }

        public int read(long diskOffset, byte[] block, int offset, int count) {
            streamView.setPosition(diskOffset - getStart());
            return streamView.read(block, offset, count);
        }

        public void disposeReadState() {
            if (streamView != null) {
                streamView.close();
                streamView = null;
            }
        }
    }

    private static class GrainTableExtent extends BuilderExtent {

        private SparseStream content;

        private final Ownership contentOwnership;

        private List<Long> grainContiguousRangeMapping;

        private List<Long> grainMapping;

        private MemoryStream grainTableStream;

        private final ServerSparseExtentHeader header;

        public GrainTableExtent(long outputStart, SparseStream content, ServerSparseExtentHeader header) {
            this(outputStart, content, Ownership.None, header);
        }

        public GrainTableExtent(long outputStart,
                SparseStream content,
                Ownership contentOwnership,
                ServerSparseExtentHeader header) {
            super(outputStart, calcSize(content, header));
            this.content = content;
            this.contentOwnership = contentOwnership;
            this.header = header;
        }

        public void close() throws IOException {
            if (content != null && contentOwnership == Ownership.Dispose) {
                content.close();
                content = null;
            }

            if (grainTableStream != null) {
                grainTableStream.close();
                grainTableStream = null;
            }
        }

        public void prepareForRead() {
            byte[] grainTable = new byte[MathUtilities.roundUp(header.numGTEsPerGT * 4, Sizes.Sector)];
            long dataSector = (getStart() + grainTable.length) / Sizes.Sector;
            grainMapping = new ArrayList<>();
            grainContiguousRangeMapping = new ArrayList<>();
            for (Range grainRange : StreamExtent.blocks(content.getExtents(), header.grainSize * Sizes.Sector)) {
                for (int i = 0; i < grainRange.getCount(); ++i) {
                    EndianUtilities
                            .writeBytesLittleEndian((int) dataSector, grainTable, (int) (4 * (grainRange.getOffset() + i)));
                    dataSector += header.grainSize;
                    grainMapping.add(grainRange.getOffset() + i);
                    grainContiguousRangeMapping.add(grainRange.getCount() - i);
                }
            }
            grainTableStream = new MemoryStream(grainTable, false);
        }

        public int read(long diskOffset, byte[] block, int offset, int count) {
            long relOffset = diskOffset - getStart();
            if (relOffset < grainTableStream.getLength()) {
                grainTableStream.setPosition(relOffset);
                return grainTableStream.read(block, offset, count);
            }

            long grainSize = header.grainSize * Sizes.Sector;
            int grainIdx = (int) ((relOffset - grainTableStream.getLength()) / grainSize);
            long grainOffset = relOffset - grainTableStream.getLength() - grainIdx * grainSize;
            int maxToRead = (int) Math.min(count, grainSize * grainContiguousRangeMapping.get(grainIdx) - grainOffset);
            content.setPosition(grainMapping.get(grainIdx) * grainSize + grainOffset);
            return content.read(block, offset, maxToRead);
        }

        public void disposeReadState() {
            if (grainTableStream != null) {
                grainTableStream.close();
                grainTableStream = null;
            }

            grainMapping = null;
            grainContiguousRangeMapping = null;
        }

        private static long calcSize(SparseStream content, ServerSparseExtentHeader header) {
            long numDataGrains = StreamExtent.blockCount(content.getExtents(), header.grainSize * Sizes.Sector);
            long grainTableSectors = MathUtilities.ceil(header.numGTEsPerGT * 4, Sizes.Sector);
            return (grainTableSectors + numDataGrains * header.grainSize) * Sizes.Sector;
        }
    }
}
