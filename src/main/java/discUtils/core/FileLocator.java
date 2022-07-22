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

package discUtils.core;

import java.io.File;
import java.util.function.BiConsumer;

import discUtils.core.internal.Utilities;
import discUtils.setup.FileOpenEventArgs;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.Stream;


public abstract class FileLocator {
    public abstract boolean exists(String fileName);

    /**
     * Allows intercepting any file open operation
     *
     * Can be used to wrap the opened file for special use cases, modify the
     * parameters for opening files, validate file names and many more.
     */
    public static BiConsumer<Object, FileOpenEventArgs> openingFile;

    public Stream open(String fileName, FileMode mode, FileAccess access, FileShare share) {
        FileOpenEventArgs args = new FileOpenEventArgs(fileName, mode, access, share, this::openFile);
        if (openingFile != null) {
            openingFile.accept(this, args);
        }
        if (args.getResult() != null)
            return args.getResult();
        return openFile(args.getFileName(), args.getFileMode(), args.getFileAccess(), args.getFileShare());
    }

    protected abstract Stream openFile(String fileName, FileMode mode, FileAccess access, FileShare share);

    public abstract FileLocator getRelativeLocator(String path);

    public abstract String getFullPath(String path);

    public abstract String getDirectoryFromPath(String path);

    public abstract String getFileFromPath(String path);

    public abstract long getLastWriteTimeUtc(String path);

    public abstract boolean hasCommonRoot(FileLocator other);

    public abstract String resolveRelativePath(String path);

    public String makeRelativePath(FileLocator fileLocator, String path) {
        if (!hasCommonRoot(fileLocator)) {
            return null;
        }

        String ourFullPath = getFullPath("") + File.separator;
        String otherFullPath = fileLocator.getFullPath(path);

        return Utilities.makeRelativePath(otherFullPath, ourFullPath);
    }
}
