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

package FileExtract;

import java.io.File;
import java.io.IOException;

import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;

import DiscUtils.Common.HexDump;
import DiscUtils.Common.ProgramBase;
import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.FileSystemManager;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VirtualDiskManager;
import DiscUtils.Core.VolumeInfo;
import DiscUtils.Core.VolumeManager;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.Stream;


@Options
public class Program extends ProgramBase {
    @Option(option = "f", argName = "_diskFile", description = "The disks to inspect.", args = 1, required = true)
    private String _diskFile;

    @Option(option = "d", argName = "_inFilePath", description = "The path of the file to extract.", args = 1, required = true)
    private String _inFilePath;

    @Option(option = "o", argName = "_outFilePath", description = "The output file to be written.", args = 1, required = true)
    private String _outFilePath;

    @Option(option = "dt", argName = "_diskType", description = "Force the type of disk - use a file extension (one of TODO)")
    private String _diskType;

    @Option(option = "hd",
            argName = "_hexDump",
            description = "Output a HexDump of the NTFS stream to the console, in addition to writing it to the output file.")
    private boolean _hexDump;

    public static void main(String[] args) throws Exception {
        System.err.println("SupportedDiskTypes: " + String.join(", ", VirtualDiskManager.getSupportedDiskTypes()));
        Program program = new Program();
        Options.Util.bind(args, program);
        program.doRun();
    }

    protected void doRun() throws IOException {
        VolumeManager volMgr = new VolumeManager();
System.err.println("file: " + _diskFile);
        volMgr.addDisk(VirtualDisk.openDisk(_diskFile
                .replace(File.separator, "\\"), _diskType != null ? _diskType : null, FileAccess.Read, getUserName(), getPassword()));
        VolumeInfo volInfo = null;
        if (getVolumeId() != null && !getVolumeId().isEmpty()) {
            volInfo = volMgr.getVolume(getVolumeId());
        } else if (getPartition() >= 0) {
            volInfo = volMgr.getPhysicalVolumes().get(getPartition());
        } else {
            volInfo = volMgr.getLogicalVolumes().iterator().next();
        }
        DiscUtils.Core.FileSystemInfo fsInfo = FileSystemManager.detectFileSystems(volInfo).get(0);

        try (DiscFileSystem fs = fsInfo.open(volInfo, getFileSystemParameters())) {

            try (Stream source = fs.openFile(_inFilePath, FileMode.Open, FileAccess.Read)) {

                try (FileStream outFile = new FileStream(_outFilePath, FileMode.Create, FileAccess.ReadWrite)) {
                    pumpStreams(source, outFile);
                }
                if (_hexDump) {
                    source.setPosition(0);
                    HexDump.generate(source, System.out);
                }
            }
        }
    }

    private static void pumpStreams(Stream inStream, Stream outStream) {
        byte[] buffer = new byte[4096];
        int bytesRead = inStream.read(buffer, 0, 4096);
        while (bytesRead != 0) {
            outStream.write(buffer, 0, bytesRead);
            bytesRead = inStream.read(buffer, 0, 4096);
        }
    }
}
