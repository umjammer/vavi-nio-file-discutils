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

package discUtils.core.vfs;

import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import discUtils.core.DiscFileSystem;
import discUtils.core.DiscFileSystemOptions;
import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.internal.ObjectCache;
import discUtils.core.internal.Utilities;
import discUtils.streams.SparseStream;
import discUtils.streams.buffer.BufferStream;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.util.compat.StringUtilities;

import static java.lang.System.getLogger;


/**
 * base class for VFS file systems. The concrete type representing directory
 * entries.
 *
 * @param <TDirEntry> The concrete type representing files.
 * @param <TFile> The concrete type representing
 * @param <TDirectory> directories.
 * @param <TContext> The concrete type holding global state.
 */
public abstract class VfsFileSystem<TDirEntry extends VfsDirEntry, TFile extends IVfsFile, TDirectory extends IVfsDirectory<TDirEntry, TFile> & IVfsFile, TContext extends VfsContext> extends DiscFileSystem {

    private static final Logger logger = getLogger(VfsFileSystem.class.getName());

    private static final String FS = File.separator;
    private static final char FSC = File.separatorChar;

    private final ObjectCache<Long, TFile> fileCache;

    /**
     * Initializes a new instance of the VfsFileSystem class.
     *
     * @param defaultOptions The default file system options.
     */
    protected VfsFileSystem(DiscFileSystemOptions defaultOptions) {
        super(defaultOptions);
        fileCache = new ObjectCache<>();
    }

    /**
     * Gets or sets the global shared state.
     */
    private TContext context;

    protected TContext getContext() {
        return context;
    }

    protected void setContext(TContext value) {
        context = value;
    }

    /**
     * Gets or sets the object representing the root directory.
     */
    private TDirectory rootDirectory;

    protected TDirectory getRootDirectory() {
        return rootDirectory;
    }

    protected void setRootDirectory(TDirectory value) {
        rootDirectory = value;
    }

    /**
     * Gets the volume label.
     */
    @Override
    public abstract String getVolumeLabel();

    /**
     * Copies a file - not supported on read-only file systems.
     *
     * @param sourceFile The source file.
     * @param destinationFile The destination file.
     * @param overwrite Whether to permit over-writing of an existing file.
     */
    @Override
    public void copyFile(String sourceFile, String destinationFile, boolean overwrite) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a directory - not supported on read-only file systems.
     *
     * @param path The path of the new directory.
     */
    @Override
    public void createDirectory(String path) {
        throw new UnsupportedOperationException();
    }

    /**
     * Deletes a directory - not supported on read-only file systems.
     *
     * @param path The path of the directory to delete.
     */
    @Override
    public void deleteDirectory(String path) {
        throw new UnsupportedOperationException();
    }

    /**
     * Deletes a file - not supported on read-only file systems.
     *
     * @param path The path of the file to delete.
     */
    @Override
    public void deleteFile(String path) {
        throw new UnsupportedOperationException();
    }

    /**
     * Indicates if a directory exists.
     *
     * @param path The path to test.
     * @return true if the directory exists.
     */
    @Override
    public boolean directoryExists(String path) {
        if (isRoot(path)) {
            return true;
        }

        TDirEntry dirEntry = getDirectoryEntry(path);
        if (dirEntry != null) {
            return dirEntry.isDirectory();
        }

        return false;
    }

    /**
     * Indicates if a file exists.
     *
     * @param path The path to test.
     * @return true if the file exists.
     */
    @Override
    public boolean fileExists(String path) {
        TDirEntry dirEntry = getDirectoryEntry(path);
        if (dirEntry != null) {
            return !dirEntry.isDirectory();
        }

        return false;
    }

