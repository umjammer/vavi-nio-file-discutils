//
// DiscUtils Copyright (c) 2008-2011, Kenneth Bell
//
// Original NativeFileSystem contributed by bsobel:
//    http://discutils.codeplex.com/workitem/5190
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
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import vavi.util.Debug;

import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.internal.LocalFileLocator;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;


/**
 * Provides an implementation for OS-mounted file systems.
 */
public class NativeFileSystem extends DiscFileSystem {

    private static final String FS = File.separator;

    private final boolean readOnly;

    /**
     * Initializes a new instance of the NativeFileSystem class.
     *
     * @param basePath The 'root' directory of the new instance.
     * @param readOnly Only permit 'read' activities.
     */
    public NativeFileSystem(String basePath, boolean readOnly) {
        this.basePath = basePath;
        if (!this.basePath.endsWith(FS)) {
            this.basePath += FS;
        }

        this.readOnly = readOnly;
    }

    /**
     * Gets the base path used to create the file system.
     */
    private String basePath;

    public String getBasePath() {
        return basePath;
    }

    /**
     * Indicates whether the file system is read-only or read-write.
     *
     * @return true if the file system is read-write.
     */
    public boolean canWrite() {
        return !readOnly;
    }

    /**
     * Provides a friendly description of the file system type.
     */
    public String getFriendlyName() {
        return "Native";
    }

    /**
     * Gets a value indicating whether the file system is thread-safe. The
     * Native File System is thread safe.
     */
    public boolean isThreadSafe() {
        return true;
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
        return "";
    }

    /**
     * Copies an existing file to a new file.
     *
     * @param sourceFile The source file.
     * @param destinationFile The destination file.
     * @throws IOException
     */
    public void copyFile(String sourceFile, String destinationFile) throws IOException {
        copyFile(sourceFile, destinationFile, true);
    }

    /**
     * Copies an existing file to a new file, allowing overwriting of an
     * existing file.
     *
     * @param sourceFile The source file.
     * @param destinationFile The destination file.
     * @param overwrite Whether to permit over-writing of an existing file.
     * @throws IOException
     */
    public void copyFile(String sourceFile, String destinationFile, boolean overwrite) throws IOException {
        if (readOnly) {
            throw new IOException("read only");
        }

        if (sourceFile.startsWith(FS)) {
            sourceFile = sourceFile.substring(1);
        }

        if (destinationFile.startsWith(FS)) {
            destinationFile = destinationFile.substring(1);
        }

        Files.copy(Paths.get(getBasePath(), sourceFile), Paths.get(getBasePath(), destinationFile));
    }

    /**
     * Creates a directory.
     *
     * @param path The path of the new directory.
     * @throws IOException
     */
    public void createDirectory(String path) throws IOException {
        if (readOnly) {
            throw new IOException("read only");
        }

        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        Files.createDirectory(Paths.get(getBasePath(), path));
    }

    /**
     * Deletes a directory.
     *
     * @param path The path of the directory to delete.
     * @throws IOException
     */
    public void deleteDirectory(String path) throws IOException {
        if (readOnly) {
            throw new IOException("read only");
        }

        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        Files.delete(Paths.get(getBasePath(), path));
    }

    /**
     * Deletes a directory, optionally with all descendants.
     *
     * @param path The path of the directory to delete.
     * @param recursive Determines if the all descendants should be deleted.
     * @throws IOException
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
     * @throws IOException
     */
    public void deleteFile(String path) throws IOException {
        if (readOnly) {
            throw new IOException("read only");
        }

        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        Files.delete(Paths.get(getBasePath(), path));
    }

    /**
     * Indicates if a directory exists.
     *
     * @param path The path to test.
     * @return true if the directory exists.
     */
    public boolean directoryExists(String path) {
        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        return Files.exists(Paths.get(getBasePath(), path));
    }

    /**
     * Indicates if a file exists.
     *
     * @param path The path to test.
     * @return true if the file exists.
     */
    public boolean fileExists(String path) {
        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        return Files.exists(Paths.get(getBasePath(), path));
    }

    /**
     * Indicates if a file or directory exists.
     *
     * @param path The path to test.
     * @return true if the file or directory exists.
     */
    public boolean exists(String path) {
        return fileExists(path) || directoryExists(path);
    }

