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

package discUtils.fat;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import discUtils.core.DiscFileSystem;
import discUtils.core.FileSystemParameters;
import discUtils.core.FloppyDiskType;
import discUtils.core.Geometry;
import discUtils.core.TimeConverter;
import discUtils.core.VirtualDisk;
import discUtils.core.internal.Utilities;
import discUtils.streams.SparseStream;
import discUtils.streams.SubStream;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileNotFoundException;
import dotnet4j.io.Stream;
import dotnet4j.util.compat.StringUtilities;
import vavi.util.ByteUtil;

import static java.lang.System.getLogger;


/**
 * Class for accessing FAT file systems.
 */
public final class FatFileSystem extends DiscFileSystem {

    private static final Logger logger = getLogger(FatFileSystem.class.getName());

    private static final String FS = File.separator;

    /**
     * The Epoch for FAT file systems (1st Jan, 1980) at system default zone.
     */
    public static final long Epoch = ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();

    private BootSector bs;

    private final Map<Integer, Directory> dirCache;

    private Ownership ownsData;

    private final TimeConverter timeConverter;

    private Stream data;

    private Directory rootDir;

    /**
     * Initializes a new instance of the FatFileSystem class.
     *
     * @param data The stream containing the file system. Local time is the
     *            effective timezone of the new instance.
     */
    public FatFileSystem(Stream data) {
        super(new FatFileSystemOptions());
        dirCache = new HashMap<>();
        timeConverter = this::defaultTimeConverter;
        initialize(data, null);
    }

    /**
     * Initializes a new instance of the FatFileSystem class.
     *
     * @param data The stream containing the file system.
     * @param ownsData Indicates if the new instance should take ownership of
     *            {@code data}. Local time is the effective timezone of the new
     *            instance.
     */
    public FatFileSystem(Stream data, Ownership ownsData) {
        super(new FatFileSystemOptions());
        dirCache = new HashMap<>();
        timeConverter = this::defaultTimeConverter;
        initialize(data, null);
        this.ownsData = ownsData;
    }

    /**
     * Initializes a new instance of the FatFileSystem class.
     *
     * @param data The stream containing the file system.
     * @param timeConverter A delegate to convert to/from the file system's
     *            timezone.
     */
    public FatFileSystem(Stream data, TimeConverter timeConverter) {
        super(new FatFileSystemOptions());
        dirCache = new HashMap<>();
        this.timeConverter = timeConverter;
        initialize(data, null);
    }

    /**
     * Initializes a new instance of the FatFileSystem class.
     *
     * @param data The stream containing the file system.
     * @param ownsData Indicates if the new instance should take ownership of
     *            {@code data}.
     * @param timeConverter A delegate to convert to/from the file system's
     *            timezone.
     */
    public FatFileSystem(Stream data, Ownership ownsData, TimeConverter timeConverter) {
        super(new FatFileSystemOptions());
        dirCache = new HashMap<>();
        this.timeConverter = timeConverter;
        initialize(data, null);
        this.ownsData = ownsData;
    }

    /**
     * Initializes a new instance of the FatFileSystem class.
     *
     * @param data The stream containing the file system.
     * @param ownsData Indicates if the new instance should take ownership of
     *            {@code data}.
     * @param parameters The parameters for the file system.
     */
    public FatFileSystem(Stream data, BootSector bs, Ownership ownsData, FileSystemParameters parameters) {
        super(new FatFileSystemOptions(parameters, bs instanceof ATBootSector));
        dirCache = new HashMap<>();
        if (parameters != null && parameters.getTimeConverter() != null) {
            timeConverter = parameters.getTimeConverter();
        } else {
            timeConverter = this::defaultTimeConverter;
        }
        initialize(data, bs);
        this.ownsData = ownsData;
    }

    /**
     * Gets the number of bytes per sector (as stored in the file-system meta
     * data).
     */
    public int getBytesPerSector() {
        return bs.getBytesPerSector();
    }

    /**
     * Indicates if this file system is read-only or read-write.
     */
    @Override public boolean canWrite() {
        return data.canWrite();
    }

    private ClusterReader clusterReader;

    ClusterReader getClusterReader() {
        return clusterReader;
    }

    void setClusterReader(ClusterReader value) {
        clusterReader = value;
    }

    private FileAllocationTable fat;

    FileAllocationTable getFat() {
        return fat;
    }

    void setFat(FileAllocationTable value) {
        fat = value;
    }

    /**
     * Gets the FAT file system options, which can be modified.
     */
    public FatFileSystemOptions getFatOptions() {
        return (FatFileSystemOptions) getOptions();
    }

    /**
     * Gets the FAT variant of the file system.
     */
    public FatType getFatVariant() {
        return bs.getFatVariant();
    }

    /**
     * Gets the friendly name for the file system, including FAT variant.
     */
    @Override public String getFriendlyName() {
       return bs.getFatVariant().getFriendlyName();
    }

    /**
     * Gets the total number of sectors on the disk.
     */
    public long getTotalSectors() {
        return bs.getTotalSectors();
    }

    /**
     * Gets the volume label.
     */
    @Override public String getVolumeLabel() {
        long volId = rootDir.findVolumeId();
        if (volId < 0) {
            return bs.getVolumeLabel();
        }

        return rootDir.getEntry(volId).getName().getRawName(getFatOptions().getFileNameEncoding());
    }

