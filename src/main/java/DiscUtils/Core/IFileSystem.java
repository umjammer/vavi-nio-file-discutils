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
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import DiscUtils.Streams.SparseStream;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileMode;


/**
 * Common interface for all file systems.
 */
public interface IFileSystem {
    /**
     * Gets a value indicating whether the file system is read-only or
     * read-write.
     * 
     * @return true if the file system is read-write.
     */
    boolean canWrite();

    /**
     * Gets a value indicating whether the file system is thread-safe.
     */
    boolean isThreadSafe();

    /**
     * Gets the root directory of the file system.
     */
    DiscDirectoryInfo getRoot();

    /**
     * Copies an existing file to a new file.
     * 
     * @param sourceFile The source file.
     * @param destinationFile The destination file.
     */
    void copyFile(String sourceFile, String destinationFile) throws IOException;

    /**
     * Copies an existing file to a new file, allowing overwriting of an
     * existing file.
     * 
     * @param sourceFile The source file.
     * @param destinationFile The destination file.
     * @param overwrite Whether to permit over-writing of an existing file.
     */
    void copyFile(String sourceFile, String destinationFile, boolean overwrite) throws IOException;

    /**
     * Creates a directory.
     * 
     * @param path The path of the new directory.
     */
    void createDirectory(String path) throws IOException;

    /**
     * Deletes a directory.
     * 
     * @param path The path of the directory to delete.
     */
    void deleteDirectory(String path) throws IOException;

    /**
     * Deletes a directory, optionally with all descendants.
     * 
     * @param path The path of the directory to delete.
     * @param recursive Determines if the all descendants should be deleted.
     */
    void deleteDirectory(String path, boolean recursive) throws IOException;

    /**
     * Deletes a file.
     * 
     * @param path The path of the file to delete.
     */
    void deleteFile(String path) throws IOException;

    /**
     * Indicates if a directory exists.
     * 
     * @param path The path to test.
     * @return true if the directory exists.
     */
    boolean directoryExists(String path) throws IOException;

    /**
     * Indicates if a file exists.
     * 
     * @param path The path to test.
     * @return true if the file exists.
     */
    boolean fileExists(String path) throws IOException;

    /**
     * Indicates if a file or directory exists.
     * 
     * @param path The path to test.
     * @return true if the file or directory exists.
     */
    boolean exists(String path) throws IOException;

    /**
     * Gets the names of subdirectories in a specified directory.
     * 
     * @param path The path to search.
     * @return Array of directories.
     */
    List<String> getDirectories(String path) throws IOException;

    /**
     * Gets the names of subdirectories in a specified directory matching a
     * specified
     * search pattern.
     * 
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return Array of directories matching the search pattern.
     */
    List<String> getDirectories(String path, String searchPattern) throws IOException;

    /**
     * Gets the names of subdirectories in a specified directory matching a
     * specified
     * search pattern, using a value to determine whether to search
     * subdirectories.
     * 
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @param searchOption Indicates whether to search subdirectories.
     * @return Array of directories matching the search pattern.
     */
    List<String> getDirectories(String path, String searchPattern, String searchOption) throws IOException;

    /**
     * Gets the names of files in a specified directory.
     * 
     * @param path The path to search.
     * @return Array of files.
     */
    List<String> getFiles(String path) throws IOException;

    /**
     * Gets the names of files in a specified directory.
     * 
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return Array of files matching the search pattern.
     */
    List<String> getFiles(String path, String searchPattern) throws IOException;

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
    List<String> getFiles(String path, String searchPattern, String searchOption) throws IOException;

    /**
     * Gets the names of all files and subdirectories in a specified directory.
     * 
     * @param path The path to search.
     * @return Array of files and subdirectories matching the search pattern.
     */
    List<String> getFileSystemEntries(String path) throws IOException;

    /**
     * Gets the names of files and subdirectories in a specified directory
     * matching a specified
     * search pattern.
     * 
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return Array of files and subdirectories matching the search pattern.
     */
    List<String> getFileSystemEntries(String path, String searchPattern) throws IOException;

    /**
     * Moves a directory.
     * 
     * @param sourceDirectoryName The directory to move.
     * @param destinationDirectoryName The target directory name.
     */
    void moveDirectory(String sourceDirectoryName, String destinationDirectoryName) throws IOException;

    /**
     * Moves a file.
     * 
     * @param sourceName The file to move.
     * @param destinationName The target file name.
     */
    void moveFile(String sourceName, String destinationName) throws IOException;

