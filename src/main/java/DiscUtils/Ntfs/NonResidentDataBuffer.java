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

package DiscUtils.Ntfs;

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Buffer.Buffer;
import DiscUtils.Streams.Buffer.IMappedBuffer;
import DiscUtils.Streams.Util.Range;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.IOException;


public class NonResidentDataBuffer extends Buffer implements IMappedBuffer {
    protected ClusterStream _activeStream;

    protected long _bytesPerCluster;

    protected INtfsContext _context;

    protected CookedDataRuns _cookedRuns;

    protected byte[] _ioBuffer;

    protected RawClusterStream _rawStream;

    public NonResidentDataBuffer(INtfsContext context, NonResidentAttributeRecord record) {
        this(context, new CookedDataRuns(record.getDataRuns(), record), false);
    }

    public NonResidentDataBuffer(INtfsContext context, CookedDataRuns cookedRuns, boolean isMft) {
        _context = context;
        _cookedRuns = cookedRuns;

        _rawStream = new RawClusterStream(_context, _cookedRuns, isMft);
        _activeStream = _rawStream;

        _bytesPerCluster = _context.getBiosParameterBlock().getBytesPerCluster();
        _ioBuffer = new byte[(int) _bytesPerCluster];
    }

    public long getVirtualClusterCount() {
        return _cookedRuns.getNextVirtualCluster();
    }

    public boolean canRead() {
        return _context.getRawStream().canRead();
    }

    public boolean canWrite() {
        return false;
    }

    public long getCapacity() {
        return getVirtualClusterCount() * _bytesPerCluster;
    }

    public List<StreamExtent> getExtents() {
        List<StreamExtent> extents = new ArrayList<>();
        for (Range range : _activeStream.getStoredClusters()) {
            extents.add(new StreamExtent(range.getOffset() * _bytesPerCluster, range.getCount() * _bytesPerCluster));
        }
//Debug.println(extents + ", " + new StreamExtent(0, getCapacity()));
        return StreamExtent.intersect(extents, new StreamExtent(0, getCapacity()));
    }

    public List<StreamExtent> getExtentsInRange(long start, long count) {
//Debug.println(getExtents() + ", " + new StreamExtent(start, count));
        return StreamExtent.intersect(getExtents(), new StreamExtent(start, count));
    }

    public long mapPosition(long pos) {
        long vcn = pos / _bytesPerCluster;
        int dataRunIdx = _cookedRuns.findDataRun(vcn, 0);

        if (_cookedRuns.get___idx(dataRunIdx).isSparse()) {
            return -1;
        }
        return _cookedRuns.get___idx(dataRunIdx).getStartLcn() * _bytesPerCluster +
               (pos - _cookedRuns.get___idx(dataRunIdx).getStartVcn() * _bytesPerCluster);
    }

    public int read(long pos, byte[] buffer, int offset, int count) {
        if (!canRead()) {
            throw new IOException("Attempt to read from file not opened for read");
        }

        StreamUtilities.assertBufferParameters(buffer, offset, count);

        // Limit read to length of attribute
        int totalToRead = (int) Math.min(count, getCapacity() - pos);
        if (totalToRead <= 0) {
            return 0;
        }

        long focusPos = pos;
        while (focusPos < pos + totalToRead) {
            long vcn = focusPos / _bytesPerCluster;
            long remaining = pos + totalToRead - focusPos;
            long clusterOffset = focusPos - vcn * _bytesPerCluster;

            if (vcn * _bytesPerCluster != focusPos || remaining < _bytesPerCluster) {
                // Unaligned or short read
                _activeStream.readClusters(vcn, 1, _ioBuffer, 0);

                int toRead = (int) Math.min(remaining, _bytesPerCluster - clusterOffset);

                System.arraycopy(_ioBuffer, (int) clusterOffset, buffer, (int) (offset + (focusPos - pos)), toRead);

                focusPos += toRead;
            } else {
                // Aligned, full cluster reads...
                int fullClusters = (int) (remaining / _bytesPerCluster);
                _activeStream.readClusters(vcn, fullClusters, buffer, (int) (offset + (focusPos - pos)));

                focusPos += fullClusters * _bytesPerCluster;
            }
        }

        return totalToRead;
    }

    public void write(long pos, byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    public void setCapacity(long value) {
        throw new UnsupportedOperationException();
    }
}
