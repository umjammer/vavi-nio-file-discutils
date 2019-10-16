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

package DiscUtils.Core.Partitions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import DiscUtils.Core.Geometry;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.Internal.Crc32Algorithm;
import DiscUtils.Core.Internal.Crc32LittleEndian;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * Represents a GUID Partition Table.
 */
public final class GuidPartitionTable extends PartitionTable {
    private static final UUID EMPTY = new UUID(0L, 0L);

    private Stream _diskData;

    private Geometry _diskGeometry;

    private byte[] _entryBuffer;

    private GptHeader _primaryHeader;

    private GptHeader _secondaryHeader;

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
    public UUID getDiskGuid() {
        return _primaryHeader.DiskGuid;
    }

    /**
     * Gets the first sector of the disk available to hold partitions.
     */
    public long getFirstUsableSector() {
        return _primaryHeader.FirstUsable;
    }

    /**
     * Gets the last sector of the disk available to hold partitions.
     */
    public long getLastUsableSector() {
        return _primaryHeader.LastUsable;
    }

    /**
     * Gets a collection of the partitions for storing Operating System
     * file-systems.
     */
    public List<PartitionInfo> getPartitions() {
        return Collections.unmodifiableList(getAllEntries().stream().map(e -> {
            return new GuidPartitionInfo(this, e);
        }).collect(Collectors.toList()));
    }

