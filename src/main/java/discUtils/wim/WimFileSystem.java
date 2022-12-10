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

package discUtils.wim;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import vavi.util.win32.DateUtil;

import discUtils.core.IWindowsFileSystem;
import discUtils.core.ReadOnlyDiscFileSystem;
import discUtils.core.ReparsePoint;
import discUtils.core.WindowsFileInformation;
import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.internal.ObjectCache;
import discUtils.core.internal.Utilities;
import discUtils.streams.SparseStream;
import discUtils.streams.ZeroStream;
import discUtils.streams.readerWriter.LittleEndianDataReader;
import discUtils.streams.util.MathUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileNotFoundException;
import dotnet4j.io.Stream;
import dotnet4j.util.compat.StringUtilities;
import dotnet4j.security.accessControl.RawSecurityDescriptor;


/**
 * Provides access to the file system within a WIM file image.
 */
public class WimFileSystem extends ReadOnlyDiscFileSystem implements IWindowsFileSystem {

    private static final String FS = File.separator;

    private final ObjectCache<Long, List<DirectoryEntry>> dirCache;

    private WimFile file;

    private Stream metaDataStream;

    private long rootDirPos;

    private List<RawSecurityDescriptor> securityDescriptors;

    WimFileSystem(WimFile file, int index) {
        this.file = file;
        ShortResourceHeader metaDataFileInfo = this.file.locateImage(index);
        if (metaDataFileInfo == null) {
            throw new IllegalArgumentException("No such image: " + index);
        }

        metaDataStream = this.file.openResourceStream(metaDataFileInfo);
        readSecurityDescriptors();
        dirCache = new ObjectCache<>();
    }

    /**
     * Provides a friendly description of the file system type.
     */
    @Override public String getFriendlyName() {
        return "Microsoft WIM";
    }

    /**
     * Gets the security descriptor associated with the file or directory.
     *
     * @param path The file or directory to inspect.
     * @return The security descriptor.
     */
    @Override public RawSecurityDescriptor getSecurity(String path) {
        int id = getEntry(path).securityId;
        if (id == 0xffff_ffff) {
            return null;
        }

        if (id >= 0 && id < securityDescriptors.size()) {
            return securityDescriptors.get(id);
        }

        throw new UnsupportedOperationException();
    }

    // What if there is no descriptor?

