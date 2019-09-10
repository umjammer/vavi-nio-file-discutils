
package DiscUtils.Core.Vfs;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.FileSystemParameters;
import DiscUtils.Core.VolumeInfo;
import moe.yo3explorer.dotnetio4j.Stream;

@FunctionalInterface
public interface VfsFileSystemOpener {
    DiscFileSystem invoke(Stream stream, VolumeInfo volumeInfo, FileSystemParameters parameters);

//    List<VfsFileSystemOpener> getInvocationList();

}
