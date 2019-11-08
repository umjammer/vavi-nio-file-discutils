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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Range;
import dotnet4j.Tuple;
import dotnet4j.io.FileAccess;


class ClusterBitmap implements Closeable {
    private Bitmap _bitmap;

    private final File _file;

    private boolean _fragmentedDiskMode;

    private long _nextDataCluster;

    public ClusterBitmap(File file) {
        _file = file;
        _bitmap = new Bitmap(_file.openStream(AttributeType.Data, null, FileAccess.ReadWrite),
                             MathUtilities.ceil(file.getContext().getBiosParameterBlock()._totalSectors64,
                                                file.getContext().getBiosParameterBlock().getSectorsPerCluster()));
    }

    Bitmap getBitmap() {
        return _bitmap;
    }

    public void close() throws IOException {
        if (_bitmap != null) {
            _bitmap.close();
            _bitmap = null;
        }
    }

    /**
     * Allocates clusters from the disk.
     *
     * @param count The number of clusters to allocate.
     * @param proposedStart The proposed start cluster (or -1).
     * @param isMft {@code true} if this attribute is the $MFT\$DATA attribute.
     * @param total The total number of clusters in the file, including this
     *            allocation.
     * @return The list of cluster allocations.
     */
    public List<Tuple<Long, Long>> allocateClusters(long count, long proposedStart, boolean isMft, long total) {
        List<Tuple<Long, Long>> result = new ArrayList<>();

        long numFound = 0;

        long totalClusters = _file.getContext().getRawStream().getLength() /
                             _file.getContext().getBiosParameterBlock().getBytesPerCluster();

        if (isMft) {
            // First, try to extend the existing cluster run (if available)
            if (proposedStart >= 0) {
                numFound += extendRun(count - numFound, result, proposedStart, totalClusters);
            }

            // The MFT grows sequentially across the disk
            if (numFound < count && !_fragmentedDiskMode) {
                numFound += findClusters(count - numFound, result, 0, totalClusters, isMft, true, 0);
            }

            if (numFound < count) {
                numFound += findClusters(count - numFound, result, 0, totalClusters, isMft, false, 0);
            }
        } else {
            // First, try to extend the existing cluster run (if available)
            if (proposedStart >= 0) {
                numFound += extendRun(count - numFound, result, proposedStart, totalClusters);
            }

            // Try to find a contiguous range
            if (numFound < count && !_fragmentedDiskMode) {
                numFound += findClusters(count - numFound, result, totalClusters / 8, totalClusters, isMft, true, total / 4);
            }

            if (numFound < count) {
                numFound += findClusters(count - numFound, result, totalClusters / 8, totalClusters, isMft, false, 0);
            }

            if (numFound < count) {
                numFound = findClusters(count - numFound, result, totalClusters / 16, totalClusters / 8, isMft, false, 0);
            }

            if (numFound < count) {
                numFound = findClusters(count - numFound, result, totalClusters / 32, totalClusters / 16, isMft, false, 0);
            }

            if (numFound < count) {
                numFound = findClusters(count - numFound, result, 0, totalClusters / 32, isMft, false, 0);
            }
        }

        if (numFound < count) {
            freeClusters(result);
            throw new dotnet4j.io.IOException("Out of disk space");
        }

        // If we found more than two clusters, or we have a fragmented result,
        // then switch out of trying to allocate contiguous ranges. Similarly,
        // switch back if we found a resonable quantity in a single span.
        if ((numFound > 4 && result.size() == 1) || result.size() > 1) {
            _fragmentedDiskMode = numFound / result.size() < 4;
        }

        return result;
    }

    void markAllocated(long first, long count) {
        _bitmap.markPresentRange(first, count);
    }

    void freeClusters(List<Tuple<Long, Long>> runs) {
        for (Tuple<Long, Long> run : runs) {
            _bitmap.markAbsentRange(run.Item1, run.Item2);
        }
    }

    void freeClusters(Range... runs) {
        for (Range run : runs) {
            _bitmap.markAbsentRange(run.getOffset(), run.getCount());
        }
    }

    /**
     * Sets the total number of clusters managed in the volume.
     *
     * Any clusters represented in the bitmap beyond the total number in the
     * volume are marked as in-use.
     *
     * @param numClusters Total number of clusters in the volume.
     */
    void setTotalClusters(long numClusters) {
        long actualClusters = _bitmap.setTotalEntries(numClusters);
        if (actualClusters != numClusters) {
            markAllocated(numClusters, actualClusters - numClusters);
        }
    }

    private long extendRun(long count, List<Tuple<Long, Long>> result, long start, long end) {
        long focusCluster = start;
        while (!_bitmap.isPresent(focusCluster) && focusCluster < end && focusCluster - start < count) {
            ++focusCluster;
        }

        long numFound = focusCluster - start;

        if (numFound > 0) {
            _bitmap.markPresentRange(start, numFound);
            result.add(new Tuple<>(start, numFound));
        }

        return numFound;
    }

    /**
     * Finds one or more free clusters in a range.
     *
     * @param count The number of clusters required.
     * @param result The list of clusters found (i.e. out param).
     * @param start The first cluster in the range to look at.
     * @param end The last cluster in the range to look at (exclusive).
     * @param isMft Indicates if the clusters are for the MFT.
     * @param contiguous Indicates if contiguous clusters are required.
     * @param headroom Indicates how many clusters to skip before next
     *            allocation, to prevent fragmentation.
     * @return The number of clusters found in the range.
     */
    private long findClusters(long count,
                              List<Tuple<Long, Long>> result,
                              long start,
                              long end,
                              boolean isMft,
                              boolean contiguous,
                              long headroom) {
        long numFound = 0;

        long focusCluster;
        if (isMft) {
            focusCluster = start;
        } else {
            if (_nextDataCluster < start || _nextDataCluster >= end) {
                _nextDataCluster = start;
            }

            focusCluster = _nextDataCluster;
        }

        long numInspected = 0;
        while (numFound < count && focusCluster >= start && numInspected < end - start) {
            if (!_bitmap.isPresent(focusCluster)) {
                // Start of a run...
                long runStart = focusCluster;
                ++focusCluster;

                while (!_bitmap.isPresent(focusCluster) && focusCluster - runStart < count - numFound) {
                    ++focusCluster;
                    ++numInspected;
                }

                if (!contiguous || focusCluster - runStart == count - numFound) {
                    _bitmap.markPresentRange(runStart, focusCluster - runStart);
                    result.add(new Tuple<>(runStart, focusCluster - runStart));
                    numFound += focusCluster - runStart;
                }
            } else {
                ++focusCluster;
            }

            ++numInspected;

            if (focusCluster >= end) {
                focusCluster = start;
            }
        }

        if (!isMft) {
            _nextDataCluster = focusCluster + headroom;
        }

        return numFound;
    }
}
