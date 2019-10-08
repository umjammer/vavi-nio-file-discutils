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

package DiscUtils.Ntfs;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import DiscUtils.Core.ClusterMap;
import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.IClusterBasedFileSystem;
import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Core.IWindowsFileSystem;
import DiscUtils.Core.InvalidFileSystemException;
import DiscUtils.Core.ReparsePoint;
import DiscUtils.Core.VolumeInfo;
import DiscUtils.Core.WindowsFileInformation;
import DiscUtils.Core.Internal.ObjectCache;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Block.BlockCacheSettings;
import DiscUtils.Streams.Block.BlockCacheStream;
import DiscUtils.Streams.Util.BitCounter;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Range;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.FileNotFoundException;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * Class for accessing NTFS file systems.
 */
public final class NtfsFileSystem extends DiscFileSystem implements
                                  IClusterBasedFileSystem,
                                  IWindowsFileSystem,
                                  IDiagnosticTraceable {

    private static final Map<String, Object> NonSettableFileAttributes = new HashMap<String, Object>() {
        {
            put("Directory", true);
            put("Offline", true);
            put("ReparsePoint", true);
        }
    };

    private final NtfsContext _context;

    // Top-level file system structures
    // Working state
    private final ObjectCache<Long, File> _fileCache;

    private final VolumeInformation _volumeInfo;

    /**
     * Initializes a new instance of the NtfsFileSystem class.
     *
     * @param stream The stream containing the NTFS file system.
     */
    public NtfsFileSystem(Stream stream) {
        super(new NtfsOptions());
        _context = new NtfsContext();
        _context.setRawStream(stream);
        _context.setOptions(getNtfsOptions());
        _context.setGetFileByIndex(new GetFileByIndexFn() {
            public File invoke(long index) {
                return getFile(index);
            }

            public List<GetFileByIndexFn> getInvocationList() {
                List<GetFileByIndexFn> ret = new ArrayList<>();
                ret.add(this);
                return ret;
            }

        });
        _context.setGetFileByRef(new GetFileByRefFn() {
            public File invoke(FileRecordReference reference) {
                return getFile(reference);
            }

            public List<GetFileByRefFn> getInvocationList() {
                List<GetFileByRefFn> ret = new ArrayList<>();
                ret.add(this);
                return ret;
            }

        });
        _context.setGetDirectoryByRef(new GetDirectoryByRefFn() {
            public Directory invoke(FileRecordReference reference) {
                return getDirectory(reference);
            }

            public List<GetDirectoryByRefFn> getInvocationList() {
                List<GetDirectoryByRefFn> ret = new ArrayList<>();
                ret.add(this);
                return ret;
            }

        });
        _context.setGetDirectoryByIndex(new GetDirectoryByIndexFn() {
            public Directory invoke(long index) {
                return getDirectory(index);
            }

            public List<GetDirectoryByIndexFn> getInvocationList() {
                List<GetDirectoryByIndexFn> ret = new ArrayList<>();
                ret.add(this);
                return ret;
            }

        });
        _context.setAllocateFile(flags -> {
            return allocateFile(flags);
        });
        _context.setForgetFile(new ForgetFileFn() {
            public void invoke(File file) {
                forgetFile(file);
            }

            public List<ForgetFileFn> getInvocationList() {
                List<ForgetFileFn> ret = new ArrayList<>();
                ret.add(this);
                return ret;
            }

        });
        _context.setReadOnly(!stream.canWrite());
        _fileCache = new ObjectCache<>();
        stream.setPosition(0);
        byte[] bytes = StreamUtilities.readExact(stream, 512);
        _context.setBiosParameterBlock(BiosParameterBlock.fromBytes(bytes, 0));
        if (!_context.getBiosParameterBlock().isValid(stream.getLength())) {
            throw new InvalidFileSystemException("BIOS Parameter Block is invalid for an NTFS file system");
        }

        if (getNtfsOptions().getReadCacheEnabled()) {
            BlockCacheSettings cacheSettings = new BlockCacheSettings();
            cacheSettings.setBlockSize(_context.getBiosParameterBlock().getBytesPerCluster());
            _context.setRawStream(new BlockCacheStream(SparseStream.fromStream(stream, Ownership.None),
                                                       Ownership.None,
                                                       cacheSettings));
        }

        // Bootstrap the Master File Table
        _context.setMft(new MasterFileTable(_context));
        File mftFile = new File(_context, _context.getMft().getBootstrapRecord());
        _fileCache.set___idx(MasterFileTable.MftIndex, mftFile);
        _context.getMft().initialize(mftFile);
        // Get volume information (includes version number)
        File volumeInfoFile = getFile(MasterFileTable.VolumeIndex);
        _volumeInfo = volumeInfoFile.getStream(AttributeType.VolumeInformation, null).getContent();
        // Initialize access to the other well-known metadata files
        _context.setClusterBitmap(new ClusterBitmap(getFile(MasterFileTable.BitmapIndex)));
        _context.setAttributeDefinitions(new AttributeDefinitions(getFile(MasterFileTable.AttrDefIndex)));
        _context.setUpperCase(new UpperCase(getFile(MasterFileTable.UpCaseIndex)));
        if (_volumeInfo.getVersion() >= VolumeInformation.VersionW2k) {
            _context.setSecurityDescriptors(new SecurityDescriptors(getFile(MasterFileTable.SecureIndex)));
            _context.setObjectIds(new ObjectIds(getFile(getDirectoryEntry("$Extend\\$ObjId").getReference())));
            _context.setReparsePoints(new ReparsePoints(getFile(getDirectoryEntry("$Extend\\$Reparse").getReference())));
            _context.setQuotas(new Quotas(getFile(getDirectoryEntry("$Extend\\$Quota").getReference())));
        }

    }

    private boolean getCreateShortNames() {
        return _context.getOptions().getShortNameCreation() == ShortFileNameOption.Enabled ||
               (_context.getOptions().getShortNameCreation() == ShortFileNameOption.UseVolumeFlag &&
                !_volumeInfo.getFlags().contains(VolumeInformationFlags.DisableShortNameCreation));
    }

    /**
     * Gets the friendly name for the file system.
     */
    public String getFriendlyName() {
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
    public String getVolumeLabel() {
        File volumeFile = getFile(MasterFileTable.VolumeIndex);
        NtfsStream volNameStream = volumeFile.getStream(AttributeType.VolumeName, null);
        return VolumeName.class.cast(volNameStream.getContent()).getName();
    }

    /**
     * Indicates if the file system supports write operations.
     */
    // For now, we don't...
    public boolean canWrite() {
        return !_context.getReadOnly();
    }

    /**
     * Gets the size of each cluster (in bytes).
     */
    public long getClusterSize() {
        return _context.getBiosParameterBlock().getBytesPerCluster();
    }

    /**
     * Gets the total number of clusters managed by the file system.
     */
    public long getTotalClusters() {
        return MathUtilities.ceil(_context.getBiosParameterBlock().TotalSectors64,
                                  _context.getBiosParameterBlock().SectorsPerCluster);
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
    public void copyFile(String sourceFile, String destinationFile, boolean overwrite) {
        Closeable __newVar0 = new NtfsTransaction();
        try {
            {
                DirectoryEntry sourceParentDirEntry = getDirectoryEntry(Utilities.getDirectoryFromPath(sourceFile));
                if (sourceParentDirEntry == null || !sourceParentDirEntry.getIsDirectory()) {
                    throw new FileNotFoundException("No such file " + sourceFile);
                }

                Directory sourceParentDir = getDirectory(sourceParentDirEntry.getReference());
                DirectoryEntry sourceEntry = sourceParentDir.getEntryByName(Utilities.getFileFromPath(sourceFile));
                if (sourceEntry == null || sourceEntry.getIsDirectory()) {
                    throw new FileNotFoundException("No such file " + sourceFile);
                }

                File origFile = getFile(sourceEntry.getReference());
                DirectoryEntry destParentDirEntry = getDirectoryEntry(Utilities.getDirectoryFromPath(destinationFile));
                if (destParentDirEntry == null || !destParentDirEntry.getIsDirectory()) {
                    throw new FileNotFoundException("Destination directory not found " + destinationFile);
                }

                Directory destParentDir = getDirectory(destParentDirEntry.getReference());
                DirectoryEntry destDirEntry = destParentDir.getEntryByName(Utilities.getFileFromPath(destinationFile));
                if (destDirEntry != null && !destDirEntry.getIsDirectory()) {
                    if (overwrite) {
                        if (destDirEntry.getReference().getMftIndex() == sourceEntry.getReference().getMftIndex()) {
                            throw new moe.yo3explorer.dotnetio4j.IOException("Destination file already exists and is the source file");
                        }

                        File oldFile = getFile(destDirEntry.getReference());
                        destParentDir.removeEntry(destDirEntry);
                        if (oldFile.getHardLinkCount() == 0) {
                            oldFile.delete();
                        }

                    } else {
                        throw new moe.yo3explorer.dotnetio4j.IOException("Destination file already exists");
                    }
                }

                File newFile = File.createNew(_context, destParentDir.getStandardInformation()._FileAttributes);
                for (NtfsStream origStream : origFile.getAllStreams()) {
                    NtfsStream newStream = newFile.getStream(origStream.getAttributeType(), origStream.getName());
                    switch (origStream.getAttributeType()) {
                    case Data:
                        if (newStream == null) {
                            newStream = newFile.createStream(origStream.getAttributeType(), origStream.getName());
                        }

                        SparseStream s = origStream.open(FileAccess.Read);
                        try {
                            SparseStream d = newStream.open(FileAccess.Write);
                            try {
                                {
                                    byte[] buffer = new byte[64 * (int) Sizes.OneKiB];
                                    int numRead;
                                    do {
                                        numRead = s.read(buffer, 0, buffer.length);
                                        d.write(buffer, 0, numRead);
                                    } while (numRead != 0);
                                }
                            } finally {
                                if (d != null)
                                    try {
                                        d.close();
                                    } catch (IOException e) {
                                        throw new moe.yo3explorer.dotnetio4j.IOException(e);
                                    }
                            }
                        } finally {
                            if (s != null)
                                try {
                                    s.close();
                                } catch (IOException e) {
                                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                                }
                        }
                        break;
                    case StandardInformation:
                        StandardInformation newSi = origStream.getContent();
                        newStream.setContent(newSi);
                        break;
                    }
                }
                addFileToDirectory(newFile, destParentDir, Utilities.getFileFromPath(destinationFile), null);
                destParentDirEntry.updateFrom(destParentDir);
            }
        } finally {
            if (__newVar0 != null)
                try {
                    __newVar0.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
    }

    /**
     * Creates a directory.
     *
     * @param path The path of the new directory.
     */
    public void createDirectory(String path) {
        createDirectory(path, null);
    }

    /**
     * Deletes a directory.
     *
     * @param path The path of the directory to delete.
     */
    public void deleteDirectory(String path) {
        Closeable __newVar1 = new NtfsTransaction();
        try {
            if (path == null || path.isEmpty()) {
                throw new moe.yo3explorer.dotnetio4j.IOException("Unable to delete root directory");
            }

            String parent = Utilities.getDirectoryFromPath(path);
            DirectoryEntry parentDirEntry = getDirectoryEntry(parent);
            if (parentDirEntry == null || !parentDirEntry.getIsDirectory()) {
                throw new FileNotFoundException("No such directory: " + path);
            }

            Directory parentDir = getDirectory(parentDirEntry.getReference());
            DirectoryEntry dirEntry = parentDir.getEntryByName(Utilities.getFileFromPath(path));
            if (dirEntry == null || !dirEntry.getIsDirectory()) {
                throw new FileNotFoundException("No such directory: " + path);
            }

            Directory dir = getDirectory(dirEntry.getReference());
            if (!dir.getIsEmpty()) {
                throw new moe.yo3explorer.dotnetio4j.IOException("Unable to delete non-empty directory");
            }

            if (dirEntry.getDetails().getFileAttributes().containsKey("ReparsePoint")) {
                removeReparsePoint(dir);
            }

            removeFileFromDirectory(parentDir, dir, Utilities.getFileFromPath(path));
            if (dir.getHardLinkCount() == 0) {
                dir.delete();
            }
        } finally {
            if (__newVar1 != null)
                try {
                    __newVar1.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
    }

    /**
     * Deletes a file.
     *
     * @param path The path of the file to delete.
     */
    public void deleteFile(String path) {
        Closeable __newVar2 = new NtfsTransaction();
        try {
            String[] attributeName = new String[1];
            AttributeType[] attributeType = new AttributeType[1];
            String dirEntryPath = parsePath(path, attributeName, attributeType);
            String parentDirPath = Utilities.getDirectoryFromPath(dirEntryPath);
            DirectoryEntry parentDirEntry = getDirectoryEntry(parentDirPath);
            if (parentDirEntry == null || !parentDirEntry.getIsDirectory()) {
                throw new FileNotFoundException("No such file " + path);
            }

            Directory parentDir = getDirectory(parentDirEntry.getReference());
            DirectoryEntry dirEntry = parentDir.getEntryByName(Utilities.getFileFromPath(dirEntryPath));
            if (dirEntry == null || dirEntry.getIsDirectory()) {
                throw new FileNotFoundException("No such file " + path);
            }

            File file = getFile(dirEntry.getReference());
            if ((attributeName[0] == null || attributeName[0].isEmpty()) && attributeType[0] == AttributeType.Data) {
                if (dirEntry.getDetails().getFileAttributes().containsKey("ReparsePoint")) {
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
        } finally {
            if (__newVar2 != null)
                try {
                    __newVar2.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
    }

    /**
     * Indicates if a directory exists.
     *
     * @param path The path to test.
     * @return true if the directory exists.
     */
    public boolean directoryExists(String path) {
        Closeable __newVar3 = new NtfsTransaction();
        try {
            // Special case - root directory
            if (path == null || path.isEmpty()) {
                return true;
            }

            DirectoryEntry dirEntry = getDirectoryEntry(path);
            return dirEntry != null && dirEntry.getDetails().getFileAttributes().containsKey("Directory");
        } finally {
            if (__newVar3 != null)
                try {
                    __newVar3.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
    }

    /**
     * Indicates if a file exists.
     *
     * @param path The path to test.
     * @return true if the file exists.
     */
    public boolean fileExists(String path) {
        Closeable __newVar4 = new NtfsTransaction();
        try {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            return dirEntry != null && !dirEntry.getDetails().getFileAttributes().containsKey("Directory");
        } finally {
            if (__newVar4 != null)
                try {
                    __newVar4.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
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
        Closeable __newVar5 = new NtfsTransaction();
        try {
            Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);
            List<String> dirs = new ArrayList<>();
            doSearch(dirs, path, re, searchOption.equals("AllDirectories"), true, false);
            return dirs;
        } finally {
            if (__newVar5 != null)
                try {
                    __newVar5.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
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
     * @return Array of files matching the search pattern.
     */
    public List<String> getFiles(String path, String searchPattern, String searchOption) {
        Closeable __newVar6 = new NtfsTransaction();
        try {
            Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);
            List<String> results = new ArrayList<>();
            doSearch(results, path, re, searchOption.equals("AllDirectories"), false, true);
            return results;
        } finally {
            if (__newVar6 != null)
                try {
                    __newVar6.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
    }

    /**
     * Gets the names of all files and subdirectories in a specified directory.
     *
     * @param path The path to search.
     * @return Array of files and subdirectories.
     */
    public List<String> getFileSystemEntries(String path) {
        Closeable __newVar7 = new NtfsTransaction();
        try {
            DirectoryEntry parentDirEntry = getDirectoryEntry(path);
            if (parentDirEntry == null) {
                throw new FileNotFoundException(String.format("The directory '{0}' does not exist", path));
            }

            Directory parentDir = getDirectory(parentDirEntry.getReference());
            return parentDir.getAllEntries(true).stream().map(m -> {
                return Utilities.combinePaths(path, m.getDetails().FileName);
            }).collect(Collectors.toList());
        } finally {
            if (__newVar7 != null)
                try {
                    __newVar7.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
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
        Closeable __newVar8 = new NtfsTransaction();
        try {
            // TODO: Be smarter, use the B*Tree for better performance when the start of the pattern is known
            // characters
            Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);
            DirectoryEntry parentDirEntry = getDirectoryEntry(path);
            if (parentDirEntry == null) {
                throw new FileNotFoundException(String.format("The directory '{0}' does not exist", path));
            }

            Directory parentDir = getDirectory(parentDirEntry.getReference());
            List<String> result = new ArrayList<>();
            for (DirectoryEntry dirEntry : parentDir.getAllEntries(true)) {
                if (re.matcher(dirEntry.getDetails().FileName).find()) {
                    result.add(Utilities.combinePaths(path, dirEntry.getDetails().FileName));
                }
            }
            return result;
        } finally {
            if (__newVar8 != null)
                try {
                    __newVar8.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
    }

    /**
     * Moves a directory.
     *
     * @param sourceDirectoryName The directory to move.
     * @param destinationDirectoryName The target directory name.
     */
    public void moveDirectory(String sourceDirectoryName, String destinationDirectoryName) {
        try (Closeable __newVar9 = new NtfsTransaction()) {
            try (Closeable __newVar10 = new NtfsTransaction()) {
                DirectoryEntry sourceParentDirEntry = getDirectoryEntry(Utilities.getDirectoryFromPath(sourceDirectoryName));
                if (sourceParentDirEntry == null || !sourceParentDirEntry.getIsDirectory()) {
                    throw new FileNotFoundException("No such directory: " + sourceDirectoryName);
                }

                Directory sourceParentDir = getDirectory(sourceParentDirEntry.getReference());
                DirectoryEntry sourceEntry = sourceParentDir.getEntryByName(Utilities.getFileFromPath(sourceDirectoryName));
                if (sourceEntry == null || !sourceEntry.getIsDirectory()) {
                    throw new FileNotFoundException("No such directory: " + sourceDirectoryName);
                }

                File file = getFile(sourceEntry.getReference());
                DirectoryEntry destParentDirEntry = getDirectoryEntry(Utilities.getDirectoryFromPath(destinationDirectoryName));
                if (destParentDirEntry == null || !destParentDirEntry.getIsDirectory()) {
                    throw new FileNotFoundException("Destination directory not found: " + destinationDirectoryName);
                }

                Directory destParentDir = getDirectory(destParentDirEntry.getReference());
                DirectoryEntry destDirEntry = destParentDir.getEntryByName(Utilities.getFileFromPath(destinationDirectoryName));
                if (destDirEntry != null) {
                    throw new moe.yo3explorer.dotnetio4j.IOException("Destination directory already exists");
                }

                removeFileFromDirectory(sourceParentDir, file, sourceEntry.getDetails().FileName);
                addFileToDirectory(file, destParentDir, Utilities.getFileFromPath(destinationDirectoryName), null);
            }
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Moves a file, allowing an existing file to be overwritten.
     *
     * @param sourceName The file to move.
     * @param destinationName The target file name.
     * @param overwrite Whether to permit a destination file to be overwritten.
     */
    public void moveFile(String sourceName, String destinationName, boolean overwrite) {
        try (Closeable __newVar11 = new NtfsTransaction()) {
            DirectoryEntry sourceParentDirEntry = getDirectoryEntry(Utilities.getDirectoryFromPath(sourceName));
            if (sourceParentDirEntry == null || !sourceParentDirEntry.getIsDirectory()) {
                throw new FileNotFoundException("No such file " + sourceName);
            }

            Directory sourceParentDir = getDirectory(sourceParentDirEntry.getReference());
            DirectoryEntry sourceEntry = sourceParentDir.getEntryByName(Utilities.getFileFromPath(sourceName));
            if (sourceEntry == null || sourceEntry.getIsDirectory()) {
                throw new FileNotFoundException("No such file " + sourceName);
            }

            File file = getFile(sourceEntry.getReference());
            DirectoryEntry destParentDirEntry = getDirectoryEntry(Utilities.getDirectoryFromPath(destinationName));
            if (destParentDirEntry == null || !destParentDirEntry.getIsDirectory()) {
                throw new FileNotFoundException("Destination directory not found " + destinationName);
            }

            Directory destParentDir = getDirectory(destParentDirEntry.getReference());
            DirectoryEntry destDirEntry = destParentDir.getEntryByName(Utilities.getFileFromPath(destinationName));
            if (destDirEntry != null && !destDirEntry.getIsDirectory()) {
                if (overwrite) {
                    if (destDirEntry.getReference().getMftIndex() == sourceEntry.getReference().getMftIndex()) {
                        throw new moe.yo3explorer.dotnetio4j.IOException("Destination file already exists and is the source file");
                    }

                    File oldFile = getFile(destDirEntry.getReference());
                    destParentDir.removeEntry(destDirEntry);
                    if (oldFile.getHardLinkCount() == 0) {
                        oldFile.delete();
                    }
                } else {
                    throw new moe.yo3explorer.dotnetio4j.IOException("Destination file already exists");
                }
            }

            removeFileFromDirectory(sourceParentDir, file, sourceEntry.getDetails().FileName);
            addFileToDirectory(file, destParentDir, Utilities.getFileFromPath(destinationName), null);
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
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
    public SparseStream openFile(String path, FileMode mode, FileAccess access) {
        return openFile(path, mode, access, null);
    }

    /**
     * Gets the attributes of a file or directory.
     *
     * @param path The file or directory to inspect.
     * @return The attributes of the file or directory.
     */
    public Map<String, Object> getAttributes(String path) {
        Closeable __newVar12 = new NtfsTransaction();
        try {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            return dirEntry.getDetails().getFileAttributes();
        } finally {
            if (__newVar12 != null)
                try {
                    __newVar12.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
    }

    /**
     * Sets the attributes of a file or directory.
     *
     * @param path The file or directory to change.
     * @param newValue The new attributes of the file or directory.
     */
    public void setAttributes(String path, Map<String, Object> newValue) {
        try (Closeable __newVar13 = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            Map<String, Object> oldValue = dirEntry.getDetails().getFileAttributes();
            Map<String, Object> changedAttribs = oldValue ^ newValue;
            if (changedAttribs == 0) {
                return;
            }

            // Abort - nothing changed
            if (changedAttribs.containsKey("NonSettableFileAttributes")) {
                throw new IllegalArgumentException("Attempt to change attributes that are read-only");
            }

            File file = getFile(dirEntry.getReference());
            if (changedAttribs.containsKey("SparseFile")) {
                if (dirEntry.getIsDirectory()) {
                    throw new IllegalArgumentException("Attempt to change sparse attribute on a directory");
                }

                if (newValue.containsKey("SparseFile")) {
                    throw new IllegalArgumentException("Attempt to remove sparse attribute from file");
                }

                NtfsAttribute ntfsAttr = file.getAttribute(AttributeType.Data, null);
                if (ntfsAttr.getFlags().contains(AttributeFlags.Compressed)) {
                    throw new IllegalArgumentException("Attempt to mark compressed file as sparse");
                }

                ntfsAttr.getFlags().add(AttributeFlags.Sparse);
                if (ntfsAttr.getIsNonResident()) {
                    ntfsAttr.setCompressedDataSize(ntfsAttr.getPrimaryRecord().getAllocatedLength());
                    ntfsAttr.setCompressionUnitSize(16);
                    ((NonResidentAttributeBuffer) ntfsAttr.getRawBuffer()).alignVirtualClusterCount();
                }

            }

            if (changedAttribs.containsKey("Compressed") && !dirEntry.getIsDirectory()) {
                if (!newValue.containsKey("Compressed")) {
                    throw new IllegalArgumentException("Attempt to remove compressed attribute from file " + "newValue");
                }

                NtfsAttribute ntfsAttr = file.getAttribute(AttributeType.Data, null);
                if (ntfsAttr.getFlags().contains(AttributeFlags.Sparse)) {
                    throw new IllegalArgumentException("Attempt to mark sparse file as compressed");
                }

                ntfsAttr.getFlags().add(AttributeFlags.Compressed);
                if (ntfsAttr.getIsNonResident()) {
                    ntfsAttr.setCompressedDataSize(ntfsAttr.getPrimaryRecord().getAllocatedLength());
                    ntfsAttr.setCompressionUnitSize(16);
                    ((NonResidentAttributeBuffer) ntfsAttr.getRawBuffer()).alignVirtualClusterCount();
                }
            }

            updateStandardInformation(dirEntry, file, si -> {
                si._FileAttributes = FileNameRecord.setAttributes(newValue, si._FileAttributes);
            });
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Gets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The creation time.
     */
    public long getCreationTimeUtc(String path) {
        try (Closeable __newVar14 = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            return dirEntry.getDetails().CreationTime;
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Sets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setCreationTimeUtc(String path, long newTime) {
        try (Closeable __newVar15 = new NtfsTransaction()) {
            updateStandardInformation(path, si -> {
                si.CreationTime = newTime;
            });
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Gets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last access time.
     */
    public long getLastAccessTimeUtc(String path) {
        try (Closeable __newVar16 = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            return dirEntry.getDetails().LastAccessTime;
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Sets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setLastAccessTimeUtc(String path, long newTime) {
        try (Closeable __newVar17 = new NtfsTransaction()) {
            updateStandardInformation(path, si -> {
                si.LastAccessTime = newTime;
            });
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Gets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last write time.
     */
    public long getLastWriteTimeUtc(String path) {
        try (Closeable __newVar18 = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found" + path);
            }

            return dirEntry.getDetails().ModificationTime;
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Sets the last modification time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    public void setLastWriteTimeUtc(String path, long newTime) {
        try (Closeable __newVar19 = new NtfsTransaction()) {
            updateStandardInformation(path, si -> {
                si.ModificationTime = newTime;
            });
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Gets the length of a file.
     *
     * @param path The path to the file.
     * @return The length in bytes.
     */
    public long getFileLength(String path) {
        try (Closeable __newVar20 = new NtfsTransaction()) {
            String[] attributeName = new String[1];
            AttributeType[] attributeType = new AttributeType[1];
            String dirEntryPath = parsePath(path, attributeName, attributeType);
            DirectoryEntry dirEntry = getDirectoryEntry(dirEntryPath);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            // Ordinary file length request, use info from directory entry for efficiency - if allowed
            if (getNtfsOptions().getFileLengthFromDirectoryEntries() && attributeName[0] == null &&
                attributeType[0] == AttributeType.Data) {
                return dirEntry.getDetails().RealSize;
            }

            // Alternate stream / attribute, pull info from attribute record
            File file = getFile(dirEntry.getReference());
            NtfsAttribute attr = file.getAttribute(attributeType[0], attributeName[0]);
            if (attr == null) {
                throw new FileNotFoundException(String.format("No such attribute '%s(%s)'", attributeName, attributeType));
            }

            return attr.getLength();
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Converts a cluster (index) into an absolute byte position in the
     * underlying stream.
     *
     * @param cluster The cluster to convert.
     * @return The corresponding absolute byte position.
     */
    public long clusterToOffset(long cluster) {
        return cluster * getClusterSize();
    }

    /**
     * Converts an absolute byte position in the underlying stream to a cluster
     * (index).
     *
     * @param offset The byte position to convert.
     * @return The cluster containing the specified byte.
     */
    public long offsetToCluster(long offset) {
        return offset / getClusterSize();
    }

    /**
     * Converts a file name to the list of clusters occupied by the file's data.
     *
     * @param path The path to inspect.
     * @return The clusters as a list of cluster ranges.Note that in some file
     *         systems, small files may not have dedicated
     *         clusters. Only dedicated clusters will be returned.
     */
    public List<Range> pathToClusters(String path) {
        String[] plainPath = new String[1];
        String[] attributeName = new String[1];
        splitPath(path, plainPath, attributeName);
        DirectoryEntry dirEntry = getDirectoryEntry(plainPath[0]);
        if (dirEntry == null || dirEntry.getIsDirectory()) {
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
     * @param path The path to inspect.
     * @return The file extents, as absolute byte positions in the underlying
     *         stream.Use this method with caution - NTFS supports encrypted,
     *         sparse and compressed files
     *         where bytes are not directly stored in extents. Small files may
     *         be entirely stored in the
     *         Master File Table, where corruption protection algorithms mean
     *         that some bytes do not contain
     *         the expected values. This method merely indicates where file data
     *         is stored,
     *         not what's stored. To access the contents of a file, use
     *         OpenFile.
     */
    public List<StreamExtent> pathToExtents(String path) {
        String[] plainPath = new String[1];
        String[] attributeName = new String[1];
        splitPath(path, plainPath, attributeName);
        DirectoryEntry dirEntry = getDirectoryEntry(plainPath[0]);
        if (dirEntry == null || dirEntry.getIsDirectory()) {
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
    public ClusterMap buildClusterMap() {
        return _context.getMft().getClusterMap();
    }

    /**
     * Reads the boot code of the file system into a byte array.
     *
     * @return The boot code, or
     *         {@code null}
     *         if not available.
     */
    public byte[] readBootCode() {
        try (Stream s = openFile("\\$Boot", FileMode.Open)) {
            return StreamUtilities.readExact(s, (int) s.getLength());
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Writes a diagnostic dump of key NTFS structures.
     *
     * @param writer The writer to receive the dump.
     * @param linePrefix The indent to apply to the start of each line of
     *            output.
     */
    public void dump(PrintWriter writer, String linePrefix) {
        writer.println(linePrefix + "NTFS File System Dump");
        writer.println(linePrefix + "=====================");
        //_context.Mft.Dump(writer, linePrefix);
        writer.println(linePrefix);
        _context.getBiosParameterBlock().dump(writer, linePrefix);
        if (_context.getSecurityDescriptors() != null) {
            writer.println(linePrefix);
            _context.getSecurityDescriptors().dump(writer, linePrefix);
        }

        if (_context.getObjectIds() != null) {
            writer.println(linePrefix);
            _context.getObjectIds().dump(writer, linePrefix);
        }

        if (_context.getReparsePoints() != null) {
            writer.println(linePrefix);
            _context.getReparsePoints().dump(writer, linePrefix);
        }

        if (_context.getQuotas() != null) {
            writer.println(linePrefix);
            _context.getQuotas().dump(writer, linePrefix);
        }

        writer.println(linePrefix);
        getDirectory(MasterFileTable.RootDirIndex).dump(writer, linePrefix);
        writer.println(linePrefix);
        writer.println(linePrefix + "FULL FILE LISTING");
        for (FileRecord record : _context.getMft().getRecords()) {
            // Don't go through cache - these are short-lived, and this is (just!) diagnostics
            File f = new File(_context, record);
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
        writer.println(linePrefix + "\\ (5)");
        dumpDirectory(getDirectory(MasterFileTable.RootDirIndex), writer, linePrefix);
    }

    // 5 = Root Dir
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
        return getHardLinkCount(path) > 1;
    }

    /**
     * Gets the security descriptor associated with the file or directory.
     *
     * @param path The file or directory to inspect.
     * @return The security descriptor.
     */
    public RawSecurityDescriptor getSecurity(String path) {
        try (Closeable __newVar21 = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            File file = getFile(dirEntry.getReference());
            return doGetSecurity(file);
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Sets the security descriptor associated with the file or directory.
     *
     * @param path The file or directory to change.
     * @param securityDescriptor The new security descriptor.
     */
    public void setSecurity(String path, RawSecurityDescriptor securityDescriptor) {
        try (Closeable __newVar22 = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            File file = getFile(dirEntry.getReference());
            doSetSecurity(file, securityDescriptor);
            // Update the directory entry used to open the file
            dirEntry.updateFrom(file);
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Sets the reparse point data on a file or directory.
     *
     * @param path The file to set the reparse point on.
     * @param reparsePoint The new reparse point.
     */
    public void setReparsePoint(String path, ReparsePoint reparsePoint) {
        try (Closeable __newVar23 = new NtfsTransaction()) {
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
                    _context.getReparsePoints().remove(rp.Tag, dirEntry.getReference());
                }
            } else {
                stream = file.createStream(AttributeType.ReparsePoint, null);
            }
            // Set the new content
            ReparsePointRecord newRp = new ReparsePointRecord();
            newRp.Tag = reparsePoint.getTag();
            newRp.Content = reparsePoint.getContent();
            byte[] contentBuffer = new byte[(int) newRp.getSize()];
            newRp.writeTo(contentBuffer, 0);
            try (Stream contentStream = stream.open(FileAccess.ReadWrite)) {
                contentStream.write(contentBuffer, 0, contentBuffer.length);
                contentStream.setLength(contentBuffer.length);
            }
            // Update the standard information attribute - so it reflects the actual file state
            NtfsStream stdInfoStream = file.getStream(AttributeType.StandardInformation, null);
            StandardInformation si = stdInfoStream.getContent();
            si._FileAttributes.add(FileAttributeFlags.ReparsePoint);
            stdInfoStream.setContent(si);
            // Update the directory entry used to open the file, so it's accurate
            dirEntry.getDetails().EASizeOrReparsePointTag = newRp.Tag;
            dirEntry.updateFrom(file);
            // Write attribute changes back to the Master File Table
            file.updateRecordInMft();
            // Add the reparse point to the index
            _context.getReparsePoints().add(newRp.Tag, dirEntry.getReference());
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Gets the reparse point data associated with a file or directory.
     *
     * @param path The file to query.
     * @return The reparse point information.
     */
    public ReparsePoint getReparsePoint(String path) {
        try (Closeable __newVar24 = new NtfsTransaction()) {
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
                    return new ReparsePoint(rp.Tag, rp.Content);
                }
            }
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
        return null;
    }

    /**
     * Removes a reparse point from a file or directory, without deleting the
     * file or directory.
     *
     * @param path The path to the file or directory to remove the reparse point
     *            from.
     */
    public void removeReparsePoint(String path) {
        try (Closeable __newVar25 = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            File file = getFile(dirEntry.getReference());
            removeReparsePoint(file);
            // Update the directory entry used to open the file, so it's accurate
            dirEntry.updateFrom(file);
            // Write attribute changes back to the Master File Table
            file.updateRecordInMft();
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
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
        try (Closeable __newVar26 = new NtfsTransaction()) {
            String parentPath = Utilities.getDirectoryFromPath(path);
            DirectoryEntry parentEntry = getDirectoryEntry(parentPath);
            if (parentEntry == null || parentEntry.getDetails().getFileAttributes().containsKey("Directory")) {
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

            if (givenEntry.getDetails()._FileNameNamespace == FileNameNamespace.Dos) {
                return givenEntry.getDetails().FileName;
            }

            if (givenEntry.getDetails()._FileNameNamespace == FileNameNamespace.Win32) {
                File file = getFile(givenEntry.getReference());
                for (NtfsStream stream : file.getStreams(AttributeType.FileName, null)) {
                    FileNameRecord fnr = stream.getContent();
                    if (fnr.ParentDirectory.equals(givenEntry.getDetails().ParentDirectory) &&
                        fnr._FileNameNamespace == FileNameNamespace.Dos) {
                        return fnr.FileName;
                    }
                }
            }

            return null;
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Sets the short name for a given file or directory.
     *
     * @param path The full path to the file or directory to change.
     * @param shortName The shortName, which should not include a path.
     */
    public void setShortName(String path, String shortName) {
        if (!Utilities.is8Dot3(shortName)) {
            throw new IllegalArgumentException("Short name is not a valid 8.3 file name");
        }

        try (Closeable __newVar27 = new NtfsTransaction()) {
            String parentPath = Utilities.getDirectoryFromPath(path);
            DirectoryEntry parentEntry = getDirectoryEntry(parentPath);
            if (parentEntry == null || parentEntry.getDetails().getFileAttributes().containsKey("Directory")) {
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

            FileNameNamespace givenNamespace = givenEntry.getDetails()._FileNameNamespace;
            File file = getFile(givenEntry.getReference());
            if (givenNamespace == FileNameNamespace.Posix && file.getHasWin32OrDosName()) {
                throw new UnsupportedOperationException("Cannot set a short name on hard links");
            }

            // Convert Posix/Win32AndDos to just Win32
            if (givenEntry.getDetails()._FileNameNamespace != FileNameNamespace.Win32) {
                dir.removeEntry(givenEntry);
                dir.addEntry(file, givenEntry.getDetails().FileName, FileNameNamespace.Win32);
            }

            // Remove any existing Dos names, and set the new one
            List<NtfsStream> nameStreams = new ArrayList<>(file.getStreams(AttributeType.FileName, null));
            for (NtfsStream stream : nameStreams) {
                FileNameRecord fnr = stream.getContent();
                if (fnr.ParentDirectory.equals(givenEntry.getDetails().ParentDirectory) &&
                    fnr._FileNameNamespace == FileNameNamespace.Dos) {
                    DirectoryEntry oldEntry = dir.getEntryByName(fnr.FileName);
                    dir.removeEntry(oldEntry);
                }
            }
            dir.addEntry(file, shortName, FileNameNamespace.Dos);
            parentEntry.updateFrom(dir);
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Gets the standard file information for a file.
     *
     * @param path The full path to the file or directory to query.
     * @return The standard file information.
     */
    public WindowsFileInformation getFileStandardInformation(String path) {
        try (Closeable __newVar28 = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            File file = getFile(dirEntry.getReference());
            StandardInformation si = file.getStandardInformation();
            return new WindowsFileInformation();
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Sets the standard file information for a file.
     *
     * @param path The full path to the file or directory to query.
     * @param info The standard file information.
     */
    public void setFileStandardInformation(String path, WindowsFileInformation info) {
        try (Closeable __newVar29 = new NtfsTransaction()) {
            updateStandardInformation(path, si -> {
                si.CreationTime = info.getCreationTime();
                si.LastAccessTime = info.getLastAccessTime();
                si.MftChangedTime = info.getChangeTime();
                si.ModificationTime = info.getLastWriteTime();
                si._FileAttributes = StandardInformation.setFileAttributes(info.getFileAttributes(), si._FileAttributes);
            });
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Gets the file id for a given path.
     *
     * @param path The path to get the id of.
     * @return The file id.
     *         The returned file id includes the MFT index of the primary file
     *         record for the file.
     *         The file id can be used to determine if two paths refer to the
     *         same actual file.
     *         The MFT index is held in the lower 48 bits of the id.
     */
    public long getFileId(String path) {
        try (Closeable __newVar30 = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            return dirEntry.getReference().getValue();
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
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
     * @return
     *         {@code true}
     *         if NTFS is detected, else
     *         {@code false}
     *         .
     */
    public static boolean detect(Stream stream) {
        if (stream.getLength() < 512) {
            return false;
        }

        stream.setPosition(0);
        byte[] bytes = StreamUtilities.readExact(stream, 512);
        BiosParameterBlock bpb = BiosParameterBlock.fromBytes(bytes, 0);
        return bpb.isValid(stream.getLength());
    }

    /**
     * Gets the Master File Table for this file system.
     *
     * Use the returned object to explore the internals of the file system -
     * most people will
     * never need to use this.
     *
     * @return The Master File Table.
     */
    public DiscUtils.Ntfs.Internals.MasterFileTable getMasterFileTable() {
        return new DiscUtils.Ntfs.Internals.MasterFileTable(_context, _context.getMft());
    }

    /**
     * Creates a directory.
     *
     * @param path The path of the new directory.
     * @param options Options controlling attributes of the new Director, or
     *            {@code null}
     *            for defaults.
     */
    public void createDirectory(String path, NewFileOptions options) {
        try (Closeable __newVar31 = new NtfsTransaction()) {
            String[] pathElements = path.split("\\");
            Directory focusDir = getDirectory(MasterFileTable.RootDirIndex);
            DirectoryEntry focusDirEntry = focusDir.getDirectoryEntry();
            for (int i = 0; i < pathElements.length; ++i) {
                DirectoryEntry childDirEntry = focusDir.getEntryByName(pathElements[i]);
                if (childDirEntry == null) {
                    EnumSet<FileAttributeFlags> newDirAttrs = focusDir.getStandardInformation()._FileAttributes;
                    if (options != null && options.getCompressed().isPresent()) {
                        if (options.getCompressed().get()) {
                            newDirAttrs.add(FileAttributeFlags.Compressed);
                        } else {
                            newDirAttrs.remove(FileAttributeFlags.Compressed);
                        }
                    }

                    Directory childDir = Directory.createNew(_context, newDirAttrs);
                    try {
                        childDirEntry = addFileToDirectory(childDir, focusDir, pathElements[i], options);
                        RawSecurityDescriptor parentSd = doGetSecurity(focusDir);
                        RawSecurityDescriptor newSd = new RawSecurityDescriptor();
                        if (options != null && options.getSecurityDescriptor() != null) {
                            newSd = options.getSecurityDescriptor();
                        } else {
                            newSd = SecurityDescriptor.calcNewObjectDescriptor(parentSd, false);
                        }
                        doSetSecurity(childDir, newSd);
                        childDirEntry.updateFrom(childDir);
                        // Update the directory entry by which we found the directory we've just modified
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
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Opens the specified file.
     *
     * @param path The full path of the file to open.
     * @param mode The file mode for the created stream.
     * @param access The access permissions for the returned stream.
     * @param options Options controlling attributes of a new file, or
     *            {@code null}
     *            for defaults (ignored if file exists).
     * @return The new stream.
     */
    public SparseStream openFile(String path, FileMode mode, FileAccess access, NewFileOptions options) {
        try (Closeable __newVar32 = new NtfsTransaction()) {
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
                throw new moe.yo3explorer.dotnetio4j.IOException("File already exists");
            }

            if (entry.getDetails().getFileAttributes().containsKey("Directory") && attributeType[0] == AttributeType.Data) {
                throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to open directory as a file");
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
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
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
        try (Closeable __newVar33 = new NtfsTransaction()) {
            DirectoryEntry entry = getDirectoryEntry(file);
            if (entry == null) {
                throw new FileNotFoundException("No such file " + file);
            }

            File fileObj = getFile(entry.getReference());
            return fileObj.openStream(type, name, access);
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Creates an NTFS hard link to an existing file.
     *
     * @param sourceName An existing name of the file.
     * @param destinationName The name of the new hard link to the file.
     */
    public void createHardLink(String sourceName, String destinationName) {
        try (Closeable __newVar34 = new NtfsTransaction()) {
            DirectoryEntry sourceDirEntry = getDirectoryEntry(sourceName);
            if (sourceDirEntry == null) {
                throw new FileNotFoundException("Source file not found " + sourceName);
            }

            String destinationDirName = Utilities.getDirectoryFromPath(destinationName);
            DirectoryEntry destinationDirSelfEntry = getDirectoryEntry(destinationDirName);
            if (destinationDirSelfEntry == null ||
                destinationDirSelfEntry.getDetails().getFileAttributes().containsKey("Directory")) {
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
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Gets the number of hard links to a given file or directory.
     *
     * @param path The path of the file or directory.
     * @return The number of hard links.All files have at least one hard link.
     */
    public int getHardLinkCount(String path) {
        try (Closeable __newVar35 = new NtfsTransaction()) {
            DirectoryEntry dirEntry = getDirectoryEntry(path);
            if (dirEntry == null) {
                throw new FileNotFoundException("File not found " + path);
            }

            File file = getFile(dirEntry.getReference());
            if (!_context.getOptions().getHideDosFileNames()) {
                return file.getHardLinkCount();
            }

            int numHardLinks = 0;
            for (NtfsStream fnStream : file.getStreams(AttributeType.FileName, null)) {
                FileNameRecord fnr = fnStream.getContent();
                if (fnr._FileNameNamespace != FileNameNamespace.Dos) {
                    ++numHardLinks;
                }
            }
            return numHardLinks;
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     * Updates the BIOS Parameter Block (BPB) of the file system to reflect a
     * new disk geometry.
     *
     * @param geometry The disk's new BIOS geometry.Having an accurate geometry
     *            in the BPB is essential for booting some Operating Systems
     *            (e.g. Windows XP).
     */
    public void updateBiosGeometry(Geometry geometry) {
        _context.getBiosParameterBlock().SectorsPerTrack = (short) geometry.getSectorsPerTrack();
        _context.getBiosParameterBlock().NumHeads = (short) geometry.getHeadsPerCylinder();
        _context.getRawStream().setPosition(0);
        byte[] bpbSector = StreamUtilities.readExact(_context.getRawStream(), 512);
        _context.getBiosParameterBlock().toBytes(bpbSector, 0);
        _context.getRawStream().setPosition(0);
        _context.getRawStream().write(bpbSector, 0, bpbSector.length);
    }

    public DirectoryEntry getDirectoryEntry(String path) {
        return getDirectoryEntry(getDirectory(MasterFileTable.RootDirIndex), path);
    }

    /**
     * Disposes of this instance.
     */
    public void close() throws IOException {
        if (_context != null && _context.getMft() != null) {
            _context.getMft().close();
            _context.setMft(null);
        }

        Closeable disposableCompressor = _context.getOptions()
                .getCompressor() instanceof Closeable ? (Closeable) _context.getOptions().getCompressor() : (Closeable) null;
        if (disposableCompressor != null) {
            disposableCompressor.close();
            _context.getOptions().setCompressor(null);
        }

        super.close();
    }

    private static void removeFileFromDirectory(Directory dir, File file, String name) {
        List<String> aliases = new ArrayList<>();
        DirectoryEntry dirEntry = dir.getEntryByName(name);
        if (dirEntry.getDetails()._FileNameNamespace == FileNameNamespace.Dos ||
            dirEntry.getDetails()._FileNameNamespace == FileNameNamespace.Win32) {
            for (NtfsStream fnStream : file.getStreams(AttributeType.FileName, null)) {
                FileNameRecord fnr = fnStream.getContent();
                if ((fnr._FileNameNamespace == FileNameNamespace.Win32 || fnr._FileNameNamespace == FileNameNamespace.Dos) &&
                    fnr.ParentDirectory.getValue() == dir.getMftReference().getValue()) {
                    aliases.add(fnr.FileName);
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
        // Update the standard information attribute - so it reflects the actual file state
        NtfsStream stream = file.getStream(AttributeType.StandardInformation, null);
        StandardInformation si = stream.getContent();
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
        EnumSet<FileAttributeFlags> newFileAttrs = parentDir.getStandardInformation()._FileAttributes;
        if (options != null && options.getCompressed().isPresent()) {
            if (options.getCompressed().get()) {
                newFileAttrs.add(FileAttributeFlags.Compressed);
            } else {
                newFileAttrs.remove(FileAttributeFlags.Compressed);
            }
        }

        File file = File.createNew(_context, newFileAttrs);
        try {
            result = addFileToDirectory(file, parentDir, Utilities.getFileFromPath(path), options);
            RawSecurityDescriptor parentSd = doGetSecurity(parentDir);
            RawSecurityDescriptor newSd = new RawSecurityDescriptor();
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
        String[] pathElements = path.split("\\");
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
            boolean isDir = de.getDetails().getFileAttributes().containsKey("Directory");
            if ((isDir && dirs) || (!isDir && files)) {
                if (regex.matcher(de.getSearchName()).find()) {
                    results.add(Utilities.combinePaths(path, de.getDetails().FileName));
                }
            }

            if (subFolders && isDir) {
                doSearch(results, Utilities.combinePaths(path, de.getDetails().FileName), regex, subFolders, dirs, files);
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

            if (entry.getDetails().getFileAttributes().containsKey("Directory")) {
                return getDirectoryEntry(getDirectory(entry.getReference()), pathEntries, pathOffset + 1);
            }

            throw new moe.yo3explorer.dotnetio4j.IOException(String.format("{0} is a file, not a directory",
                                                                           pathEntries[pathOffset]));
        }

        return null;
    }

    private DirectoryEntry addFileToDirectory(File file, Directory dir, String name, NewFileOptions options) {
        DirectoryEntry entry;
        boolean createShortNames;
        if (options != null && options.getCreateShortNames().isPresent()) {
            createShortNames = options.getCreateShortNames().get();
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
            Stream contentStream = stream.open(FileAccess.Read);
            try {
                byte[] buffer = StreamUtilities.readExact(contentStream, (int) contentStream.getLength());
                rp.readFrom(buffer, 0);
            } finally {
                if (contentStream != null)
                    try {
                        contentStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
            }
            file.removeStream(stream);
            // Update the standard information attribute - so it reflects the actual file state
            NtfsStream stdInfoStream = file.getStream(AttributeType.StandardInformation, null);
            StandardInformation si = stdInfoStream.getContent();
            si._FileAttributes.remove(FileAttributeFlags.ReparsePoint);
            stdInfoStream.setContent(si);
            // Remove the reparse point from the index
            _context.getReparsePoints().remove(rp.Tag, file.getMftReference());
        }
    }

    private RawSecurityDescriptor doGetSecurity(File file) {
        NtfsStream legacyStream = file.getStream(AttributeType.SecurityDescriptor, null);
        if (legacyStream != null) {
            return legacyStream.getContent().Descriptor;
        }

        StandardInformation si = file.getStandardInformation();
        return _context.getSecurityDescriptors().getDescriptorById(si.SecurityId);
    }

    private void doSetSecurity(File file, RawSecurityDescriptor securityDescriptor) {
        NtfsStream legacyStream = file.getStream(AttributeType.SecurityDescriptor, null);
        if (legacyStream != null) {
            SecurityDescriptor sd = new SecurityDescriptor();
            sd.setDescriptor(securityDescriptor);
            legacyStream.setContent(sd);
        } else {
            int id = _context.getSecurityDescriptors().addDescriptor(securityDescriptor);
            // Update the standard information attribute - so it reflects the actual file state
            NtfsStream stream = file.getStream(AttributeType.StandardInformation, null);
            StandardInformation si = stream.getContent();
            si.SecurityId = id;
            stream.setContent(si);
            // Write attribute changes back to the Master File Table
            file.updateRecordInMft();
        }
    }

    private void dumpDirectory(Directory dir, PrintWriter writer, String indent) {
        for (DirectoryEntry dirEntry : dir.getAllEntries(true)) {
            File file = getFile(dirEntry.getReference());
            Directory asDir = file instanceof Directory ? (Directory) file : (Directory) null;
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
        String[] fileNameElements = fileName.split("\\", 3);
        fileName = fileNameElements[0];
        if (fileNameElements.length > 1) {
            attributeName[0] = fileNameElements[1];
            if (attributeName[0] == null || attributeName[0].isEmpty()) {
                attributeName[0] = null;
            }
        }

        if (fileNameElements.length > 2) {
            String typeName = fileNameElements[2];
            AttributeDefinitionRecord typeDefn = _context.getAttributeDefinitions().lookup(typeName);
            if (typeDefn == null) {
                throw new FileNotFoundException(String.format("No such attribute type '%s'", typeName));
            }

            attributeType[0] = typeDefn.Type;
        }

        try {
            String dirName = Utilities.getDirectoryFromPath(path);
            return Utilities.combinePaths(dirName, fileName);
        } catch (IllegalArgumentException __dummyCatchVar0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Invalid path: " + path);
        }
    }

//    private static class __MultiStandardInformationModifier implements StandardInformationModifier {
//        public void invoke(StandardInformation si) {
//            List<StandardInformationModifier> copy = new ArrayList<>(), members = this.getInvocationList();
//            synchronized (members) {
//                copy = new LinkedList<>(members);
//            }
//            for (StandardInformationModifier d : copy) {
//                d.invoke(si);
//            }
//        }
//
//        private List<StandardInformationModifier> _invocationList = new ArrayList<>();
//
//        public static StandardInformationModifier combine(StandardInformationModifier a, StandardInformationModifier b) {
//            if (a == null)
//                return b;
//
//            if (b == null)
//                return a;
//
//            __MultiStandardInformationModifier ret = new __MultiStandardInformationModifier();
//            ret._invocationList = a.getInvocationList();
//            ret._invocationList.addAll(b.getInvocationList());
//            return ret;
//        }
//
//        public static StandardInformationModifier remove(StandardInformationModifier a, StandardInformationModifier b) {
//            if (a == null || b == null)
//                return a;
//
//            List<StandardInformationModifier> aInvList = a.getInvocationList();
//            List<StandardInformationModifier> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//            if (aInvList == newInvList) {
//                return a;
//            } else {
//                __MultiStandardInformationModifier ret = new __MultiStandardInformationModifier();
//                ret._invocationList = newInvList;
//                return ret;
//            }
//        }
//
//        public List<StandardInformationModifier> getInvocationList() {
//            return _invocationList;
//        }
//    }

    @FunctionalInterface
    private static interface StandardInformationModifier {

        void invoke(StandardInformation si);

//        List<StandardInformationModifier> getInvocationList();
    }

    public Directory getDirectory(long index) {
        return (Directory) getFile(index);
    }

    public Directory getDirectory(FileRecordReference fileReference) {
        return (Directory) getFile(fileReference);
    }

    public File getFile(FileRecordReference fileReference) {
        FileRecord record = _context.getMft().getRecord(fileReference);
        if (record == null) {
            return null;
        }

        // Don't create file objects for file record segments that are part of another
        // logical file.
        if (record.getBaseFile().getValue() != 0) {
            return null;
        }

        File file = _fileCache.get___idx(fileReference.getMftIndex());
        if (file != null && file.getMftReference().getSequenceNumber() != fileReference.getSequenceNumber()) {
            file = null;
        }

        if (file == null) {
            if (record.getFlags().contains(FileRecordFlags.IsDirectory)) {
                file = new Directory(_context, record);
            } else {
                file = new File(_context, record);
            }
            _fileCache.set___idx(fileReference.getMftIndex(), file);
        }

        return file;
    }

    public File getFile(long index) {
        FileRecord record = _context.getMft().getRecord(index, false);
        if (record == null) {
            return null;
        }

        // Don't create file objects for file record segments that are part of another
        // logical file.
        if (record.getBaseFile().getValue() != 0) {
            return null;
        }

        File file = _fileCache.get___idx(index);
        if (file != null && file.getMftReference().getSequenceNumber() != record.getSequenceNumber()) {
            file = null;
        }

        if (file == null) {
            if (record.getFlags().contains(FileRecordFlags.IsDirectory)) {
                file = new Directory(_context, record);
            } else {
                file = new File(_context, record);
            }
            _fileCache.set___idx(index, file);
        }

        return file;
    }

    public File allocateFile(EnumSet<FileRecordFlags> flags) {
        File result = null;
        if (flags.contains(FileRecordFlags.IsDirectory)) {
            result = new Directory(_context, _context.getMft().allocateRecord(flags, false));
        } else {
            result = new File(_context, _context.getMft().allocateRecord(flags, false));
        }
        _fileCache.set___idx(result.getMftReference().getMftIndex(), result);
        return result;
    }

    public void forgetFile(File file) {
        _fileCache.remove((long) file.getIndexInMft());
    }

    /**
     * Size of the Filesystem in bytes
     */
    public long getSize() {
        return getTotalClusters() * getClusterSize();
    }

    /**
     * Used space of the Filesystem in bytes
     */
    public long getUsedSpace() {
        long usedCluster = 0;
        Bitmap bitmap = _context.getClusterBitmap().getBitmap();
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
    public long getAvailableSpace() {
        return getSize() - getUsedSpace();
    }
}
