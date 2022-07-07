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

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import discUtils.streams.SparseStream;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;


/**
 * Provides the base class for all file systems.
 */
public abstract class DiscFileSystem implements Serializable, IFileSystem, Closeable {

    /**
     * Initializes a new instance of the DiscFileSystem class.
     */
    protected DiscFileSystem() {
        _options = new DiscFileSystemOptions();
    }

    /**
     * Initializes a new instance of the DiscFileSystem class.
     *
     * @param defaultOptions The options instance to use for this file system
     *            instance.
     */
    protected DiscFileSystem(DiscFileSystemOptions defaultOptions) {
        _options = defaultOptions;
    }

    /**
     * Finalizes an instance of the DiscFileSystem class.
     */
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    /**
     * Gets the file system options, which can be modified.
     */
    private DiscFileSystemOptions _options;

    public DiscFileSystemOptions getOptions() {
        return _options;
    }

    /**
     * Gets a friendly description of the file system type.
     */
    public abstract String getFriendlyName();

    /**
     * Gets a value indicating whether the file system is read-only or
     * read-write.
     *
     * @return true if the file system is read-write.
     */
    public abstract boolean canWrite();

    /**
     * Gets the root directory of the file system.
     */
    public DiscDirectoryInfo getRoot() {
        return new DiscDirectoryInfo(this, "");
    }

    /**
     * Gets the volume label.
     */
    public String getVolumeLabel() {
        return "";
    }

    /**
     * Gets a value indicating whether the file system is thread-safe.
     */
    public boolean isThreadSafe() {
        return false;
    }

    /**
     * Copies an existing file to a new file.
     *
     * @param sourceFile The source file.
     * @param destinationFile The destination file.
     */
    public void copyFile(String sourceFile, String destinationFile) throws IOException {
        copyFile(sourceFile, destinationFile, false);
    }

    /**
     * Copies an existing file to a new file, allowing overwriting of an
     * existing file.
     *
     * @param sourceFile The source file.
     * @param destinationFile The destination file.
     * @param overwrite Whether to permit over-writing of an existing file.
     */
    public abstract void copyFile(String sourceFile, String destinationFile, boolean overwrite) throws IOException;

    /**
     * Creates a directory.
     *
     * @param path The path of the new directory.
     */
    public abstract void createDirectory(String path) throws IOException;

    /**
     * Deletes a directory.
     *
     * @param path The path of the directory to delete.
     */
    public abstract void deleteDirectory(String path) throws IOException;

    /**
     * Deletes a directory, optionally with all descendants.
     *
     * @param path The path of the directory to delete.
     * @param recursive Determines if the all descendants should be deleted.
     */
    public void deleteDirectory(String path, boolean recursive) throws IOException {
        if (recursive) {
            for (String dir : getDirectories(path)) {
                deleteDirectory(dir, true);
            }
            for (String file : getFiles(path)) {
                deleteFile(file);
            }
        }

        deleteDirectory(path);
    }

    /**
     * Deletes a file.
     *
     * @param path The path of the file to delete.
     */
    public abstract void deleteFile(String path) throws IOException;

    /**
     * Indicates if a directory exists.
     *
     * @param path The path to test.
     * @return true if the directory exists.
     */
    public abstract boolean directoryExists(String path) throws IOException;

    /**
     * Indicates if a file exists.
     *
     * @param path The path to test.
     * @return true if the file exists.
     */
    public abstract boolean fileExists(String path) throws IOException;

    /**
     * Indicates if a file or directory exists.
     *
     * @param path The path to test.
     * @return true if the file or directory exists.
     */
    public boolean exists(String path) throws IOException {
        return fileExists(path) || directoryExists(path);
    }

    /**
     * Gets the names of subdirectories in a specified directory.
     *
     * @param path The path to search.
     * @return Array of directories.
     */
    public List<String> getDirectories(String path) throws IOException {
        return getDirectories(path, "*.*", TOP_DIRECTORY_ONLY);
    }

    /**
     * Gets the names of subdirectories in a specified directory matching a
     * specified search pattern.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return Array of directories matching the search pattern.
     */
    public List<String> getDirectories(String path, String searchPattern) throws IOException {
        return getDirectories(path, searchPattern, TOP_DIRECTORY_ONLY);
    }

    /**
     * Gets the names of subdirectories in a specified directory matching a
     * specified search pattern, using a value to determine whether to search
     * subdirectories.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @param searchOption Indicates whether to search subdirectories.
     * @return Array of directories matching the search pattern.
     */
    public abstract List<String> getDirectories(String path, String searchPattern, String searchOption) throws IOException;

    /**
     * Gets the names of files in a specified directory.
     *
     * @param path The path to search.
     * @return Array of files.
     */
    public List<String> getFiles(String path) throws IOException {
        return getFiles(path, "*.*", TOP_DIRECTORY_ONLY);
    }

    /**
     * Gets the names of files in a specified directory.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return Array of files matching the search pattern.
     */
    public List<String> getFiles(String path, String searchPattern) throws IOException {
        return getFiles(path, searchPattern, TOP_DIRECTORY_ONLY);
    }

    /**
     * Gets the names of files in a specified directory matching a specified
     * search pattern, using a value to determine whether to search
     * subdirectories.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @param searchOption Indicates whether to search subdirectories.
     * @return Array of files matching the search pattern.
     */
    public abstract List<String> getFiles(String path, String searchPattern, String searchOption) throws IOException;

