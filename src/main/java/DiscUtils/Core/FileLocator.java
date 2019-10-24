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

package DiscUtils.Core;

import java.io.IOException;
import java.util.function.BiConsumer;

import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Setup.FileOpenEventArgs;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.Stream;


public abstract class FileLocator {
    public abstract boolean exists(String fileName) throws IOException;

    public static BiConsumer<Object, FileOpenEventArgs> openingFile;

    public Stream open(String fileName, FileMode mode, FileAccess access, FileShare share) {
        FileOpenEventArgs args = new FileOpenEventArgs(fileName, mode, access, share, (fileName1, mode1, access1, share1) -> {
            try {
                return openFile(fileName1, mode1, access1, share1);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        });
        if (openingFile != null) {
            openingFile.accept(this, args);
        }
        if (args.getResult() != null)
            return args.getResult();

        try {
            return openFile(args.getFileName(), args.getFileMode(), args.getFileAccess(), args.getFileShare());
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    protected abstract Stream openFile(String fileName, FileMode mode, FileAccess access, FileShare share) throws IOException;

    public abstract FileLocator getRelativeLocator(String path);

    public abstract String getFullPath(String path);

    public abstract String getDirectoryFromPath(String path);

    public abstract String getFileFromPath(String path);

    public abstract long getLastWriteTimeUtc(String path) throws IOException;

    public abstract boolean hasCommonRoot(FileLocator other);

    public abstract String resolveRelativePath(String path);

    public String makeRelativePath(FileLocator fileLocator, String path) {
        if (!hasCommonRoot(fileLocator)) {
            return null;
        }

        String ourFullPath = getFullPath("") + "\\";
        String otherFullPath = fileLocator.getFullPath(path);
        return Utilities.makeRelativePath(otherFullPath, ourFullPath);
    }
}
