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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;


/**
 * Class that identifies the role of each cluster in a file system.
 */
public final class ClusterMap {
    private final Object[] _clusterToFileId;

    private final EnumSet<ClusterRoles>[] _clusterToRole;

    private final Map<Object, String[]> _fileIdToPaths;

    public ClusterMap(EnumSet<ClusterRoles>[] clusterToRole,
            Object[] clusterToFileId,
            Map<Object, String[]> fileIdToPaths) {
        _clusterToRole = clusterToRole;
        _clusterToFileId = clusterToFileId;
        _fileIdToPaths = fileIdToPaths;
    }

    /**
     * Gets the role of a cluster within the file system.
     *
     * @param cluster The cluster to inspect.
     * @return The clusters role (or roles).
     */
    public EnumSet<ClusterRoles> getRole(int cluster) {
        if (_clusterToRole == null || _clusterToRole.length < cluster) {
            return EnumSet.noneOf(ClusterRoles.class);
        }

        return _clusterToRole[cluster];
    }

    /**
     * Converts a cluster to a list of file names.
     *
     * @param cluster The cluster to inspect.
     * @return A list of paths that map to the cluster.A list is returned
     *         because on file systems with the notion of
     *         hard links, a cluster may correspond to multiple directory
     *         entries.
     */
    public String[] clusterToPaths(int cluster) {
        if (!Collections.disjoint(getRole(cluster), EnumSet.of(ClusterRoles.DataFile, ClusterRoles.SystemFile))) {
            Object fileId = _clusterToFileId[cluster];
            return _fileIdToPaths.get(fileId);
        }

        return new String[0];
    }
}
