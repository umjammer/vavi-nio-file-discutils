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

package discUtils.ntfs;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import discUtils.core.ClusterMap;
import discUtils.core.DiscFileSystem;
import discUtils.core.Geometry;
import discUtils.core.IClusterBasedFileSystem;
import discUtils.core.IDiagnosticTraceable;
import discUtils.core.IWindowsFileSystem;
import discUtils.core.InvalidFileSystemException;
import discUtils.core.ReparsePoint;
import discUtils.core.VolumeInfo;
import discUtils.core.WindowsFileInformation;
import discUtils.core.compression.BlockCompressor;
import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.internal.ObjectCache;
import discUtils.core.internal.Utilities;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.block.BlockCacheSettings;
import discUtils.streams.block.BlockCacheStream;
import discUtils.streams.util.BitCounter;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Range;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileNotFoundException;
import dotnet4j.io.Stream;
import dotnet4j.security.accessControl.RawSecurityDescriptor;
import dotnet4j.util.compat.StringUtilities;

import static java.lang.System.getLogger;


/**
 * Class for accessing NTFS file systems.
 */
public final class NtfsFileSystem extends DiscFileSystem implements
                                  IClusterBasedFileSystem,
                                  IWindowsFileSystem,
                                  IDiagnosticTraceable {

    private static final Logger logger = getLogger(NtfsFileSystem.class.getName());

    private static final String FS = java.io.File.separator;

    private static final EnumSet<FileAttributes> NonSettableFileAttributes = EnumSet
            .of(FileAttributes.Directory, FileAttributes.Offline, FileAttributes.ReparsePoint);

    private final NtfsContext context;

    // Top-level file system structures

    // Working state
    private final ObjectCache<Long, File> fileCache;

    private final VolumeInformation volumeInfo;

    /**
     * Initializes a new instance of the NtfsFileSystem class.
     *
     * @param stream The stream containing the NTFS file system.
     */
    public NtfsFileSystem(Stream stream) {
        super(new NtfsOptions());
        context = new NtfsContext();
        context.setRawStream(stream);
        context.setOptions(getNtfsOptions());

        context.setGetFileByIndex(this::getFile);
        context.setGetFileByRef(this::getFile);
        context.setGetDirectoryByRef(this::getDirectory);
        context.setGetDirectoryByIndex(this::getDirectory);
        context.setAllocateFile(this::allocateFile);
        context.setForgetFile(this::forgetFile);
        context.setReadOnly(!stream.canWrite());

        fileCache = new ObjectCache<>();

        stream.position(0);
        byte[] bytes = StreamUtilities.readExact(stream, 512);

        context.setBiosParameterBlock(BiosParameterBlock.fromBytes(bytes, 0));
        if (!context.getBiosParameterBlock().isValid(stream.getLength())) {
            throw new InvalidFileSystemException("BIOS Parameter block is invalid for an NTFS file system");
        }

        if (getNtfsOptions().getReadCacheEnabled()) {
            BlockCacheSettings cacheSettings = new BlockCacheSettings();
            cacheSettings.setBlockSize(context.getBiosParameterBlock().getBytesPerCluster());
            context.setRawStream(new BlockCacheStream(SparseStream.fromStream(stream, Ownership.None),
                                                       Ownership.None,
                                                       cacheSettings));
        }

        // Bootstrap the Master File Table
        context.setMft(new MasterFileTable(context));
        File mftFile = new File(context, context.getMft().getBootstrapRecord());
        fileCache.put(MasterFileTable.MftIndex, mftFile);
        context.getMft().initialize(mftFile);

        // Get volume information (includes version number)
        File volumeInfoFile = getFile(MasterFileTable.VolumeIndex);
        volumeInfo = volumeInfoFile.getStream(AttributeType.VolumeInformation, null).getContent(VolumeInformation.class);
logger.log(Level.DEBUG, volumeInfo);

        // Initialize access to the other well-known metadata files
        context.setClusterBitmap(new ClusterBitmap(getFile(MasterFileTable.BitmapIndex)));
        context.setAttributeDefinitions(new AttributeDefinitions(getFile(MasterFileTable.AttrDefIndex)));
        context.setUpperCase(new UpperCase(getFile(MasterFileTable.UpCaseIndex)));

        if (volumeInfo.getVersion() >= VolumeInformation.VersionW2k) {
            context.setSecurityDescriptors(new SecurityDescriptors(getFile(MasterFileTable.SecureIndex)));
            context.setObjectIds(new ObjectIds(getFile(getDirectoryEntry("$Extend" + FS + "$ObjId").getReference())));
            context.setReparsePoints(new ReparsePoints(getFile(getDirectoryEntry("$Extend" + FS + "$Reparse").getReference())));
            context.setQuotas(new Quotas(getFile(getDirectoryEntry("$Extend" + FS + "$Quota").getReference())));
        }
    }

    private boolean getCreateShortNames() {
        return context.getOptions().getShortNameCreation() == ShortFileNameOption.Enabled ||
               (context.getOptions().getShortNameCreation() == ShortFileNameOption.UseVolumeFlag &&
                !volumeInfo.getFlags().contains(VolumeInformationFlags.DisableShortNameCreation));
    }

    /**
     * Gets the friendly name for the file system.
     */
    @Override public String getFriendlyName() {
        return "Microsoft NTFS";
    }

    /**
     * Gets the options that control how the file system is interpreted.
     */
    public NtfsOptions getNtfsOptions() {
        return (NtfsOptions) getOptions();
    }

    /**
     * Gets the volume label.
     */
    @Override public String getVolumeLabel() {
        File volumeFile = getFile(MasterFileTable.VolumeIndex);
        NtfsStream volNameStream = volumeFile.getStream(AttributeType.VolumeName, null);
        return volNameStream.getContent(VolumeName.class).getName();
    }

    /**
     * Indicates if the file system supports write operations.
     */
    @Override public boolean canWrite() {
        // For now, we don't...
        return !context.getReadOnly();
    }

    /**
     * Gets the size of each cluster (in bytes).
     */
    @Override public long getClusterSize() {
        return context.getBiosParameterBlock().getBytesPerCluster();
    }

    /**
     * Gets the total number of clusters managed by the file system.
     */
    @Override public long getTotalClusters() {
        return MathUtilities.ceil(context.getBiosParameterBlock().totalSectors64,
                                  context.getBiosParameterBlock().getSectorsPerCluster());
    }

    /**
     * Copies an existing file to a new file, allowing overwriting of an
     * existing file.
     *
     * @param sourceFile The source file.
     * @param destinationFile The destination file.
     * @param overwrite Whether to permit over-writing of an existing file.
     */
    @SuppressWarnings("incomplete-switch")
    @Override public void copyFile(String sourceFile, String destinationFile, boolean overwrite) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry sourceParentDirEntry = getDirectoryEntry(Utilities.getDirectoryFromPath(sourceFile));
            if (sourceParentDirEntry == null || !sourceParentDirEntry.isDirectory()) {
                throw new FileNotFoundException("No such file " + sourceFile);
            }

            Directory sourceParentDir = getDirectory(sourceParentDirEntry.getReference());

            DirectoryEntry sourceEntry = sourceParentDir.getEntryByName(Utilities.getFileFromPath(sourceFile));
            if (sourceEntry == null || sourceEntry.isDirectory()) {
                throw new FileNotFoundException("No such file " + sourceFile);
            }

            File origFile = getFile(sourceEntry.getReference());

            DirectoryEntry destParentDirEntry = getDirectoryEntry(Utilities.getDirectoryFromPath(destinationFile));
            if (destParentDirEntry == null || !destParentDirEntry.isDirectory()) {
                throw new FileNotFoundException("Destination directory not found " + destinationFile);
            }

            Directory destParentDir = getDirectory(destParentDirEntry.getReference());

            DirectoryEntry destDirEntry = destParentDir.getEntryByName(Utilities.getFileFromPath(destinationFile));
            if (destDirEntry != null && !destDirEntry.isDirectory()) {
                if (overwrite) {
                    if (destDirEntry.getReference().getMftIndex() == sourceEntry.getReference().getMftIndex()) {
                        throw new dotnet4j.io.IOException("Destination file already exists and is the source file");
                    }

                    File oldFile = getFile(destDirEntry.getReference());
                    destParentDir.removeEntry(destDirEntry);
                    if (oldFile.getHardLinkCount() == 0) {
                        oldFile.delete();
                    }
                } else {
                    throw new dotnet4j.io.IOException("Destination file already exists");
                }
            }

            File newFile = File.createNew(context, destParentDir.getStandardInformation().fileAttributeFlags);
            for (NtfsStream origStream : origFile.getAllStreams()) {
                NtfsStream newStream = newFile.getStream(origStream.getAttributeType(), origStream.getName());

                switch (origStream.getAttributeType()) {
                case Data:
                    if (newStream == null) {
                        newStream = newFile.createStream(origStream.getAttributeType(), origStream.getName());
                    }

                    try (SparseStream s = origStream.open(FileAccess.Read);
                         SparseStream d = newStream.open(FileAccess.Write)) {
                        byte[] buffer = new byte[64 * (int) Sizes.OneKiB];
                        int numRead;

                        do {
                            numRead = s.read(buffer, 0, buffer.length);
                            d.write(buffer, 0, numRead);
                        } while (numRead != 0);
                    }

                    break;

                case StandardInformation:
                    StandardInformation newSi = origStream.getContent(StandardInformation.class);
                    newStream.setContent(newSi);
                    break;
                }
            }

            addFileToDirectory(newFile, destParentDir, Utilities.getFileFromPath(destinationFile), null);
            destParentDirEntry.updateFrom(destParentDir);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Creates a directory.
     *
     * @param path The path of the new directory.
     */
    @Override public void createDirectory(String path) {
        createDirectory(path, null);
    }

    /**
     * Deletes a directory.
     *
     * @param path The path of the directory to delete.
     */
    @Override public void deleteDirectory(String path) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            if (path == null || path.isEmpty()) {
                throw new dotnet4j.io.IOException("Unable to delete root directory");
            }

            String parent = Utilities.getDirectoryFromPath(path);
            DirectoryEntry parentDirEntry = getDirectoryEntry(parent);
            if (parentDirEntry == null || !parentDirEntry.isDirectory()) {
                throw new FileNotFoundException("No such directory: " + path);
            }

            Directory parentDir = getDirectory(parentDirEntry.getReference());
            DirectoryEntry dirEntry = parentDir.getEntryByName(Utilities.getFileFromPath(path));
            if (dirEntry == null || !dirEntry.isDirectory()) {
                throw new FileNotFoundException("No such directory: " + path);
            }

            Directory dir = getDirectory(dirEntry.getReference());
            if (!dir.isEmpty()) {
                throw new dotnet4j.io.IOException("Unable to delete non-empty directory");
            }

            if (dirEntry.getDetails().getFileAttributes().contains(FileAttributes.ReparsePoint)) {
                removeReparsePoint(dir);
            }

            removeFileFromDirectory(parentDir, dir, Utilities.getFileFromPath(path));
            if (dir.getHardLinkCount() == 0) {
                dir.delete();
            }
        }
    }

    /**
     * Deletes a file.
     *
     * @param path The path of the file to delete.
     */
    @Override public void deleteFile(String path) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            String[] attributeName = new String[1];
            AttributeType[] attributeType = new AttributeType[1];
            String dirEntryPath = parsePath(path, attributeName, attributeType);

            String parentDirPath = Utilities.getDirectoryFromPath(dirEntryPath);

            DirectoryEntry parentDirEntry = getDirectoryEntry(parentDirPath);
            if (parentDirEntry == null || !parentDirEntry.isDirectory()) {
                throw new FileNotFoundException("No such file " + path);
            }

            Directory parentDir = getDirectory(parentDirEntry.getReference());

            DirectoryEntry dirEntry = parentDir.getEntryByName(Utilities.getFileFromPath(dirEntryPath));
            if (dirEntry == null || dirEntry.isDirectory()) {
                throw new FileNotFoundException("No such file " + path);
            }

            File file = getFile(dirEntry.getReference());

            if ((attributeName[0] == null || attributeName[0].isEmpty()) && attributeType[0] == AttributeType.Data) {
                if (dirEntry.getDetails().getFileAttributes().contains(FileAttributes.ReparsePoint)) {
                    removeReparsePoint(file);
                }

                removeFileFromDirectory(parentDir, file, Utilities.getFileFromPath(path));

                if (file.getHardLinkCount() == 0) {
                    file.delete();
                }
            } else {
                NtfsStream attrStream = file.getStream(attributeType[0], attributeName[0]);
                if (attrStream == null) {
                    throw new FileNotFoundException("No such attribute: " + attributeName[0]);
                }
                file.removeStream(attrStream);
            }
        }
    }

    /**
     * Indicates if a directory exists.
     *
     * @param path The path to test.
     * @return true if the directory exists.
     */
    @Override public boolean directoryExists(String path) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            // Special case - root directory
            if (path == null || path.isEmpty()) {
                return true;
            }

            DirectoryEntry dirEntry = getDirectoryEntry(path);
            return dirEntry != null && dirEntry.getDetails().getFileAttributes().contains(FileAttributes.Directory);
        }
    }

    /**
     * Indicates if a file exists.
     *
     * @param path The path to test.
     * @return true if the file exists.
     */
    @Override public boolean fileExists(String path) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            return dirEntry != null && !dirEntry.getDetails().getFileAttributes().contains(FileAttributes.Directory);
        }
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
        try (NtfsTransaction c = new NtfsTransaction()) {
            Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);
            List<String> dirs = new ArrayList<>();
            doSearch(dirs, path, re, searchOption.equals(ALL_DIRECTORIES), true, false);
            return dirs;
        }
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
        try (NtfsTransaction c = new NtfsTransaction()) {
            Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);
            List<String> results = new ArrayList<>();
            doSearch(results, path, re, searchOption.equals(ALL_DIRECTORIES), false, true);
            return results;
        }
    }

    /**
     * Gets the names of all files and subdirectories in a specified directory.
     *
     * @param path The path to search.
     * @return list of files and subdirectories.
     */
    @Override public List<String> getFileSystemEntries(String path) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry parentDirEntry = getDirectoryEntry(path);
            if (parentDirEntry == null) {
                throw new FileNotFoundException(String.format("The directory '%s' does not exist", path));
            }

            Directory parentDir = getDirectory(parentDirEntry.getReference());
            return parentDir.getAllEntries(true).stream().map(m -> Utilities.combinePaths(path, m.getDetails().fileName)).collect(Collectors.toList());
        }
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
        try (NtfsTransaction c = new NtfsTransaction()) {
            // TODO: Be smarter, use the B*Tree for better performance when the
            // start of the
            // pattern is known characters
            Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);
            DirectoryEntry parentDirEntry = getDirectoryEntry(path);
            if (parentDirEntry == null) {
                throw new FileNotFoundException(String.format("The directory '%s' does not exist", path));
            }

            Directory parentDir = getDirectory(parentDirEntry.getReference());
            List<String> result = new ArrayList<>();
            for (DirectoryEntry dirEntry : parentDir.getAllEntries(true)) {
                if (re.matcher(dirEntry.getDetails().fileName).find()) {
                    result.add(Utilities.combinePaths(path, dirEntry.getDetails().fileName));
                }
            }
            return result;
        }
    }

    /**
     * Moves a directory.
     *
     * @param sourceDirectoryName The directory to move.
     * @param destinationDirectoryName The target directory name.
     */
    @Override public void moveDirectory(String sourceDirectoryName, String destinationDirectoryName) {
        try (NtfsTransaction c1 = new NtfsTransaction()) {
            try (NtfsTransaction c2 = new NtfsTransaction()) {
                DirectoryEntry sourceParentDirEntry = getDirectoryEntry(Utilities.getDirectoryFromPath(sourceDirectoryName));
                if (sourceParentDirEntry == null || !sourceParentDirEntry.isDirectory()) {
                    throw new FileNotFoundException("No such directory: " + sourceDirectoryName);
                }

                Directory sourceParentDir = getDirectory(sourceParentDirEntry.getReference());

                DirectoryEntry sourceEntry = sourceParentDir.getEntryByName(Utilities.getFileFromPath(sourceDirectoryName));
                if (sourceEntry == null || !sourceEntry.isDirectory()) {
                    throw new FileNotFoundException("No such directory: " + sourceDirectoryName);
                }

                File file = getFile(sourceEntry.getReference());

                DirectoryEntry destParentDirEntry = getDirectoryEntry(Utilities.getDirectoryFromPath(destinationDirectoryName));
                if (destParentDirEntry == null || !destParentDirEntry.isDirectory()) {
                    throw new FileNotFoundException("Destination directory not found: " + destinationDirectoryName);
                }

                Directory destParentDir = getDirectory(destParentDirEntry.getReference());

                DirectoryEntry destDirEntry = destParentDir.getEntryByName(Utilities.getFileFromPath(destinationDirectoryName));
                if (destDirEntry != null) {
                    throw new dotnet4j.io.IOException("Destination directory already exists");
                }

                removeFileFromDirectory(sourceParentDir, file, sourceEntry.getDetails().fileName);
                addFileToDirectory(file, destParentDir, Utilities.getFileFromPath(destinationDirectoryName), null);
            }
        }
    }

    /**
     * Moves a file, allowing an existing file to be overwritten.
     *
     * @param sourceName The file to move.
     * @param destinationName The target file name.
     * @param overwrite Whether to permit a destination file to be overwritten.
     */
    @Override public void moveFile(String sourceName, String destinationName, boolean overwrite) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry sourceParentDirEntry = getDirectoryEntry(Utilities.getDirectoryFromPath(sourceName));
            if (sourceParentDirEntry == null || !sourceParentDirEntry.isDirectory()) {
                throw new FileNotFoundException("No such file " + sourceName);
            }

            Directory sourceParentDir = getDirectory(sourceParentDirEntry.getReference());
            DirectoryEntry sourceEntry = sourceParentDir.getEntryByName(Utilities.getFileFromPath(sourceName));
            if (sourceEntry == null || sourceEntry.isDirectory()) {
                throw new FileNotFoundException("No such file " + sourceName);
            }

            File file = getFile(sourceEntry.getReference());
            DirectoryEntry destParentDirEntry = getDirectoryEntry(Utilities.getDirectoryFromPath(destinationName));
            if (destParentDirEntry == null || !destParentDirEntry.isDirectory()) {
                throw new FileNotFoundException("Destination directory not found " + destinationName);
            }

            Directory destParentDir = getDirectory(destParentDirEntry.getReference());
            DirectoryEntry destDirEntry = destParentDir.getEntryByName(Utilities.getFileFromPath(destinationName));
            if (destDirEntry != null && !destDirEntry.isDirectory()) {
                if (overwrite) {
                    if (destDirEntry.getReference().getMftIndex() == sourceEntry.getReference().getMftIndex()) {
                        throw new dotnet4j.io.IOException("Destination file already exists and is the source file");
                    }

                    File oldFile = getFile(destDirEntry.getReference());
                    destParentDir.removeEntry(destDirEntry);
                    if (oldFile.getHardLinkCount() == 0) {
                        oldFile.delete();
                    }
                } else {
                    throw new dotnet4j.io.IOException("Destination file already exists");
                }
            }

            removeFileFromDirectory(sourceParentDir, file, sourceEntry.getDetails().fileName);
            addFileToDirectory(file, destParentDir, Utilities.getFileFromPath(destinationName), null);
        }
    }

    /**
     * Opens the specified file.
     *
     * @param path The full path of the file to open.
     * @param mode The file mode for the created stream.
     * @param access The access permissions for the returned stream.
     * @return The new stream.
     */
    @Override public SparseStream openFile(String path, FileMode mode, FileAccess access) {
        return openFile(path, mode, access, null);
    }

    /**
     * Gets the attributes of a file or directory.
     *
     * @param path The file or directory to inspect.
     * @return The attributes of the file or directory.
     */
    @Override public Map<String, Object> getAttributes(String path) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            return FileAttributes.toMap(dirEntry.getDetails().getFileAttributes());
        }
    }

    /**
     * Sets the attributes of a file or directory.
     *
     * @param path The file or directory to change.
     * @param newValue The new attributes of the file or directory.
     */
    @Override public void setAttributes(String path, Map<String, Object> newValue) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            EnumSet<FileAttributes> oldValue = dirEntry.getDetails().getFileAttributes();
            EnumSet<FileAttributes> _newValue = FileAttributes.toEnumSet(newValue);
            EnumSet<FileAttributes> changedAttribs = FileAttributes.xor(oldValue, _newValue);

            if (changedAttribs.isEmpty()) {
                // Abort - nothing changed
                return;
            }

            if (!Collections.disjoint(changedAttribs, NonSettableFileAttributes)) {
                throw new IllegalArgumentException("Attempt to change attributes that are read-only: " + _newValue);
            }

            File file = getFile(dirEntry.getReference());

            if (changedAttribs.contains(FileAttributes.SparseFile)) {
                if (dirEntry.isDirectory()) {
                    throw new IllegalArgumentException("Attempt to change sparse attribute on a directory: " + _newValue);
                }

                if (!_newValue.contains(FileAttributes.SparseFile)) {
                    throw new IllegalArgumentException("Attempt to remove sparse attribute from file: " + _newValue);
                }
                NtfsAttribute ntfsAttr = file.getAttribute(AttributeType.Data, null);
                if (ntfsAttr.getFlags().contains(AttributeFlags.Compressed)) {
                    throw new IllegalArgumentException("Attempt to mark compressed file as sparse: " + _newValue);
                }

                ntfsAttr.addFlag(AttributeFlags.Sparse);
                if (ntfsAttr.isNonResident()) {
                    ntfsAttr.setCompressedDataSize(ntfsAttr.getPrimaryRecord().getAllocatedLength());
                    ntfsAttr.setCompressionUnitSize(16);
                    ((NonResidentAttributeBuffer) ntfsAttr.getRawBuffer()).alignVirtualClusterCount();
                }
            }

            if (changedAttribs.contains(FileAttributes.Compressed) && !dirEntry.isDirectory()) {
                if (!_newValue.contains(FileAttributes.Compressed)) {
                    throw new IllegalArgumentException("Attempt to remove compressed attribute from file " + _newValue);
                }
                NtfsAttribute ntfsAttr = file.getAttribute(AttributeType.Data, null);
                if (ntfsAttr.getFlags().contains(AttributeFlags.Sparse)) {
                    throw new IllegalArgumentException("Attempt to mark sparse file as compressed" + _newValue);
                }

                ntfsAttr.addFlag(AttributeFlags.Compressed);
                if (ntfsAttr.isNonResident()) {
                    ntfsAttr.setCompressedDataSize(ntfsAttr.getPrimaryRecord().getAllocatedLength());
                    ntfsAttr.setCompressionUnitSize(16);
                    ((NonResidentAttributeBuffer) ntfsAttr.getRawBuffer()).alignVirtualClusterCount();
                }
            }

            updateStandardInformation(dirEntry, file, si -> si.fileAttributeFlags = FileNameRecord.setAttributes(_newValue, si.fileAttributeFlags));
        }
    }

    /**
     * Gets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The creation time.
     */
    @Override public long getCreationTimeUtc(String path) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            return dirEntry.getDetails().creationTime;
        }
    }

    /**
     * Sets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override public void setCreationTimeUtc(String path, long newTime) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            updateStandardInformation(path, si -> si.creationTime = newTime);
        }
    }

    /**
     * Gets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last access time.
     */
    @Override public long getLastAccessTimeUtc(String path) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            return dirEntry.getDetails().lastAccessTime;
        }
    }

    /**
     * Sets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override public void setLastAccessTimeUtc(String path, long newTime) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            updateStandardInformation(path, si -> si.lastAccessTime = newTime);
        }
    }

    /**
     * Gets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last write time.
     */
    @Override public long getLastWriteTimeUtc(String path) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found" + path);
            }

            return dirEntry.getDetails().modificationTime;
        }
    }

    /**
     * Sets the last modification time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override public void setLastWriteTimeUtc(String path, long newTime) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            updateStandardInformation(path, si -> si.modificationTime = newTime);
        }
    }

    /**
     * Gets the length of a file.
     *
     * @param path The path to the file.
     * @return The length in bytes.
     */
    @Override public long getFileLength(String path) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            String[] attributeName = new String[1];
            AttributeType[] attributeType = new AttributeType[1];
            String dirEntryPath = parsePath(path, attributeName, attributeType);
            DirectoryEntry dirEntry = getDirectoryEntry(dirEntryPath);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            // Ordinary file length request, use info from directory entry for
            // efficiency - if allowed
            if (getNtfsOptions().getFileLengthFromDirectoryEntries() && attributeName[0] == null &&
                attributeType[0] == AttributeType.Data) {
                return dirEntry.getDetails().realSize;
            }

            // Alternate stream / attribute, pull info from attribute record
            File file = getFile(dirEntry.getReference());
            NtfsAttribute attr = file.getAttribute(attributeType[0], attributeName[0]);
            if (attr == null) {
                throw new FileNotFoundException(String
                        .format("No such attribute '%s(%s)'", attributeName[0], attributeType[0]));
            }

            return attr.getLength();
        }
    }

    /**
     * Converts a cluster (index) into an absolute byte position in the
     * underlying stream.
     *
     * @param cluster The cluster to convert.
     * @return The corresponding absolute byte position.
     */
    @Override public long clusterToOffset(long cluster) {
        return cluster * getClusterSize();
    }

    /**
     * Converts an absolute byte position in the underlying stream to a cluster
     * (index).
     *
     * @param offset The byte position to convert.
     * @return The cluster containing the specified byte.
     */
    @Override
    public long offsetToCluster(long offset) {
        return offset / getClusterSize();
    }

    /**
     * Converts a file name to the list of clusters occupied by the file's data.
     *
     * Note that in some file systems, small files may not have dedicated
     * clusters. Only dedicated clusters will be returned.
     *
     * @param path The path to inspect.
     * @return The clusters as a list of cluster ranges.
     */
    @Override public List<Range> pathToClusters(String path) {
        String[] plainPath = new String[1];
        String[] attributeName = new String[1];
        splitPath(path, plainPath, attributeName);
        DirectoryEntry dirEntry = getDirectoryEntry(plainPath[0]);
        if (dirEntry == null || dirEntry.isDirectory()) {
            throw new FileNotFoundException("No such file " + path);
        }

        File file = getFile(dirEntry.getReference());
        NtfsStream stream = file.getStream(AttributeType.Data, attributeName[0]);
        if (stream == null) {
            throw new FileNotFoundException(String.format("File does not contain '%s' data attribute", attributeName[0]));
        }

        return stream.getClusters();
    }

    /**
     * Converts a file name to the extents containing its data.
     *
     * Use this method with caution - NTFS supports encrypted, sparse and
     * compressed files where bytes are not directly stored in extents. Small
     * files may be entirely stored in the Master File Table, where corruption
     * protection algorithms mean that some bytes do not contain the expected
     * values. This method merely indicates where file data is stored, not
     * what's stored. To access the contents of a file, use OpenFile.
     *
     * @param path The path to inspect.
     * @return The file extents, as absolute byte positions in the underlying
     *         stream.
     */
    @Override public List<StreamExtent> pathToExtents(String path) {
        String[] plainPath = new String[1];
        String[] attributeName = new String[1];
        splitPath(path, plainPath, attributeName);
        DirectoryEntry dirEntry = getDirectoryEntry(plainPath[0]);
        if (dirEntry == null || dirEntry.isDirectory()) {
            throw new FileNotFoundException("No such file " + path);
        }

        File file = getFile(dirEntry.getReference());
        NtfsStream stream = file.getStream(AttributeType.Data, attributeName[0]);
        if (stream == null) {
            throw new FileNotFoundException(String.format("File does not contain '%s' data attribute", attributeName[0]));
        }

        return stream.getAbsoluteExtents();
    }

    /**
     * Gets an object that can convert between clusters and files.
     *
     * @return The cluster map.
     */
    @Override public ClusterMap buildClusterMap() {
        return context.getMft().getClusterMap();
    }

    /**
     * Reads the boot code of the file system into a byte array.
     *
     * @return The boot code, or {@code null} if not available.
     */
    @Override public byte[] readBootCode() {
        try (Stream s = openFile(FS + "$Boot", FileMode.Open)) {
            return StreamUtilities.readExact(s, (int) s.getLength());
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Writes a diagnostic dump of key NTFS structures.
     *
     * @param writer The writer to receive the dump.
     * @param linePrefix The indent to apply to the start of each line of
     *            output.
     */
    @Override public void dump(PrintWriter writer, String linePrefix) {
        writer.println(linePrefix + "NTFS File System Dump");
        writer.println(linePrefix + "=====================");

//        context.getMft().dump(writer, linePrefix);
        writer.println(linePrefix);
        context.getBiosParameterBlock().dump(writer, linePrefix);

        if (context.getSecurityDescriptors() != null) {
            writer.println(linePrefix);
            context.getSecurityDescriptors().dump(writer, linePrefix);
        }

        if (context.getObjectIds() != null) {
            writer.println(linePrefix);
            context.getObjectIds().dump(writer, linePrefix);
        }

        if (context.getReparsePoints() != null) {
            writer.println(linePrefix);
            context.getReparsePoints().dump(writer, linePrefix);
        }

        if (context.getQuotas() != null) {
            writer.println(linePrefix);
            context.getQuotas().dump(writer, linePrefix);
        }

        writer.println(linePrefix);
        getDirectory(MasterFileTable.RootDirIndex).dump(writer, linePrefix);

        writer.println(linePrefix);
        writer.println(linePrefix + "FULL FILE LISTING");
        for (FileRecord record : context.getMft().getRecords()) {
            // Don't go through cache - these are short-lived, and this is
            // (just!) diagnostics
            File f = new File(context, record);
            f.dump(writer, linePrefix);

            for (NtfsStream stream : f.getAllStreams()) {
                if (stream.getAttributeType() == AttributeType.IndexRoot) {
                    try {
                        writer.println(linePrefix + "  INDEX (" + stream.getName() + ")");
                        f.getIndex(stream.getName()).dump(writer, linePrefix + "    ");
                    } catch (Exception e) {
                        writer.println(linePrefix + "!Exception: " + e);
                    }
                }
            }
        }

        writer.println(linePrefix);
        writer.println(linePrefix + "DIRECTORY TREE");
        writer.println(linePrefix + FS + " (5)");
        // 5 = Root Dir
        dumpDirectory(getDirectory(MasterFileTable.RootDirIndex), writer, linePrefix);
    }

    /**
     * Indicates whether the file is known by other names.
     *
     * @param path The file to inspect.
     * @return {@code true} if the file has other names, else {@code false} .
     */
    @Override public boolean hasHardLinks(String path) {
        return getHardLinkCount(path) > 1;
    }

    /**
     * Gets the security descriptor associated with the file or directory.
     *
     * @param path The file or directory to inspect.
     * @return The security descriptor.
     */
    @Override public RawSecurityDescriptor getSecurity(String path) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }
            File file = getFile(dirEntry.getReference());
            return doGetSecurity(file);
        }
    }

    /**
     * Sets the security descriptor associated with the file or directory.
     *
     * @param path The file or directory to change.
     * @param securityDescriptor The new security descriptor.
     */
    @Override public void setSecurity(String path, RawSecurityDescriptor securityDescriptor) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }
            File file = getFile(dirEntry.getReference());
            doSetSecurity(file, securityDescriptor);

            // Update the directory entry used to open the file
            dirEntry.updateFrom(file);
        }
    }

    /**
     * Sets the reparse point data on a file or directory.
     *
     * @param path The file to set the reparse point on.
     * @param reparsePoint The new reparse point.
     */
    @Override public void setReparsePoint(String path, ReparsePoint reparsePoint) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }
            File file = getFile(dirEntry.getReference());

            NtfsStream stream = file.getStream(AttributeType.ReparsePoint, null);
            if (stream != null) {
                // If there's an existing reparse point, unhook it.
                try (Stream contentStream = stream.open(FileAccess.Read)) {
                    byte[] oldRpBuffer = StreamUtilities.readExact(contentStream, (int) contentStream.getLength());
                    ReparsePointRecord rp = new ReparsePointRecord();
                    rp.readFrom(oldRpBuffer, 0);
                    context.getReparsePoints().remove(rp.tag, dirEntry.getReference());
                }
            } else {
                stream = file.createStream(AttributeType.ReparsePoint, null);
            }

            // Set the new content
            ReparsePointRecord newRp = new ReparsePointRecord();
            newRp.tag = reparsePoint.getTag();
            newRp.content = reparsePoint.getContent();

            byte[] contentBuffer = new byte[newRp.size()];
            newRp.writeTo(contentBuffer, 0);
            try (Stream contentStream = stream.open(FileAccess.ReadWrite)) {
                contentStream.write(contentBuffer, 0, contentBuffer.length);
                contentStream.setLength(contentBuffer.length);
            }

            // Update the standard information attribute - so it reflects the
            // actual file state
            NtfsStream stdInfoStream = file.getStream(AttributeType.StandardInformation, null);
            StandardInformation si = stdInfoStream.getContent(StandardInformation.class);
            si.fileAttributeFlags.add(FileAttributeFlags.ReparsePoint);
            stdInfoStream.setContent(si);

            // Update the directory entry used to open the file, so it's
            // accurate
            dirEntry.getDetails().eaSizeOrReparsePointTag = newRp.tag;
            dirEntry.updateFrom(file);

            // Write attribute changes back to the Master File Table
            file.updateRecordInMft();

            // Add the reparse point to the index
            context.getReparsePoints().add(newRp.tag, dirEntry.getReference());
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets the reparse point data associated with a file or directory.
     *
     * @param path The file to query.
     * @return The reparse point information.
     */
    @Override public ReparsePoint getReparsePoint(String path) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }
            File file = getFile(dirEntry.getReference());

            NtfsStream stream = file.getStream(AttributeType.ReparsePoint, null);
            if (stream != null) {
                ReparsePointRecord rp = new ReparsePointRecord();
                try (Stream contentStream = stream.open(FileAccess.Read)) {
                    byte[] buffer = StreamUtilities.readExact(contentStream, (int) contentStream.getLength());
                    rp.readFrom(buffer, 0);
                    return new ReparsePoint(rp.tag, rp.content);
                }
            }

            return null;
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Removes a reparse point from a file or directory, without deleting the
     * file or directory.
     *
     * @param path The path to the file or directory to remove the reparse point
     *            from.
     */
    @Override public void removeReparsePoint(String path) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }
            File file = getFile(dirEntry.getReference());
            removeReparsePoint(file);

            // Update the directory entry used to open the file, so it's
            // accurate
            dirEntry.updateFrom(file);

            // Write attribute changes back to the Master File Table
            file.updateRecordInMft();
        }
    }

    /**
     * Gets the short name for a given path.
     *
     * This method only gets the short name for the final part of the path, to
     * convert a complete path, call this method repeatedly, once for each path
     * segment. If there is no short name for the given path, {@code null} is
     * returned.
     *
     * @param path The path to convert.
     * @return The short name.
     */
    @Override public String getShortName(String path) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            String parentPath = Utilities.getDirectoryFromPath(path);
            DirectoryEntry parentEntry = getDirectoryEntry(parentPath);
            if (parentEntry == null || !parentEntry.getDetails().getFileAttributes().contains(FileAttributes.Directory)) {
                throw new FileNotFoundException("Parent directory not found");
            }

            Directory dir = getDirectory(parentEntry.getReference());
            if (dir == null) {
                throw new FileNotFoundException("Parent directory not found");
            }

            DirectoryEntry givenEntry = dir.getEntryByName(Utilities.getFileFromPath(path));
            if (givenEntry == null) {
                throw new FileNotFoundException("Path not found " + path);
            }

            if (givenEntry.getDetails().fileNameNamespace == FileNameNamespace.Dos) {
                return givenEntry.getDetails().fileName;
            }
            if (givenEntry.getDetails().fileNameNamespace == FileNameNamespace.Win32) {
                File file = getFile(givenEntry.getReference());

                for (NtfsStream stream : file.getStreams(AttributeType.FileName, null)) {
                    FileNameRecord fnr = stream.getContent(FileNameRecord.class);
                    if (fnr.parentDirectory.equals(givenEntry.getDetails().parentDirectory) &&
                        fnr.fileNameNamespace == FileNameNamespace.Dos) {
                        return fnr.fileName;
                    }
                }
            }

            return null;
        }
    }

    /**
     * Sets the short name for a given file or directory.
     *
     * @param path The full path to the file or directory to change.
     * @param shortName The shortName, which should not include a path.
     */
    @Override public void setShortName(String path, String shortName) {
        if (!Utilities.is8Dot3(shortName)) {
            throw new IllegalArgumentException("Short name is not a valid 8.3 file name");
        }

        try (NtfsTransaction ignored = new NtfsTransaction()) {
            String parentPath = Utilities.getDirectoryFromPath(path);
            DirectoryEntry parentEntry = getDirectoryEntry(parentPath);
            if (parentEntry == null || !parentEntry.getDetails().getFileAttributes().contains(FileAttributes.Directory)) {
                throw new FileNotFoundException("Parent directory not found");
            }

            Directory dir = getDirectory(parentEntry.getReference());
            if (dir == null) {
                throw new FileNotFoundException("Parent directory not found");
            }

            DirectoryEntry givenEntry = dir.getEntryByName(Utilities.getFileFromPath(path));
            if (givenEntry == null) {
                throw new FileNotFoundException("Path not found " + path);
            }

            FileNameNamespace givenNamespace = givenEntry.getDetails().fileNameNamespace;
            File file = getFile(givenEntry.getReference());
            if (givenNamespace == FileNameNamespace.Posix && file.getHasWin32OrDosName()) {
                throw new UnsupportedOperationException("Cannot set a short name on hard links");
            }

            // Convert Posix/Win32AndDos to just Win32
            if (givenEntry.getDetails().fileNameNamespace != FileNameNamespace.Win32) {
                dir.removeEntry(givenEntry);
                dir.addEntry(file, givenEntry.getDetails().fileName, FileNameNamespace.Win32);
            }

            // Remove any existing Dos names, and set the new one
            List<NtfsStream> nameStreams = new ArrayList<>(file.getStreams(AttributeType.FileName, null));
            for (NtfsStream stream : nameStreams) {
                FileNameRecord fnr = stream.getContent(FileNameRecord.class);
                if (fnr.parentDirectory.equals(givenEntry.getDetails().parentDirectory) &&
                    fnr.fileNameNamespace == FileNameNamespace.Dos) {
                    DirectoryEntry oldEntry = dir.getEntryByName(fnr.fileName);
                    dir.removeEntry(oldEntry);
                }
            }

            dir.addEntry(file, shortName, FileNameNamespace.Dos);

            parentEntry.updateFrom(dir);
        }
    }

    /**
     * Gets the standard file information for a file.
     *
     * @param path The full path to the file or directory to query.
     * @return The standard file information.
     */
    @Override public WindowsFileInformation getFileStandardInformation(String path) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            File file = getFile(dirEntry.getReference());
            StandardInformation si = file.getStandardInformation();

            WindowsFileInformation wfi = new WindowsFileInformation();
            wfi.setCreationTime(si.creationTime);
            wfi.setLastAccessTime(si.lastAccessTime);
            wfi.setChangeTime(si.mftChangedTime);
            wfi.setLastWriteTime(si.modificationTime);
            wfi.setFileAttributes(StandardInformation.convertFlags(si.fileAttributeFlags, file.isDirectory()));
            return wfi;
        }
    }

    /**
     * Sets the standard file information for a file.
     *
     * @param path The full path to the file or directory to query.
     * @param info The standard file information.
     */
    @Override public void setFileStandardInformation(String path, WindowsFileInformation info) {
        try (Closeable ntfs = new NtfsTransaction()) {
            updateStandardInformation(path, si -> {
                si.creationTime = info.getCreationTime();
                si.lastAccessTime = info.getLastAccessTime();
                si.mftChangedTime = info.getChangeTime();
                si.modificationTime = info.getLastWriteTime();
                si.fileAttributeFlags = StandardInformation.setFileAttributes(info.getFileAttributes(), si.fileAttributeFlags);
            });
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets the file id for a given path.
     *
     * @param path The path to get the id of.
     * @return The file id. The returned file id includes the MFT index of the
     *         primary file record for the file. The file id can be used to
     *         determine if two paths refer to the same actual file. The MFT
     *         index is held in the lower 48 bits of the id.
     */
    @Override public long getFileId(String path) {
        try (Closeable ntfs = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }
            return dirEntry.getReference().getValue();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
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
        DirectoryEntry dirEntry = getDirectoryEntry(path);
        if (dirEntry == null) {
            throw new FileNotFoundException("File not found " + path);
        }

        File file = getFile(dirEntry.getReference());

        List<String> names = new ArrayList<>();
        for (NtfsStream attr : file.getAllStreams()) {
            if (attr.getAttributeType() == AttributeType.Data && attr.getName() != null && !attr.getName().isEmpty()) {
                names.add(attr.getName());
            }
        }

        return names;
    }

    /**
     * Initializes a new NTFS file system.
     *
     * @param stream The stream to write the new file system to.
     * @param label The label for the new file system.
     * @param diskGeometry The disk geometry of the disk containing the new file
     *            system.
     * @param firstSector The first sector of the new file system on the disk.
     * @param sectorCount The number of sectors allocated to the new file system
     *            on the disk.
     * @return The newly-initialized file system.
     */
    public static NtfsFileSystem format(Stream stream,
                                        String label,
                                        Geometry diskGeometry,
                                        long firstSector,
                                        long sectorCount) {
        NtfsFormatter formatter = new NtfsFormatter();
        formatter.setLabel(label);
        formatter.setDiskGeometry(diskGeometry);
        formatter.setFirstSector(firstSector);
        formatter.setSectorCount(sectorCount);
        return formatter.format(stream);
    }

    /**
     * Initializes a new NTFS file system.
     *
     * @param stream The stream to write the new file system to.
     * @param label The label for the new file system.
     * @param diskGeometry The disk geometry of the disk containing the new file
     *            system.
     * @param firstSector The first sector of the new file system on the disk.
     * @param sectorCount The number of sectors allocated to the new file system
     *            on the disk.
     * @param bootCode The Operating System's boot code.
     * @return The newly-initialized file system.
     */
    public static NtfsFileSystem format(Stream stream,
                                        String label,
                                        Geometry diskGeometry,
                                        long firstSector,
                                        long sectorCount,
                                        byte[] bootCode) {
        NtfsFormatter formatter = new NtfsFormatter();
        formatter.setLabel(label);
        formatter.setDiskGeometry(diskGeometry);
        formatter.setFirstSector(firstSector);
        formatter.setSectorCount(sectorCount);
        formatter.setBootCode(bootCode);
        return formatter.format(stream);
    }

    /**
     * Initializes a new NTFS file system.
     *
     * @param stream The stream to write the new file system to.
     * @param label The label for the new file system.
     * @param diskGeometry The disk geometry of the disk containing the new file
     *            system.
     * @param firstSector The first sector of the new file system on the disk.
     * @param sectorCount The number of sectors allocated to the new file system
     *            on the disk.
     * @param options The formatting options.
     * @return The newly-initialized file system.
     */
    public static NtfsFileSystem format(Stream stream,
                                        String label,
                                        Geometry diskGeometry,
                                        long firstSector,
                                        long sectorCount,
                                        NtfsFormatOptions options) {
        NtfsFormatter formatter = new NtfsFormatter();
        formatter.setLabel(label);
        formatter.setDiskGeometry(diskGeometry);
        formatter.setFirstSector(firstSector);
        formatter.setSectorCount(sectorCount);
        formatter.setBootCode(options.getBootCode());
        formatter.setComputerAccount(options.getComputerAccount());
        return formatter.format(stream);
    }

    /**
     * Initializes a new NTFS file system.
     *
     * @param volume The volume to format.
     * @param label The label for the new file system.
     * @return The newly-initialized file system.
     */
    public static NtfsFileSystem format(VolumeInfo volume, String label) {
        NtfsFormatter formatter = new NtfsFormatter();
        formatter.setLabel(label);
        formatter.setDiskGeometry(volume.getBiosGeometry() != null ? volume.getBiosGeometry() : Geometry.getNull());
        formatter.setFirstSector(volume.getPhysicalStartSector());
        formatter.setSectorCount(volume.getLength() / Sizes.Sector);
        return formatter.format(volume.open());
    }

    /**
     * Initializes a new NTFS file system.
     *
     * @param volume The volume to format.
     * @param label The label for the new file system.
     * @param bootCode The Operating System's boot code.
     * @return The newly-initialized file system.
     */
    public static NtfsFileSystem format(VolumeInfo volume, String label, byte[] bootCode) {
        NtfsFormatter formatter = new NtfsFormatter();
        formatter.setLabel(label);
        formatter.setDiskGeometry(volume.getBiosGeometry() != null ? volume.getBiosGeometry() : Geometry.getNull());
        formatter.setFirstSector(volume.getPhysicalStartSector());
        formatter.setSectorCount(volume.getLength() / Sizes.Sector);
        formatter.setBootCode(bootCode);
        return formatter.format(volume.open());
    }

    /**
     * Initializes a new NTFS file system.
     *
     * @param volume The volume to format.
     * @param label The label for the new file system.
     * @param options The formatting options.
     * @return The newly-initialized file system.
     */
    public static NtfsFileSystem format(VolumeInfo volume, String label, NtfsFormatOptions options) {
        NtfsFormatter formatter = new NtfsFormatter();
        formatter.setLabel(label);
        formatter.setDiskGeometry(volume.getBiosGeometry() != null ? volume.getBiosGeometry() : Geometry.getNull());
        formatter.setFirstSector(volume.getPhysicalStartSector());
        formatter.setSectorCount(volume.getLength() / Sizes.Sector);
        formatter.setBootCode(options.getBootCode());
        formatter.setComputerAccount(options.getComputerAccount());
        return formatter.format(volume.open());
    }

    /**
     * Detects if a stream contains an NTFS file system.
     *
     * @param stream The stream to inspect.
     * @return {@code true} if NTFS is detected, else {@code false} .
     */
    public static boolean detect(Stream stream) {
        if (stream.getLength() < 512) {
            return false;
        }

        stream.position(0);
        byte[] bytes = StreamUtilities.readExact(stream, 512);
        BiosParameterBlock bpb = BiosParameterBlock.fromBytes(bytes, 0);

        return bpb.isValid(stream.getLength());
    }

    /**
     * Gets the Master File Table for this file system.
     *
     * Use the returned object to explore the internals of the file system -
     * most people will never need to use this.
     *
     * @return The Master File Table.
     */
    public discUtils.ntfs.internals.MasterFileTable getMasterFileTable() {
        return new discUtils.ntfs.internals.MasterFileTable(context, context.getMft());
    }

    /**
     * Creates a directory.
     *
     * @param path The path of the new directory.
     * @param options Options controlling attributes of the new Director, or
     *            {@code null} for defaults.
     */
    public void createDirectory(String path, NewFileOptions options) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            String[] pathElements = Arrays.stream(path.split(StringUtilities.escapeForRegex(FS)))
                    .filter(e -> !e.isEmpty())
                    .toArray(String[]::new);

            Directory focusDir = getDirectory(MasterFileTable.RootDirIndex);
            DirectoryEntry focusDirEntry = focusDir.getDirectoryEntry();

            for (String pathElement : pathElements) {
                DirectoryEntry childDirEntry = focusDir.getEntryByName(pathElement);
                if (childDirEntry == null) {
                    EnumSet<FileAttributeFlags> newDirAttrs = focusDir.getStandardInformation().fileAttributeFlags;
                    if (options != null && options.getCompressed() != null) {
                        if (options.getCompressed()) {
                            newDirAttrs.add(FileAttributeFlags.Compressed);
                        } else {
                            newDirAttrs.remove(FileAttributeFlags.Compressed);
                        }
                    }

                    Directory childDir = Directory.createNew(context, newDirAttrs);
                    try {
                        childDirEntry = addFileToDirectory(childDir, focusDir, pathElement, options);
                        RawSecurityDescriptor parentSd = doGetSecurity(focusDir);
                        RawSecurityDescriptor newSd;
                        if (options != null && options.getSecurityDescriptor() != null) {
                            newSd = options.getSecurityDescriptor();
                        } else {
                            newSd = SecurityDescriptor.calcNewObjectDescriptor(parentSd, false);
                        }

                        doSetSecurity(childDir, newSd);
                        childDirEntry.updateFrom(childDir);

                        // Update the directory entry by which we found the
                        // directory we've just modified
                        focusDirEntry.updateFrom(focusDir);

                        focusDir = childDir;
                    } finally {
                        if (childDir.getHardLinkCount() == 0) {
                            childDir.delete();
                        }
                    }
                } else {
                    focusDir = getDirectory(childDirEntry.getReference());
                }

                focusDirEntry = childDirEntry;
            }
        }
    }

    /**
     * Opens the specified file.
     *
     * @param path The full path of the file to open.
     * @param mode The file mode for the created stream.
     * @param access The access permissions for the returned stream.
     * @param options Options controlling attributes of a new file, or
     *            {@code null} for defaults (ignored if file exists).
     * @return The new stream.
     */
    public SparseStream openFile(String path, FileMode mode, FileAccess access, NewFileOptions options) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            String[] attributeName = new String[1];
            AttributeType[] attributeType = new AttributeType[1];
            String dirEntryPath = parsePath(path, attributeName, attributeType);

            DirectoryEntry entry = getDirectoryEntry(dirEntryPath);
            if (entry == null) {
                if (mode == FileMode.Open) {
                    throw new FileNotFoundException("No such file " + path);
                }

                entry = createNewFile(dirEntryPath, options);
            } else if (mode == FileMode.CreateNew) {
                throw new dotnet4j.io.IOException("File already exists");
            }

            if (entry.getDetails().getFileAttributes().contains(FileAttributes.Directory) &&
                attributeType[0] == AttributeType.Data) {
                throw new dotnet4j.io.IOException("Attempt to open directory as a file");
            }
            File file = getFile(entry.getReference());
            NtfsStream ntfsStream = file.getStream(attributeType[0], attributeName[0]);

            if (ntfsStream == null) {
                if (mode == FileMode.Create || mode == FileMode.OpenOrCreate) {
                    ntfsStream = file.createStream(attributeType[0], attributeName[0]);
                } else {
                    throw new FileNotFoundException("No such attribute on file " + path);
                }
            }

            SparseStream stream = new NtfsFileStream(this, entry, attributeType[0], attributeName[0], access);

            if (mode == FileMode.Create || mode == FileMode.Truncate) {
                stream.setLength(0);
            }

            return stream;
        }
    }

    /**
     * Opens an existing file stream.
     *
     * @param file The file containing the stream.
     * @param type The type of the stream.
     * @param name The name of the stream.
     * @param access The desired access to the stream.
     * @return A stream that can be used to access the file stream.
     */
    public SparseStream openRawStream(String file, AttributeType type, String name, FileAccess access) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry entry = getDirectoryEntry(file);
            if (entry == null) {
                throw new FileNotFoundException("No such file " + file);
            }

            File fileObj = getFile(entry.getReference());
            return fileObj.openStream(type, name, access);
        }
    }

    /**
     * Creates an NTFS hard link to an existing file.
     *
     * @param sourceName An existing name of the file.
     * @param destinationName The name of the new hard link to the file.
     */
    public void createHardLink(String sourceName, String destinationName) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry sourceDirEntry = getDirectoryEntry(sourceName);
            if (sourceDirEntry == null) {
                throw new FileNotFoundException("Source file not found " + sourceName);
            }

            String destinationDirName = Utilities.getDirectoryFromPath(destinationName);
            DirectoryEntry destinationDirSelfEntry = getDirectoryEntry(destinationDirName);
            if (destinationDirSelfEntry == null ||
                !destinationDirSelfEntry.getDetails().getFileAttributes().contains(FileAttributes.Directory)) {
                throw new FileNotFoundException("Destination directory not found " + destinationDirName);
            }

            Directory destinationDir = getDirectory(destinationDirSelfEntry.getReference());
            if (destinationDir == null) {
                throw new FileNotFoundException("Destination directory not found " + destinationDirName);
            }

            DirectoryEntry destinationDirEntry = getDirectoryEntry(destinationDir, Utilities.getFileFromPath(destinationName));
            if (destinationDirEntry != null) {
                throw new IOException("A file with this name already exists: " + destinationName);
            }

            File file = getFile(sourceDirEntry.getReference());
            destinationDir.addEntry(file, Utilities.getFileFromPath(destinationName), FileNameNamespace.Posix);
            destinationDirSelfEntry.updateFrom(destinationDir);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets the number of hard links to a given file or directory.
     *
     * @param path The path of the file or directory.
     * @return The number of hard links.All files have at least one hard link.
     */
    public int getHardLinkCount(String path) {
        try (NtfsTransaction c = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            File file = getFile(dirEntry.getReference());
            if (!context.getOptions().hideDosFileNames()) {
                return file.getHardLinkCount();
            }

            int numHardLinks = 0;
            for (NtfsStream fnStream : file.getStreams(AttributeType.FileName, null)) {
                FileNameRecord fnr = fnStream.getContent(FileNameRecord.class);
                if (fnr.fileNameNamespace != FileNameNamespace.Dos) {
                    ++numHardLinks;
                }
            }
            return numHardLinks;
        }
    }

    /**
     * Updates the BIOS Parameter block (BPB) of the file system to reflect a
     * new disk geometry.
     *
     * @param geometry The disk's new BIOS geometry.Having an accurate geometry
     *            in the BPB is essential for booting some Operating Systems
     *            (e.g. Windows XP).
     */
    public void updateBiosGeometry(Geometry geometry) {
        context.getBiosParameterBlock().sectorsPerTrack = (short) geometry.getSectorsPerTrack();
        context.getBiosParameterBlock().numHeads = (short) geometry.getHeadsPerCylinder();
        context.getRawStream().position(0);
        byte[] bpbSector = StreamUtilities.readExact(context.getRawStream(), 512);
        context.getBiosParameterBlock().toBytes(bpbSector, 0);
        context.getRawStream().position(0);
        context.getRawStream().write(bpbSector, 0, bpbSector.length);
    }

    DirectoryEntry getDirectoryEntry(String path) {
        return getDirectoryEntry(getDirectory(MasterFileTable.RootDirIndex), path);
    }

    /**
     * Disposes of this instance.
     */
    @Override public void close() throws IOException {
        if (context != null && context.getMft() != null) {
            context.getMft().close();
            context.setMft(null);
        }

        BlockCompressor _disposableCompressor = context.getOptions().getCompressor();
        if (_disposableCompressor instanceof Closeable disposableCompressor) {
            disposableCompressor.close();
            context.getOptions().setCompressor(null);
        }

        super.close();
    }

    private static void removeFileFromDirectory(Directory dir, File file, String name) {
        List<String> aliases = new ArrayList<>();
        DirectoryEntry dirEntry = dir.getEntryByName(name);
        if (dirEntry.getDetails().fileNameNamespace == FileNameNamespace.Dos ||
            dirEntry.getDetails().fileNameNamespace == FileNameNamespace.Win32) {
            for (NtfsStream fnStream : file.getStreams(AttributeType.FileName, null)) {
                FileNameRecord fnr = fnStream.getContent(FileNameRecord.class);
                if ((fnr.fileNameNamespace == FileNameNamespace.Win32 || fnr.fileNameNamespace == FileNameNamespace.Dos) &&
                    fnr.parentDirectory.getValue() == dir.getMftReference().getValue()) {
                    aliases.add(fnr.fileName);
                }
            }
        } else {
            aliases.add(name);
        }
        for (String alias : aliases) {
            DirectoryEntry de = dir.getEntryByName(alias);
            dir.removeEntry(de);
        }
    }

    /**
     * @param plainPath {@cs out}
     * @param attributeName {@cs out}
     */
    private static void splitPath(String path, String[] plainPath, String[] attributeName) {
        plainPath[0] = path;
        String fileName = Utilities.getFileFromPath(path);
        attributeName[0] = null;
        int streamSepPos = fileName.indexOf(':');
        if (streamSepPos >= 0) {
            attributeName[0] = fileName.substring(streamSepPos + 1);
            plainPath[0] = plainPath[0].substring(0, path.length() - (fileName.length() - streamSepPos));
        }
    }

    private static void updateStandardInformation(DirectoryEntry dirEntry, File file, StandardInformationModifier modifier) {
        // Update the standard information attribute - so it reflects the actual
        // file state
        NtfsStream stream = file.getStream(AttributeType.StandardInformation, null);
        StandardInformation si = stream.getContent(StandardInformation.class);
        modifier.invoke(si);
        stream.setContent(si);
        // Update the directory entry used to open the file, so it's accurate
        dirEntry.updateFrom(file);
        // Write attribute changes back to the Master File Table
        file.updateRecordInMft();
    }

    private DirectoryEntry createNewFile(String path, NewFileOptions options) {
        DirectoryEntry result;
        DirectoryEntry parentDirEntry = getDirectoryEntry(Utilities.getDirectoryFromPath(path));
        Directory parentDir = getDirectory(parentDirEntry.getReference());
        EnumSet<FileAttributeFlags> newFileAttrs = parentDir.getStandardInformation().fileAttributeFlags;
        if (options != null && options.getCompressed() != null) {
            if (options.getCompressed()) {
                newFileAttrs.add(FileAttributeFlags.Compressed);
            } else {
                newFileAttrs.remove(FileAttributeFlags.Compressed);
            }
        }

        File file = File.createNew(context, newFileAttrs);
        try {
            result = addFileToDirectory(file, parentDir, Utilities.getFileFromPath(path), options);
            RawSecurityDescriptor parentSd = doGetSecurity(parentDir);
            RawSecurityDescriptor newSd;
            if (options != null && options.getSecurityDescriptor() != null) {
                newSd = options.getSecurityDescriptor();
            } else {
                newSd = SecurityDescriptor.calcNewObjectDescriptor(parentSd, false);
            }
            doSetSecurity(file, newSd);
            result.updateFrom(file);
            parentDirEntry.updateFrom(parentDir);
        } finally {
            if (file.getHardLinkCount() == 0) {
                file.delete();
            }
        }
        return result;
    }

    private DirectoryEntry getDirectoryEntry(Directory dir, String path) {
        String[] pathElements = Arrays.stream(path.split(StringUtilities.escapeForRegex(FS)))
                .filter(e -> !e.isEmpty())
                .toArray(String[]::new);
        return getDirectoryEntry(dir, pathElements, 0);
    }

    private void doSearch(List<String> results, String path, Pattern regex, boolean subFolders, boolean dirs, boolean files) {
        DirectoryEntry parentDirEntry = getDirectoryEntry(path);
        if (parentDirEntry == null) {
            throw new FileNotFoundException(String.format("The directory '%s' was not found", path));
        }

        Directory parentDir = getDirectory(parentDirEntry.getReference());
        if (parentDir == null) {
            throw new FileNotFoundException(String.format("The directory '%s' was not found", path));
        }

        for (DirectoryEntry de : parentDir.getAllEntries(true)) {
            boolean isDir = de.getDetails().getFileAttributes().contains(FileAttributes.Directory);
            if ((isDir && dirs) || (!isDir && files)) {
                if (regex.matcher(de.getSearchName()).find()) {
                    results.add(Utilities.combinePaths(path, de.getDetails().fileName));
                }
            }

            if (subFolders && isDir) {
                doSearch(results, Utilities.combinePaths(path, de.getDetails().fileName), regex, subFolders, dirs, files);
            }
        }
    }

    private DirectoryEntry getDirectoryEntry(Directory dir, String[] pathEntries, int pathOffset) {
        DirectoryEntry entry;

        if (pathEntries.length == 0) {
            return dir.getDirectoryEntry();
        }
        entry = dir.getEntryByName(pathEntries[pathOffset]);
        if (entry != null) {
            if (pathOffset == pathEntries.length - 1) {
                return entry;
            }
            if (entry.getDetails().getFileAttributes().contains(FileAttributes.Directory)) {
                return getDirectoryEntry(getDirectory(entry.getReference()), pathEntries, pathOffset + 1);
            }
            throw new dotnet4j.io.IOException(String.format("%s is a file, not a directory", pathEntries[pathOffset]));
        }
        return null;
    }

    private DirectoryEntry addFileToDirectory(File file, Directory dir, String name, NewFileOptions options) {
        DirectoryEntry entry;
        boolean createShortNames;
        if (options != null && options.getCreateShortNames() != null) {
            createShortNames = options.getCreateShortNames();
        } else {
            createShortNames = getCreateShortNames();
        }
        if (createShortNames) {
            if (Utilities.is8Dot3(name.toUpperCase())) {
                entry = dir.addEntry(file, name, FileNameNamespace.Win32AndDos);
            } else {
                entry = dir.addEntry(file, name, FileNameNamespace.Win32);
                dir.addEntry(file, dir.createShortName(name), FileNameNamespace.Dos);
            }
        } else {
            entry = dir.addEntry(file, name, FileNameNamespace.Posix);
        }
        return entry;
    }

    private void removeReparsePoint(File file) {
        NtfsStream stream = file.getStream(AttributeType.ReparsePoint, null);
        if (stream != null) {
            ReparsePointRecord rp = new ReparsePointRecord();

            try (Stream contentStream = stream.open(FileAccess.Read)) {
                byte[] buffer = StreamUtilities.readExact(contentStream, (int) contentStream.getLength());
                rp.readFrom(buffer, 0);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            file.removeStream(stream);
            // Update the standard information attribute - so it reflects the
            // actual file state
            NtfsStream stdInfoStream = file.getStream(AttributeType.StandardInformation, null);
            StandardInformation si = stdInfoStream.getContent(StandardInformation.class);
            si.fileAttributeFlags.remove(FileAttributeFlags.ReparsePoint);
            stdInfoStream.setContent(si);
            // Remove the reparse point from the index
            context.getReparsePoints().remove(rp.tag, file.getMftReference());
        }
    }

    private RawSecurityDescriptor doGetSecurity(File file) {
        NtfsStream legacyStream = file.getStream(AttributeType.SecurityDescriptor, null);
        if (legacyStream != null) {
            return legacyStream.getContent(SecurityDescriptor.class).getDescriptor();
        }

        StandardInformation si = file.getStandardInformation();
        return context.getSecurityDescriptors().getDescriptorById(si.securityId);
    }

    private void doSetSecurity(File file, RawSecurityDescriptor securityDescriptor) {
        NtfsStream legacyStream = file.getStream(AttributeType.SecurityDescriptor, null);
        if (legacyStream != null) {
            SecurityDescriptor sd = new SecurityDescriptor();
            sd.setDescriptor(securityDescriptor);
            legacyStream.setContent(sd);
        } else {
            int id = context.getSecurityDescriptors().addDescriptor(securityDescriptor);
            // Update the standard information attribute - so it reflects the
            // actual file state
            NtfsStream stream = file.getStream(AttributeType.StandardInformation, null);
            StandardInformation si = stream.getContent(StandardInformation.class);
            si.securityId = id;
            stream.setContent(si);
            // Write attribute changes back to the Master File Table
            file.updateRecordInMft();
        }
    }

    private void dumpDirectory(Directory dir, PrintWriter writer, String indent) {
        for (DirectoryEntry dirEntry : dir.getAllEntries(true)) {
            File file = getFile(dirEntry.getReference());
            Directory asDir = file instanceof Directory ? (Directory) file : null;
            writer.println(indent + "+-" + file + " (" + file.getIndexInMft() + ")");
            // Recurse - but avoid infinite recursion via the root dir...
            if (asDir != null && file.getIndexInMft() != 5) {
                dumpDirectory(asDir, writer, indent + "| ");
            }
        }
    }

    private void updateStandardInformation(String path, StandardInformationModifier modifier) {
        DirectoryEntry dirEntry = getDirectoryEntry(path);
        if (dirEntry == null) {
            throw new FileNotFoundException("File not found " + path);
        }

        File file = getFile(dirEntry.getReference());
        updateStandardInformation(dirEntry, file, modifier);
    }

    private String parsePath(String path, String[] attributeName, AttributeType[] attributeType) {
        String fileName = Utilities.getFileFromPath(path);
        attributeName[0] = null;
        attributeType[0] = AttributeType.Data;

        String[] fileNameElements = fileName.split(":", 3);
        fileName = fileNameElements[0];

        if (fileNameElements.length > 1) {
            attributeName[0] = fileNameElements[1];
            if (attributeName[0] == null || attributeName[0].isEmpty()) {
                attributeName[0] = null;
            }
        }

        if (fileNameElements.length > 2) {
            String typeName = fileNameElements[2];
            AttributeDefinitionRecord typeDefn = context.getAttributeDefinitions().lookup(typeName);
            if (typeDefn == null) {
                throw new FileNotFoundException(String.format("No such attribute type '%s'", typeName));
            }

            attributeType[0] = typeDefn.type;
        }

        try {
            String dirName = Utilities.getDirectoryFromPath(path);
            return Utilities.combinePaths(dirName, fileName);
        } catch (IllegalArgumentException e) {
            throw new dotnet4j.io.IOException("Invalid path: " + path, e);
        }
    }

    @FunctionalInterface
    private interface StandardInformationModifier {

        void invoke(StandardInformation si);
    }

    Directory getDirectory(long index) {
        return (Directory) getFile(index);
    }

    Directory getDirectory(FileRecordReference fileReference) {
        return (Directory) getFile(fileReference);
    }

    File getFile(FileRecordReference fileReference) {
        FileRecord record = context.getMft().getRecord(fileReference);
        if (record == null) {
            return null;
        }

        // Don't create file objects for file record segments that are part of
        // another logical file.
        if (record.getBaseFile().getValue() != 0) {
            return null;
        }

        File file = fileCache.get(fileReference.getMftIndex());
        if (file != null && file.getMftReference().getSequenceNumber() != fileReference.getSequenceNumber()) {
            file = null;
        }

        if (file == null) {
            if (record.getFlags().contains(FileRecordFlags.IsDirectory)) {
                file = new Directory(context, record);
            } else {
                file = new File(context, record);
            }
            fileCache.put(fileReference.getMftIndex(), file);
        }

        return file;
    }

    File getFile(long index) {
        FileRecord record = context.getMft().getRecord(index, false);
        if (record == null) {
            return null;
        }

        // Don't create file objects for file record segments that are part of
        // another logical file.
        if (record.getBaseFile().getValue() != 0) {
            return null;
        }

        File file = fileCache.get(index);
        if (file != null && file.getMftReference().getSequenceNumber() != record.getSequenceNumber()) {
            file = null;
        }

        if (file == null) {
            if (record.getFlags().contains(FileRecordFlags.IsDirectory)) {
                file = new Directory(context, record);
            } else {
                file = new File(context, record);
            }

            fileCache.put(index, file);
        }

        return file;
    }

    File allocateFile(EnumSet<FileRecordFlags> flags) {
        File result;
        if (flags.contains(FileRecordFlags.IsDirectory)) {
            result = new Directory(context, context.getMft().allocateRecord(flags, false));
        } else {
            result = new File(context, context.getMft().allocateRecord(flags, false));
        }

        fileCache.put(result.getMftReference().getMftIndex(), result);
        return result;
    }

    void forgetFile(File file) {
        fileCache.remove((long) file.getIndexInMft());
    }

    /**
     * Size of the Filesystem in bytes
     */
    @Override public long getSize() {
        return getTotalClusters() * getClusterSize();
    }

    /**
     * Used space of the Filesystem in bytes
     */
    @Override public long getUsedSpace() {
        long usedCluster = 0;
        Bitmap bitmap = context.getClusterBitmap().getBitmap();
        long processed = 0L;
        while (processed < bitmap.getSize()) {
            byte[] buffer = new byte[(int) (4 * Sizes.OneKiB)];
            int count = bitmap.getBytes(processed, buffer, 0, buffer.length);
            usedCluster += BitCounter.count(buffer, 0, count);
            processed += count;
        }
        return usedCluster * getClusterSize();
    }

    /**
     * Available space of the Filesystem in bytes
     */
    @Override public long getAvailableSpace() {
        return getSize() - getUsedSpace();
    }
}
