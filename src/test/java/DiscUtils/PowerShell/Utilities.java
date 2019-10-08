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

package DiscUtils.PowerShell;

import java.io.File;
import java.nio.file.Paths;

import DiscUtils.Core.DiscDirectoryInfo;
import DiscUtils.Core.DiscFileInfo;
import DiscUtils.HfsPlus.FileInfo;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.FileNotFoundException;
import moe.yo3explorer.dotnetio4j.FileShare;
import moe.yo3explorer.dotnetio4j.IOException;
import moe.yo3explorer.dotnetio4j.Stream;


public class Utilities {
    /**
     * Replace all ':' characters with '#'.
     *
     * @param path The path to normalize
     * @return The normalized path
     *         PowerShell has a bug that prevents tab-completion if the paths
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
     *         PowerShell has a bug that prevents tab-completion if the paths
     *         contain ':'
     *         characters, so in the external path for this provider we encode
     *         ':' as '#'.
     */
    public static String denormalizePath(String path) {
        return path.replace('#', ':');
    }

    public static Stream createPsPath(SessionState session, String filePath) {
        String parentPath = session.Path.ParseParent(filePath, null);
        String childName = session.Path.ParseChildName(filePath);
        Object parentItems = session.InvokeProvider.Item.Get(parentPath);
        if (parentItems.size() > 1) {
            throw new IOException(String.format("PowerShell path {0} is ambiguous", parentPath));
        } else if (parentItems.size() < 1) {
            throw new FileNotFoundException("No such directory");
        }

        DirectoryInfo parentAsDir = parentItems[0].BaseObject instanceof DirectoryInfo ? (DirectoryInfo) parentItems[0].BaseObject
                                                                                       : (DirectoryInfo) null;
        if (parentAsDir != null) {
            return File.Create(Path.Combine(parentAsDir.FullName, childName));
        }

        DiscDirectoryInfo parentAsDiscDir = parentItems[0].BaseObject instanceof DiscDirectoryInfo ? (DiscDirectoryInfo) parentItems[0].BaseObject
                                                                                                   : (DiscDirectoryInfo) null;
        if (parentAsDiscDir != null) {
            return parentAsDiscDir.getFileSystem()
                    .openFile(Paths.get(parentAsDiscDir.getFullName(), childName).toString(),
                              FileMode.Create,
                              FileAccess.ReadWrite);
        }

        throw new FileNotFoundException("Path is not a directory " + parentPath);
    }

    public static Stream openPsPath(SessionState session, String filePath, FileAccess access, FileShare share) {
        Object items = session.InvokeProvider.Item.Get(filePath);
        if (items.size() == 1) {
            FileInfo itemAsFile = items[0].BaseObject instanceof FileInfo ? (FileInfo) items[0].BaseObject : (FileInfo) null;
            if (itemAsFile != null) {
                return itemAsFile.open(FileMode.Open, access, share);
            }

            DiscFileInfo itemAsDiscFile = items[0].BaseObject instanceof DiscFileInfo ? (DiscFileInfo) items[0].BaseObject
                                                                                      : (DiscFileInfo) null;
            if (itemAsDiscFile != null) {
                return itemAsDiscFile.open(FileMode.Open, access);
            }

            throw new FileNotFoundException("Path is not a file " + filePath);
        } else if (items.size() > 1) {
            throw new IOException(String.format("PowerShell path {0} is ambiguous", filePath));
        } else {
            throw new FileNotFoundException("No such file " + filePath);
        }
    }

    public static String resolvePsPath(SessionState session, String filePath) {
        Object paths = session.Path.GetResolvedPSPathFromPSPath(filePath);
        if (paths.size() > 1) {
            throw new IOException(String.format("PowerShell path {0} is ambiguous", filePath));
        } else if (paths.size() < 1) {
            throw new IOException(String.format("PowerShell path {0} not found", filePath));
        }

        return paths[0].Path;
    }
}
