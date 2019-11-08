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

import DiscUtils.Core.Internal.Utilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.Stream;

public final class DiscFileLocator extends FileLocator {
    private final String _basePath;

    private final DiscFileSystem _fileSystem;

    public DiscFileLocator(DiscFileSystem fileSystem, String basePath) {
        _fileSystem = fileSystem;
        _basePath = basePath;
    }

    public boolean exists(String fileName) {
        try {
            return _fileSystem.fileExists(Utilities.combinePaths(_basePath, fileName));
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    protected Stream openFile(String fileName, FileMode mode, FileAccess access, FileShare share) {
        try {
            return _fileSystem.openFile(Utilities.combinePaths(_basePath, fileName), mode, access);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public FileLocator getRelativeLocator(String path) {
        return new DiscFileLocator(_fileSystem, Utilities.combinePaths(_basePath, path));
    }

    public String getFullPath(String path) {
        return Utilities.combinePaths(_basePath, path);
    }

    public String getDirectoryFromPath(String path) {
        return Utilities.getDirectoryFromPath(path);
    }

    public String getFileFromPath(String path) {
        return Utilities.getFileFromPath(path);
    }

    public long getLastWriteTimeUtc(String path) {
        try {
            return _fileSystem.getLastWriteTimeUtc(Utilities.combinePaths(_basePath, path));
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public boolean hasCommonRoot(FileLocator other) {
        if (!DiscFileLocator.class.isInstance(other)) {
            return false;
        }

        // Common root if the same file system instance.
        return DiscFileLocator.class.cast(other)._fileSystem == _fileSystem;
    }

    public String resolveRelativePath(String path) {
        return Utilities.resolveRelativePath(_basePath, path);
    }
}
