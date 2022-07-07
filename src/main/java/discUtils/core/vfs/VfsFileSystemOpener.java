
package discUtils.core.vfs;

import discUtils.core.DiscFileSystem;
import discUtils.core.FileSystemParameters;
import discUtils.core.VolumeInfo;
import dotnet4j.io.Stream;


@FunctionalInterface
public interface VfsFileSystemOpener {
    /**
     * Delegate for instantiating a file system.
     *
     * @param stream     The stream containing the file system.
     * @param volumeInfo Optional, information about the volume the file system is
     *                       on.
     * @param parameters Parameters for the file system.
     * @return A file system implementation.
     */
    DiscFileSystem invoke(Stream stream, VolumeInfo volumeInfo, FileSystemParameters parameters);
}
