//
// Copyright (c) 2008-2013, Kenneth Bell
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

package discUtils.vhdx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import discUtils.core.DiskImageBuilder;
import discUtils.core.DiskImageFileSpecification;
import discUtils.streams.IByteArraySerializable;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.SubStream;
import discUtils.streams.builder.BuilderBufferExtent;
import discUtils.streams.builder.BuilderExtent;
import discUtils.streams.builder.BuilderSparseStreamExtent;
import discUtils.streams.builder.StreamBuilder;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Range;
import discUtils.streams.util.Sizes;
import discUtils.vhd.ParentLocator;
import dotnet4j.io.MemoryStream;


/**
 * Creates new VHD disks by wrapping existing streams.
 * Using this method for creating virtual disks avoids consuming
 * large amounts of memory, or going via the local file system when the aim
 * is simply to present a VHD version of an existing disk.
 */
public final class DiskBuilder extends DiskImageBuilder {

    private long blockSize = 32 * Sizes.OneMiB;

    /**
     * The VHDX block size, or
     * {@code 0}
     * (indicating default).
     */
    public long getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(long value) {
        if (value % Sizes.OneMiB != 0) {
            throw new IllegalArgumentException("BlockSize must be a multiple of 1MB");
        }

        blockSize = value;
    }

    /**
     * Gets or sets the type of VHDX file to build.
     */
    private DiskType diskType = DiskType.None;

    public DiskType getDiskType() {
        return diskType;
    }

    public void setDiskType(DiskType value) {
        diskType = value;
    }

    /**
     * Initiates the build process.
     *
     * @param baseName The base name for the VHDX, for example 'foo' to create
     *            'foo.vhdx'.
     * @return A set of one or more logical files that constitute the virtual
     *         disk. The first file is
     *         the 'primary' file that is normally attached to VMs.
     */
    @Override
    public List<DiskImageFileSpecification> build(String baseName) {
        if (baseName == null || baseName.isEmpty()) {
            throw new IllegalArgumentException("Invalid base file name");
        }

        if (getContent() == null) {
            throw new IllegalStateException("No content stream specified");
        }

        DiskImageFileSpecification fileSpec = new DiskImageFileSpecification(baseName + ".vhdx",
                                                                             new DiskStreamBuilder(getContent(),
                                                                                                   getDiskType(),
                                                                                                   getBlockSize()));
        return Collections.singletonList(fileSpec);
    }

    private static class DiskStreamBuilder extends StreamBuilder {

        private final long blockSize;

        private final SparseStream content;

        private final DiskType diskType;

        public DiskStreamBuilder(SparseStream content, DiskType diskType, long blockSize) {
            this.content = content;
            this.diskType = diskType;
            this.blockSize = blockSize;
        }

