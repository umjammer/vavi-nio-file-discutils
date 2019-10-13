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

package DiscUtils.Fat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.FileSystemParameters;
import DiscUtils.Core.FloppyDiskType;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.TimeConverter;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.FileNotFoundException;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * Class for accessing FAT file systems.
 */
public final class FatFileSystem extends DiscFileSystem {
    /**
     * The Epoch for FAT file systems (1st Jan, 1980).
     */
    public static final long Epoch = 0;

    private final Map<Integer, Directory> _dirCache;

    private Ownership _ownsData;

    private final TimeConverter _timeConverter;

    private byte[] _bootSector;

    private short _bpbBkBootSec;

    private short _bpbBytesPerSec;

    private short _bpbExtFlags;

    private short _bpbFATSz16;

    private int _bpbFATSz32;

    private short _bpbFSInfo;

    private short _bpbFSVer;

    private int _bpbHiddSec;

    private short _bpbNumHeads;

    private int _bpbRootClus;

    private short _bpbRootEntCnt;

    private short _bpbRsvdSecCnt;

    private short _bpbSecPerTrk;

    private short _bpbTotSec16;

    private int _bpbTotSec32;

    private byte _bsBootSig;

    private int _bsVolId;

    private String _bsVolLab;

    private Stream _data;

    private Directory _rootDir;

    /**
     * Initializes a new instance of the FatFileSystem class.
     *
     * @param data The stream containing the file system.
     *            Local time is the effective timezone of the new instance.
     */
    public FatFileSystem(Stream data) {
        super(new FatFileSystemOptions());
        _dirCache = new HashMap<>();
        _timeConverter = this::defaultTimeConverter;
        initialize(data);
    }

    /**
     * Initializes a new instance of the FatFileSystem class.
     *
     * @param data The stream containing the file system.
     * @param ownsData Indicates if the new instance should take ownership
     *            of
     *            {@code data}
     *            .
     *            Local time is the effective timezone of the new instance.
     */
    public FatFileSystem(Stream data, Ownership ownsData) {
        super(new FatFileSystemOptions());
        _dirCache = new HashMap<>();
        _timeConverter = this::defaultTimeConverter;
        initialize(data);
        _ownsData = ownsData;
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
        _dirCache = new HashMap<>();
        _timeConverter = timeConverter;
        initialize(data);
    }

    /**
     * Initializes a new instance of the FatFileSystem class.
     *
     * @param data The stream containing the file system.
     * @param ownsData Indicates if the new instance should take ownership
     *            of
     *            {@code data}
     *            .
     * @param timeConverter A delegate to convert to/from the file system's
     *            timezone.
     */
    public FatFileSystem(Stream data, Ownership ownsData, TimeConverter timeConverter) {
        super(new FatFileSystemOptions());
        _dirCache = new HashMap<>();
        _timeConverter = timeConverter;
        initialize(data);
        _ownsData = ownsData;
    }

    /**
     * Initializes a new instance of the FatFileSystem class.
     *
     * @param data The stream containing the file system.
     * @param ownsData Indicates if the new instance should take ownership
     *            of
     *            {@code data}
     *            .
     * @param parameters The parameters for the file system.
     */
    public FatFileSystem(Stream data, Ownership ownsData, FileSystemParameters parameters) {
        super(new FatFileSystemOptions(parameters));
        _dirCache = new HashMap<>();
        if (parameters != null && parameters.getTimeConverter() != null) {
            _timeConverter = parameters.getTimeConverter();
        } else {
            _timeConverter = this::defaultTimeConverter;
        }
        initialize(data);
        _ownsData = ownsData;
    }

    /**
     * Gets the active FAT (zero-based index).
     */
    public byte getActiveFat() {
        return (byte) ((_bpbExtFlags & 0x08) != 0 ? _bpbExtFlags & 0x7 : 0);
    }

    /**
     * Gets the Sector location of the backup boot sector (FAT32 only).
     */
    public int getBackupBootSector() {
        return _bpbBkBootSec;
    }

    /**
     * Gets the BIOS drive number for BIOS Int 13h calls.
     */
    private byte __BiosDriveNumber;

    public byte getBiosDriveNumber() {
        return __BiosDriveNumber;
    }

    public void setBiosDriveNumber(byte value) {
        __BiosDriveNumber = value;
    }

    /**
     * Gets the number of bytes per sector (as stored in the file-system meta
     * data).
     */
    public int getBytesPerSector() {
        return _bpbBytesPerSec;
    }

    /**
     * Indicates if this file system is read-only or read-write.
     *
     * @return .
     */
    public boolean canWrite() {
        return _data.canWrite();
    }

    private ClusterReader __ClusterReader;

    public ClusterReader getClusterReader() {
        return __ClusterReader;
    }

    public void setClusterReader(ClusterReader value) {
        __ClusterReader = value;
    }

    /**
     * Gets a value indicating whether the VolumeId, VolumeLabel and
     * FileSystemType fields are valid.
     */
    public boolean getExtendedBootSignaturePresent() {
        return _bsBootSig == 0x29;
    }

    private FileAllocationTable __Fat;

    public FileAllocationTable getFat() {
        return __Fat;
    }

    public void setFat(FileAllocationTable value) {
        __Fat = value;
    }

    /**
     * Gets the number of FATs present.
     */
    private byte __FatCount;

    public byte getFatCount() {
        return __FatCount;
    }

    public void setFatCount(byte value) {
        __FatCount = value;
    }

