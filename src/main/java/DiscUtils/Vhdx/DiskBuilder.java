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

package DiscUtils.Vhdx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import DiscUtils.Core.DiskImageBuilder;
import DiscUtils.Core.DiskImageFileSpecification;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.Builder.BuilderBufferExtent;
import DiscUtils.Streams.Builder.BuilderExtent;
import DiscUtils.Streams.Builder.BuilderSparseStreamExtent;
import DiscUtils.Streams.Builder.StreamBuilder;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Range;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Vhd.ParentLocator;
import moe.yo3explorer.dotnetio4j.MemoryStream;


/**
 * Creates new VHD disks by wrapping existing streams.
 * Using this method for creating virtual disks avoids consuming
 * large amounts of memory, or going via the local file system when the aim
 * is simply to present a VHD version of an existing disk.
 */
public final class DiskBuilder extends DiskImageBuilder {
    private long _blockSize = 32 * Sizes.OneMiB;

    /**
     * The VHDX block size, or
     * {@code 0}
     * (indicating default).
     */
    public long getBlockSize() {
        return _blockSize;
    }

    public void setBlockSize(long value) {
        if (value % Sizes.OneMiB != 0) {
            throw new IllegalArgumentException("BlockSize must be a multiple of 1MB");
        }

        _blockSize = value;
    }

    /**
     * Gets or sets the type of VHDX file to build.
     */
    private DiskType __DiskType = DiskType.None;

    public DiskType getDiskType() {
        return __DiskType;
    }

    public void setDiskType(DiskType value) {
        __DiskType = value;
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
        return Arrays.asList(fileSpec);
    }

    private static class DiskStreamBuilder extends StreamBuilder {
        private final long _blockSize;

        private final SparseStream _content;

        private final DiskType _diskType;

        public DiskStreamBuilder(SparseStream content, DiskType diskType, long blockSize) {
            _content = content;
            _diskType = diskType;
            _blockSize = blockSize;
        }

        protected List<BuilderExtent> fixExtents(long[] totalLength) {
            if (_diskType != DiskType.Dynamic) {
                throw new UnsupportedOperationException("Creation of only dynamic disks currently implemented");
            }

            List<BuilderExtent> extents = new ArrayList<>();
            int logicalSectorSize = 512;
            int physicalSectorSize = 4096;
            long chunkRatio = 0x800000L * logicalSectorSize / _blockSize;
            long dataBlocksCount = MathUtilities.ceil(_content.getLength(), _blockSize);
            long sectorBitmapBlocksCount = MathUtilities.ceil(dataBlocksCount, chunkRatio);
            long totalBatEntriesDynamic = dataBlocksCount + (dataBlocksCount - 1) / chunkRatio;
            FileHeader fileHeader = new FileHeader();
            long fileEnd = Sizes.OneMiB;
            VhdxHeader header1 = new VhdxHeader();
            header1.SequenceNumber = 0;
            header1.FileWriteGuid = UUID.randomUUID();
            header1.DataWriteGuid = UUID.randomUUID();
            header1.LogGuid = UUID.fromString("");
            header1.LogVersion = 0;
            header1.Version = 1;
            header1.LogLength = (int) Sizes.OneMiB;
            header1.LogOffset = fileEnd;
            header1.calcChecksum();
            fileEnd += header1.LogLength;
            VhdxHeader header2 = new VhdxHeader(header1);
            header2.SequenceNumber = 1;
            header2.calcChecksum();
            RegionTable regionTable = new RegionTable();
            RegionEntry metadataRegion = new RegionEntry();
            metadataRegion.Guid = RegionEntry.MetadataRegionGuid;
            metadataRegion.FileOffset = fileEnd;
            metadataRegion.Length = (int) Sizes.OneMiB;
            metadataRegion.Flags = RegionFlags.Required;
            regionTable.Regions.put(metadataRegion.Guid, metadataRegion);
            fileEnd += metadataRegion.Length;
            RegionEntry batRegion = new RegionEntry();
            batRegion.Guid = RegionEntry.BatGuid;
            batRegion.FileOffset = fileEnd;
            batRegion.Length = (int) MathUtilities.roundUp(totalBatEntriesDynamic * 8, Sizes.OneMiB);
            batRegion.Flags = RegionFlags.Required;
            regionTable.Regions.put(batRegion.Guid, batRegion);
            fileEnd += batRegion.Length;
            extents.add(extentForStruct(fileHeader, 0));
            extents.add(extentForStruct(header1, 64 * Sizes.OneKiB));
            extents.add(extentForStruct(header2, 128 * Sizes.OneKiB));
            extents.add(extentForStruct(regionTable, 192 * Sizes.OneKiB));
            extents.add(extentForStruct(regionTable, 256 * Sizes.OneKiB));
            // Metadata
            FileParameters fileParams = new FileParameters();
            ParentLocator parentLocator = new ParentLocator();
            byte[] metadataBuffer = new byte[metadataRegion.Length];
            MemoryStream metadataStream = new MemoryStream(metadataBuffer);
            Metadata.initialize(metadataStream,
                                fileParams,
                                _content.getLength(),
                                logicalSectorSize,
                                physicalSectorSize,
                                null);
            extents.add(new BuilderBufferExtent(metadataRegion.FileOffset, metadataBuffer));
            List<Range> presentBlocks = new ArrayList<>(StreamExtent.blocks(_content.getExtents(), _blockSize));
            // BAT
            BlockAllocationTableBuilderExtent batExtent = new BlockAllocationTableBuilderExtent(batRegion.FileOffset,
                                                                                                batRegion.Length,
                                                                                                presentBlocks,
                                                                                                fileEnd,
                                                                                                _blockSize,
                                                                                                chunkRatio);
            extents.add(batExtent);
            for (Object __dummyForeachVar0 : presentBlocks) {
                // Stream contents
                Range range = (Range) __dummyForeachVar0;
                long substreamStart = range.getOffset() * _blockSize;
                long substreamCount = Math.min(_content.getLength() - substreamStart, range.getCount() * _blockSize);
                SubStream dataSubStream = new SubStream(_content, substreamStart, substreamCount);
                BuilderSparseStreamExtent dataExtent = new BuilderSparseStreamExtent(fileEnd, dataSubStream);
                extents.add(dataExtent);
                fileEnd += range.getCount() * _blockSize;
            }
            totalLength[0] = fileEnd;
            return extents;
        }

