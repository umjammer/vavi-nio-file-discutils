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

import java.util.List;

import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Range;


public final class SparseClusterStream extends ClusterStream {
    private final NtfsAttribute _attr;

    private final RawClusterStream _rawStream;

    public SparseClusterStream(NtfsAttribute attr, RawClusterStream rawStream) {
        _attr = attr;
        _rawStream = rawStream;
    }

    public long getAllocatedClusterCount() {
        return _rawStream.getAllocatedClusterCount();
    }

    public List<Range> getStoredClusters() {
        return _rawStream.getStoredClusters();
    }

    public boolean isClusterStored(long vcn) {
        return _rawStream.isClusterStored(vcn);
    }

    public void expandToClusters(long numVirtualClusters, NonResidentAttributeRecord extent, boolean allocate) {
        _rawStream.expandToClusters(compressionStart(numVirtualClusters), extent, false);
    }

    public void truncateToClusters(long numVirtualClusters) {
        long alignedNum = compressionStart(numVirtualClusters);
        _rawStream.truncateToClusters(alignedNum);
        if (alignedNum != numVirtualClusters) {
            _rawStream.releaseClusters(numVirtualClusters, (int) (alignedNum - numVirtualClusters));
        }

    }

    public void readClusters(long startVcn, int count, byte[] buffer, int offset) {
        _rawStream.readClusters(startVcn, count, buffer, offset);
    }

    public int writeClusters(long startVcn, int count, byte[] buffer, int offset) {
        int clustersAllocated = 0;
        clustersAllocated += _rawStream.allocateClusters(startVcn, count);
        clustersAllocated += _rawStream.writeClusters(startVcn, count, buffer, offset);
        return clustersAllocated;
    }

    public int clearClusters(long startVcn, int count) {
        return _rawStream.releaseClusters(startVcn, count);
    }

    private long compressionStart(long vcn) {
        return MathUtilities.roundUp(vcn, _attr.getCompressionUnitSize());
    }
}
