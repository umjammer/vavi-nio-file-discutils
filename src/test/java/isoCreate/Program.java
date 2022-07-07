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

package isoCreate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;

import discUtils.common.ProgramBase;
import discUtils.iso9660.BootDeviceEmulation;
import discUtils.iso9660.CDBuilder;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;


@Options
public class Program extends ProgramBase {
    @Option(option = "iso_file", description = "The ISO file to create.", args = 1, required = false)
    private String _isoFileParam;

    @Option(option = "sourcedir", description = "The directory to be added to the ISO", args = 1, required = false)
    private String _srcDir;

    @Option(option = "bootimage", description = "The bootable disk image, to create a bootable ISO", args = 1, required = false)
    private String _bootImage;

    @Option(option = "vl", argName = "vollabel", args = 1, description = "Volume Label for the ISO file.")
    private String _volLabelSwitch = "label";

    public static void main(String[] args) throws Exception {
        Program program = new Program();
        Options.Util.bind(args, program);
        program.run(args);
    }

    protected void doRun() throws IOException {
        Path di = Paths.get(_srcDir);
        if (!Files.exists(di)) {
            System.err.println("The source directory doesn't exist!");
            System.exit(1);
        }

        CDBuilder builder = new CDBuilder();
        if (_volLabelSwitch != null) {
            builder.setVolumeIdentifier(_volLabelSwitch);
        }

        if (_bootImage != null) {
            builder.setBootImage(new FileStream(_bootImage, FileMode.Open, FileAccess.Read),
                                 BootDeviceEmulation.NoEmulation,
                                 0);
        }

        populateFromFolder(builder, di, di.toAbsolutePath().toString());
        builder.build(_isoFileParam);
    }

    private static void populateFromFolder(CDBuilder builder, Path di, String basePath) throws IOException {
        Files.list(di).forEach(f -> {
            if (!Files.isDirectory(f)) {
                String n = f.toAbsolutePath().toString().substring(basePath.length()).replace(File.separator, "\\");
                String p = f.toAbsolutePath().toString().replace(File.separator, "\\");
//Debug.println(n + ", " + p);
                builder.addFile(n, p);
            } else
                try {
                    populateFromFolder(builder, f, basePath);
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
        });
    }
}
