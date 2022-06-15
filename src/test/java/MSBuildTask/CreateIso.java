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

package MSBuildTask;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import DiscUtils.Iso9660.BootDeviceEmulation;
import DiscUtils.Iso9660.CDBuilder;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.Stream;


public class CreateIso implements Callable {
    static final Logger logger = Logger.getLogger(CreateIso.class.getName());

    public CreateIso() {
        setUseJoliet(true);
    }

    /**
     * The name of the ISO file to create.
     */
    private ITaskItem fileName;

    public ITaskItem getFileName() {
        return fileName;
    }

    public void setFileName(ITaskItem value) {
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
    private ITaskItem[] sourceFiles;

    public ITaskItem[] getSourceFiles() {
        return sourceFiles;
    }

    public void setSourceFiles(ITaskItem[] value) {
        sourceFiles = value;
    }

    /**
     * The boot image to add to the ISO.
     */
    private ITaskItem bootImage;

    public ITaskItem getBootImage() {
        return bootImage;
    }

    public void setBootImage(ITaskItem value) {
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
    private ITaskItem[] removeRoots = new ITaskItem[1];

    public ITaskItem[] getRemoveRoots() {
        return removeRoots;
    }

    public void setRemoveRoots(ITaskItem[] value) {
        removeRoots = value;
    }

    public Boolean call() {
        logger.info("Creating ISO file: '%s'", getFileName().ItemSpec);
        try {
            CDBuilder builder = new CDBuilder();
            builder.setUseJoliet(getUseJoliet());
            if (getVolumeLabel() != null && !getVolumeLabel().isEmpty()) {
                builder.setVolumeIdentifier(getVolumeLabel());
            }

            Stream bootImageStream = null;
            if (getBootImage() != null) {
                bootImageStream = new FileStream(getBootImage().GetMetadata("FullPath"), FileMode.Open, FileAccess.Read);
                builder.setBootImage(bootImageStream, BootDeviceEmulation.NoEmulation, 0);
                builder.setUpdateIsolinuxBootTable(getUpdateIsolinuxBootTable());
            }

            for (ITaskItem sourceFile : getSourceFiles()) {
                builder.addFile(GetDestinationPath(sourceFile), sourceFile.getMetadata("FullPath"));
            }
            try {
                builder.Build(getFileName().ItemSpec);
            } finally {
                if (bootImageStream != null) {
                    bootImageStream.close();
                }

            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return false;
        }

        return !logger.HasLoggedErrors;
    }

    private String getDestinationPath(ITaskItem sourceFile) {
        String fullPath = sourceFile.GetMetadata("FullPath");
        if (getRemoveRoots() != null) {
            for (Callable root : getRemoveRoots()) {
                String rootPath = root.getMetadata("FullPath");
                if (fullPath.startsWith(rootPath)) {
                    return fullPath.substring(rootPath.length());
                }
            }
        }

        // Not under a known root - so full path (minus drive)...
        return sourceFile.getMetadata("Directory") + sourceFile.getMetadata("FileName") + sourceFile.getMetadata("Extension");
    }
}
