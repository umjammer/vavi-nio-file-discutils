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

package discUtils.core.partitions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import discUtils.core.Geometry;
import discUtils.core.VirtualDisk;
import discUtils.core.internal.Crc32Algorithm;
import discUtils.core.internal.Crc32LittleEndian;
import discUtils.core.internal.Utilities;
import discUtils.streams.SparseStream;
import discUtils.streams.SubStream;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


/**
 * Represents a GUID Partition Table.
 */
public final class GuidPartitionTable extends PartitionTable {

    private Stream diskData;

    private Geometry diskGeometry;

    private byte[] entryBuffer;

    private GptHeader primaryHeader;

    private GptHeader secondaryHeader;

    /**
     * Initializes a new instance of the GuidPartitionTable class.
     *
     * @param disk The disk containing the partition table.
     */
    public GuidPartitionTable(VirtualDisk disk) {
        init(disk.getContent(), disk.getGeometry());
    }

    /**
     * Initializes a new instance of the GuidPartitionTable class.
     *
     * @param disk The stream containing the disk data.
     * @param diskGeometry The geometry of the disk.
     */
    public GuidPartitionTable(Stream disk, Geometry diskGeometry) {
        init(disk, diskGeometry);
    }

    /**
     * Gets the unique GPT identifier for this disk.
     */
    @Override public UUID getDiskGuid() {
        return primaryHeader.diskGuid;
    }

    /**
     * Gets the first sector of the disk available to hold partitions.
     */
    public long getFirstUsableSector() {
        return primaryHeader.firstUsable;
    }

    /**
     * Gets the last sector of the disk available to hold partitions.
     */
    public long getLastUsableSector() {
        return primaryHeader.lastUsable;
    }

    /**
     * Gets a collection of the partitions for storing Operating System
     * file-systems.
     */
    @Override public List<PartitionInfo> getPartitions() {
        return Collections.unmodifiableList(getAllEntries().stream().map(e -> new GuidPartitionInfo(this, e)).collect(Collectors.toList()));
    }

    /**
     * Creates a new partition table on a disk.
     *
     * @param disk The disk to initialize.
     * @return An object to access the newly created partition table.
     */
    public static GuidPartitionTable initialize(VirtualDisk disk) {
        return initialize(disk.getContent(), disk.getGeometry());
    }

    /**
     * Creates a new partition table on a disk.
     *
     * @param disk The stream containing the disk data.
     * @param diskGeometry The geometry of the disk.
     * @return An object to access the newly created partition table.
     */
    public static GuidPartitionTable initialize(Stream disk, Geometry diskGeometry) {
        // Create the protective MBR partition record.
        BiosPartitionTable pt = BiosPartitionTable.initialize(disk, diskGeometry);
        pt.createPrimaryByCylinder(0, diskGeometry.getCylinders() - 1, BiosPartitionTypes.GptProtective, false);
        // Create the GPT headers, and blank-out the entry areas
        final int EntryCount = 128;
        final int EntrySize = 128;
        int entrySectors = (EntryCount * EntrySize + diskGeometry.getBytesPerSector() - 1) / diskGeometry.getBytesPerSector();
        byte[] entriesBuffer = new byte[EntryCount * EntrySize];
        // Prepare primary header
        GptHeader header = new GptHeader(diskGeometry.getBytesPerSector());
        header.headerLba = 1;
        header.alternateHeaderLba = disk.getLength() / diskGeometry.getBytesPerSector() - 1;
        header.firstUsable = header.headerLba + entrySectors + 1;
        header.lastUsable = header.alternateHeaderLba - entrySectors - 1;
        header.diskGuid = UUID.randomUUID();
        header.partitionEntriesLba = 2;
        header.partitionEntryCount = EntryCount;
        header.partitionEntrySize = EntrySize;
        header.entriesCrc = calcEntriesCrc(entriesBuffer);
        // Write the primary header
        byte[] headerBuffer = new byte[diskGeometry.getBytesPerSector()];
        header.writeTo(headerBuffer, 0);
        disk.position(header.headerLba * diskGeometry.getBytesPerSector());
        disk.write(headerBuffer, 0, headerBuffer.length);
        // Calc alternate header
        header.headerLba = header.alternateHeaderLba;
        header.alternateHeaderLba = 1;
        header.partitionEntriesLba = header.headerLba - entrySectors;
        // Write the alternate header
        header.writeTo(headerBuffer, 0);
        disk.position(header.headerLba * diskGeometry.getBytesPerSector());
        disk.write(headerBuffer, 0, headerBuffer.length);
        return new GuidPartitionTable(disk, diskGeometry);
    }

