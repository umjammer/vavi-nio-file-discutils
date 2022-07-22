
package diskClone;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;

import discUtils.common.ProgramBase;
import discUtils.core.DiskImageBuilder;
import discUtils.core.DiskImageFileSpecification;
import discUtils.core.Geometry;
import discUtils.core.GeometryTranslation;
import discUtils.core.partitions.BiosPartitionedDiskBuilder;
import discUtils.core.partitions.PartitionInfo;
import discUtils.ntfs.NtfsFileSystem;
import discUtils.streams.SnapshotStream;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.StreamPump;
import discUtils.streams.util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.Stream;


@Options
public class Program extends ProgramBase {

    private static final String FS = java.io.File.separator;

    @Option(option = "t",
            argName = "translation mode",
            args = 1,
            description = "Indicates the geometry adjustment to apply.  Set this parameter to match the translation " +
                          "configured in the BIOS of the machine that will boot from the disk - " +
                          "auto should work in most cases for modern BIOS.")
    private GeometryTranslation _translation = GeometryTranslation.Auto;

    @Option(option = "volume",
            description = "Volumes to clone.  The volumes should all be on the same disk.",
            args = 1,
            required = true)
    private String[] _volumes;

    @Option(option = "out_file", description = "Path to the output disk image.", args = 1, required = true)
    private String _destDisk;

    public static void main(String[] args) throws Exception {
        Program program = new Program();
        Options.Util.bind(args, program);
        program.run(args);
    }

    // return StandardSwitches.OutputFormatAndAdapterType;

    protected String[] getHelpRemarks() {
        return new String[] {
            "diskClone clones a live disk into a virtual disk file.  The volumes cloned must be formatted with NTFS, and partitioned using a conventional partition table.",
            "Only Windows 7 is supported.", "The tool must be run with administrator privilege."
        };
    }