    /**
     * Detects if a stream contains a FAT file system.
     *
     * @param stream The stream to inspect.
     * @return {@code true} if the stream appears to be a FAT file system, else
     *         {@code false}.
     */
    public static boolean detect(Stream stream) {
        if (stream.getLength() < 512) {
logger.log(Level.DEBUG, "stream length < 512");
            return false;
        }

        stream.position(0);
        byte[] bytes = StreamUtilities.readExact(stream, 512);
        short bpbBytesPerSec = ByteUtil.readLeShort(bytes, 11);
        if (bpbBytesPerSec != 512) {
logger.log(Level.DEBUG, "bpb bytes per sec != 512: " + bpbBytesPerSec);
            return false;
        }

        byte bpbNumFATs = bytes[16];
        if (bpbNumFATs == 0 || bpbNumFATs > 2) {
            return false;
        }

        short bpbTotSec16 = ByteUtil.readLeShort(bytes, 19);
        int bpbTotSec32 = ByteUtil.readLeInt(bytes, 32);
        if (!((bpbTotSec16 == 0) ^ (bpbTotSec32 == 0))) {
logger.log(Level.DEBUG, "bpb tot sec == 0");
            return false;
        }

        int totalSectors = bpbTotSec16 + bpbTotSec32;
logger.log(Level.DEBUG, "bpb total length: %s, %d, %d".formatted((totalSectors * (long) bpbBytesPerSec <= stream.getLength()), totalSectors, stream.getLength()));
        return totalSectors * (long) bpbBytesPerSec <= stream.getLength();
    }

    /**
     * Opens a file for reading and/or writing.
     *
     * @param path The full path to the file.
     * @param mode The file mode.
     * @param access The desired access.
     * @return The stream to the opened file.
     */
    @Override public SparseStream openFile(String path, FileMode mode, FileAccess access) {
        Directory[] parent = new Directory[1];
        long entryId;
        try {
            entryId = getDirectoryEntry(rootDir, path, parent);
        } catch (IllegalArgumentException e) {
            throw new dotnet4j.io.IOException("Invalid path: " + path, e);
        }

        if (parent[0] == null) {
            throw new FileNotFoundException("Could not locate file " + path);
        }

        if (entryId < 0) {
            return parent[0].openFile(FileName.fromPath(path, getFatOptions().getFileNameEncoding()), mode, access);
        }

        DirectoryEntry dirEntry = parent[0].getEntry(entryId);

        if (dirEntry.getAttributes().contains(FatAttributes.Directory)) {
            throw new dotnet4j.io.IOException("Attempt to open directory as a file");
        }
        return parent[0].openFile(dirEntry.getName(), mode, access);
    }

    /**
     * Gets the attributes of a file or directory.
     *
     * @param path The file or directory to inspect.
     * @return The attributes of the file or directory.
     */
    @Override public Map<String, Object> getAttributes(String path) {
        // Simulate a root directory entry - doesn't really exist though
        if (isRootPath(path)) {
            return FatAttributes.toMap(EnumSet.of(FatAttributes.Directory));
        }

        DirectoryEntry dirEntry = getDirectoryEntry(path);
        if (dirEntry == null) {
            throw new FileNotFoundException("No such file " + path);
        }

        // Luckily, FAT and .NET Map<String, Object> match, bit-for-bit
        return FatAttributes.toMap(dirEntry.getAttributes());
    }

    /**
     * Sets the attributes of a file or directory.
     *
     * @param path The file or directory to change.
     * @param newValue The new attributes of the file or directory.
     */
    @Override public void setAttributes(String path, Map<String, Object> newValue) {
        if (isRootPath(path)) {
            if (!newValue.containsKey(FatAttributes.Directory.name())) {
                throw new UnsupportedOperationException("The attributes of the root directory cannot be modified");
            }

            return;
        }

        Directory[] parent = new Directory[1];
        long id = getDirectoryEntry(path, parent);
        DirectoryEntry dirEntry = parent[0].getEntry(id);

        EnumSet<FatAttributes> newFatAttr = FatAttributes.toEnumSet(newValue);

        if (newFatAttr.contains(FatAttributes.Directory) != dirEntry.getAttributes().contains(FatAttributes.Directory)) {
            throw new IllegalArgumentException("Attempted to change the directory attribute");
        }

        dirEntry.setAttributes(newFatAttr);
        parent[0].updateEntry(id, dirEntry);

        // For directories, need to update their 'self' entry also
        if (dirEntry.getAttributes().contains(FatAttributes.Directory)) {
            Directory dir = getDirectory(path);
            dirEntry = dir.getSelfEntry();
            dirEntry.setAttributes(newFatAttr);
            dir.setSelfEntry(dirEntry);
        }
    }

    /**
     * Gets the creation time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The creation time.
     */
    @Override public long getCreationTime(String path) {
        if (isRootPath(path)) {
            return Epoch;
        }

        return getDirectoryEntry(path).getCreationTime();
    }

    /**
     * Sets the creation time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override public void setCreationTime(String path, long newTime) {
        if (isRootPath(path)) {
            if (newTime != Epoch) {
                throw new UnsupportedOperationException("The creation time of the root directory cannot be modified");
            }

            return;
        }

        updateDirEntry(path, e -> e.setCreationTime(newTime));
    }

    /**
     * Gets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The creation time.
     */
    @Override public long getCreationTimeUtc(String path) {
        if (isRootPath(path)) {
            return convertToUtc(Epoch);
        }

        return convertToUtc(getDirectoryEntry(path).getCreationTime());
    }

