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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.SubStream;
import discUtils.streams.builder.BuilderBytesExtent;
import discUtils.streams.builder.BuilderExtent;
import discUtils.streams.builder.BuilderStreamExtent;
import discUtils.streams.builder.StreamBuilder;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Range;
import discUtils.streams.util.Sizes;
import dotnet4j.io.MemoryStream;


public final class MonolithicSparseExtentBuilder extends StreamBuilder {

    private final SparseStream content;

    private final DescriptorFile descriptor;

    public MonolithicSparseExtentBuilder(SparseStream content, DescriptorFile descriptor) {
        this.content = content;
        this.descriptor = descriptor;
    }

    /**
     * @param totalLength {@cs out}
     */
    protected List<BuilderExtent> fixExtents(long[] totalLength) {
        List<BuilderExtent> extents = new ArrayList<>();
        MemoryStream descriptorStream = new MemoryStream();
        descriptor.write(descriptorStream);
        // Figure out grain size and number of grain tables, and adjust actual extent size to be a multiple
        // of grain size
        final int GtesPerGt = 512;
        long grainSize = 128;
        int numGrainTables = (int) MathUtilities.ceil(content.getLength(), grainSize * GtesPerGt * Sizes.Sector);
        long descriptorLength = 10 * Sizes.OneKiB;
        // MathUtilities.roundUp(descriptorStream.length, Sizes.Sector);
        long descriptorStart = 0;
        if (descriptorLength != 0) {
            descriptorStart = 1;
        }

        long redundantGrainDirStart = Math.max(descriptorStart, 1) + MathUtilities.ceil(descriptorLength, Sizes.Sector);
        long redundantGrainDirLength = numGrainTables * 4L;
        long redundantGrainTablesStart = redundantGrainDirStart + MathUtilities.ceil(redundantGrainDirLength, Sizes.Sector);
        long redundantGrainTablesLength = (long) numGrainTables * MathUtilities.roundUp(GtesPerGt * 4, Sizes.Sector);
        long grainDirStart = redundantGrainTablesStart + MathUtilities.ceil(redundantGrainTablesLength, Sizes.Sector);
        long grainDirLength = numGrainTables * 4L;
        long grainTablesStart = grainDirStart + MathUtilities.ceil(grainDirLength, Sizes.Sector);
        long grainTablesLength = (long) numGrainTables * MathUtilities.roundUp(GtesPerGt * 4, Sizes.Sector);
        long dataStart = MathUtilities.roundUp(grainTablesStart + MathUtilities.ceil(grainTablesLength, Sizes.Sector),
                                               grainSize);
        // Generate the header, and write it
        HostedSparseExtentHeader header = new HostedSparseExtentHeader();
        header.flags = EnumSet.of(HostedSparseExtentFlags.ValidLineDetectionTest, HostedSparseExtentFlags.RedundantGrainTable);
        header.capacity = MathUtilities.roundUp(content.getLength(), grainSize * Sizes.Sector) / Sizes.Sector;
        header.grainSize = grainSize;
        header.descriptorOffset = descriptorStart;
        header.descriptorSize = descriptorLength / Sizes.Sector;
        header.numGTEsPerGT = GtesPerGt;
        header.rgdOffset = redundantGrainDirStart;
        header.gdOffset = grainDirStart;
        header.overhead = dataStart;
        extents.add(new BuilderBytesExtent(0, header.getBytes()));
        // The descriptor extent
        if (descriptorLength > 0) {
            extents.add(new BuilderStreamExtent(descriptorStart * Sizes.Sector, descriptorStream));
        }

        // The grain directory extents
        extents.add(new GrainDirectoryExtent(redundantGrainDirStart * Sizes.Sector,
                                             redundantGrainTablesStart,
                                             numGrainTables,
                                             GtesPerGt));
        extents.add(new GrainDirectoryExtent(grainDirStart * Sizes.Sector, grainTablesStart, numGrainTables, GtesPerGt));
        // For each graintable span that's present...
        long dataSectorsUsed = 0;
        long gtSpan = GtesPerGt * grainSize * Sizes.Sector;
        for (Range gtRange : StreamExtent.blocks(content.getExtents(), grainSize * GtesPerGt * Sizes.Sector)) {
            for (long i = 0; i < gtRange.getCount(); ++i) {
                int gt = (int) (gtRange.getOffset() + i);
                SubStream gtStream = new SubStream(content, gt * gtSpan, Math.min(gtSpan, content.getLength() - gt * gtSpan));
                GrainTableDataExtent dataExtent = new GrainTableDataExtent((dataStart + dataSectorsUsed) * Sizes.Sector,
                                                                           gtStream,
                                                                           grainSize);
                extents.add(dataExtent);
                extents.add(new GrainTableExtent(grainTablePosition(redundantGrainTablesStart, gt, GtesPerGt),
                                                 gtStream,
                                                 dataStart + dataSectorsUsed,
                                                 GtesPerGt,
                                                 grainSize));
                extents.add(new GrainTableExtent(grainTablePosition(grainTablesStart, gt, GtesPerGt),
                                                 gtStream,
                                                 dataStart + dataSectorsUsed,
                                                 GtesPerGt,
                                                 grainSize));
                dataSectorsUsed += dataExtent.getLength() / Sizes.Sector;
            }
        }
        totalLength[0] = (dataStart + dataSectorsUsed) * Sizes.Sector;
        return extents;
    }