    /**
     * Gets the names of all files and subdirectories in a specified directory.
     *
     * @param path The path to search.
     * @return Array of files and subdirectories matching the search pattern.
     */
    public abstract List<String> getFileSystemEntries(String path) throws IOException;

    /**
     * Gets the names of files and subdirectories in a specified directory
     * matching a specified search pattern.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return Array of files and subdirectories matching the search pattern.
     */
    public abstract List<String> getFileSystemEntries(String path, String searchPattern) throws IOException;

    /**
     * Moves a directory.
     *
     * @param sourceDirectoryName The directory to move.
     * @param destinationDirectoryName The target directory name.
     */
    public abstract void moveDirectory(String sourceDirectoryName, String destinationDirectoryName) throws IOException;

    /**
     * Moves a file.
     *
     * @param sourceName The file to move.
     * @param destinationName The target file name.
     */
    public void moveFile(String sourceName, String destinationName) throws IOException {
        moveFile(sourceName, destinationName, false);
    }

    /**
     * Moves a file, allowing an existing file to be overwritten.
     *
     * @param sourceName The file to move.
     * @param destinationName The target file name.
     * @param overwrite Whether to permit a destination file to be overwritten.
     */
    public abstract void moveFile(String sourceName, String destinationName, boolean overwrite) throws IOException;

    /**
     * Opens the specified file.
     *
     * @param path The full path of the file to open.
     * @param mode The file mode for the created stream.
     * @return The new stream.
     */
    public SparseStream openFile(String path, FileMode mode) throws IOException {
        return openFile(path, mode, FileAccess.ReadWrite);
    }

    /**
     * Opens the specified file.
     *
     * @param path The full path of the file to open.
     * @param mode The file mode for the created stream.
     * @param access The access permissions for the created stream.
     * @return The new stream.
     */
    public abstract SparseStream openFile(String path, FileMode mode, FileAccess access) throws IOException;

    /**
     * Gets the attributes of a file or directory.
     *
     * @param path The file or directory to inspect.
     * @return The attributes of the file or directory.
     */
    public abstract Map<String, Object> getAttributes(String path) throws IOException;

    /**
     * Sets the attributes of a file or directory.
     *
     * @param path The file or directory to change.
     * @param newValue The new attributes of the file or directory.
     */
    public abstract void setAttributes(String path, Map<String, Object> newValue) throws IOException;

    /**
     * Gets the creation time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The creation time.
     */
    public long getCreationTime(String path) throws IOException {
        return getCreationTimeUtc(path);
    }

    /**
     * Sets the creation time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setCreationTime(String path, long newTime) throws IOException {
        setCreationTimeUtc(path, newTime);
    }

    /**
     * Gets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The creation time.
     */
    public abstract long getCreationTimeUtc(String path) throws IOException;

    /**
     * Sets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public abstract void setCreationTimeUtc(String path, long newTime) throws IOException;

    /**
     * Gets the last access time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last access time.
     */
    public long getLastAccessTime(String path) throws IOException {
        return getLastAccessTimeUtc(path);
    }

    /**
     * Sets the last access time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setLastAccessTime(String path, long newTime) throws IOException {
        setLastAccessTimeUtc(path, newTime);
    }

    /**
     * Gets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last access time.
     */
    public abstract long getLastAccessTimeUtc(String path) throws IOException;

    /**
     * Sets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public abstract void setLastAccessTimeUtc(String path, long newTime) throws IOException;

    /**
     * Gets the last modification time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last write time.
     */
    public long getLastWriteTime(String path) throws IOException {
        return getLastWriteTimeUtc(path);
    }

    /**
     * Sets the last modification time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setLastWriteTime(String path, long newTime) throws IOException {
        setLastWriteTimeUtc(path, newTime);
    }

    /**
     * Gets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last write time.
     */
    public abstract long getLastWriteTimeUtc(String path) throws IOException;

    /**
     * Sets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public abstract void setLastWriteTimeUtc(String path, long newTime) throws IOException;

    /**
     * Gets the length of a file.
     *
     * @param path The path to the file.
     * @return The length in bytes.
     */
    public abstract long getFileLength(String path) throws IOException;

    /**
     * Gets an object representing a possible file.
     *
     * The file does not need to exist.
     * 
     * @param path The file path.
     * @return The representing object.
     */
    public DiscFileInfo getFileInfo(String path) throws IOException {
        return new DiscFileInfo(this, path);
    }

    /**
     * Gets an object representing a possible directory.
     *
     * The directory does not need to exist.
     * 
     * @param path The directory path.
     * @return The representing object.
     */
    public DiscDirectoryInfo getDirectoryInfo(String path) throws IOException {
        return new DiscDirectoryInfo(this, path);
    }

    /**
     * Gets an object representing a possible file system object (file or
     * directory).
     *
     * The file system object does not need to exist.
     * 
     * @param path The file system path.
     * @return The representing object.
     */
    public DiscFileSystemInfo getFileSystemInfo(String path) throws IOException {
        return new DiscFileSystemInfo(this, path);
    }

    /**
     * Reads the boot code of the file system into a byte array.
     *
     * @return The boot code, or {@code null} if not available.
     */
    public byte[] readBootCode() throws IOException {
        return null;
    }

    /**
     * Size of the Filesystem in bytes
     */
    public abstract long getSize() throws IOException;

    /**
     * Used space of the Filesystem in bytes
     */
    public abstract long getUsedSpace() throws IOException;

    /**
     * Available space of the Filesystem in bytes
     */
    public abstract long getAvailableSpace() throws IOException;

    /**
     * Disposes of this instance, releasing all resources.
     */
    public void close() throws IOException {
    }
}
