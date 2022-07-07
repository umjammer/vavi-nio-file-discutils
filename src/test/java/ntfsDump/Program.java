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

package ntfsDump;

import java.io.IOException;
import java.io.PrintWriter;

import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;

import discUtils.common.ProgramBase;
import discUtils.core.VirtualDisk;
import discUtils.core.VolumeManager;
import discUtils.ntfs.NtfsFileSystem;
import discUtils.streams.SparseStream;
import discUtils.streams.block.BlockCacheStream;
import discUtils.streams.util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;


@Options
public class Program extends ProgramBase {
    @Option(option = "disk", description = "Paths to the disks to inspect.", args = 1, required = true)
    private String _diskFile; // TODO array

    @Option(option = "H", argName = "hidden",
            description = "Don't hide files and directories with the hidden attribute set in the directory listing.")
    private boolean _showHidden;

    @Option(option = "S", argName = "system",
            description = "Don't hide files and directories with the system attribute set in the directory listing.")
    private boolean _showSystem;

    @Option(option = "M", argName = "meta",
            description = "Don't hide files and directories that are part of the file system itself in the directory listing.")
    private boolean _showMeta;

    public static void main(String[] args) throws Exception {
        Program program = new Program();
        Options.Util.bind(args, program);
        program.run(args);
    }

//        return StandardSwitches.UserAndPassword | StandardSwitches.PartitionOrVolume;

    protected void doRun() throws IOException {
        VolumeManager volMgr = new VolumeManager();
String[] _diskFiles = new String[] { _diskFile };
        for (String disk : _diskFiles) {
            volMgr.addDisk(VirtualDisk.openDisk(disk, FileAccess.Read, getUserName(), getPassword()));
        }
        Stream partitionStream = null;
        if (getVolumeId() != null && !getVolumeId().isEmpty()) {
            partitionStream = volMgr.getVolume(getVolumeId()).open();
        } else if (getPartition() >= 0) {
            partitionStream = volMgr.getPhysicalVolumes().get(getPartition()).open();
        } else {
            partitionStream = volMgr.getLogicalVolumes().get(0).open();
        }
        SparseStream cacheStream = SparseStream.fromStream(partitionStream, Ownership.None);
        cacheStream = new BlockCacheStream(cacheStream, Ownership.None);
        NtfsFileSystem fs = new NtfsFileSystem(cacheStream);
        fs.getNtfsOptions().setHideHiddenFiles(!_showHidden);
        fs.getNtfsOptions().setHideSystemFiles(!_showSystem);
        fs.getNtfsOptions().setHideMetafiles(!_showMeta);
        fs.dump(new PrintWriter(System.err), "");
    }
}