    protected void doRun() throws IOException {
        if (!isAdministrator()) {
            System.err.println("\nThis utility must be run as an administrator!\n");
            System.exit(1);
        }

        DiskImageBuilder builder = DiskImageBuilder.getBuilder(getOutputDiskType(), getOutputDiskVariant());
        builder.setGenericAdapterType(getAdapterType());
        String[] sourceVolume = _volumes;
        int diskNumber;
        int[] refVar___0 = new int[1];
        List<CloneVolume> cloneVolumes = gatherVolumes(sourceVolume, refVar___0);
        diskNumber = refVar___0[0];
        if (!getQuiet()) {
            System.err.println("Inspecting Disk...");
        }

        // Construct a stream representing the contents of the cloned disk.
        BiosPartitionedDiskBuilder contentBuilder;
        Geometry biosGeometry;
        Geometry ideGeometry;
        long capacity;

        try (Disk disk = new Disk(diskNumber)) {
            contentBuilder = new BiosPartitionedDiskBuilder(disk);
            biosGeometry = disk.getBiosGeometry();
            ideGeometry = disk.getGeometry();
            capacity = disk.getCapacity();
        }
        // Preserve the IDE (aka Physical) geometry
        builder.setGeometry(ideGeometry);
        // Translate the BIOS (aka Logical) geometry
        GeometryTranslation translation = _translation;
        if (builder.getPreservesBiosGeometry() && translation == GeometryTranslation.Auto) {
            // If the new format preserves BIOS geometry, then take no action if
            // asked for 'auto'
            builder.setBiosGeometry(biosGeometry);
            translation = GeometryTranslation.None;
        } else {
            builder.setBiosGeometry(ideGeometry.translateToBios(0, translation));
        }
        if (translation != GeometryTranslation.None) {
            contentBuilder.updateBiosGeometry(builder.getBiosGeometry());
        }

        IVssBackupComponents backupCmpnts;
        int status;
        if (Marshal.SizeOf(IntPtr.class) == 4) {
            IVssBackupComponents[] refVar___1 = new IVssBackupComponents[1];
            status = NativeMethods.INSTANCE.createVssBackupComponents(refVar___1);
            backupCmpnts = refVar___1[0];
        } else {
            IVssBackupComponents[] refVar___2 = new IVssBackupComponents[1];
            status = NativeMethods.INSTANCE.createVssBackupComponents64(refVar___2);
            backupCmpnts = refVar___2[0];
        }
        UUID snapshotSetId = createSnapshotSet(cloneVolumes, backupCmpnts);
        if (!getQuiet()) {
            System.err.print("Copying Disk...");
        }

        for (CloneVolume sv : cloneVolumes) {
            Volume sourceVol = new Volume(sv.SnapshotProperties.SnapshotDeviceObject, sv.SourceExtent.ExtentLength);
            SnapshotStream rawVolStream = new SnapshotStream(sourceVol.getContent(), Ownership.None);
            rawVolStream.snapshot();
            byte[] volBitmap;
            int clusterSize;

            try (NtfsFileSystem ntfs = new NtfsFileSystem(rawVolStream)) {
                ntfs.getNtfsOptions().setHideSystemFiles(false);
                ntfs.getNtfsOptions().setHideHiddenFiles(false);
                ntfs.getNtfsOptions().setHideMetafiles(false);
                for (String filePath : ntfs.getFiles(FS + "System Volume Information",
                                                     "*{3808876B-C176-4e48-B7AE-04046E6CC752}")) {
                    // Remove VSS snapshot files (can be very large)
                    ntfs.deleteFile(filePath);
                }
                // Remove the page file
                if (ntfs.fileExists(FS + "Pagefile.sys")) {
                    ntfs.deleteFile(FS + "Pagefile.sys");
                }

                // Remove the hibernation file
                if (ntfs.fileExists(FS + "hiberfil.sys")) {
                    ntfs.deleteFile(FS + "hiberfil.sys");
                }

                try (Stream bitmapStream = ntfs.openFile("$Bitmap", FileMode.Open)) {
                    volBitmap = new byte[(int) bitmapStream.getLength()];
                    int totalRead = 0;
                    int numRead = bitmapStream.read(volBitmap, 0, volBitmap.length - totalRead);
                    while (numRead > 0) {
                        totalRead += numRead;
                        numRead = bitmapStream.read(volBitmap, totalRead, volBitmap.length - totalRead);
                    }
                }
                clusterSize = (int) ntfs.getClusterSize();
                if (translation != GeometryTranslation.None) {
                    ntfs.updateBiosGeometry(builder.getBiosGeometry());
                }
            }
            List<StreamExtent> extents = new ArrayList<>(bitmapToRanges(volBitmap, clusterSize));
            SparseStream partSourceStream = SparseStream.fromStream(rawVolStream, Ownership.None, extents);
            for (int i = 0; i < contentBuilder.getPartitionTable().getPartitions().size(); ++i) {
                PartitionInfo part = contentBuilder.getPartitionTable().getPartitions().get(i);
                if (part.getFirstSector() * 512 == sv.SourceExtent.StartingOffset) {
                    contentBuilder.setPartitionContent(i, partSourceStream);
                }
            }
        }
        SparseStream contentStream = contentBuilder.build();
        // Write out the disk images
        String dir = Paths.get(_destDisk).getParent().toString();
        String file = Paths.get(_destDisk).getFileName().toString().substring(_destDisk.lastIndexOf('.') + 1);
        builder.setContent(contentStream);
        List<DiskImageFileSpecification> fileSpecs = builder.build(file);
        for (int i = 0; i < fileSpecs.size(); ++i) {
            // Construct the destination file path from the directory of the
            // primary file.
            String outputPath = Paths.get(dir, fileSpecs.get(i).getName()).toString();
            // Force the primary file to the be one from the command-line.
            if (i == 0) {
                outputPath = _destDisk;
            }

            try (SparseStream vhdStream = fileSpecs.get(i).openStream()) {
                try (FileStream fs = new FileStream(outputPath, FileMode.Create, FileAccess.ReadWrite)) {
                    StreamPump pump = new StreamPump();
                    long totalBytes = 0;
                    for (StreamExtent se : vhdStream.getExtents()) {
                        totalBytes += se.getLength();
                    }
                    if (!getQuiet()) {
                        System.err.println();
                        long now = System.currentTimeMillis();
                        pump.ProgressEvent = (o, e) -> showProgress(fileSpecs.get(i).getName(), totalBytes, now, o, e);
                    }

                    pump.run();
                    if (!getQuiet()) {
                        System.err.println();
                    }
                }
            }
        }
        // complete - tidy up
        callAsyncMethod(backupCmpnts.backupComplete());
        long numDeleteFailed;
        UUID deleteFailed = UUID.randomUUID();
        /* VSS_OBJECT_SNAPSHOT_SET */
        long[] refVar___3 = new long[1];
        UUID[] refVar___4 = new UUID[1];
        backupCmpnts.deleteSnapshots(snapshotSetId, 2, true, refVar___3, refVar___4);
        numDeleteFailed = refVar___3[0];
        deleteFailed = refVar___4[0];
        Marshal.ReleaseComObject(backupCmpnts);
    }

    private static boolean isAdministrator() {
        WindowsPrincipal principal = new WindowsPrincipal(WindowsIdentity.GetCurrent());
        return principal.IsInRole(WindowsBuiltInRole.Administrator);
    }

    private static List<StreamExtent> bitmapToRanges(byte[] bitmap, int bytesPerCluster) {
        List<StreamExtent> result = new ArrayList<>();
        long numClusters = bitmap.length * 8L;
        long cluster = 0;
        while (cluster < numClusters && !isSet(bitmap, cluster)) {
            ++cluster;
        }
        while (cluster < numClusters) {
            long startCluster = cluster;
            while (cluster < numClusters && isSet(bitmap, cluster)) {
                ++cluster;
            }

            result.add(new StreamExtent(startCluster * bytesPerCluster, (cluster - startCluster) * bytesPerCluster));

            while (cluster < numClusters && !isSet(bitmap, cluster)) {
                ++cluster;
            }
        }
        return result;
    }

