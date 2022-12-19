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
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import discUtils.core.ChsAddress;
import discUtils.core.Geometry;
import discUtils.core.VirtualDisk;
import discUtils.core.internal.Utilities;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.SubStream;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


/**
 * Represents a BIOS (MBR) Partition Table.
 */
public final class BiosPartitionTable extends PartitionTable {

    private Stream diskData;

    private Geometry diskGeometry;

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
     * returns {@code null} ).
     */
    @Override public UUID getDiskGuid() {
        return EMPTY;
    }

    /**
     * Gets a collection of the partitions for storing Operating System
     * file-systems.
     */
    @Override public List<PartitionInfo> getPartitions() {
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
            disk.position(0);
            byte[] bootSector = StreamUtilities.readExact(disk, Sizes.Sector);
            if ((bootSector[510] & 0xff) == 0x55 && (bootSector[511] & 0xff) == 0xAA) {
                int maxHead = 0;
                int maxSector = 0;
                for (BiosPartitionRecord record : readPrimaryRecords(bootSector)) {
                    maxHead = Math.max(maxHead, record.getEndHead());
                    maxSector = Math.max(maxSector, record.getEndSector());
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
     * @return {@code true} if the partition table is valid, else {@code false}.
     */
    public static boolean isValid(Stream disk) {
        if (disk.getLength() < Sizes.Sector) {
            return false;
        }

        disk.position(0);
        byte[] bootSector = StreamUtilities.readExact(disk, Sizes.Sector);

        // Check for the 'bootable sector' marker
        if ((bootSector[510] & 0xff) != 0x55 || (bootSector[511] & 0xff) != 0xAA) {
            return false;
        }

        List<StreamExtent> knownPartitions = new ArrayList<>();
        for (BiosPartitionRecord record : readPrimaryRecords(bootSector)) {
            // If the partition extends beyond the end of the disk, this is
            // probably an invalid partition table
            if (record.getLBALength() != 0xFFFF_FFFF &&
                (record.getLBAStart() + record.getLBALength()) * Sizes.Sector > disk.getLength()) {
                return false;
            }

            if (record.getLBALength() > 0) {
                List<StreamExtent> thisPartitionExtents = Collections.singletonList(new StreamExtent(record.getLBAStart(), record.getLBALength()));

                // If the partition intersects another partition, this is
                // probably an invalid partition table
                for (@SuppressWarnings("unused")
                StreamExtent overlap : StreamExtent.intersect(knownPartitions, thisPartitionExtents)) {
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
            data.position(0);
            bootSector = StreamUtilities.readExact(data, Sizes.Sector);
        } else {
            bootSector = new byte[Sizes.Sector];
        }

        // Wipe all four 16-byte partition table entries
        Arrays.fill(bootSector, 0x01BE, 0x01BE + 16 * 4, (byte) 0);

        // Marker bytes
        bootSector[510] = 0x55;
        bootSector[511] = (byte) 0xAA;

        data.position(0);
        data.write(bootSector, 0, bootSector.length);

        return new BiosPartitionTable(disk, diskGeometry);
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
        Geometry allocationGeometry = new Geometry(diskData.getLength(),
                                                   diskGeometry.getHeadsPerCylinder(),
                                                   diskGeometry.getSectorsPerTrack(),
                                                   diskGeometry.getBytesPerSector());

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
    @Override public int create(long size, WellKnownPartitionType type, boolean active) {
        int cylinderCapacity = diskGeometry.getSectorsPerTrack() * diskGeometry.getHeadsPerCylinder() *
                               diskGeometry.getBytesPerSector();
        int numCylinders = (int) (size / cylinderCapacity);

        int startCylinder = findCylinderGap(numCylinders);

        return createPrimaryByCylinder(startCylinder, startCylinder + numCylinders - 1, convertType(type, size), active);
    }

    /**
     * Creates a new aligned partition that encompasses the entire disk.
     *
     * The partition table must be empty before this method is called, otherwise
     * IOException is thrown.
     *
     * Traditionally partitions were aligned to the physical structure of the
     * underlying disk, however with modern storage greater efficiency is
     * achieved by aligning partitions on large values that are a power of two.
     * 
     * @param type The partition type.
     * @param active Whether the partition is active (bootable).
     * @param alignment The alignment (in bytes).
     * @return The index of the partition.
     */
    @Override public int createAligned(WellKnownPartitionType type, boolean active, int alignment) {
        Geometry allocationGeometry = new Geometry(diskData.getLength(),
                                                   diskGeometry.getHeadsPerCylinder(),
                                                   diskGeometry.getSectorsPerTrack(),
                                                   diskGeometry.getBytesPerSector());

        ChsAddress start = new ChsAddress(0, 1, 1);

        long startLba = MathUtilities.roundUp(allocationGeometry.toLogicalBlockAddress(start),
                                              alignment / diskGeometry.getBytesPerSector());
        long lastLba = MathUtilities.roundDown(diskData.getLength() / diskGeometry.getBytesPerSector(),
                                               alignment / diskGeometry.getBytesPerSector());

        return createPrimaryBySector(startLba,
                                     lastLba - 1,
                                     convertType(type, (lastLba - startLba) * diskGeometry.getBytesPerSector()),
                                     active);
    }

    /**
     * Creates a new aligned partition with a target size.
     *
     * Traditionally partitions were aligned to the physical structure of the
     * underlying disk, however with modern storage greater efficiency is
     * achieved by aligning partitions on large values that are a power of two.
     *
     * @param size The target size (in bytes).
     * @param type The partition type.
     * @param active Whether the partition is active (bootable).
     * @param alignment The alignment (in bytes).
     * @return The index of the new partition.
     */
    @Override public int createAligned(long size, WellKnownPartitionType type, boolean active, int alignment) {
        if (size < diskGeometry.getBytesPerSector()) {
            throw new IndexOutOfBoundsException("size must be at least one sector");
        }

        if (alignment % diskGeometry.getBytesPerSector() != 0) {
            throw new IllegalArgumentException("alignment is not a multiple of the sector size");
        }

        if (size % alignment != 0) {
            throw new IllegalArgumentException("size is not a multiple of the alignment");
        }

        long sectorLength = size / diskGeometry.getBytesPerSector();
        long start = findGap(size / diskGeometry.getBytesPerSector(), alignment / diskGeometry.getBytesPerSector());

        return createPrimaryBySector(start, start + sectorLength - 1, convertType(type, sectorLength * Sizes.Sector), active);
    }

    /**
     * Deletes a partition at a given index.
     *
     * @param index The index of the partition.
     */
    @Override public void delete(int index) {
        writeRecord(index, new BiosPartitionRecord());
    }

    /**
     * Creates a new Primary Partition that occupies whole cylinders, for best
     * compatibility.
     *
     * If the cylinder 0 is given, the first track will not be used, to reserve
     * space for the meta-data at the start of the disk.
     *
     * @param first The first cylinder to include in the partition (inclusive).
     * @param last The last cylinder to include in the partition (inclusive).
     * @param type The BIOS (MBR) type of the new partition.
     * @param markActive Whether to mark the partition active (bootable).
     * @return The index of the new partition.
     */
    public int createPrimaryByCylinder(int first, int last, byte type, boolean markActive) {
        if (first < 0) {
            throw new IndexOutOfBoundsException("first cylinder must be Zero or greater");
        }

        if (last <= first) {
            throw new IllegalArgumentException("Last cylinder must be greater than first");
        }

        long lbaStart = first == 0 ? diskGeometry.toLogicalBlockAddress(0, 1, 1)
                                   : diskGeometry.toLogicalBlockAddress(first, 0, 1);
        long lbaLast = diskGeometry
                .toLogicalBlockAddress(last, diskGeometry.getHeadsPerCylinder() - 1, diskGeometry.getSectorsPerTrack());

        return createPrimaryBySector(lbaStart, lbaLast, type, markActive);
    }

    /**
     * Creates a new Primary Partition, specified by Logical block Addresses.
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

        if ((last + 1) * diskGeometry.getBytesPerSector() > diskData.getLength()) {
            throw new IndexOutOfBoundsException("The last sector extends beyond the end of the disk");
        }

        BiosPartitionRecord[] existing = getPrimaryRecords();

        BiosPartitionRecord newRecord = new BiosPartitionRecord();
        ChsAddress startAddr = diskGeometry.toChsAddress(first);
        ChsAddress endAddr = diskGeometry.toChsAddress(last);

        // Because C/H/S addresses can max out at lower values than the LBA
        // values,
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

        // First check for overlap with existing partition...
        for (BiosPartitionRecord r : existing) {
            if (Utilities.rangesOverlap(first, last + 1, r.getLBAStartAbsolute(), r.getLBAStartAbsolute() + r.getLBALength())) {
                throw new dotnet4j.io.IOException("New partition overlaps with existing partition");
            }
        }

        // Now look for empty partition
        for (int i = 0; i < 4; ++i) {
            if (!existing[i].isValid()) {
                writeRecord(i, newRecord);
                return i;
            }
        }

        throw new dotnet4j.io.IOException("No primary partition slots available");
    }

    /**
     * Sets the active partition.
     *
     * The supplied index is the index within the primary partition, see
     * {@code PrimaryIndex} on {@code BiosPartitionInfo} .
     *
     * @param index The index of the primary partition to mark bootable, or
     *            {@code -1} for none.
     */
    public void setActivePartition(int index) {
        List<BiosPartitionRecord> records = Arrays.asList(getPrimaryRecords());

        for (int i = 0; i < records.size(); ++i) {
            records.get(i).setStatus(i == index ? (byte) 0x80 : (byte) 0x00);
            writeRecord(i, records.get(i));
        }
    }

    /**
     * Gets all of the disk ranges containing partition table metadata.
     *
     * @return list of stream extents, indicated as byte offset from the start of
     *         the disk.
     */
    public List<StreamExtent> getMetadataDiskExtents() {
        List<StreamExtent> extents = new ArrayList<>();

        extents.add(new StreamExtent(0, Sizes.Sector));

        for (BiosPartitionRecord primaryRecord : getPrimaryRecords()) {
            if (primaryRecord.isValid()) {
                if (isExtendedPartition(primaryRecord)) {
                    extents.addAll(new BiosExtendedPartitionTable(diskData, (int) primaryRecord.getLBAStart())
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
     * The partitions are not relocated to a cylinder boundary, just the CHS
     * fields are updated on the assumption the LBA fields are definitive.
     *
     * @param geometry The disk's new BIOS geometry.
     */
    public void updateBiosGeometry(Geometry geometry) {
        diskData.position(0);
        byte[] bootSector = StreamUtilities.readExact(diskData, Sizes.Sector);

        BiosPartitionRecord[] records = readPrimaryRecords(bootSector);
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
        diskGeometry = geometry;
    }

    SparseStream open(BiosPartitionRecord record) {
        return new SubStream(diskData,
                             Ownership.None,
                             record.getLBAStartAbsolute() * diskGeometry.getBytesPerSector(),
                             record.getLBALength() * diskGeometry.getBytesPerSector());
    }

    private static BiosPartitionRecord[] readPrimaryRecords(byte[] bootSector) {
        BiosPartitionRecord[] records = new BiosPartitionRecord[4];
        for (int i = 0; i < 4; ++i) {
            records[i] = new BiosPartitionRecord(bootSector, 0x01BE + i * 0x10, 0, i);
        }

        return records;
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
        for (BiosPartitionRecord primaryRecord : getPrimaryRecords()) {
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

    private BiosPartitionRecord[] getPrimaryRecords() {
        diskData.position(0);
        byte[] bootSector = StreamUtilities.readExact(diskData, Sizes.Sector);
        return readPrimaryRecords(bootSector);
    }

    private List<BiosPartitionRecord> getExtendedRecords(BiosPartitionRecord r) {
        return new BiosExtendedPartitionTable(diskData, (int) r.getLBAStart()).getPartitions();
    }

    private void writeRecord(int i, BiosPartitionRecord newRecord) {
        diskData.position(0);
        byte[] bootSector = StreamUtilities.readExact(diskData, Sizes.Sector);
        newRecord.writeTo(bootSector, 0x01BE + i * 16);
        diskData.position(0);
        diskData.write(bootSector, 0, bootSector.length);
    }

    private int findCylinderGap(int numCylinders) {
        List<BiosPartitionRecord> list = Arrays.stream(getPrimaryRecords()).filter(BiosPartitionRecord::isValid).sorted().collect(Collectors.toList());

        int startCylinder = 0;
        for (BiosPartitionRecord r : list) {
            int existingStart = r.getStartCylinder();
            int existingEnd = r.getEndCylinder();

            // LBA can represent bigger disk locations than CHS, so assume the
            // LBA to be
            // definitive in the case where it
            // appears the CHS address has been truncated.
            if (r.getLBAStart() > diskGeometry
                    .toLogicalBlockAddress(r.getStartCylinder(), r.getStartHead(), r.getStartSector())) {
                existingStart = diskGeometry.toChsAddress(r.getLBAStart()).getCylinder();
            }

            if (r.getLBAStart() + r.getLBALength() > diskGeometry
                    .toLogicalBlockAddress(r.getEndCylinder(), r.getEndHead(), r.getEndSector())) {
                existingEnd = diskGeometry.toChsAddress(r.getLBAStart() + r.getLBALength()).getCylinder();
            }

            if (!Utilities.rangesOverlap(startCylinder, startCylinder + numCylinders - 1, existingStart, existingEnd)) {
                break;
            }
            startCylinder = existingEnd + 1;
        }

        return startCylinder;
    }

    private long findGap(long numSectors, long alignmentSectors) {
        List<BiosPartitionRecord> list = Arrays.stream(getPrimaryRecords()).filter(BiosPartitionRecord::isValid).sorted().collect(Collectors.toList());
        long startSector = MathUtilities.roundUp(diskGeometry.toLogicalBlockAddress(0, 1, 1), alignmentSectors);
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
        if (diskGeometry.getTotalSectorsLong() - startSector < numSectors) {
            throw new dotnet4j.io.IOException(String.format("Unable to find free space of %s sectors", numSectors));
        }

        return startSector;
    }

    private void init(Stream disk, Geometry diskGeometry) {
        diskData = disk;
        this.diskGeometry = diskGeometry;
        diskData.position(0);
        byte[] bootSector = StreamUtilities.readExact(diskData, Sizes.Sector);
        if ((bootSector[510] & 0xff) != 0x55 || (bootSector[511] & 0xff) != 0xAA) {
            throw new dotnet4j.io.IOException("Invalid boot sector - no magic number 0xAA55");
        }
    }
}