    /**
     * Sets the security descriptor associated with the file or directory.
     *
     * @param path The file or directory to change.
     * @param securityDescriptor The new security descriptor.
     */
    @Override public void setSecurity(String path, RawSecurityDescriptor securityDescriptor) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the reparse point data associated with a file or directory.
     *
     * @param path The file to query.
     * @return The reparse point information.
     */
    @Override public ReparsePoint getReparsePoint(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        ShortResourceHeader hdr = file.locateResource(dirEntry.hash);
        if (hdr == null) {
            throw new dotnet4j.io.IOException("No reparse point");
        }

        try (Stream s = file.openResourceStream(hdr)) {
            byte[] buffer = new byte[(int) s.getLength()];
            s.read(buffer, 0, buffer.length);
            return new ReparsePoint(dirEntry.reparseTag, buffer);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Sets the reparse point data on a file or directory.
     *
     * @param path The file to set the reparse point on.
     * @param reparsePoint The new reparse point.
     */
    @Override public void setReparsePoint(String path, ReparsePoint reparsePoint) {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes a reparse point from a file or directory, without deleting the
     * file or directory.
     *
     * @param path The path to the file or directory to remove the reparse point
     *            from.
     */
    @Override public void removeReparsePoint(String path) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the short name for a given path.
     *
     * @param path The path to convert.
     * @return The short name. This method only gets the short name for the
     *         final part of the path, to convert a complete path, call this
     *         method repeatedly, once for each path segment. If there is no
     *         short name for the given path, {@code null} is returned.
     */
    @Override public String getShortName(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        return dirEntry.shortName;
    }

    /**
     * Sets the short name for a given file or directory.
     *
     * @param path The full path to the file or directory to change.
     * @param shortName The shortName, which should not include a path.
     */
    @Override public void setShortName(String path, String shortName) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the standard file information for a file.
     *
     * @param path The full path to the file or directory to query.
     * @return The standard file information.
     */
    @Override public WindowsFileInformation getFileStandardInformation(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        WindowsFileInformation wfi = new WindowsFileInformation();
        wfi.setCreationTime(DateUtil.fromFileTime(dirEntry.creationTime));
        wfi.setLastAccessTime(DateUtil.fromFileTime(dirEntry.lastAccessTime));
        wfi.setChangeTime(DateUtil
                .fromFileTime(Math.max(dirEntry.lastWriteTime, Math.max(dirEntry.creationTime, dirEntry.lastAccessTime))));
        wfi.setLastWriteTime(DateUtil.fromFileTime(dirEntry.lastWriteTime));
        wfi.setFileAttributes(dirEntry.attributes);
        return wfi;
    }

    /**
     * Sets the standard file information for a file.
     *
     * @param path The full path to the file or directory to query.
     * @param info The standard file information.
     */
    @Override public void setFileStandardInformation(String path, WindowsFileInformation info) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the names of the alternate data streams for a file.
     *
     * @param path The path to the file.
     * @return The list of alternate data streams (or empty, if none). To access
     *         the contents of the alternate streams, use OpenFile(path + ":" +
     *         name, ...).
     */
    @Override public List<String> getAlternateDataStreams(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        List<String> names = new ArrayList<>();
        if (dirEntry.alternateStreams != null) {
            for (Map.Entry<String, AlternateStreamEntry> altStream : dirEntry.alternateStreams.entrySet()) {
                if (altStream.getKey() != null && altStream.getKey().isEmpty()) {
                    names.add(altStream.getKey());
                }
            }
        }

        return names;
    }

    /**
     * Gets the file id for a given path.
     *
     * @param path The path to get the id of.
     * @return The file id, or -1. The returned file id uniquely identifies the
     *         file, and is shared by all hard links to the same file. The value
     *         -1 indicates no unique identifier is available, and so it can be
     *         assumed the file has no hard links.
     */
    @Override public long getFileId(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        return dirEntry.hardLink == 0 ? -1 : (long) dirEntry.hardLink;
    }

    /**
     * Indicates whether the file is known by other names.
     *
     * @param path The file to inspect.
     * @return {@code true} if the file has other names, else {@code false} .
     */
    @Override public boolean hasHardLinks(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        return dirEntry.hardLink != 0;
    }

    /**
     * Indicates if a directory exists.
     *
     * @param path The path to test.
     * @return true if the directory exists.
     */
    @Override public boolean directoryExists(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        return dirEntry != null && dirEntry.attributes.contains(FileAttributes.Directory);
    }

    /**
     * Indicates if a file exists.
     *
     * @param path The path to test.
     * @return true if the file exists.
     */
    @Override public boolean fileExists(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        return dirEntry != null && !dirEntry.attributes.contains(FileAttributes.Directory);
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
        doSearch(dirs, path, re, searchOption.equals("AllDirectories"), true, false);
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
    @Override public List<String> getFiles(String path, String searchPattern, String searchOption) {
        Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);
        List<String> results = new ArrayList<>();
        doSearch(results, path, re, searchOption.equals("AllDirectories"), false, true);
        return results;
    }

    /**
     * Gets the names of all files and subdirectories in a specified directory.
     *
     * @param path The path to search.
     * @return list of files and subdirectories matching the search pattern.
     */
    @Override public List<String> getFileSystemEntries(String path) {
        DirectoryEntry parentDirEntry = getEntry(path);
        if (parentDirEntry == null) {
            throw new FileNotFoundException(String.format("The directory '%s' does not exist", path));
        }

        List<DirectoryEntry> parentDir = getDirectory(parentDirEntry.subdirOffset);
        return parentDir.stream().map(m -> Utilities.combinePaths(path, m.fileName)).collect(Collectors.toList());
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
        DirectoryEntry parentDirEntry = getEntry(path);
        if (parentDirEntry == null) {
            throw new FileNotFoundException(String.format("The directory '%s' does not exist", path));
        }

        List<DirectoryEntry> parentDir = getDirectory(parentDirEntry.subdirOffset);
        List<String> result = new ArrayList<>();
        for (DirectoryEntry dirEntry : parentDir) {
            if (re.matcher(dirEntry.fileName).find()) {
                result.add(Utilities.combinePaths(path, dirEntry.fileName));
            }

        }
        return result;
    }

    /**
     * Opens the specified file.
     *
     * @param path The full path of the file to open.
     * @param mode The file mode for the created stream.
     * @param access The access permissions for the created stream.
     * @return The new stream.
     */
    @Override public SparseStream openFile(String path, FileMode mode, FileAccess access) {
        if (mode != FileMode.Open && mode != FileMode.OpenOrCreate) {
            throw new UnsupportedOperationException("No write support for WIM files");
        }

        if (access != FileAccess.Read) {
            throw new UnsupportedOperationException("No write support for WIM files");
        }

        byte[] streamHash = getFileHash(path);
        ShortResourceHeader hdr = file.locateResource(streamHash);
        if (hdr == null) {
            if (Utilities.isAllZeros(streamHash, 0, streamHash.length)) {
                return new ZeroStream(0);
            }

            throw new dotnet4j.io.IOException("Unable to locate file contents");
        }

        return file.openResourceStream(hdr);
    }

    /**
     * Gets the attributes of a file or directory.
     *
     * @param path The file or directory to inspect.
     * @return The attributes of the file or directory.
     */
    @Override public Map<String, Object> getAttributes(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        if (dirEntry == null) {
            throw new FileNotFoundException("No such file or directory " + path);
        }

        return FileAttributes.toMap(dirEntry.attributes);
    }

    /**
     * Gets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The creation time.
     */
    @Override public long getCreationTimeUtc(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        if (dirEntry == null) {
            throw new FileNotFoundException("No such file or directory " + path);
        }

        return DateUtil.filetimeToLong(dirEntry.creationTime);
    }

    /**
     * Gets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last access time.
     */
    @Override public long getLastAccessTimeUtc(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        if (dirEntry == null) {
            throw new FileNotFoundException("No such file or directory " + path);
        }

        return DateUtil.filetimeToLong(dirEntry.lastAccessTime);
    }

    /**
     * Gets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last write time.
     */
    @Override public long getLastWriteTimeUtc(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        if (dirEntry == null) {
            throw new FileNotFoundException("No such file or directory " + path);
        }

        return DateUtil.filetimeToLong(dirEntry.lastWriteTime);
    }

    /**
     * Gets the length of a file.
     *
     * @param path The path to the file.
     * @return The length in bytes.
     */
    @Override public long getFileLength(String path) {
        String[] filePart = new String[1];
        String[] altStreamPart = new String[1];
        splitFileName(path, filePart, altStreamPart);
        DirectoryEntry dirEntry = getEntry(filePart[0]);
        if (dirEntry == null) {
            throw new FileNotFoundException("No such file or directory " + path);
        }

        return dirEntry.getLength(altStreamPart[0]);
    }

    /**
     * Gets the SHA-1 hash of a file's contents.
     *
     * The WIM file format internally stores the SHA-1 hash of files. This
     * method provides access to the stored hash. Callers can use this value to
     * compare against the actual hash of the byte stream to validate the
     * integrity of the file contents.
     *
     * @param path The path to the file.
     * @return The 160-bit hash.
     */
    public byte[] getFileHash(String path) {
        String[] filePart = new String[1];
        String[] altStreamPart = new String[1];
        splitFileName(path, filePart, altStreamPart);
        DirectoryEntry dirEntry = getEntry(filePart[0]);
        if (dirEntry == null) {
            throw new FileNotFoundException("No such file or directory " + path);
        }

        return dirEntry.getStreamHash(altStreamPart[0]);
    }

    /**
     * Size of the Filesystem in bytes
     */
    @Override public long getSize() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }

    /**
     * Used space of the Filesystem in bytes
     */
    @Override public long getUsedSpace() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }

    /**
     * Available space of the Filesystem in bytes
     */
    @Override public long getAvailableSpace() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }

