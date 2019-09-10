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

package DiscUtils.Core.Vfs;

import java.nio.file.attribute.BasicFileAttributes;


/**
 * Base class for directory entries in a file system.
 * 
 * File system implementations should have a class that derives from
 * this abstract class. If the file system implementation is read-only,
 * it is acceptable to throw
 * {@code NotImplementedException}
 * from methods
 * that attempt to modify the file system.
 */
public abstract class VfsDirEntry {
    /**
     * Gets the creation time of the file or directory.
     * 
     * May throw
     * {@code NotSupportedException}
     * if
     * {@code HasVfsTimeInfo}
     * is
     * {@code false}
     * .
     */
    public abstract long getCreationTimeUtc();

    /**
     * Gets the file attributes from the directory entry.
     * 
     * May throw
     * {@code NotSupportedException}
     * if
     * {@code HasVfsFileAttributes}
     * is
     * {@code false}
     * .
     */
    public abstract BasicFileAttributes getFileAttributes();

    /**
     * Gets the name of this directory entry.
     */
    public abstract String getFileName();

    /**
     * Gets a value indicating whether this directory entry contains file
     * attribute information.
     * Typically either always returns
     * {@code true}
     * or
     * {@code false}
     * .
     */
    public abstract boolean hasVfsFileAttributes();

    /**
     * Gets a value indicating whether this directory entry contains time
     * information.
     * Typically either always returns
     * {@code true}
     * or
     * {@code false}
     * .
     */
    public abstract boolean hasVfsTimeInfo();

    /**
     * Gets a value indicating whether this directory entry represents a
     * directory (rather than a file).
     */
    public abstract boolean isDirectory();

    /**
     * Gets a value indicating whether this directory entry represents a symlink
     * (rather than a file or directory).
     */
    public abstract boolean getIsSymlink();

    /**
     * Gets the last access time of the file or directory.
     * 
     * May throw
     * {@code NotSupportedException}
     * if
     * {@code HasVfsTimeInfo}
     * is
     * {@code false}
     * .
     */
    public abstract long getLastAccessTimeUtc();

    /**
     * Gets the last write time of the file or directory.
     * 
     * May throw
     * {@code NotSupportedException}
     * if
     * {@code HasVfsTimeInfo}
     * is
     * {@code false}
     * .
     */
    public abstract long getLastWriteTimeUtc();

    /**
     * Gets a version of FileName that can be used in wildcard matches.
     * 
     * The returned name, must have an extension separator '.', and not have any
     * optional version
     * information found in some files. The returned name is matched against a
     * wildcard patterns
     * such as "*.*".
     */
    public String getSearchName() {
        String fileName = getFileName();
        if (fileName.indexOf('.') == -1) {
            return fileName + ".";
        }

        return fileName;
    }

    /**
     * Gets a unique id for the file or directory represented by this directory
     * entry.
     */
    public abstract long getUniqueCacheId();

}