    /**
     * Gets the names of subdirectories in a specified directory.
     *
     * @param path The path to search.
     * @return Array of directories.
     */
    public List<String> getDirectories(String path) {
        return getDirectories(path, "*.*", "TopDirectoryOnly");
    }

    /**
     * Gets the names of subdirectories in a specified directory matching a
     * specified search pattern.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return Array of directories matching the search pattern.
     */
    public List<String> getDirectories(String path, String searchPattern) {
        return getDirectories(path, searchPattern, "TopDirectoryOnly");
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
    public List<String> getDirectories(String path, String searchPattern, String searchOption) {
        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        try {
            return cleanItems(Files.list(Paths
                    .get(getBasePath(), path)/* , searchPattern, searchOption */) // TODO impl
                    .map(Path::toString)
                    .collect(Collectors.toList()));
        } catch (IOException e) {
Debug.printStackTrace(e);
            return Collections.emptyList();
        }
    }

    /**
     * Gets the names of files in a specified directory.
     *
     * @param path The path to search.
     * @return Array of files.
     */
    public List<String> getFiles(String path) {
        return getFiles(path, "*.*", "TopDirectoryOnly");
    }

    /**
     * Gets the names of files in a specified directory.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return Array of files matching the search pattern.
     */
    public List<String> getFiles(String path, String searchPattern) {
        return getFiles(path, searchPattern, "TopDirectoryOnly");
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
    public List<String> getFiles(String path, String searchPattern, String searchOption) {
        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        try {
            return cleanItems(Files.list(Paths
                    .get(getBasePath(), path)/* , searchPattern, searchOption */) // TODO impl
                    .map(Path::toString)
                    .collect(Collectors.toList()));
        } catch (IOException e) {
Debug.printStackTrace(e);
            return Collections.emptyList();
        }
    }

    /**
     * Gets the names of all files and subdirectories in a specified directory.
     *
     * @param path The path to search.
     * @return Array of files and subdirectories matching the search pattern.
     */
    public List<String> getFileSystemEntries(String path) {
        return getFileSystemEntries(path, "*.*");
    }

    /**
     * Gets the names of files and subdirectories in a specified directory
     * matching a specified search pattern.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return Array of files and subdirectories matching the search pattern.
     */
    public List<String> getFileSystemEntries(String path, String searchPattern) {
        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        try {
            return cleanItems(Files
                    .list(Paths.get(getBasePath(), path)/* , searchPattern */) // TODO impl
                    .map(Path::toString)
                    .collect(Collectors.toList()));
        } catch (IOException e) {
Debug.printStackTrace(e);
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Moves a directory.
     *
     * @param sourceDirectoryName The directory to move.
     * @param destinationDirectoryName The target directory name.
     * @throws IOException
     */
    public void moveDirectory(String sourceDirectoryName, String destinationDirectoryName) throws IOException {
        if (readOnly) {
            throw new IOException("read only");
        }

        if (sourceDirectoryName.startsWith(FS)) {
            sourceDirectoryName = sourceDirectoryName.substring(1);
        }

        if (destinationDirectoryName.startsWith(FS)) {
            destinationDirectoryName = destinationDirectoryName.substring(1);
        }

        Files.move(Paths.get(getBasePath(), sourceDirectoryName), Paths.get(getBasePath(), destinationDirectoryName));
    }

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
     * @throws IOException
     */
    public void moveFile(String sourceName, String destinationName, boolean overwrite) throws IOException {
        if (readOnly) {
            throw new IOException("read only");
        }

        if (destinationName.startsWith(FS)) {
            destinationName = destinationName.substring(1);
        }

        if (Files.exists(Paths.get(getBasePath(), destinationName))) {
            if (overwrite) {
                Files.delete(Paths.get(getBasePath(), destinationName));
            } else {
                throw new IOException("File already exists");
            }
        }

        if (sourceName.startsWith(FS)) {
            sourceName = sourceName.substring(1);
        }

        Files.move(Paths.get(getBasePath(), sourceName), Paths.get(getBasePath(), destinationName));
    }

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
     * @throws IOException
     */
    public SparseStream openFile(String path, FileMode mode, FileAccess access) throws IOException {
        if (readOnly && access != FileAccess.Read) {
            throw new IOException("read only");
        }

        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        FileShare fileShare = FileShare.None;
        if (access == FileAccess.Read) {
            fileShare = FileShare.Read;
        }

        LocalFileLocator locator = new LocalFileLocator(getBasePath());
        return SparseStream.fromStream(locator.open(path, mode, access, fileShare), Ownership.Dispose);
    }

    /**
     * Gets the attributes of a file or directory.
     *
     * @param path The file or directory to inspect.
     * @return The attributes of the file or directory.
     */
    public Map<String, Object> getAttributes(String path) throws IOException {
        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        Map<String, Object> result = new HashMap<>(); // TODO impl
        for (FileAttributes key : FileAttributes.values()) {
            result.put(key.name(), Files.getAttribute(Paths.get(getBasePath(), path), key.name()));
        }
        return result;
    }

    /**
     * Sets the attributes of a file or directory.
     *
     * @param path The file or directory to change.
     * @param newValue The new attributes of the file or directory.
     */
    public void setAttributes(String path, Map<String, Object> newValue) throws IOException {
        if (readOnly) {
            throw new IOException("read only");
        }

        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        for (Map.Entry<String, Object> e : newValue.entrySet()) {
            Files.setAttribute(Paths.get(getBasePath(), path), e.getKey(), e.getValue());
        }
    }

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
     * @throws IOException
     */
    public long getCreationTimeUtc(String path) throws IOException {
        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        return Files.getLastModifiedTime(Paths.get(getBasePath(), path)).toMillis();
    }

    /**
     * Sets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setCreationTimeUtc(String path, long newTime) throws IOException {
        if (readOnly) {
            throw new IOException("read only");
        }

        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        Files.setLastModifiedTime(Paths.get(getBasePath(), path), FileTime.fromMillis(newTime));
    }

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
    public long getLastAccessTimeUtc(String path) throws IOException {
        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        return Files.getLastModifiedTime(Paths.get(getBasePath(), path)).toMillis();
    }

    /**
     * Sets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setLastAccessTimeUtc(String path, long newTime) throws IOException {
        if (readOnly) {
            throw new IOException("read only");
        }

        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        Files.setLastModifiedTime(Paths.get(getBasePath(), path), FileTime.fromMillis(newTime));
    }

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
    public long getLastWriteTimeUtc(String path) throws IOException {
        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        return Files.getLastModifiedTime(Paths.get(getBasePath(), path)).toMillis();
    }

    /**
     * Sets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setLastWriteTimeUtc(String path, long newTime) throws IOException {
        if (readOnly) {
            throw new IOException("read only");
        }

        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        Files.setLastModifiedTime(Paths.get(getBasePath(), path), FileTime.fromMillis(newTime));
    }

    /**
     * Gets the length of a file.
     *
     * @param path The path to the file.
     * @return The length in bytes.
     */
    public long getFileLength(String path) throws IOException {
        if (path.startsWith(FS)) {
            path = path.substring(1);
        }

        return Files.size(Paths.get(getBasePath(), path));
    }

    /**
     * Gets an object representing a possible file.
     *
     * The file does not need to exist.
     *
     * @param path The file path.
     * @return The representing object.
     */
    public DiscFileInfo getFileInfo(String path) {
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
    public DiscDirectoryInfo getDirectoryInfo(String path) {
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
    public DiscFileSystemInfo getFileSystemInfo(String path) {
        return new DiscFileSystemInfo(this, path);
    }

    /**
     * Size of the Filesystem in bytes
     */
    public long getSize() throws IOException {
        FileStore info = Paths.get(getBasePath()).getFileSystem().getFileStores().iterator().next();
        return info.getTotalSpace();
    }

    /**
     * Used space of the Filesystem in bytes
     */
    public long getUsedSpace() throws IOException {
        return getSize() - getAvailableSpace();
    }

    /**
     * Available space of the Filesystem in bytes
     */
    public long getAvailableSpace() throws IOException {
        FileStore info = Paths.get(getBasePath()).getFileSystem().getFileStores().iterator().next();
        return info.getUsableSpace();
    }

    private List<String> cleanItems(List<String> dirtyItems) {
        List<String> cleanList = new ArrayList<>(dirtyItems.size());
        for (int x = 0; x < dirtyItems.size(); x++) {
            cleanList.set(x, dirtyItems.get(x).substring(getBasePath().length() - 1));
        }
        return cleanList;
    }
}
