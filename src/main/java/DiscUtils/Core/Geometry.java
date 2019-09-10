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

package DiscUtils.Core;

import DiscUtils.Streams.Util.Sizes;


/**
 * Class whose instances represent disk geometries.
 * Instances of this class are immutable.
 */
public final class Geometry {
    /**
     * Initializes a new instance of the Geometry class. The default 512 bytes
     * per sector is assumed.
     * 
     * @param cylinders The number of cylinders of the disk.
     * @param headsPerCylinder The number of heads (aka platters) of the disk.
     * @param sectorsPerTrack The number of sectors per track/cylinder of the
     *            disk.
     */
    public Geometry(int cylinders, int headsPerCylinder, int sectorsPerTrack) {
        __Cylinders = cylinders;
        __HeadsPerCylinder = headsPerCylinder;
        __SectorsPerTrack = sectorsPerTrack;
        __BytesPerSector = 512;
    }

    /**
     * Initializes a new instance of the Geometry class.
     * 
     * @param cylinders The number of cylinders of the disk.
     * @param headsPerCylinder The number of heads (aka platters) of the disk.
     * @param sectorsPerTrack The number of sectors per track/cylinder of the
     *            disk.
     * @param bytesPerSector The number of bytes per sector of the disk.
     */
    public Geometry(int cylinders, int headsPerCylinder, int sectorsPerTrack, int bytesPerSector) {
        __Cylinders = cylinders;
        __HeadsPerCylinder = headsPerCylinder;
        __SectorsPerTrack = sectorsPerTrack;
        __BytesPerSector = bytesPerSector;
    }

    /**
     * Initializes a new instance of the Geometry class.
     * 
     * @param capacity The total capacity of the disk.
     * @param headsPerCylinder The number of heads (aka platters) of the disk.
     * @param sectorsPerTrack The number of sectors per track/cylinder of the
     *            disk.
     * @param bytesPerSector The number of bytes per sector of the disk.
     */
    public Geometry(long capacity, int headsPerCylinder, int sectorsPerTrack, int bytesPerSector) {
        __Cylinders = (int) (capacity / (headsPerCylinder * (long) sectorsPerTrack * bytesPerSector));
        __HeadsPerCylinder = headsPerCylinder;
        __SectorsPerTrack = sectorsPerTrack;
        __BytesPerSector = bytesPerSector;
    }

    /**
     * Gets the number of bytes in each sector.
     */
    private int __BytesPerSector;

    public int getBytesPerSector() {
        return __BytesPerSector;
    }

    /**
     * Gets the total capacity of the disk (in bytes).
     */
    public long getCapacity() {
        return getTotalSectorsLong() * getBytesPerSector();
    }

    /**
     * Gets the number of cylinders.
     */
    private int __Cylinders;

    public int getCylinders() {
        return __Cylinders;
    }

    /**
     * Gets the number of heads (aka platters).
     */
    private int __HeadsPerCylinder;

    public int getHeadsPerCylinder() {
        return __HeadsPerCylinder;
    }

    /**
     * Gets a value indicating whether the Geometry is representable both by the
     * BIOS and by IDE.
     */
    public boolean getIsBiosAndIdeSafe() {
        return getCylinders() <= 1024 && getHeadsPerCylinder() <= 16 && getSectorsPerTrack() <= 63;
    }

    /**
     * Gets a value indicating whether the Geometry is consistent with the
     * values a BIOS can support.
     */
    public boolean getIsBiosSafe() {
        return getCylinders() <= 1024 && getHeadsPerCylinder() <= 255 && getSectorsPerTrack() <= 63;
    }

    /**
     * Gets a value indicating whether the Geometry is consistent with the
     * values IDE can represent.
     */
    public boolean getIsIdeSafe() {
        return getCylinders() <= 65536 && getHeadsPerCylinder() <= 16 && getSectorsPerTrack() <= 255;
    }

    /**
     * Gets the address of the last sector on the disk.
     */
    public ChsAddress getLastSector() {
        return new ChsAddress(getCylinders() - 1, getHeadsPerCylinder() - 1, getSectorsPerTrack());
    }

    /**
     * Gets a null geometry, which has 512-byte sectors but zero sectors, tracks
     * or cylinders.
     */
    public static Geometry getNull() {
        return new Geometry(0, 0, 0, 512);
    }

    /**
     * Gets the number of sectors per track.
     */
    private int __SectorsPerTrack;

    public int getSectorsPerTrack() {
        return __SectorsPerTrack;
    }

    /**
     * Gets the total size of the disk (in sectors).
     */
    public int getTotalSectors() {
        return getCylinders() * getHeadsPerCylinder() * getSectorsPerTrack();
    }

    /**
     * Gets the total size of the disk (in sectors).
     */
    public long getTotalSectorsLong() {
        return getCylinders() * (long) getHeadsPerCylinder() * getSectorsPerTrack();
    }

