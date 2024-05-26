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
import java.io.IOException;
import java.util.EnumSet;

import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.internal.Utilities;
import dotnet4j.util.compat.StringUtilities;


/**
 * Provides the base class for both {@link DiscFileInfo} and
 * {@link DiscDirectoryInfo} objects.
 */
public class DiscFileSystemInfo {

    private static final String FS = File.separator;

    public DiscFileSystemInfo(DiscFileSystem fileSystem, String path) {
        if (path == null) {
            throw new NullPointerException("path");
        }

        this.fileSystem = fileSystem;
        this.path = path.replaceAll(StringUtilities.escapeForRegex("(^" + FS + "*|" + FS + "*$)"), "");
//logger.log(Level.DEBUG, _path);
    }

    /**
     * Gets or sets the {@link FileAttributes} of the current
     * {@link DiscFileSystemInfo} object.
     */
    public EnumSet<FileAttributes> getAttributes() {
        try {
            return FileAttributes.toEnumSet(fileSystem.getAttributes(path));
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public void setAttributes(EnumSet<FileAttributes> value) {
        try {
            fileSystem.setAttributes(path, FileAttributes.toMap(value));
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
            return fileSystem.getCreationTimeUtc(path);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public void setCreationTimeUtc(long value) {
        try {
            fileSystem.setCreationTimeUtc(path, value);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets a value indicating whether the file system object exists.
     */
    public boolean exists() {
        try {
            return fileSystem.exists(path);
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
    protected DiscFileSystem fileSystem;

    public DiscFileSystem getFileSystem() {
        return fileSystem;
    }

    /**
     * Gets the full path of the file or directory.
     */
    public String getFullName() {
        return path;
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
            return fileSystem.getLastAccessTimeUtc(path);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public void setLastAccessTimeUtc(long value) {
        try {
            fileSystem.setLastAccessTimeUtc(path, value);
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
            return fileSystem.getLastWriteTimeUtc(path);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public void setLastWriteTimeUtc(long value) {
        try {
            fileSystem.setLastWriteTimeUtc(path, value);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets the name of the file or directory.
     */
    public String getName() {
        return Utilities.getFileFromPath(path);
    }

    /**
     * Gets the {@link DiscDirectoryInfo} of the directory containing the current
     * {@link #DiscFileSystemInfo} object.
     */
    public DiscDirectoryInfo getParent() {
        if (path == null || path.isEmpty()) {
            return null;
        }

        return new DiscDirectoryInfo(fileSystem, Utilities.getDirectoryFromPath(path));
    }

    /**
     * Gets the path to the referenced file.
     */
    protected String path;

    /**
     * Deletes a file or directory.
     */
    public void delete() {
        try {
            if (getAttributes().contains(FileAttributes.Directory)) {
                fileSystem.deleteDirectory(path);
            } else {
                fileSystem.deleteFile(path);
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
        if (!(obj instanceof DiscFileSystemInfo)) {
            return false;
        }
        DiscFileSystemInfo asInfo = (DiscFileSystemInfo) obj;
        return path.equals(asInfo.path) && fileSystem.equals(asInfo.fileSystem);
    }

    /**
     * Gets the hash code for this object.
     *
     * @return The hash code.
     */
    public int hashCode() {
        return path.hashCode() ^ fileSystem.hashCode();
    }
}
