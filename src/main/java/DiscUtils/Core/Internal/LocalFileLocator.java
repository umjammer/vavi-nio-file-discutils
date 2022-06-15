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

package DiscUtils.Core.Internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import DiscUtils.Core.FileLocator;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.FileStream;
import dotnet4j.io.Stream;

public final class LocalFileLocator extends FileLocator {
    private final String _dir;

    public LocalFileLocator(String dir) {
        _dir = dir;
    }

    public boolean exists(String fileName) {
        return Files.exists(Paths.get(Utilities.combinePaths(_dir, fileName)));
    }

    protected Stream openFile(String fileName, FileMode mode, FileAccess access, FileShare share) {
        return new FileStream(Utilities.combinePaths(_dir, fileName).replace("\\", File.separator), mode, access, share);
    }

    public FileLocator getRelativeLocator(String path) {
        return new LocalFileLocator(Utilities.combinePaths(_dir, path));
    }

    public String getFullPath(String path) {
        String combinedPath = Utilities.combinePaths(_dir, path);
        if (combinedPath.isEmpty()) {
            return System.getProperty("user.dir").replace(File.separator, "\\");
        }

        return Paths.get(combinedPath).toAbsolutePath().toString().replace(File.separator, "\\");
    }

    public String getDirectoryFromPath(String path) {
        return Utilities.getDirectoryFromPath(path);
    }

    public String getFileFromPath(String path) {
        return Utilities.getFileFromPath(path);
    }

    public long getLastWriteTimeUtc(String path) {
        try {
            return Files.getLastModifiedTime(Paths.get(_dir, path)).toMillis();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public boolean hasCommonRoot(FileLocator other) {
        if (!(other instanceof LocalFileLocator)) {
            return false;
        }

        // If the paths have drive specifiers, then common root depends on them having a common
        // drive letter.
        String otherDir = ((LocalFileLocator) other)._dir;
        if (otherDir.length() >= 2 && _dir.length() >= 2) {
            if (otherDir.charAt(1) == ':' && _dir.charAt(1) == ':') {
                return Character.toUpperCase(otherDir.charAt(0)) == Character.toUpperCase(_dir.charAt(0));
            }
        }

        return true;
    }

    public String resolveRelativePath(String path) {
        return Utilities.resolveRelativePath(_dir, path);
    }
}
