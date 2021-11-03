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
import java.util.Arrays;
import java.util.List;

import DiscUtils.Streams.Util.Range;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.Tuple;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


/**
 * Low-level non-resident attribute operations.
 * <p>
 * Responsible for:
 * <ul>
 * <li> Cluster Allocation / Release
 * <li> Reading clusters from disk
 * <li> Writing clusters to disk
 * <li> Substituting zeros for 'sparse'/'unallocated' clusters
 * </ul>
 * Not responsible for:
 * <ul>
 * <li> Compression / Decompression
 * <li> Extending attributes.
 * </ul>
 */
final class RawClusterStream extends ClusterStream {
    private final int _bytesPerCluster;

    private final INtfsContext _context;

    private final CookedDataRuns _cookedRuns;

    private final Stream _fsStream;

    private final boolean _isMft;

    public RawClusterStream(INtfsContext context, CookedDataRuns cookedRuns, boolean isMft) {
        _context = context;
        _cookedRuns = cookedRuns;
        _isMft = isMft;

        _fsStream = _context.getRawStream();
        _bytesPerCluster = context.getBiosParameterBlock().getBytesPerCluster();
    }

    public long getAllocatedClusterCount() {
        long total = 0;
        for (int i = 0; i < _cookedRuns.getCount(); ++i) {
            CookedDataRun run = _cookedRuns.get(i);
            total += run.isSparse() ? 0 : run.getLength();
        }

        return total;
    }

    public List<Range> getStoredClusters() {
        Range lastVcnRange = null;
        List<Range> ranges = new ArrayList<>();

        int runCount = _cookedRuns.getCount();
        for (int i = 0; i < runCount; i++) {
            CookedDataRun cookedRun = _cookedRuns.get(i);
            if (!cookedRun.isSparse()) {
                long startPos = cookedRun.getStartVcn();
                if (lastVcnRange != null && lastVcnRange.getOffset() + lastVcnRange.getCount() == startPos) {
                    lastVcnRange = new Range(lastVcnRange.getOffset(), lastVcnRange.getCount() + cookedRun.getLength());
                    ranges.set(ranges.size() - 1, lastVcnRange);
                } else {
                    lastVcnRange = new Range(cookedRun.getStartVcn(), cookedRun.getLength());
                    ranges.add(lastVcnRange);
                }
            }
        }

        return ranges;
    }

    public boolean isClusterStored(long vcn) {
        int runIdx = _cookedRuns.findDataRun(vcn, 0);
        return !_cookedRuns.get(runIdx).isSparse();
    }

    public boolean areAllClustersStored(long vcn, int count) {
        int runIdx = 0;
        long focusVcn = vcn;
        while (focusVcn < vcn + count) {
            runIdx = _cookedRuns.findDataRun(focusVcn, runIdx);

            CookedDataRun run = _cookedRuns.get(runIdx);
            if (run.isSparse()) {
                return false;
            }

            focusVcn = run.getStartVcn() + run.getLength();
        }

        return true;
    }

    public void expandToClusters(long numVirtualClusters, NonResidentAttributeRecord extent, boolean allocate) {
        long totalVirtualClusters = _cookedRuns.getNextVirtualCluster();
        if (totalVirtualClusters < numVirtualClusters) {
            NonResidentAttributeRecord realExtent = extent;
            if (realExtent == null) {
                realExtent = _cookedRuns.getLast().getAttributeExtent();
            }

            DataRun newRun = new DataRun(0, numVirtualClusters - totalVirtualClusters, true);
            realExtent.getDataRuns().add(newRun);
            _cookedRuns.append(newRun, extent);
            realExtent.setLastVcn(numVirtualClusters - 1);
        }

        if (allocate) {
            allocateClusters(totalVirtualClusters, (int) (numVirtualClusters - totalVirtualClusters));
        }
    }

    public void truncateToClusters(long numVirtualClusters) {
        if (numVirtualClusters < _cookedRuns.getNextVirtualCluster()) {
            releaseClusters(numVirtualClusters, (int) (_cookedRuns.getNextVirtualCluster() - numVirtualClusters));

            int runIdx = _cookedRuns.findDataRun(numVirtualClusters, 0);

            if (numVirtualClusters != _cookedRuns.get(runIdx).getStartVcn()) {
                _cookedRuns.splitRun(runIdx, numVirtualClusters);
                runIdx++;
            }

            _cookedRuns.truncateAt(runIdx);
        }
    }

