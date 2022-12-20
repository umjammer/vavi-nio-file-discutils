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

package discUtils.vhdx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import discUtils.core.FileLocator;
import discUtils.core.Geometry;
import discUtils.core.VirtualDiskExtent;
import discUtils.core.VirtualDiskLayer;
import discUtils.core.internal.LocalFileLocator;
import discUtils.core.internal.Utilities;
import discUtils.streams.AligningStream;
import discUtils.streams.CircularStream;
import discUtils.streams.MappedStream;
import discUtils.streams.SnapshotStream;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.SubStream;
import discUtils.streams.ZeroStream;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;
import vavi.util.ByteUtil;


/**
 * Represents a single .VHDX file.
 */
public final class DiskImageFile extends VirtualDiskLayer {

    private static final UUID EMPTY = new UUID(0L, 0L);

    /**
     * Which VHDX header is active.
     */
    private int activeHeader;

    /**
     * block Allocation Table for disk content.
     */
    private Stream batStream;

    /**
     * The object that can be used to locate relative file paths.
     */
    private FileLocator fileLocator;

    /**
     * The file name of this VHDX.
     */
    private String fileName;

    /**
     * The stream containing the VHDX file.
     */
    private Stream fileStream;

    /**
     * Table of all free space in the file.
     */
    private FreeSpaceTable freeSpace;

    /**
     * Value of the active VHDX header.
     */
    private VhdxHeader header;

    /**
     * The stream containing the logical VHDX content and metadata allowing for
     * log replay.
     */
    private Stream logicalStream;

    /**
     * VHDX metadata region content.
     */
    private Metadata metadata;

    /**
     * Indicates if this object controls the lifetime of the stream.
     */
    private Ownership ownership;

    /**
     * The set of VHDX regions.
     */
    private RegionTable regionTable;

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param stream The stream to interpret.
     */
    public DiskImageFile(Stream stream) {
        fileStream = stream;
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
        fileStream = stream;
        this.ownership = ownership;
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
        fileLocator = locator.getRelativeLocator(locator.getDirectoryFromPath(path));
        fileName = locator.getFileFromPath(path);
    }

