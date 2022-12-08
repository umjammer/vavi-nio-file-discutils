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
import java.util.Arrays;
import java.util.List;

import discUtils.streams.util.Range;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.util.compat.Tuple;
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
 * <li> compression / Decompression
 * <li> Extending attributes.
 * </ul>
 */
final class RawClusterStream extends ClusterStream {

    private final int bytesPerCluster;

    private final INtfsContext context;

    private final CookedDataRuns cookedRuns;

    private final Stream fsStream;

    private final boolean isMft;

    public RawClusterStream(INtfsContext context, CookedDataRuns cookedRuns, boolean isMft) {
        this.context = context;
        this.cookedRuns = cookedRuns;
        this.isMft = isMft;

        fsStream = this.context.getRawStream();
        bytesPerCluster = context.getBiosParameterBlock().getBytesPerCluster();
    }

    public long getAllocatedClusterCount() {
        long total = 0;
        for (int i = 0; i < cookedRuns.getCount(); ++i) {
            CookedDataRun run = cookedRuns.get(i);
            total += run.isSparse() ? 0 : run.getLength();
        }

        return total;
    }

    public List<Range> getStoredClusters() {
        Range lastVcnRange = null;
        List<Range> ranges = new ArrayList<>();

        int runCount = cookedRuns.getCount();
        for (int i = 0; i < runCount; i++) {
            CookedDataRun cookedRun = cookedRuns.get(i);
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
        int runIdx = cookedRuns.findDataRun(vcn, 0);
        return !cookedRuns.get(runIdx).isSparse();
    }

    public boolean areAllClustersStored(long vcn, int count) {
        int runIdx = 0;
        long focusVcn = vcn;
        while (focusVcn < vcn + count) {
            runIdx = cookedRuns.findDataRun(focusVcn, runIdx);

            CookedDataRun run = cookedRuns.get(runIdx);
            if (run.isSparse()) {
                return false;
            }

            focusVcn = run.getStartVcn() + run.getLength();
        }

        return true;
    }

    public void expandToClusters(long numVirtualClusters, NonResidentAttributeRecord extent, boolean allocate) {
        long totalVirtualClusters = cookedRuns.getNextVirtualCluster();
        if (totalVirtualClusters < numVirtualClusters) {
            NonResidentAttributeRecord realExtent = extent;
            if (realExtent == null) {
                realExtent = cookedRuns.getLast().getAttributeExtent();
            }

            DataRun newRun = new DataRun(0, numVirtualClusters - totalVirtualClusters, true);
            realExtent.getDataRuns().add(newRun);
            cookedRuns.append(newRun, extent);
            realExtent.setLastVcn(numVirtualClusters - 1);
        }

        if (allocate) {
            allocateClusters(totalVirtualClusters, (int) (numVirtualClusters - totalVirtualClusters));
        }
    }

    public void truncateToClusters(long numVirtualClusters) {
        if (numVirtualClusters < cookedRuns.getNextVirtualCluster()) {
            releaseClusters(numVirtualClusters, (int) (cookedRuns.getNextVirtualCluster() - numVirtualClusters));

            int runIdx = cookedRuns.findDataRun(numVirtualClusters, 0);

            if (numVirtualClusters != cookedRuns.get(runIdx).getStartVcn()) {
                cookedRuns.splitRun(runIdx, numVirtualClusters);
                runIdx++;
            }

            cookedRuns.truncateAt(runIdx);
        }
    }

    public int allocateClusters(long startVcn, int count) {
        if (startVcn + count > cookedRuns.getNextVirtualCluster()) {
            throw new IOException("Attempt to allocate unknown clusters");
        }

        int totalAllocated = 0;
        int runIdx = 0;

        long focus = startVcn;
        while (focus < startVcn + count) {
            runIdx = cookedRuns.findDataRun(focus, runIdx);
            CookedDataRun run = cookedRuns.get(runIdx);

            if (run.isSparse()) {
                if (focus != run.getStartVcn()) {
                    cookedRuns.splitRun(runIdx, focus);
                    runIdx++;
                    run = cookedRuns.get(runIdx);
                }

                long numClusters = Math.min(startVcn + count - focus, run.getLength());
                if (numClusters != run.getLength()) {
                    cookedRuns.splitRun(runIdx, focus + numClusters);
                    run = cookedRuns.get(runIdx);
                }

                long nextCluster = -1;
                for (int i = runIdx - 1; i >= 0; --i) {
                    if (!cookedRuns.get(i).isSparse()) {
                        nextCluster = cookedRuns.get(i).getStartLcn() + cookedRuns.get(i).getLength();
                        break;
                    }
                }

                List<Tuple<Long, Long>> alloced = context.getClusterBitmap()
                        .allocateClusters(numClusters, nextCluster, isMft, getAllocatedClusterCount());

                List<DataRun> runs = new ArrayList<>();

                long lcn = runIdx == 0 ? 0 : cookedRuns.get(runIdx - 1).getStartLcn();
                for (Tuple<Long, Long> allocation : alloced) {
                    runs.add(new DataRun(allocation.getItem1() - lcn, allocation.getItem2(), false));
                    lcn = allocation.getItem1();
                }

                cookedRuns.makeNonSparse(runIdx, runs);

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
            runIdx = cookedRuns.findDataRun(focus, runIdx);
            CookedDataRun run = cookedRuns.get(runIdx);

            if (run.isSparse()) {
                focus += run.getLength();
            } else {
                if (focus != run.getStartVcn()) {
                    cookedRuns.splitRun(runIdx, focus);
                    runIdx++;
                    run = cookedRuns.get(runIdx);
                }

                long numClusters = Math.min(startVcn + count - focus, run.getLength());
                if (numClusters != run.getLength()) {
                    cookedRuns.splitRun(runIdx, focus + numClusters);
                    run = cookedRuns.get(runIdx);
                }

                context.getClusterBitmap().freeClusters(new Range(run.getStartLcn(), run.getLength()));
                cookedRuns.makeSparse(runIdx);
                totalReleased += (int) run.getLength();

                focus += numClusters;
            }
        }

        return totalReleased;
    }

    public void readClusters(long startVcn, int count, byte[] buffer, int offset) {
        StreamUtilities.assertBufferParameters(buffer, offset, count * bytesPerCluster);

        int runIdx = 0;
        int totalRead = 0;
        while (totalRead < count) {
            long focusVcn = startVcn + totalRead;

            runIdx = cookedRuns.findDataRun(focusVcn, runIdx);
            CookedDataRun run = cookedRuns.get(runIdx);

            int toRead = (int) Math.min(count - totalRead, run.getLength() - (focusVcn - run.getStartVcn()));

            if (run.isSparse()) {
                Arrays.fill(buffer,
                            offset + totalRead * bytesPerCluster,
                            offset + totalRead * bytesPerCluster + toRead * bytesPerCluster,
                            (byte) 0);
            } else {
                long lcn = cookedRuns.get(runIdx).getStartLcn() + (focusVcn - run.getStartVcn());
                fsStream.position(lcn * bytesPerCluster);
                StreamUtilities.readExact(fsStream, buffer, offset + totalRead * bytesPerCluster, toRead * bytesPerCluster);
            }

            totalRead += toRead;
        }
    }

    public int writeClusters(long startVcn, int count, byte[] buffer, int offset) {
        StreamUtilities.assertBufferParameters(buffer, offset, count * bytesPerCluster);

        int runIdx = 0;
        int totalWritten = 0;
        while (totalWritten < count) {
            long focusVcn = startVcn + totalWritten;

            runIdx = cookedRuns.findDataRun(focusVcn, runIdx);
            CookedDataRun run = cookedRuns.get(runIdx);

            if (run.isSparse()) {
                throw new UnsupportedOperationException("Writing to sparse datarun");
            }

            int toWrite = (int) Math.min(count - totalWritten, run.getLength() - (focusVcn - run.getStartVcn()));

            long lcn = cookedRuns.get(runIdx).getStartLcn() + (focusVcn - run.getStartVcn());
            fsStream.position(lcn * bytesPerCluster);
            fsStream.write(buffer, offset + totalWritten * bytesPerCluster, toWrite * bytesPerCluster);

            totalWritten += toWrite;
        }

        return 0;
    }

    public int clearClusters(long startVcn, int count) {
        byte[] zeroBuffer = new byte[16 * bytesPerCluster];

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