    /**
     * Gets the 'Large' BIOS geometry for a disk, given it's physical geometry.
     * 
     * @param ideGeometry The physical (aka IDE) geometry of the disk.
     * @return The geometry a BIOS using the 'Large' method for calculating disk
     *         geometry will indicate for the disk.
     */
    public static Geometry largeBiosGeometry(Geometry ideGeometry) {
        int cylinders = ideGeometry.getCylinders();
        int heads = ideGeometry.getHeadsPerCylinder();
        int sectors = ideGeometry.getSectorsPerTrack();
        while (cylinders > 1024 && heads <= 127) {
            cylinders >>= 1;
            heads <<= 1;
        }
        return new Geometry(cylinders, heads, sectors);
    }

    /**
     * Gets the 'LBA Assisted' BIOS geometry for a disk, given it's capacity.
     * 
     * @param capacity The capacity of the disk.
     * @return The geometry a BIOS using the 'LBA Assisted' method for
     *         calculating disk geometry will indicate for the disk.
     */
    public static Geometry lbaAssistedBiosGeometry(long capacity) {
        int heads;
        if (capacity <= 504 * Sizes.OneMiB) {
            heads = 16;
        } else if (capacity <= 1008 * Sizes.OneMiB) {
            heads = 32;
        } else if (capacity <= 2016 * Sizes.OneMiB) {
            heads = 64;
        } else if (capacity <= 4032 * Sizes.OneMiB) {
            heads = 128;
        } else {
            heads = 255;
        }
        int sectors = 63;
        int cylinders = (int) Math.min(1024, capacity / (sectors * (long) heads * Sizes.Sector));
        return new Geometry(cylinders, heads, sectors, Sizes.Sector);
    }

    /**
     * Converts a geometry into one that is BIOS-safe, if not already.
     * 
     * @param geometry The geometry to make BIOS-safe.
     * @param capacity The capacity of the disk.
     * @return The new geometry.This method returns the LBA-Assisted geometry if
     *         the given geometry isn't BIOS-safe.
     */
    public static Geometry makeBiosSafe(Geometry geometry, long capacity) {
        if (geometry == null) {
            return lbaAssistedBiosGeometry(capacity);
        }

        if (geometry.getIsBiosSafe()) {
            return geometry;
        }

        return lbaAssistedBiosGeometry(capacity);
    }

    /**
     * Calculates a sensible disk geometry for a disk capacity using the VHD
     * algorithm (errs under).
     * 
     * @param capacity The desired capacity of the disk.
     * @return The appropriate disk geometry.The geometry returned tends to
     *         produce a disk with less capacity
     *         than requested (an exact capacity is not always possible). The
     *         geometry returned is the IDE
     *         (aka Physical) geometry of the disk, not necessarily the geometry
     *         used by the BIOS.
     */
    public static Geometry fromCapacity(long capacity) {
        return fromCapacity(capacity, Sizes.Sector);
    }

    /**
     * Calculates a sensible disk geometry for a disk capacity using the VHD
     * algorithm (errs under).
     * 
     * @param capacity The desired capacity of the disk.
     * @param sectorSize The logical sector size of the disk.
     * @return The appropriate disk geometry.The geometry returned tends to
     *         produce a disk with less capacity
     *         than requested (an exact capacity is not always possible). The
     *         geometry returned is the IDE
     *         (aka Physical) geometry of the disk, not necessarily the geometry
     *         used by the BIOS.
     */
    public static Geometry fromCapacity(long capacity, int sectorSize) {
        int totalSectors;
        int cylinders;
        int headsPerCylinder;
        int sectorsPerTrack;
        // If more than ~128GB truncate at ~128GB
        if (capacity > 65535 * (long) 16 * 255 * sectorSize) {
            totalSectors = 65535 * 16 * 255;
        } else {
            totalSectors = (int) (capacity / sectorSize);
        }
        // If more than ~32GB, break partition table compatibility.
        // Partition table has max 63 sectors per track.  Otherwise
        // we're looking for a geometry that's valid for both BIOS
        // and ATA.
        if (totalSectors > 65535 * 16 * 63) {
            sectorsPerTrack = 255;
            headsPerCylinder = 16;
        } else {
            sectorsPerTrack = 17;
            int cylindersTimesHeads = totalSectors / sectorsPerTrack;
            headsPerCylinder = (cylindersTimesHeads + 1023) / 1024;
            if (headsPerCylinder < 4) {
                headsPerCylinder = 4;
            }

            // If we need more than 1023 cylinders, or 16 heads, try more sectors per track
            if (cylindersTimesHeads >= headsPerCylinder * 1024 || headsPerCylinder > 16) {
                sectorsPerTrack = 31;
                headsPerCylinder = 16;
                cylindersTimesHeads = totalSectors / sectorsPerTrack;
            }

            // We need 63 sectors per track to keep the cylinder count down
            if (cylindersTimesHeads >= headsPerCylinder * 1024) {
                sectorsPerTrack = 63;
                headsPerCylinder = 16;
            }

        }
        cylinders = totalSectors / sectorsPerTrack / headsPerCylinder;
        return new Geometry(cylinders, headsPerCylinder, sectorsPerTrack, sectorSize);
    }