    /**
     * Gets the FAT file system options, which can be modified.
     */
    public FatFileSystemOptions getFatOptions() {
        return (FatFileSystemOptions) getOptions();
    }

    /**
     * Gets the size of a single FAT, in sectors.
     */
    public long getFatSize() {
        return _bpbFATSz16 != 0 ? _bpbFATSz16 : _bpbFATSz32;
    }

    /**
     * Gets the FAT variant of the file system.
     */
    private FatType __FatVariant = FatType.None;

    public FatType getFatVariant() {
        return __FatVariant;
    }

    public void setFatVariant(FatType value) {
        __FatVariant = value;
    }

    /**
     * Gets the (informational only) file system type recorded in the meta-data.
     */
    private String __FileSystemType;

    public String getFileSystemType() {
        return __FileSystemType;
    }

    public void setFileSystemType(String value) {
        __FileSystemType = value;
    }

    /**
     * Gets the friendly name for the file system, including FAT variant.
     */
    public String getFriendlyName() {
        switch (getFatVariant()) {
        case Fat12:
            return "Microsoft FAT12";
        case Fat16:
            return "Microsoft FAT16";
        case Fat32:
            return "Microsoft FAT32";
        default:
            return "Unknown FAT";

        }
    }

    /**
     * Gets the sector location of the FSINFO structure (FAT32 only).
     */
    public int getFSInfoSector() {
        return _bpbFSInfo;
    }

    /**
     * Gets the number of logical heads.
     */
    public int getHeads() {
        return _bpbNumHeads;
    }

    /**
     * Gets the number of hidden sectors, hiding partition tables, etc.
     */
    public long getHiddenSectors() {
        return _bpbHiddSec;
    }

    /**
     * Gets the maximum number of root directory entries (on FAT variants that
     * have a limit).
     */
    public int getMaxRootDirectoryEntries() {
        return _bpbRootEntCnt;
    }

    /**
     * Gets the Media marker byte, which indicates fixed or removable media.
     */
    private byte __Media;

    public byte getMedia() {
        return __Media;
    }

    public void setMedia(byte value) {
        __Media = value;
    }

    /**
     * Gets a value indicating whether FAT changes are mirrored to all copies of
     * the FAT.
     */
    public boolean getMirrorFat() {
        return (_bpbExtFlags & 0x08) == 0;
    }

    /**
     * Gets the OEM name from the file system.
     */
    private String __OemName;

    public String getOemName() {
        return __OemName;
    }

    public void setOemName(String value) {
        __OemName = value;
    }

    /**
     * Gets the number of reserved sectors at the start of the disk.
     */
    public int getReservedSectorCount() {
        return _bpbRsvdSecCnt;
    }

    /**
     * Gets the cluster number of the first cluster of the root directory (FAT32
     * only).
     */
    public long getRootDirectoryCluster() {
        return _bpbRootClus;
    }

    /**
     * Gets the number of contiguous sectors that make up one cluster.
     */
    private byte __SectorsPerCluster;

    public byte getSectorsPerCluster() {
        return __SectorsPerCluster;
    }

    public void setSectorsPerCluster(byte value) {
        __SectorsPerCluster = value;
    }

    /**
     * Gets the number of sectors per logical track.
     */
    public int getSectorsPerTrack() {
        return _bpbSecPerTrk;
    }

    /**
     * Gets the total number of sectors on the disk.
     */
    public long getTotalSectors() {
        return _bpbTotSec16 != 0 ? _bpbTotSec16 : _bpbTotSec32;
    }

    /**
     * Gets the file-system version (usually 0).
     */
    public int getVersion() {
        return _bpbFSVer;
    }

    /**
     * Gets the volume serial number.
     */
    public int getVolumeId() {
        return _bsVolId;
    }

    /**
     * Gets the volume label.
     */
    public String getVolumeLabel() {
        long volId = _rootDir.findVolumeId();
        if (volId < 0) {
            return _bsVolLab;
        }

        return _rootDir.getEntry(volId).getName().getRawName(getFatOptions().getFileNameEncoding());
    }

    /**
     * Detects if a stream contains a FAT file system.
     *
     * @param stream The stream to inspect.
     * @return
     *         {@code true}
     *         if the stream appears to be a FAT file system, else
     *         {@code false}
     *         .
     */
    public static boolean detect(Stream stream) {
        if (stream.getLength() < 512) {
            return false;
        }

        stream.setPosition(0);
        byte[] bytes = StreamUtilities.readExact(stream, 512);
        short bpbBytesPerSec = (short) EndianUtilities.toUInt16LittleEndian(bytes, 11);
        if (bpbBytesPerSec != 512) {
            return false;
        }

        byte bpbNumFATs = bytes[16];
        if (bpbNumFATs == 0 || bpbNumFATs > 2) {
            return false;
        }

        short bpbTotSec16 = (short) EndianUtilities.toUInt16LittleEndian(bytes, 19);
        int bpbTotSec32 = EndianUtilities.toUInt32LittleEndian(bytes, 32);
        if (!((bpbTotSec16 == 0) ^ (bpbTotSec32 == 0))) {
            return false;
        }

        int totalSectors = bpbTotSec16 + bpbTotSec32;
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
    public SparseStream openFile(String path, FileMode mode, FileAccess access) {
        Directory[] parent = new Directory[1];
        long entryId;
        try {
            entryId = getDirectoryEntry(_rootDir, path, parent);
        } catch (IllegalArgumentException __dummyCatchVar0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Invalid path: " + path);
        }

        if (parent[0] == null) {
            throw new FileNotFoundException("Could not locate file " + path);
        }

        if (entryId < 0) {
            return parent[0].openFile(FileName.fromPath(path, getFatOptions().getFileNameEncoding()), mode, access);
        }

        DirectoryEntry dirEntry = parent[0].getEntry(entryId);
        if (dirEntry.getAttributes().contains(FatAttributes.Directory)) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to open directory as a file");
        }

        return parent[0].openFile(dirEntry.getName(), mode, access);
    }

