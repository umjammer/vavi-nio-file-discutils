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

package DiscUtils.Wim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import vavi.util.win32.DateUtil;

import DiscUtils.Core.IWindowsFileSystem;
import DiscUtils.Core.ReadOnlyDiscFileSystem;
import DiscUtils.Core.ReparsePoint;
import DiscUtils.Core.WindowsFileInformation;
import DiscUtils.Core.Internal.ObjectCache;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.ZeroStream;
import DiscUtils.Streams.ReaderWriter.LittleEndianDataReader;
import DiscUtils.Streams.Util.MathUtilities;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.FileNotFoundException;
import moe.yo3explorer.dotnetio4j.IOException;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * Provides access to the file system within a WIM file image.
 */
public class WimFileSystem extends ReadOnlyDiscFileSystem implements IWindowsFileSystem {
    private final ObjectCache<Long, List<DirectoryEntry>> _dirCache;

    private WimFile _file;

    private Stream _metaDataStream;

    private long _rootDirPos;

    private List<RawSecurityDescriptor> _securityDescriptors;

    public WimFileSystem(WimFile file, int index) {
        _file = file;
        ShortResourceHeader metaDataFileInfo = _file.locateImage(index);
        if (metaDataFileInfo == null) {
            throw new IllegalArgumentException("No such image: " + index);
        }

        _metaDataStream = _file.openResourceStream(metaDataFileInfo);
        readSecurityDescriptors();
        _dirCache = new ObjectCache<>();
    }

    /**
     * Provides a friendly description of the file system type.
     */
    public String getFriendlyName() {
        return "Microsoft WIM";
    }

