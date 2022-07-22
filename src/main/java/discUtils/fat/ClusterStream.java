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

import java.util.ArrayList;
import java.util.List;

import dotnet4j.io.FileAccess;
import dotnet4j.io.IOException;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public class ClusterStream extends Stream {

    private final FileAccess access;

    private final byte[] clusterBuffer;

    private final FileAllocationTable fat;

    private final List<Integer> knownClusters;

    private final ClusterReader reader;

    private boolean atEOF;

    private int currentCluster;

    private int length;

    private long position;

    public ClusterStream(FatFileSystem fileSystem, FileAccess access, int firstCluster, int length) {
        this.access = access;
        reader = fileSystem.getClusterReader();
        fat = fileSystem.getFat();
        this.length = length;
        knownClusters = new ArrayList<>();
        if (firstCluster != 0) {
            knownClusters.add(firstCluster);
        } else {
            knownClusters.add(FatBuffer.EndOfChain);
        }
        if (this.length == 0xffff_ffff) {
            this.length = detectLength();
        }

        currentCluster = 0xffff_ffff;
        clusterBuffer = new byte[reader.getClusterSize()];
    }

    public boolean canRead() {
        return access == FileAccess.Read || access == FileAccess.ReadWrite;
    }

    public boolean canSeek() {
        return true;
    }

    public boolean canWrite() {
        return access == FileAccess.ReadWrite || access == FileAccess.Write;
    }

    public long getLength() {
        return length;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long value) {
        if (value >= 0) {
            position = value;
            atEOF = false;
        } else {
            throw new IndexOutOfBoundsException("Attempt to move before beginning of stream");
        }
    }

    public FirstClusterChangedDelegate firstClusterChanged;

    public void flush() {
    }

    public int read(byte[] buffer, int offset, int count) {
        if (!canRead()) {
            throw new IOException("Attempt to read from file not opened for read");
        }

        if (position > length) {
            throw new IOException("Attempt to read beyond end of file");
        }

        if (count < 0) {
            throw new IndexOutOfBoundsException("Attempt to read negative number of bytes");
        }

        int target = count;
        if (length - position < count) {
            target = (int) (length - position);
        }

        if (!tryLoadCurrentCluster()) {
            if ((position == length || position == detectLength()) && !atEOF) {
                atEOF = true;
                return 0;
            }

            throw new IOException("Attempt to read beyond known clusters");
        }

        int numRead = 0;
        while (numRead < target) {
            int clusterOffset = (int) (position % reader.getClusterSize());
            int toCopy = Math.min(reader.getClusterSize() - clusterOffset, target - numRead);
            System.arraycopy(clusterBuffer, clusterOffset, buffer, offset + numRead, toCopy);
            // Remember how many we've read in total
            numRead += toCopy;
            // Increment the position
            position += toCopy;
            // Abort if we've hit the end of the file
            if (!tryLoadCurrentCluster()) {
                break;
            }

        }
        if (numRead == 0) {
            atEOF = true;
        }

        return numRead;
    }

    public long seek(long offset, SeekOrigin origin) {
        long newPos = offset;
        if (origin == SeekOrigin.Current) {
            newPos += position;
        } else if (origin == SeekOrigin.End) {
            newPos += getLength();
        }

        position = newPos;
        atEOF = false;
        return newPos;
    }

    public void setLength(long value) {
        long desiredNumClusters = (value + reader.getClusterSize() - 1) / reader.getClusterSize();
        long actualNumClusters = (length + reader.getClusterSize() - 1) / reader.getClusterSize();
        if (desiredNumClusters < actualNumClusters) {
            int[] cluster = new int[1];
            boolean result = !tryGetClusterByPosition(value, cluster);
            if (result) {
                throw new IOException("internal state corrupt - unable to find cluster");
            }

            int firstToFree = fat.getNext(cluster[0]);
            fat.setEndOfChain(cluster[0]);
            fat.freeChain(firstToFree);
            while (knownClusters.size() > desiredNumClusters) {
                knownClusters.remove(knownClusters.size() - 1);
            }
            knownClusters.add(FatBuffer.EndOfChain);
            if (desiredNumClusters == 0) {
                fireFirstClusterAllocated(0);
            }

        } else if (desiredNumClusters > actualNumClusters) {
            int[] cluster = new int[1];
            while (!tryGetClusterByPosition(value, cluster)) {
                cluster[0] = extendChain();
                reader.wipeCluster(cluster[0]);
            }
        }

        if (length != value) {
            length = (int) value;
            if (position > length) {
                position = length;
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
                // Extend the stream until it encompasses position
                int[] cluster = new int[1];
                while (!tryGetClusterByPosition(position, cluster)) {
                    cluster[0] = extendChain();
                    reader.wipeCluster(cluster[0]);
                }
                // Fill this cluster with as much data as we can (WriteToCluster
                // preserves existing cluster
                // data, if necessary)
                int numWritten = writeToCluster(cluster[0],
                                                (int) (position % reader.getClusterSize()),
                                                buffer,
                                                offset,
                                                bytesRemaining);
                offset += numWritten;
                bytesRemaining -= numWritten;
                position += numWritten;
            }
            length = (int) Math.max(length, position);
        } finally {
            fat.flush();
        }
        atEOF = false;
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
        if (pos == 0 && count >= reader.getClusterSize()) {
            currentCluster = cluster;
            System.arraycopy(buffer, offset, clusterBuffer, 0, reader.getClusterSize());
            writeCurrentCluster();
            return reader.getClusterSize();
        }

        // Partial cluster, so need to read existing cluster data first
        loadCluster(cluster);
        int copyLength = Math.min(count, reader.getClusterSize() - pos % reader.getClusterSize());
        System.arraycopy(buffer, offset, clusterBuffer, pos, copyLength);
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
        if (!fat.isEndOfChain(knownClusters.get(knownClusters.size() - 1))) {
            throw new IOException("Corrupt file system: final cluster isn't End-of-Chain");
        }

        int[] cluster = new int[1];
        boolean result = !fat.tryGetFreeCluster(cluster);
        if (result) {
            throw new IOException("Out of disk space");
        }

        fat.setEndOfChain(cluster[0]);
        if (knownClusters.size() == 1) {
            fireFirstClusterAllocated(cluster[0]);
        } else {
            fat.setNext(knownClusters.get(knownClusters.size() - 2), cluster[0]);
        }
        knownClusters.set(knownClusters.size() - 1, cluster[0]);
        knownClusters.add(fat.getNext(cluster[0]));
        return cluster[0];
    }

    private void fireFirstClusterAllocated(int cluster) {
        if (firstClusterChanged != null) {
            firstClusterChanged.invoke(cluster);
        }
    }

    private boolean tryLoadCurrentCluster() {
        return tryLoadClusterByPosition(position);
    }

    private boolean tryLoadClusterByPosition(long pos) {
        int[] cluster = new int[1];
        boolean result = !tryGetClusterByPosition(pos, cluster);
        if (result) {
            return false;
        }

        // Read the cluster, it's different to the one currently loaded
        if (cluster[0] != currentCluster) {
            reader.readCluster(cluster[0], clusterBuffer, 0);
            currentCluster = cluster[0];
        }

        return true;
    }

    private void loadCluster(int cluster) {
        // Read the cluster, it's different to the one currently loaded
        if (cluster != currentCluster) {
            reader.readCluster(cluster, clusterBuffer, 0);
            currentCluster = cluster;
        }

    }

    private void writeCurrentCluster() {
        reader.writeCluster(currentCluster, clusterBuffer, 0);
    }

    /**
     * @param cluster {@cs out}
     */
    private boolean tryGetClusterByPosition(long pos, int[] cluster) {
        int index = (int) (pos / reader.getClusterSize());
        if (knownClusters.size() <= index) {
            if (!tryPopulateKnownClusters(index)) {
                cluster[0] = 0xffff_ffff;
                return false;
            }
        }

        // Chain is shorter than the current stream position
        if (knownClusters.size() <= index) {
            cluster[0] = 0xffff_ffff;
            return false;
        }

        cluster[0] = knownClusters.get(index);
        // This is the 'special' End-of-chain cluster identifier, so the stream
        // position is greater than the actual file length.
        if (fat.isEndOfChain(cluster[0])) {
            return false;
        }

        return true;
    }

    private boolean tryPopulateKnownClusters(int index) {
        int lastKnown = knownClusters.get(knownClusters.size() - 1);
        while (!fat.isEndOfChain(lastKnown) && knownClusters.size() <= index) {
            lastKnown = fat.getNext(lastKnown);
            knownClusters.add(lastKnown);
        }
        return knownClusters.size() > index;
    }

    private int detectLength() {
        while (!fat.isEndOfChain(knownClusters.get(knownClusters.size() - 1))) {
            if (!tryPopulateKnownClusters(knownClusters.size())) {
                throw new IOException("Corrupt file stream - unable to discover end of cluster chain");
            }
        }
        return (int) ((knownClusters.size() - 1) * (long) reader.getClusterSize());
    }

    @Override
    public void close() throws java.io.IOException {
    }
}
