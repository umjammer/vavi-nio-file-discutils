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
import java.util.EnumSet;

import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Core.Internal.Utilities;
import dotnet4j.io.compat.StringUtilities;


/**
 * Provides the base class for both {@link DiscFileInfo} and
 * {@link DiscDirectoryInfo} objects.
 */
public class DiscFileSystemInfo {
    public DiscFileSystemInfo(DiscFileSystem fileSystem, String path) {
        if (path == null) {
            throw new NullPointerException("path");
        }

        _fileSystem = fileSystem;
        _path = path.replaceAll(StringUtilities.escapeForRegex("(^\\*|\\*$)"), "");
//Debug.println(_path);
    }

    /**
     * Gets or sets the {@link FileAttributes} of the current
     * {@link DiscFileSystemInfo} object.
     */
    public EnumSet<FileAttributes> getAttributes() {
        try {
            return FileAttributes.toEnumSet(getFileSystem().getAttributes(getPath()));
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public void setAttributes(EnumSet<FileAttributes> value) {
        try {
            getFileSystem().setAttributes(getPath(), FileAttributes.toMap(value));
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets or sets the creation time (in local time) of the current
     * {@link DiscFileSystemInfo} object.
     */
    public long getCreationTime() {
        return getCreationTimeUtc();
    }

    public void setCreationTime(long value) {
        setCreationTimeUtc(value);
    }

    /**
     * Gets or sets the creation time (in UTC) of the current
     * {@link DiscFileSystemInfo} object.
     */
    public long getCreationTimeUtc() {
        try {
            return getFileSystem().getCreationTimeUtc(getPath());
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public void setCreationTimeUtc(long value) {
        try {
            getFileSystem().setCreationTimeUtc(getPath(), value);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets a value indicating whether the file system object exists.
     */
    public boolean exists() {
        try {
            return getFileSystem().exists(getPath());
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets the extension part of the file or directory name.
     */
    public String getExtension() {
        String name = getName();
        int sepIdx = name.lastIndexOf('.');
        if (sepIdx >= 0) {
            return name.substring(sepIdx + 1);
        }

        return "";
    }

    /**
     * Gets the file system the referenced file or directory exists on.
     */
    private DiscFileSystem _fileSystem;

    public DiscFileSystem getFileSystem() {
        return _fileSystem;
    }

    /**
     * Gets the full path of the file or directory.
     */
    public String getFullName() {
        return getPath();
    }

    /**
     * Gets or sets the last time (in local time) the file or directory was
     * accessed. Read-only file systems will never update this value, it will remain
     * at a fixed value.
     */
    public long getLastAccessTime() {
        return getLastAccessTimeUtc();
    }

    public void setLastAccessTime(long value) {
        setLastAccessTimeUtc(value);
    }

    /**
     * Gets or sets the last time (in UTC) the file or directory was accessed.
     * Read-only file systems will never update this value, it will remain at a
     * fixed value.
     */
    public long getLastAccessTimeUtc() {
        try {
            return getFileSystem().getLastAccessTimeUtc(getPath());
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public void setLastAccessTimeUtc(long value) {
        try {
            getFileSystem().setLastAccessTimeUtc(getPath(), value);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets or sets the last time (in local time) the file or directory was written
     * to.
     */
    public long getLastWriteTime() {
        return getLastWriteTimeUtc();
    }

    public void setLastWriteTime(long value) {
        setLastWriteTimeUtc(value);
    }

    /**
     * Gets or sets the last time (in UTC) the file or directory was written to.
     */
    public long getLastWriteTimeUtc() {
        try {
            return getFileSystem().getLastWriteTimeUtc(getPath());
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public void setLastWriteTimeUtc(long value) {
        try {
            getFileSystem().setLastWriteTimeUtc(getPath(), value);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets the name of the file or directory.
     */
    public String getName() {
        return Utilities.getFileFromPath(getPath());
    }

    /**
     * Gets the {@link DiscDirectoryInfo} of the directory containing the current
     * {@link #DiscFileSystemInfo} object.
     */
    public DiscDirectoryInfo getParent() {
        if (getPath() == null || getPath().isEmpty()) {
            return null;
        }

        return new DiscDirectoryInfo(getFileSystem(), Utilities.getDirectoryFromPath(getPath()));
    }

    /**
     * Gets the path to the referenced file.
     */
    private String _path;

    protected String getPath() {
        return _path;
    }

    /**
     * Deletes a file or directory.
     */
    public void delete() {
        try {
            if (getAttributes().contains(FileAttributes.Directory)) {
                getFileSystem().deleteDirectory(getPath());
            } else {
                getFileSystem().deleteFile(getPath());
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Indicates if {@code obj} is equivalent to this object.
     *
     * @param obj The object to compare.
     * @return {@code true} if {@code obj} is equivalent, else {@code false} .
     */
    public boolean equals(Object obj) {
        DiscFileSystemInfo asInfo = obj instanceof DiscFileSystemInfo ? (DiscFileSystemInfo) obj : (DiscFileSystemInfo) null;
        if (obj == null) {
            return false;
        }

        return getPath().equals(asInfo.getPath()) && getFileSystem().equals(asInfo.getFileSystem());
    }

    /**
     * Gets the hash code for this object.
     *
     * @return The hash code.
     */
    public int hashCode() {
        return getPath().hashCode() ^ getFileSystem().hashCode();
    }
}
