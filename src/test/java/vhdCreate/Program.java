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

package vhdCreate;

import java.io.IOException;

import discUtils.common.ProgramBase;
import discUtils.streams.util.Ownership;
import discUtils.vhd.Disk;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.FileStream;
import dotnet4j.util.compat.Utilities;
import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;


@Options
public class Program extends ProgramBase {

    @Option(option = "new.vhd", description = "Path to the VHD file to create.", args = 1, required = true)
    private String sourceFile;

    @Option(option = "base.vhd", description = "For differencing disks, the path to the base disk.", args = 1, required = false)
    private String destFile;

    @Option(option = "t",
            argName = "type",
            description = "The type of disk to create, one of: fixed, dynamic, diff.  The default is dynamic.")
    private String typeSwitch = "type";

    @Option(option = "bs",
            argName = "blocksize",
            description = "For dynamic disks, the allocation int size for new disk regions in bytes.  The default is 2MB.    Use B, KB, MB, GB to specify units (units default to bytes if not specified).")
    private String blockSizeSwitch = "size";

    public static void main(String[] args) throws Exception {
        Program program = new Program();
        Options.Util.bind(args, program);
        program.run(args);
        System.exit(exitCode);
    }

    static int exitCode;

    protected void doRun() throws IOException {

        if ((typeSwitch != null && Utilities.equals(typeSwitch, "dynamic")) || typeSwitch == null) {
            long[] blockSize = new long[] {2 * 1024 * 1024};
            if (blockSizeSwitch != null) {
                boolean r = !discUtils.common.Utilities.tryParseDiskSize(blockSizeSwitch, blockSize);
                if (r) {
                    exitCode = 1;
                    return;
                }
            }

            if (blockSize[0] == 0 || ((blockSize[0] & 0x1FF) != 0) || !isPowerOfTwo(blockSize[0] / 512)) {
                System.err.println("ERROR: blocksize must be power of 2 sectors - e.g. 512B, 1KB, 2KB, 4KB, ...");
                exitCode = 2;
                return;
            }

            if (getDiskSize() <= 0) {
                System.err.println("ERROR: disk size must be greater than zero.");
                exitCode = 3;
                return;
            }

            try (FileStream fs = new FileStream(destFile, FileMode.Create, FileAccess.ReadWrite, FileShare.None)) {
                Disk.initializeDynamic(fs, Ownership.None, getDiskSize(), blockSize[0]);
            }
        } else if (Utilities.equals(typeSwitch, "diff")) {
            // Create Diff
            if (sourceFile == null) {
                exitCode = 1;
                return;
            }

            Disk.initializeDifferencing(destFile, sourceFile);
        } else if (Utilities.equals(typeSwitch, "fixed")) {
            if (getDiskSize() <= 0) {
                System.err.println("ERROR: disk size must be greater than zero.");
                exitCode = 3;
                return;
            }

            // Create Fixed disk
            try (FileStream fs = new FileStream(destFile, FileMode.Create, FileAccess.ReadWrite, FileShare.None)) {
                Disk.initializeFixed(fs, Ownership.None, getDiskSize());
            }
        } else {
            exitCode = 1;
        }
    }

    private static boolean isPowerOfTwo(long val) {
        while (val > 0) {
            if ((val & 0x1) != 0) {
                // If the low bit is set, it should be the only bit set
                if (val != 0x1) {
                    return false;
                }
            }
            val >>= 1;
        }
        return true;
    }
}