    /**
     * Converts a CHS (Cylinder,Head,Sector) address to a LBA (Logical Block
     * Address).
     * 
     * @param chsAddress The CHS address to convert.
     * @return The Logical Block Address (in sectors).
     */
    public long toLogicalBlockAddress(ChsAddress chsAddress) {
        return toLogicalBlockAddress(chsAddress.getCylinder(), chsAddress.getHead(), chsAddress.getSector());
    }

    /**
     * Converts a CHS (Cylinder,Head,Sector) address to a LBA (Logical Block
     * Address).
     * 
     * @param cylinder The cylinder of the address.
     * @param head The head of the address.
     * @param sector The sector of the address.
     * @return The Logical Block Address (in sectors).
     */
    public long toLogicalBlockAddress(int cylinder, int head, int sector) {
        if (cylinder < 0) {
            throw new IndexOutOfBoundsException("cylinder number is negative");
        }

        if (head >= getHeadsPerCylinder()) {
            throw new IndexOutOfBoundsException("head number is larger than disk geometry");
        }

        if (head < 0) {
            throw new IndexOutOfBoundsException("head number is negative");
        }

        if (sector > getSectorsPerTrack()) {
            throw new IndexOutOfBoundsException("sector number is larger than disk geometry");
        }

        if (sector < 1) {
            throw new IndexOutOfBoundsException("sector number is less than one (sectors are 1-based)");
        }

        return (cylinder * (long) getHeadsPerCylinder() + head) * getSectorsPerTrack() + sector - 1;
    }

    /**
     * Converts a LBA (Logical Block Address) to a CHS (Cylinder, Head, Sector)
     * address.
     * 
     * @param logicalBlockAddress The logical block address (in sectors).
     * @return The address in CHS form.
     */
    public ChsAddress toChsAddress(long logicalBlockAddress) {
        if (logicalBlockAddress < 0) {
            throw new IndexOutOfBoundsException("Logical Block Address is negative");
        }

        int cylinder = (int) (logicalBlockAddress / (getHeadsPerCylinder() * getSectorsPerTrack()));
        int temp = (int) (logicalBlockAddress % (getHeadsPerCylinder() * getSectorsPerTrack()));
        int head = temp / getSectorsPerTrack();
        int sector = temp % getSectorsPerTrack() + 1;
        return new ChsAddress(cylinder, head, sector);
    }

    /**
     * Translates an IDE (aka Physical) geometry to a BIOS (aka Logical)
     * geometry.
     * 
     * @param translation The translation to perform.
     * @return The translated disk geometry.
     */
    public Geometry translateToBios(GeometryTranslation translation) {
        return translateToBios(0, translation);
    }

    /**
     * Translates an IDE (aka Physical) geometry to a BIOS (aka Logical)
     * geometry.
     * 
     * @param capacity The capacity of the disk, required if the geometry is an
     *            approximation on the actual disk size.
     * @param translation The translation to perform.
     * @return The translated disk geometry.
     */
    public Geometry translateToBios(long capacity, GeometryTranslation translation) {
        if (capacity <= 0) {
            capacity = getTotalSectorsLong() * 512L;
        }

        switch (translation) {
        case None:
            return this;
        case Auto:
            if (getIsBiosSafe()) {
                return this;
            }

            return lbaAssistedBiosGeometry(capacity);
        case Lba:
            return lbaAssistedBiosGeometry(capacity);
        case Large:
            return largeBiosGeometry(this);
        default:
            throw new IllegalArgumentException(String.format("Translation mode '%s' not yet implemented", translation));

        }
    }

    /**
     * Determines if this object is equivalent to another.
     * 
     * @param obj The object to test against.
     * @return
     *         {@code true}
     *         if the
     *         {@code obj}
     *         is equivalent, else
     *         {@code false}
     *         .
     */
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        Geometry other = (Geometry) obj;
        return getCylinders() == other.getCylinders() && getHeadsPerCylinder() == other.getHeadsPerCylinder() &&
               getSectorsPerTrack() == other.getSectorsPerTrack() && getBytesPerSector() == other.getBytesPerSector();
    }

    /**
     * Calculates the hash code for this object.
     * 
     * @return The hash code.
     */
    public int hashCode() {
        return getCylinders() ^ getHeadsPerCylinder() ^ getSectorsPerTrack() ^ getBytesPerSector();
    }

    /**
     * Gets a string representation of this object, in the form (C/H/S).
     * 
     * @return The string representation.
     */
    public String toString() {
        if (getBytesPerSector() == 512) {
            return "(" + getCylinders() + "/" + getHeadsPerCylinder() + "/" + getSectorsPerTrack() + ")";
        }

        return "(" + getCylinders() + "/" + getHeadsPerCylinder() + "/" + getSectorsPerTrack() + ":" + getBytesPerSector() +
               ")";
    }
}
