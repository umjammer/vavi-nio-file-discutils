//
// Aaru Data Preservation Suite
//
//
// Filename       : Structs.cs
// Author(s)      : Natalia Portillo <claunia@claunia.com>
//
// Component      : Disk image plugins.
//
// Description
//
//     Contains classures for MAME Compressed Hunks of Data disk images.
//
// License
//
//     This library is free software; you can redistribute it and/or modify
//     it under the terms of the GNU Lesser General Public License as
//     published by the Free Software Foundation; either version 2.1 of the
//     License, or (at your option) any later version.
//
//     This library is distributed in the hope that it will be useful, but
//     WITHOUT ANY WARRANTY; without even the implied warranty of
//     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//     Lesser General Public License for more details.
//
//     You should have received a copy of the GNU Lesser General Public
//     License along with this library; if not, see <http://www.gnu.org/licenses/>.
//
//
// Copyright © 2011-2021 Natalia Portillo
//

package aaru.image.chd;

import vavi.util.serdes.Element;
import vavi.util.serdes.Serdes;


public class Structs {

    /**
     * Hunks are represented in a 64 bit integer with 44 bit as offset, 20 bits as length
     * Sectors are fixed at 512 bytes/sector
     */
    @Serdes
    static class HeaderV1 {
        /** Magic identifier, 'MComprHD' */
        @Element(sequence = 1, value = "8")
        public byte[] tag;
        /** Length of header */
        @Element(sequence = 2)
        public int length;
        /** Image format version */
        @Element(sequence = 3)
        public int version;
        /** Image flags, @see Enum.flags */
        @Element(sequence = 4)
        public int flags;
        /** Compression algorithm, @see Enum.Compression */
        @Element(sequence = 5)
        public int compression;
        /** Sectors per hunk */
        @Element(sequence = 6)
        public int hunksize;
        /** Total # of hunk in image */
        @Element(sequence = 7)
        public int totalhunks;
        /** cylinders on disk */
        @Element(sequence = 8)
        public int cylinders;
        /** heads per cylinder */
        @Element(sequence = 9)
        public int heads;
        /** Sectors per track */
        @Element(sequence = 10)
        public int sectors;
        /** MD5 of raw data */
        @Element(sequence = 11, value = "16")
        public byte[] md5;
        /** MD5 of parent file */
        @Element(sequence = 12, value = "16")
        public byte[] parentMd5;
    }

    /** Hunks are represented in a 64 bit integer with 44 bit as offset, 20 bits as length */
    @Serdes
    static class HeaderV2 {
        /** Magic identifier, 'MComprHD' */
        @Element(sequence = 1, value = "8")
        public byte[] tag;
        /** Length of header */
        @Element(sequence = 2)
        public int length;
        /** Image format version */
        @Element(sequence = 3)
        public int version;
        /** Image flags, @see Enums.flags */
        @Element(sequence = 4)
        public int flags;
        /** Compression algorithm, @see Enums.Compression */
        @Element(sequence = 5)
        public int compression;
        /** Sectors per hunk */
        @Element(sequence = 6)
        public int hunkSize;
        /** Total # of hunk in image */
        @Element(sequence = 7)
        public int totalHunks;
        /** cylinders on disk */
        @Element(sequence = 8)
        public int cylinders;
        /** heads per cylinder */
        @Element(sequence = 9)
        public int heads;
        /** Sectors per track */
        @Element(sequence = 10)
        public int sectors;
        /** MD5 of raw data */
        @Element(sequence = 11, value = "16")
        public byte[] md5;
        /** MD5 of parent file */
        @Element(sequence = 12, value = "16")
        public byte[] parentMd5;
        /** Bytes per sector */
        @Element(sequence = 13)
        public int seclen;
    }

    @Serdes
    static class HeaderV3 {
        /** Magic identifier, 'MComprHD' */
        @Element(sequence = 1, value = "8")
        public byte[] tag;
        /** Length of header */
        @Element(sequence = 2)
        public int length;
        /** Image format version */
        @Element(sequence = 3)
        public int version;
        /** Image flags, @see Enums.flags */
        @Element(sequence = 4)
        public int flags;
        /** Compression algorithm, @see Enums.Compression */
        @Element(sequence = 5)
        public int compression;
        /** Total # of hunk in image */
        @Element(sequence = 6)
        public int totalHunks;
        /** Total bytes in image */
        @Element(sequence = 7)
        public long logicalBytes;
        /** Offset to first metadata blob */
        @Element(sequence = 8)
        public long metaOffset;
        /** MD5 of raw data */
        @Element(sequence = 9, value = "16")
        public byte[] md5;
        /** MD5 of parent file */
        @Element(sequence = 10, value = "16")
        public byte[] parentMd5;
        /** Bytes per hunk */
        @Element(sequence = 11)
        public int hunkBytes;
        /** SHA1 of raw data */
        @Element(sequence = 12, value = "20")
        public byte[] sha1;
        /** SHA1 of parent file */
        @Element(sequence = 13, value = "20")
        public byte[] parentSha1;
    }

