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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import DiscUtils.Core.FileLocator;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.FileShare;
import moe.yo3explorer.dotnetio4j.FileStream;
import moe.yo3explorer.dotnetio4j.Stream;

public final class LocalFileLocator extends FileLocator {
    private final String _dir;

    public LocalFileLocator(String dir) {
        _dir = dir;
    }

    public boolean exists(String fileName) {
        return Files.exists(Paths.get(_dir, fileName));
    }

    protected Stream openFile(String fileName, FileMode mode, FileAccess access, FileShare share) {
        return new FileStream(Paths.get(_dir, fileName).toString(), mode);
    }

    public FileLocator getRelativeLocator(String path) {
        return new LocalFileLocator(Paths.get(_dir, path).toString());
    }

    public String getFullPath(String path) {
        String combinedPath = Paths.get(_dir, path).toString();
        if (combinedPath.isEmpty()) {
            return System.getenv("PWD");
        }

        return Paths.get(combinedPath).toAbsolutePath().toString();
    }

    public String getDirectoryFromPath(String path) {
        return Paths.get(path).getParent().toString();
    }

    public String getFileFromPath(String path) {
        return Paths.get(path).getFileName().toString();
    }

    public long getLastWriteTimeUtc(String path) {
        try {
            return Files.getLastModifiedTime(Paths.get(_dir, path)).toMillis();
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    public boolean hasCommonRoot(FileLocator other) {
        LocalFileLocator otherLocal = other instanceof LocalFileLocator ? (LocalFileLocator) other : (LocalFileLocator) null;
        if (otherLocal == null) {
            return false;
        }

        // If the paths have drive specifiers, then common root depends on them having a common
        // drive letter.
        String otherDir = otherLocal._dir;
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