    private static boolean isSet(byte[] buffer, long bit) {
        int byteIdx = (int) (bit >> 3);
        if (byteIdx >= buffer.length) {
            return false;
        }

        byte val = buffer[byteIdx];
        byte mask = (byte) (1 << (int) (bit & 0x7));
        return (val & mask) != 0;
    }

    /**
     * @param diskNumber {@cs out}
     */
    private List<CloneVolume> gatherVolumes(String[] sourceVolume, int[] diskNumber) throws IOException {
        diskNumber[0] = 0xffffffff;
        List<CloneVolume> cloneVolumes = new ArrayList<>(sourceVolume.length);
        if (!getQuiet()) {
            System.err.println("Inspecting Volumes...");
        }

        for (String s : sourceVolume) {

            try (Volume vol = new Volume(s, 0)) {
                NativeMethods.DiskExtent[] sourceExtents = vol.getDiskExtents();
                if (sourceExtents.length > 1) {
                    System.err.printf("Volume '%s' is made up of multiple extents, which is not supported\n", s);
                    System.exit(1);
                }

                if (diskNumber[0] == 0xffffffff) {
                    diskNumber[0] = sourceExtents[0].DiskNumber;
                } else if (diskNumber[0] != sourceExtents[0].DiskNumber) {
                    System.err.println("Specified volumes span multiple disks, which is not supported");
                    System.exit(1);
                }

                String volPath = s;
                if (volPath.charAt(volPath.length() - 1) != File.separatorChar) {
                    volPath += FS;
                }

                cloneVolumes.add(new CloneVolume());
            }
        }
        return cloneVolumes;
    }

    private UUID createSnapshotSet(List<CloneVolume> cloneVolumes, IVssBackupComponents backupCmpnts) {
        if (!getQuiet()) {
            System.err.println("Snapshotting Volumes...");
        }

        backupCmpnts.initializeForBackup(null);
        backupCmpnts.setContext(0);
        /* VSS_CTX_BACKUP */
        backupCmpnts.setBackupState(false, true, 5, false);
        /* VSS_BT_COPY */
        callAsyncMethod(backupCmpnts.gatherWriterMetadata());
        UUID snapshotSetId;
        try {
            UUID[] refVar___5 = new UUID[1];
            backupCmpnts.startSnapshotSet(refVar___5);
            snapshotSetId = refVar___5[0];
            for (CloneVolume vol : cloneVolumes) {
                UUID[] refVar___6 = new UUID[1];
                backupCmpnts.addToSnapshotSet(vol.Path, new UUID(0, 0), refVar___6);
                vol.SnapshotId = refVar___6[0];
            }
            callAsyncMethod(backupCmpnts.prepareForBackup());
            callAsyncMethod(backupCmpnts.doSnapshotSet());
        } catch (Exception __dummyCatchVar0) {
            backupCmpnts.abortBackup();
            throw __dummyCatchVar0;
        }

        for (CloneVolume vol : cloneVolumes) {
            vol.SnapshotProperties = getSnapshotProperties(backupCmpnts, vol.SnapshotId);
        }
        return snapshotSetId;
    }

    private static VssSnapshotProperties getSnapshotProperties(IVssBackupComponents backupComponents, UUID snapshotId) {
        VssSnapshotProperties props = new VssSnapshotProperties();
        int[][] buffer = Marshal.AllocHGlobal(Marshal.SizeOf(VssSnapshotProperties.class));
        backupComponents.getSnapshotProperties(snapshotId, buffer);
        Marshal.PtrToStructure(buffer, props);
        NativeMethods.INSTANCE.vssFreeSnapshotProperties(buffer);
        return props;
    }

    @FunctionalInterface
    private interface VssAsyncMethod {
        void invoke(IVssAsync[] result);
    }

    private static void callAsyncMethod(VssAsyncMethod method) {
        IVssAsync async;
        int reserved = 0;
        int hResult;
        IVssAsync[] refVar___8 = new IVssAsync[1];
        method.invoke(refVar___8);
        async = refVar___8[0];
        async.Wait(60 * 1000);
        int[] refVar___9 = new int[1];
        int[] refVar___10 = new int[] {
            reserved
        };
        async.queryStatus(refVar___9, refVar___10);
        hResult = refVar___9[0];
        reserved = refVar___10[0];
        if (hResult != 0 && hResult != 0x0004230a) {
            /* VSS_S_ASYNC_FINISHED */
            Marshal.ThrowExceptionForHR((int) hResult);
        }
    }
}