    /**
     * Gets the attributes of a file or directory.
     *
     * @param path The file or directory to inspect.
     * @return The attributes of the file or directory.
     */
    public Map<String, Object> getAttributes(String path) {
        // Simulate a root directory entry - doesn't really exist though
        if (isRootPath(path)) {
            return new HashMap<String, Object>() {
                {
                    put("Directory", true);
                }
            };
        }

        DirectoryEntry dirEntry = getDirectoryEntry(path);
        if (dirEntry == null) {
            throw new FileNotFoundException("No such file " + path);
        }

        return FatAttributes.convert(dirEntry.getAttributes());
    }

    // Luckily, FAT and .NET Map<String, Object> match, bit-for-bit

    /**
     * Sets the attributes of a file or directory.
     *
     * @param path The file or directory to change.
     * @param newValue The new attributes of the file or directory.
     */
    public void setAttributes(String path, Map<String, Object> newValue) {
        if (isRootPath(path)) {
            if (!newValue.containsKey("Directory")) {
                throw new UnsupportedOperationException("The attributes of the root directory cannot be modified");
            }

            return;
        }

        Directory[] parent = new Directory[1];
        long id = getDirectoryEntry(path, parent);
        DirectoryEntry dirEntry = parent[0].getEntry(id);
        EnumSet<FatAttributes> newFatAttr = FatAttributes.convert(newValue);
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
    public long getCreationTime(String path) {
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
    public void setCreationTime(String path, long newTime) {
        if (isRootPath(path)) {
            if (newTime != Epoch) {
                throw new UnsupportedOperationException("The creation time of the root directory cannot be modified");
            }

            return;
        }

        updateDirEntry(path, e -> {
            e.setCreationTime(newTime);
        });
    }

    /**
     * Gets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The creation time.
     */
    public long getCreationTimeUtc(String path) {
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
    public void setCreationTimeUtc(String path, long newTime) {
        if (isRootPath(path)) {
            if (convertFromUtc(newTime) != Epoch) {
                throw new UnsupportedOperationException("The last write time of the root directory cannot be modified");
            }

            return;
        }

        updateDirEntry(path, e -> {
            e.setCreationTime(convertFromUtc(newTime));
        });
    }

    /**
     * Gets the last access time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The time the file or directory was last accessed.
     */
    public long getLastAccessTime(String path) {
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
    public void setLastAccessTime(String path, long newTime) {
        if (isRootPath(path)) {
            if (newTime != Epoch) {
                throw new UnsupportedOperationException("The last access time of the root directory cannot be modified");
            }

            return;
        }

        updateDirEntry(path, e -> {
            e.setLastAccessTime(newTime);
        });
    }

    /**
     * Gets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The time the file or directory was last accessed.
     */
    public long getLastAccessTimeUtc(String path) {
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
    public void setLastAccessTimeUtc(String path, long newTime) {
        if (isRootPath(path)) {
            if (convertFromUtc(newTime) != Epoch) {
                throw new UnsupportedOperationException("The last write time of the root directory cannot be modified");
            }

            return;
        }

        updateDirEntry(path, e -> {
            e.setLastAccessTime(convertFromUtc(newTime));
        });
    }

    /**
     * Gets the last modification time (in local time) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The time the file or directory was last modified.
     */
    public long getLastWriteTime(String path) {
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
    public void setLastWriteTime(String path, long newTime) {
        if (isRootPath(path)) {
            if (newTime != Epoch) {
                throw new UnsupportedOperationException("The last write time of the root directory cannot be modified");
            }

            return;
        }

        updateDirEntry(path, e -> {
            e.setLastWriteTime(newTime);
        });
    }

    /**
     * Gets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The time the file or directory was last modified.
     */
    public long getLastWriteTimeUtc(String path) {
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
    public void setLastWriteTimeUtc(String path, long newTime) {
        if (isRootPath(path)) {
            if (convertFromUtc(newTime) != Epoch) {
                throw new UnsupportedOperationException("The last write time of the root directory cannot be modified");
            }

            return;
        }

        updateDirEntry(path, e -> {
            e.setLastWriteTime(convertFromUtc(newTime));
        });
    }

    /**
     * Gets the length of a file.
     *
     * @param path The path to the file.
     * @return The length in bytes.
     */
    public long getFileLength(String path) {
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
    public void copyFile(String sourceFile, String destinationFile, boolean overwrite) {
        Directory[] sourceDir = new Directory[1];
        long sourceEntryId = getDirectoryEntry(sourceFile, sourceDir);
        if (sourceDir[0] == null || sourceEntryId < 0) {
            throw new FileNotFoundException(String.format("The source file '%s' was not found", sourceFile));
        }

        DirectoryEntry sourceEntry = sourceDir[0].getEntry(sourceEntryId);
        if (sourceEntry.getAttributes().contains(FatAttributes.Directory)) {
            throw new moe.yo3explorer.dotnetio4j.IOException("The source file is a directory");
        }

        DirectoryEntry newEntry = new DirectoryEntry(sourceEntry);
        newEntry.setName(FileName.fromPath(destinationFile, getFatOptions().getFileNameEncoding()));
        newEntry.setFirstCluster(0);
        Directory[] destDir = new Directory[1];
        long destEntryId = getDirectoryEntry(destinationFile, destDir);
        if (destDir[0] == null) {
            throw new FileNotFoundException(String.format("The destination directory for '%s' was not found", destinationFile));
        }

        // If the destination is a directory, use the old file name to construct a full path.
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
                throw new moe.yo3explorer.dotnetio4j.IOException("Destination file is an existing directory");
            }

            if (!overwrite) {
                throw new moe.yo3explorer.dotnetio4j.IOException("Destination file already exists");
            }

            // Remove the old file
            destDir[0].deleteEntry(destEntryId, true);
        }

        // Add the new file's entry
        destEntryId = destDir[0].addEntry(newEntry);
        // Copy the contents...
        Stream sourceStream = new FatFileStream(this, sourceDir[0], sourceEntryId, FileAccess.Read),
                destStream = new FatFileStream(this, destDir[0], destEntryId, FileAccess.Write);
        try {
            StreamUtilities.pumpStreams(sourceStream, destStream);
        } finally {
            try {
                if (sourceStream != null)
                    sourceStream.close();
                if (destStream != null)
                    destStream.close();
            } catch (IOException e) {
                throw new moe.yo3explorer.dotnetio4j.IOException(e);
            }
        }
    }

    /**
     * Creates a directory.
     *
     * @param path The directory to create.
     */
    public void createDirectory(String path) {
        String[] pathElements = path.split(Utilities.escapeForRegex("\\"));
        Directory focusDir = _rootDir;
        for (int i = 0; i < pathElements.length; ++i) {
            FileName name;
            try {
                name = new FileName(pathElements[i], getFatOptions().getFileNameEncoding());
            } catch (IllegalArgumentException ae) {
                throw new moe.yo3explorer.dotnetio4j.IOException("Invalid path: " + path, ae);
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
    public void deleteDirectory(String path) {
        Directory dir = getDirectory(path);
        if (dir == null) {
            throw new FileNotFoundException(String.format("No such directory: {0}", path));
        }

        if (!dir.getIsEmpty()) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Unable to delete non-empty directory");
        }

        Directory[] parent = new Directory[1];
        long id = getDirectoryEntry(path, parent);
        if (parent[0] == null && id == 0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Unable to delete root directory");
        }

        if (parent != null && id >= 0) {
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
    public void deleteFile(String path) {
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
    public boolean directoryExists(String path) {
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
    public boolean fileExists(String path) {
        // Special case - root directory
        if (path == null || path.isEmpty()) {
            return true;
        }

        DirectoryEntry dirEntry = getDirectoryEntry(path);
        return dirEntry != null && dirEntry.getAttributes().contains(FatAttributes.Directory);
    }

    /**
     * Indicates if a file or directory exists.
     *
     * @param path The path to test.
     * @return true if the file or directory exists.
     */
    public boolean exists(String path) {
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
     * @return Array of directories.
     */
    public List<String> getDirectories(String path) {
        Directory dir = getDirectory(path);
        if (dir == null) {
            throw new FileNotFoundException(String.format("The directory '{0}' was not found", path));
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
        doSearch(dirs, path, re, searchOption == "AllDirectories", true, false);
        return dirs;
    }

    /**
     * Gets the names of files in a specified directory.
     *
     * @param path The path to search.
     * @return Array of files.
     */
    public List<String> getFiles(String path) {
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
     * @return Array of files matching the search pattern.
     */
    public List<String> getFiles(String path, String searchPattern, String searchOption) {
        Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);
        List<String> results = new ArrayList<>();
        doSearch(results, path, re, searchOption == "AllDirectories", false, true);
        return results;
    }

    /**
     * Gets the names of all files and subdirectories in a specified directory.
     *
     * @param path The path to search.
     * @return Array of files and subdirectories matching the search pattern.
     */
    public List<String> getFileSystemEntries(String path) {
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
     * matching a specified
     * search pattern.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return Array of files and subdirectories matching the search pattern.
     */
    public List<String> getFileSystemEntries(String path, String searchPattern) {
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
    public void moveDirectory(String sourceDirectoryName, String destinationDirectoryName) {
        if (destinationDirectoryName == null || destinationDirectoryName.isEmpty()) {
            if (destinationDirectoryName == null) {
                throw new NullPointerException(destinationDirectoryName);
            }

            throw new IllegalArgumentException("Invalid destination name (empty string)");
        }

        Directory[] destParent = new Directory[1];
        long destId = getDirectoryEntry(destinationDirectoryName, destParent);
        if (destParent[0] == null) {
            throw new FileNotFoundException("Target directory doesn't exist");
        }

        if (destId >= 0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Target directory already exists");
        }

        Directory[] sourceParent = new Directory[1];
        long sourceId = getDirectoryEntry(sourceDirectoryName, sourceParent);
        if (sourceParent[0] == null || sourceId < 0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Source directory doesn't exist");
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
    public void moveFile(String sourceName, String destinationName, boolean overwrite) {
        Directory[] sourceDir = new Directory[1];
        long sourceEntryId = getDirectoryEntry(sourceName, sourceDir);
        if (sourceDir[0] == null || sourceEntryId < 0) {
            throw new FileNotFoundException(String.format("The source file '%s' was not found", sourceName));
        }

        DirectoryEntry sourceEntry = sourceDir[0].getEntry(sourceEntryId);
        if (sourceEntry.getAttributes().contains(FatAttributes.Directory)) {
            throw new moe.yo3explorer.dotnetio4j.IOException("The source file is a directory");
        }

        DirectoryEntry newEntry = new DirectoryEntry(sourceEntry);
        newEntry.setName(FileName.fromPath(destinationName, getFatOptions().getFileNameEncoding()));
        Directory[] destDir = new Directory[1];
        long destEntryId = getDirectoryEntry(destinationName, destDir);
        if (destDir[0] == null) {
            throw new FileNotFoundException(String.format("The destination directory for '%s' was not found", destinationName));
        }

        // If the destination is a directory, use the old file name to construct a full path.
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
                throw new moe.yo3explorer.dotnetio4j.IOException("Destination file is an existing directory");
            }

            if (!overwrite) {
                throw new moe.yo3explorer.dotnetio4j.IOException("Destination file already exists");
            }

            // Remove the old file
            destDir[0].deleteEntry(destEntryId, true);
        }

        // Add the new file's entry and remove the old link to the file's contents
        destDir[0].addEntry(newEntry);
        sourceDir[0].deleteEntry(sourceEntryId, false);
    }

    public long convertToUtc(long dateTime) {
        return _timeConverter.invoke(dateTime, true);
    }

    public long convertFromUtc(long dateTime) {
        return _timeConverter.invoke(dateTime, false);
    }

    public Directory getDirectory(String path) {
        Directory[] parent = new Directory[1];
        if (path == null || path.isEmpty() || path.equals("\\")) {
            return _rootDir;
        }

        long id = getDirectoryEntry(_rootDir, path, parent);
        if (id >= 0) {
            return getDirectory(parent[0], id);
        }

        return null;
    }

    public Directory getDirectory(Directory parent, long parentId) {
        if (parent == null) {
            return _rootDir;
        }

        DirectoryEntry dirEntry = parent.getEntry(parentId);
        if (!dirEntry.getAttributes().contains(FatAttributes.Directory)) {
            throw new FileNotFoundException(dirEntry.getName()
                    .getRawName(Charset.forName(System.getProperty("file.encoding"))));
        }

        // If we have this one cached, return it
        if (_dirCache.containsKey(dirEntry.getFirstCluster())) {
            return _dirCache.get(dirEntry.getFirstCluster());
        }

        // Not cached - create a new one.
        Directory result = new Directory(parent, parentId);
        _dirCache.put(dirEntry.getFirstCluster(), result);
        return result;
    }

    public void forgetDirectory(DirectoryEntry entry) {
        int index = entry.getFirstCluster();
        if (index != 0 && _dirCache.containsKey(index)) {
            Directory dir = _dirCache.get(index);
            _dirCache.remove(index);
            dir.close();
        }
    }

    public DirectoryEntry getDirectoryEntry(String path) {
        Directory[] parent = new Directory[1];
        long id = getDirectoryEntry(_rootDir, path, parent);
        if (parent == null || id < 0) {
            return null;
        }

        return parent[0].getEntry(id);
    }

    public long getDirectoryEntry(String path, Directory[] parent) {
        return getDirectoryEntry(_rootDir, path, parent);
    }

    /**
     * Disposes of this instance.
     */
    public void close() throws IOException {
        try {
            for (Directory dir : _dirCache.values()) {
                dir.close();
            }
            _rootDir.close();
            if (_ownsData == Ownership.Dispose) {
                _data.close();
                _data = null;
            }
        } finally {
            super.close();
        }
    }

    /**
     * Writes a FAT12/FAT16 BPB.
     *
     * @param bootSector The buffer to fill.
     * @param sectors The total capacity of the disk (in sectors).
     * @param fatType The number of bits in each FAT entry.
     * @param maxRootEntries The maximum number of root directory entries.
     * @param hiddenSectors The number of hidden sectors before this file system
     *            (i.e. partition offset).
     * @param reservedSectors The number of reserved sectors before the FAT.
     * @param sectorsPerCluster The number of sectors per cluster.
     * @param diskGeometry The geometry of the disk containing the Fat file
     *            system.
     * @param isFloppy Indicates if the disk is a removable media (a floppy
     *            disk).
     * @param volId The disk's volume Id.
     * @param label The disk's label (or null).
     */
    private static void writeBPB(byte[] bootSector,
                                 int sectors,
                                 FatType fatType,
                                 short maxRootEntries,
                                 int hiddenSectors,
                                 short reservedSectors,
                                 byte sectorsPerCluster,
                                 Geometry diskGeometry,
                                 boolean isFloppy,
                                 int volId,
                                 String label) {
        int fatSectors = calcFatSize(sectors, fatType, sectorsPerCluster);
        bootSector[0] = (byte) 0xEB;
        bootSector[1] = 0x3C;
        bootSector[2] = (byte) 0x90;
        // OEM Name
        EndianUtilities.stringToBytes("DISCUTIL", bootSector, 3, 8);
        // Bytes Per Sector (512)
        bootSector[11] = 0;
        bootSector[12] = 2;
        // Sectors Per Cluster
        bootSector[13] = sectorsPerCluster;
        // Reserved Sector Count
        EndianUtilities.writeBytesLittleEndian(reservedSectors, bootSector, 14);
        // Number of FATs
        bootSector[16] = 2;
        // Number of Entries in the root directory
        EndianUtilities.writeBytesLittleEndian(maxRootEntries, bootSector, 17);
        // Total number of sectors (small)
        EndianUtilities.writeBytesLittleEndian((short) (sectors < 0x10000 ? sectors : 0), bootSector, 19);
        // Media
        bootSector[21] = (byte) (isFloppy ? 0xF0 : 0xF8);
        // FAT size (FAT12/FAT16)
        EndianUtilities
                .writeBytesLittleEndian((short) (fatType.ordinal() < FatType.Fat32.ordinal() ? fatSectors : 0), bootSector, 22);
        // Sectors Per Track
        EndianUtilities.writeBytesLittleEndian((short) diskGeometry.getSectorsPerTrack(), bootSector, 24);
        // Heads Per Cylinder
        EndianUtilities.writeBytesLittleEndian((short) diskGeometry.getHeadsPerCylinder(), bootSector, 26);
        // Hidden Sectors
        EndianUtilities.writeBytesLittleEndian(hiddenSectors, bootSector, 28);
        // Total number of sectors (large)
        EndianUtilities.writeBytesLittleEndian(sectors >= 0x10000 ? sectors : 0, bootSector, 32);
        if (fatType.ordinal() < FatType.Fat32.ordinal()) {
            writeBS(bootSector, 36, isFloppy, volId, label, fatType);
        } else {
            // FAT size (FAT32)
            EndianUtilities.writeBytesLittleEndian(fatSectors, bootSector, 36);
            // Ext flags: 0x80 = FAT 1 (i.e. Zero) active, mirroring
            bootSector[40] = 0x00;
            bootSector[41] = 0x00;
            // Filesystem version (0.0)
            bootSector[42] = 0;
            bootSector[43] = 0;
            // First cluster of the root directory, always 2 since we don't do bad sectors...
            EndianUtilities.writeBytesLittleEndian(2, bootSector, 44);
            // Sector number of FSINFO
            EndianUtilities.writeBytesLittleEndian(1, bootSector, 48);
            // Sector number of the Backup Boot Sector
            EndianUtilities.writeBytesLittleEndian(6, bootSector, 50);
            // Reserved area - must be set to 0
            Arrays.fill(bootSector, 52, 12, (byte) 0);
            writeBS(bootSector, 64, isFloppy, volId, label, fatType);
        }
        bootSector[510] = 0x55;
        bootSector[511] = (byte) 0xAA;
    }

    private static int calcFatSize(int sectors, FatType fatType, byte sectorsPerCluster) {
        int numClusters = sectors / sectorsPerCluster;
        int fatBytes = numClusters * (short) fatType.ordinal() / 8;
        return (fatBytes + Sizes.Sector - 1) / Sizes.Sector;
    }

    private static void writeBS(byte[] bootSector, int offset, boolean isFloppy, int volId, String label, FatType fatType) {
        if (label == null || label.isEmpty()) {
            label = "NO NAME    ";
        }

        String fsType = "FAT32   ";
        if (fatType == FatType.Fat12) {
            fsType = "FAT12   ";
        } else if (fatType == FatType.Fat16) {
            fsType = "FAT16   ";
        }

        // Drive Number (for BIOS)
        bootSector[offset + 0] = (byte) (isFloppy ? 0x00 : 0x80);
        // Reserved
        bootSector[offset + 1] = 0;
        // Boot Signature (indicates next 3 fields present)
        bootSector[offset + 2] = 0x29;
        // Volume Id
        EndianUtilities.writeBytesLittleEndian(volId, bootSector, offset + 3);
        // Volume Label
        EndianUtilities.stringToBytes(label + "           ", bootSector, offset + 7, 11);
        // File System Type
        EndianUtilities.stringToBytes(fsType, bootSector, offset + 18, 8);
    }

    private static FatType detectFATType(byte[] bpb) {
        int bpbBytesPerSec = EndianUtilities.toUInt16LittleEndian(bpb, 11);
        if (bpbBytesPerSec == 0) {
            throw new IllegalStateException("Bytes per sector is 0, invalid or corrupt filesystem.");
        }

        int bpbRootEntCnt = EndianUtilities.toUInt16LittleEndian(bpb, 17);
        int bpbFATSz16 = EndianUtilities.toUInt16LittleEndian(bpb, 22);
        int bpbFATSz32 = EndianUtilities.toUInt32LittleEndian(bpb, 36);
        int bpbTotSec16 = EndianUtilities.toUInt16LittleEndian(bpb, 19);
        int bpbTotSec32 = EndianUtilities.toUInt32LittleEndian(bpb, 32);
        int bpbResvdSecCnt = EndianUtilities.toUInt16LittleEndian(bpb, 14);
        int bpbNumFATs = bpb[16];
        int bpbSecPerClus = bpb[13];
        int rootDirSectors = (bpbRootEntCnt * 32 + bpbBytesPerSec - 1) / bpbBytesPerSec;
        int fatSz = bpbFATSz16 != 0 ? bpbFATSz16 : bpbFATSz32;
        int totalSec = bpbTotSec16 != 0 ? bpbTotSec16 : bpbTotSec32;
        int dataSec = totalSec - (bpbResvdSecCnt + bpbNumFATs * fatSz + rootDirSectors);
        int countOfClusters = dataSec / bpbSecPerClus;
        if (countOfClusters < 4085) {
            return FatType.Fat12;
        }

        if (countOfClusters < 65525) {
            return FatType.Fat16;
        }

        return FatType.Fat32;
    }

    private static boolean isRootPath(String path) {
        return (path == null || path.isEmpty()) || path.equals("\\");
    }

    private long defaultTimeConverter(long time, boolean toUtc) {
        return toUtc ? time : Instant.ofEpochMilli(time).toEpochMilli(); // TODO ToLocalTime();
    }

    private void initialize(Stream data) {
        _data = data;
        _data.setPosition(0);
        _bootSector = StreamUtilities.readSector(_data);
//System.err.println(StringUtil.getDump(_bootSector, 64));
        setFatVariant(detectFATType(_bootSector));
        readBPB();
        loadFAT();
        loadClusterReader();
        loadRootDirectory();
    }

    private void loadClusterReader() {
        int rootDirSectors = (_bpbRootEntCnt * 32 + (_bpbBytesPerSec - 1)) / _bpbBytesPerSec;
        int firstDataSector = (int) (_bpbRsvdSecCnt + getFatCount() * getFatSize() + rootDirSectors);
        setClusterReader(new ClusterReader(_data, firstDataSector, getSectorsPerCluster(), _bpbBytesPerSec));
    }

    private void loadRootDirectory() {
        Stream fatStream;
        if (getFatVariant() != FatType.Fat32) {
            fatStream = new SubStream(_data,
                                      (_bpbRsvdSecCnt + getFatCount() * _bpbFATSz16) * _bpbBytesPerSec,
                                      _bpbRootEntCnt * 32);
        } else {
            fatStream = new ClusterStream(this, FileAccess.ReadWrite, _bpbRootClus, Integer.MAX_VALUE);
        }
        _rootDir = new Directory(this, fatStream);
    }

    private void loadFAT() {
        setFat(new FileAllocationTable(getFatVariant(),
                                       _data,
                                       _bpbRsvdSecCnt,
                                       (int) getFatSize(),
                                       getFatCount(),
                                       getActiveFat()));
    }

    private void readBPB() {
        setOemName(new String(_bootSector, 3, 8, Charset.forName("ASCII")).replaceFirst("\0*$", ""));
        _bpbBytesPerSec = (short) EndianUtilities.toUInt16LittleEndian(_bootSector, 11);
        setSectorsPerCluster(_bootSector[13]);
        _bpbRsvdSecCnt = (short) EndianUtilities.toUInt16LittleEndian(_bootSector, 14);
        setFatCount(_bootSector[16]);
        _bpbRootEntCnt = (short) EndianUtilities.toUInt16LittleEndian(_bootSector, 17);
        _bpbTotSec16 = (short) EndianUtilities.toUInt16LittleEndian(_bootSector, 19);
        setMedia(_bootSector[21]);
        _bpbFATSz16 = (short) EndianUtilities.toUInt16LittleEndian(_bootSector, 22);
        _bpbSecPerTrk = (short) EndianUtilities.toUInt16LittleEndian(_bootSector, 24);
        _bpbNumHeads = (short) EndianUtilities.toUInt16LittleEndian(_bootSector, 26);
        _bpbHiddSec = EndianUtilities.toUInt32LittleEndian(_bootSector, 28);
        _bpbTotSec32 = EndianUtilities.toUInt32LittleEndian(_bootSector, 32);
        if (getFatVariant() != FatType.Fat32) {
            readBS(36);
        } else {
            _bpbFATSz32 = EndianUtilities.toUInt32LittleEndian(_bootSector, 36);
            _bpbExtFlags = (short) EndianUtilities.toUInt16LittleEndian(_bootSector, 40);
            _bpbFSVer = (short) EndianUtilities.toUInt16LittleEndian(_bootSector, 42);
            _bpbRootClus = EndianUtilities.toUInt32LittleEndian(_bootSector, 44);
            _bpbFSInfo = (short) EndianUtilities.toUInt16LittleEndian(_bootSector, 48);
            _bpbBkBootSec = (short) EndianUtilities.toUInt16LittleEndian(_bootSector, 50);
            readBS(64);
        }
    }

    private void readBS(int offset) {
        setBiosDriveNumber(_bootSector[offset]);
        _bsBootSig = _bootSector[offset + 2];
        _bsVolId = EndianUtilities.toUInt32LittleEndian(_bootSector, offset + 3);
        _bsVolLab = new String(_bootSector, offset + 7, 11, Charset.forName("ASCII"));
        setFileSystemType(new String(_bootSector, offset + 18, 8, Charset.forName("ASCII")));
    }

    private long getDirectoryEntry(Directory dir, String path, Directory[] parent) {
        String[] pathElements = path.split(Utilities.escapeForRegex("\\"));
        return getDirectoryEntry(dir, pathElements, 0, parent);
    }

    private long getDirectoryEntry(Directory dir, String[] pathEntries, int pathOffset, Directory[] parent) {
        long entryId;
        if (pathEntries.length == 0) {
            // Looking for root directory, simulate the directory entry in its parent...
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
            throw new FileNotFoundException(String.format("The directory '{0}' was not found", path));
        }

        List<DirectoryEntry> entries = dir.getEntries();
        for (DirectoryEntry de : entries) {
            boolean isDir = de.getAttributes().contains(FatAttributes.Directory);
            if ((isDir && dirs) || (!isDir && files)) {
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
    public long getSize() {
        return ((getTotalSectors() - getReservedSectorCount() - (getFatSize() * getFatCount())) * getBytesPerSector());
    }

    /**
     * Used space of the Filesystem in bytes
     */
    public long getUsedSpace() {
        int usedCluster = 0;
        for (int i = 2; i < getFat().getNumEntries(); i++) {
            int fatValue = getFat().getNext(i);
            if (!getFat().isFree(fatValue)) {
                usedCluster++;
            }
        }
        return (usedCluster * getSectorsPerCluster() * getBytesPerSector());
    }

    /**
     * Available space of the Filesystem in bytes
     */
    public long getAvailableSpace() {
        return getSize() - getUsedSpace();
    }

//    private static class __MultiEntryUpdateAction implements EntryUpdateAction {
//        public void invoke(DirectoryEntry entry) {
//            List<EntryUpdateAction> copy = new ArrayList<>(), members = this.getInvocationList();
//            synchronized (members) {
//                copy = new LinkedList<>(members);
//            }
//            for (EntryUpdateAction d : copy) {
//                d.invoke(entry);
//            }
//        }
//
//        private List<EntryUpdateAction> _invocationList = new ArrayList<>();
//
//        public static EntryUpdateAction combine(EntryUpdateAction a, EntryUpdateAction b) {
//            if (a == null)
//                return b;
//
//            if (b == null)
//                return a;
//
//            __MultiEntryUpdateAction ret = new __MultiEntryUpdateAction();
//            ret._invocationList = a.getInvocationList();
//            ret._invocationList.addAll(b.getInvocationList());
//            return ret;
//        }
//
//        public static EntryUpdateAction remove(EntryUpdateAction a, EntryUpdateAction b) {
//            if (a == null || b == null)
//                return a;
//
//            List<EntryUpdateAction> aInvList = a.getInvocationList();
//            List<EntryUpdateAction> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//            if (aInvList == newInvList) {
//                return a;
//            } else {
//                __MultiEntryUpdateAction ret = new __MultiEntryUpdateAction();
//                ret._invocationList = newInvList;
//                return ret;
//            }
//        }
//
//        public List<EntryUpdateAction> getInvocationList() {
//            return _invocationList;
//        }
//    }

    @FunctionalInterface
    private static interface EntryUpdateAction {

        void invoke(DirectoryEntry entry);

//        List<EntryUpdateAction> getInvocationList();
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
        long pos = stream.getPosition();
        long ticks = System.currentTimeMillis();
        int volId = (int) ((ticks & 0xFFFF) | (ticks >>> 32));
        // Write the BIOS Parameter Block (BPB) - a single sector
        byte[] bpb = new byte[512];
        int sectors;
        switch (type) {
        case DoubleDensity:
            sectors = 1440;
            writeBPB(bpb,
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
            writeBPB(bpb,
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
            writeBPB(bpb,
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
        int fatSize = calcFatSize(sectors, FatType.Fat12, (byte) 1);
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
        // Write a single byte at the end of the disk to ensure the stream is at least as big
        // as needed for this disk image.
        stream.setPosition(pos + sectors * Sizes.Sector - 1);
        stream.writeByte((byte) 0);
        // Give the caller access to the new file system
        stream.setPosition(pos);
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
        Stream partitionStream = disk.getPartitions().get___idx(partitionIndex).open();
        try {
            return formatPartition(partitionStream,
                                   label,
                                   disk.getGeometry(),
                                   (int) disk.getPartitions().get___idx(partitionIndex).getFirstSector(),
                                   (int) (1 + disk.getPartitions().get___idx(partitionIndex).getLastSector() -
                                          disk.getPartitions().get___idx(partitionIndex).getFirstSector()),
                                   (short) 0);
        } finally {
            if (partitionStream != null)
                try {
                    partitionStream.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
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
        long pos = stream.getPosition();
        long ticks = System.currentTimeMillis();
        int volId = (int) ((ticks & 0xFFFF) | (ticks >>> 32));
        byte sectorsPerCluster;
        FatType fatType = FatType.None;
        short maxRootEntries;
        /* Write the BIOS Parameter Block (BPB) - a single sector */
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
        writeBPB(bpb,
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
        /* Skip the reserved sectors */
        stream.setPosition(pos + reservedSectors * Sizes.Sector);
        /* Write both FAT copies */
        byte[] fat = new byte[calcFatSize(sectorCount, fatType, sectorsPerCluster) * Sizes.Sector];
        FatBuffer fatBuffer = new FatBuffer(fatType, fat);
        fatBuffer.setNext(0, 0xFFFFFFF8);
        fatBuffer.setEndOfChain(1);
        if (fatType.ordinal() >= FatType.Fat32.ordinal()) {
            // Mark cluster 2 as End-of-chain (i.e. root directory
            // is a single cluster in length)
            fatBuffer.setEndOfChain(2);
        }

        stream.write(fat, 0, fat.length);
        stream.write(fat, 0, fat.length);
        /* Write the (empty) root directory */
        int rootDirSectors;
        if (fatType.ordinal() < FatType.Fat32.ordinal()) {
            rootDirSectors = (maxRootEntries * 32 + Sizes.Sector - 1) / Sizes.Sector;
        } else {
            rootDirSectors = sectorsPerCluster;
        }
        byte[] rootDir = new byte[rootDirSectors * Sizes.Sector];
        stream.write(rootDir, 0, rootDir.length);
        /* Make sure the stream is at least as large as the partition
         * requires. */
        if (stream.getLength() < pos + sectorCount * Sizes.Sector) {
            stream.setLength(pos + sectorCount * Sizes.Sector);
        }

        /* Give the caller access to the new file system */
        stream.setPosition(pos);
        return new FatFileSystem(stream);
    }
}