    /**
     * Creates a new partition table on a disk containing a single partition.
     *
     * @param disk The disk to initialize.
     * @param type The partition type for the single partition.
     * @return An object to access the newly created partition table.
     */
    public static GuidPartitionTable initialize(VirtualDisk disk, WellKnownPartitionType type) {
        GuidPartitionTable pt = initialize(disk);
        pt.create(type, true);
        return pt;
    }

    /**
     * Creates a new partition that encompasses the entire disk.
     *
     * The partition table must be empty before this method is called, otherwise
     * IOException is thrown.
     *
     * @param type The partition type.
     * @param active Whether the partition is active (bootable).
     * @return The index of the partition.
     */
    @Override public int create(WellKnownPartitionType type, boolean active) {
        List<GptEntry> allEntries = new ArrayList<>(getAllEntries());
        establishReservedPartition(allEntries);
        // Fill the rest of the disk with the requested partition
        long start = firstAvailableSector(allEntries);
        long end = findLastFreeSector(start, allEntries);
        return create(start, end, GuidPartitionTypes.convert(type), 0, "Data Partition");
    }

    /**
     * Creates a new primary partition with a target size.
     *
     * @param size The target size (in bytes).
     * @param type The partition type.
     * @param active Whether the partition is active (bootable).
     * @return The index of the new partition.
     */
    @Override public int create(long size, WellKnownPartitionType type, boolean active) {
        if (size < diskGeometry.getBytesPerSector()) {
            throw new IndexOutOfBoundsException("size must be at least one sector");
        }

        long sectorLength = size / diskGeometry.getBytesPerSector();
        long start = findGap(size / diskGeometry.getBytesPerSector(), 1);
        return create(start, start + sectorLength - 1, GuidPartitionTypes.convert(type), 0, "Data Partition");
    }

    /**
     * Creates a new aligned partition that encompasses the entire disk.
     *
     * The partition table must be empty before this method is called, otherwise
     * IOException is thrown.
     *
     * Traditionally partitions were aligned to the physical structure of the
     * underlying disk, however with modern storage greater efficiency is
     * acheived by aligning partitions on large values that are a power of two.
     *
     * @param type The partition type.
     * @param active Whether the partition is active (bootable).
     * @param alignment The alignment (in bytes).
     * @return The index of the partition.
     */
    @Override public int createAligned(WellKnownPartitionType type, boolean active, int alignment) {
        if (alignment % diskGeometry.getBytesPerSector() != 0) {
            throw new IllegalArgumentException("Alignment is not a multiple of the sector size");
        }

        List<GptEntry> allEntries = new ArrayList<>(getAllEntries());
        establishReservedPartition(allEntries);
        // Fill the rest of the disk with the requested partition
        long start = MathUtilities.roundUp(firstAvailableSector(allEntries), alignment / diskGeometry.getBytesPerSector());
        long end = MathUtilities.roundDown(findLastFreeSector(start, allEntries) + 1,
                                           alignment / diskGeometry.getBytesPerSector());
        if (end <= start) {
            throw new dotnet4j.io.IOException("No available space");
        }

        return create(start, end - 1, GuidPartitionTypes.convert(type), 0, "Data Partition");
    }

    /**
     * Creates a new aligned partition with a target size.
     *
     * @param size The target size (in bytes).
     * @param type The partition type.
     * @param active Whether the partition is active (bootable).
     * @param alignment The alignment (in bytes).
     * @return The index of the new partition. Traditionally partitions were
     *         aligned to the physical structure of the underlying disk, however
     *         with modern storage greater efficiency is achieved by aligning
     *         partitions on large values that are a power of two.
     */
    @Override public int createAligned(long size, WellKnownPartitionType type, boolean active, int alignment) {
        if (size < diskGeometry.getBytesPerSector()) {
            throw new IndexOutOfBoundsException("size must be at least one sector");
        }

        if (alignment % diskGeometry.getBytesPerSector() != 0) {
            throw new IllegalArgumentException("Alignment is not a multiple of the sector size");
        }

        if (size % alignment != 0) {
            throw new IllegalArgumentException("Size is not a multiple of the alignment");
        }

        long sectorLength = size / diskGeometry.getBytesPerSector();
        long start = findGap(size / diskGeometry.getBytesPerSector(), alignment / diskGeometry.getBytesPerSector());
        return create(start, start + sectorLength - 1, GuidPartitionTypes.convert(type), 0, "Data Partition");
    }

