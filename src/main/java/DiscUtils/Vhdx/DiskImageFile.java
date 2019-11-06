//
// Copyright (c) 2008-2012, Kenneth Bell
// Copyright (c) 2017, Bianco Veigel
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

package DiscUtils.Vhdx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import DiscUtils.Core.FileLocator;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.VirtualDiskExtent;
import DiscUtils.Core.VirtualDiskLayer;
import DiscUtils.Core.Internal.LocalFileLocator;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.AligningStream;
import DiscUtils.Streams.CircularStream;
import DiscUtils.Streams.MappedStream;
import DiscUtils.Streams.SnapshotStream;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.ZeroStream;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


/**
 * Represents a single .VHDX file.
 */
public final class DiskImageFile extends VirtualDiskLayer {
    private static final UUID EMPTY = new UUID(0L, 0L);

    /**
     * Which VHDX header is active.
     */
    private int _activeHeader;

    /**
     * Block Allocation Table for disk content.
     */
    private Stream _batStream;

    /**
     * The object that can be used to locate relative file paths.
     */
    private FileLocator _fileLocator;

    /**
     * The file name of this VHDX.
     */
    private String _fileName;

    /**
     * The stream containing the VHDX file.
     */
    private Stream _fileStream;

    /**
     * Table of all free space in the file.
     */
    private FreeSpaceTable _freeSpace;

    /**
     * Value of the active VHDX header.
     */
    private VhdxHeader _header;

    /**
     * The stream containing the logical VHDX content and metadata allowing for
     * log replay.
     */
    private Stream _logicalStream;

    /**
     * VHDX metadata region content.
     */
    private Metadata _metadata;

    /**
     * Indicates if this object controls the lifetime of the stream.
     */
    private Ownership _ownership;

    /**
     * The set of VHDX regions.
     */
    private RegionTable _regionTable;

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param stream The stream to interpret.
     */
    public DiskImageFile(Stream stream) {
        _fileStream = stream;
        initialize();
    }

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param stream The stream to interpret.
     * @param ownership Indicates if the new instance should control the
     *            lifetime of the stream.
     */
    public DiskImageFile(Stream stream, Ownership ownership) {
        _fileStream = stream;
        _ownership = ownership;
        initialize();
    }

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param path The file path to open.
     * @param access Controls how the file can be accessed.
     */
    public DiskImageFile(String path, FileAccess access) {
        this(new LocalFileLocator(Utilities.getDirectoryFromPath(path)), Utilities.getFileFromPath(path), access);
    }

    DiskImageFile(FileLocator locator, String path, Stream stream, Ownership ownsStream) {
        this(stream, ownsStream);
        _fileLocator = locator.getRelativeLocator(locator.getDirectoryFromPath(path));
        _fileName = locator.getFileFromPath(path);
    }

