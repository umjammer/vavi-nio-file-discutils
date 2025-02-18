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

package diskDump;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import discUtils.common.HexDump;
import discUtils.common.ProgramBase;
import discUtils.core.DiscDirectoryInfo;
import discUtils.core.DiscFileInfo;
import discUtils.core.DiscFileSystem;
import discUtils.core.FileSystemInfo;
import discUtils.core.FileSystemManager;
import discUtils.core.LogicalVolumeInfo;
import discUtils.core.LogicalVolumeStatus;
import discUtils.core.PhysicalVolumeInfo;
import discUtils.core.VirtualDisk;
import discUtils.core.VirtualDiskManager;
import discUtils.core.VolumeManager;
import discUtils.core.logicalDiskManager.DynamicDiskManager;
import discUtils.core.partitions.BiosPartitionInfo;
import discUtils.core.partitions.BiosPartitionTypes;
import discUtils.core.partitions.PartitionInfo;
import discUtils.streams.StreamExtent;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;
import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;
import vavi.util.Debug;


@Options
public class Program extends ProgramBase {

    static final String types;

    static {
        types = String.join(", ", VirtualDiskManager.getSupportedDiskTypes());
        System.err.println(types);
    }

    @Option(option = "disk",
            description = "Paths to the disks to inspect.  Where a volume manager is used to span volumes across multiple virtual disks, specify all disks in the set.",
            args = 1,
            required = false)
    private String inFile; // TODO array option

    @Option(option = "db", argName = "diskbytes", description = "Includes a hexdump of all disk content in the output")
    private boolean showContent;

    @Option(option = "vb", argName = "volbytes", description = "Includes a hexdump of all volumes content in the output")
    private boolean showVolContent;

    @Option(option = "sf", argName = "showfiles", description = "Includes a list of all files found in volumes")
    private boolean showFiles;

    @Option(option = "bc", argName = "bootcode", description = "Includes a hexdump of the MBR and OS boot code in the output")
    private boolean showBootCode;

    @Option(option = "he",
            argName = "hideextents",
            description = "Suppresses display of the stored extents, which can be slow for large disk images")
    private boolean hideExtents;

    @Option(option = "dt",
            argName = "disktype",
            args = 1,
            description = "Force the type of disk - use a file extension (one of TODO)")
    private String diskType/* = "type" */;

    public static void main(String[] args) throws Exception {
        Program program = new Program();
        Options.Util.bind(args, program);
        program.run(args);
    }

//    StandardSwitches.UserAndPassword | StandardSwitches.FileNameEncoding;