    /**
     * Creates a new GUID partition on the disk.
     *
     * @param startSector The first sector of the partition.
     * @param endSector The last sector of the partition.
     * @param type The partition type.
     * @param attributes The partition attributes.
     * @param name The name of the partition.
     * @return The index of the new partition.No checking is performed on the
     *         parameters, the caller is responsible for ensuring that the
     *         partition does not overlap other partitions.
     */
    public int create(long startSector, long endSector, UUID type, long attributes, String name) {
        GptEntry newEntry = createEntry(startSector, endSector, type, attributes, name);
        return getEntryIndex(newEntry.identity);
    }

    /**
     * Deletes a partition at a given index.
     *
     * @param index The index of the partition.
     */
    @Override public void delete(int index) {
        int offset = getPartitionOffset(index);
        Arrays.fill(entryBuffer, offset, offset + primaryHeader.partitionEntrySize, (byte) 0);
        write();
    }

    public SparseStream open(GptEntry entry) {
        long start = entry.firstUsedLogicalBlock * diskGeometry.getBytesPerSector();
        long end = (entry.lastUsedLogicalBlock + 1) * diskGeometry.getBytesPerSector();
        return new SubStream(diskData, start, end - start);
    }

    private static int calcEntriesCrc(byte[] buffer) {
        return Crc32LittleEndian.compute(Crc32Algorithm.Common, buffer, 0, buffer.length);
    }

    private static <T> int countEntries(Collection<T> values, Predicate<T> pred) {
        int count = 0;
        for (T val : values) {
            if (pred.test(val)) {
                ++count;
            }

        }
        return count;
    }

    private void init(Stream disk, Geometry diskGeometry) {
        BiosPartitionTable bpt;
        try {
            bpt = new BiosPartitionTable(disk, diskGeometry);
        } catch (dotnet4j.io.IOException ioe) {
            throw new dotnet4j.io.IOException("Invalid GPT disk, protective MBR table not present or invalid", ioe);
        }

        if (bpt.getCount() != 1 || bpt.get(0).getBiosType() != BiosPartitionTypes.GptProtective) {
            throw new dotnet4j.io.IOException("Invalid GPT disk, protective MBR table is not valid");
        }

        diskData = disk;
        this.diskGeometry = diskGeometry;
        disk.position(diskGeometry.getBytesPerSector());
        byte[] sector = StreamUtilities.readExact(disk, diskGeometry.getBytesPerSector());
        primaryHeader = new GptHeader(diskGeometry.getBytesPerSector());
        if (!primaryHeader.readFrom(sector, 0) || !readEntries(primaryHeader)) {
            disk.position(disk.getLength() - diskGeometry.getBytesPerSector());
            disk.read(sector, 0, sector.length);
            secondaryHeader = new GptHeader(diskGeometry.getBytesPerSector());
            if (!secondaryHeader.readFrom(sector, 0) || !readEntries(secondaryHeader)) {
                throw new dotnet4j.io.IOException("No valid GUID Partition Table found");
            }

            // Generate from the primary table from the secondary one
            primaryHeader = new GptHeader(secondaryHeader);
            primaryHeader.headerLba = secondaryHeader.alternateHeaderLba;
            primaryHeader.alternateHeaderLba = secondaryHeader.headerLba;
            primaryHeader.partitionEntriesLba = 2;
            // If the disk is writeable, fix up the primary partition table
            // based on the
            // (valid) secondary table.
            if (disk.canWrite()) {
                writePrimaryHeader();
            }

        }

        if (secondaryHeader == null) {
            secondaryHeader = new GptHeader(diskGeometry.getBytesPerSector());
            disk.position(disk.getLength() - diskGeometry.getBytesPerSector());
            disk.read(sector, 0, sector.length);
            if (!secondaryHeader.readFrom(sector, 0) || !readEntries(secondaryHeader)) {
                // Generate from the secondary table from the primary one
                secondaryHeader = new GptHeader(primaryHeader);
                secondaryHeader.headerLba = secondaryHeader.alternateHeaderLba;
                secondaryHeader.alternateHeaderLba = secondaryHeader.headerLba;
                secondaryHeader.partitionEntriesLba = secondaryHeader.headerLba - MathUtilities
                        .roundUp(secondaryHeader.partitionEntryCount * secondaryHeader.partitionEntrySize,
                                 diskGeometry.getBytesPerSector());
                // If the disk is writeable, fix up the secondary partition
                // table based on the
                // (valid) primary table.
                if (disk.canWrite()) {
                    writeSecondaryHeader();
                }
            }
        }
    }

