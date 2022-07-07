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
    private final SparseStream _content;

    public VmfsSparseExtentBuilder(SparseStream content) {
        _content = content;
    }

    /**
     * @param totalLength {@cs out}
     */
    protected List<BuilderExtent> fixExtents(long[] totalLength) {
        List<BuilderExtent> extents = new ArrayList<>();

        ServerSparseExtentHeader header = DiskImageFile.createServerSparseExtentHeader(_content.getLength());
        GlobalDirectoryExtent gdExtent = new GlobalDirectoryExtent(header);

        long grainTableStart = header.GdOffset * Sizes.Sector + gdExtent.getLength();
        long grainTableCoverage = header.NumGTEsPerGT * header.GrainSize * Sizes.Sector;

        for (Range grainTableRange : StreamExtent.blocks(_content.getExtents(), grainTableCoverage)) {
            for (int i = 0; i < grainTableRange.getCount(); ++i) {
                long grainTable = grainTableRange.getOffset() + i;
                long dataStart = grainTable * grainTableCoverage;
                GrainTableExtent gtExtent = new GrainTableExtent(grainTableStart,
                                                                 new SubStream(_content,
                                                                               dataStart,
                                                                               Math.min(grainTableCoverage,
                                                                                        _content.getLength() - dataStart)),
                                                                 header);
                extents.add(gtExtent);
                gdExtent.setEntry((int) grainTable, (int) (grainTableStart / Sizes.Sector));

                grainTableStart += gtExtent.getLength();
            }
        }

        extents.add(0, gdExtent);

        header.FreeSector = (int) (grainTableStart / Sizes.Sector);

        byte[] buffer = header.getBytes();
        extents.add(0, new BuilderBufferExtent(0, buffer));

        totalLength[0] = grainTableStart;

        return extents;
    }

    private static class GlobalDirectoryExtent extends BuilderExtent {
        private final byte[] _buffer;

        private MemoryStream _streamView;

        public GlobalDirectoryExtent(ServerSparseExtentHeader header) {
            super(header.GdOffset * Sizes.Sector, MathUtilities.roundUp(header.NumGdEntries * 4, Sizes.Sector));
            _buffer = new byte[(int) getLength()];
        }

        public void close() throws IOException {
            if (_streamView != null) {
                _streamView.close();
                _streamView = null;
            }

        }

        public void setEntry(int index, int grainTableSector) {
            EndianUtilities.writeBytesLittleEndian(grainTableSector, _buffer, index * 4);
        }

        public void prepareForRead() {
            _streamView = new MemoryStream(_buffer, false);
        }

        public int read(long diskOffset, byte[] block, int offset, int count) {
            _streamView.setPosition(diskOffset - getStart());
            return _streamView.read(block, offset, count);
        }

        public void disposeReadState() {
            if (_streamView != null) {
                try {
                    _streamView.close();
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
                _streamView = null;
            }

        }

    }

    private static class GrainTableExtent extends BuilderExtent {
        private SparseStream _content;

        private final Ownership _contentOwnership;

        private List<Long> _grainContiguousRangeMapping;

        private List<Long> _grainMapping;

        private MemoryStream _grainTableStream;

        private final ServerSparseExtentHeader _header;

        public GrainTableExtent(long outputStart, SparseStream content, ServerSparseExtentHeader header) {
            this(outputStart, content, Ownership.None, header);
        }

        public GrainTableExtent(long outputStart,
                SparseStream content,
                Ownership contentOwnership,
                ServerSparseExtentHeader header) {
            super(outputStart, calcSize(content, header));
            _content = content;
            _contentOwnership = contentOwnership;
            _header = header;
        }

        public void close() throws IOException {
            if (_content != null && _contentOwnership == Ownership.Dispose) {
                _content.close();
                _content = null;
            }

            if (_grainTableStream != null) {
                _grainTableStream.close();
                _grainTableStream = null;
            }
        }

        public void prepareForRead() {
            byte[] grainTable = new byte[MathUtilities.roundUp(_header.NumGTEsPerGT * 4, Sizes.Sector)];
            long dataSector = (getStart() + grainTable.length) / Sizes.Sector;
            _grainMapping = new ArrayList<>();
            _grainContiguousRangeMapping = new ArrayList<>();
            for (Range grainRange : StreamExtent.blocks(_content.getExtents(), _header.GrainSize * Sizes.Sector)) {
                for (int i = 0; i < grainRange.getCount(); ++i) {
                    EndianUtilities
                            .writeBytesLittleEndian((int) dataSector, grainTable, (int) (4 * (grainRange.getOffset() + i)));
                    dataSector += _header.GrainSize;
                    _grainMapping.add(grainRange.getOffset() + i);
                    _grainContiguousRangeMapping.add(grainRange.getCount() - i);
                }
            }
            _grainTableStream = new MemoryStream(grainTable, false);
        }

        public int read(long diskOffset, byte[] block, int offset, int count) {
            long relOffset = diskOffset - getStart();
            if (relOffset < _grainTableStream.getLength()) {
                _grainTableStream.setPosition(relOffset);
                return _grainTableStream.read(block, offset, count);
            }

            long grainSize = _header.GrainSize * Sizes.Sector;
            int grainIdx = (int) ((relOffset - _grainTableStream.getLength()) / grainSize);
            long grainOffset = relOffset - _grainTableStream.getLength() - grainIdx * grainSize;
            int maxToRead = (int) Math.min(count, grainSize * _grainContiguousRangeMapping.get(grainIdx) - grainOffset);
            _content.setPosition(_grainMapping.get(grainIdx) * grainSize + grainOffset);
            return _content.read(block, offset, maxToRead);
        }

        public void disposeReadState() {
            if (_grainTableStream != null) {
                try {
                    _grainTableStream.close();
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
                _grainTableStream = null;
            }

            _grainMapping = null;
            _grainContiguousRangeMapping = null;
        }

        private static long calcSize(SparseStream content, ServerSparseExtentHeader header) {
            long numDataGrains = StreamExtent.blockCount(content.getExtents(), header.GrainSize * Sizes.Sector);
            long grainTableSectors = MathUtilities.ceil(header.NumGTEsPerGT * 4, Sizes.Sector);
            return (grainTableSectors + numDataGrains * header.GrainSize) * Sizes.Sector;
        }

    }

}