        /**
         * @param totalLength {@cs out}
         */
        @Override
        protected List<BuilderExtent> fixExtents(long[] totalLength) {
            if (diskType != DiskType.Dynamic) {
                throw new UnsupportedOperationException("Creation of only dynamic disks currently implemented");
            }

            List<BuilderExtent> extents = new ArrayList<>();

            int logicalSectorSize = 512;
            int physicalSectorSize = 4096;
            long chunkRatio = 0x800000L * logicalSectorSize / blockSize;
            long dataBlocksCount = MathUtilities.ceil(content.getLength(), blockSize);
            @SuppressWarnings("unused")
            long sectorBitmapBlocksCount = MathUtilities.ceil(dataBlocksCount, chunkRatio);
            long totalBatEntriesDynamic = dataBlocksCount + (dataBlocksCount - 1) / chunkRatio;

            FileHeader fileHeader = new FileHeader();
            fileHeader.creator = ".NET DiscUtils";

            long fileEnd = Sizes.OneMiB;

            VhdxHeader header1 = new VhdxHeader();
            header1.sequenceNumber = 0;
            header1.fileWriteGuid = UUID.randomUUID();
            header1.dataWriteGuid = UUID.randomUUID();
            header1.logGuid = new UUID(0L, 0L);
            header1.logVersion = 0;
            header1.version = 1;
            header1.logLength = (int) Sizes.OneMiB;
            header1.logOffset = fileEnd;
            header1.calcChecksum();

            fileEnd += header1.logLength;

            VhdxHeader header2 = new VhdxHeader(header1);
            header2.sequenceNumber = 1;
            header2.calcChecksum();

            RegionTable regionTable = new RegionTable();

            RegionEntry metadataRegion = new RegionEntry();
            metadataRegion.guid = RegionEntry.MetadataRegionGuid;
            metadataRegion.fileOffset = fileEnd;
            metadataRegion.setLength((int) Sizes.OneMiB);
            metadataRegion.flags = RegionFlags.Required;
            regionTable.regions.put(metadataRegion.guid, metadataRegion);

            fileEnd += metadataRegion.getLength();

            RegionEntry batRegion = new RegionEntry();
            batRegion.guid = RegionEntry.BatGuid;
            batRegion.fileOffset = fileEnd;
            batRegion.setLength((int) MathUtilities.roundUp(totalBatEntriesDynamic * 8, Sizes.OneMiB));
            batRegion.flags = RegionFlags.Required;
            regionTable.regions.put(batRegion.guid, batRegion);

            fileEnd += batRegion.getLength();

            extents.add(extentForStruct(fileHeader, 0));
            extents.add(extentForStruct(header1, 64 * Sizes.OneKiB));
            extents.add(extentForStruct(header2, 128 * Sizes.OneKiB));
            extents.add(extentForStruct(regionTable, 192 * Sizes.OneKiB));
            extents.add(extentForStruct(regionTable, 256 * Sizes.OneKiB));

            // Metadata
            FileParameters fileParams = new FileParameters();
            fileParams.blockSize = (int) blockSize;
            fileParams.flags = EnumSet.of(FileParametersFlags.None);
            @SuppressWarnings("unused")
            ParentLocator parentLocator = new ParentLocator();

            byte[] metadataBuffer = new byte[(int) metadataRegion.getLength()];
            MemoryStream metadataStream = new MemoryStream(metadataBuffer);
            Metadata.initialize(metadataStream,
                                fileParams,
                                content.getLength(),
                                logicalSectorSize,
                                physicalSectorSize,
                                null);
            extents.add(new BuilderBufferExtent(metadataRegion.fileOffset, metadataBuffer));
            List<Range> presentBlocks = StreamExtent.blocks(content.getExtents(), blockSize);

            // BAT
            BlockAllocationTableBuilderExtent batExtent = new BlockAllocationTableBuilderExtent(batRegion.fileOffset,
                                                                                                batRegion.getLength(),
                                                                                                presentBlocks,
                                                                                                fileEnd,
                    blockSize,
                                                                                                chunkRatio);
            extents.add(batExtent);

            // Stream contents
            for (Range range : presentBlocks) {
                long substreamStart = range.getOffset() * blockSize;
                long substreamCount = Math.min(content.getLength() - substreamStart, range.getCount() * blockSize);
                SubStream dataSubStream = new SubStream(content, substreamStart, substreamCount);
                BuilderSparseStreamExtent dataExtent = new BuilderSparseStreamExtent(fileEnd, dataSubStream);
                extents.add(dataExtent);
                fileEnd += range.getCount() * blockSize;
            }

            totalLength[0] = fileEnd;

            return extents;
        }

        private static BuilderExtent extentForStruct(IByteArraySerializable structure, long position) {
            byte[] buffer = new byte[structure.size()];
            structure.writeTo(buffer, 0);
            return new BuilderBufferExtent(position, buffer);
        }
    }

    private static class BlockAllocationTableBuilderExtent extends BuilderExtent {

        private byte[] batData;

        private final List<Range> blocks;

        private final long blockSize;

        private final long chunkRatio;

        private final long dataStart;

        public BlockAllocationTableBuilderExtent(long start,
                long length,
                List<Range> blocks,
                long dataStart,
                long blockSize,
                long chunkRatio) {
            super(start, length);
            this.blocks = blocks;
            this.dataStart = dataStart;
            this.blockSize = blockSize;
            this.chunkRatio = chunkRatio;
        }

        @Override
        public void close() {
            batData = null;
        }

        @Override
        public void prepareForRead() {
            batData = new byte[(int) getLength()];
            long fileOffset = dataStart;
            BatEntry entry = new BatEntry();
            for (Range range : blocks) {
                for (long block = range.getOffset(); block < range.getOffset() + range.getCount(); ++block) {
                    long chunk = block / chunkRatio;
                    long chunkOffset = block % chunkRatio;
                    long batIndex = chunk * (chunkRatio + 1) + chunkOffset;
                    entry.setFileOffsetMB(fileOffset / Sizes.OneMiB);
                    entry.setPayloadBlockStatus(PayloadBlockStatus.FullyPresent);
                    entry.writeTo(batData, (int) (batIndex * 8));
                    fileOffset += blockSize;
                }
            }
        }

        @Override
        public int read(long diskOffset, byte[] block, int offset, int count) {
            int start = (int) Math.min(diskOffset - getStart(), batData.length);
            int numRead = Math.min(count, batData.length - start);
            System.arraycopy(batData, start, block, offset, numRead);
            return numRead;
        }

        @Override
        public void disposeReadState() {
            batData = null;
        }
    }
}
