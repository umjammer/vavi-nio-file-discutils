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

package DiscUtils.Fat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Streams.Util.EndianUtilities;
import moe.yo3explorer.dotnetio4j.Stream;


public class FatBuffer {
    /**
     * The End-of-chain marker to WRITE (SetNext). Don't use this value to test
     * for end of chain.
     *
     * The actual end-of-chain marker bits on disk vary by FAT type, and can end
     * ...F8 through ...FF.
     */
    public static final int EndOfChain = 0xFFFFFFFF;

    /**
     * The Bad-Cluster marker to WRITE (SetNext). Don't use this value to test
     * for bad clusters.
     *
     * The actual bad-cluster marker bits on disk vary by FAT type.
     */
    public static final int BadCluster = 0xFFFFFFF7;

    /**
     * The Free-Cluster marker to WRITE (SetNext). Don't use this value to test
     * for free clusters.
     *
     * The actual free-cluster marker bits on disk vary by FAT type.
     */
    public static final int FreeCluster = 0;

    private static final int DirtyRegionSize = 512;

    private final byte[] _buffer;

    private final Map<Integer, Integer> _dirtySectors;

    private FatType _type = FatType.None;

    private int _nextFreeCandidate;

    public FatBuffer(FatType type, byte[] buffer) {
        _type = type;
        _buffer = buffer;
        _dirtySectors = new HashMap<>();
    }

    public int getNumEntries() {
        switch (_type) {
        case Fat12:
            return _buffer.length / 3 * 2;
        case Fat16:
            return _buffer.length / 2;
        default:
            return _buffer.length / 4;
        }
    }

    // FAT32
    public long getSize() {
        return _buffer.length;
    }

    public boolean isFree(int val) {
        return val == 0;
    }

    public boolean isEndOfChain(int val) {
        switch (_type) {
        case Fat12:
            return (val & 0x0FFF) >= 0x0FF8;
        case Fat16:
            return (val & 0xFFFF) >= 0xFFF8;
        case Fat32:
            return (val & 0x0FFFFFF8) >= 0x0FFFFFF8;
        default:
            throw new IllegalArgumentException("Unknown FAT type");

        }
    }

    public boolean isBadCluster(int val) {
        switch (_type) {
        case Fat12:
            return (val & 0x0FFF) == 0x0FF7;
        case Fat16:
            return (val & 0xFFFF) == 0xFFF7;
        case Fat32:
            return (val & 0x0FFFFFF8) == 0x0FFFFFF7;
        default:
            throw new IllegalArgumentException("Unknown FAT type");

        }
    }

    public int getNext(int cluster) {
        if (_type == FatType.Fat16) {
            return EndianUtilities.toUInt16LittleEndian(_buffer, cluster * 2);
        }

        if (_type == FatType.Fat32) {
            return EndianUtilities.toUInt32LittleEndian(_buffer, cluster * 4) & 0x0FFFFFFF;
        }

        // FAT12
        if ((cluster & 1) != 0) {
            return (EndianUtilities.toUInt16LittleEndian(_buffer, cluster + cluster / 2) >> 4) & 0x0FFF;
        }

        return EndianUtilities.toUInt16LittleEndian(_buffer, cluster + cluster / 2) & 0x0FFF;
    }

    public void setEndOfChain(int cluster) {
        setNext(cluster, EndOfChain);
    }

    public void setBadCluster(int cluster) {
        setNext(cluster, BadCluster);
    }

    public void setFree(int cluster) {
        if (cluster < _nextFreeCandidate) {
            _nextFreeCandidate = cluster;
        }

        setNext(cluster, FreeCluster);
    }

    public void setNext(int cluster, int next) {
        if (_type == FatType.Fat16) {
            markDirty(cluster * 2);
            EndianUtilities.writeBytesLittleEndian((short) next, _buffer, cluster * 2);
        } else if (_type == FatType.Fat32) {
            markDirty(cluster * 4);
            int oldVal = EndianUtilities.toUInt32LittleEndian(_buffer, cluster * 4);
            int newVal = (oldVal & 0xF0000000) | (next & 0x0FFFFFFF);
            EndianUtilities.writeBytesLittleEndian(newVal, _buffer, cluster * 4);
        } else {
            int offset = cluster + cluster / 2;
            markDirty(offset);
            markDirty(offset + 1);
            // On alternate sector boundaries, cluster info crosses two sectors
            short maskedOldVal;
            if ((cluster & 1) != 0) {
                next = next << 4;
                maskedOldVal = (short) (EndianUtilities.toUInt16LittleEndian(_buffer, offset) & 0x000F);
            } else {
                next = next & 0x0FFF;
                maskedOldVal = (short) (EndianUtilities.toUInt16LittleEndian(_buffer, offset) & 0xF000);
            }
            short newVal = (short) (maskedOldVal | next);
            EndianUtilities.writeBytesLittleEndian(newVal, _buffer, offset);
        }
    }

    public boolean tryGetFreeCluster(int[] cluster) {
        // Simple scan - don't hold a free list...
        int numEntries = getNumEntries();
        for (int i = 0; i < numEntries; i++) {
            int candidate = (i + _nextFreeCandidate) % numEntries;
            if (isFree(getNext(candidate))) {
                cluster[0] = candidate;
                _nextFreeCandidate = candidate + 1;
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
            while (!isEndOfChain(focus)) {
                result.add(focus);
                focus = getNext(focus);
            }
        }
        return result;
    }

    public void markDirty(int offset) {
        _dirtySectors.put(offset / DirtyRegionSize, offset / DirtyRegionSize);
    }

    public void writeDirtyRegions(Stream stream, long position) {
        for (int val : _dirtySectors.values()) {
            stream.setPosition(position + val * DirtyRegionSize);
            stream.write(_buffer, val * DirtyRegionSize, DirtyRegionSize);
        }
    }

    public void clearDirtyRegions() {
        _dirtySectors.clear();
    }
}