    private static long grainTablePosition(long grainTablesStart, long grainTable, int gtesPerGt) {
        return grainTablesStart * Sizes.Sector + grainTable * MathUtilities.roundUp(gtesPerGt * 4, Sizes.Sector);
    }

    private static class GrainDirectoryExtent extends BuilderBytesExtent {

        private final long grainTablesStart;

        private final int gtesPerGt;

        private final int numGrainTables;

        public GrainDirectoryExtent(long start, long grainTablesStart, int numGrainTables, int gtesPerGt) {
            super(start, MathUtilities.roundUp(numGrainTables * 4, Sizes.Sector));
            this.grainTablesStart = grainTablesStart;
            this.numGrainTables = numGrainTables;
            this.gtesPerGt = gtesPerGt;
        }

        public void prepareForRead() {
            data = new byte[(int) getLength()];
            for (int i = 0; i < numGrainTables; ++i) {
                EndianUtilities.writeBytesLittleEndian(
                        (int) (grainTablesStart +
                                i * MathUtilities.ceil(gtesPerGt * 4, Sizes.Sector)),
                        data,
                        i * 4);
            }
        }

        public void disposeReadState() {
            data = null;
        }
    }

    private static class GrainTableExtent extends BuilderBytesExtent {

        private final SparseStream content;

        private final long dataStart;

        private final long grainSize;

        private final int gtesPerGt;

        public GrainTableExtent(long start,
                SparseStream content,
                long dataStart,
                int gtesPerGt,
                long grainSize) {
            super(start, gtesPerGt * 4L);
            this.content = content;
            this.grainSize = grainSize;
            this.gtesPerGt = gtesPerGt;
            this.dataStart = dataStart;
        }

        public void prepareForRead() {
            data = new byte[gtesPerGt * 4];
            long gtSpan = gtesPerGt * grainSize * Sizes.Sector;
            long sectorsAllocated = 0;
            for (Range block : StreamExtent.blocks(content.getExtents(), grainSize * Sizes.Sector)) {
                for (int i = 0; i < block.getCount(); ++i) {
                    EndianUtilities.writeBytesLittleEndian((int) (dataStart + sectorsAllocated),
                            data,
                                                           (int) ((block.getOffset() + i) * 4));
                    sectorsAllocated += grainSize;
                }
            }
        }

        public void disposeReadState() {
            data = null;
        }
    }

    private static class GrainTableDataExtent extends BuilderExtent {

        private SparseStream content;

        private final Ownership contentOwnership;

        private int[] grainMapOffsets = new int[] {};

        private Range[] grainMapRanges;

        private final long grainSize;

        public GrainTableDataExtent(long start, SparseStream content, long grainSize) {
            this(start, content, Ownership.None, grainSize);
        }

        public GrainTableDataExtent(long start,
                SparseStream content,
                Ownership contentOwnership,
                long grainSize) {
            super(start, sectorsPresent(content, grainSize) * Sizes.Sector);
            this.content = content;
            this.contentOwnership = contentOwnership;
            this.grainSize = grainSize;
        }

        public void close() throws IOException {
            if (content != null && contentOwnership != Ownership.Dispose) {
                content.close();
                content = null;
            }
        }

        public void prepareForRead() {
            long outputGrain = 0;
            grainMapOffsets = new int[(int) (getLength() / (grainSize * Sizes.Sector))];
            grainMapRanges = new Range[grainMapOffsets.length];
            for (Range grainRange : StreamExtent.blocks(content.getExtents(), grainSize * Sizes.Sector)) {
                for (int i = 0; i < grainRange.getCount(); ++i) {
                    grainMapOffsets[(int) outputGrain] = i;
                    grainMapRanges[(int) outputGrain] = grainRange;
                    outputGrain++;
                }
            }
        }

        public int read(long diskOffset, byte[] block, int offset, int count) {
            long start = diskOffset - getStart();
            long grainSizeBytes = grainSize * Sizes.Sector;
            long outputGrain = start / grainSizeBytes;
            long outputGrainOffset = start % grainSizeBytes;
            long grainStart = (grainMapRanges[(int) outputGrain].getOffset() + grainMapOffsets[(int) outputGrain]) *
                              grainSizeBytes;
            long maxRead = (grainMapRanges[(int) outputGrain].getCount() - grainMapOffsets[(int) outputGrain]) *
                           grainSizeBytes;
            long readStart = grainStart + outputGrainOffset;
            int toRead = (int) Math.min(count, maxRead - outputGrainOffset);
            if (readStart > content.getLength()) {
                Arrays.fill(block, offset, offset + toRead, (byte) 0);
                return toRead;
            }

            content.position(readStart);
            return content.read(block, offset, toRead);
        }

        public void disposeReadState() {
            grainMapOffsets = null;
            grainMapRanges = null;
        }

        private static long sectorsPresent(SparseStream content, long grainSize) {
            long total = 0;
            for (Range grainRange : StreamExtent.blocks(content.getExtents(), grainSize * Sizes.Sector)) {
                total += grainRange.getCount() * grainSize;
            }
            return total;
        }
    }
}