        private static BuilderExtent extentForStruct(IByteArraySerializable structure, long position) {
            byte[] buffer = new byte[structure.getSize()];
            structure.writeTo(buffer, 0);
            return new BuilderBufferExtent(position, buffer);
        }

    }

    private static class BlockAllocationTableBuilderExtent extends BuilderExtent {
        private byte[] _batData;

        private final List<Range> _blocks;

        private final long _blockSize;

        private final long _chunkRatio;

        private final long _dataStart;

        public BlockAllocationTableBuilderExtent(long start,
                long length,
                List<Range> blocks,
                long dataStart,
                long blockSize,
                long chunkRatio) {
            super(start, length);
            _blocks = blocks;
            _dataStart = dataStart;
            _blockSize = blockSize;
            _chunkRatio = chunkRatio;
        }

        public void close() {
            _batData = null;
        }

        public void prepareForRead() {
            _batData = new byte[(int) getLength()];
            long fileOffset = _dataStart;
            BatEntry entry = new BatEntry();
            for (Range range : _blocks) {
                for (long block = range.getOffset(); block < range.getOffset() + range.getCount(); ++block) {
                    long chunk = block / _chunkRatio;
                    long chunkOffset = block % _chunkRatio;
                    long batIndex = chunk * (_chunkRatio + 1) + chunkOffset;
                    entry.setFileOffsetMB(fileOffset / Sizes.OneMiB);
                    entry.setPayloadBlockStatus(PayloadBlockStatus.FullyPresent);
                    entry.writeTo(_batData, (int) (batIndex * 8));
                    fileOffset += _blockSize;
                }
            }
        }

        public int read(long diskOffset, byte[] block, int offset, int count) {
            int start = (int) Math.min(diskOffset - getStart(), _batData.length);
            int numRead = Math.min(count, _batData.length - start);
            System.arraycopy(_batData, start, block, offset, numRead);
            return numRead;
        }

        public void disposeReadState() {
            _batData = null;
        }

    }

}
