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

import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


public final class ClusterReader {

    private final int bytesPerSector;

    /**
     * Pre-calculated value because of number of uses of this externally.
     */
    private final int clusterSize;

    private final int firstDataSector;

    private final int sectorsPerCluster;

    private final Stream stream;

    public ClusterReader(Stream stream, int firstDataSector, int sectorsPerCluster, int bytesPerSector) {
        this.stream = stream;
        this.firstDataSector = firstDataSector;
        this.sectorsPerCluster = sectorsPerCluster;
        this.bytesPerSector = bytesPerSector;
        clusterSize = this.sectorsPerCluster * this.bytesPerSector;
    }

    public int getClusterSize() {
        return clusterSize;
    }

    public void readCluster(int cluster, byte[] buffer, int offset) {
        if (offset + getClusterSize() > buffer.length) {
            throw new IndexOutOfBoundsException("buffer is too small - cluster would overflow buffer");
        }

        int firstSector = (cluster - 2) * sectorsPerCluster + firstDataSector;
        stream.position((long) firstSector * bytesPerSector);
        StreamUtilities.readExact(stream, buffer, offset, clusterSize);
    }

    public void writeCluster(int cluster, byte[] buffer, int offset) {
        if (offset + getClusterSize() > buffer.length) {
            throw new IndexOutOfBoundsException("buffer is too small - cluster would overflow buffer");
        }

        int firstSector = (cluster - 2) * sectorsPerCluster + firstDataSector;
        stream.position((long) firstSector * bytesPerSector);
        stream.write(buffer, offset, clusterSize);
    }

    public void wipeCluster(int cluster) {
        int firstSector = (cluster - 2) * sectorsPerCluster + firstDataSector;
        stream.position((long) firstSector * bytesPerSector);
        stream.write(new byte[clusterSize], 0, clusterSize);
    }
}
