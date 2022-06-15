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

import DiscUtils.SquashFs.SquashFileSystemBuilder;


public class CreateSquashFileSystem implements Callable<Boolean> {
    static final Logger logger = Logger.getLogger(CreateSquashFileSystem.class.getName());

    public CreateSquashFileSystem() {
    }

    /**
     * The name of the file to create, containing the filesystem image.
     */
    private ITaskItem fileName;

    public ITaskItem getFileName() {
        return fileName;
    }

    public void setFileName(ITaskItem value) {
        fileName = value;
    }

    /**
     * The files to add to the filesystem image.
     */
    private ITaskItem[] sourceFiles;

    public ITaskItem[] getSourceFiles() {
        return sourceFiles;
    }

    public void setSourceFiles(ITaskItem[] value) {
        sourceFiles = value;
    }

    /**
     * Sets the root to remove from the source files.
     *
     * If the source file is C:\MyDir\MySubDir\file.txt, and RemoveRoot is
     * C:\MyDir, the filesystem will contain \MySubDir\file.txt. If not
     * specified, the file would be named \MyDir\MySubDir\file.txt.
     */
    private ITaskItem removeRoot;

    public ITaskItem getRemoveRoot() {
        return removeRoot;
    }

    public void setRemoveRoot(ITaskItem value) {
        removeRoot = value;
    }

    public Boolean call() {
        logger.log(String.format("Creating SquashFS file: '%s'", getFileName().ItemSpec));
        try {
            SquashFileSystemBuilder builder = new SquashFileSystemBuilder();
            for (ITaskItem sourceFile : getSourceFiles()) {
                if (this.getRemoveRoot() != null) {
                    String location = (sourceFile.GetMetadata("FullPath")).Replace(this.getRemoveRoot().GetMetadata("FullPath"),
                                                                                   "");
                    builder.addFile(location, sourceFile.GetMetadata("FullPath"));
                } else {
                    builder.addFile(sourceFile.GetMetadata("Directory") + sourceFile.GetMetadata("FileName") +
                                    sourceFile.GetMetadata("Extension"),
                                    sourceFile.GetMetadata("FullPath"));
                }
            }
            builder.Build(getFileName().ItemSpec);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return false;
        }

        return !logger.HasLoggedErrors;
    }
}
