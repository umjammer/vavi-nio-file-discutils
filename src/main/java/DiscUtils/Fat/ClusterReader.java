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

import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.Stream;


public final class ClusterReader {
    private final int _bytesPerSector;

    /**
     * Pre-calculated value because of number of uses of this externally.
     */
    private final int _clusterSize;

    private final int _firstDataSector;

    private final int _sectorsPerCluster;

    private final Stream _stream;

    public ClusterReader(Stream stream, int firstDataSector, int sectorsPerCluster, int bytesPerSector) {
        _stream = stream;
        _firstDataSector = firstDataSector;
        _sectorsPerCluster = sectorsPerCluster;
        _bytesPerSector = bytesPerSector;
        _clusterSize = _sectorsPerCluster * _bytesPerSector;
    }

    public int getClusterSize() {
        return _clusterSize;
    }

    public void readCluster(int cluster, byte[] buffer, int offset) {
        if (offset + getClusterSize() > buffer.length) {
            throw new IndexOutOfBoundsException("buffer is too small - cluster would overflow buffer");
        }

        int firstSector = (cluster - 2) * _sectorsPerCluster + _firstDataSector;
        _stream.setPosition((long) firstSector * _bytesPerSector);
        StreamUtilities.readExact(_stream, buffer, offset, _clusterSize);
    }

    public void writeCluster(int cluster, byte[] buffer, int offset) {
        if (offset + getClusterSize() > buffer.length) {
            throw new IndexOutOfBoundsException("buffer is too small - cluster would overflow buffer");
        }

        int firstSector = (cluster - 2) * _sectorsPerCluster + _firstDataSector;
        _stream.setPosition((long) firstSector * _bytesPerSector);
        _stream.write(buffer, offset, _clusterSize);
    }

    public void wipeCluster(int cluster) {
        int firstSector = (cluster - 2) * _sectorsPerCluster + _firstDataSector;
        _stream.setPosition((long) firstSector * _bytesPerSector);
        _stream.write(new byte[_clusterSize], 0, _clusterSize);
    }
}