    private void establishReservedPartition(List<GptEntry> allEntries) {
        // If no MicrosoftReserved partition, and no Microsoft Data partitions,
        // and the
        // disk
        // has a 'reasonable' size free, create a Microsoft Reserved partition.
        if (countEntries(allEntries, e -> e.partitionType.equals(GuidPartitionTypes.MicrosoftReserved)) == 0 &&
            countEntries(allEntries, e -> e.partitionType.equals(GuidPartitionTypes.WindowsBasicData)) == 0 &&
            diskGeometry.getCapacity() > 512 * 1024 * 1024) {
            long reservedStart = firstAvailableSector(allEntries);
            long reservedEnd = findLastFreeSector(reservedStart, allEntries);
            if ((reservedEnd - reservedStart + 1) * diskGeometry.getBytesPerSector() > 512 * 1024 * 1024) {
                long size = (diskGeometry.getCapacity() < 16 * 1024L * 1024 * 1024 ? 32 : 128) * 1024 * 1024;
                reservedEnd = reservedStart + size / diskGeometry.getBytesPerSector() - 1;
                int reservedOffset = getFreeEntryOffset();
                GptEntry newReservedEntry = new GptEntry();
                newReservedEntry.partitionType = GuidPartitionTypes.MicrosoftReserved;
                newReservedEntry.identity = UUID.randomUUID();
                newReservedEntry.firstUsedLogicalBlock = reservedStart;
                newReservedEntry.lastUsedLogicalBlock = reservedEnd;
                newReservedEntry.attributes = 0;
                newReservedEntry.name = "Microsoft reserved partition";
                newReservedEntry.writeTo(entryBuffer, reservedOffset);
                allEntries.add(newReservedEntry);
            }
        }
    }

    private GptEntry createEntry(long startSector, long endSector, UUID type, long attributes, String name) {
        if (endSector < startSector) {
            throw new IllegalArgumentException("The end sector is before the start sector");
        }

        int offset = getFreeEntryOffset();
        GptEntry newEntry = new GptEntry();
        newEntry.partitionType = type;
        newEntry.identity = UUID.randomUUID();
        newEntry.firstUsedLogicalBlock = startSector;
        newEntry.lastUsedLogicalBlock = endSector;
        newEntry.attributes = attributes;
        newEntry.name = name;
        newEntry.writeTo(entryBuffer, offset);
        // Commit changes to disk
        write();
        return newEntry;
    }

    private long findGap(long numSectors, long alignmentSectors) {
        List<GptEntry> list = getAllEntries();
        Collections.sort(list);
        long startSector = MathUtilities.roundUp(primaryHeader.firstUsable, alignmentSectors);
        for (GptEntry entry : list) {
            if (!Utilities.rangesOverlap(startSector,
                                         startSector + numSectors - 1,
                                         entry.firstUsedLogicalBlock,
                                         entry.lastUsedLogicalBlock)) {
                break;
            }

            startSector = MathUtilities.roundUp(entry.lastUsedLogicalBlock + 1, alignmentSectors);
        }
        if (diskGeometry.getTotalSectorsLong() - startSector < numSectors) {
            throw new dotnet4j.io.IOException(String.format("Unable to find free space of %d sectors", numSectors));
        }

        return startSector;
    }

