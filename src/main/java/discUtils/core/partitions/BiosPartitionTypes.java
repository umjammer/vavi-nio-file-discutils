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

/**
 * Convenient access to well-known BIOS (MBR) Partition Types.
 */
public class BiosPartitionTypes {
    /**
     * Microsoft FAT12 (fewer than 32,680 sectors in the volume).
     */
    public static final byte Fat12 = 0x01;

    /**
     * Microsoft FAT16 (32,680-65,535 sectors or 16 MB-33 MB).
     */
    public static final byte Fat16Small = 0x04;

    /**
     * Extended Partition (contains other partitions).
     */
    public static final byte Extended = 0x05;

    /**
     * Microsoft BIGDOS FAT16 (33 MB-4 GB).
     */
    public static final byte Fat16 = 0x06;

    /**
     * Installable File System (NTFS).
     */
    public static final byte Ntfs = 0x07;

    /**
     * Microsoft FAT32.
     */
    public static final byte Fat32 = 0x0B;

    /**
     * Microsoft FAT32, accessed using Int13h BIOS LBA extensions.
     */
    public static final byte Fat32Lba = 0x0C;

    /**
     * Microsoft BIGDOS FAT16, accessed using Int13h BIOS LBA extensions.
     */
    public static final byte Fat16Lba = 0x0E;

    /**
     * Extended Partition (contains other partitions), accessed using Int13h
     * BIOS LBA extensions.
     */
    public static final byte ExtendedLba = 0x0F;

    /**
     * Windows Logical Disk Manager dynamic volume.
     */
    public static final byte WindowsDynamicVolume = 0x42;

    /**
     * Linux swap.
     */
    public static final byte LinuxSwap = (byte) 0x82;

    /**
     * Linux Native (ext2 and friends).
     */
    public static final byte LinuxNative = (byte) 0x83;

    /**
     * Linux Logical Volume Manager (LVM).
     */
    public static final byte LinuxLvm = (byte) 0x8E;

    /**
     * GUID Partition Table (GPT) protective partition, fills entire disk.
     */
    public static final byte GptProtective = (byte) 0xEE;

    /**
     * EFI System partition on an MBR disk.
     */
    public static final byte EfiSystem = (byte) 0xEF;

    /**
     * Provides a string representation of some known BIOS partition types.
     *
     * @param type The partition type to represent as a string.
     * @return The string representation.
     */
    public static String toString(byte type) {
        return switch (type) {
            case 0x00 -> "Unused";
            case 0x01 -> "FAT12";
            case 0x02 -> "XENIX root";
            case 0x03 -> "XENIX /usr";
            case 0x04 -> "FAT16 (<32M)";
            case 0x05 -> "Extended (non-LBA)";
            case 0x06 -> "FAT16 (>32M)";
            case 0x07 -> "IFS (NTFS or HPFS)";
            case 0x0B -> "FAT32 (non-LBA)";
            case 0x0C -> "FAT32 (LBA)";
            case 0x0E -> "FAT16 (LBA)";
            case 0x0F -> "Extended (LBA)";
            case 0x11 -> "Hidden FAT12";
            case 0x12 -> "Vendor Config/Recovery/diagnostics";
            case 0x14 -> "Hidden FAT16 (<32M)";
            case 0x16 -> "Hidden FAT16 (>32M)";
            case 0x17 -> "Hidden IFS (NTFS or HPFS)";
            case 0x1B -> "Hidden FAT32 (non-LBA)";
            case 0x1C -> "Hidden FAT32 (LBA)";
            case 0x1E -> "Hidden FAT16 (LBA)";
            case 0x27 -> "Windows Recovery Environment";
            case 0x42 -> "Windows Dynamic Volume";
            case (byte) 0x80 -> "Minix v1.1 - v1.4a";
            case (byte) 0x81 -> "Minix / Early Linux";
            case (byte) 0x82 -> "Linux swap";
            case (byte) 0x83 -> "Linux Native";
            case (byte) 0x84 -> "Hibernation";
            case (byte) 0x8E -> "Linux LVM";
            case (byte) 0xA0 -> "Laptop Hibernation";
            case (byte) 0xA8 -> "Mac OS-X";
            case (byte) 0xAB -> "Mac OS-X Boot";
            case (byte) 0xAF -> "Mac OS-X HFS";
            case (byte) 0xC0 -> "NTFT";
            case (byte) 0xDE -> "Dell OEM";
            case (byte) 0xEE -> "GPT Protective";
            case (byte) 0xEF -> "EFI";
            case (byte) 0xFB -> "VMware File System";
            case (byte) 0xFC -> "VMware swap";
            case (byte) 0xFE -> "IBM OEM";
            default -> "Unknown";
        };
    }
}