    DiskImageFile(FileLocator locator, String path, FileAccess access) {
        FileShare share = access == FileAccess.Read ? FileShare.Read : FileShare.None;
        _fileStream = locator.open(path, FileMode.Open, access, share);
        _ownership = Ownership.Dispose;
        try {
            _fileLocator = locator.getRelativeLocator(locator.getDirectoryFromPath(path));
            _fileName = locator.getFileFromPath(path);
            initialize();
        } finally {
            try {
                _fileStream.close();
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }
    }

    public long getCapacity() {
        return _metadata.getDiskSize();
    }

    /**
     * Gets the extent that comprises this file.
     */
    public List<VirtualDiskExtent> getExtents() {
        List<VirtualDiskExtent> result = new ArrayList<>();
        result.add(new DiskExtent(this));
        return result;
    }

    /**
     * Gets the full path to this disk layer, or empty string.
     */
    public String getFullPath() {
        if (_fileLocator != null && _fileName != null) {
            return _fileLocator.getFullPath(_fileName);
        }

        return "";
    }

    /**
     * Gets the geometry of the virtual disk.
     */
    public Geometry getGeometry() {
        return Geometry.fromCapacity(getCapacity(), _metadata.getLogicalSectorSize());
    }

    /**
     * Gets detailed information about the VHDX file.
     */
    public DiskImageFileInfo getInformation() {
        _fileStream.setPosition(0);
        FileHeader fileHeader = StreamUtilities.readStruct(FileHeader.class, _fileStream);

        _fileStream.setPosition(64 * Sizes.OneKiB);
        VhdxHeader vhdxHeader1 = StreamUtilities.readStruct(VhdxHeader.class, _fileStream);

        _fileStream.setPosition(128 * Sizes.OneKiB);
        VhdxHeader vhdxHeader2 = StreamUtilities.readStruct(VhdxHeader.class, _fileStream);

        LogSequence activeLogSequence = findActiveLogSequence();

        return new DiskImageFileInfo(fileHeader, vhdxHeader1, vhdxHeader2, _regionTable, _metadata, activeLogSequence);
    }

    /**
     * Gets a value indicating if the layer only stores meaningful sectors.
     */
    public boolean isSparse() {
        return true;
    }

    /**
     * Gets the logical sector size of the virtual disk.
     */
    public long getLogicalSectorSize() {
        return _metadata.getLogicalSectorSize();
    }

    /**
     * Gets a value indicating whether the file is a differencing disk.
     */
    public boolean needsParent() {
        return _metadata.getFileParameters().Flags.contains(FileParametersFlags.HasParent);
    }

    /**
     * Gets the unique id of the parent disk.
     */
    public UUID getParentUniqueId() {
        if (!_metadata.getFileParameters().Flags.contains(FileParametersFlags.HasParent)) {
            return EMPTY;
        }

        if (_metadata.getParentLocator().getEntries().containsKey("parent_linkage")) {
            return UUID.fromString(_metadata.getParentLocator().getEntries().get("parent_linkage"));
        }

        return EMPTY;
    }

    public FileLocator getRelativeFileLocator() {
        return _fileLocator;
    }

    long getStoredSize() {
        return _fileStream.getLength();
    }

    /**
     * Gets the unique id of this file.
     */
    public UUID getUniqueId() {
        return _header.DataWriteGuid;
    }

    /**
     * Initializes a stream as a fixed-sized VHDX file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @return An object that accesses the stream as a VHDX file.
     */
    public static DiskImageFile initializeFixed(Stream stream, Ownership ownsStream, long capacity) {
        return initializeFixed(stream, ownsStream, capacity, null);
    }

    /**
     * Initializes a stream as a fixed-sized VHDX file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @param geometry The desired geometry of the new disk, or {@code null} for
     *            default.
     * @return An object that accesses the stream as a VHDX file.
     */
    public static DiskImageFile initializeFixed(Stream stream, Ownership ownsStream, long capacity, Geometry geometry) {
        initializeFixedInternal(stream, capacity, geometry);
        return new DiskImageFile(stream, ownsStream);
    }

    /**
     * Initializes a stream as a dynamically-sized VHDX file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @return An object that accesses the stream as a VHDX file.
     */
    public static DiskImageFile initializeDynamic(Stream stream, Ownership ownsStream, long capacity) {
        initializeDynamicInternal(stream, capacity, FileParameters.DefaultDynamicBlockSize);
        return new DiskImageFile(stream, ownsStream);
    }

    /**
     * Initializes a stream as a dynamically-sized VHDX file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @param blockSize The size of each block (unit of allocation).
     * @return An object that accesses the stream as a VHDX file.
     */
    public static DiskImageFile initializeDynamic(Stream stream, Ownership ownsStream, long capacity, long blockSize) {
        initializeDynamicInternal(stream, capacity, blockSize);
        return new DiskImageFile(stream, ownsStream);
    }

    /**
     * Initializes a stream as a differencing disk VHDX file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param parent The disk this file is a different from.
     * @param parentAbsolutePath The full path to the parent disk.
     * @param parentRelativePath The relative path from the new disk to the
     *            parent disk.
     * @param parentModificationTimeUtc The time the parent disk's file was last
     *            modified (from file system).
     * @return An object that accesses the stream as a VHDX file.
     */
    public static DiskImageFile initializeDifferencing(Stream stream,
                                                       Ownership ownsStream,
                                                       DiskImageFile parent,
                                                       String parentAbsolutePath,
                                                       String parentRelativePath,
                                                       long parentModificationTimeUtc) {
        initializeDifferencingInternal(stream, parent, parentAbsolutePath, parentRelativePath, parentModificationTimeUtc);
        return new DiskImageFile(stream, ownsStream);
    }

    /**
     * Opens an existing region within the VHDX file.
     *
     * @param region Identifier for the region to open.
     * @return A stream containing the region data.Regions are an extension
     *         mechanism in VHDX - with some regions defined by the VHDX
     *         specification to hold metadata and the block allocation data.
     */
    public Stream openRegion(UUID region) {
        RegionEntry metadataRegion = _regionTable.Regions.get(region);
        return new SubStream(_logicalStream, metadataRegion.fileOffset, metadataRegion.getLength());
    }

    /**
     * Opens the content of the disk image file as a stream.
     *
     * @param parent The parent file's content (if any).
     * @param ownsParent Whether the created stream assumes ownership of parent
     *            stream.
     * @return The new content stream.
     */
    public SparseStream openContent(SparseStream parent, Ownership ownsParent) {
        return doOpenContent(parent, ownsParent);
    }

    /**
     * Gets the location of the parent file, given a base path.
     *
     * @return Array of candidate file locations.
     */
    public List<String> getParentLocations() {
        return getParentLocations(_fileLocator);
    }

    /**
     * Gets the location of the parent file, given a base path.
     *
     * @param basePath The full path to this file.
     * @return Array of candidate file locations.
     */
    public List<String> getParentLocations(String basePath) {
        return getParentLocations(new LocalFileLocator(basePath));
    }

    static DiskImageFile initializeFixed(FileLocator locator, String path, long capacity, Geometry geometry) {
        DiskImageFile result = null;
        try (Stream stream = locator.open(path, FileMode.Create, FileAccess.ReadWrite, FileShare.None)) {
            initializeFixedInternal(stream, capacity, geometry);
            result = new DiskImageFile(locator, path, stream, Ownership.Dispose);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }

        return result;
    }

    static DiskImageFile initializeDynamic(FileLocator locator, String path, long capacity, long blockSize) {
        DiskImageFile result = null;
        try (Stream stream = locator.open(path, FileMode.Create, FileAccess.ReadWrite, FileShare.None)) {
            initializeDynamicInternal(stream, capacity, blockSize);
            result = new DiskImageFile(locator, path, stream, Ownership.Dispose);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }

        return result;
    }

    DiskImageFile createDifferencing(FileLocator fileLocator, String path) {
        Stream stream = fileLocator.open(path, FileMode.Create, FileAccess.ReadWrite, FileShare.None);
        String fullPath = _fileLocator.getFullPath(_fileName);
        String relativePath = fileLocator.makeRelativePath(_fileLocator, _fileName);
        long lastWriteTime = _fileLocator.getLastWriteTimeUtc(_fileName);
        initializeDifferencingInternal(stream, this, fullPath, relativePath, lastWriteTime);
        return new DiskImageFile(fileLocator, path, stream, Ownership.Dispose);
    }

    MappedStream doOpenContent(SparseStream parent, Ownership ownsParent) {
        SparseStream theParent = parent;
        Ownership theOwnership = ownsParent;

        if (parent == null) {
            theParent = new ZeroStream(getCapacity());
            theOwnership = Ownership.Dispose;
        }

        ContentStream contentStream = new ContentStream(SparseStream.fromStream(_logicalStream, Ownership.None),
                                                        _fileStream.canWrite(),
                                                        _batStream,
                                                        _freeSpace,
                                                        _metadata,
                                                        getCapacity(),
                                                        theParent,
                                                        theOwnership);
        return new AligningStream(contentStream, Ownership.Dispose, _metadata.getLogicalSectorSize());
    }

    /**
     * Disposes of underlying resources.
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        if (_logicalStream != _fileStream && _logicalStream != null) {
            _logicalStream.close();
        }

        _logicalStream = null;
        if (_ownership == Ownership.Dispose && _fileStream != null) {
            _fileStream.close();
        }

        _fileStream = null;
    }

    private static void initializeFixedInternal(Stream stream, long capacity, Geometry geometry) {
        throw new UnsupportedOperationException();
    }

    private static void initializeDynamicInternal(Stream stream, long capacity, long blockSize) {
        if (blockSize < Sizes.OneMiB || blockSize > Sizes.OneMiB * 256 || !Utilities.isPowerOfTwo(blockSize)) {
            throw new IndexOutOfBoundsException("BlockSize must be a power of 2 between 1MB and 256MB");
        }

        int logicalSectorSize = 512;
        int physicalSectorSize = 4096;
        long chunkRatio = 0x800000L * logicalSectorSize / blockSize;
        long dataBlocksCount = MathUtilities.ceil(capacity, blockSize);
        @SuppressWarnings("unused")
        long sectorBitmapBlocksCount = MathUtilities.ceil(dataBlocksCount, chunkRatio);
        long totalBatEntriesDynamic = dataBlocksCount + (dataBlocksCount - 1) / chunkRatio;

        FileHeader fileHeader = new FileHeader();
        fileHeader.Creator = ".NET DiscUtils";

        long fileEnd = Sizes.OneMiB;

        VhdxHeader header1 = new VhdxHeader();
        header1.SequenceNumber = 0;
        header1.FileWriteGuid = UUID.randomUUID();
        header1.DataWriteGuid = UUID.randomUUID();
        header1.LogGuid = EMPTY;
        header1.LogVersion = 0;
        header1.Version = 1;
        header1.LogLength = (int) Sizes.OneMiB;
        header1.LogOffset = fileEnd;
        header1.calcChecksum();

        fileEnd += header1.LogLength;

        VhdxHeader header2 = new VhdxHeader(header1);
        header2.SequenceNumber = 1;
        header2.calcChecksum();

        RegionTable regionTable = new RegionTable();

        RegionEntry metadataRegion = new RegionEntry();
        metadataRegion.guid = RegionEntry.MetadataRegionGuid;
        metadataRegion.fileOffset = fileEnd;
        metadataRegion.setLength((int) Sizes.OneMiB);
        metadataRegion.flags = RegionFlags.Required;
        regionTable.Regions.put(metadataRegion.guid, metadataRegion);

        fileEnd += metadataRegion.getLength();

        RegionEntry batRegion = new RegionEntry();
        batRegion.guid = RegionEntry.BatGuid;
        batRegion.fileOffset = 3 * Sizes.OneMiB;
        batRegion.setLength((int) MathUtilities.roundUp(totalBatEntriesDynamic * 8, Sizes.OneMiB));
        batRegion.flags = RegionFlags.Required;
        regionTable.Regions.put(batRegion.guid, batRegion);

        fileEnd += batRegion.getLength();

        stream.setPosition(0);
        StreamUtilities.writeStruct(stream, fileHeader);

        stream.setPosition(64 * Sizes.OneKiB);
        StreamUtilities.writeStruct(stream, header1);

        stream.setPosition(128 * Sizes.OneKiB);
        StreamUtilities.writeStruct(stream, header2);

        stream.setPosition(192 * Sizes.OneKiB);
        StreamUtilities.writeStruct(stream, regionTable);

        stream.setPosition(256 * Sizes.OneKiB);
        StreamUtilities.writeStruct(stream, regionTable);

        // Set stream to min size
        stream.setPosition(fileEnd - 1);
        stream.writeByte((byte) 0);

        // Metadata
        FileParameters fileParams = new FileParameters();
        fileParams.BlockSize = (int) blockSize;
        fileParams.Flags = EnumSet.of(FileParametersFlags.None);
        @SuppressWarnings("unused")
        ParentLocator parentLocator = new ParentLocator();

        Stream metadataStream = new SubStream(stream, metadataRegion.fileOffset, metadataRegion.getLength());
        @SuppressWarnings("unused")
        Metadata metadata = Metadata
                .initialize(metadataStream, fileParams, capacity, logicalSectorSize, physicalSectorSize, null);
    }

    private static void initializeDifferencingInternal(Stream stream,
                                                       DiskImageFile parent,
                                                       String parentAbsolutePath,
                                                       String parentRelativePath,
                                                       long parentModificationTimeUtc) {
        throw new UnsupportedOperationException();
    }

    private void initialize() {
        _fileStream.setPosition(0);
        FileHeader fileHeader = StreamUtilities.readStruct(FileHeader.class, _fileStream);
        if (!fileHeader.isValid()) {
            throw new dotnet4j.io.IOException("Invalid VHDX file - file signature mismatch");
        }

        _freeSpace = new FreeSpaceTable(_fileStream.getLength());

        readHeaders();

        replayLog();

        readRegionTable();

        readMetadata();

        _batStream = openRegion(RegionTable.BatGuid);
        _freeSpace.reserve(batControlledFileExtents());

        // Indicate the file is open for modification
        if (_fileStream.canWrite()) {
            _header.FileWriteGuid = UUID.randomUUID();
            writeHeader();
        }
    }

    private List<StreamExtent> batControlledFileExtents() {
        _batStream.setPosition(0);
        byte[] batData = StreamUtilities.readExact(_batStream, (int) _batStream.getLength());
        int blockSize = _metadata.getFileParameters().BlockSize;
        long chunkSize = (1L << 23) * _metadata.getLogicalSectorSize();
        int chunkRatio = (int) (chunkSize / _metadata.getFileParameters().BlockSize);
        List<StreamExtent> extents = new ArrayList<>();
        for (int i = 0; i < batData.length; i += 8) {
            long entry = EndianUtilities.toUInt64LittleEndian(batData, i);
            long filePos = ((entry >>> 20) & 0xFFF_FFFF_FFFFl) * Sizes.OneMiB;
            if (filePos != 0) {
                if (i % ((chunkRatio + 1) * 8) == chunkRatio * 8) {
                    // This is a sector bitmap block (always 1MB in size)
                    extents.add(new StreamExtent(filePos, Sizes.OneMiB));
                } else {
                    extents.add(new StreamExtent(filePos, blockSize));
                }
            }
        }
        Collections.sort(extents);
        return extents;
    }

    private void readMetadata() {
        Stream regionStream = openRegion(RegionTable.MetadataRegionGuid);
        _metadata = new Metadata(regionStream);
    }

    private void replayLog() {
        _freeSpace.reserve(_header.LogOffset, _header.LogLength);
        _logicalStream = _fileStream;
        // If log is empty, skip.
        if (_header.LogGuid.equals(EMPTY)) {
            return;
        }

        LogSequence activeLogSequence = findActiveLogSequence();
        if (activeLogSequence == null || activeLogSequence.size() == 0) {
            throw new dotnet4j.io.IOException("Unable to replay VHDX log, suspected corrupt VHDX file");
        }

        if (activeLogSequence.getHead().getFlushedFileOffset() > _logicalStream.getLength()) {
            throw new dotnet4j.io.IOException("truncated VHDX file found while replaying log");
        }

        if (activeLogSequence.size() > 1 || !activeLogSequence.getHead().getIsEmpty()) {
            // However, have seen VHDX with a non-empty log with no data to
            // replay. These are 'safe' to open.
            if (!_fileStream.canWrite()) {
                SnapshotStream replayStream = new SnapshotStream(_fileStream, Ownership.None);
                replayStream.snapshot();
                _logicalStream = replayStream;
            }

            for (LogEntry logEntry : activeLogSequence) {
                if (logEntry.getLogGuid() != _header.LogGuid)
                    throw new dotnet4j.io.IOException("Invalid log entry in VHDX log, suspected currupt VHDX file");

                if (logEntry.getIsEmpty())
                    continue;

                logEntry.replay(_logicalStream);
            }
            _logicalStream.seek(activeLogSequence.getHead().getLastFileOffset(), SeekOrigin.Begin);
        }
    }

    private LogSequence findActiveLogSequence() {

        try (Stream logStream = new CircularStream(new SubStream(_fileStream, _header.LogOffset, _header.LogLength),
                                                   Ownership.Dispose)) {
            LogSequence candidateActiveSequence = new LogSequence();
            LogEntry[] logEntry = new LogEntry[1];

            long oldTail;
            long currentTail = 0;

            do {
                oldTail = currentTail;

                logStream.setPosition(currentTail);
                LogSequence currentSequence = new LogSequence();

                while (LogEntry.tryRead(logStream, logEntry) && logEntry[0].getLogGuid().equals(_header.LogGuid) &&
                       (currentSequence.size() == 0 ||
                        logEntry[0].getSequenceNumber() == currentSequence.getHead().getSequenceNumber() + 1)) {
                    currentSequence.add(logEntry[0]);
                    logEntry[0] = null;
                }

                if (currentSequence.size() > 0 && currentSequence.contains(currentSequence.getHead().getTail()) &&
                    currentSequence.higherSequenceThan(candidateActiveSequence)) {
                    candidateActiveSequence = currentSequence;
                }

                if (currentSequence.size() == 0) {
                    currentTail += LogEntry.LogSectorSize;
                } else {
                    currentTail = currentSequence.getHead().getPosition() + LogEntry.LogSectorSize;
                }

                currentTail = currentTail % logStream.getLength();
            } while (currentTail > oldTail);

            return candidateActiveSequence;
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    private void readRegionTable() {
        _fileStream.setPosition(192 * Sizes.OneKiB);
        _regionTable = StreamUtilities.readStruct(RegionTable.class, _fileStream);
        for (RegionEntry entry : _regionTable.Regions.values()) {
            if (entry.flags == RegionFlags.Required) {
                if (!entry.guid.equals(RegionTable.BatGuid) && !entry.guid.equals(RegionTable.MetadataRegionGuid)) {
                    throw new dotnet4j.io.IOException("Invalid VHDX file - unrecognised required region: " + entry.guid);
                }
            }

            _freeSpace.reserve(entry.fileOffset, entry.getLength());
        }
    }

    private void readHeaders() {
        _freeSpace.reserve(0, Sizes.OneMiB);
        _activeHeader = 0;
        _fileStream.setPosition(64 * Sizes.OneKiB);
        VhdxHeader vhdxHeader1 = StreamUtilities.readStruct(VhdxHeader.class, _fileStream);
        if (vhdxHeader1.isValid()) {
            _header = vhdxHeader1;
            _activeHeader = 1;
        }

        _fileStream.setPosition(128 * Sizes.OneKiB);
        VhdxHeader vhdxHeader2 = StreamUtilities.readStruct(VhdxHeader.class, _fileStream);
        if (vhdxHeader2.isValid() && (_activeHeader == 0 || _header.SequenceNumber < vhdxHeader2.SequenceNumber)) {
            _header = vhdxHeader2;
            _activeHeader = 2;
        }

        if (_activeHeader == 0) {
            throw new dotnet4j.io.IOException("Invalid VHDX file - no valid VHDX headers found");
        }
    }

    private void writeHeader() {
        long otherPos;

        _header.SequenceNumber++;
        _header.calcChecksum();

        if (_activeHeader == 1) {
            _fileStream.setPosition(128 * Sizes.OneKiB);
            otherPos = 64 * Sizes.OneKiB;
        } else {
            _fileStream.setPosition(64 * Sizes.OneKiB);
            otherPos = 128 * Sizes.OneKiB;
        }

        StreamUtilities.writeStruct(_fileStream, _header);
        _fileStream.flush();

        _header.SequenceNumber++;
        _header.calcChecksum();

        _fileStream.setPosition(otherPos);
        StreamUtilities.writeStruct(_fileStream, _header);
        _fileStream.flush();
    }

    /**
     * Gets the locations of the parent file.
     *
     * @param fileLocator The file locator to use.
     * @return Array of candidate file locations.
     */
    private List<String> getParentLocations(FileLocator fileLocator) {
        if (!needsParent()) {
            throw new dotnet4j.io.IOException("Only differencing disks contain parent locations");
        }

        if (fileLocator == null) {
            // Use working directory by default
            fileLocator = new LocalFileLocator("");
        }

        List<String> paths = new ArrayList<>();
        ParentLocator locator = _metadata.getParentLocator();
        if (locator.getEntries().containsKey("relative_path")) {
            paths.add(fileLocator.resolveRelativePath(locator.getEntries().get("relative_path")));
        }

        if (locator.getEntries().containsKey("volume_path")) {
            paths.add(locator.getEntries().get("volume_path"));
        }

        if (locator.getEntries().containsKey("absolute_win32_path")) {
            paths.add(locator.getEntries().get("absolute_win32_path"));
        }

        return paths;
    }
}