    private long firstAvailableSector(List<GptEntry> allEntries) {
        long start = primaryHeader.firstUsable;
        for (GptEntry entry : allEntries) {
            if (entry.lastUsedLogicalBlock >= start) {
                start = entry.lastUsedLogicalBlock + 1;
            }

        }
        return start;
    }

    private long findLastFreeSector(long start, List<GptEntry> allEntries) {
        long end = primaryHeader.lastUsable;
        for (GptEntry entry : allEntries) {
            if (entry.lastUsedLogicalBlock > start && entry.firstUsedLogicalBlock <= end) {
                end = entry.firstUsedLogicalBlock - 1;
            }
        }
        return end;
    }

    private void write() {
        writePrimaryHeader();
        writeSecondaryHeader();
    }

    private void writePrimaryHeader() {
        byte[] buffer = new byte[diskGeometry.getBytesPerSector()];
        primaryHeader.entriesCrc = calcEntriesCrc();
        primaryHeader.writeTo(buffer, 0);
        diskData.position(diskGeometry.getBytesPerSector());
        diskData.write(buffer, 0, buffer.length);
        diskData.position(2L * diskGeometry.getBytesPerSector());
        diskData.write(entryBuffer, 0, entryBuffer.length);
    }

    private void writeSecondaryHeader() {
        byte[] buffer = new byte[diskGeometry.getBytesPerSector()];
        secondaryHeader.entriesCrc = calcEntriesCrc();
        secondaryHeader.writeTo(buffer, 0);
        diskData.position(diskData.getLength() - diskGeometry.getBytesPerSector());
        diskData.write(buffer, 0, buffer.length);
        diskData.position(secondaryHeader.partitionEntriesLba * diskGeometry.getBytesPerSector());
        diskData.write(entryBuffer, 0, entryBuffer.length);
    }

    private boolean readEntries(GptHeader header) {
        diskData.position(header.partitionEntriesLba * diskGeometry.getBytesPerSector());
        entryBuffer = StreamUtilities.readExact(diskData, header.partitionEntrySize * header.partitionEntryCount);
        if (header.entriesCrc != calcEntriesCrc()) {
            return false;
        }

        return true;
    }

    private int calcEntriesCrc() {
        return Crc32LittleEndian.compute(Crc32Algorithm.Common, entryBuffer, 0, entryBuffer.length);
    }

    private List<GptEntry> getAllEntries() {
        List<GptEntry> result = new ArrayList<>();
        for (int i = 0; i < primaryHeader.partitionEntryCount; ++i) {
            GptEntry entry = new GptEntry();
            entry.readFrom(entryBuffer, i * primaryHeader.partitionEntrySize);
            if (!entry.partitionType.equals(EMPTY)) {
                result.add(entry);
            }
        }
        return result;
    }

    private int getPartitionOffset(int index) {
        boolean found = false;
        int entriesSoFar = 0;
        int position = 0;
        while (!found && position < primaryHeader.partitionEntryCount) {
            GptEntry entry = new GptEntry();
            entry.readFrom(entryBuffer, position * primaryHeader.partitionEntrySize);
            if (!entry.partitionType.equals(EMPTY)) {
                if (index == entriesSoFar) {
                    found = true;
                    break;
                }

                entriesSoFar++;
            }

            position++;
        }
        if (found) {
            return position * primaryHeader.partitionEntrySize;
        }

        throw new dotnet4j.io.IOException(String.format("No such partition: %d", index));
    }

    private int getEntryIndex(UUID identity) {
        int index = 0;
        for (int i = 0; i < primaryHeader.partitionEntryCount; ++i) {
            GptEntry entry = new GptEntry();
            entry.readFrom(entryBuffer, i * primaryHeader.partitionEntrySize);
            if (entry.identity.equals(identity)) {
                return index;
            }

            if (!entry.partitionType.equals(EMPTY)) {
                index++;
            }
        }
        throw new dotnet4j.io.IOException("No such partition");
    }

    private int getFreeEntryOffset() {
        for (int i = 0; i < primaryHeader.partitionEntryCount; ++i) {
            GptEntry entry = new GptEntry();
            entry.readFrom(entryBuffer, i * primaryHeader.partitionEntrySize);
            if (entry.partitionType.equals(EMPTY)) {
                return i * primaryHeader.partitionEntrySize;
            }
        }
        throw new dotnet4j.io.IOException("No free partition entries available");
    }
}