    /**
     * Sets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override public void setCreationTimeUtc(String path, long newTime) {
        if (isRootPath(path)) {
            if (convertFromUtc(newTime) != Epoch) {
                throw new UnsupportedOperationException("The last write time of the root directory cannot be modified");
            }

            return;
        }

        updateDirEntry(path, e -> e.setCreationTime(convertFromUtc(newTime)));
    }

    /**
     * Gets the last access time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The time the file or directory was last accessed.
     */
    @Override public long getLastAccessTime(String path) {
        if (isRootPath(path)) {
            return Epoch;
        }

        return getDirectoryEntry(path).getLastAccessTime();
    }

    /**
     * Sets the last access time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override public void setLastAccessTime(String path, long newTime) {
        if (isRootPath(path)) {
            if (newTime != Epoch) {
                throw new UnsupportedOperationException("The last access time of the root directory cannot be modified");
            }

            return;
        }

        updateDirEntry(path, e -> e.setLastAccessTime(newTime));
    }

    /**
     * Gets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The time the file or directory was last accessed.
     */
    @Override public long getLastAccessTimeUtc(String path) {
        if (isRootPath(path)) {
            return convertToUtc(Epoch);
        }

        return convertToUtc(getDirectoryEntry(path).getLastAccessTime());
    }

    /**
     * Sets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override public void setLastAccessTimeUtc(String path, long newTime) {
        if (isRootPath(path)) {
            if (convertFromUtc(newTime) != Epoch) {
                throw new UnsupportedOperationException("The last write time of the root directory cannot be modified");
            }

            return;
        }

        updateDirEntry(path, e -> e.setLastAccessTime(convertFromUtc(newTime)));
    }

    /**
     * Gets the last modification time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The time the file or directory was last modified.
     */
    @Override public long getLastWriteTime(String path) {
        if (isRootPath(path)) {
            return Epoch;
        }

        return getDirectoryEntry(path).getLastWriteTime();
    }

    /**
     * Sets the last modification time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override public void setLastWriteTime(String path, long newTime) {
        if (isRootPath(path)) {
            if (newTime != Epoch) {
                throw new UnsupportedOperationException("The last write time of the root directory cannot be modified");
            }

            return;
        }

        updateDirEntry(path, e -> e.setLastWriteTime(newTime));
    }

    /**
     * Gets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The time the file or directory was last modified.
     */
    @Override public long getLastWriteTimeUtc(String path) {
        if (isRootPath(path)) {
            return convertToUtc(Epoch);
        }

        return convertToUtc(getDirectoryEntry(path).getLastWriteTime());
    }

    /**
     * Sets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override public void setLastWriteTimeUtc(String path, long newTime) {
        if (isRootPath(path)) {
            if (convertFromUtc(newTime) != Epoch) {
                throw new UnsupportedOperationException("The last write time of the root directory cannot be modified");
            }

            return;
        }

        updateDirEntry(path, e -> e.setLastWriteTime(convertFromUtc(newTime)));
    }

    /**
     * Gets the length of a file.
     *
     * @param path The path to the file.
     * @return The length in bytes.
     */
    @Override public long getFileLength(String path) {
        return getDirectoryEntry(path).getFileSize();
    }