    public int allocateClusters(long startVcn, int count) {
        if (startVcn + count > _cookedRuns.getNextVirtualCluster()) {
            throw new IOException("Attempt to allocate unknown clusters");
        }

        int totalAllocated = 0;
        int runIdx = 0;

        long focus = startVcn;
        while (focus < startVcn + count) {
            runIdx = _cookedRuns.findDataRun(focus, runIdx);
            CookedDataRun run = _cookedRuns.get(runIdx);

            if (run.isSparse()) {
                if (focus != run.getStartVcn()) {
                    _cookedRuns.splitRun(runIdx, focus);
                    runIdx++;
                    run = _cookedRuns.get(runIdx);
                }

                long numClusters = Math.min(startVcn + count - focus, run.getLength());
                if (numClusters != run.getLength()) {
                    _cookedRuns.splitRun(runIdx, focus + numClusters);
                    run = _cookedRuns.get(runIdx);
                }

                long nextCluster = -1;
                for (int i = runIdx - 1; i >= 0; --i) {
                    if (!_cookedRuns.get(i).isSparse()) {
                        nextCluster = _cookedRuns.get(i).getStartLcn() + _cookedRuns.get(i).getLength();
                        break;
                    }
                }

                List<Tuple<Long, Long>> alloced = _context.getClusterBitmap()
                        .allocateClusters(numClusters, nextCluster, _isMft, getAllocatedClusterCount());

                List<DataRun> runs = new ArrayList<>();

                long lcn = runIdx == 0 ? 0 : _cookedRuns.get(runIdx - 1).getStartLcn();
                for (Tuple<Long, Long> allocation : alloced) {
                    runs.add(new DataRun(allocation.Item1 - lcn, allocation.Item2, false));
                    lcn = allocation.Item1;
                }

                _cookedRuns.makeNonSparse(runIdx, runs);

                totalAllocated += (int) numClusters;
                focus += numClusters;
            } else {
                focus = run.getStartVcn() + run.getLength();
            }
        }

        return totalAllocated;
    }

    public int releaseClusters(long startVcn, int count) {
        int runIdx = 0;

        int totalReleased = 0;

        long focus = startVcn;
        while (focus < startVcn + count) {
            runIdx = _cookedRuns.findDataRun(focus, runIdx);
            CookedDataRun run = _cookedRuns.get(runIdx);

            if (run.isSparse()) {
                focus += run.getLength();
            } else {
                if (focus != run.getStartVcn()) {
                    _cookedRuns.splitRun(runIdx, focus);
                    runIdx++;
                    run = _cookedRuns.get(runIdx);
                }

                long numClusters = Math.min(startVcn + count - focus, run.getLength());
                if (numClusters != run.getLength()) {
                    _cookedRuns.splitRun(runIdx, focus + numClusters);
                    run = _cookedRuns.get(runIdx);
                }

                _context.getClusterBitmap().freeClusters(new Range(run.getStartLcn(), run.getLength()));
                _cookedRuns.makeSparse(runIdx);
                totalReleased += (int) run.getLength();

                focus += numClusters;
            }
        }

        return totalReleased;
    }

    public void readClusters(long startVcn, int count, byte[] buffer, int offset) {
        StreamUtilities.assertBufferParameters(buffer, offset, count * _bytesPerCluster);

        int runIdx = 0;
        int totalRead = 0;
        while (totalRead < count) {
            long focusVcn = startVcn + totalRead;

            runIdx = _cookedRuns.findDataRun(focusVcn, runIdx);
            CookedDataRun run = _cookedRuns.get(runIdx);

            int toRead = (int) Math.min(count - totalRead, run.getLength() - (focusVcn - run.getStartVcn()));

            if (run.isSparse()) {
                Arrays.fill(buffer,
                            offset + totalRead * _bytesPerCluster,
                            offset + totalRead * _bytesPerCluster + toRead * _bytesPerCluster,
                            (byte) 0);
            } else {
                long lcn = _cookedRuns.get(runIdx).getStartLcn() + (focusVcn - run.getStartVcn());
                _fsStream.setPosition(lcn * _bytesPerCluster);
                StreamUtilities.readExact(_fsStream, buffer, offset + totalRead * _bytesPerCluster, toRead * _bytesPerCluster);
            }

            totalRead += toRead;
        }
    }

    public int writeClusters(long startVcn, int count, byte[] buffer, int offset) {
        StreamUtilities.assertBufferParameters(buffer, offset, count * _bytesPerCluster);

        int runIdx = 0;
        int totalWritten = 0;
        while (totalWritten < count) {
            long focusVcn = startVcn + totalWritten;

            runIdx = _cookedRuns.findDataRun(focusVcn, runIdx);
            CookedDataRun run = _cookedRuns.get(runIdx);

            if (run.isSparse()) {
                throw new UnsupportedOperationException("Writing to sparse datarun");
            }

            int toWrite = (int) Math.min(count - totalWritten, run.getLength() - (focusVcn - run.getStartVcn()));

            long lcn = _cookedRuns.get(runIdx).getStartLcn() + (focusVcn - run.getStartVcn());
            _fsStream.setPosition(lcn * _bytesPerCluster);
            _fsStream.write(buffer, offset + totalWritten * _bytesPerCluster, toWrite * _bytesPerCluster);

            totalWritten += toWrite;
        }

        return 0;
    }

    public int clearClusters(long startVcn, int count) {
        byte[] zeroBuffer = new byte[16 * _bytesPerCluster];

        int clustersAllocated = 0;

        int numWritten = 0;
        while (numWritten < count) {
            int toWrite = Math.min(count - numWritten, 16);

            clustersAllocated += writeClusters(startVcn + numWritten, toWrite, zeroBuffer, 0);

            numWritten += toWrite;
        }

        return -clustersAllocated;
    }
}
