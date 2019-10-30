//
// Copyright (c) 2008-2013, Kenneth Bell
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

package VHDXDump;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.UUID;

import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;

import DiscUtils.Common.ProgramBase;
import DiscUtils.Streams.Util.Range;
import DiscUtils.Vhdx.DiskImageFile;
import DiscUtils.Vhdx.DiskImageFileInfo;
import DiscUtils.Vhdx.HeaderInfo;
import DiscUtils.Vhdx.LogEntryInfo;
import DiscUtils.Vhdx.MetadataInfo;
import DiscUtils.Vhdx.MetadataTableInfo;
import DiscUtils.Vhdx.RegionInfo;
import DiscUtils.Vhdx.RegionTableInfo;
import dotnet4j.io.FileAccess;


@Options
public class Program extends ProgramBase {
    @Option(option = "vhdx_file", description = "Path to the VHDX file to inspect.", required = false)
    private String _vhdxFile;

    public static void Main(String[] args) throws Exception {
        Program program = new Program();
        program.run(args);
    }

    static final UUID EMPTY = new UUID(0, 0);

    protected void doRun() throws IOException {
        try (DiskImageFile vhdxFile = new DiskImageFile(_vhdxFile, FileAccess.Read)) {
            DiskImageFileInfo info = vhdxFile.getInformation();
            Path fileInfo = Paths.get(_vhdxFile);
            System.err.println("File Info");
            System.err.println("---------");
            System.err.printf("           File Name: %s\n", fileInfo.toAbsolutePath());
            System.err.printf("           File Size: %d (%d bytes)\n",
                              DiscUtils.Common.Utilities.approximateDiskSize(Files.size(fileInfo)),
                              Files.size(fileInfo));
            System.err.printf("  File Creation Time: %s (UTC)\n",
                              Files.readAttributes(fileInfo, BasicFileAttributes.class).creationTime());
            System.err.printf("     File Write Time: %s (UTC)\n", Files.getLastModifiedTime(fileInfo));
            System.err.println();
            System.err.println("VHDX File Info");
            System.err.println("--------------");
            System.err.printf("           Signature: %8x\n", info.getSignature());
            System.err.printf("             Creator: %8x\n", info.getCreator());
            System.err.printf("          Block Size: %1$d (0x%1$8X)\n", info.getBlockSize());
            System.err.printf("Leave Blocks Alloced: %d\n", info.leaveBlocksAllocated());
            System.err.printf("          Has Parent: %s\n", info.hasParent());
            System.err.printf("           Disk Size: %1$d (%2$d (0x%2$8X))\n",
                              DiscUtils.Common.Utilities.approximateDiskSize(info.getDiskSize()),
                              info.getDiskSize());
            System.err.printf(" Logical Sector Size: %1$d (0x%1$8X)\n", info.getLogicalSectorSize());
            System.err.printf("Physical Sector Size: %1$d (0x%1$8X)\n", info.getPhysicalSectorSize());
            System.err.printf(" Parent Locator Type: %s\n", info.getParentLocatorType());
            writeParentLocations(info);
            System.err.println();
            writeHeaderInfo(info.getFirstHeader());
            writeHeaderInfo(info.getSecondHeader());
            if (!info.getActiveHeader().getLogGuid().equals(EMPTY)) {
                System.err.println("Log Info (Active Sequence)");
                System.err.println("--------------------------");
                for (LogEntryInfo entry : info.getActiveLogSequence()) {
                    System.err.println("   Log Entry");
                    System.err.println("   ---------");
                    System.err.printf("         Sequence Number: %d\n", entry.getSequenceNumber());
                    System.err.printf("                    Tail: %d\n", entry.getTail());
                    System.err.printf("     Flushed File Offset: %1$d (0x%1$8X)\n", entry.getFlushedFileOffset());
                    System.err.printf("        Last File Offset: %1$d (0x%1$8X)\n", entry.getLastFileOffset());
                    System.err.printf("            File Extents: %s\n", entry.getIsEmpty() ? "<none>" : "");
                    for (Range extent : entry.getModifiedExtents()) {
                        System.err.printf("                          %1$d +%2$d  (0x%1$8X +0x%2$8X)\n",
                                          extent.getOffset(),
                                          extent.getCount());
                    }
                    System.err.println();
                }
            }

            RegionTableInfo regionTable = info.getRegionTable();
            System.err.println("Region Table Info");
            System.err.println("-----------------");
            System.err.printf("           Signature: %s\n", regionTable.getSignature());
            System.err.printf("            Checksum: %8x\n", regionTable.getChecksum());
            System.err.printf("         Entry Count: %d\n", regionTable.getCount());
            System.err.println();
            for (RegionInfo entry : regionTable) {
                System.err.println("Region Table Entry Info");
                System.err.println("-----------------------");
                System.err.printf("                Guid: %s\n", entry.getGuid());
                System.err.printf("     Well-Known Name: %s\n", entry.getWellKnownName());
                System.err.printf("         File Offset: %1$d (0x%1$8X)\n", entry.getFileOffset());
                System.err.printf("              Length: %1$d (0x%1$8X)\n", entry.getLength());
                System.err.printf("         Is Required: %s\n", entry.getIsRequired());
                System.err.println();
            }
            MetadataTableInfo metadataTable = info.getMetadataTable();
            System.err.println("Metadata Table Info");
            System.err.println("-------------------");
            System.err.printf("           Signature: %s\n", metadataTable.getSignature());
            System.err.printf("         Entry Count: %d\n", metadataTable.getCount());
            System.err.println();
            for (MetadataInfo entry : metadataTable) {
                System.err.println("Metadata Table Entry Info");
                System.err.println("-------------------------");
                System.err.printf("             Item Id: %s\n", entry.getItemId());
                System.err.printf("     Well-Known Name: %s\n", entry.getWellKnownName());
                System.err.printf("              Offset: %1$d (0x%1$8X)\n", entry.getOffset());
                System.err.printf("              Length: %1$d (0x%1$8X)\n", entry.getLength());
                System.err.printf("             Is User: %s\n", entry.isUser());
                System.err.printf("         Is Required: %s\n", entry.isRequired());
                System.err.printf("     Is Virtual Disk: %s\n", entry.isVirtualDisk());
                System.err.println();
            }
        }
    }

