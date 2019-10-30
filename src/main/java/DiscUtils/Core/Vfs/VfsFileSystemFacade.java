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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import DiscUtils.Core.DiscDirectoryInfo;
import DiscUtils.Core.DiscFileInfo;
import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.DiscFileSystemInfo;
import DiscUtils.Core.DiscFileSystemOptions;
import DiscUtils.Streams.SparseStream;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;


/**
 * Base class for the public facade on a file system.
 *
 * The derived class can extend the functionality available from a file system
 * beyond that defined by DiscFileSystem.
 */
public abstract class VfsFileSystemFacade extends DiscFileSystem {
    private final DiscFileSystem _wrapped;

    /**
     * Initializes a new instance of the VfsFileSystemFacade class.
     *
     * @param toWrap The actual file system instance.
     */
    protected VfsFileSystemFacade(DiscFileSystem toWrap) {
        _wrapped = toWrap;
    }

    /**
     * Indicates whether the file system is read-only or read-write.
     *
     * @return true if the file system is read-write.
     */
    public boolean canWrite() {
        return _wrapped.canWrite();
    }

    /**
     * Gets a friendly name for the file system.
     */
    public String getFriendlyName() {
        return _wrapped.getFriendlyName();
    }

    /**
     * Gets a value indicating whether the file system is thread-safe.
     */
    public boolean isThreadSafe() {
        return _wrapped.isThreadSafe();
    }