    /**
     * Gets the security descriptor associated with the file or directory.
     *
     * @param path The file or directory to inspect.
     * @return The security descriptor.
     */
    public RawSecurityDescriptor getSecurity(String path) {
        int id = getEntry(path).SecurityId;
        if (id == Integer.MAX_VALUE) {
            return null;
        }

        if (id >= 0 && id < _securityDescriptors.size()) {
            return _securityDescriptors[id];
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
    public void setSecurity(String path, RawSecurityDescriptor securityDescriptor) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the reparse point data associated with a file or directory.
     *
     * @param path The file to query.
     * @return The reparse point information.
     */
    public ReparsePoint getReparsePoint(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        ShortResourceHeader hdr = _file.locateResource(dirEntry.Hash);
        if (hdr == null) {
            throw new IOException("No reparse point");
        }

        Stream s = _file.openResourceStream(hdr);
        try {
            {
                byte[] buffer = new byte[(int) s.getLength()];
                s.read(buffer, 0, buffer.length);
                return new ReparsePoint(dirEntry.ReparseTag, buffer);
            }
        } finally {
            if (s != null)
                s.close();
        }
    }

    /**
     * Sets the reparse point data on a file or directory.
     *
     * @param path The file to set the reparse point on.
     * @param reparsePoint The new reparse point.
     */
    public void setReparsePoint(String path, ReparsePoint reparsePoint) {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes a reparse point from a file or directory, without deleting the
     * file or directory.
     *
     * @param path The path to the file or directory to remove the reparse point
     *            from.
     */
    public void removeReparsePoint(String path) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the short name for a given path.
     *
     * @param path The path to convert.
     * @return The short name.
     *         This method only gets the short name for the final part of the
     *         path, to
     *         convert a complete path, call this method repeatedly, once for
     *         each path
     *         segment. If there is no short name for the given path,
     *         {@code null}
     *         is
     *         returned.
     */
    public String getShortName(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        return dirEntry.ShortName;
    }

    /**
     * Sets the short name for a given file or directory.
     *
     * @param path The full path to the file or directory to change.
     * @param shortName The shortName, which should not include a path.
     */
    public void setShortName(String path, String shortName) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the standard file information for a file.
     *
     * @param path The full path to the file or directory to query.
     * @return The standard file information.
     */
    public WindowsFileInformation getFileStandardInformation(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        return new WindowsFileInformation();
    }

    /**
     * Sets the standard file information for a file.
     *
     * @param path The full path to the file or directory to query.
     * @param info The standard file information.
     */
    public void setFileStandardInformation(String path, WindowsFileInformation info) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the names of the alternate data streams for a file.
     *
     * @param path The path to the file.
     * @return
     *         The list of alternate data streams (or empty, if none). To access
     *         the contents
     *         of the alternate streams, use OpenFile(path + ":" + name, ...).
     */
    public List<String> getAlternateDataStreams(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        List<String> names = new ArrayList<>();
        if (dirEntry.AlternateStreams != null) {
            for (Map.Entry<String, AlternateStreamEntry> altStream : dirEntry.AlternateStreams.entrySet()) {
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
     * @return The file id, or -1.
     *         The returned file id uniquely identifies the file, and is shared
     *         by all hard
     *         links to the same file. The value -1 indicates no unique
     *         identifier is
     *         available, and so it can be assumed the file has no hard links.
     */
    public long getFileId(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        return dirEntry.HardLink == 0 ? -1 : (long) dirEntry.HardLink;
    }

    /**
     * Indicates whether the file is known by other names.
     *
     * @param path The file to inspect.
     * @return
     *         {@code true}
     *         if the file has other names, else
     *         {@code false}
     *         .
     */
    public boolean hasHardLinks(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        return dirEntry.HardLink != 0;
    }

    /**
     * Indicates if a directory exists.
     *
     * @param path The path to test.
     * @return true if the directory exists.
     */
    public boolean directoryExists(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        return dirEntry != null && dirEntry.Attributes.containsKey("Directory");
    }

    /**
     * Indicates if a file exists.
     *
     * @param path The path to test.
     * @return true if the file exists.
     */
    public boolean fileExists(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        return dirEntry != null && dirEntry.Attributes.containsKey("Directory");
    }

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
    public List<String> getDirectories(String path, String searchPattern, String searchOption) {
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
     * @return Array of files matching the search pattern.
     */
    public List<String> getFiles(String path, String searchPattern, String searchOption) {
        Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);
        List<String> results = new ArrayList<>();
        doSearch(results, path, re, searchOption.equals("AllDirectories"), false, true);
        return results;
    }

    /**
     * Gets the names of all files and subdirectories in a specified directory.
     *
     * @param path The path to search.
     * @return Array of files and subdirectories matching the search pattern.
     */
    public List<String> getFileSystemEntries(String path) {
        DirectoryEntry parentDirEntry = getEntry(path);
        if (parentDirEntry == null) {
            throw new FileNotFoundException(String.format("The directory '%s' does not exist", path));
        }

        List<DirectoryEntry> parentDir = getDirectory(parentDirEntry.SubdirOffset);
        return parentDir.stream().map(m -> {
            return Utilities.combinePaths(path, m.FileName);
        }).collect(Collectors.toList());
    }

    /**
     * Gets the names of files and subdirectories in a specified directory
     * matching a specified
     * search pattern.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return Array of files and subdirectories matching the search pattern.
     */
    public List<String> getFileSystemEntries(String path, String searchPattern) {
        Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);
        DirectoryEntry parentDirEntry = getEntry(path);
        if (parentDirEntry == null) {
            throw new FileNotFoundException(String.format("The directory '%s' does not exist", path));
        }

        List<DirectoryEntry> parentDir = getDirectory(parentDirEntry.SubdirOffset);
        List<String> result = new ArrayList<>();
        for (DirectoryEntry dirEntry : parentDir) {
            if (re.matcher(dirEntry.FileName).find()) {
                result.add(Utilities.combinePaths(path, dirEntry.FileName));
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
    public SparseStream openFile(String path, FileMode mode, FileAccess access) {
        if (mode != FileMode.Open && mode != FileMode.OpenOrCreate) {
            throw new UnsupportedOperationException("No write support for WIM files");
        }

        if (access != FileAccess.Read) {
            throw new UnsupportedOperationException("No write support for WIM files");
        }

        byte[] streamHash = getFileHash(path);
        ShortResourceHeader hdr = _file.locateResource(streamHash);
        if (hdr == null) {
            if (Utilities.isAllZeros(streamHash, 0, streamHash.length)) {
                return new ZeroStream(0);
            }

            throw new IOException("Unable to locate file contents");
        }

        return _file.openResourceStream(hdr);
    }

    /**
     * Gets the attributes of a file or directory.
     *
     * @param path The file or directory to inspect.
     * @return The attributes of the file or directory.
     */
    public Map<String, Object> getAttributes(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        if (dirEntry == null) {
            throw new FileNotFoundException("No such file or directory " + path);
        }

        return dirEntry.Attributes;
    }

    /**
     * Gets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The creation time.
     */
    public long getCreationTimeUtc(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        if (dirEntry == null) {
            throw new FileNotFoundException("No such file or directory " + path);
        }

        return DateUtil.filetimeToLong(dirEntry.CreationTime);
    }

    /**
     * Gets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last access time.
     */
    public long getLastAccessTimeUtc(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        if (dirEntry == null) {
            throw new FileNotFoundException("No such file or directory " + path);
        }

        return DateUtil.filetimeToLong(dirEntry.LastAccessTime);
    }

    /**
     * Gets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last write time.
     */
    public long getLastWriteTimeUtc(String path) {
        DirectoryEntry dirEntry = getEntry(path);
        if (dirEntry == null) {
            throw new FileNotFoundException("No such file or directory " + path);
        }

        return DateUtil.filetimeToLong(dirEntry.LastWriteTime);
    }

    /**
     * Gets the length of a file.
     *
     * @param path The path to the file.
     * @return The length in bytes.
     */
    public long getFileLength(String path) {
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
     * @param path The path to the file.
     * @return The 160-bit hash.The WIM file format internally stores the SHA-1
     *         hash of files.
     *         This method provides access to the stored hash. Callers can use
     *         this
     *         value to compare against the actual hash of the byte stream to
     *         validate
     *         the integrity of the file contents.
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
    public long getSize() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }

    /**
     * Used space of the Filesystem in bytes
     */
    public long getUsedSpace() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }

    /**
     * Available space of the Filesystem in bytes
     */
    public long getAvailableSpace() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }

    /**
     * Disposes of this instance.
     */
    public void close() throws IOException {
        try {
            _metaDataStream.close();
            _metaDataStream = null;
            _file = null;
        } finally {
            super.close();
        }
    }

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
        List<DirectoryEntry> dir = _dirCache.get___idx(id);
        if (dir == null) {
            _metaDataStream.setPosition(id == 0 ? _rootDirPos : id);
            LittleEndianDataReader reader = new LittleEndianDataReader(_metaDataStream);
            dir = new ArrayList<>();
            DirectoryEntry entry = DirectoryEntry.readFrom(reader);
            while (entry != null) {
                dir.add(entry);
                entry = DirectoryEntry.readFrom(reader);
            }
            _dirCache.set___idx(id, dir);
        }

        return dir;
    }

    private void readSecurityDescriptors() {
        LittleEndianDataReader reader = new LittleEndianDataReader(_metaDataStream);
        long startPos = reader.getPosition();
        int totalLength = reader.readUInt32();
        int numEntries = reader.readUInt32();
        long[] sdLengths = new long[numEntries];
        for (int i = 0; i < numEntries; ++i) {
            sdLengths[i] = reader.readUInt64();
        }
        _securityDescriptors = new ArrayList<>((int) numEntries);
        for (int i = 0; i < numEntries; ++i) {
            _securityDescriptors.add(new RawSecurityDescriptor(reader.readBytes((int) sdLengths[i]), 0));
        }
        if (reader.getPosition() < startPos + totalLength) {
            reader.skip((int) (startPos + totalLength - reader.getPosition()));
        }

        _rootDirPos = MathUtilities.roundUp(startPos + totalLength, 8);
    }

    private DirectoryEntry getEntry(String path) {
        if (path.endsWith("\\")) {
            path = path.substring(0, path.length() - 1);
        }

        if (path != null && !path.isEmpty() && !path.startsWith("\\")) {
            path = "\\" + path;
        }

        return getEntry(getDirectory(0), path.split("\\"));
    }

    private DirectoryEntry getEntry(List<DirectoryEntry> dir, String[] path) {
        List<DirectoryEntry> currentDir = dir;
        DirectoryEntry nextEntry = null;
        for (int i = 0; i < path.length; ++i) {
            nextEntry = null;
            for (DirectoryEntry entry : currentDir) {
                if (path[i].equals(entry.FileName) ||
                    (entry.ShortName != null && !entry.ShortName.isEmpty() && path[i].equals(entry.ShortName))) {
                    nextEntry = entry;
                    break;
                }

            }
            if (nextEntry == null) {
                return null;
            }

            if (nextEntry.SubdirOffset != 0) {
                currentDir = getDirectory(nextEntry.SubdirOffset);
            }

        }
        return nextEntry;
    }

    private void doSearch(List<String> results, String path, Pattern regex, boolean subFolders, boolean dirs, boolean files) {
        DirectoryEntry parentDirEntry = getEntry(path);
        if (parentDirEntry.SubdirOffset == 0) {
            return;
        }

        List<DirectoryEntry> parentDir = getDirectory(parentDirEntry.SubdirOffset);
        for (DirectoryEntry de : parentDir) {
            boolean isDir = de.Attributes.containsKey("Directory");
            if ((isDir && dirs) || (!isDir && files)) {
                if (regex.matcher(de.getSearchName()).find()) {
                    results.add(Utilities.combinePaths(path, de.FileName));
                }
            }

            if (subFolders && isDir) {
                doSearch(results, Utilities.combinePaths(path, de.FileName), regex, subFolders, dirs, files);
            }
        }
    }
}
