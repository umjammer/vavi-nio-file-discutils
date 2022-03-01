
package ExternalFileSystem;

import java.nio.charset.StandardCharsets;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.FileSystemParameters;
import DiscUtils.Core.VolumeInfo;
import DiscUtils.Core.Vfs.VfsFileSystemFactory;
import DiscUtils.Core.Vfs.VfsFileSystemFactoryAttribute;
import DiscUtils.Core.Vfs.VfsFileSystemInfo;
import dotnet4j.io.Stream;
import dotnet4j.io.compat.Utilities;


@VfsFileSystemFactoryAttribute
public class MyFileSystemFactory extends VfsFileSystemFactory {
    public DiscUtils.Core.FileSystemInfo[] detect(Stream stream, VolumeInfo volumeInfo) {
        byte[] header = new byte[4];
        stream.read(header, 0, 4);
        if (Utilities.equals(new String(header, 0, 4, StandardCharsets.US_ASCII), "MYFS")) {
            return new DiscUtils.Core.FileSystemInfo[] {
                new VfsFileSystemInfo("MyFs", "My File System", this::open)
            };
        }

        return new DiscUtils.Core.FileSystemInfo[0];
    }

    private DiscFileSystem open(Stream stream, VolumeInfo volInfo, FileSystemParameters parameters) {
        return new MyFileSystem();
    }
}
