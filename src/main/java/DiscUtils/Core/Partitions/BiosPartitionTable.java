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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import DiscUtils.Core.ChsAddress;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * Represents a BIOS (MBR) Partition Table.
 */
public final class BiosPartitionTable extends PartitionTable {
    private Stream _diskData;

    private Geometry _diskGeometry;

    /**
     * Initializes a new instance of the BiosPartitionTable class.
     *
     * @param disk The disk containing the partition table.
     */
    public BiosPartitionTable(VirtualDisk disk) {
        init(disk.getContent(), disk.getBiosGeometry());
    }

    /**
     * Initializes a new instance of the BiosPartitionTable class.
     *
     * @param disk The stream containing the disk data.
     * @param diskGeometry The geometry of the disk.
     */
    public BiosPartitionTable(Stream disk, Geometry diskGeometry) {
        init(disk, diskGeometry);
    }

    /**
     * Gets a collection of the partitions for storing Operating System
     * file-systems.
     */
    public List<BiosPartitionInfo> getBiosUserPartitions() {
        List<BiosPartitionInfo> result = new ArrayList<>();
        for (BiosPartitionRecord r : getAllRecords()) {
            if (r.isValid()) {
                result.add(new BiosPartitionInfo(this, r));
            }

        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Gets the GUID that uniquely identifies this disk, if supported (else
     * returns
     * {@code null}
     * ).
     */
    public UUID getDiskGuid() {
        return null;
    }

    /**
     * Gets a collection of the partitions for storing Operating System
     * file-systems.
     */
    public List<PartitionInfo> getPartitions() {
        List<PartitionInfo> result = new ArrayList<>();
        for (BiosPartitionRecord r : getAllRecords()) {
            if (r.isValid()) {
                result.add(new BiosPartitionInfo(this, r));
            }

        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Makes a best guess at the geometry of a disk.
     *
     * @param disk String containing the disk image to detect the geometry from.
     * @return The detected geometry.
     */
    public static Geometry detectGeometry(Stream disk) {
        if (disk.getLength() >= Sizes.Sector) {
            disk.setPosition(0);
            byte[] bootSector = StreamUtilities.readExact(disk, Sizes.Sector);
            if ((bootSector[510] & 0xff) == 0x55 && (bootSector[511] & 0xff) == 0xAA) {
                byte maxHead = 0;
                byte maxSector = 0;
                for (BiosPartitionRecord record : readPrimaryRecords(bootSector)) {
                    maxHead = (byte) Math.max(maxHead, record.getEndHead());
                    maxSector = (byte) Math.max(maxSector, record.getEndSector());
                }
                if (maxHead > 0 && maxSector > 0) {
                    int cylSize = (maxHead + 1) * maxSector * 512;
                    return new Geometry((int) MathUtilities.ceil(disk.getLength(), cylSize), maxHead + 1, maxSector);
                }

            }

        }

        return Geometry.fromCapacity(disk.getLength());
    }

    /**
     * Indicates if a stream contains a valid partition table.
     *
     * @param disk The stream to inspect.
     * @return
     *         {@code true}
     *         if the partition table is valid, else
     *         {@code false}
     *         .
     */
    public static boolean isValid(Stream disk) {
        if (disk.getLength() < Sizes.Sector) {
            return false;
        }

        disk.setPosition(0);
        byte[] bootSector = StreamUtilities.readExact(disk, Sizes.Sector);
        // Check for the 'bootable sector' marker
        if ((bootSector[510] & 0xff) != 0x55 || (bootSector[511] & 0xff) != 0xAA) {
            return false;
        }

        List<StreamExtent> knownPartitions = new ArrayList<>();
        for (BiosPartitionRecord record : readPrimaryRecords(bootSector)) {
            // If the partition extends beyond the end of the disk, this is probably an invalid partition table
            if (record.getLBALength() != 0xFFFFFFFF &&
                (record.getLBAStart() + record.getLBALength()) * Sizes.Sector > disk.getLength()) {
                return false;
            }

            if (record.getLBALength() > 0) {
                List<StreamExtent> thisPartitionExtents = Arrays
                        .asList(new StreamExtent(record.getLBAStart(), record.getLBALength()));
                // If the partition intersects another partition, this is probably an invalid partition table
                for (@SuppressWarnings("unused") StreamExtent overlap : StreamExtent.intersect(knownPartitions, thisPartitionExtents)) {
                    return false;
                }
                knownPartitions = new ArrayList<>(StreamExtent.union(knownPartitions, thisPartitionExtents));
            }

        }
        return true;
    }

    /**
     * Creates a new partition table on a disk.
     *
     * @param disk The disk to initialize.
     * @return An object to access the newly created partition table.
     */
    public static BiosPartitionTable initialize(VirtualDisk disk) {
        return initialize(disk.getContent(), disk.getBiosGeometry());
    }

    /**
     * Creates a new partition table on a disk containing a single partition.
     *
     * @param disk The disk to initialize.
     * @param type The partition type for the single partition.
     * @return An object to access the newly created partition table.
     */
    public static BiosPartitionTable initialize(VirtualDisk disk, WellKnownPartitionType type) {
        BiosPartitionTable table = initialize(disk.getContent(), disk.getBiosGeometry());
        table.create(type, true);
        return table;
    }

    /**
     * Creates a new partition table on a disk.
     *
     * @param disk The stream containing the disk data.
     * @param diskGeometry The geometry of the disk.
     * @return An object to access the newly created partition table.
     */
    public static BiosPartitionTable initialize(Stream disk, Geometry diskGeometry) {
        Stream data = disk;
        byte[] bootSector;
        if (data.getLength() >= Sizes.Sector) {
            data.setPosition(0);
            bootSector = StreamUtilities.readExact(data, Sizes.Sector);
        } else {
            bootSector = new byte[Sizes.Sector];
        }
        // Wipe all four 16-byte partition table entries
        Arrays.fill(bootSector, 0x01BE, 0x01BE + 16 * 4, (byte) 0);
        // Marker bytes
        bootSector[510] = 0x55;
        bootSector[511] = (byte) 0xAA;
        data.setPosition(0);
        data.write(bootSector, 0, bootSector.length);
        return new BiosPartitionTable(disk, diskGeometry);
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
        Geometry allocationGeometry = new Geometry(_diskData.getLength(),
                                                   _diskGeometry.getHeadsPerCylinder(),
                                                   _diskGeometry.getSectorsPerTrack(),
                                                   _diskGeometry.getBytesPerSector());
        ChsAddress start = new ChsAddress(0, 1, 1);
        ChsAddress last = allocationGeometry.getLastSector();
        long startLba = allocationGeometry.toLogicalBlockAddress(start);
        long lastLba = allocationGeometry.toLogicalBlockAddress(last);
        return createPrimaryByCylinder(0,
                                       allocationGeometry.getCylinders() - 1,
                                       convertType(type, (lastLba - startLba) * Sizes.Sector),
                                       active);
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
        int cylinderCapacity = _diskGeometry.getSectorsPerTrack() * _diskGeometry.getHeadsPerCylinder() *
                               _diskGeometry.getBytesPerSector();
        int numCylinders = (int) (size / cylinderCapacity);
        int startCylinder = findCylinderGap(numCylinders);
        return createPrimaryByCylinder(startCylinder, startCylinder + numCylinders - 1, convertType(type, size), active);
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
        Geometry allocationGeometry = new Geometry(_diskData.getLength(),
                                                   _diskGeometry.getHeadsPerCylinder(),
                                                   _diskGeometry.getSectorsPerTrack(),
                                                   _diskGeometry.getBytesPerSector());
        ChsAddress start = new ChsAddress(0, 1, 1);
        long startLba = MathUtilities.roundUp(allocationGeometry.toLogicalBlockAddress(start),
                                              alignment / _diskGeometry.getBytesPerSector());
        long lastLba = MathUtilities.roundDown(_diskData.getLength() / _diskGeometry.getBytesPerSector(),
                                               alignment / _diskGeometry.getBytesPerSector());
        return createPrimaryBySector(startLba,
                                     lastLba - 1,
                                     convertType(type, (lastLba - startLba) * _diskGeometry.getBytesPerSector()),
                                     active);
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
            throw new IllegalArgumentException("alignment is not a multiple of the sector size");
        }

        if (size % alignment != 0) {
            throw new IllegalArgumentException("size is not a multiple of the alignment");
        }

        long sectorLength = size / _diskGeometry.getBytesPerSector();
        long start = findGap(size / _diskGeometry.getBytesPerSector(), alignment / _diskGeometry.getBytesPerSector());
        return createPrimaryBySector(start, start + sectorLength - 1, convertType(type, sectorLength * Sizes.Sector), active);
    }

    /**
     * Deletes a partition at a given index.
     *
     * @param index The index of the partition.
     */
    public void delete(int index) {
        writeRecord(index, new BiosPartitionRecord());
    }

    /**
     * Creates a new Primary Partition that occupies whole cylinders, for best
     * compatibility.
     *
     * @param first The first cylinder to include in the partition (inclusive).
     * @param last The last cylinder to include in the partition (inclusive).
     * @param type The BIOS (MBR) type of the new partition.
     * @param markActive Whether to mark the partition active (bootable).
     * @return The index of the new partition.If the cylinder 0 is given, the
     *         first track will not be used, to reserve space
     *         for the meta-data at the start of the disk.
     */
    public int createPrimaryByCylinder(int first, int last, byte type, boolean markActive) {
        if (first < 0) {
            throw new IndexOutOfBoundsException("first cylinder must be Zero or greater");
        }

        if (last <= first) {
            throw new IllegalArgumentException("Last cylinder must be greater than first");
        }

        long lbaStart = first == 0 ? _diskGeometry.toLogicalBlockAddress(0, 1, 1)
                                   : _diskGeometry.toLogicalBlockAddress(first, 0, 1);
        long lbaLast = _diskGeometry
                .toLogicalBlockAddress(last, _diskGeometry.getHeadsPerCylinder() - 1, _diskGeometry.getSectorsPerTrack());
System.err.printf("%x, %x, %x, %s\n", first, last, lbaStart, lbaLast);
        return createPrimaryBySector(lbaStart, lbaLast, type, markActive);
    }

    /**
     * Creates a new Primary Partition, specified by Logical Block Addresses.
     *
     * @param first The LBA address of the first sector (inclusive).
     * @param last The LBA address of the last sector (inclusive).
     * @param type The BIOS (MBR) type of the new partition.
     * @param markActive Whether to mark the partition active (bootable).
     * @return The index of the new partition.
     */
    public int createPrimaryBySector(long first, long last, byte type, boolean markActive) {
        if (first >= last) {
            throw new IllegalArgumentException("The first sector in a partition must be before the last");
        }

System.err.printf("%x, %x, %x\n", (last + 1), _diskGeometry.getBytesPerSector(), _diskData.getLength());
        if ((last + 1) * _diskGeometry.getBytesPerSector() > _diskData.getLength()) {
            throw new IndexOutOfBoundsException("The last sector extends beyond the end of the disk");
        }

        List<BiosPartitionRecord> existing = getPrimaryRecords();
        BiosPartitionRecord newRecord = new BiosPartitionRecord();
        ChsAddress startAddr = _diskGeometry.toChsAddress(first);
        ChsAddress endAddr = _diskGeometry.toChsAddress(last);
        // Because C/H/S addresses can max out at lower values than the LBA values,
        // the special tuple (1023, 254, 63) is used.
        if (startAddr.getCylinder() > 1023) {
            startAddr = new ChsAddress(1023, 254, 63);
        }

        if (endAddr.getCylinder() > 1023) {
            endAddr = new ChsAddress(1023, 254, 63);
        }

        newRecord.setStartCylinder((short) startAddr.getCylinder());
        newRecord.setStartHead((byte) startAddr.getHead());
        newRecord.setStartSector((byte) startAddr.getSector());
        newRecord.setEndCylinder((short) endAddr.getCylinder());
        newRecord.setEndHead((byte) endAddr.getHead());
        newRecord.setEndSector((byte) endAddr.getSector());
        newRecord.setLBAStart((int) first);
        newRecord.setLBALength((int) (last - first + 1));
        newRecord.setPartitionType(type);
        newRecord.setStatus((byte) (markActive ? 0x80 : 0x00));
        for (BiosPartitionRecord r  : existing) {
            // First check for overlap with existing partition...
            if (Utilities.rangesOverlap(first,
                                        last + 1,
                                        r.getLBAStartAbsolute(),
                                        r.getLBAStartAbsolute() + r.getLBALength())) {
                throw new moe.yo3explorer.dotnetio4j.IOException("New partition overlaps with existing partition");
            }

        }
        for (int i = 0; i < 4; ++i) {
            // Now look for empty partition
            if (!existing.get(i).isValid()) {
                writeRecord(i, newRecord);
                return i;
            }

        }
        throw new moe.yo3explorer.dotnetio4j.IOException("No primary partition slots available");
    }

    /**
     * Sets the active partition.
     *
     * @param index The index of the primary partition to mark bootable, or
     *            {@code -1}
     *            for none.The supplied index is the index within the primary
     *            partition, see
     *            {@code PrimaryIndex}
     *            on
     *            {@code BiosPartitionInfo}
     *            .
     */
    public void setActivePartition(int index) {
        List<BiosPartitionRecord> records = new ArrayList<>(getPrimaryRecords());
        for (int i = 0; i < records.size(); ++i) {
            records.get(i).setStatus(i == index ? (byte) 0x80 : (byte) 0x00);
            writeRecord(i, records.get(i));
        }
    }

    /**
     * Gets all of the disk ranges containing partition table metadata.
     *
     * @return Set of stream extents, indicated as byte offset from the start of
     *         the disk.
     */
    public List<StreamExtent> getMetadataDiskExtents() {
        List<StreamExtent> extents = new ArrayList<>();
        extents.add(new StreamExtent(0, Sizes.Sector));
        for (BiosPartitionRecord primaryRecord : getPrimaryRecords()) {
            if (primaryRecord.isValid()) {
                if (isExtendedPartition(primaryRecord)) {
                    extents.addAll(new BiosExtendedPartitionTable(_diskData, (int) primaryRecord.getLBAStart())
                            .getMetadataDiskExtents());
                }

            }

        }
        return extents;
    }

    /**
     * Updates the CHS fields in partition records to reflect a new BIOS
     * geometry.
     *
     * @param geometry The disk's new BIOS geometry.The partitions are not
     *            relocated to a cylinder boundary, just the CHS fields are
     *            updated on the
     *            assumption the LBA fields are definitive.
     */
    public void updateBiosGeometry(Geometry geometry) {
        _diskData.setPosition(0);
        byte[] bootSector = StreamUtilities.readExact(_diskData, Sizes.Sector);
        List<BiosPartitionRecord> records = readPrimaryRecords(bootSector);
        int i = 0;
        for (BiosPartitionRecord record : records) {
            if (record.isValid()) {
                ChsAddress newStartAddress = geometry.toChsAddress(record.getLBAStartAbsolute());
                if (newStartAddress.getCylinder() > 1023) {
                    newStartAddress = new ChsAddress(1023, geometry.getHeadsPerCylinder() - 1, geometry.getSectorsPerTrack());
                }

                ChsAddress newEndAddress = geometry.toChsAddress(record.getLBAStartAbsolute() + record.getLBALength() - 1);
                if (newEndAddress.getCylinder() > 1023) {
                    newEndAddress = new ChsAddress(1023, geometry.getHeadsPerCylinder() - 1, geometry.getSectorsPerTrack());
                }

                record.setStartCylinder((short) newStartAddress.getCylinder());
                record.setStartHead((byte) newStartAddress.getHead());
                record.setStartSector((byte) newStartAddress.getSector());
                record.setEndCylinder((short) newEndAddress.getCylinder());
                record.setEndHead((byte) newEndAddress.getHead());
                record.setEndSector((byte) newEndAddress.getSector());
                writeRecord(i++, record);
            }

        }
        _diskGeometry = geometry;
    }

    public SparseStream open(BiosPartitionRecord record) {
        return new SubStream(_diskData,
                             Ownership.None,
                             record.getLBAStartAbsolute() * _diskGeometry.getBytesPerSector(),
                             record.getLBALength() * _diskGeometry.getBytesPerSector());
    }

    private static List<BiosPartitionRecord> readPrimaryRecords(byte[] bootSector) {
        BiosPartitionRecord[] records = new BiosPartitionRecord[4];
        for (int i = 0; i < 4; ++i) {
            records[i] = new BiosPartitionRecord(bootSector, 0x01BE + i * 0x10, 0, i);
        }
        return Arrays.asList(records);
    }

    private static boolean isExtendedPartition(BiosPartitionRecord r) {
        return r.getPartitionType() == BiosPartitionTypes.Extended || r.getPartitionType() == BiosPartitionTypes.ExtendedLba;
    }

    private static byte convertType(WellKnownPartitionType type, long size) {
        switch (type) {
        case WindowsFat:
            if (size < 512 * Sizes.OneMiB) {
                return BiosPartitionTypes.Fat16;
            }

            if (size < 1023 * (long) 254 * 63 * 512) {
                return BiosPartitionTypes.Fat32;
            }

            return BiosPartitionTypes.Fat32Lba;
        case WindowsNtfs:
            return BiosPartitionTypes.Ntfs;
        case Linux:
            return BiosPartitionTypes.LinuxNative;
        case LinuxSwap:
            return BiosPartitionTypes.LinuxSwap;
        case LinuxLvm:
            return BiosPartitionTypes.LinuxLvm;
        default:
            throw new IllegalArgumentException(String.format("Unrecognized partition type: '%s'", type));

        }
    }

    // Max BIOS size
    private List<BiosPartitionRecord> getAllRecords() {
        List<BiosPartitionRecord> newList = new ArrayList<>();
        for (BiosPartitionRecord primaryRecord  : getPrimaryRecords()) {
            if (primaryRecord.isValid()) {
                if (isExtendedPartition(primaryRecord)) {
                    newList.addAll(getExtendedRecords(primaryRecord));
                } else {
                    newList.add(primaryRecord);
                }
            }

        }
        return newList;
    }

    private List<BiosPartitionRecord> getPrimaryRecords() {
        _diskData.setPosition(0);
        byte[] bootSector = StreamUtilities.readExact(_diskData, Sizes.Sector);
        return readPrimaryRecords(bootSector);
    }

    private List<BiosPartitionRecord> getExtendedRecords(BiosPartitionRecord r) {
        return new BiosExtendedPartitionTable(_diskData, (int) r.getLBAStart()).getPartitions();
    }

    private void writeRecord(int i, BiosPartitionRecord newRecord) {
        _diskData.setPosition(0);
        byte[] bootSector = StreamUtilities.readExact(_diskData, Sizes.Sector);
        newRecord.writeTo(bootSector, 0x01BE + i * 16);
        _diskData.setPosition(0);
        _diskData.write(bootSector, 0, bootSector.length);
    }

    private int findCylinderGap(int numCylinders) {
        List<BiosPartitionRecord> list = getPrimaryRecords().stream().filter(r -> r.isValid()).collect(Collectors.toList());
        Collections.sort(list);
        int startCylinder = 0;
        for (BiosPartitionRecord r : list) {
            int existingStart = r.getStartCylinder();
            int existingEnd = r.getEndCylinder();
            // LBA can represent bigger disk locations than CHS, so assume the LBA to be definitive in the case where it
            // appears the CHS address has been truncated.
            if (r.getLBAStart() > _diskGeometry
                    .toLogicalBlockAddress(r.getStartCylinder(), r.getStartHead(), r.getStartSector())) {
                existingStart = _diskGeometry.toChsAddress(r.getLBAStart()).getCylinder();
            }

            if (r.getLBAStart() + r.getLBALength() > _diskGeometry
                    .toLogicalBlockAddress(r.getEndCylinder(), r.getEndHead(), r.getEndSector())) {
                existingEnd = _diskGeometry.toChsAddress(r.getLBAStart() + r.getLBALength()).getCylinder();
            }

            if (!Utilities.rangesOverlap(startCylinder, startCylinder + numCylinders - 1, existingStart, existingEnd)) {
                break;
            }

            startCylinder = existingEnd + 1;
        }
        return startCylinder;
    }

    private long findGap(long numSectors, long alignmentSectors) {
        List<BiosPartitionRecord> list = getPrimaryRecords().stream().filter(r -> r.isValid()).collect(Collectors.toList());
        Collections.sort(list);
        long startSector = MathUtilities.roundUp(_diskGeometry.toLogicalBlockAddress(0, 1, 1), alignmentSectors);
        int idx = 0;
        while (idx < list.size()) {
            BiosPartitionRecord entry = list.get(idx);
            while (idx < list.size() && startSector >= entry.getLBAStartAbsolute() + entry.getLBALength()) {
                idx++;
                entry = list.get(idx);
            }
            if (Utilities.rangesOverlap(startSector,
                                        startSector + numSectors,
                                        entry.getLBAStartAbsolute(),
                                        entry.getLBAStartAbsolute() + entry.getLBALength())) {
                startSector = MathUtilities.roundUp(entry.getLBAStartAbsolute() + entry.getLBALength(), alignmentSectors);
            }

            idx++;
        }
        if (_diskGeometry.getTotalSectorsLong() - startSector < numSectors) {
            throw new moe.yo3explorer.dotnetio4j.IOException(String.format("Unable to find free space of %s sectors", numSectors));
        }

        return startSector;
    }

    private void init(Stream disk, Geometry diskGeometry) {
        _diskData = disk;
        _diskGeometry = diskGeometry;
        _diskData.setPosition(0);
        byte[] bootSector = StreamUtilities.readExact(_diskData, Sizes.Sector);
        if ((bootSector[510] & 0xff) != 0x55 || (bootSector[511] & 0xff) != 0xAA) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Invalid boot sector - no magic number 0xAA55");
        }
    }
}