    /**
     * Moves a file, allowing an existing file to be overwritten.
     * 
     * @param sourceName The file to move.
     * @param destinationName The target file name.
     * @param overwrite Whether to permit a destination file to be overwritten.
     */
    void moveFile(String sourceName, String destinationName, boolean overwrite) throws IOException;

    /**
     * Opens the specified file.
     * 
     * @param path The full path of the file to open.
     * @param mode The file mode for the created stream.
     * @return The new stream.
     */
    SparseStream openFile(String path, FileMode mode) throws IOException;

    /**
     * Opens the specified file.
     * 
     * @param path The full path of the file to open.
     * @param mode The file mode for the created stream.
     * @param access The access permissions for the created stream.
     * @return The new stream.
     */
    SparseStream openFile(String path, FileMode mode, FileAccess access) throws IOException;

    /**
     * Gets the attributes of a file or directory.
     * 
     * @param path The file or directory to inspect.
     * @return The attributes of the file or directory.
     */
    BasicFileAttributes getAttributes(String path) throws IOException;

    /**
     * Sets the attributes of a file or directory.
     * 
     * @param path The file or directory to change.
     * @param newValue The new attributes of the file or directory.
     */
    void setAttributes(String path, BasicFileAttributes newValue) throws IOException;

    /**
     * Gets the creation time (in local time) of a file or directory.
     * 
     * @param path The path of the file or directory.
     * @return The creation time.
     */
    long getCreationTime(String path) throws IOException;

    /**
     * Sets the creation time (in local time) of a file or directory.
     * 
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    void setCreationTime(String path, long newTime) throws IOException;

    /**
     * Gets the creation time (in UTC) of a file or directory.
     * 
     * @param path The path of the file or directory.
     * @return The creation time.
     */
    long getCreationTimeUtc(String path) throws IOException;

    /**
     * Sets the creation time (in UTC) of a file or directory.
     * 
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    void setCreationTimeUtc(String path, long newTime) throws IOException;

    /**
     * Gets the last access time (in local time) of a file or directory.
     * 
     * @param path The path of the file or directory.
     * @return The last access time.
     */
    long getLastAccessTime(String path) throws IOException;

    /**
     * Sets the last access time (in local time) of a file or directory.
     * 
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    void setLastAccessTime(String path, long newTime) throws IOException;

    /**
     * Gets the last access time (in UTC) of a file or directory.
     * 
     * @param path The path of the file or directory.
     * @return The last access time.
     */
    long getLastAccessTimeUtc(String path) throws IOException;

    /**
     * Sets the last access time (in UTC) of a file or directory.
     * 
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    void setLastAccessTimeUtc(String path, long newTime) throws IOException;

    /**
     * Gets the last modification time (in local time) of a file or directory.
     * 
     * @param path The path of the file or directory.
     * @return The last write time.
     */
    long getLastWriteTime(String path) throws IOException;

    /**
     * Sets the last modification time (in local time) of a file or directory.
     * 
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    void setLastWriteTime(String path, long newTime) throws IOException;

    /**
     * Gets the last modification time (in UTC) of a file or directory.
     * 
     * @param path The path of the file or directory.
     * @return The last write time.
     */
    long getLastWriteTimeUtc(String path) throws IOException;

    /**
     * Sets the last modification time (in UTC) of a file or directory.
     * 
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    void setLastWriteTimeUtc(String path, long newTime) throws IOException;

    /**
     * Gets the length of a file.
     * 
     * @param path The path to the file.
     * @return The length in bytes.
     */
    long getFileLength(String path) throws IOException;

    /**
     * Gets an object representing a possible file.
     * 
     * @param path The file path.
     * @return The representing object.The file does not need to exist.
     */
    DiscFileInfo getFileInfo(String path) throws IOException;

    /**
     * Gets an object representing a possible directory.
     * 
     * @param path The directory path.
     * @return The representing object.The directory does not need to exist.
     */
    DiscDirectoryInfo getDirectoryInfo(String path) throws IOException;

    /**
     * Gets an object representing a possible file system object (file or
     * directory).
     * 
     * @param path The file system path.
     * @return The representing object.The file system object does not need to
     *         exist.
     */
    DiscFileSystemInfo getFileSystemInfo(String path) throws IOException;

    /**
     * Reads the boot code of the file system into a byte array.
     * 
     * @return The boot code, or
     *         {@code null}
     *         if not available.
     */
    byte[] readBootCode() throws IOException;

    /**
     * Size of the Filesystem in bytes
     */
    long getSize() throws IOException;

    /**
     * Used space of the Filesystem in bytes
     */
    long getUsedSpace() throws IOException;

    /**
     * Available space of the Filesystem in bytes
     */
    long getAvailableSpace() throws IOException;

}
