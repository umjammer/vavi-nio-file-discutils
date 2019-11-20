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

package VHDDump;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;

import DiscUtils.Common.ProgramBase;
import DiscUtils.Core.ReportLevels;
import DiscUtils.Vhd.DiskImageFile;
import DiscUtils.Vhd.DiskImageFileInfo;
import DiscUtils.Vhd.FileChecker;
import DiscUtils.Vhd.FileType;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.Stream;


@Options
public class Program extends ProgramBase {
    @Option(option = "vhd_file", description = "Path to the VHD file to inspect.", args = 1, required = true)
    private String _vhdFile;

    @Option(option = "nc", argName = "noCheck", description = "Don't check the VHD file format for corruption")
    private boolean _dontCheck;

    public static void main(String[] args) throws Exception {
        Program program = new Program();
        Options.Util.bind(args, program);
        program.run(args);
    }

    protected void doRun() throws IOException {
        if (!_dontCheck) {
            try (Stream s = new FileStream(_vhdFile, FileMode.Open, FileAccess.Read)) {
                FileChecker vhdChecker = new FileChecker(s);
                if (!vhdChecker.check(new PrintWriter(System.err), ReportLevels.All)) {
                    System.err.println("Aborting: Invalid VHD file");
                    System.exit(1);
                }
            }
        }

        try (DiskImageFile vhdFile = new DiskImageFile(_vhdFile, FileAccess.Read)) {
            DiskImageFileInfo info = vhdFile.getInformation();
            Path fileInfo = Paths.get(_vhdFile);
            System.err.println("File Info");
            System.err.println("---------");
            System.err.printf("           File Name: %s\n", fileInfo.toAbsolutePath());
            System.err.printf("           File Size: %s bytes\n", Files.size(fileInfo));
            System.err.printf("  File Creation Time: %s (UTC)\n", Files.readAttributes(fileInfo, BasicFileAttributes.class).creationTime());
            System.err.printf("     File Write Time: %s (UTC)\n", Files.getLastModifiedTime( fileInfo));
            System.err.println();
            System.err.println("Common Disk Info");
            System.err.println("-----------------");
            System.err.printf("              Cookie: %8x\n", info.getCookie());
            System.err.printf("            Features: %8x\n", info.getFeatures());
            System.err.printf(" File Format Version: %s.%s\n",
                               ((info.getFileFormatVersion() >> 16) & 0xFFFF),
                               (info.getFileFormatVersion() & 0xFFFF));
            System.err.printf("       Creation Time: %s (UTC)\n", info.getCreationTimestamp());
            System.err.printf("         Creator App: %8x\n", info.getCreatorApp());
            System.err.printf("     Creator Version: %s.%s\n",
                               ((info.getCreatorVersion() >> 16) & 0xFFFF),
                               (info.getCreatorVersion() & 0xFFFF));
            System.err.printf("     Creator Host OS: %s\n", info.getCreatorHostOS());
            System.err.printf("       Original Size: %s bytes\n", info.getOriginalSize());
            System.err.printf("        Current Size: %s bytes\n", info.getCurrentSize());
            System.err.printf("    Geometry (C/H/S): %s\n", info.getGeometry());
            System.err.printf("           Disk Type: %s\n", info.getDiskType());
            System.err.printf("            Checksum: %8x\n", info.getFooterChecksum());
            System.err.printf("           Unique Id: %s\n", info.getUniqueId());
            System.err.printf("         Saved State: %s\n", info.getSavedState());
            System.err.println();
            if (info.getDiskType() == FileType.Differencing || info.getDiskType() == FileType.Dynamic) {
                System.err.println();
                System.err.println("Dynamic Disk Info");
                System.err.println("-----------------");
                System.err.printf("              Cookie: %s\n", info.getDynamicCookie());
                System.err.printf("      Header Version: %s.%s\n",
                                   ((info.getDynamicHeaderVersion() >> 16) & 0xFFFF),
                                   (info.getDynamicHeaderVersion() & 0xFFFF));
                System.err.printf("         Block Count: %s\n", info.getDynamicBlockCount());
                System.err.printf("          Block Size: %s bytes\n", info.getDynamicBlockSize());
                System.err.printf("            Checksum: %8x\n", info.getDynamicChecksum());
                System.err.printf("    Parent Unique Id: %s\n", info.getDynamicParentUniqueId());
                System.err.printf("   Parent Write Time: %s (UTC)\n", info.getDynamicParentTimestamp());
                System.err.printf("         Parent Name: %s\n", info.getDynamicParentUnicodeName());
                System.err.print("    Parent Locations: ");
                for (Object __dummyForeachVar0 : info.getDynamicParentLocators()) {
                    String parentLocation = (String) __dummyForeachVar0;
                    System.err.printf("%s\n                      \n", parentLocation);
                }
                System.err.println();
            }
        }
    }
}