    DiskImageFile(FileLocator locator, String path, FileAccess access) {
        FileShare share = access == FileAccess.Read ? FileShare.Read : FileShare.None;
        fileStream = locator.open(path, FileMode.Open, access, share);
        ownership = Ownership.Dispose;
        try {
            fileLocator = locator.getRelativeLocator(locator.getDirectoryFromPath(path));
            fileName = locator.getFileFromPath(path);
            initialize();
        } finally {
            try {
                fileStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override public long getCapacity() {
        return metadata.getDiskSize();
    }

    /**
     * Gets the extent that comprises this file.
     */
    @Override public List<VirtualDiskExtent> getExtents() {
        List<VirtualDiskExtent> result = new ArrayList<>();
        result.add(new DiskExtent(this));
        return result;
    }

    /**
     * Gets the full path to this disk layer, or empty string.
     */
    @Override public String getFullPath() {
        if (fileLocator != null && fileName != null) {
            return fileLocator.getFullPath(fileName);
        }

        return "";
    }

    /**
     * Gets the geometry of the virtual disk.
     */
    @Override public Geometry getGeometry() {
        return Geometry.fromCapacity(getCapacity(), metadata.getLogicalSectorSize());
    }

    /**
     * Gets detailed information about the VHDX file.
     */
    public DiskImageFileInfo getInformation() {
        fileStream.position(0);
        FileHeader fileHeader = StreamUtilities.readStruct(FileHeader.class, fileStream);

        fileStream.position(64 * Sizes.OneKiB);
        VhdxHeader vhdxHeader1 = StreamUtilities.readStruct(VhdxHeader.class, fileStream);

        fileStream.position(128 * Sizes.OneKiB);
        VhdxHeader vhdxHeader2 = StreamUtilities.readStruct(VhdxHeader.class, fileStream);

        LogSequence activeLogSequence = findActiveLogSequence();

        return new DiskImageFileInfo(fileHeader, vhdxHeader1, vhdxHeader2, regionTable, metadata, activeLogSequence);
    }

    /**
     * Gets a value indicating if the layer only stores meaningful sectors.
     */
    @Override public boolean isSparse() {
        return true;
    }

    /**
     * Gets the logical sector size of the virtual disk.
     */
    public long getLogicalSectorSize() {
        return metadata.getLogicalSectorSize();
    }

    /**
     * Gets a value indicating whether the file is a differencing disk.
     */
    public boolean needsParent() {
        return metadata.getFileParameters().flags.contains(FileParametersFlags.HasParent);
    }

    /**
     * Gets the unique id of the parent disk.
     */
    public UUID getParentUniqueId() {
        if (!metadata.getFileParameters().flags.contains(FileParametersFlags.HasParent)) {
            return EMPTY;
        }

        if (metadata.getParentLocator().getEntries().containsKey("parent_linkage")) {
            return UUID.fromString(metadata.getParentLocator().getEntries().get("parent_linkage"));
        }

        return EMPTY;
    }

    public FileLocator getRelativeFileLocator() {
        return fileLocator;
    }

    long getStoredSize() {
        return fileStream.getLength();
    }

    /**
     * Gets the unique id of this file.
     */
    public UUID getUniqueId() {
        return header.dataWriteGuid;
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
     * @return A stream containing the region data.regions are an extension
     *         mechanism in VHDX - with some regions defined by the VHDX
     *         specification to hold metadata and the block allocation data.
     */
    public Stream openRegion(UUID region) {
        RegionEntry metadataRegion = regionTable.regions.get(region);
        return new SubStream(logicalStream, metadataRegion.fileOffset, metadataRegion.getLength());
    }

    /**
     * Opens the content of the disk image file as a stream.
     *
     * @param parent The parent file's content (if any).
     * @param ownsParent Whether the created stream assumes ownership of parent
     *            stream.
     * @return The new content stream.
     */
    @Override public SparseStream openContent(SparseStream parent, Ownership ownsParent) {
        return doOpenContent(parent, ownsParent);
    }

    /**
     * Gets the location of the parent file, given a base path.
     *
     * @return lsit of candidate file locations.
     */
    @Override public List<String> getParentLocations() {
        return getParentLocations(fileLocator);
    }

    /**
     * Gets the location of the parent file, given a base path.
     *
     * @param basePath The full path to this file.
     * @return list of candidate file locations.
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
        String fullPath = this.fileLocator.getFullPath(fileName);
        String relativePath = fileLocator.makeRelativePath(this.fileLocator, fileName);
        long lastWriteTime = this.fileLocator.getLastWriteTimeUtc(fileName);
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

        ContentStream contentStream = new ContentStream(SparseStream.fromStream(logicalStream, Ownership.None),
                                                        fileStream.canWrite(),
                batStream,
                freeSpace,
                metadata,
                                                        getCapacity(),
                                                        theParent,
                                                        theOwnership);
        return new AligningStream(contentStream, Ownership.Dispose, metadata.getLogicalSectorSize());
    }

    /**
     * Disposes of underlying resources.
     *
     * @throws IOException when an io error occurs
     */
    @Override public void close() throws IOException {
        if (logicalStream != fileStream && logicalStream != null) {
            logicalStream.close();
        }

        logicalStream = null;
        if (ownership == Ownership.Dispose && fileStream != null) {
            fileStream.close();
        }

        fileStream = null;
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
        fileHeader.creator = ".NET DiscUtils";

        long fileEnd = Sizes.OneMiB;

        VhdxHeader header1 = new VhdxHeader();
        header1.sequenceNumber = 0;
        header1.fileWriteGuid = UUID.randomUUID();
        header1.dataWriteGuid = UUID.randomUUID();
        header1.logGuid = EMPTY;
        header1.logVersion = 0;
        header1.version = 1;
        header1.logLength = (int) Sizes.OneMiB;
        header1.logOffset = fileEnd;
        header1.calcChecksum();

        fileEnd += header1.logLength;

        VhdxHeader header2 = new VhdxHeader(header1);
        header2.sequenceNumber = 1;
        header2.calcChecksum();

        RegionTable regionTable = new RegionTable();

        RegionEntry metadataRegion = new RegionEntry();
        metadataRegion.guid = RegionEntry.MetadataRegionGuid;
        metadataRegion.fileOffset = fileEnd;
        metadataRegion.setLength((int) Sizes.OneMiB);
        metadataRegion.flags = RegionFlags.Required;
        regionTable.regions.put(metadataRegion.guid, metadataRegion);

        fileEnd += metadataRegion.getLength();

        RegionEntry batRegion = new RegionEntry();
        batRegion.guid = RegionEntry.BatGuid;
        batRegion.fileOffset = 3 * Sizes.OneMiB;
        batRegion.setLength((int) MathUtilities.roundUp(totalBatEntriesDynamic * 8, Sizes.OneMiB));
        batRegion.flags = RegionFlags.Required;
        regionTable.regions.put(batRegion.guid, batRegion);

        fileEnd += batRegion.getLength();

        stream.position(0);
        StreamUtilities.writeStruct(stream, fileHeader);

        stream.position(64 * Sizes.OneKiB);
        StreamUtilities.writeStruct(stream, header1);

        stream.position(128 * Sizes.OneKiB);
        StreamUtilities.writeStruct(stream, header2);

        stream.position(192 * Sizes.OneKiB);
        StreamUtilities.writeStruct(stream, regionTable);

        stream.position(256 * Sizes.OneKiB);
        StreamUtilities.writeStruct(stream, regionTable);

        // Set stream to min size
        stream.position(fileEnd - 1);
        stream.writeByte((byte) 0);

        // Metadata
        FileParameters fileParams = new FileParameters();
        fileParams.blockSize = (int) blockSize;
        fileParams.flags = EnumSet.of(FileParametersFlags.None);
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
        fileStream.position(0);
        FileHeader fileHeader = StreamUtilities.readStruct(FileHeader.class, fileStream);
        if (!fileHeader.isValid()) {
            throw new dotnet4j.io.IOException("Invalid VHDX file - file signature mismatch");
        }

        freeSpace = new FreeSpaceTable(fileStream.getLength());

        readHeaders();

        replayLog();

        readRegionTable();

        readMetadata();

        batStream = openRegion(RegionTable.BatGuid);
        freeSpace.reserve(batControlledFileExtents());

        // Indicate the file is open for modification
        if (fileStream.canWrite()) {
            header.fileWriteGuid = UUID.randomUUID();
            writeHeader();
        }
    }

    private List<StreamExtent> batControlledFileExtents() {
        batStream.position(0);
        byte[] batData = StreamUtilities.readExact(batStream, (int) batStream.getLength());

        int blockSize = metadata.getFileParameters().blockSize;
        long chunkSize = (1L << 23) * metadata.getLogicalSectorSize();
        int chunkRatio = (int) (chunkSize / metadata.getFileParameters().blockSize);

        List<StreamExtent> extents = new ArrayList<>();
        for (int i = 0; i < batData.length; i += 8) {
            long entry = ByteUtil.readLeLong(batData, i);
            long filePos = ((entry >>> 20) & 0xFFF_FFFF_FFFFL) * Sizes.OneMiB;
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
        metadata = new Metadata(regionStream);
    }

    private void replayLog() {
        freeSpace.reserve(header.logOffset, header.logLength);

        logicalStream = fileStream;

        // If log is empty, skip.
        if (header.logGuid.equals(EMPTY)) {
            return;
        }

        LogSequence activeLogSequence = findActiveLogSequence();

        if (activeLogSequence == null || activeLogSequence.size() == 0) {
            throw new dotnet4j.io.IOException("Unable to replay VHDX log, suspected corrupt VHDX file");
        }

        if (activeLogSequence.getHead().getFlushedFileOffset() > logicalStream.getLength()) {
            throw new dotnet4j.io.IOException("truncated VHDX file found while replaying log");
        }

        if (activeLogSequence.size() > 1 || !activeLogSequence.getHead().isEmpty()) {
            // However, have seen VHDX with a non-empty log with no data to
            // replay. These are 'safe' to open.
            if (!fileStream.canWrite()) {
                SnapshotStream replayStream = new SnapshotStream(fileStream, Ownership.None);
                replayStream.snapshot();
                logicalStream = replayStream;
            }
            for (LogEntry logEntry : activeLogSequence) {
                if (!logEntry.getLogGuid().equals(header.logGuid))
                    throw new dotnet4j.io.IOException("Invalid log entry in VHDX log, suspected currupt VHDX file");
                if (logEntry.isEmpty())
                    continue;
                logEntry.replay(logicalStream);
            }
            logicalStream.seek(activeLogSequence.getHead().getLastFileOffset(), SeekOrigin.Begin);
        }
    }

    private LogSequence findActiveLogSequence() {
        try (Stream logStream = new CircularStream(new SubStream(fileStream, header.logOffset, header.logLength),
                                                   Ownership.Dispose)) {
            LogSequence candidateActiveSequence = new LogSequence();
            LogEntry[] logEntry = new LogEntry[1];

            long oldTail;
            long currentTail = 0;

            do {
                oldTail = currentTail;

                logStream.position(currentTail);
                LogSequence currentSequence = new LogSequence();

                while (LogEntry.tryRead(logStream, logEntry) && logEntry[0].getLogGuid().equals(header.logGuid) &&
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
        fileStream.position(192 * Sizes.OneKiB);
        regionTable = StreamUtilities.readStruct(RegionTable.class, fileStream);
        for (RegionEntry entry : regionTable.regions.values()) {
            if (entry.flags == RegionFlags.Required) {
                if (!entry.guid.equals(RegionTable.BatGuid) && !entry.guid.equals(RegionTable.MetadataRegionGuid)) {
                    throw new dotnet4j.io.IOException("Invalid VHDX file - unrecognised required region: " + entry.guid);
                }
            }

            freeSpace.reserve(entry.fileOffset, entry.getLength());
        }
    }

    private void readHeaders() {
        freeSpace.reserve(0, Sizes.OneMiB);

        activeHeader = 0;

        fileStream.position(64 * Sizes.OneKiB);
        VhdxHeader vhdxHeader1 = StreamUtilities.readStruct(VhdxHeader.class, fileStream);
        if (vhdxHeader1.isValid()) {
            header = vhdxHeader1;
            activeHeader = 1;
        }

        fileStream.position(128 * Sizes.OneKiB);
        VhdxHeader vhdxHeader2 = StreamUtilities.readStruct(VhdxHeader.class, fileStream);
        if (vhdxHeader2.isValid() && (activeHeader == 0 || header.sequenceNumber < vhdxHeader2.sequenceNumber)) {
            header = vhdxHeader2;
            activeHeader = 2;
        }

        if (activeHeader == 0) {
            throw new dotnet4j.io.IOException("Invalid VHDX file - no valid VHDX headers found");
        }
    }

    private void writeHeader() {
        long otherPos;

        header.sequenceNumber++;
        header.calcChecksum();

        if (activeHeader == 1) {
            fileStream.position(128 * Sizes.OneKiB);
            otherPos = 64 * Sizes.OneKiB;
        } else {
            fileStream.position(64 * Sizes.OneKiB);
            otherPos = 128 * Sizes.OneKiB;
        }

        StreamUtilities.writeStruct(fileStream, header);
        fileStream.flush();

        header.sequenceNumber++;
        header.calcChecksum();

        fileStream.position(otherPos);
        StreamUtilities.writeStruct(fileStream, header);
        fileStream.flush();
    }

    /**
     * Gets the locations of the parent file.
     *
     * @param fileLocator The file locator to use.
     * @return list of candidate file locations.
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

        ParentLocator locator = metadata.getParentLocator();

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