    @Serdes
    static class MapEntryV3 {
        /** Offset to hunk from start of image */
        @Element(sequence = 1)
        public long offset;
        /** CRC32 of uncompressed hunk */
        @Element(sequence = 2)
        public int crc;
        /** Lower 16 bits of length */
        @Element(sequence = 3)
        public short lengthLsb;
        /** Upper 8 bits of length */
        @Element(sequence = 4)
        public byte length;
        /** Hunk flags */
        @Element(sequence = 5)
        public byte flags;
    }

    @Serdes
    static class TrackOld {
        @Element(sequence = 1)
        public int type;
        @Element(sequence = 2)
        public int subType;
        @Element(sequence = 3)
        public int dataSize;
        @Element(sequence = 4)
        public int subSize;
        @Element(sequence = 5)
        public int frames;
        @Element(sequence = 6)
        public int extraFrames;
    }

    @Serdes
    static class HeaderV4 {
        /** Magic identifier, 'MComprHD' */
        @Element(sequence = 1, value = "8")
        public byte[] tag;
        /** Length of header */
        @Element(sequence = 2)
        public int length;
        /** Image format version */
        @Element(sequence = 3)
        public int version;
        /** Image flags, @see Enums.flags */
        @Element(sequence = 4)
        public int flags;
        /** Compression algorithm, @see Enums.Compression */
        @Element(sequence = 5)
        public int compression;
        /** Total # of hunk in image */
        @Element(sequence = 6)
        public int totalHunks;
        /** Total bytes in image */
        @Element(sequence = 7)
        public long logicalBytes;
        /** Offset to first metadata blob */
        @Element(sequence = 8)
        public long metaOffset;
        /** Bytes per hunk */
        @Element(sequence = 9)
        public int hunkBytes;
        /** SHA1 of raw+meta data */
        @Element(sequence = 10, value = "20")
        public byte[] sha1;
        /** SHA1 of parent file */
        @Element(sequence = 11, value = "20")
        public byte[] parentSha1;
        /** SHA1 of raw data */
        @Element(sequence = 12, value = "20")
        public byte[] rawSha1;
    }

    @Serdes
    static class HeaderV5 {
        /** Magic identifier, 'MComprHD' */
        @Element(sequence = 1, value = "8")
        public byte[] tag;
        /** Length of header */
        @Element(sequence = 2)
        public int length;
        /** Image format version */
        @Element(sequence = 3)
        public int version;
        /** Compressor 0 */
        @Element(sequence = 4)
        public int compressor0;
        /** Compressor 1 */
        @Element(sequence = 5)
        public int compressor1;
        /** Compressor 2 */
        @Element(sequence = 6)
        public int compressor2;
        /** Compressor 3 */
        @Element(sequence = 7)
        public int compressor3;
        /** Total bytes in image */
        @Element(sequence = 8)
        public long logicalBytes;
        /** Offset to hunk map */
        @Element(sequence = 9)
        public long mapOffset;
        /** Offset to first metadata blob */
        @Element(sequence = 10)
        public long metaOffset;
        /** Bytes per hunk */
        @Element(sequence = 11)
        public int hunkBytes;
        /** Bytes per unit within hunk */
        @Element(sequence = 12)
        public int unitBytes;
        /** SHA1 of raw data */
        @Element(sequence = 13, value = "20")
        public byte[] rawsha1;
        /** SHA1 of raw+meta data */
        @Element(sequence = 14, value = "20")
        public byte[] sha1;
        /** SHA1 of parent file */
        @Element(sequence = 15, value = "20")
        public byte[] parentsha1;
    }

    @Serdes
    static class CompressedMapHeaderV5 {
        /** Length of compressed map */
        @Element(sequence = 1)
        public int length;
        /** Offset of first block (48 bits) and CRC16 of map (16 bits) */
        @Element(sequence = 2)
        public long startAndCrc;
        /** Bits used to encode compressed length on map entry */
        @Element(sequence = 3)
        public byte bitsUsedToEncodeCompLength;
        /** Bits used to encode self-refs */
        @Element(sequence = 4)
        public byte bitsUsedToEncodeSelfRefs;
        /** Bits used to encode parent unit refs */
        @Element(sequence = 5)
        public byte bitsUsedToEncodeParentUnits;
        @Element(sequence = 6)
        public byte reserved;
    }

    @Serdes
    static class MapEntryV5 {
        /** Compression (8 bits) and length (24 bits) */
        @Element(sequence = 1)
        public int compAndLength;
        /** Offset (48 bits) and CRC (16 bits) */
        @Element(sequence = 2)
        public long offsetAndCrc;
    }

    @Serdes
    static class MetadataHeader {
        @Element(sequence = 1)
        public int tag;
        @Element(sequence = 2)
        public int flagsAndLength;
        @Element(sequence = 3)
        public long next;
    }

    @Serdes(bigEndian = false)
    static class HunkSector {
        @Element(sequence = 1, value = "64")
        public long[] hunkEntry;
    }

    @Serdes(bigEndian = false)
    static class HunkSectorSmall {
        @Element(sequence = 1, value = "128")
        public int[] hunkEntry;
    }
}
