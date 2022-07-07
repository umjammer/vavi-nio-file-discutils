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

package discUtils.core;

import java.util.List;

import discUtils.streams.StreamExtent;
import discUtils.streams.util.Range;


/**
 * base class for all file systems based on a cluster model.
 */
public interface IClusterBasedFileSystem extends IFileSystem {
    /**
     * Gets the size (in bytes) of each cluster.
     */
    long getClusterSize();

    /**
     * Gets the total number of clusters managed by the file system.
     */
    long getTotalClusters();

    /**
     * Converts a cluster (index) into an absolute byte position in the
     * underlying stream.
     *
     * @param cluster The cluster to convert.
     * @return The corresponding absolute byte position.
     */
    long clusterToOffset(long cluster);

    /**
     * Converts an absolute byte position in the underlying stream to a cluster
     * (index).
     *
     * @param offset The byte position to convert.
     * @return The cluster containing the specified byte.
     */
    long offsetToCluster(long offset);

    /**
     * Converts a file name to the list of clusters occupied by the file's data.
     *
     * @param path The path to inspect.
     * @return The clusters.Note that in some file systems, small files may not
     *         have dedicated
     *         clusters. Only dedicated clusters will be returned.
     */
    List<Range> pathToClusters(String path);

    /**
     * Converts a file name to the extents containing its data.
     *
     * @param path The path to inspect.
     * @return The file extents, as absolute byte positions in the underlying
     *         stream.Use this method with caution - not all file systems will
     *         store all bytes
     *         directly in extents. Files may be compressed, sparse or
     *         encrypted. This method
     *         merely indicates where file data is stored, not what's stored.
     */
    List<StreamExtent> pathToExtents(String path);

    /**
     * Gets an object that can convert between clusters and files.
     *
     * @return The cluster map.
     */
    ClusterMap buildClusterMap();
}
