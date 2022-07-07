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

package discUtils.iso9660;

import java.util.List;
import java.util.NoSuchElementException;

import discUtils.core.ClusterMap;
import discUtils.core.IClusterBasedFileSystem;
import discUtils.core.IUnixFileSystem;
import discUtils.core.UnixFileSystemInfo;
import discUtils.core.vfs.VfsFileSystemFacade;
import discUtils.streams.StreamExtent;
import discUtils.streams.util.Range;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


/**
 * Class for reading existing ISO images.
 */
public class CDReader extends VfsFileSystemFacade implements IClusterBasedFileSystem, IUnixFileSystem {
    /**
     * Initializes a new instance of the CDReader class.
     *
     * @param data The stream to read the ISO image from.
     * @param joliet Whether to read Joliet extensions.
     */
    public CDReader(Stream data, boolean joliet) {
        super(new VfsCDReader(data, joliet, false));
    }

    /**
     * Initializes a new instance of the CDReader class.
     *
     * @param data The stream to read the ISO image from.
     * @param joliet Whether to read Joliet extensions.
     * @param hideVersions Hides version numbers (e.g. ";1") from the end of
     *            files.
     */
    public CDReader(Stream data, boolean joliet, boolean hideVersions) {
        super(new VfsCDReader(data, joliet, hideVersions));
    }

    /**
     * Gets which of the iso9660 variants is being used.
     */
    public Iso9660Variant getActiveVariant() {
        return VfsCDReader.class.cast(getRealFileSystem()).getActiveVariant();
    }

    /**
     * Gets the emulation requested of BIOS when the image is loaded.
     */
    public BootDeviceEmulation getBootEmulation() {
        return VfsCDReader.class.cast(getRealFileSystem()).getBootEmulation();
    }

    /**
     * Gets the absolute start position (in bytes) of the boot image, or zero if
     * not found.
     */
    public long getBootImageStart() {
        return VfsCDReader.class.cast(getRealFileSystem()).getBootImageStart();
    }

    /**
     * Gets the memory segment the image should be loaded into (0 for default).
     */
    public int getBootLoadSegment() {
        return VfsCDReader.class.cast(getRealFileSystem()).getBootLoadSegment();
    }

    /**
     * Gets a value indicating whether a boot image is present.
     */
    public boolean hasBootImage() {
        return VfsCDReader.class.cast(getRealFileSystem()).getHasBootImage();
    }

    /**
     * Gets the size (in bytes) of each cluster.
     */
    public long getClusterSize() {
        return VfsCDReader.class.cast(getRealFileSystem()).getClusterSize();
    }

    /**
     * Gets the total number of clusters managed by the file system.
     */
    public long getTotalClusters() {
        return VfsCDReader.class.cast(getRealFileSystem()).getTotalClusters();
    }

    /**
     * Converts a cluster (index) into an absolute byte position in the
     * underlying stream.
     *
     * @param cluster The cluster to convert.
     * @return The corresponding absolute byte position.
     */
    public long clusterToOffset(long cluster) {
        return VfsCDReader.class.cast(getRealFileSystem()).clusterToOffset(cluster);
    }

    /**
     * Converts an absolute byte position in the underlying stream to a cluster
     * (index).
     *
     * @param offset The byte position to convert.
     * @return The cluster containing the specified byte.
     */
    public long offsetToCluster(long offset) {
        return VfsCDReader.class.cast(getRealFileSystem()).offsetToCluster(offset);
    }

    /**
     * Converts a file name to the list of clusters occupied by the file's data.
     *
     * @param path The path to inspect.
     * @return The clusters.Note that in some file systems, small files may not
     *         have dedicated clusters. Only dedicated clusters will be
     *         returned.
     */
    public List<Range> pathToClusters(String path) {
        return VfsCDReader.class.cast(getRealFileSystem()).pathToClusters(path);
    }

    /**
     * Converts a file name to the extents containing its data.
     *
     * Use this method with caution - not all file systems will store all bytes
     * directly in extents. Files may be compressed, sparse or encrypted. This
     * method merely indicates where file data is stored, not what's stored.
     *
     * @param path The path to inspect.
     * @return The file extents, as absolute byte positions in the underlying
     *         stream.
     */
    public List<StreamExtent> pathToExtents(String path) {
        return VfsCDReader.class.cast(getRealFileSystem()).pathToExtents(path);
    }

    /**
     * Gets an object that can convert between clusters and files.
     *
     * @return The cluster map.
     */
    public ClusterMap buildClusterMap() {
        return VfsCDReader.class.cast(getRealFileSystem()).buildClusterMap();
    }

    /**
     * Retrieves Unix-specific information about a file or directory.
     *
     * @param path Path to the file or directory.
     * @return Information about the owner, group, permissions and type of the
     *         file or directory.
     */
    public UnixFileSystemInfo getUnixFileInfo(String path) {
        return VfsCDReader.class.cast(getRealFileSystem()).getUnixFileInfo(path);
    }

    /**
     * Detects if a stream contains a valid ISO file system.
     *
     * @param data The stream to inspect.
     * @return {@code true} if the stream contains an ISO file system, else
     *         false.
     */
    public static boolean detect(Stream data) {
        byte[] buffer = new byte[IsoUtilities.SectorSize];
        if (data.getLength() < 0x8000 + IsoUtilities.SectorSize) {
            return false;
        }

        data.setPosition(0x8000);
        int numRead = StreamUtilities.readMaximum(data, buffer, 0, IsoUtilities.SectorSize);
        if (numRead != IsoUtilities.SectorSize) {
            return false;
        }

        try {
            BaseVolumeDescriptor bvd = new BaseVolumeDescriptor(buffer, 0);
            return bvd.StandardIdentifier.equals(BaseVolumeDescriptor.Iso9660StandardIdentifier);
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Opens a stream containing the boot image.
     *
     * @return The boot image as a stream.
     */
    public Stream openBootImage() {
        return VfsCDReader.class.cast(getRealFileSystem()).openBootImage();
    }
}
