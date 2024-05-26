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

package discUtils.ntfs;

import java.util.ArrayList;
import java.util.List;

import discUtils.streams.StreamExtent;
import discUtils.streams.buffer.Buffer;
import discUtils.streams.buffer.IMappedBuffer;
import discUtils.streams.util.Range;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.IOException;


public class NonResidentDataBuffer extends Buffer implements IMappedBuffer {

    protected ClusterStream activeStream;

    protected long bytesPerCluster;

    protected INtfsContext context;

    protected CookedDataRuns cookedRuns;

    protected byte[] ioBuffer;

    protected RawClusterStream rawStream;

    public NonResidentDataBuffer(INtfsContext context, NonResidentAttributeRecord record) {
        this(context, new CookedDataRuns(record.getDataRuns(), record), false);
    }

    public NonResidentDataBuffer(INtfsContext context, CookedDataRuns cookedRuns, boolean isMft) {
        this.context = context;
        this.cookedRuns = cookedRuns;

        rawStream = new RawClusterStream(this.context, this.cookedRuns, isMft);
        activeStream = rawStream;

        bytesPerCluster = this.context.getBiosParameterBlock().getBytesPerCluster();
        ioBuffer = new byte[(int) bytesPerCluster];
    }

    public long getVirtualClusterCount() {
        return cookedRuns.getNextVirtualCluster();
    }

    @Override public boolean canRead() {
        return context.getRawStream().canRead();
    }

    @Override public boolean canWrite() {
        return false;
    }

    @Override public long getCapacity() {
        return getVirtualClusterCount() * bytesPerCluster;
    }

    @Override public List<StreamExtent> getExtents() {
        List<StreamExtent> extents = new ArrayList<>();
        for (Range range : activeStream.getStoredClusters()) {
            extents.add(new StreamExtent(range.getOffset() * bytesPerCluster, range.getCount() * bytesPerCluster));
        }
//logger.log(Level.DEBUG, extents + ", " + new StreamExtent(0, getCapacity()));
        return StreamExtent.intersect(extents, new StreamExtent(0, getCapacity()));
    }

    @Override public List<StreamExtent> getExtentsInRange(long start, long count) {
//logger.log(Level.DEBUG, getExtents() + ", " + new StreamExtent(start, count));
        return StreamExtent.intersect(getExtents(), new StreamExtent(start, count));
    }

    @Override public long mapPosition(long pos) {
        long vcn = pos / bytesPerCluster;
        int dataRunIdx = cookedRuns.findDataRun(vcn, 0);

        if (cookedRuns.get(dataRunIdx).isSparse()) {
            return -1;
        }
        return cookedRuns.get(dataRunIdx).getStartLcn() * bytesPerCluster +
               (pos - cookedRuns.get(dataRunIdx).getStartVcn() * bytesPerCluster);
    }

    @Override public int read(long pos, byte[] buffer, int offset, int count) {
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
            long vcn = focusPos / bytesPerCluster;
            long remaining = pos + totalToRead - focusPos;
            long clusterOffset = focusPos - vcn * bytesPerCluster;

            if (vcn * bytesPerCluster != focusPos || remaining < bytesPerCluster) {
                // Unaligned or short read
                activeStream.readClusters(vcn, 1, ioBuffer, 0);

                int toRead = (int) Math.min(remaining, bytesPerCluster - clusterOffset);

                System.arraycopy(ioBuffer, (int) clusterOffset, buffer, (int) (offset + (focusPos - pos)), toRead);

                focusPos += toRead;
            } else {
                // Aligned, full cluster reads...
                int fullClusters = (int) (remaining / bytesPerCluster);
                activeStream.readClusters(vcn, fullClusters, buffer, (int) (offset + (focusPos - pos)));

                focusPos += fullClusters * bytesPerCluster;
            }
        }

        return totalToRead;
    }

    @Override public void write(long pos, byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    @Override public void setCapacity(long value) {
        throw new UnsupportedOperationException();
    }
}
