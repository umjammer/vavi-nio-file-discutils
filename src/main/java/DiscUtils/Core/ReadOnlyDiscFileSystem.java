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
import java.util.Map;

import DiscUtils.Streams.SparseStream;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;


/**
 * Base class for file systems that are by their nature read-only, causes
 * UnsupportedOperationException to be thrown
 * from all methods that are always invalid.
 */
public abstract class ReadOnlyDiscFileSystem extends DiscFileSystem {
    /**
     * Initializes a new instance of the ReadOnlyDiscFileSystem class.
     */
    protected ReadOnlyDiscFileSystem() {
    }

    /**
     * Initializes a new instance of the ReadOnlyDiscFileSystem class.
     *
     * @param defaultOptions The options instance to use for this file system
     *            instance.
     */
    protected ReadOnlyDiscFileSystem(DiscFileSystemOptions defaultOptions) {
        super(defaultOptions);
    }

    /**
     * Indicates whether the file system is read-only or read-write.
     *
     * @return Always false.
     */
    public boolean canWrite() {
        return false;
    }

    /**
     * Copies a file - not supported on read-only file systems.
     *
     * @param sourceFile The source file.
     * @param destinationFile The destination file.
     * @param overwrite Whether to permit over-writing of an existing file.
     */
    public void copyFile(String sourceFile, String destinationFile, boolean overwrite) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a directory - not supported on read-only file systems.
     *
     * @param path The path of the new directory.
     */
    public void createDirectory(String path) {
        throw new UnsupportedOperationException();
    }

    /**
     * Deletes a directory - not supported on read-only file systems.
     *
     * @param path The path of the directory to delete.
     */
    public void deleteDirectory(String path) {
        throw new UnsupportedOperationException();
    }

    /**
     * Deletes a file - not supported on read-only file systems.
     *
     * @param path The path of the file to delete.
     */
    public void deleteFile(String path) {
        throw new UnsupportedOperationException();
    }

    /**
     * Moves a directory - not supported on read-only file systems.
     *
     * @param sourceDirectoryName The directory to move.
     * @param destinationDirectoryName The target directory name.
     */
    public void moveDirectory(String sourceDirectoryName, String destinationDirectoryName) {
        throw new UnsupportedOperationException();
    }

    /**
     * Moves a file - not supported on read-only file systems.
     *
     * @param sourceName The file to move.
     * @param destinationName The target file name.
     * @param overwrite Whether to allow an existing file to be overwritten.
     */
    public void moveFile(String sourceName, String destinationName, boolean overwrite) {
        throw new UnsupportedOperationException();
    }

    /**
     * Opens the specified file.
     *
     * @param path The full path of the file to open.
     * @param mode The file mode for the created stream.
     * @return The new stream.
     */
    public SparseStream openFile(String path, FileMode mode) throws IOException {
        return openFile(path, mode, FileAccess.Read);
    }

    /**
     * Sets the attributes of a file or directory - not supported on read-only
     * file systems.
     *
     * @param path The file or directory to change.
     * @param newValue The new attributes of the file or directory.
     */
    public void setAttributes(String path, Map<String, Object> newValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the creation time (in UTC) of a file or directory - not supported on
     * read-only file systems.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setCreationTimeUtc(String path, long newTime) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the last access time (in UTC) of a file or directory - not supported
     * on read-only file systems.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setLastAccessTimeUtc(String path, long newTime) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the last modification time (in UTC) of a file or directory - not
     * supported on read-only file systems.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setLastWriteTimeUtc(String path, long newTime) {
        throw new UnsupportedOperationException();
    }

}
