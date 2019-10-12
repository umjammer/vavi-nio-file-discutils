//

package LibraryTests;

import java.util.Arrays;
import java.util.List;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.FloppyDiskType;
import DiscUtils.Core.Geometry;
import DiscUtils.Diagnostics.ValidatingFileSystem;
import DiscUtils.Ntfs.NtfsFileSystem;
import DiscUtils.Ntfs.NtfsFileSystemChecker;
import DiscUtils.Streams.SparseMemoryBuffer;
import DiscUtils.Streams.SparseMemoryStream;


public class FileSystemSource {
    public static List<NewFileSystemDelegate[]> getReadWriteFileSystems() {
        return Arrays.asList(
            new NewFileSystemDelegate[] {
                FileSystemSource::fatFileSystem
            },
            // TODO: When format code complete, format a vanilla partition rather than relying on file on disk
            new NewFileSystemDelegate[] {
                FileSystemSource::diagnosticNtfsFileSystem
            }
        );
    }

    public static List<NewFileSystemDelegate[]> getQuickReadWriteFileSystems() {
        return Arrays.asList(
            new NewFileSystemDelegate[] {
                FileSystemSource::fatFileSystem
            },
            new NewFileSystemDelegate[] {
                FileSystemSource::ntfsFileSystem
            }
        );
    }

    private static DiscFileSystem fatFileSystem() {
        SparseMemoryBuffer buffer = new SparseMemoryBuffer(4096);
        SparseMemoryStream ms = new SparseMemoryStream();
        Geometry diskGeometry = Geometry.fromCapacity(30 * 1024 * 1024);
        return DiscUtils.Fat.FatFileSystem.formatFloppy(ms, FloppyDiskType.Extended, null);
    }

    public static DiscFileSystem diagnosticNtfsFileSystem() {
        SparseMemoryBuffer buffer = new SparseMemoryBuffer(4096);
        SparseMemoryStream ms = new SparseMemoryStream();
        Geometry diskGeometry = Geometry.fromCapacity(30 * 1024 * 1024);
        DiscUtils.Ntfs.NtfsFileSystem.format(ms, "", diskGeometry, 0, diskGeometry.getTotalSectorsLong());
        ValidatingFileSystem discFs = new ValidatingFileSystem(NtfsFileSystem.class, NtfsFileSystemChecker.class, ms);
        discFs.setCheckpointInterval(1);
        discFs.setGlobalIOTraceCapturesStackTraces(false);
        return discFs;
    }

    public static DiscUtils.Ntfs.NtfsFileSystem ntfsFileSystem() {
        SparseMemoryBuffer buffer = new SparseMemoryBuffer(4096);
        SparseMemoryStream ms = new SparseMemoryStream();
        Geometry diskGeometry = Geometry.fromCapacity(30 * 1024 * 1024);
        return DiscUtils.Ntfs.NtfsFileSystem.format(ms, "", diskGeometry, 0, diskGeometry.getTotalSectorsLong());
    }
}