    /**
     * Copies an existing file to a new file, allowing overwriting of an
     * existing file.
     *
     * @param sourceFile The source file.
     * @param destinationFile The destination file.
     * @param overwrite Whether to permit over-writing of an existing file.
     */
    @Override public void copyFile(String sourceFile, String destinationFile, boolean overwrite) {
        Directory[] sourceDir = new Directory[1];
        long sourceEntryId = getDirectoryEntry(sourceFile, sourceDir);

        if (sourceDir[0] == null || sourceEntryId < 0) {
            throw new FileNotFoundException("The source file '%s' was not found".formatted(sourceFile));
        }

        DirectoryEntry sourceEntry = sourceDir[0].getEntry(sourceEntryId);

        if (sourceEntry.getAttributes().contains(FatAttributes.Directory)) {
            throw new dotnet4j.io.IOException("The source file is a directory");
        }

        DirectoryEntry newEntry = new DirectoryEntry(sourceEntry);
        newEntry.setName(FileName.fromPath(destinationFile, getFatOptions().getFileNameEncoding()));
        newEntry.setFirstCluster(0);

        Directory[] destDir = new Directory[1];
        long destEntryId = getDirectoryEntry(destinationFile, destDir);

        if (destDir[0] == null) {
            throw new FileNotFoundException("The destination directory for '%s' was not found".formatted(destinationFile));
        }

        // If the destination is a directory, use the old file name to construct
        // a full path.
        if (destEntryId >= 0) {
            DirectoryEntry destEntry = destDir[0].getEntry(destEntryId);
            if (destEntry.getAttributes().contains(FatAttributes.Directory)) {
                newEntry.setName(FileName.fromPath(sourceFile, getFatOptions().getFileNameEncoding()));
                destinationFile = Utilities.combinePaths(destinationFile, Utilities.getFileFromPath(sourceFile));

                destEntryId = getDirectoryEntry(destinationFile, destDir);
            }
        }

        // If there's an existing entry...
        if (destEntryId >= 0) {
            DirectoryEntry destEntry = destDir[0].getEntry(destEntryId);

            if (destEntry.getAttributes().contains(FatAttributes.Directory)) {
                throw new dotnet4j.io.IOException("Destination file is an existing directory");
            }

            if (!overwrite) {
                throw new dotnet4j.io.IOException("Destination file already exists");
            }

            // Remove the old file
            destDir[0].deleteEntry(destEntryId, true);
        }

        // Add the new file's entry
        destEntryId = destDir[0].addEntry(newEntry);

        // Copy the contents...
        try (Stream sourceStream = new FatFileStream(this, sourceDir[0], sourceEntryId, FileAccess.Read);
             Stream destStream = new FatFileStream(this, destDir[0], destEntryId, FileAccess.Write)) {
            StreamUtilities.pumpStreams(sourceStream, destStream);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Creates a directory.
     *
     * @param path The directory to create.
     */
    @Override public void createDirectory(String path) {
        String[] pathElements = Arrays.stream(path.split(StringUtilities.escapeForRegex(FS)))
                .filter(e -> !e.isEmpty())
                .toArray(String[]::new);
        Directory focusDir = rootDir;
        for (String pathElement : pathElements) {
            FileName name;
            try {
                name = new FileName(pathElement, getFatOptions().getFileNameEncoding());
            } catch (IllegalArgumentException ae) {
                throw new dotnet4j.io.IOException("Invalid path: " + path, ae);
            }

            Directory child = focusDir.getChildDirectory(name);
            if (child == null) {
                child = focusDir.createChildDirectory(name);
            }

            focusDir = child;
        }
    }

    /**
     * Deletes a directory, optionally with all descendants.
     *
     * @param path The path of the directory to delete.
     */
    @Override public void deleteDirectory(String path) {
        Directory dir = getDirectory(path);
        if (dir == null) {
            throw new FileNotFoundException("No such directory: %s".formatted(path));
        }

        if (!dir.isEmpty()) {
            throw new dotnet4j.io.IOException("Unable to delete non-empty directory");
        }

        Directory[] parent = new Directory[1];
        long id = getDirectoryEntry(path, parent);
        if (parent[0] == null && id == 0) {
            throw new dotnet4j.io.IOException("Unable to delete root directory");
        }

        if (parent[0] != null && id >= 0) {
            DirectoryEntry deadEntry = parent[0].getEntry(id);
            parent[0].deleteEntry(id, true);
            forgetDirectory(deadEntry);
        } else {
            throw new FileNotFoundException("No such directory: " + path);
        }
    }

    /**
     * Deletes a file.
     *
     * @param path The path of the file to delete.
     */
    @Override public void deleteFile(String path) {
        Directory[] parent = new Directory[1];
        long id = getDirectoryEntry(path, parent);
        if (parent[0] == null || id < 0) {
            throw new FileNotFoundException("No such file " + path);
        }

        DirectoryEntry entry = parent[0].getEntry(id);
        if (entry == null || entry.getAttributes().contains(FatAttributes.Directory)) {
            throw new FileNotFoundException("No such file " + path);
        }

        parent[0].deleteEntry(id, true);
    }

    /**
     * Indicates if a directory exists.
     *
     * @param path The path to test.
     * @return true if the directory exists.
     */
    @Override public boolean directoryExists(String path) {
        // Special case - root directory
        if (path == null || path.isEmpty()) {
            return true;
        }

        DirectoryEntry dirEntry = getDirectoryEntry(path);
        return dirEntry != null && dirEntry.getAttributes().contains(FatAttributes.Directory);
    }

    /**
     * Indicates if a file exists.
     *
     * @param path The path to test.
     * @return true if the file exists.
     */
    @Override public boolean fileExists(String path) {
        // Special case - root directory
        if (path == null || path.isEmpty()) {
            return true;
        }

        DirectoryEntry dirEntry = getDirectoryEntry(path);
        return dirEntry != null && !dirEntry.getAttributes().contains(FatAttributes.Directory);
    }

    /**
     * Indicates if a file or directory exists.
     *
     * @param path The path to test.
     * @return true if the file or directory exists.
     */
    @Override public boolean exists(String path) {
        // Special case - root directory
        if (path == null || path.isEmpty()) {
            return true;
        }

        return getDirectoryEntry(path) != null;
    }

    /**
     * Gets the names of subdirectories in a specified directory.
     *
     * @param path The path to search.
     * @return list of directories.
     */
    @Override public List<String> getDirectories(String path) {
        Directory dir = getDirectory(path);
        if (dir == null) {
            throw new FileNotFoundException("The directory '%s' was not found".formatted(path));
        }

        List<DirectoryEntry> entries = dir.getDirectories();
        List<String> dirs = new ArrayList<>(entries.size());
        for (DirectoryEntry dirEntry : entries) {
            dirs.add(Utilities.combinePaths(path, dirEntry.getName().getDisplayName(getFatOptions().getFileNameEncoding())));
        }
        return dirs;
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
    @Override public List<String> getDirectories(String path, String searchPattern, String searchOption) {
        Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);
        List<String> dirs = new ArrayList<>();
        doSearch(dirs, path, re, "AllDirectories".equalsIgnoreCase(searchOption), true, false);
        return dirs;
    }

    /**
     * Gets the names of files in a specified directory.
     *
     * @param path The path to search.
     * @return list of files.
     */
    @Override public List<String> getFiles(String path) {
        Directory dir = getDirectory(path);
        List<DirectoryEntry> entries = dir.getFiles();
        List<String> files = new ArrayList<>(entries.size());
        for (DirectoryEntry dirEntry : entries) {
            files.add(Utilities.combinePaths(path, dirEntry.getName().getDisplayName(getFatOptions().getFileNameEncoding())));
        }
        return files;
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
    @Override public List<String> getFiles(String path, String searchPattern, String searchOption) {
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
    @Override public List<String> getFileSystemEntries(String path) {
        Directory dir = getDirectory(path);
        List<DirectoryEntry> entries = dir.getEntries();
        List<String> result = new ArrayList<>(entries.size());
        for (DirectoryEntry dirEntry : entries) {
            result.add(Utilities.combinePaths(path, dirEntry.getName().getDisplayName(getFatOptions().getFileNameEncoding())));
        }
        return result;
    }

    /**
     * Gets the names of files and subdirectories in a specified directory
     * matching a specified search pattern.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return list of files and subdirectories matching the search pattern.
     */
    @Override public List<String> getFileSystemEntries(String path, String searchPattern) {
        Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);
        Directory dir = getDirectory(path);
        List<DirectoryEntry> entries = dir.getEntries();
        List<String> result = new ArrayList<>(entries.size());
        for (DirectoryEntry dirEntry : entries) {
            if (re.matcher(dirEntry.getName().getSearchName(getFatOptions().getFileNameEncoding())).find()) {
                result.add(Utilities.combinePaths(path,
                                                  dirEntry.getName().getDisplayName(getFatOptions().getFileNameEncoding())));
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
    @Override public void moveDirectory(String sourceDirectoryName, String destinationDirectoryName) {
        if (destinationDirectoryName == null || destinationDirectoryName.isEmpty()) {
            if (destinationDirectoryName == null) {
                throw new NullPointerException("destinationDirectoryName");
            }

            throw new IllegalArgumentException("Invalid destination name (empty string)");
        }

        Directory[] destParent = new Directory[1];
        long destId = getDirectoryEntry(destinationDirectoryName, destParent);
        if (destParent[0] == null) {
            throw new FileNotFoundException("Target directory doesn't exist");
        }

        if (destId >= 0) {
            throw new dotnet4j.io.IOException("Target directory already exists");
        }

        Directory[] sourceParent = new Directory[1];
        long sourceId = getDirectoryEntry(sourceDirectoryName, sourceParent);
        if (sourceParent[0] == null || sourceId < 0) {
            throw new dotnet4j.io.IOException("Source directory doesn't exist");
        }

        destParent[0].attachChildDirectory(FileName.fromPath(destinationDirectoryName, getFatOptions().getFileNameEncoding()),
                                           getDirectory(sourceDirectoryName));
        sourceParent[0].deleteEntry(sourceId, false);
    }

    /**
     * Moves a file, allowing an existing file to be overwritten.
     *
     * @param sourceName The file to move.
     * @param destinationName The target file name.
     * @param overwrite Whether to permit a destination file to be overwritten.
     */
    @Override public void moveFile(String sourceName, String destinationName, boolean overwrite) {
        Directory[] sourceDir = new Directory[1];
        long sourceEntryId = getDirectoryEntry(sourceName, sourceDir);

        if (sourceDir[0] == null || sourceEntryId < 0) {
            throw new FileNotFoundException("The source file '%s' was not found".formatted(sourceName));
        }

        DirectoryEntry sourceEntry = sourceDir[0].getEntry(sourceEntryId);

        if (sourceEntry.getAttributes().contains(FatAttributes.Directory)) {
            throw new dotnet4j.io.IOException("The source file is a directory");
        }

        DirectoryEntry newEntry = new DirectoryEntry(sourceEntry);
        newEntry.setName(FileName.fromPath(destinationName, getFatOptions().getFileNameEncoding()));

        Directory[] destDir = new Directory[1];
        long destEntryId = getDirectoryEntry(destinationName, destDir);

        if (destDir[0] == null) {
            throw new FileNotFoundException("The destination directory for '%s' was not found".formatted(destinationName));
        }

        // If the destination is a directory, use the old file name to construct
        // a full
        // path.
        if (destEntryId >= 0) {
            DirectoryEntry destEntry = destDir[0].getEntry(destEntryId);
            if (destEntry.getAttributes().contains(FatAttributes.Directory)) {
                newEntry.setName(FileName.fromPath(sourceName, getFatOptions().getFileNameEncoding()));
                destinationName = Utilities.combinePaths(destinationName, Utilities.getFileFromPath(sourceName));

                destEntryId = getDirectoryEntry(destinationName, destDir);
            }
        }

        // If there's an existing entry...
        if (destEntryId >= 0) {
            DirectoryEntry destEntry = destDir[0].getEntry(destEntryId);

            if (destEntry.getAttributes().contains(FatAttributes.Directory)) {
                throw new dotnet4j.io.IOException("Destination file is an existing directory");
            }

            if (!overwrite) {
                throw new dotnet4j.io.IOException("Destination file already exists");
            }

            // Remove the old file
            destDir[0].deleteEntry(destEntryId, true);
        }

        // Add the new file's entry and remove the old link to the file's
        // contents
        destDir[0].addEntry(newEntry);
        sourceDir[0].deleteEntry(sourceEntryId, false);
    }

    long convertToUtc(long dateTime) {
        return timeConverter.invoke(dateTime, true);
    }

    long convertFromUtc(long dateTime) {
        return timeConverter.invoke(dateTime, false);
    }

    Directory getDirectory(String path) {
        Directory[] parent = new Directory[1];
        if (path == null || path.isEmpty() || path.equals(FS)) {
            return rootDir;
        }

        long id = getDirectoryEntry(rootDir, path, parent);
        if (id >= 0) {
            return getDirectory(parent[0], id);
        }

        return null;
    }

    Directory getDirectory(Directory parent, long parentId) {
        if (parent == null) {
            return rootDir;
        }

        DirectoryEntry dirEntry = parent.getEntry(parentId);
        if (!dirEntry.getAttributes().contains(FatAttributes.Directory)) {
            throw new FileNotFoundException(dirEntry.getName() + " is not a directory");
        }

        // If we have this one cached, return it
        if (dirCache.containsKey(dirEntry.getFirstCluster())) {
            return dirCache.get(dirEntry.getFirstCluster());
        }

        // Not cached - create a new one.
        Directory result = new Directory(parent, parentId);
        dirCache.put(dirEntry.getFirstCluster(), result);
        return result;
    }

    void forgetDirectory(DirectoryEntry entry) {
        int index = entry.getFirstCluster();
        if (index != 0 && dirCache.containsKey(index)) {
            Directory dir = dirCache.get(index);
            dirCache.remove(index);
            try {
                dir.close();
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }
    }

    DirectoryEntry getDirectoryEntry(String path) {
        Directory[] parent = new Directory[1];
        long id = getDirectoryEntry(rootDir, path, parent);
        if (parent[0] == null || id < 0) {
            return null;
        }

        return parent[0].getEntry(id);
    }

    long getDirectoryEntry(String path, Directory[] parent) {
        return getDirectoryEntry(rootDir, path, parent);
    }

    /**
     * Disposes of this instance.
     */
    @Override public void close() throws IOException {
        try {
            for (Directory dir : dirCache.values()) {
                dir.close();
            }
            rootDir.close();
            if (ownsData == Ownership.Dispose) {
                data.close();
                data = null;
            }
        } finally {
            super.close();
        }
    }

    private static boolean isRootPath(String path) {
        return (path == null || path.isEmpty()) || path.equals(FS);
    }

    private long defaultTimeConverter(long time, boolean toUtc) {
        return toUtc ? Instant.ofEpochMilli(time).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
                     : Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void initialize(Stream data, BootSector bs) {
        this.data = data;

        this.bs = bs != null ? bs : new ATBootSector(data);

        loadFAT();
        loadClusterReader();
        loadRootDirectory();
    }

    private void loadClusterReader() {
        int rootDirSectors = (bs.getMaxRootDirectoryEntries() * 32 + (bs.getBytesPerSector() - 1)) / bs.getBytesPerSector();
        int firstDataSector = (int) (bs.getReservedSectorCount() + bs.getFatCount() * bs.getFatSize() + rootDirSectors);
        setClusterReader(new ClusterReader(data, firstDataSector, bs.getSectorsPerCluster(), bs.getBytesPerSector()));
    }

    private void loadRootDirectory() {
        Stream fatStream;
        if (bs.getFatVariant() != FatType.Fat32) {
//logger.log(Level.DEBUG, String.format(Level.FINE, "%016x, %016x\n", (bs.getReservedSectorCount() + (long) bs.getFatCount() * bs.getFatSize16()) * bs.getBytesPerSector(), bs.getMaxRootDirectoryEntries() * 32L));
            fatStream = new SubStream(data,
                    (bs.getReservedSectorCount() + (long) bs.getFatCount() * bs.getFatSize16()) * bs.getBytesPerSector(),
                    bs.getMaxRootDirectoryEntries() * 32L);
        } else {
            fatStream = new ClusterStream(this, FileAccess.ReadWrite, (int) bs.getRootDirectoryCluster(), 0xffff_ffff);
        }
        rootDir = new Directory(this, fatStream);
    }

    private void loadFAT() {
        setFat(new FileAllocationTable(bs.getFatVariant(),
                data,
                bs.getReservedSectorCount(),
                (int) bs.getFatSize(),
                bs.getFatCount(),
                bs.getActiveFat()));
    }

    private long getDirectoryEntry(Directory dir, String path, Directory[] parent) {
        String[] pathElements = Arrays.stream(path.split(StringUtilities.escapeForRegex(FS)))
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        return getDirectoryEntry(dir, pathElements, 0, parent);
    }

    private long getDirectoryEntry(Directory dir, String[] pathEntries, int pathOffset, Directory[] parent) {
        long entryId;
        if (pathEntries.length == 0) {
            // Looking for root directory, simulate the directory entry in its
            // parent...
            parent[0] = null;
            return 0;
        }

        entryId = dir.findEntry(new FileName(pathEntries[pathOffset], getFatOptions().getFileNameEncoding()));
        if (entryId >= 0) {
            if (pathOffset == pathEntries.length - 1) {
                parent[0] = dir;
                return entryId;
            }

            return getDirectoryEntry(getDirectory(dir, entryId), pathEntries, pathOffset + 1, parent);
        }

        if (pathOffset == pathEntries.length - 1) {
            parent[0] = dir;
            return -1;
        }

        parent[0] = null;
        return -1;
    }

    private void doSearch(List<String> results, String path, Pattern regex, boolean subFolders, boolean dirs, boolean files) {
        Directory dir = getDirectory(path);
        if (dir == null) {
            throw new FileNotFoundException("The directory '%s' was not found".formatted(path));
        }

        List<DirectoryEntry> entries = dir.getEntries();

        for (DirectoryEntry de : entries) {
            boolean isDir = de.getAttributes().contains(FatAttributes.Directory);

            if ((isDir && dirs) || (!isDir && files)) {
//logger.log(Level.DEBUG, regex+ ", " + de.getName().getSearchName(getFatOptions().getFileNameEncoding()) + ", " + regex.matcher(de.getName().getSearchName(getFatOptions().getFileNameEncoding())).find());
                if (regex.matcher(de.getName().getSearchName(getFatOptions().getFileNameEncoding())).find()) {
                    results.add(Utilities.combinePaths(path,
                                                       de.getName().getDisplayName(getFatOptions().getFileNameEncoding())));
                }
            }

            if (subFolders && isDir) {
                doSearch(results,
                         Utilities.combinePaths(path, de.getName().getDisplayName(getFatOptions().getFileNameEncoding())),
                         regex,
                         subFolders,
                         dirs,
                         files);
            }
        }
    }

    private void updateDirEntry(String path, EntryUpdateAction action) {
        Directory[] parent = new Directory[1];
        long id = getDirectoryEntry(path, parent);
        DirectoryEntry entry = parent[0].getEntry(id);
        action.invoke(entry);
        parent[0].updateEntry(id, entry);

        if (entry.getAttributes().contains(FatAttributes.Directory)) {
            Directory dir = getDirectory(path);
            DirectoryEntry selfEntry = dir.getSelfEntry();
            action.invoke(selfEntry);
            dir.setSelfEntry(selfEntry);
        }
    }

    /**
     * Size of the Filesystem in bytes
     */
    @Override public long getSize() {
        return ((getTotalSectors() - bs.getReservedSectorCount() - (bs.getFatSize() * bs.getFatCount())) * getBytesPerSector());
    }

    /**
     * Used space of the Filesystem in bytes
     */
    @Override
    public long getUsedSpace() {
        int usedCluster = 0;
        for (int i = 2; i < getFat().getNumEntries(); i++) {
            int fatValue = getFat().getNext(i);
            if (!getFat().isFree(fatValue)) {
                usedCluster++;
            }
        }
        return ((long) usedCluster * bs.getSectorsPerCluster() * getBytesPerSector());
    }

    /**
     * Available space of the Filesystem in bytes
     */
    @Override public long getAvailableSpace() {
        return getSize() - getUsedSpace();
    }

    @FunctionalInterface
    private interface EntryUpdateAction {

        void invoke(DirectoryEntry entry);
    }

    /**
     * Creates a formatted floppy disk image in a stream.
     *
     * @param stream The stream to write the blank image to.
     * @param type The type of floppy to create.
     * @param label The volume label for the floppy (or null).
     * @return An object that provides access to the newly created floppy disk
     *         image.
     */
    public static FatFileSystem formatFloppy(Stream stream, FloppyDiskType type, String label) {
        long pos = stream.position();

        long ticks = System.currentTimeMillis();
        int volId = (int) ((ticks & 0xFFFF) | (ticks >>> 32));

        // Write the BIOS Parameter block (BPB) - a single sector
        byte[] bpb = new byte[512];
        int sectors;
        switch (type) {
        case DoubleDensity:
            sectors = 1440;
            ATBootSector.writeBPB(bpb,
                     sectors,
                     FatType.Fat12,
                     (short) 224,
                     0,
                     (short) 1,
                     (byte) 1,
                     new Geometry(80, 2, 9),
                     true,
                     volId,
                     label);
            break;
        case HighDensity:
            sectors = 2880;
            ATBootSector.writeBPB(bpb,
                     sectors,
                     FatType.Fat12,
                     (short) 224,
                     0,
                     (short) 1,
                     (byte) 1,
                     new Geometry(80, 2, 18),
                     true,
                     volId,
                     label);
            break;
        case Extended:
            sectors = 5760;
            ATBootSector.writeBPB(bpb,
                     sectors,
                     FatType.Fat12,
                     (short) 224,
                     0,
                     (short) 1,
                     (byte) 1,
                     new Geometry(80, 2, 36),
                     true,
                     volId,
                     label);
            break;
        default:
            throw new IllegalArgumentException("Unrecognised Floppy Disk type");
        }

        stream.write(bpb, 0, bpb.length);

        // Write both FAT copies
        int fatSize = ATBootSector.calcFatSize(sectors, FatType.Fat12, (byte) 1);
        byte[] fat = new byte[fatSize * Sizes.Sector];
        FatBuffer fatBuffer = new FatBuffer(FatType.Fat12, fat);
        fatBuffer.setNext(0, 0xFFFFFFF0);
        fatBuffer.setEndOfChain(1);
        stream.write(fat, 0, fat.length);
        stream.write(fat, 0, fat.length);

        // Write the (empty) root directory
        int rootDirSectors = (224 * 32 + Sizes.Sector - 1) / Sizes.Sector;
        byte[] rootDir = new byte[rootDirSectors * Sizes.Sector];
        stream.write(rootDir, 0, rootDir.length);

        // Write a single byte at the end of the disk to ensure the stream is at
        // least as big as needed for this disk image.
        stream.position(pos + sectors * Sizes.Sector - 1);
        stream.writeByte((byte) 0);

        // Give the caller access to the new file system
        stream.position(pos);
        return new FatFileSystem(stream);
    }

    /**
     * Formats a virtual hard disk partition.
     *
     * @param disk The disk containing the partition.
     * @param partitionIndex The index of the partition on the disk.
     * @param label The volume label for the partition (or null).
     * @return An object that provides access to the newly created partition
     *         file system.
     */
    public static FatFileSystem formatPartition(VirtualDisk disk, int partitionIndex, String label) {
        try (Stream partitionStream = disk.getPartitions().get(partitionIndex).open()) {
            return formatPartition(partitionStream,
                                   label,
                                   disk.getGeometry(),
                                   (int) disk.getPartitions().get(partitionIndex).getFirstSector(),
                                   (int) (1 + disk.getPartitions().get(partitionIndex).getLastSector() -
                                          disk.getPartitions().get(partitionIndex).getFirstSector()),
                                   (short) 0);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Creates a formatted hard disk partition in a stream.
     *
     * @param stream The stream to write the new file system to.
     * @param label The volume label for the partition (or null).
     * @param diskGeometry The geometry of the disk containing the partition.
     * @param firstSector The starting sector number of this partition (hide's
     *            sectors in other partitions).
     * @param sectorCount The number of sectors in this partition.
     * @param reservedSectors The number of reserved sectors at the start of the
     *            partition.
     * @return An object that provides access to the newly created partition
     *         file system.
     */
    public static FatFileSystem formatPartition(Stream stream,
                                                String label,
                                                Geometry diskGeometry,
                                                int firstSector,
                                                int sectorCount,
                                                short reservedSectors) {
        long pos = stream.position();

        long ticks = System.currentTimeMillis();
        int volId = (int) ((ticks & 0xFFFF) | (ticks >>> 32));

        byte sectorsPerCluster;
        FatType fatType;
        short maxRootEntries;

        // Write the BIOS Parameter block (BPB) - a single sector

        byte[] bpb = new byte[512];
        if (sectorCount <= 8400) {
            throw new IllegalArgumentException("Requested size is too small for a partition");
        }
        if (sectorCount < 1024 * 1024) {
            fatType = FatType.Fat16;
            maxRootEntries = 512;
            if (sectorCount <= 32680) {
                sectorsPerCluster = 2;
            } else if (sectorCount <= 262144) {
                sectorsPerCluster = 4;
            } else if (sectorCount <= 524288) {
                sectorsPerCluster = 8;
            } else {
                sectorsPerCluster = 16;
            }
            if (reservedSectors < 1) {
                reservedSectors = 1;
            }
        } else {
            fatType = FatType.Fat32;
            maxRootEntries = 0;
            if (sectorCount <= 532480) {
                sectorsPerCluster = 1;
            } else if (sectorCount <= 16777216) {
                sectorsPerCluster = 8;
            } else if (sectorCount <= 33554432) {
                sectorsPerCluster = 16;
            } else if (sectorCount <= 67108864) {
                sectorsPerCluster = 32;
            } else {
                sectorsPerCluster = 64;
            }
            if (reservedSectors < 32) {
                reservedSectors = 32;
            }
        }

        ATBootSector.writeBPB(bpb,
                 sectorCount,
                 fatType,
                 maxRootEntries,
                 firstSector,
                 reservedSectors,
                 sectorsPerCluster,
                 diskGeometry,
                 false,
                 volId,
                 label);
        stream.write(bpb, 0, bpb.length);

        // Skip the reserved sectors

        stream.position(pos + reservedSectors * Sizes.Sector);

        // Write both FAT copies

        byte[] fat = new byte[ATBootSector.calcFatSize(sectorCount, fatType, sectorsPerCluster) * Sizes.Sector];
        FatBuffer fatBuffer = new FatBuffer(fatType, fat);
        fatBuffer.setNext(0, 0xFFFFFFF8);
        fatBuffer.setEndOfChain(1);
        if (fatType.getValue() >= FatType.Fat32.getValue()) {
            // Mark cluster 2 as End-of-chain (i.e. root directory
            // is a single cluster in length)
            fatBuffer.setEndOfChain(2);
        }

        stream.write(fat, 0, fat.length);
        stream.write(fat, 0, fat.length);

        // Write the (empty) root directory

        int rootDirSectors;
        if (fatType.getValue() < FatType.Fat32.getValue()) {
            rootDirSectors = (maxRootEntries * 32 + Sizes.Sector - 1) / Sizes.Sector;
        } else {
            rootDirSectors = sectorsPerCluster;
        }

        byte[] rootDir = new byte[rootDirSectors * Sizes.Sector];
        stream.write(rootDir, 0, rootDir.length);

        // Make sure the stream is at least as large as the partition requires.

        if (stream.getLength() < pos + (long) sectorCount * Sizes.Sector) {
            stream.setLength(pos + (long) sectorCount * Sizes.Sector);
        }

        // Give the caller access to the new file system

        stream.position(pos);
        return new FatFileSystem(stream);
    }

    @Override
    public String toString() {
        return getFriendlyName() + ", " + bs.getOemName();
    }
}
