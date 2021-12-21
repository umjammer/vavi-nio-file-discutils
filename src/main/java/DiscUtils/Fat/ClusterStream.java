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
import java.util.List;

import dotnet4j.io.FileAccess;
import dotnet4j.io.IOException;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public class ClusterStream extends Stream {
    private final FileAccess _access;

    private final byte[] _clusterBuffer;

    private final FileAllocationTable _fat;

    private final List<Integer> _knownClusters;

    private final ClusterReader _reader;

    private boolean _atEOF;

    private int _currentCluster;

    private int _length;

    private long _position;

    public ClusterStream(FatFileSystem fileSystem, FileAccess access, int firstCluster, int length) {
        _access = access;
        _reader = fileSystem.getClusterReader();
        _fat = fileSystem.getFat();
        _length = length;
        _knownClusters = new ArrayList<>();
        if (firstCluster != 0) {
            _knownClusters.add(firstCluster);
        } else {
            _knownClusters.add(FatBuffer.EndOfChain);
        }
        if (_length == 0xffff_ffff) {
            _length = detectLength();
        }

        _currentCluster = 0xffff_ffff;
        _clusterBuffer = new byte[_reader.getClusterSize()];
    }

    public boolean canRead() {
        return _access == FileAccess.Read || _access == FileAccess.ReadWrite;
    }

    public boolean canSeek() {
        return true;
    }

    public boolean canWrite() {
        return _access == FileAccess.ReadWrite || _access == FileAccess.Write;
    }

    public long getLength() {
        return _length;
    }

    public long getPosition() {
        return _position;
    }

    public void setPosition(long value) {
        if (value >= 0) {
            _position = value;
            _atEOF = false;
        } else {
            throw new IndexOutOfBoundsException("Attempt to move before beginning of stream");
        }
    }

    public FirstClusterChangedDelegate FirstClusterChanged;

    public void flush() {
    }

    public int read(byte[] buffer, int offset, int count) {
        if (!canRead()) {
            throw new IOException("Attempt to read from file not opened for read");
        }

        if (_position > _length) {
            throw new IOException("Attempt to read beyond end of file");
        }

        if (count < 0) {
            throw new IndexOutOfBoundsException("Attempt to read negative number of bytes");
        }

        int target = count;
        if (_length - _position < count) {
            target = (int) (_length - _position);
        }

        if (!tryLoadCurrentCluster()) {
            if ((_position == _length || _position == detectLength()) && !_atEOF) {
                _atEOF = true;
                return 0;
            }

            throw new IOException("Attempt to read beyond known clusters");
        }

        int numRead = 0;
        while (numRead < target) {
            int clusterOffset = (int) (_position % _reader.getClusterSize());
            int toCopy = Math.min(_reader.getClusterSize() - clusterOffset, target - numRead);
            System.arraycopy(_clusterBuffer, clusterOffset, buffer, offset + numRead, toCopy);
            // Remember how many we've read in total
            numRead += toCopy;
            // Increment the position
            _position += toCopy;
            // Abort if we've hit the end of the file
            if (!tryLoadCurrentCluster()) {
                break;
            }

        }
        if (numRead == 0) {
            _atEOF = true;
        }

        return numRead;
    }

    public long seek(long offset, SeekOrigin origin) {
        long newPos = offset;
        if (origin == SeekOrigin.Current) {
            newPos += _position;
        } else if (origin == SeekOrigin.End) {
            newPos += getLength();
        }

        _position = newPos;
        _atEOF = false;
        return newPos;
    }

    public void setLength(long value) {
        long desiredNumClusters = (value + _reader.getClusterSize() - 1) / _reader.getClusterSize();
        long actualNumClusters = (_length + _reader.getClusterSize() - 1) / _reader.getClusterSize();
        if (desiredNumClusters < actualNumClusters) {
            int[] cluster = new int[1];
            boolean result = !tryGetClusterByPosition(value, cluster);
            if (result) {
                throw new IOException("Internal state corrupt - unable to find cluster");
            }

            int firstToFree = _fat.getNext(cluster[0]);
            _fat.setEndOfChain(cluster[0]);
            _fat.freeChain(firstToFree);
            while (_knownClusters.size() > desiredNumClusters) {
                _knownClusters.remove(_knownClusters.size() - 1);
            }
            _knownClusters.add(FatBuffer.EndOfChain);
            if (desiredNumClusters == 0) {
                fireFirstClusterAllocated(0);
            }

        } else if (desiredNumClusters > actualNumClusters) {
            int[] cluster = new int[1];
            while (!tryGetClusterByPosition(value, cluster)) {
                cluster[0] = extendChain();
                _reader.wipeCluster(cluster[0]);
            }
        }

        if (_length != value) {
            _length = (int) value;
            if (_position > _length) {
                _position = _length;
            }
        }
    }

    public void write(byte[] buffer, int offset, int count) {
        int bytesRemaining = count;
        if (!canWrite()) {
            throw new IOException("Attempting to write to file not opened for writing");
        }

        if (count < 0) {
            throw new IndexOutOfBoundsException("Attempting to write negative number of bytes");
        }

        if (offset > buffer.length || offset + count > buffer.length) {
            throw new IllegalArgumentException("Attempt to write bytes outside of the buffer");
        }

        try {
            while (bytesRemaining > 0) {
                // TODO: Free space check...
                // Extend the stream until it encompasses _position
                int[] cluster = new int[1];
                while (!tryGetClusterByPosition(_position, cluster)) {
                    cluster[0] = extendChain();
                    _reader.wipeCluster(cluster[0]);
                }
                // Fill this cluster with as much data as we can (WriteToCluster
                // preserves existing cluster
                // data, if necessary)
                int numWritten = writeToCluster(cluster[0],
                                                (int) (_position % _reader.getClusterSize()),
                                                buffer,
                                                offset,
                                                bytesRemaining);
                offset += numWritten;
                bytesRemaining -= numWritten;
                _position += numWritten;
            }
            _length = (int) Math.max(_length, _position);
        } finally {
            _fat.flush();
        }
        _atEOF = false;
    }

    /**
     * Writes up to the next cluster boundary, making sure to preserve existing
     * data in the cluster that falls outside of the updated range.
     *
     * @param cluster The cluster to write to.
     * @param pos The file position of the write (within the cluster).
     * @param buffer The buffer with the new data.
     * @param offset Offset into buffer of the first byte to write.
     * @param count The maximum number of bytes to write.
     * @return The number of bytes written - either count, or the number that
     *         fit up to the cluster boundary.
     */
    private int writeToCluster(int cluster, int pos, byte[] buffer, int offset, int count) {
        if (pos == 0 && count >= _reader.getClusterSize()) {
            _currentCluster = cluster;
            System.arraycopy(buffer, offset, _clusterBuffer, 0, _reader.getClusterSize());
            writeCurrentCluster();
            return _reader.getClusterSize();
        }

        // Partial cluster, so need to read existing cluster data first
        loadCluster(cluster);
        int copyLength = Math.min(count, _reader.getClusterSize() - pos % _reader.getClusterSize());
        System.arraycopy(buffer, offset, _clusterBuffer, pos, copyLength);
        writeCurrentCluster();
        return copyLength;
    }

    /**
     * Adds a new cluster to the end of the existing chain, by allocating a free
     * cluster.
     *
     * This method does not initialize the data in the cluster, the caller
     * should perform a write to ensure the cluster data is in known state.
     *
     * @return The cluster allocated.
     */
    private int extendChain() {
        // Sanity check - make sure the final known cluster is the EOC marker
        if (!_fat.isEndOfChain(_knownClusters.get(_knownClusters.size() - 1))) {
            throw new IOException("Corrupt file system: final cluster isn't End-of-Chain");
        }

        int[] cluster = new int[1];
        boolean result = !_fat.tryGetFreeCluster(cluster);
        if (result) {
            throw new IOException("Out of disk space");
        }

        _fat.setEndOfChain(cluster[0]);
        if (_knownClusters.size() == 1) {
            fireFirstClusterAllocated(cluster[0]);
        } else {
            _fat.setNext(_knownClusters.get(_knownClusters.size() - 2), cluster[0]);
        }
        _knownClusters.set(_knownClusters.size() - 1, cluster[0]);
        _knownClusters.add(_fat.getNext(cluster[0]));
        return cluster[0];
    }

    private void fireFirstClusterAllocated(int cluster) {
        if (FirstClusterChanged != null) {
            FirstClusterChanged.invoke(cluster);
        }
    }

    private boolean tryLoadCurrentCluster() {
        return tryLoadClusterByPosition(_position);
    }

    private boolean tryLoadClusterByPosition(long pos) {
        int[] cluster = new int[1];
        boolean result = !tryGetClusterByPosition(pos, cluster);
        if (result) {
            return false;
        }

        // Read the cluster, it's different to the one currently loaded
        if (cluster[0] != _currentCluster) {
            _reader.readCluster(cluster[0], _clusterBuffer, 0);
            _currentCluster = cluster[0];
        }

        return true;
    }

    private void loadCluster(int cluster) {
        // Read the cluster, it's different to the one currently loaded
        if (cluster != _currentCluster) {
            _reader.readCluster(cluster, _clusterBuffer, 0);
            _currentCluster = cluster;
        }

    }

    private void writeCurrentCluster() {
        _reader.writeCluster(_currentCluster, _clusterBuffer, 0);
    }

    /**
     * @param cluster {@cs out}
     */
    private boolean tryGetClusterByPosition(long pos, int[] cluster) {
        int index = (int) (pos / _reader.getClusterSize());
        if (_knownClusters.size() <= index) {
            if (!tryPopulateKnownClusters(index)) {
                cluster[0] = 0xffff_ffff;
                return false;
            }
        }

        // Chain is shorter than the current stream position
        if (_knownClusters.size() <= index) {
            cluster[0] = 0xffff_ffff;
            return false;
        }

        cluster[0] = _knownClusters.get(index);
        // This is the 'special' End-of-chain cluster identifier, so the stream
        // position is greater than the actual file length.
        if (_fat.isEndOfChain(cluster[0])) {
            return false;
        }

        return true;
    }

    private boolean tryPopulateKnownClusters(int index) {
        int lastKnown = _knownClusters.get(_knownClusters.size() - 1);
        while (!_fat.isEndOfChain(lastKnown) && _knownClusters.size() <= index) {
            lastKnown = _fat.getNext(lastKnown);
            _knownClusters.add(lastKnown);
        }
        return _knownClusters.size() > index;
    }

    private int detectLength() {
        while (!_fat.isEndOfChain(_knownClusters.get(_knownClusters.size() - 1))) {
            if (!tryPopulateKnownClusters(_knownClusters.size())) {
                throw new IOException("Corrupt file stream - unable to discover end of cluster chain");
            }
        }
        return (int) ((_knownClusters.size() - 1) * (long) _reader.getClusterSize());
    }

    @Override
    public void close() throws java.io.IOException {
    }
}
