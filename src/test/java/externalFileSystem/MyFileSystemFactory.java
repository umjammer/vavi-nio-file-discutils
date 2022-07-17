
package externalFileSystem;

import java.nio.charset.StandardCharsets;

import discUtils.core.DiscFileSystem;
import discUtils.core.FileSystemParameters;
import discUtils.core.VolumeInfo;
import discUtils.core.vfs.VfsFileSystemFactory;
import discUtils.core.vfs.VfsFileSystemInfo;
import dotnet4j.io.Stream;
import dotnet4j.util.compat.Utilities;


public class MyFileSystemFactory extends VfsFileSystemFactory {
    public discUtils.core.FileSystemInfo[] detect(Stream stream, VolumeInfo volumeInfo) {
        byte[] header = new byte[4];
        stream.read(header, 0, 4);
        if (Utilities.equals(new String(header, 0, 4, StandardCharsets.US_ASCII), "MYFS")) {
            return new discUtils.core.FileSystemInfo[] {
                new VfsFileSystemInfo("MyFs", "My File System", this::open)
            };
        }

        return new discUtils.core.FileSystemInfo[0];
    }

    private DiscFileSystem open(Stream stream, VolumeInfo volInfo, FileSystemParameters parameters) {
        return new MyFileSystem();
    }
}
