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

import java.io.IOException;

import discUtils.core.internal.Utilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.Stream;

public final class DiscFileLocator extends FileLocator {

    private final String basePath;

    private final DiscFileSystem fileSystem;

    public DiscFileLocator(DiscFileSystem fileSystem, String basePath) {
        this.fileSystem = fileSystem;
        this.basePath = basePath;
    }

    public boolean exists(String fileName) {
        try {
            return fileSystem.fileExists(Utilities.combinePaths(basePath, fileName));
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    protected Stream openFile(String fileName, FileMode mode, FileAccess access, FileShare share) {
        try {
            return fileSystem.openFile(Utilities.combinePaths(basePath, fileName), mode, access);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public FileLocator getRelativeLocator(String path) {
        return new DiscFileLocator(fileSystem, Utilities.combinePaths(basePath, path));
    }

    public String getFullPath(String path) {
        return Utilities.combinePaths(basePath, path);
    }

    public String getDirectoryFromPath(String path) {
        return Utilities.getDirectoryFromPath(path);
    }

    public String getFileFromPath(String path) {
        return Utilities.getFileFromPath(path);
    }

    public long getLastWriteTimeUtc(String path) {
        try {
            return fileSystem.getLastWriteTimeUtc(Utilities.combinePaths(basePath, path));
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public boolean hasCommonRoot(FileLocator other) {
        if (!(other instanceof DiscFileLocator)) {
            return false;
        }

        // common root if the same file system instance.
        return ((DiscFileLocator) other).fileSystem == fileSystem; // TODO object compare
    }

    public String resolveRelativePath(String path) {
        return Utilities.resolveRelativePath(basePath, path);
    }
}