    /**
     * Disposes of this instance.
     */
    @Override public void close() throws IOException {
        try {
            metaDataStream.close();
            metaDataStream = null;
            file = null;
        } finally {
            super.close();
        }
    }

    /**
     * @param filePart {@cs out}
     * @param altStreamPart {@cs out}
     */
    private static void splitFileName(String path, String[] filePart, String[] altStreamPart) {
        int streamSepPos = path.indexOf(":");
        if (streamSepPos >= 0) {
            filePart[0] = path.substring(0, streamSepPos);
            altStreamPart[0] = path.substring(streamSepPos + 1);
        } else {
            filePart[0] = path;
            altStreamPart[0] = "";
        }
    }

    private List<DirectoryEntry> getDirectory(long id) {
        List<DirectoryEntry> dir = dirCache.get(id);
        if (dir == null) {
            metaDataStream.position(id == 0 ? rootDirPos : id);
            LittleEndianDataReader reader = new LittleEndianDataReader(metaDataStream);
            dir = new ArrayList<>();
            DirectoryEntry entry = DirectoryEntry.readFrom(reader);
            while (entry != null) {
                dir.add(entry);
                entry = DirectoryEntry.readFrom(reader);
            }
            dirCache.put(id, dir);
        }

        return dir;
    }

    private void readSecurityDescriptors() {
        LittleEndianDataReader reader = new LittleEndianDataReader(metaDataStream);
        long startPos = reader.position();
        int totalLength = reader.readUInt32();
        int numEntries = reader.readUInt32();
        long[] sdLengths = new long[numEntries];
        for (int i = 0; i < numEntries; ++i) {
            sdLengths[i] = reader.readUInt64();
        }
        securityDescriptors = new ArrayList<>(numEntries);
        for (int i = 0; i < numEntries; ++i) {
            securityDescriptors.add(new RawSecurityDescriptor(reader.readBytes((int) sdLengths[i]), 0));
        }
        if (reader.position() < startPos + totalLength) {
            reader.skip((int) (startPos + totalLength - reader.position()));
        }

        rootDirPos = MathUtilities.roundUp(startPos + totalLength, 8);
    }