    private static void writeParentLocations(DiskImageFileInfo info) {
        System.err.print("    Parent Locations: ");
        boolean first = true;
        for (Map.Entry<String, String> entry : info.getParentLocatorEntries().entrySet()) {
            if (!first) {
                System.err.println("                      ");
            }

            first = false;
            System.err.printf("%s -> %s\n", entry.getKey(), entry.getValue());
        }
        if (first) {
            System.err.println();
        }
    }

    private void writeHeaderInfo(HeaderInfo info) {
        System.err.println("Header Info");
        System.err.println("-----------");
        System.err.printf("    Header Signature: %s\n", info.getSignature());
        System.err.printf("     Sequence Number: %d\n", info.getSequenceNumber());
        System.err.printf("            Checksum: %8x\n", info.getChecksum());
        System.err.printf("     File Write Guid: %s\n", info.getFileWriteGuid());
        System.err.printf("     Data Write Guid: %s\n", info.getDataWriteGuid());
        System.err.printf("            Log Guid: %s\n", info.getLogGuid());
        System.err.printf("         Log Version: %d\n", info.getLogVersion());
        System.err.printf("             Version: %s\n", info.getVersion());
        System.err.printf("          Log Length: %1$d (0x%1$8X)\n", info.getLogLength());
        System.err.printf("     Log File Offset: %1$s (0x%1$8X)\n", info.getLogOffset());
        System.err.println();
    }
}