    /**
     * Creates a new partition table on a disk.
     *
     * @param disk The disk to initialize.
     * @return An object to access the newly created partition table.
     */
    public static GuidPartitionTable initialize(VirtualDisk disk) throws IOException {
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
        header.HeaderLba = 1;
        header.AlternateHeaderLba = disk.getLength() / diskGeometry.getBytesPerSector() - 1;
        header.FirstUsable = header.HeaderLba + entrySectors + 1;
        header.LastUsable = header.AlternateHeaderLba - entrySectors - 1;
        header.DiskGuid = UUID.randomUUID();
        header.PartitionEntriesLba = 2;
        header.PartitionEntryCount = EntryCount;
        header.PartitionEntrySize = EntrySize;
        header.EntriesCrc = calcEntriesCrc(entriesBuffer);
        // Write the primary header
        byte[] headerBuffer = new byte[diskGeometry.getBytesPerSector()];
        header.writeTo(headerBuffer, 0);
        disk.setPosition(header.HeaderLba * diskGeometry.getBytesPerSector());
        disk.write(headerBuffer, 0, headerBuffer.length);
        // Calc alternate header
        header.HeaderLba = header.AlternateHeaderLba;
        header.AlternateHeaderLba = 1;
        header.PartitionEntriesLba = header.HeaderLba - entrySectors;
        // Write the alternate header
        header.writeTo(headerBuffer, 0);
        disk.setPosition(header.HeaderLba * diskGeometry.getBytesPerSector());
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
    public static GuidPartitionTable initialize(VirtualDisk disk, WellKnownPartitionType type) throws IOException {
        GuidPartitionTable pt = initialize(disk);
        pt.create(type, true);
        return pt;
    }

    /**
     * Creates a new partition that encompasses the entire disk.
     *
     * @param type The partition type.
     * @param active Whether the partition is active (bootable).
     * @return The index of the partition.The partition table must be empty
     *         before this method is called,
     *         otherwise IOException is thrown.
     */
    public int create(WellKnownPartitionType type, boolean active) {
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
    public int create(long size, WellKnownPartitionType type, boolean active) {
        if (size < _diskGeometry.getBytesPerSector()) {
            throw new IndexOutOfBoundsException("size must be at least one sector");
        }

        long sectorLength = size / _diskGeometry.getBytesPerSector();
        long start = findGap(size / _diskGeometry.getBytesPerSector(), 1);
        return create(start, start + sectorLength - 1, GuidPartitionTypes.convert(type), 0, "Data Partition");
    }

    /**
     * Creates a new aligned partition that encompasses the entire disk.
     *
     * @param type The partition type.
     * @param active Whether the partition is active (bootable).
     * @param alignment The alignment (in bytes).
     * @return The index of the partition.The partition table must be empty
     *         before this method is called,
     *         otherwise IOException is thrown.
     *         Traditionally partitions were aligned to the physical structure
     *         of the underlying disk,
     *         however with modern storage greater efficiency is acheived by
     *         aligning partitions on
     *         large values that are a power of two.
     */
    public int createAligned(WellKnownPartitionType type, boolean active, int alignment) {
        if (alignment % _diskGeometry.getBytesPerSector() != 0) {
            throw new IllegalArgumentException("Alignment is not a multiple of the sector size");
        }

        List<GptEntry> allEntries = new ArrayList<>(getAllEntries());
        establishReservedPartition(allEntries);
        // Fill the rest of the disk with the requested partition
        long start = MathUtilities.roundUp(firstAvailableSector(allEntries), alignment / _diskGeometry.getBytesPerSector());
        long end = MathUtilities.roundDown(findLastFreeSector(start, allEntries) + 1,
                                           alignment / _diskGeometry.getBytesPerSector());
        if (end <= start) {
            throw new moe.yo3explorer.dotnetio4j.IOException("No available space");
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
     * @return The index of the new partition.
     *         Traditionally partitions were aligned to the physical structure
     *         of the underlying disk,
     *         however with modern storage greater efficiency is achieved by
     *         aligning partitions on
     *         large values that are a power of two.
     */
    public int createAligned(long size, WellKnownPartitionType type, boolean active, int alignment) {
        if (size < _diskGeometry.getBytesPerSector()) {
            throw new IndexOutOfBoundsException("size must be at least one sector");
        }

        if (alignment % _diskGeometry.getBytesPerSector() != 0) {
            throw new IllegalArgumentException("Alignment is not a multiple of the sector size");
        }

        if (size % alignment != 0) {
            throw new IllegalArgumentException("Size is not a multiple of the alignment");
        }

        long sectorLength = size / _diskGeometry.getBytesPerSector();
        long start = findGap(size / _diskGeometry.getBytesPerSector(), alignment / _diskGeometry.getBytesPerSector());
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
     *         parameters, the caller is
     *         responsible for ensuring that the partition does not overlap
     *         other partitions.
     */
    public int create(long startSector, long endSector, UUID type, long attributes, String name) {
        GptEntry newEntry = createEntry(startSector, endSector, type, attributes, name);
        return getEntryIndex(newEntry.Identity);
    }

    /**
     * Deletes a partition at a given index.
     *
     * @param index The index of the partition.
     */
    public void delete(int index) {
        int offset = getPartitionOffset(index);
        Arrays.fill(_entryBuffer, offset, offset + _primaryHeader.PartitionEntrySize, (byte) 0);
        write();
    }

    public SparseStream open(GptEntry entry) {
        long start = entry.FirstUsedLogicalBlock * _diskGeometry.getBytesPerSector();
        long end = (entry.LastUsedLogicalBlock + 1) * _diskGeometry.getBytesPerSector();
        return new SubStream(_diskData, start, end - start);
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
        } catch (moe.yo3explorer.dotnetio4j.IOException ioe) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Invalid GPT disk, protective MBR table not present or invalid");
        }

        if (bpt.getCount() != 1 || bpt.get___idx(0).getBiosType() != BiosPartitionTypes.GptProtective) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Invalid GPT disk, protective MBR table is not valid");
        }

        _diskData = disk;
        _diskGeometry = diskGeometry;
        disk.setPosition(diskGeometry.getBytesPerSector());
        byte[] sector = StreamUtilities.readExact(disk, diskGeometry.getBytesPerSector());
        _primaryHeader = new GptHeader(diskGeometry.getBytesPerSector());
        if (!_primaryHeader.readFrom(sector, 0) || !readEntries(_primaryHeader)) {
            disk.setPosition(disk.getLength() - diskGeometry.getBytesPerSector());
            disk.read(sector, 0, sector.length);
            _secondaryHeader = new GptHeader(diskGeometry.getBytesPerSector());
            if (!_secondaryHeader.readFrom(sector, 0) || !readEntries(_secondaryHeader)) {
                throw new moe.yo3explorer.dotnetio4j.IOException("No valid GUID Partition Table found");
            }

            // Generate from the primary table from the secondary one
            _primaryHeader = new GptHeader(_secondaryHeader);
            _primaryHeader.HeaderLba = _secondaryHeader.AlternateHeaderLba;
            _primaryHeader.AlternateHeaderLba = _secondaryHeader.HeaderLba;
            _primaryHeader.PartitionEntriesLba = 2;
            // If the disk is writeable, fix up the primary partition table based on the
            // (valid) secondary table.
            if (disk.canWrite()) {
                writePrimaryHeader();
            }

        }

        if (_secondaryHeader == null) {
            _secondaryHeader = new GptHeader(diskGeometry.getBytesPerSector());
            disk.setPosition(disk.getLength() - diskGeometry.getBytesPerSector());
            disk.read(sector, 0, sector.length);
            if (!_secondaryHeader.readFrom(sector, 0) || !readEntries(_secondaryHeader)) {
                // Generate from the secondary table from the primary one
                _secondaryHeader = new GptHeader(_primaryHeader);
                _secondaryHeader.HeaderLba = _secondaryHeader.AlternateHeaderLba;
                _secondaryHeader.AlternateHeaderLba = _secondaryHeader.HeaderLba;
                _secondaryHeader.PartitionEntriesLba = _secondaryHeader.HeaderLba - MathUtilities
                        .roundUp(_secondaryHeader.PartitionEntryCount * _secondaryHeader.PartitionEntrySize,
                                 diskGeometry.getBytesPerSector());
                // If the disk is writeable, fix up the secondary partition table based on the
                // (valid) primary table.
                if (disk.canWrite()) {
                    writeSecondaryHeader();
                }

            }

        }

    }

    private void establishReservedPartition(List<GptEntry> allEntries) {
        // If no MicrosoftReserved partition, and no Microsoft Data partitions, and the disk
        // has a 'reasonable' size free, create a Microsoft Reserved partition.
        if (countEntries(allEntries, e -> {
            return e.PartitionType == GuidPartitionTypes.MicrosoftReserved;
        }) == 0 && countEntries(allEntries, e -> {
            return e.PartitionType == GuidPartitionTypes.WindowsBasicData;
        }) == 0 && _diskGeometry.getCapacity() > 512 * 1024 * 1024) {
            long reservedStart = firstAvailableSector(allEntries);
            long reservedEnd = findLastFreeSector(reservedStart, allEntries);
            if ((reservedEnd - reservedStart + 1) * _diskGeometry.getBytesPerSector() > 512 * 1024 * 1024) {
                long size = (_diskGeometry.getCapacity() < 16 * 1024L * 1024 * 1024 ? 32 : 128) * 1024 * 1024;
                reservedEnd = reservedStart + size / _diskGeometry.getBytesPerSector() - 1;
                int reservedOffset = getFreeEntryOffset();
                GptEntry newReservedEntry = new GptEntry();
                newReservedEntry.PartitionType = GuidPartitionTypes.MicrosoftReserved;
                newReservedEntry.Identity = UUID.randomUUID();
                newReservedEntry.FirstUsedLogicalBlock = reservedStart;
                newReservedEntry.LastUsedLogicalBlock = reservedEnd;
                newReservedEntry.Attributes = 0;
                newReservedEntry.Name = "Microsoft reserved partition";
                newReservedEntry.writeTo(_entryBuffer, reservedOffset);
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
        newEntry.PartitionType = type;
        newEntry.Identity = UUID.randomUUID();
        newEntry.FirstUsedLogicalBlock = startSector;
        newEntry.LastUsedLogicalBlock = endSector;
        newEntry.Attributes = attributes;
        newEntry.Name = name;
        newEntry.writeTo(_entryBuffer, offset);
        // Commit changes to disk
        write();
        return newEntry;
    }

    private long findGap(long numSectors, long alignmentSectors) {
        List<GptEntry> list = getAllEntries();
        Collections.sort(list);
        long startSector = MathUtilities.roundUp(_primaryHeader.FirstUsable, alignmentSectors);
        for (GptEntry entry : list) {
            if (!Utilities.rangesOverlap(startSector,
                                         startSector + numSectors - 1,
                                         entry.FirstUsedLogicalBlock,
                                         entry.LastUsedLogicalBlock)) {
                break;
            }

            startSector = MathUtilities.roundUp(entry.LastUsedLogicalBlock + 1, alignmentSectors);
        }
        if (_diskGeometry.getTotalSectorsLong() - startSector < numSectors) {
            throw new moe.yo3explorer.dotnetio4j.IOException(String.format("Unable to find free space of %d sectors", numSectors));
        }

        return startSector;
    }

    private long firstAvailableSector(List<GptEntry> allEntries) {
        long start = _primaryHeader.FirstUsable;
        for (GptEntry entry  : allEntries) {
            if (entry.LastUsedLogicalBlock >= start) {
                start = entry.LastUsedLogicalBlock + 1;
            }

        }
        return start;
    }

    private long findLastFreeSector(long start, List<GptEntry> allEntries) {
        long end = _primaryHeader.LastUsable;
        for (GptEntry entry : allEntries) {
            if (entry.LastUsedLogicalBlock > start && entry.FirstUsedLogicalBlock <= end) {
                end = entry.FirstUsedLogicalBlock - 1;
            }

        }
        return end;
    }

    private void write() {
        writePrimaryHeader();
        writeSecondaryHeader();
    }

    private void writePrimaryHeader() {
        byte[] buffer = new byte[_diskGeometry.getBytesPerSector()];
        _primaryHeader.EntriesCrc = calcEntriesCrc();
        _primaryHeader.writeTo(buffer, 0);
        _diskData.setPosition(_diskGeometry.getBytesPerSector());
        _diskData.write(buffer, 0, buffer.length);
        _diskData.setPosition(2 * _diskGeometry.getBytesPerSector());
        _diskData.write(_entryBuffer, 0, _entryBuffer.length);
    }

    private void writeSecondaryHeader() {
        byte[] buffer = new byte[_diskGeometry.getBytesPerSector()];
        _secondaryHeader.EntriesCrc = calcEntriesCrc();
        _secondaryHeader.writeTo(buffer, 0);
        _diskData.setPosition(_diskData.getLength() - _diskGeometry.getBytesPerSector());
        _diskData.write(buffer, 0, buffer.length);
        _diskData.setPosition(_secondaryHeader.PartitionEntriesLba * _diskGeometry.getBytesPerSector());
        _diskData.write(_entryBuffer, 0, _entryBuffer.length);
    }

    private boolean readEntries(GptHeader header) {
        _diskData.setPosition(header.PartitionEntriesLba * _diskGeometry.getBytesPerSector());
        _entryBuffer = StreamUtilities.readExact(_diskData, header.PartitionEntrySize * header.PartitionEntryCount);
        if (header.EntriesCrc != calcEntriesCrc()) {
            return false;
        }

        return true;
    }

    private int calcEntriesCrc() {
        return Crc32LittleEndian.compute(Crc32Algorithm.Common, _entryBuffer, 0, _entryBuffer.length);
    }

    private List<GptEntry> getAllEntries() {
        List<GptEntry> result = new ArrayList<>();
        for (int i = 0; i < _primaryHeader.PartitionEntryCount; ++i) {
            GptEntry entry = new GptEntry();
            entry.readFrom(_entryBuffer, i * _primaryHeader.PartitionEntrySize);
            if (!entry.PartitionType.equals(EMPTY)) {
                result.add(entry);
            }
        }
        return result;
    }

    private int getPartitionOffset(int index) {
        boolean found = false;
        int entriesSoFar = 0;
        int position = 0;
        while (!found && position < _primaryHeader.PartitionEntryCount) {
            GptEntry entry = new GptEntry();
            entry.readFrom(_entryBuffer, position * _primaryHeader.PartitionEntrySize);
            if (!entry.PartitionType.equals(EMPTY)) {
                if (index == entriesSoFar) {
                    found = true;
                    break;
                }

                entriesSoFar++;
            }

            position++;
        }
        if (found) {
            return position * _primaryHeader.PartitionEntrySize;
        }

        throw new moe.yo3explorer.dotnetio4j.IOException(String.format("No such partition: %d", index));
    }

    private int getEntryIndex(UUID identity) {
        int index = 0;
        for (int i = 0; i < _primaryHeader.PartitionEntryCount; ++i) {
            GptEntry entry = new GptEntry();
            entry.readFrom(_entryBuffer, i * _primaryHeader.PartitionEntrySize);
            if (entry.Identity.equals(identity)) {
                return index;
            }

            if (!entry.PartitionType.equals(EMPTY)) {
                index++;
            }
        }
        throw new moe.yo3explorer.dotnetio4j.IOException("No such partition");
    }

    private int getFreeEntryOffset() {
        for (int i = 0; i < _primaryHeader.PartitionEntryCount; ++i) {
            GptEntry entry = new GptEntry();
            entry.readFrom(_entryBuffer, i * _primaryHeader.PartitionEntrySize);
            if (entry.PartitionType.equals(EMPTY)) {
                return i * _primaryHeader.PartitionEntrySize;
            }
        }
        throw new moe.yo3explorer.dotnetio4j.IOException("No free partition entries available");
    }
}