    private DirectoryEntry getEntry(String path) {
        if (path.endsWith(FS)) {
            path = path.substring(0, path.length() - 1);
        }

        if (path != null && !path.isEmpty() && !path.startsWith(FS)) {
            path = FS + path;
        }

        return getEntry(getDirectory(0), path.split(StringUtilities.escapeForRegex(FS)));
    }

    private DirectoryEntry getEntry(List<DirectoryEntry> dir, String[] path) {
        List<DirectoryEntry> currentDir = dir;
        DirectoryEntry nextEntry = null;
        for (String s : path) {
            nextEntry = null;
            for (DirectoryEntry entry : currentDir) {
                if (s.equals(entry.fileName) ||
                        (entry.shortName != null && !entry.shortName.isEmpty() && s.equals(entry.shortName))) {
                    nextEntry = entry;
                    break;
                }
            }
            if (nextEntry == null) {
                return null;
            }

            if (nextEntry.subdirOffset != 0) {
                currentDir = getDirectory(nextEntry.subdirOffset);
            }
        }
        return nextEntry;
    }

    private void doSearch(List<String> results, String path, Pattern regex, boolean subFolders, boolean dirs, boolean files) {
        DirectoryEntry parentDirEntry = getEntry(path);

        if (parentDirEntry.subdirOffset == 0) {
            return;
        }

        List<DirectoryEntry> parentDir = getDirectory(parentDirEntry.subdirOffset);

        for (DirectoryEntry de : parentDir) {
            boolean isDir = de.attributes.contains(FileAttributes.Directory);

            if ((isDir && dirs) || (!isDir && files)) {
                if (regex.matcher(de.getSearchName()).find()) {
                    results.add(Utilities.combinePaths(path, de.fileName));
                }
            }

            if (subFolders && isDir) {
                doSearch(results, Utilities.combinePaths(path, de.fileName), regex, subFolders, dirs, files);
            }
        }
    }
}