    /**
     * Gets the file system options, which can be modified.
     */
    public DiscFileSystemOptions getOptions() {
        return _wrapped.getOptions();
    }

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
        return _wrapped.getVolumeLabel();
    }

    /**
     * Copies an existing file to a new file.
     *
     * @param sourceFile The source file.
     * @param destinationFile The destination file.
     */
    public void copyFile(String sourceFile, String destinationFile) throws IOException {
        _wrapped.copyFile(sourceFile, destinationFile);
    }

    /**
     * Copies an existing file to a new file.
     *
     * @param sourceFile The source file.
     * @param destinationFile The destination file.
     * @param overwrite Overwrite any existing file.
     */
    public void copyFile(String sourceFile, String destinationFile, boolean overwrite) throws IOException {
        _wrapped.copyFile(sourceFile, destinationFile, overwrite);
    }

    /**
     * Creates a directory.
     *
     * @param path The path of the new directory.
     */
    public void createDirectory(String path) throws IOException {
        _wrapped.createDirectory(path);
    }

    /**
     * Deletes a directory.
     *
     * @param path The path of the directory to delete.
     */
    public void deleteDirectory(String path) throws IOException {
        _wrapped.deleteDirectory(path);
    }

    /**
     * Deletes a directory, optionally with all descendants.
     *
     * @param path The path of the directory to delete.
     * @param recursive Determines if the all descendants should be deleted.
     */
    public void deleteDirectory(String path, boolean recursive) throws IOException {
        _wrapped.deleteDirectory(path, recursive);
    }

    /**
     * Deletes a file.
     *
     * @param path The path of the file to delete.
     */
    public void deleteFile(String path) throws IOException {
        _wrapped.deleteFile(path);
    }

    /**
     * Indicates if a directory exists.
     *
     * @param path The path to test.
     * @return true if the directory exists.
     */
    public boolean directoryExists(String path) throws IOException {
        return _wrapped.directoryExists(path);
    }

    /**
     * Indicates if a file exists.
     *
     * @param path The path to test.
     * @return true if the file exists.
     */
    public boolean fileExists(String path) throws IOException {
        return _wrapped.fileExists(path);
    }

    /**
     * Indicates if a file or directory exists.
     *
     * @param path The path to test.
     * @return true if the file or directory exists.
     */
    public boolean exists(String path) throws IOException {
        return _wrapped.exists(path);
    }

    /**
     * Gets the names of subdirectories in a specified directory.
     *
     * @param path The path to search.
     * @return Array of directories.
     */
    public List<String> getDirectories(String path) throws IOException {
        return _wrapped.getDirectories(path);
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
        return _wrapped.getDirectories(path, searchPattern);
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
    public List<String> getDirectories(String path, String searchPattern, String searchOption) throws IOException {
        return _wrapped.getDirectories(path, searchPattern, searchOption);
    }

    /**
     * Gets the names of files in a specified directory.
     *
     * @param path The path to search.
     * @return Array of files.
     */
    public List<String> getFiles(String path) throws IOException {
        return _wrapped.getFiles(path);
    }

    /**
     * Gets the names of files in a specified directory.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return Array of files matching the search pattern.
     */
    public List<String> getFiles(String path, String searchPattern) throws IOException {
        return _wrapped.getFiles(path, searchPattern);
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
    public List<String> getFiles(String path, String searchPattern, String searchOption) throws IOException {
        return _wrapped.getFiles(path, searchPattern, searchOption);
    }

    /**
     * Gets the names of all files and subdirectories in a specified directory.
     *
     * @param path The path to search.
     * @return Array of files and subdirectories matching the search pattern.
     */
    public List<String> getFileSystemEntries(String path) throws IOException {
        return _wrapped.getFileSystemEntries(path);
    }

    /**
     * Gets the names of files and subdirectories in a specified directory
     * matching a specified search pattern.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return Array of files and subdirectories matching the search pattern.
     */
    public List<String> getFileSystemEntries(String path, String searchPattern) throws IOException {
        return _wrapped.getFileSystemEntries(path, searchPattern);
    }

    /**
     * Moves a directory.
     *
     * @param sourceDirectoryName The directory to move.
     * @param destinationDirectoryName The target directory name.
     */
    public void moveDirectory(String sourceDirectoryName, String destinationDirectoryName) throws IOException {
        _wrapped.moveDirectory(sourceDirectoryName, destinationDirectoryName);
    }

    /**
     * Moves a file.
     *
     * @param sourceName The file to move.
     * @param destinationName The target file name.
     */
    public void moveFile(String sourceName, String destinationName) throws IOException {
        _wrapped.moveFile(sourceName, destinationName);
    }

    /**
     * Moves a file, allowing an existing file to be overwritten.
     *
     * @param sourceName The file to move.
     * @param destinationName The target file name.
     * @param overwrite Whether to permit a destination file to be overwritten.
     */
    public void moveFile(String sourceName, String destinationName, boolean overwrite) throws IOException {
        _wrapped.moveFile(sourceName, destinationName, overwrite);
    }

    /**
     * Opens the specified file.
     *
     * @param path The full path of the file to open.
     * @param mode The file mode for the created stream.
     * @return The new stream.
     */
    public SparseStream openFile(String path, FileMode mode) throws IOException {
        return _wrapped.openFile(path, mode);
    }

    /**
     * Opens the specified file.
     *
     * @param path The full path of the file to open.
     * @param mode The file mode for the created stream.
     * @param access The access permissions for the created stream.
     * @return The new stream.
     */
    public SparseStream openFile(String path, FileMode mode, FileAccess access) throws IOException {
        return _wrapped.openFile(path, mode, access);
    }

    /**
     * Gets the attributes of a file or directory.
     *
     * @param path The file or directory to inspect.
     * @return The attributes of the file or directory.
     */
    public Map<String, Object> getAttributes(String path) throws IOException {
        return _wrapped.getAttributes(path);
    }

    /**
     * Sets the attributes of a file or directory.
     *
     * @param path The file or directory to change.
     * @param newValue The new attributes of the file or directory.
     */
    public void setAttributes(String path, Map<String, Object> newValue) throws IOException {
        _wrapped.setAttributes(path, newValue);
    }

    /**
     * Gets the creation time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The creation time.
     */
    public long getCreationTime(String path) throws IOException {
        return _wrapped.getCreationTime(path);
    }

    /**
     * Sets the creation time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setCreationTime(String path, long newTime) throws IOException {
        _wrapped.setCreationTime(path, newTime);
    }

    /**
     * Gets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The creation time.
     */
    public long getCreationTimeUtc(String path) throws IOException {
        return _wrapped.getCreationTimeUtc(path);
    }

    /**
     * Sets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setCreationTimeUtc(String path, long newTime) throws IOException {
        _wrapped.setCreationTimeUtc(path, newTime);
    }

    /**
     * Gets the last access time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last access time.
     */
    public long getLastAccessTime(String path) throws IOException {
        return _wrapped.getLastAccessTime(path);
    }

    /**
     * Sets the last access time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setLastAccessTime(String path, long newTime) throws IOException {
        _wrapped.setLastAccessTime(path, newTime);
    }

    /**
     * Gets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last access time.
     */
    public long getLastAccessTimeUtc(String path) throws IOException {
        return _wrapped.getLastAccessTimeUtc(path);
    }

    /**
     * Sets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setLastAccessTimeUtc(String path, long newTime) throws IOException {
        _wrapped.setLastAccessTimeUtc(path, newTime);
    }

    /**
     * Gets the last modification time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last write time.
     */
    public long getLastWriteTime(String path) throws IOException {
        return _wrapped.getLastWriteTime(path);
    }

    /**
     * Sets the last modification time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setLastWriteTime(String path, long newTime) throws IOException {
        _wrapped.setLastWriteTime(path, newTime);
    }

    /**
     * Gets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last write time.
     */
    public long getLastWriteTimeUtc(String path) throws IOException {
        return _wrapped.getLastWriteTimeUtc(path);
    }

    /**
     * Sets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setLastWriteTimeUtc(String path, long newTime) throws IOException {
        _wrapped.setLastWriteTimeUtc(path, newTime);
    }

    /**
     * Gets the length of a file.
     *
     * @param path The path to the file.
     * @return The length in bytes.
     */
    public long getFileLength(String path) throws IOException {
        return _wrapped.getFileLength(path);
    }

    /**
     * Gets an object representing a possible file.
     *
     * @param path The file path.
     * @return The representing object.The file does not need to exist.
     */
    public DiscFileInfo getFileInfo(String path) {
        return new DiscFileInfo(this, path);
    }

    /**
     * Gets an object representing a possible directory.
     *
     * @param path The directory path.
     * @return The representing object.The directory does not need to exist.
     */
    public DiscDirectoryInfo getDirectoryInfo(String path) {
        return new DiscDirectoryInfo(this, path);
    }

    /**
     * Gets an object representing a possible file system object (file or
     * directory).
     *
     * @param path The file system path.
     * @return The representing object.The file system object does not need to
     *         exist.
     */
    public DiscFileSystemInfo getFileSystemInfo(String path) {
        return new DiscFileSystemInfo(this, path);
    }

    /**
     * Size of the Filesystem in bytes
     */
    public long getSize() throws IOException {
        return _wrapped.getSize();
    }

    /**
     * Used space of the Filesystem in bytes
     */
    public long getUsedSpace() throws IOException {
        return _wrapped.getUsedSpace();
    }

    /**
     * Available space of the Filesystem in bytes
     */
    public long getAvailableSpace() throws IOException {
        return _wrapped.getAvailableSpace();
    }

    /**
     * Provides access to the actual file system implementation. The concrete
     * type representing directory entries.The concrete type representing
     * files.The concrete type representing directories.The concrete type
     * holding global state.
     * 
     * @return The actual file system instance.
     */
    protected <TDirEntry extends VfsDirEntry, TFile extends IVfsFile, TDirectory extends IVfsDirectory<TDirEntry, TFile> & IVfsFile, TContext extends VfsContext> VfsFileSystem<TDirEntry, TFile, TDirectory, TContext> getRealFileSystem() {
        return VfsFileSystem.class.cast(_wrapped);
    }
}
