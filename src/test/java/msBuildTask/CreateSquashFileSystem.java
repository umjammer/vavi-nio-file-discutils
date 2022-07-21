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

import discUtils.squashFs.SquashFileSystemBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;


@Mojo(name = "discutil-plugin-create-squashfs", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class CreateSquashFileSystem implements  org.apache.maven.plugin.Mojo {

    private Log log;

    /**
     * The name of the file to create, containing the filesystem image.
     */
    private String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String value) {
        fileName = value;
    }

    /**
     * The files to add to the filesystem image.
     */
    private String[] sourceFiles;

    public String[] getSourceFiles() {
        return sourceFiles;
    }

    public void setSourceFiles(String[] value) {
        sourceFiles = value;
    }

    /**
     * Sets the root to remove from the source files.
     *
     * If the source file is C:\MyDir\MySubDir\file.txt, and RemoveRoot is
     * C:\MyDir, the filesystem will contain \MySubDir\file.txt. If not
     * specified, the file would be named \MyDir\MySubDir\file.txt.
     */
    private String removeRoot;

    public String getRemoveRoot() {
        return removeRoot;
    }

    public void setRemoveRoot(String value) {
        removeRoot = value;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log.info(String.format("Creating SquashFS file: '%s'", fileName));
        try {
            SquashFileSystemBuilder builder = new SquashFileSystemBuilder();
            for (String sourceFile : sourceFiles) {
                if (this.removeRoot != null) {
                    String location = Paths.get(sourceFile).toAbsolutePath().toString().replace(Paths.get(this.removeRoot).toAbsolutePath().toString(), "");
                    builder.addFile(location, Paths.get(sourceFile).toAbsolutePath().toString());
                } else {
                    builder.addFile(sourceFile, Paths.get(sourceFile).toAbsolutePath().toString());
                }
            }
            builder.build(fileName);
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