    @Override
    protected void doRun() throws IOException {
//        System.setProperty("file.encoding", StandardCharsets.UTF_8.name());

        List<VirtualDisk> disks = new ArrayList<>();
System.err.println(inFile);
String[] inFiles = new String[] {inFile};
        for (String path : inFiles) {
            VirtualDisk disk = VirtualDisk
                    .openDisk(path, diskType != null ? diskType : null, FileAccess.Read, getUserName(), getPassword());
            disks.add(disk);

            System.err.println();
            System.err.println("DISK: " + path);
            System.err.println();
            System.err.printf("       Capacity: %16X\n", disk.getCapacity());
            System.err.printf("       Geometry: %s\n", disk.getGeometry());
            System.err.printf("  BIOS Geometry: %s\n", disk.getBiosGeometry());
            System.err.printf("      Signature: %8X\n", disk.getSignature());
            if (disk.isPartitioned()) {
                System.err.printf("           GUID: %s\n", disk.getPartitions().getDiskGuid());
            }
            System.err.println();

            if (!hideExtents) {
                System.err.println();
                System.err.println("  Stored Extents");
                System.err.println();
                for (StreamExtent extent : disk.getContent().getExtents()) {
                    System.err.printf("    %16X - %16X\n", extent.getStart(), extent.getStart() + extent.getLength());
                }
                System.err.println();
            }

            if (showBootCode) {
                System.err.println();
                System.err.println("  Master Boot Record (MBR)");
                System.err.println();
                try {
                    byte[] mbr = new byte[512];
                    disk.getContent().position(0);
                    disk.getContent().read(mbr, 0, 512);
                    HexDump.generate(mbr, System.err);
                } catch (Exception e) {
                    System.err.println(e);
                }
                System.err.println();
            }

            System.err.println();
            System.err.println("  partitions");
            System.err.println();
            if (disk.isPartitioned()) {
                System.err.println("    T   Start (bytes)     End (bytes)       Type");
                System.err.println("    ==  ================  ================  ==================");
                for (PartitionInfo partition : disk.getPartitions().getPartitions()) {
                    System.err.printf("    %2X  %16X  %16X  %s\n",
                                      partition.getBiosType(),
                                      partition.getFirstSector() * disk.getSectorSize(),
                                      (partition.getLastSector() + 1) * disk.getSectorSize(),
                                      partition.getTypeAsString());
                    BiosPartitionInfo bpi = partition instanceof BiosPartitionInfo ? (BiosPartitionInfo) partition
                                                                                   : null;
                    if (bpi != null) {
                        System.err.printf("        %-16s  %s\n", bpi.getStart(), bpi.getEnd());
                        System.err.println();
                    }
                }
            } else {
                System.err.println("    No partitions");
                System.err.println();
            }
        }

        System.err.println();
        System.err.println();
        System.err.println("VOLUMES");
        System.err.println();
        VolumeManager volMgr = new VolumeManager();
        for (VirtualDisk disk : disks) {
            volMgr.addDisk(disk);
        }

        try {
            System.err.println();
            System.err.println("  Physical Volumes");
            System.err.println();
            for (PhysicalVolumeInfo vol : volMgr.getPhysicalVolumes()) {
                System.err.println("  " + vol.getIdentity());
                System.err.println("    Type: " + vol.getVolumeType());
                System.err.println("    BIOS Type: " + String.format("%2X", vol.getBiosType()) + " [" +
                                   BiosPartitionTypes.toString(vol.getBiosType()) + "]");
                System.err.println("    Size: " + vol.getLength());
                System.err.println("    Disk Id: " + vol.getDiskIdentity());
                System.err.println("    Disk Sig: " + String.format("%8X", vol.getDiskSignature()));
                System.err.println("    Partition: " + vol.getPartitionIdentity());
                System.err.println("    Disk Geometry: " + vol.getPhysicalGeometry());
                System.err.println("    BIOS Geometry: " + vol.getBiosGeometry());
                System.err.println("    First Sector: " + vol.getPhysicalStartSector());
                System.err.println();
            }
        } catch (Exception e) {
            Debug.printStackTrace(e);
        }

        try {
            System.err.println();
            System.err.println("  Logical Volumes");
            System.err.println();
            for (LogicalVolumeInfo vol : volMgr.getLogicalVolumes()) {
                System.err.println("  " + vol.getIdentity());
                System.err.println("    BIOS Type: " + String.format("%2X", vol.getBiosType()) + " [" +
                                   BiosPartitionTypes.toString(vol.getBiosType()) + "]");
                System.err.println("    Status: " + vol.getStatus());
                System.err.println("    Size: " + vol.getLength());
                System.err.println("    Disk Geometry: " + vol.getPhysicalGeometry());
                System.err.println("    BIOS Geometry: " + vol.getBiosGeometry());
                System.err.println("    First Sector: " + vol.getPhysicalStartSector());

                if (vol.getStatus() == LogicalVolumeStatus.Failed) {
                    System.err.println("    File Systems: <unknown - failed volume>");
                    System.err.println();
                    continue;
                }

                List<discUtils.core.FileSystemInfo> fileSystemInfos = FileSystemManager.detectFileSystems(vol);
                System.err.println("    File Systems: " +
                                   String.join(", ", fileSystemInfos.stream().map(FileSystemInfo::toString).toArray(String[]::new)));

                System.err.println();

                if (showVolContent) {
                    System.err.println("    Binary Contents...");
                    try {
                        try (Stream s = vol.open()) {
                            HexDump.generate(s, System.err);
                        }
                    } catch (Exception e) {
                        Debug.printStackTrace(e);
                    }
                    System.err.println();
                }

                if (showBootCode) {
                    for (FileSystemInfo fsi : fileSystemInfos) {
                        System.err.printf("    Boot Code: %s\n", fsi.getName());
                        try {
                            try (DiscFileSystem fs = fsi.open(vol, getFileSystemParameters())) {
                                byte[] bootCode = fs.readBootCode();
                                if (bootCode != null) {
                                    HexDump.generate(bootCode, System.err);
                                } else {
                                    System.err.println("      <file system reports no boot code>");
                                }
                            }
                        } catch (Exception e) {
                            Debug.printStackTrace(e);
                            System.err.println("      Unable to show boot code: " + e.getMessage());
                        }
                        System.err.println();
                    }
                }

                if (showFiles) {
                    for (FileSystemInfo fsi : fileSystemInfos) {
                        try (DiscFileSystem fs = fsi.open(vol, getFileSystemParameters())) {
                            System.err.printf("    %s Volume Label: %s\n", fsi.getName(), fs.getVolumeLabel());
                            System.err.printf("    Files (%s)...\n", fsi.getName());
                            showDir(fs.getRoot(), 6);
                        }
                        System.err.println();
                    }
                }
            }
        } catch (Exception e) {
            Debug.printStackTrace(e);
        }

        try {
            boolean foundDynDisk = false;
            DynamicDiskManager dynDiskManager = new DynamicDiskManager();
            for (VirtualDisk disk : disks) {
                if (DynamicDiskManager.isDynamicDisk(disk)) {
                    dynDiskManager.add(disk);
                    foundDynDisk = true;
                }
            }
            if (foundDynDisk) {
                System.err.println();
                System.err.println("  Logical Disk Manager Info");
                System.err.println();
                dynDiskManager.dump(new PrintWriter(System.err), "  ");
            }
        } catch (Exception e) {
            Debug.printStackTrace(e);
        }

        if (showContent) {
            for (String path : inFiles) {
                try (VirtualDisk disk = VirtualDisk.openDisk(path, FileAccess.Read, getUserName(), getPassword())) {
                    System.err.println();
                    System.err.printf("DISK CONTENTS (%s)\n", path);
                    System.err.println();
                    HexDump.generate(disk.getContent(), System.err);
                    System.err.println();
                }
            }
        }
    }

