//
// Copyright (c) 2014, Quamotion
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

package dmgExtract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import discUtils.common.ProgramBase;
import discUtils.core.DiscDirectoryInfo;
import discUtils.core.DiscFileInfo;
import discUtils.core.FileSystemInfo;
import discUtils.core.FileSystemManager;
import discUtils.core.PhysicalVolumeInfo;
import discUtils.core.VirtualDisk;
import discUtils.core.VolumeManager;
import discUtils.hfsPlus.HfsPlusFileSystem;
import dotnet4j.io.File;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;
import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;


@Options
class Program extends ProgramBase {

    @Option(option = "dmg", description = "Path to the .dmg file from which to extract the files", args = 1, required = true)
    String dmg;

    @Option(option = "folder", description = "Paths to the folders from which to extract the files.", args = 1, required = true)
    String folder;

    @Option(option = "out", description = "Paths to the folders from which to extract the files.", args = 1)
    String out = System.getenv("user.dir");

    @Option(option = "r", argName = "recursive", description = "Include all subfolders of the folder specified")
    boolean recursive;

    public static void main(String[] args) throws Exception {
        Program program = new Program();
        Options.Util.bind(args, program);
        program.run(args);
    }

    @Override
    protected void doRun() throws IOException {
        try (VirtualDisk disk = VirtualDisk.openDisk(dmg, FileAccess.Read)) {
            // Find the first (and supposedly, only, HFS partition)

            for (PhysicalVolumeInfo volume : VolumeManager.getPhysicalVolumes(disk)) {
                for (FileSystemInfo fileSystem : FileSystemManager.detectFileSystems(volume)) {
                    if (fileSystem.getName().equals("HFS+")) {
                        try (HfsPlusFileSystem hfs = (HfsPlusFileSystem) fileSystem.open(volume)) {
                            DiscDirectoryInfo source = hfs.getDirectoryInfo(folder);
                            Path target = Paths.get(out, source.getFullName());

                            Files.deleteIfExists(target);

                            Files.createDirectories(target);

                            copyDirectory(source, target, recursive);
                        }
                        break;
                    }
                }
            }
        }
    }

    private static final String FS = java.io.File.separator;

    private void copyDirectory(DiscDirectoryInfo source, Path target, boolean recurse) throws IOException {
        if (recurse) {
            for (DiscDirectoryInfo childDiscDirectory : source.getDirectories()) {
                Path childDirectory = Files.createDirectory(target.resolve(childDiscDirectory.getName()));
                copyDirectory(childDiscDirectory, childDirectory, recurse);
            }
        }

        System.err.println(source.getName());

        for (DiscFileInfo childFile : source.getFiles()) {
            String n = target.resolve(childFile.getName()).toString();
            try (Stream sourceStream = childFile.openRead();
                 Stream targetStream = File.openWrite(n)) {
                sourceStream.copyTo(targetStream);
            }
        }
    }
}
