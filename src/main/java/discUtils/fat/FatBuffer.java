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

package discUtils.fat;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dotnet4j.io.Stream;
import vavi.util.StringUtil;


public class FatBuffer {

    private static final Logger logger = System.getLogger(FatBuffer.class.getName());

    /**
     * The End-of-chain marker to WRITE (SetNext). Don't use this value to test
     * for end of chain.
     *
     * The actual end-of-chain marker bits on disk vary by FAT type, and can end
     * ...F8 through ...FF.
     */
    public static final int EndOfChain = 0xffff_ffff;

    /**
     * The Bad-Cluster marker to WRITE (SetNext). Don't use this value to test
     * for bad clusters.
     *
     * The actual bad-cluster marker bits on disk vary by FAT type.
     */
    public static final int BadCluster = 0xffff_fff7;

    /**
     * The Free-Cluster marker to WRITE (SetNext). Don't use this value to test
     * for free clusters.
     *
     * The actual free-cluster marker bits on disk vary by FAT type.
     */
    public static final int FreeCluster = 0;

    private static final int DirtyRegionSize = 512;

    private final byte[] buffer;

    private final Map<Integer, Integer> dirtySectors;

    private final FatType type;

    private int nextFreeCandidate;

    public FatBuffer(FatType type, byte[] buffer) {
//logger.log(Level.TRACE, "\n" + StringUtil.getDump(buffer));
        this.type = type;
        this.buffer = buffer;
        dirtySectors = new HashMap<>();
    }

    public int getNumEntries() {
        return type.getNumEntries(buffer);
    }

    // FAT32
    public long getSize() {
        return buffer.length;
    }

    public boolean isFree(int val) {
        return val == 0;
    }

    public boolean isEndOfChain(int val) {
        return type.isEndOfChain(val);
    }

    public boolean isBadCluster(int val) {
        return type.isBadCluster(val);
    }

    public int getNext(int cluster) {
        return type.getNext(cluster, buffer);
    }

    public void setEndOfChain(int cluster) {
        setNext(cluster, EndOfChain);
    }

    public void setBadCluster(int cluster) {
        setNext(cluster, BadCluster);
    }

    public void setFree(int cluster) {
        if (cluster < nextFreeCandidate) {
            nextFreeCandidate = cluster;
        }

        setNext(cluster, FreeCluster);
    }

    public void setNext(int cluster, int next) {
        type.setNext(cluster, next, buffer, this::markDirty);
    }

    /**
     * @param cluster {@cs out}
     */
    public boolean tryGetFreeCluster(int[] cluster) {
        // Simple scan - don't hold a free list...
        int numEntries = type.getNumEntries(buffer);
        for (int i = 0; i < numEntries; i++) {
            int candidate = (i + nextFreeCandidate) % numEntries;
            if (isFree(type.getNext(candidate, buffer))) {
                cluster[0] = candidate;
                nextFreeCandidate = candidate + 1;
                return true;
            }

        }
        cluster[0] = 0;
        return false;
    }

    public void freeChain(int head) {
        for (int cluster : getChain(head)) {
            setFree(cluster);
        }
    }

    public List<Integer> getChain(int head) {
        List<Integer> result = new ArrayList<>();
        if (head != 0) {
            int focus = head;
            while (!type.isEndOfChain(focus)) {
                result.add(focus);
                focus = type.getNext(focus, buffer);
            }
        }
        return result;
    }

    private void markDirty(int offset) {
        dirtySectors.put(offset / DirtyRegionSize, offset / DirtyRegionSize);
    }

    public void writeDirtyRegions(Stream stream, long position) {
        for (int val : dirtySectors.values()) {
            stream.position(position + (long) val * DirtyRegionSize);
            stream.write(buffer, val * DirtyRegionSize, DirtyRegionSize);
        }
    }

    public void clearDirtyRegions() {
        dirtySectors.clear();
    }
}