    private static void showDir(DiscDirectoryInfo dirInfo, int indent) {
try {
        System.err.printf("%s%-50s [%s]\n",
                          new String(new char[indent]).replace('\0', ' '),
                          cleanName(dirInfo.getFullName()),
                          Instant.ofEpochMilli(dirInfo.getCreationTimeUtc()));
        for (DiscDirectoryInfo subDir : dirInfo.getDirectories()) {
            showDir(subDir, indent + 0);
        }
} catch (Exception e) {
 Debug.printStackTrace(e);
 return;
}
        for (DiscFileInfo file : dirInfo.getFiles()) {
try {
            System.err.printf("%s%-50s [%s]\n",
                              new String(new char[indent]).replace('\0', ' '),
                              cleanName(file.getFullName()),
                              Instant.ofEpochMilli(file.getCreationTimeUtc()));
} catch (Exception e) {
 Debug.printStackTrace(e);
}
        }
    }

    private static final char[] BadNameChars = {
        '\r', '\n', '\0'
    };

    private static String cleanName(String name) {
        if (name.indexOf(BadNameChars[0]) >= 0 || name.indexOf(BadNameChars[1]) >= 0 || name.indexOf(BadNameChars[2]) >= 0) {
            return name.replace('\r', '?').replace('\n', '?').replace('\0', '?');
        }

        return name;
    }
}
