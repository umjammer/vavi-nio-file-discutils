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

package DmgExtract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;

import DiscUtils.Common.ProgramBase;
import DiscUtils.Core.DiscDirectoryInfo;
import DiscUtils.Core.DiscFileInfo;
import DiscUtils.Core.FileSystemInfo;
import DiscUtils.Core.FileSystemManager;
import DiscUtils.Core.PhysicalVolumeInfo;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VolumeManager;
import DiscUtils.HfsPlus.HfsPlusFileSystem;
import dotnet4j.io.File;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;


@Options
class Program extends ProgramBase {
    @Option(option = "dmg", description = "Path to the .dmg file from which to extract the files", args = 1, required = true)
    String _dmg;

    @Option(option = "folder", description = "Paths to the folders from which to extract the files.", args = 1, required = true)
    String _folder;

    @Option(option = "out", description = "Paths to the folders from which to extract the files.", args = 1)
    String _out = System.getenv("user.dir");

    @Option(option = "r", argName = "recursive", description = "Include all subfolders of the folder specified")
    boolean _recursive;

    public static void main(String[] args) throws Exception {
        Program program = new Program();
        Options.Util.bind(args, program);
        program.run(args);
    }

    protected void doRun() throws IOException {
        try (VirtualDisk disk = VirtualDisk.openDisk(_dmg, FileAccess.Read)) {
            // Find the first (and supposedly, only, HFS partition)

            for (PhysicalVolumeInfo volume : VolumeManager.getPhysicalVolumes(disk)) {
                for (FileSystemInfo fileSystem : FileSystemManager.detectFileSystems(volume)) {
                    if (fileSystem.getName().equals("HFS+")) {
                        try (HfsPlusFileSystem hfs = (HfsPlusFileSystem) fileSystem.open(volume)) {
                            DiscDirectoryInfo source = hfs.getDirectoryInfo(_folder.replace(FS, "\\"));
                            Path target = Paths.get(_out, source.getFullName().replace("\\", FS));

                            Files.deleteIfExists(target);

                            Files.createDirectories(target);

                            copyDirectory(source, target, _recursive);
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
            String n = target.resolve(childFile.getName()).toString().replace(FS, "\\");
            try (Stream sourceStream = childFile.openRead();
                 Stream targetStream = File.openWrite(n)) {
                sourceStream.copyTo(targetStream);
            }
        }
    }
}