    /**
     * Gets the names of subdirectories in a specified directory matching a
     * specified search pattern, using a value to determine whether to search
     * subdirectories.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @param searchOption Indicates whether to search subdirectories.
     * @return list of directories matching the search pattern.
     */
    @Override
    public List<String> getDirectories(String path, String searchPattern, String searchOption) {
        Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);
        List<String> dirs = new ArrayList<>();
        doSearch(dirs, path, re, "AllDirectories".equalsIgnoreCase(searchOption), true, false);
        return dirs;
    }

    /**
     * Gets the names of files in a specified directory matching a specified
     * search pattern, using a value to determine whether to search
     * subdirectories.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @param searchOption Indicates whether to search subdirectories.
     * @return list of files matching the search pattern.
     */
    @Override
    public List<String> getFiles(String path, String searchPattern, String searchOption) {
        Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);
        List<String> results = new ArrayList<>();
        doSearch(results, path, re, "AllDirectories".equalsIgnoreCase(searchOption), false, true);
        return results;
    }

    /**
     * Gets the names of all files and subdirectories in a specified directory.
     *
     * @param path The path to search.
     * @return list of files and subdirectories matching the search pattern.
     */
    @Override
    public List<String> getFileSystemEntries(String path) {
        String fullPath = path;
        if (!fullPath.startsWith(FS)) {
            fullPath = FS + fullPath;
        }

        String _fullPath = fullPath;
        TDirectory parentDir = getDirectory(fullPath);
        return parentDir.getAllEntries()
                .stream()
                .map(m -> Utilities.combinePaths(_fullPath, formatFileName(m.getFileName())))
                .collect(Collectors.toList());
    }

    /**
     * Gets the names of files and subdirectories in a specified directory
     * matching a specified search pattern.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return list of files and subdirectories matching the search pattern.
     */
    @Override
    public List<String> getFileSystemEntries(String path, String searchPattern) {
        Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);

        TDirectory parentDir = getDirectory(path);

        List<String> result = new ArrayList<>();
        for (TDirEntry dirEntry : parentDir.getAllEntries()) {
            if (re.matcher(dirEntry.getSearchName()).find()) {
                result.add(Utilities.combinePaths(path, dirEntry.getFileName()));
            }
        }
        return result;
    }

    /**
     * Moves a directory.
     *
     * @param sourceDirectoryName The directory to move.
     * @param destinationDirectoryName The target directory name.
     */
    @Override
    public void moveDirectory(String sourceDirectoryName, String destinationDirectoryName) {
        throw new UnsupportedOperationException();
    }

    /**
     * Moves a file.
     *
     * @param sourceName The file to move.
     * @param destinationName The target file name.
     * @param overwrite Overwrite any existing file.
     */
    @Override
    public void moveFile(String sourceName, String destinationName, boolean overwrite) {
        throw new UnsupportedOperationException();
    }

    /**
     * Opens the specified file.
     *
     * @param path The full path of the file to open.
     * @param mode The file mode for the created stream.
     * @param access The access permissions for the created stream.
     * @return The new stream.
     */
    @Override
    public SparseStream openFile(String path, FileMode mode, FileAccess access) {
        if (!canWrite()) {
            if (mode != FileMode.Open) {
                throw new UnsupportedOperationException("Only existing files can be opened");
            }

            if (access != FileAccess.Read) {
                throw new UnsupportedOperationException("Files cannot be opened for write");
            }
        }

        String fileName = Utilities.getFileFromPath(path);
        String attributeName = null;

        int streamSepPos = fileName.indexOf(':');
        if (streamSepPos >= 0) {
            attributeName = fileName.substring(streamSepPos + 1);
        }

        String dirName;
        try {
            dirName = Utilities.getDirectoryFromPath(path);
        } catch (IllegalArgumentException e) {
            throw new dotnet4j.io.IOException("Invalid path: " + path, e);
        }

        String entryPath = Utilities.combinePaths(dirName, fileName);
        TDirEntry entry = getDirectoryEntry(entryPath);
        if (entry == null) {
            if (mode == FileMode.Open) {
                throw new dotnet4j.io.FileNotFoundException("No such file: " + path);
            }
            TDirectory parentDir = getDirectory(Utilities.getDirectoryFromPath(path));
            entry = parentDir.createNewFile(Utilities.getFileFromPath(path));
        } else if (mode == FileMode.CreateNew) {
            throw new dotnet4j.io.IOException("File already exists");
        }

        if (entry.isSymlink()) {
            entry = resolveSymlink(entry, entryPath);
        }

        if (entry.isDirectory()) {
            throw new dotnet4j.io.IOException("Attempt to open directory as a file");
        }
        TFile file = getFile(entry);

        SparseStream stream;
        if (attributeName == null || attributeName.isEmpty()) {
            stream = new BufferStream(file.getFileContent(), access);
        } else {
            if (file instanceof IVfsFileWithStreams fileStreams) {
                stream = fileStreams.openExistingStream(attributeName);
                if (stream == null) {
                    if (mode == FileMode.Create || mode == FileMode.OpenOrCreate) {
                        stream = fileStreams.createStream(attributeName);
                    } else {
                        throw new dotnet4j.io.FileNotFoundException("No such attribute on file: " + path);
                    }
                }
            } else {
                throw new UnsupportedOperationException("Attempt to open a file stream on a file system that doesn't support them");
            }
        }

        if (mode == FileMode.Create || mode == FileMode.Truncate) {
            stream.setLength(0);
        }

        return stream;
    }

    /**
     * Gets the attributes of a file or directory.
     *
     * @param path The file or directory to inspect.
     * @return The attributes of the file or directory.
     */
    @Override
    public Map<String, Object> getAttributes(String path) {
        if (isRoot(path)) {
            return FileAttributes.toMap(getRootDirectory().getFileAttributes());
        }

        TDirEntry dirEntry = getDirectoryEntry(path);
        if (dirEntry == null) {
            throw new dotnet4j.io.FileNotFoundException("File not found: " + path);
        }

        if (dirEntry.hasVfsFileAttributes()) {
            return FileAttributes.toMap(dirEntry.getFileAttributes());
        }
        return FileAttributes.toMap(getFile(dirEntry).getFileAttributes());
    }

    /**
     * Sets the attributes of a file or directory.
     *
     * @param path The file or directory to change.
     * @param newValue The new attributes of the file or directory.
     */
    @Override
    public void setAttributes(String path, Map<String, Object> newValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The creation time.
     */
    @Override
    public long getCreationTimeUtc(String path) {
        if (isRoot(path)) {
            return getRootDirectory().getCreationTimeUtc();
        }

        TDirEntry dirEntry = getDirectoryEntry(path);
        if (dirEntry == null) {
            // TODO check file status
            throw new dotnet4j.io.FileNotFoundException("No such file or directory: " + path);
        }

        if (dirEntry.hasVfsTimeInfo()) {
            return dirEntry.getCreationTimeUtc();
        }
        return getFile(dirEntry).getCreationTimeUtc();
    }

    /**
     * Sets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override
    public void setCreationTimeUtc(String path, long newTime) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last access time.
     */
    @Override
    public long getLastAccessTimeUtc(String path) {
        if (isRoot(path)) {
            return getRootDirectory().getLastAccessTimeUtc();
        }

        TDirEntry dirEntry = getDirectoryEntry(path);
        if (dirEntry == null) {
            throw new dotnet4j.io.FileNotFoundException("No such file or directory: " + path);
        }

        if (dirEntry.hasVfsTimeInfo()) {
            return dirEntry.getLastAccessTimeUtc();
        }
        return getFile(dirEntry).getLastAccessTimeUtc();
    }

    /**
     * Sets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override
    public void setLastAccessTimeUtc(String path, long newTime) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last write time.
     */
    @Override
    public long getLastWriteTimeUtc(String path) {
        if (isRoot(path)) {
            return getRootDirectory().getLastWriteTimeUtc();
        }

        TDirEntry dirEntry = getDirectoryEntry(path);
        if (dirEntry == null) {
            throw new dotnet4j.io.FileNotFoundException("No such file or directory: " + path);
        }

        if (dirEntry.hasVfsTimeInfo()) {
            return dirEntry.getLastWriteTimeUtc();
        }
        return getFile(dirEntry).getLastWriteTimeUtc();
    }

    /**
     * Sets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override
    public void setLastWriteTimeUtc(String path, long newTime) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the length of a file.
     *
     * @param path The path to the file.
     * @return The length in bytes.
     */
    @Override
    public long getFileLength(String path) {
        TFile file = getFile(path);
        if (file == null || file.getFileAttributes().contains(FileAttributes.Directory)) {
            throw new dotnet4j.io.FileNotFoundException("No such file: " + path);
        }

        return file.getFileLength();
    }

    public TFile getFile(TDirEntry dirEntry) {
        long cacheKey = dirEntry.getUniqueCacheId();
        TFile file = fileCache.get(cacheKey);
        if (file == null) {
            file = convertDirEntryToFile(dirEntry);
            fileCache.put(cacheKey, file);
        }
//logger.log(Level.DEBUG, file);
        return file;
    }

    @SuppressWarnings({"unchecked"})
    public TDirectory getDirectory(String path) {
        if (isRoot(path)) {
            return getRootDirectory();
        }

        TDirEntry dirEntry = getDirectoryEntry(path);

        if (dirEntry != null && dirEntry.isSymlink()) {
            dirEntry = resolveSymlink(dirEntry, path);
        }

        if (dirEntry == null || !dirEntry.isDirectory()) {
            throw new dotnet4j.io.FileNotFoundException("No such directory: " + path);
        }

        return (TDirectory) getFile(dirEntry);
    }

    public TDirEntry getDirectoryEntry(String path) {
        return getDirectoryEntry(getRootDirectory(), path);
    }

    /**
     * Gets all directory entries in the specified directory and
     * sub-directories.
     *
     * @param path The path to inspect.
     * @param handler Delegate invoked for each directory entry.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void forAllDirEntries(String path, DirEntryHandler handler) {
        TDirectory dir = null;
        TDirEntry self = getDirectoryEntry(path);

        if (self != null) {
            handler.invoke(path, self);
            if (self.isDirectory()) {
                dir = (TDirectory) getFile(self);
            }
        } else {
            dir = (TDirectory) getFile(path);
        }

        if (dir != null) {
            for (TDirEntry subentry : dir.getAllEntries()) {
                forAllDirEntries(Utilities.combinePaths(path, subentry.getFileName()), handler);
            }
        }
    }

    /**
     * Gets the file object for a given path.
     *
     * @param path The path to query.
     * @return The file object corresponding to the path.
     */
    @SuppressWarnings({"unchecked"})
    protected TFile getFile(String path) {
        if (isRoot(path)) {
            return (TFile) getRootDirectory();
        }
        if (path == null) {
            return null;
        }

        TDirEntry dirEntry = getDirectoryEntry(path);
        if (dirEntry == null) {
            throw new dotnet4j.io.FileNotFoundException("No such file or directory: " + path);
        }

        return getFile(dirEntry);
    }

    /**
     * Converts a directory entry to an object representing a file.
     *
     * @param dirEntry The directory entry to convert.
     * @return The corresponding file object.
     */
    protected abstract TFile convertDirEntryToFile(TDirEntry dirEntry);

    /**
     * Converts an internal directory entry name into an external one.
     *
     * @param name The name to convert.
     * @return The external name. This method is called on a single path element
     *         (i.e. name contains no path separators).
     */
    protected String formatFileName(String name) {
        return name;
    }

    private static boolean isRoot(String path) {
        return path == null || path.isEmpty() || FS.equals(path);
    }

    private TDirEntry getDirectoryEntry(TDirectory dir, String path) {
        String[] pathElements = Arrays.stream(path.split(StringUtilities.escapeForRegex(FS)))
                .filter(e -> !e.isEmpty())
                .toArray(String[]::new);
        return getDirectoryEntry(dir, pathElements, 0);
    }

    @SuppressWarnings("unchecked")
    private TDirEntry getDirectoryEntry(TDirectory dir, String[] pathEntries, int pathOffset) {
        TDirEntry entry;

        if (pathEntries.length == 0) {
            return dir.getSelf();
        }

        entry = dir.getEntryByName(pathEntries[pathOffset]);
        if (entry != null) {
            if (pathOffset == pathEntries.length - 1) {
                return entry;
            }
            if (entry.isDirectory()) {
                return getDirectoryEntry((TDirectory) convertDirEntryToFile(entry), pathEntries, pathOffset + 1);
            }
            throw new dotnet4j.io.IOException(String.format("%s is a file, not a directory", pathEntries[pathOffset]));
        }

        return null;
    }

    private void doSearch(List<String> results, String path, Pattern regex, boolean subFolders, boolean dirs, boolean files) {
        TDirectory parentDir = getDirectory(path);
        if (parentDir == null) {
            throw new dotnet4j.io.FileNotFoundException(String.format("The directory '%s' was not found", path));
        }

        String resultPrefixPath = path;
        if (isRoot(path)) {
            resultPrefixPath = FS;
        }

        for (TDirEntry de : parentDir.getAllEntries()) {
            TDirEntry entry = de;
            if (entry.isSymlink()) {
                entry = resolveSymlink(entry, path + FSC + entry.getFileName());
            }

            boolean isDir = entry.isDirectory();

            if ((isDir && dirs) || (!isDir && files)) {
                if (regex.matcher(de.getSearchName()).find()) {
                    results.add(Utilities.combinePaths(resultPrefixPath, formatFileName(entry.getFileName())));
                }
            }

            if (subFolders && isDir) {
                doSearch(results,
                         Utilities.combinePaths(resultPrefixPath, formatFileName(entry.getFileName())),
                         regex,
                         subFolders,
                         dirs,
                         files);
            }
        }
    }

    private TDirEntry resolveSymlink(TDirEntry entry, String path) {
        TDirEntry currentEntry = entry;
        if (!path.isEmpty() && path.charAt(0) != FSC) {
            path = FSC + path;
        }
        String currentPath = path;
        int resolvesLeft = 20;
        while (currentEntry.isSymlink() && resolvesLeft > 0) {
            TFile file = getFile(currentEntry);
            if (!(file instanceof IVfsSymlink)) {
                throw new dotnet4j.io.FileNotFoundException("Unable to resolve symlink: " + path);
            }
            @SuppressWarnings("unchecked")
            IVfsSymlink<TDirEntry, TFile> symlink = (IVfsSymlink<TDirEntry, TFile>) file;

            currentPath = Utilities.resolvePath(currentPath.replaceFirst(StringUtilities.escapeForRegex(FS + "*$"), ""),
                                                symlink.getTargetPath());
            currentEntry = getDirectoryEntry(currentPath);
            if (currentEntry == null) {
logger.log(Level.DEBUG, currentPath);
                throw new dotnet4j.io.FileNotFoundException("Unable to resolve symlink: " + path);
            }

            --resolvesLeft;
        }

        if (currentEntry.isSymlink()) {
            throw new dotnet4j.io.FileNotFoundException("Unable to resolve symlink - too many links: " + path);
        }

        return currentEntry;
    }

    /**
     * Delegate for processing directory entries.
     */
    @FunctionalInterface
    protected interface DirEntryHandler<TDirEntry extends VfsDirEntry> {

        /**
         * @param path Full path to the directory entry.
         * @param dirEntry The directory entry itself.
         */
        void invoke(String path, TDirEntry dirEntry);
    }
}
