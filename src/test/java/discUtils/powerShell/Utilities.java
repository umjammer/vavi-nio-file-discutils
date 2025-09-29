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

package discUtils.powerShell;

import java.nio.file.Paths;

import discUtils.core.DiscDirectoryInfo;
import discUtils.core.DiscFileInfo;
import discUtils.hfsPlus.FileInfo;
import dotnet4j.io.File;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileNotFoundException;
import dotnet4j.io.FileShare;
import dotnet4j.io.IOException;
import dotnet4j.io.Path;
import dotnet4j.io.Stream;


public class Utilities {
    /**
     * Replace all ':' characters with '#'.
     *
     * @param path The path to normalize
     * @return The normalized path
     *         powerShell has a bug that prevents tab-completion if the paths
     *         contain ':'
     *         characters, so in the external path for this provider we encode
     *         ':' as '#'.
     */
    public static String normalizePath(String path) {
        return path.replace(':', '#');
    }

    /**
     * Replace all '#' characters with ':'.
     *
     * @param path The path to normalize
     * @return The normalized path
     *         powerShell has a bug that prevents tab-completion if the paths
     *         contain ':'
     *         characters, so in the external path for this provider we encode
     *         ':' as '#'.
     */
    public static String denormalizePath(String path) {
        return path.replace('#', ':');
    }

    public static Stream createPsPath(SessionState session, String filePath) throws java.io.IOException {
        String parentPath = session.Path.ParseParent(filePath, null);
        String childName = session.Path.ParseChildName(filePath);
        Object parentItems = session.InvokeProvider.Item.get(parentPath);
        if (parentItems.size() > 1) {
            throw new IOException("powerShell path %s is ambiguous".formatted(parentPath));
        } else if (parentItems.size() < 1) {
            throw new FileNotFoundException("No such directory");
        }

        DirectoryInfo parentAsDir = parentItems[0].BaseObject instanceof DirectoryInfo ? (DirectoryInfo) parentItems[0].BaseObject
                                                                                       : null;
        if (parentAsDir != null) {
            return File.create(Path.combine(parentAsDir.FullName, childName));
        }

        DiscDirectoryInfo parentAsDiscDir = parentItems[0].BaseObject instanceof DiscDirectoryInfo ? (DiscDirectoryInfo) parentItems[0].BaseObject
                                                                                                   : null;
        if (parentAsDiscDir != null) {
            return parentAsDiscDir.getFileSystem()
                    .openFile(Paths.get(parentAsDiscDir.getFullName(), childName).toString(),
                              FileMode.Create,
                              FileAccess.ReadWrite);
        }

        throw new FileNotFoundException("Path is not a directory " + parentPath);
    }

    public static Stream openPsPath(SessionState session, String filePath, FileAccess access, FileShare share) {
        Object items = session.InvokeProvider.Item.get(filePath);
        if (items.size() == 1) {
            FileInfo itemAsFile = items[0].BaseObject instanceof FileInfo ? (FileInfo) items[0].BaseObject : null;
            if (itemAsFile != null) {
                return itemAsFile.open(FileMode.Open, access, share);
            }

            DiscFileInfo itemAsDiscFile = items[0].BaseObject instanceof DiscFileInfo ? (DiscFileInfo) items[0].BaseObject
                                                                                      : null;
            if (itemAsDiscFile != null) {
                return itemAsDiscFile.open(FileMode.Open, access);
            }

            throw new FileNotFoundException("Path is not a file " + filePath);
        } else if (items.size() > 1) {
            throw new IOException("powerShell path %s is ambiguous".formatted(filePath));
        } else {
            throw new FileNotFoundException("No such file " + filePath);
        }
    }

    public static String resolvePsPath(SessionState session, String filePath) {
        Object paths = session.Path.getResolvedPSPathFromPSPath(filePath);
        if (paths.size() > 1) {
            throw new IOException("powerShell path %s is ambiguous".formatted(filePath));
        } else if (paths.size() < 1) {
            throw new IOException("powerShell path %s not found".formatted(filePath));
        }

        return paths[0].Path;
    }
}
