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

package msBuildTask;

import java.nio.file.Paths;

import discUtils.iso9660.BootDeviceEmulation;
import discUtils.iso9660.CDBuilder;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.Stream;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;


@Mojo(name = "discutil-plugin-create-iso", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class CreateIso implements org.apache.maven.plugin.Mojo {

    private Log log;

    public CreateIso() {
        useJoliet = true;
    }

    /**
     * The name of the ISO file to create.
     */
    private String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String value) {
        fileName = value;
    }

    /**
     * Whether to use Joliet encoding for the ISO (default true).
     */
    private boolean useJoliet;

    public boolean getUseJoliet() {
        return useJoliet;
    }

    public void setUseJoliet(boolean value) {
        useJoliet = value;
    }

    /**
     * The label for the ISO (may be truncated if too long)
     */
    private String volumeLabel;

    public String getVolumeLabel() {
        return volumeLabel;
    }

    public void setVolumeLabel(String value) {
        volumeLabel = value;
    }

    /**
     * The files to add to the ISO.
     */
    private String[] sourceFiles;

    public String[] getSourceFiles() {
        return sourceFiles;
    }

    public void setSourceFiles(String[] value) {
        sourceFiles = value;
    }

    /**
     * The boot image to add to the ISO.
     */
    private String bootImage;

    public String getBootImage() {
        return bootImage;
    }

    public void setBootImage(String value) {
        bootImage = value;
    }

    /**
     * Whether to patch to boot image (per ISOLINUX requireents).
     *
     * Unless patched, ISOLINUX will indicate a checksum error upon boot.
     */
    private boolean updateIsolinuxBootTable;

    public boolean getUpdateIsolinuxBootTable() {
        return updateIsolinuxBootTable;
    }

    public void setUpdateIsolinuxBootTable(boolean value) {
        updateIsolinuxBootTable = value;
    }

    /**
    * Sets the root to remove from the source files.
    *
    * If the source file is C:\MyDir\MySubDir\file.txt, and RemoveRoot is C:\MyDir, the ISO will
    * contain \MySubDir\file.txt.  If not specified, the file would be named \MyDir\MySubDir\file.txt.
    */
    private String[] removeRoots = new String[1];

    public String[] getRemoveRoots() {
        return removeRoots;
    }

    public void setRemoveRoots(String[] value) {
        removeRoots = value;
    }

    private String getDestinationPath(String sourceFile) {
        String fullPath = Paths.get(sourceFile).toAbsolutePath().toString();
        if (getRemoveRoots() != null) {
            for (String root : getRemoveRoots()) {
                String rootPath = Paths.get(root).toAbsolutePath().toString();
                if (fullPath.startsWith(rootPath)) {
                    return fullPath.substring(rootPath.length());
                }
            }
        }

        // Not under a known root - so full path (minus drive)...
        return sourceFile;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log.info(String.format("Creating ISO file: '%s'", fileName));
        try {
            CDBuilder builder = new CDBuilder();
            builder.setUseJoliet(getUseJoliet());
            if (getVolumeLabel() != null && !getVolumeLabel().isEmpty()) {
                builder.setVolumeIdentifier(getVolumeLabel());
            }

            Stream bootImageStream = null;
            if (getBootImage() != null) {
                bootImageStream = new FileStream(Paths.get(bootImage).toAbsolutePath().toString(), FileMode.Open, FileAccess.Read);
                builder.setBootImage(bootImageStream, BootDeviceEmulation.NoEmulation, 0);
                builder.setUpdateIsolinuxBootTable(getUpdateIsolinuxBootTable());
            }

            for (String sourceFile : getSourceFiles()) {
                builder.addFile(getDestinationPath(sourceFile), Paths.get(sourceFile).toAbsolutePath().toString());
            }
            try {
                builder.build(fileName);
            } finally {
                if (bootImageStream != null) {
                    bootImageStream.close();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void setLog(Log log) {
        this.log = log;
    }

    @Override
    public Log getLog() {
        return log;
    }
}
