//
// Aaru Data Preservation Suite
//
//
// Filename       : CHD.cs
// Author(s)      : Natalia Portillo <claunia@claunia.com>
//
// Component      : Disc image plugins.
//
// [ Description ]
//
//     Manages MAME Compressed Hunks of Data disk images.
//
// [ License ]
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


import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import aaru.checksum.CdChecksums;
import aaru.commonType.ImageInfo;
import aaru.commonType.MediaTagType;
import aaru.commonType.Partition;
import aaru.commonType.SectorTagType;
import aaru.commonType.Track;
import aaru.commonType.TrackSubChannelType;
import aaru.commonType.TrackType;
import aaru.commonType.XmlMediaType;
import aaru.commonType.device.ata.Identify;
import aaru.decoder.Sector;
import aaru.decoder.SectorBuilder;
import discUtils.iscsi.Session;
import dotnet4j.io.IOException;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;
import dotnet4j.io.compression.CompressionMode;
import dotnet4j.io.compression.DeflateStream;
import dotnet4j.util.compat.ArrayHelpers;
import vavi.util.ByteUtil;
import vavi.util.Debug;
import vavi.util.StringUtil;
import vavi.util.serdes.Serdes;


/**
 * Implements reading MAME CHD disk images
 * <p>
 * TODO: Implement PCMCIA support
 */
public class Chd {
    /** "MComprHD" */
    static final byte[] chdTag = {
            0x4D, 0x43, 0x6F, 0x6D, 0x70, 0x72, 0x48, 0x44
    };
    SectorBuilder sectorBuilder;
    int bytesPerHunk;
    byte[] cis;
    byte[] expectedChecksum;
    int hdrCompression;
    int hdrCompression1;
    int hdrCompression2;
    int hdrCompression3;
    Map<Long, byte[]> hunkCache;
    byte[] hunkMap;
    long[] hunkTable;
    int[] hunkTableSmall;
    byte[] identify;
    ImageInfo imageInfo;
    Stream imageStream;
    boolean isCdrom;
    boolean isGdrom;
    boolean isHdd;
    int mapVersion;
    int maxBlockCache;
    int maxSectorCache;
    Map<Long, Integer> offsetMap;
    List<Partition> partitions;
    Map<Long, byte[]> sectorCache;
    int sectorsPerHunk;
    boolean swapAudio;
    int totalHunks;
    Map<Integer, Track> tracks;

    public Chd() {
        imageInfo = new ImageInfo() {
            {
                readableSectorTags = new ArrayList<>();
                readableMediaTags = new ArrayList<>();
                hasPartitions = false;
                hasSessions = false;
                application = "MAME";
                creator = null;
                comments = null;
                mediaManufacturer = null;
                mediaModel = null;
                mediaSerialNumber = null;
                mediaBarcode = null;
                mediaPartNumber = null;
                mediaSequence = 0;
                lastMediaSequence = 0;
                driveManufacturer = null;
                driveModel = null;
                driveSerialNumber = null;
                driveFirmwareRevision = null;
            }
        };
    }

//#region Properties

    /**  */
    public ImageInfo getInfo() {
        return imageInfo;
    }

    /**  */
    public String getName() {
        return "MAME Compressed Hunks of Data";
    }

    /**  */
    public UUID getId() {
        return UUID.fromString("0D50233A-08BD-47D4-988B-27EAA0358597");
    }

    /**  */
    public String getFormat() {
        return "Compressed Hunks of Data";
    }

    /**  */
    public String getAuthor() {
        return "Natalia Portillo";
    }

    /**  */
    public List<Partition> getPartitions() {
        return isHdd ? null : partitions;
    }

    /**  */
    public List<Track> getTracks() {
        return isHdd ? null : new ArrayList<>(tracks.values());
    }

    /**  */
    public List<Session> getSessions() {
        if (isHdd)
            return null;

        // TODO: Implement
        return null;
    }

//#endregion

//#region Read

    /**  */
    public int open(Stream stream) {
        stream.seek(0, SeekOrigin.Begin);
        byte[] magic = new byte[8];
        stream.read(magic, 0, 8);

        if (!Arrays.equals(chdTag, magic)) {
            Debug.println(StringUtil.getDump(magic));
            throw new IllegalArgumentException("invalid magic");
        }

        // Read length
        byte[] buffer = new byte[4];
        stream.read(buffer, 0, 4);
        int length = ByteUtil.readBeInt(buffer, 0);
        Debug.println("length: " + length);
        buffer = new byte[4];
        stream.read(buffer, 0, 4);
        int version = ByteUtil.readBeInt(buffer, 0);
        Debug.println("version: " + version);

        buffer = new byte[length];
        stream.seek(0, SeekOrigin.Begin);
        stream.read(buffer, 0, length);

        long nextMetaOff = 0;

        switch (version) {
        case 1: {
            Structs.HeaderV1 hdrV1 = new Structs.HeaderV1();
            try {
                Serdes.Util.deserialize(new ByteArrayInputStream(buffer), hdrV1);
            } catch (java.io.IOException e) {
                throw new IOException(e);
            }

            Debug.printf("CHD plugin: hdrV1.tag = \"%d\"",
                    new String(hdrV1.tag), StandardCharsets.US_ASCII);

            Debug.printf("CHD plugin: hdrV1.length = %d bytes", hdrV1.length);
            Debug.printf("CHD plugin: hdrV1.version = %d", hdrV1.version);
            Debug.printf("CHD plugin: hdrV1.flags = %d", Enums.Flags.values()[hdrV1.flags]);

            Debug.printf("CHD plugin: hdrV1.compression = %s", hdrV1.compression);

            Debug.printf("CHD plugin: hdrV1.hunkSize = %d", hdrV1.hunksize);
            Debug.printf("CHD plugin: hdrV1.totalHunks = %d", hdrV1.totalhunks);
            Debug.printf("CHD plugin: hdrV1.cylinders = %d", hdrV1.cylinders);
            Debug.printf("CHD plugin: hdrV1.heads = %d", hdrV1.heads);
            Debug.printf("CHD plugin: hdrV1.sectors = %d", hdrV1.sectors);
            Debug.printf("CHD plugin: hdrV1.md5 = %d", ByteUtil.toHexString(hdrV1.md5));

            Debug.printf("CHD plugin: hdrV1.parentMd5 = %d",
                    ArrayHelpers.isArrayNullOrEmpty(hdrV1.parentMd5) ? "null"
                            : ByteUtil.toHexString(hdrV1.parentMd5));

            Debug.printf("CHD plugin: Reading Hunk map.");
            Instant start = Instant.now();

            hunkTable = new long[hdrV1.totalhunks];

            int hunkSectorCount = (int) Math.ceil((double) hdrV1.totalhunks * 8 / 512);

            byte[] hunkSectorBytes = new byte[512];

            for (int i = 0; i < hunkSectorCount; i++) {
                stream.read(hunkSectorBytes, 0, 512);

                // This does the big-endian trick but reverses the order of elements also
                ArrayHelpers.reverse(hunkSectorBytes);
                Structs.HunkSector hunkSector = new Structs.HunkSector();
                try {
                    Serdes.Util.deserialize(new ByteArrayInputStream(hunkSectorBytes), hunkSector);
                } catch (java.io.IOException e) {
                    throw new IOException(e);
                }

                // This restores the order of elements
                ArrayHelpers.reverse(hunkSector.hunkEntry);

                if (hunkTable.length >= (i * 512 / 8) + (512 / 8))
                    System.arraycopy(hunkSector.hunkEntry, 0, hunkTable, i * 512 / 8, 512 / 8);
                else
                    System.arraycopy(hunkSector.hunkEntry, 0, hunkTable, i * 512 / 8,
                            hunkTable.length - (i * 512 / 8));
            }

            Instant end = Instant.now();
            Debug.printf("CHD plugin: Took %d seconds", end.minusSeconds(start.getEpochSecond()));

            imageInfo.mediaType = "GENERIC_HDD";
            imageInfo.sectors = (long) hdrV1.hunksize * hdrV1.totalhunks;
            imageInfo.xmlMediaType = XmlMediaType.BlockMedia;
            imageInfo.sectorSize = 512;
            imageInfo.version = "1";
            imageInfo.imageSize = (long) imageInfo.sectorSize * hdrV1.hunksize * hdrV1.totalhunks;

            totalHunks = hdrV1.totalhunks;
            sectorsPerHunk = hdrV1.hunksize;
            hdrCompression = hdrV1.compression;
            mapVersion = 1;
            isHdd = true;

            imageInfo.cylinders = hdrV1.cylinders;
            imageInfo.heads = hdrV1.heads;
            imageInfo.sectorsPerTrack = hdrV1.sectors;

            break;
        }

        case 2: {
            Structs.HeaderV2 hdrV2 = new Structs.HeaderV2();
            try {
                Serdes.Util.deserialize(new ByteArrayInputStream(buffer), hdrV2);
            } catch (java.io.IOException e) {
                throw new IOException(e);
            }

            Debug.printf("CHD plugin: hdrV2.tag = \"%d\"",
                    new String(hdrV2.tag), StandardCharsets.US_ASCII);

            Debug.printf("CHD plugin: hdrV2.length = %d bytes", hdrV2.length);
            Debug.printf("CHD plugin: hdrV2.version = %d", hdrV2.version);
            Debug.printf("CHD plugin: hdrV2.flags = %d", Enums.Flags.values()[hdrV2.flags]);

            Debug.printf("CHD plugin: hdrV2.compression = %s", Enums.Compression.values()[hdrV2.compression]);

            Debug.printf("CHD plugin: hdrV2.hunkSize = %d", hdrV2.hunkSize);
            Debug.printf("CHD plugin: hdrV2.totalHunks = %d", hdrV2.totalHunks);
            Debug.printf("CHD plugin: hdrV2.cylinders = %d", hdrV2.cylinders);
            Debug.printf("CHD plugin: hdrV2.heads = %d", hdrV2.heads);
            Debug.printf("CHD plugin: hdrV2.sectors = %d", hdrV2.sectors);
            Debug.printf("CHD plugin: hdrV2.md5 = %d", ByteUtil.toHexString(hdrV2.md5));

            Debug.printf("CHD plugin: hdrV2.parentMd5 = %s",
                    ArrayHelpers.isArrayNullOrEmpty(hdrV2.parentMd5) ? "null"
                            : ByteUtil.toHexString(hdrV2.parentMd5));

            Debug.printf("CHD plugin: hdrV2.seclen = %d", hdrV2.seclen);

            Debug.printf("CHD plugin: Reading Hunk map.");
            Instant start = Instant.now();

            hunkTable = new long[hdrV2.totalHunks];

            // How many sectors uses the BAT
            int hunkSectorCount = (int) Math.ceil((double) hdrV2.totalHunks * 8 / 512);

            byte[] hunkSectorBytes = new byte[512];

            for (int i = 0; i < hunkSectorCount; i++) {
                stream.read(hunkSectorBytes, 0, 512);

                // This does the big-endian trick but reverses the order of elements also
                ArrayHelpers.reverse(hunkSectorBytes);
                Structs.HunkSector hunkSector = new Structs.HunkSector();
                try {
                    Serdes.Util.deserialize(new ByteArrayInputStream(hunkSectorBytes), hunkSector);
                } catch (java.io.IOException e) {
                    throw new IOException(e);
                }

                // This restores the order of elements
                ArrayHelpers.reverse(hunkSector.hunkEntry);

                if (hunkTable.length >= (i * 512 / 8) + (512 / 8))
                    System.arraycopy(hunkSector.hunkEntry, 0, hunkTable, i * 512 / 8, 512 / 8);
                else
                    System.arraycopy(hunkSector.hunkEntry, 0, hunkTable, i * 512 / 8,
                            hunkTable.length - (i * 512 / 8));
            }

            Instant end = Instant.now();
            Debug.printf("CHD plugin: Took %d seconds", end.minusSeconds(start.getEpochSecond()));

            imageInfo.mediaType = "GENERIC_HDD";
            imageInfo.sectors = (long) hdrV2.hunkSize * hdrV2.totalHunks;
            imageInfo.xmlMediaType = XmlMediaType.BlockMedia;
            imageInfo.sectorSize = hdrV2.seclen;
            imageInfo.version = "2";
            imageInfo.imageSize = (long) imageInfo.sectorSize * hdrV2.hunkSize * hdrV2.totalHunks;

            totalHunks = hdrV2.totalHunks;
            sectorsPerHunk = hdrV2.hunkSize;
            hdrCompression = hdrV2.compression;
            mapVersion = 1;
            isHdd = true;

            imageInfo.cylinders = hdrV2.cylinders;
            imageInfo.heads = hdrV2.heads;
            imageInfo.sectorsPerTrack = hdrV2.sectors;

            break;
        }

        case 3: {
            Structs.HeaderV3 hdrV3 = new Structs.HeaderV3();
            try {
                Serdes.Util.deserialize(new ByteArrayInputStream(buffer), hdrV3);
            } catch (java.io.IOException e) {
                throw new IOException(e);
            }

            Debug.printf("CHD plugin: hdrV3.tag = \"%d\"",
                    new String(hdrV3.tag), StandardCharsets.US_ASCII);

            Debug.printf("CHD plugin: hdrV3.length = %d bytes", hdrV3.length);
            Debug.printf("CHD plugin: hdrV3.version = %d", hdrV3.version);
            Debug.printf("CHD plugin: hdrV3.flags = %d", Enums.Flags.values()[hdrV3.flags]);

            Debug.printf("CHD plugin: hdrV3.compression = %s", Enums.Compression.values()[hdrV3.compression]);

            Debug.printf("CHD plugin: hdrV3.totalHunks = %d", hdrV3.totalHunks);
            Debug.printf("CHD plugin: hdrV3.logicalBytes = %d", hdrV3.logicalBytes);
            Debug.printf("CHD plugin: hdrV3.metaOffset = %d", hdrV3.metaOffset);
            Debug.printf("CHD plugin: hdrV3.md5 = %d", ByteUtil.toHexString(hdrV3.md5));

            Debug.printf("CHD plugin: hdrV3.parentMd5 = %d",
                    ArrayHelpers.isArrayNullOrEmpty(hdrV3.parentMd5) ? "null"
                            : ByteUtil.toHexString(hdrV3.parentMd5));

            Debug.printf("CHD plugin: hdrV3.hunkBytes = %d", hdrV3.hunkBytes);

            Debug.printf("CHD plugin: hdrV3.sha1 = %s",
                    ByteUtil.toHexString(hdrV3.sha1));

            Debug.printf("CHD plugin: hdrV3.parentSha1 = %s",
                    ArrayHelpers.isArrayNullOrEmpty(hdrV3.parentSha1) ? "null"
                            : ByteUtil.toHexString(hdrV3.parentSha1));

            Debug.printf("CHD plugin: Reading Hunk map.");
            Instant start = Instant.now();

            hunkMap = new byte[hdrV3.totalHunks * 16];
            stream.read(hunkMap, 0, hunkMap.length);

            Instant end = Instant.now();
            Debug.printf("CHD plugin: Took %d seconds", end.minusSeconds(start.getEpochSecond()));

            nextMetaOff = hdrV3.metaOffset;

            imageInfo.imageSize = hdrV3.logicalBytes;
            imageInfo.version = "3";

            totalHunks = hdrV3.totalHunks;
            bytesPerHunk = hdrV3.hunkBytes;
            hdrCompression = hdrV3.compression;
            mapVersion = 3;

            break;
        }

        case 4: {
            Structs.HeaderV4 hdrV4 = new Structs.HeaderV4();
            try {
                Serdes.Util.deserialize(new ByteArrayInputStream(buffer), hdrV4);
            } catch (java.io.IOException e) {
                throw new IOException(e);
            }

            Debug.printf("CHD plugin: hdrV4.tag = \"%d\"",
                    new String(hdrV4.tag), StandardCharsets.US_ASCII);

            Debug.printf("CHD plugin: hdrV4.length = %d bytes", hdrV4.length);
            Debug.printf("CHD plugin: hdrV4.version = %d", hdrV4.version);
            Debug.printf("CHD plugin: hdrV4.flags = %d", Enums.Flags.values()[hdrV4.flags]);

            Debug.printf("CHD plugin: hdrV4.compression = %s", Enums.Compression.values()[hdrV4.compression]);

            Debug.printf("CHD plugin: hdrV4.totalHunks = %d", hdrV4.totalHunks);
            Debug.printf("CHD plugin: hdrV4.logicalBytes = %d", hdrV4.logicalBytes);
            Debug.printf("CHD plugin: hdrV4.metaOffset = %d", hdrV4.metaOffset);
            Debug.printf("CHD plugin: hdrV4.hunkBytes = %d", hdrV4.hunkBytes);

            Debug.printf("CHD plugin: hdrV4.sha1 = %d",
                    ByteUtil.toHexString(hdrV4.sha1));

            Debug.printf("CHD plugin: hdrV4.parentSha1 = %s",
                    ArrayHelpers.isArrayNullOrEmpty(hdrV4.parentSha1) ? "null"
                            : ByteUtil.toHexString(hdrV4.parentSha1));

            Debug.printf("CHD plugin: hdrV4.rawSha1 = %s",
                    ByteUtil.toHexString(hdrV4.rawSha1));

            Debug.printf("CHD plugin: Reading Hunk map.");
            Instant start = Instant.now();

            hunkMap = new byte[hdrV4.totalHunks * 16];
            stream.read(hunkMap, 0, hunkMap.length);

            Instant end = Instant.now();
            Debug.printf("CHD plugin: Took %d seconds", end.minusSeconds(start.getEpochSecond()));

            nextMetaOff = hdrV4.metaOffset;

            imageInfo.imageSize = hdrV4.logicalBytes;
            imageInfo.version = "4";

            totalHunks = hdrV4.totalHunks;
            bytesPerHunk = hdrV4.hunkBytes;
            hdrCompression = hdrV4.compression;
            mapVersion = 3;

            break;
        }

        case 5: {
            // TODO: Check why reading is misaligned
            Debug.println(Level.WARNING, "CHD version 5 is not yet supported.");

            //throw new UnsupportedOperationException();

            Structs.HeaderV5 hdrV5 = new Structs.HeaderV5();
            try {
                Serdes.Util.deserialize(new ByteArrayInputStream(buffer), hdrV5);
            } catch (java.io.IOException e) {
                throw new IOException(e);
            }

            Debug.printf("CHD plugin: hdrV5.tag = \"%s\"",
                    new String(hdrV5.tag, StandardCharsets.US_ASCII));

            Debug.printf("CHD plugin: hdrV5.length = %d bytes", hdrV5.length);
            Debug.printf("CHD plugin: hdrV5.version = %d", hdrV5.version);

            Debug.printf("CHD plugin: hdrV5.compressor0 = \"%s\"",
                    new String(ByteUtil.getLeBytes(hdrV5.compressor0), StandardCharsets.US_ASCII));

            Debug.printf("CHD plugin: hdrV5.compressor1 = \"%s\"",
                    new String(ByteUtil.getLeBytes(hdrV5.compressor1), StandardCharsets.US_ASCII));

            Debug.printf("CHD plugin: hdrV5.compressor2 = \"%s\"",
                    new String(ByteUtil.getLeBytes(hdrV5.compressor2), StandardCharsets.US_ASCII));

            Debug.printf("CHD plugin: hdrV5.compressor3 = \"%s\"",
                    new String(ByteUtil.getLeBytes(hdrV5.compressor3), StandardCharsets.US_ASCII));

            Debug.printf("CHD plugin: hdrV5.logicalBytes = %d", hdrV5.logicalBytes);
            Debug.printf("CHD plugin: hdrV5.mapOffset = %d", hdrV5.mapOffset);
            Debug.printf("CHD plugin: hdrV5.metaOffset = %d", hdrV5.metaOffset);
            Debug.printf("CHD plugin: hdrV5.hunkBytes = %d", hdrV5.hunkBytes);
            Debug.printf("CHD plugin: hdrV5.unitBytes = %d", hdrV5.unitBytes);

            Debug.printf("CHD plugin: hdrV5.sha1 = %s",
                    ByteUtil.toHexString(hdrV5.sha1));

            Debug.printf("CHD plugin: hdrV5.parentSha1 = %s",
                    ArrayHelpers.isArrayNullOrEmpty(hdrV5.parentsha1) ? "null"
                            : ByteUtil.toHexString(hdrV5.parentsha1));

            Debug.printf("CHD plugin: hdrV5.rawSha1 = %s",
                    ByteUtil.toHexString(hdrV5.rawsha1));

            // TODO: Implement compressed CHD v5
            if (hdrV5.compressor0 == 0) {
                Debug.printf("CHD plugin: Reading Hunk map.");
                Instant start = Instant.now();

                hunkTableSmall = new int[(int) hdrV5.logicalBytes / hdrV5.hunkBytes];

                int hunkSectorCount = (int) Math.ceil((double) hunkTableSmall.length * 4 / 512);

                byte[] hunkSectorBytes = new byte[512];

                stream.seek(hdrV5.mapOffset, SeekOrigin.Begin);

                for (int i = 0; i < hunkSectorCount; i++) {
                    stream.read(hunkSectorBytes, 0, 512);

                    // This does the big-endian trick but reverses the order of elements also
                    ArrayHelpers.reverse(hunkSectorBytes);

                    Structs.HunkSectorSmall hunkSector = new Structs.HunkSectorSmall();
                    try {
                        Serdes.Util.deserialize(new ByteArrayInputStream(hunkSectorBytes), hunkSector);
                    } catch (java.io.IOException e) {
                        throw new IOException(e);
                    }

                    // This restores the order of elements
                    ArrayHelpers.reverse(hunkSector.hunkEntry);

                    if (hunkTableSmall.length >= (i * 512 / 4) + (512 / 4))
                        System.arraycopy(hunkSector.hunkEntry, 0, hunkTableSmall, i * 512 / 4, 512 / 4);
                    else
                        System.arraycopy(hunkSector.hunkEntry, 0, hunkTableSmall, i * 512 / 4,
                                hunkTableSmall.length - (i * 512 / 4));
                }

                Instant end = Instant.now();
                Debug.printf("CHD plugin: Took %d seconds", end.minusSeconds(start.getEpochSecond()));
            } else {
                Debug.println("Cannot read compressed CHD version 5");

                return -1;
            }

            nextMetaOff = hdrV5.metaOffset;

            imageInfo.imageSize = hdrV5.logicalBytes;
            imageInfo.version = "5";

            totalHunks = (int) (hdrV5.logicalBytes / hdrV5.hunkBytes);
            bytesPerHunk = hdrV5.hunkBytes;
            hdrCompression = hdrV5.compressor0;
            hdrCompression1 = hdrV5.compressor1;
            hdrCompression2 = hdrV5.compressor2;
            hdrCompression3 = hdrV5.compressor3;
            mapVersion = 5;

            break;
        }

        default:
            Debug.println("Unsupported CHD version " + version);

            return -1;
        }

        if (mapVersion >= 3) {
            isCdrom = false;
            isHdd = false;
            isGdrom = false;
            swapAudio = false;
            tracks = new HashMap<>();

            Debug.printf("CHD plugin: Reading metadata.");

            long currentSector = 0;
            int currentTrack = 1;

            while (nextMetaOff > 0) {
                byte[] hdrBytes = new byte[16];
                stream.seek(nextMetaOff, SeekOrigin.Begin);
                stream.read(hdrBytes, 0, hdrBytes.length);
                Structs.MetadataHeader header = new Structs.MetadataHeader();
                try {
                    Serdes.Util.deserialize(new ByteArrayInputStream(hdrBytes), header);
                } catch (java.io.IOException e) {
                    throw new IOException(e);
                }
                byte[] meta = new byte[header.flagsAndLength & 0xFFFFFF];
                stream.read(meta, 0, meta.length);

                Debug.printf("CHD plugin: Found metadata \"%s\"",
                        new String(ByteUtil.getLeBytes(header.tag)), StandardCharsets.US_ASCII);

                switch (header.tag) {
                // "GDDD"
                case Constants.HARD_DISK_METADATA:
                    if (isCdrom || isGdrom) {
                        Debug.println("Image cannot be a hard disk and a C/GD-ROM at the same time, aborting.");

                        return -1;
                    }

                    String gddd = new String(meta, StandardCharsets.US_ASCII).replace("\u0000+$", "");
                    Pattern gdddRegEx = Pattern.compile(Constants.REGEX_METADATA_HDD);
                    Matcher gdddMatch = gdddRegEx.matcher(gddd);

                    if (gdddMatch.matches()) {
                        isHdd = true;
                        imageInfo.sectorSize = Integer.parseInt(gdddMatch.group(4));
                        imageInfo.cylinders = Integer.parseInt(gdddMatch.group(1));
                        imageInfo.heads = Integer.parseInt(gdddMatch.group(2));
                        imageInfo.sectorsPerTrack = Integer.parseInt(gdddMatch.group(3));
                    }

                    break;

                // "CHCD"
                case Constants.CDROM_OLD_METADATA:
                    if (isHdd) {
                        Debug.println("Image cannot be a hard disk and a CD-ROM at the same time, aborting.");

                        return -1;
                    }

                    if (isGdrom) {
                        Debug.println("Image cannot be a GD-ROM and a CD-ROM at the same time, aborting.");

                        return -1;
                    }

                    int chdTracksNumber = ByteUtil.readBeInt(meta, 0);

                    // Byteswapped
                    if (chdTracksNumber > 99)
                        chdTracksNumber = ByteUtil.readBeInt(meta, 0);

                    currentSector = 0;

                    for (int i = 0; i < chdTracksNumber; i++) {
                        int x = i;
                        Structs.TrackOld chdTrack = new Structs.TrackOld() {{
                            type = ByteUtil.readBeInt(meta, 4 + (x * 24) + 0);
                            subType = ByteUtil.readBeInt(meta, 4 + (x * 24) + 4);
                            dataSize = ByteUtil.readBeInt(meta, 4 + (x * 24) + 8);
                            subSize = ByteUtil.readBeInt(meta, 4 + (x * 24) + 12);
                            frames = ByteUtil.readBeInt(meta, 4 + (x * 24) + 16);
                            extraFrames = ByteUtil.readBeInt(meta, 4 + (x * 24) + 20);
                        }};

                        Track aaruTrack = new Track();

                        switch (Enums.TrackTypeOld.values()[chdTrack.type]) {
                        case Audio:
                            aaruTrack.bytesPerSector = 2352;
                            aaruTrack.rawBytesPerSector = 2352;
                            aaruTrack.type = TrackType.Audio;

                            break;
                        case Mode1:
                            aaruTrack.bytesPerSector = 2048;
                            aaruTrack.rawBytesPerSector = 2048;
                            aaruTrack.type = TrackType.CdMode1;

                            break;
                        case Mode1Raw:
                            aaruTrack.bytesPerSector = 2048;
                            aaruTrack.rawBytesPerSector = 2352;
                            aaruTrack.type = TrackType.CdMode1;

                            break;
                        case Mode2:
                        case Mode2FormMix:
                            aaruTrack.bytesPerSector = 2336;
                            aaruTrack.rawBytesPerSector = 2336;
                            aaruTrack.type = TrackType.CdMode2Formless;

                            break;
                        case Mode2Form1:
                            aaruTrack.bytesPerSector = 2048;
                            aaruTrack.rawBytesPerSector = 2048;
                            aaruTrack.type = TrackType.CdMode2Form1;

                            break;
                        case Mode2Form2:
                            aaruTrack.bytesPerSector = 2324;
                            aaruTrack.rawBytesPerSector = 2324;
                            aaruTrack.type = TrackType.CdMode2Form2;

                            break;
                        case Mode2Raw:
                            aaruTrack.bytesPerSector = 2336;
                            aaruTrack.rawBytesPerSector = 2352;
                            aaruTrack.type = TrackType.CdMode2Formless;

                            break;
                        default: {
                            Debug.println("Unsupported track type " + chdTrack.type);

                            return -1;
                        }
                        }

                        switch (Enums.SubTypeOld.values()[chdTrack.subType]) {
                        case Cooked:
                            aaruTrack.subChannelFile = stream.toString();
                            aaruTrack.subChannelType = TrackSubChannelType.PackedInterleaved;
                            aaruTrack.subChannelFilter = stream;

                            break;
                        case None:
                            aaruTrack.subChannelType = TrackSubChannelType.None;

                            break;
                        case Raw:
                            aaruTrack.subChannelFile = stream.toString();
                            aaruTrack.subChannelType = TrackSubChannelType.RawInterleaved;
                            aaruTrack.subChannelFilter = stream;

                            break;
                        default: {
                            Debug.println("Unsupported subchannel type " + chdTrack.type);

                            return -1;
                        }
                        }

                        aaruTrack.description = "Track {i + 1}";
                        aaruTrack.endSector = currentSector + chdTrack.frames - 1;
                        aaruTrack.file = stream.toString();
                        aaruTrack.fileType = "BINARY";
                        aaruTrack.filter = stream;
                        aaruTrack.startSector = currentSector;
                        aaruTrack.sequence = i + 1;
                        aaruTrack.session = 1;

                        if (aaruTrack.sequence == 1)
                            aaruTrack.indexes.put(0, -150);

                        aaruTrack.indexes.put(1, (int) currentSector);
                        currentSector += chdTrack.frames + chdTrack.extraFrames;
                        tracks.put(aaruTrack.sequence, aaruTrack);
                    }

                    isCdrom = true;

                    break;

                // "CHTR"
                case Constants.CDROM_TRACK_METADATA:
                    if (isHdd) {
                        Debug.println("Image cannot be a hard disk and a CD-ROM at the same time, aborting.");

                        return -1;
                    }

                    if (isGdrom) {
                        Debug.println("Image cannot be a GD-ROM and a CD-ROM at the same time, aborting.");

                        return -1;
                    }

                    String chtr = new String(meta, StandardCharsets.US_ASCII).replace("\u0000*$", "");
                    Pattern chtrRegEx = Pattern.compile(Constants.REGEX_METADATA_CDROM);
                    Matcher chtrMatch = chtrRegEx.matcher(chtr);

                    if (chtrMatch.matches()) {
                        isCdrom = true;

                        int trackNo = Integer.parseInt(chtrMatch.group(1));
                        int frames = Integer.parseInt(chtrMatch.group(4));
                        String subtype = chtrMatch.group(3);
                        String tracktype = chtrMatch.group(2);

                        if (trackNo != currentTrack) {
                            Debug.println("Unsorted tracks, cannot proceed.");

                            return -1;
                        }

                        Track aaruTrack = new Track();

                        switch (tracktype) {
                        case Constants.TRACK_TYPE_AUDIO:
                            aaruTrack.bytesPerSector = 2352;
                            aaruTrack.rawBytesPerSector = 2352;
                            aaruTrack.type = TrackType.Audio;

                            break;
                        case Constants.TRACK_TYPE_MODE1:
                        case Constants.TRACK_TYPE_MODE1_2K:
                            aaruTrack.bytesPerSector = 2048;
                            aaruTrack.rawBytesPerSector = 2048;
                            aaruTrack.type = TrackType.CdMode1;

                            break;
                        case Constants.TRACK_TYPE_MODE1_RAW:
                        case Constants.TRACK_TYPE_MODE1_RAW_2K:
                            aaruTrack.bytesPerSector = 2048;
                            aaruTrack.rawBytesPerSector = 2352;
                            aaruTrack.type = TrackType.CdMode1;

                            break;
                        case Constants.TRACK_TYPE_MODE2:
                        case Constants.TRACK_TYPE_MODE2_2K:
                        case Constants.TRACK_TYPE_MODE2_FM:
                            aaruTrack.bytesPerSector = 2336;
                            aaruTrack.rawBytesPerSector = 2336;
                            aaruTrack.type = TrackType.CdMode2Formless;

                            break;
                        case Constants.TRACK_TYPE_MODE2_F1:
                        case Constants.TRACK_TYPE_MODE2_F1_2K:
                            aaruTrack.bytesPerSector = 2048;
                            aaruTrack.rawBytesPerSector = 2048;
                            aaruTrack.type = TrackType.CdMode2Form1;

                            break;
                        case Constants.TRACK_TYPE_MODE2_F2:
                        case Constants.TRACK_TYPE_MODE2_F2_2K:
                            aaruTrack.bytesPerSector = 2324;
                            aaruTrack.rawBytesPerSector = 2324;
                            aaruTrack.type = TrackType.CdMode2Form2;

                            break;
                        case Constants.TRACK_TYPE_MODE2_RAW:
                        case Constants.TRACK_TYPE_MODE2_RAW_2K:
                            aaruTrack.bytesPerSector = 2336;
                            aaruTrack.rawBytesPerSector = 2352;
                            aaruTrack.type = TrackType.CdMode2Formless;

                            break;
                        default: {
                            Debug.println("Unsupported track type " + tracktype);

                            return -1;
                        }
                        }

                        switch (subtype) {
                        case Constants.SUB_TYPE_COOKED:
                            aaruTrack.subChannelFile = stream.toString();
                            aaruTrack.subChannelType = TrackSubChannelType.PackedInterleaved;
                            aaruTrack.subChannelFilter = stream;

                            break;
                        case Constants.SUB_TYPE_NONE:
                            aaruTrack.subChannelType = TrackSubChannelType.None;

                            break;
                        case Constants.SUB_TYPE_RAW:
                            aaruTrack.subChannelFile = stream.toString();
                            aaruTrack.subChannelType = TrackSubChannelType.RawInterleaved;
                            aaruTrack.subChannelFilter = stream;

                            break;
                        default: {
                            Debug.println("Unsupported subchannel type " + subtype);

                            return -1;
                        }
                        }

                        aaruTrack.description = "Track {trackNo}";
                        aaruTrack.endSector = currentSector + frames - 1;
                        aaruTrack.file = stream.toString();
                        aaruTrack.fileType = "BINARY";
                        aaruTrack.filter = stream;
                        aaruTrack.startSector = currentSector;
                        aaruTrack.sequence = trackNo;
                        aaruTrack.session = 1;

                        if (aaruTrack.sequence == 1)
                            aaruTrack.indexes.put(0, -150);

                        aaruTrack.indexes.put(1, (int) currentSector);
                        currentSector += frames;
                        currentTrack++;
                        tracks.put(aaruTrack.sequence, aaruTrack);
                    }

                    break;

                // "CHT2"
                case Constants.CDROM_TRACK_METADATA2:
                    if (isHdd) {
                        Debug.println("Image cannot be a hard disk and a CD-ROM at the same time, aborting.");

                        return -1;
                    }

                    if (isGdrom) {
                        Debug.println("Image cannot be a GD-ROM and a CD-ROM at the same time, aborting.");

                        return -1;
                    }

                    String cht2 = new String(meta, StandardCharsets.US_ASCII).replace("\u0000*$", "");
                    Pattern cht2RegEx = Pattern.compile(Constants.REGEX_METADATA_CDROM2);
                    Matcher cht2Match = cht2RegEx.matcher(cht2);

                    if (cht2Match.matches()) {
                        isCdrom = true;

                        int trackNo = Integer.parseInt(cht2Match.group(1));
                        int frames = Integer.parseInt(cht2Match.group(4));
                        String subtype = cht2Match.group(3);
                        String trackType = cht2Match.group(2);

                        int preGap = Integer.parseInt(cht2Match.group(5));

                        // What is this, really? Same as track type?
                        String preGapType = cht2Match.group(6);

                        // Read above, but for subchannel
                        String preGapSubType = cht2Match.group(7);

                        // This is a recommendation (shall) of 150 sectors at the end of the last data track,
                        // or of any data track followed by an audio track, according to Yellow Book.
                        // It is indistinguishable from normal data.
                        // TODO: Does CHD store it, or like CDRWin, ignores it?
                        int postGap = Integer.parseInt(cht2Match.group(8));

                        if (trackNo != currentTrack) {
                            Debug.println("Unsorted tracks, cannot proceed.");

                            return -1;
                        }

                        Track aaruTrack = new Track();

                        switch (trackType) {
                        case Constants.TRACK_TYPE_AUDIO:
                            aaruTrack.bytesPerSector = 2352;
                            aaruTrack.rawBytesPerSector = 2352;
                            aaruTrack.type = TrackType.Audio;

                            break;
                        case Constants.TRACK_TYPE_MODE1:
                        case Constants.TRACK_TYPE_MODE1_2K:
                            aaruTrack.bytesPerSector = 2048;
                            aaruTrack.rawBytesPerSector = 2048;
                            aaruTrack.type = TrackType.CdMode1;

                            break;
                        case Constants.TRACK_TYPE_MODE1_RAW:
                        case Constants.TRACK_TYPE_MODE1_RAW_2K:
                            aaruTrack.bytesPerSector = 2048;
                            aaruTrack.rawBytesPerSector = 2352;
                            aaruTrack.type = TrackType.CdMode1;

                            break;
                        case Constants.TRACK_TYPE_MODE2:
                        case Constants.TRACK_TYPE_MODE2_2K:
                        case Constants.TRACK_TYPE_MODE2_FM:
                            aaruTrack.bytesPerSector = 2336;
                            aaruTrack.rawBytesPerSector = 2336;
                            aaruTrack.type = TrackType.CdMode2Formless;

                            break;
                        case Constants.TRACK_TYPE_MODE2_F1:
                        case Constants.TRACK_TYPE_MODE2_F1_2K:
                            aaruTrack.bytesPerSector = 2048;
                            aaruTrack.rawBytesPerSector = 2048;
                            aaruTrack.type = TrackType.CdMode2Form1;

                            break;
                        case Constants.TRACK_TYPE_MODE2_F2:
                        case Constants.TRACK_TYPE_MODE2_F2_2K:
                            aaruTrack.bytesPerSector = 2324;
                            aaruTrack.rawBytesPerSector = 2324;
                            aaruTrack.type = TrackType.CdMode2Form2;

                            break;
                        case Constants.TRACK_TYPE_MODE2_RAW:
                        case Constants.TRACK_TYPE_MODE2_RAW_2K:
                            aaruTrack.bytesPerSector = 2336;
                            aaruTrack.rawBytesPerSector = 2352;
                            aaruTrack.type = TrackType.CdMode2Formless;

                            break;
                        default: {
                            Debug.println("Unsupported track type " + trackType);

                            return -1;
                        }
                        }

                        switch (subtype) {
                        case Constants.SUB_TYPE_COOKED:
                            aaruTrack.subChannelFile = stream.toString();
                            aaruTrack.subChannelType = TrackSubChannelType.PackedInterleaved;
                            aaruTrack.subChannelFilter = stream;

                            break;
                        case Constants.SUB_TYPE_NONE:
                            aaruTrack.subChannelType = TrackSubChannelType.None;

                            break;
                        case Constants.SUB_TYPE_RAW:
                            aaruTrack.subChannelFile = stream.toString();
                            aaruTrack.subChannelType = TrackSubChannelType.RawInterleaved;
                            aaruTrack.subChannelFilter = stream;

                            break;
                        default: {
                            Debug.println("Unsupported subchannel type " + subtype);

                            return -1;
                        }
                        }

                        aaruTrack.description = "Track " + trackNo;
                        aaruTrack.endSector = currentSector + frames - 1;
                        aaruTrack.file = stream.toString();
                        aaruTrack.fileType = "BINARY";
                        aaruTrack.filter = stream;
                        aaruTrack.startSector = currentSector;
                        aaruTrack.sequence = trackNo;
                        aaruTrack.session = 1;

                        if (aaruTrack.sequence == 1) {
                            if (preGap <= 150) {
                                aaruTrack.indexes.put(0, -150);
                                aaruTrack.preGap = 150;
                            } else {
                                aaruTrack.indexes.put(0, -1 * preGap);
                                aaruTrack.preGap = preGap;
                            }

                            aaruTrack.indexes.put(1, (int) currentSector);
                        } else if (preGap > 0) {
                            aaruTrack.indexes.put(0, (int) currentSector);
                            aaruTrack.preGap = preGap;
                            aaruTrack.indexes.put(1, (int) (currentSector + preGap));
                        } else
                            aaruTrack.indexes.put(1, (int) currentSector);

                        currentSector += frames;
                        currentTrack++;
                        tracks.put(aaruTrack.sequence, aaruTrack);
                    }

                    break;

                // "CHGT"
                case Constants.GDROM_OLD_METADATA:
                    swapAudio = true;
                    // break case GDROM_METADATA;

                    // "CHGD"
                case Constants.GDROM_METADATA:
                    if (isHdd) {
                        Debug.println("Image cannot be a hard disk and a GD-ROM at the same time, aborting.");

                        return -1;
                    }

                    if (isCdrom) {
                        Debug.println("Image cannot be a CD-ROM and a GD-ROM at the same time, aborting.");

                        return -1;
                    }

                    String chgd = new String(meta, StandardCharsets.US_ASCII).replace("\u0000*$", "");
                    Pattern chgdRegEx = Pattern.compile(Constants.REGEX_METADATA_GDROM);
                    Matcher chgdMatch = chgdRegEx.matcher(chgd);

                    if (chgdMatch.matches()) {
                        isGdrom = true;

                        int trackNo = Integer.parseInt(chgdMatch.group(1));
                        int frames = Integer.parseInt(chgdMatch.group(4));
                        String subtype = chgdMatch.group(3);
                        String trackType = chgdMatch.group(2);

                        // TODO: Check pregap, postgap and pad behaviour
                        int pregap = Integer.parseInt(chgdMatch.group(6));
                        String pregapType = chgdMatch.group(7);
                        String pregapSubType = chgdMatch.group(8);
                        int postgap = Integer.parseInt(chgdMatch.group(9));
                        int pad = Integer.parseInt(chgdMatch.group(5));

                        if (trackNo != currentTrack) {
                            Debug.println("Unsorted tracks, cannot proceed.");

                            return -1;
                        }

                        Track aaruTrack = new Track();

                        switch (trackType) {
                        case Constants.TRACK_TYPE_AUDIO:
                            aaruTrack.bytesPerSector = 2352;
                            aaruTrack.rawBytesPerSector = 2352;
                            aaruTrack.type = TrackType.Audio;

                            break;
                        case Constants.TRACK_TYPE_MODE1:
                        case Constants.TRACK_TYPE_MODE1_2K:
                            aaruTrack.bytesPerSector = 2048;
                            aaruTrack.rawBytesPerSector = 2048;
                            aaruTrack.type = TrackType.CdMode1;

                            break;
                        case Constants.TRACK_TYPE_MODE1_RAW:
                        case Constants.TRACK_TYPE_MODE1_RAW_2K:
                            aaruTrack.bytesPerSector = 2048;
                            aaruTrack.rawBytesPerSector = 2352;
                            aaruTrack.type = TrackType.CdMode1;

                            break;
                        case Constants.TRACK_TYPE_MODE2:
                        case Constants.TRACK_TYPE_MODE2_2K:
                        case Constants.TRACK_TYPE_MODE2_FM:
                            aaruTrack.bytesPerSector = 2336;
                            aaruTrack.rawBytesPerSector = 2336;
                            aaruTrack.type = TrackType.CdMode2Formless;

                            break;
                        case Constants.TRACK_TYPE_MODE2_F1:
                        case Constants.TRACK_TYPE_MODE2_F1_2K:
                            aaruTrack.bytesPerSector = 2048;
                            aaruTrack.rawBytesPerSector = 2048;
                            aaruTrack.type = TrackType.CdMode2Form1;

                            break;
                        case Constants.TRACK_TYPE_MODE2_F2:
                        case Constants.TRACK_TYPE_MODE2_F2_2K:
                            aaruTrack.bytesPerSector = 2324;
                            aaruTrack.rawBytesPerSector = 2324;
                            aaruTrack.type = TrackType.CdMode2Form2;

                            break;
                        case Constants.TRACK_TYPE_MODE2_RAW:
                        case Constants.TRACK_TYPE_MODE2_RAW_2K:
                            aaruTrack.bytesPerSector = 2336;
                            aaruTrack.rawBytesPerSector = 2352;
                            aaruTrack.type = TrackType.CdMode2Formless;

                            break;
                        default: {
                            Debug.println("Unsupported track type " + trackType);

                            return -1;
                        }
                        }

                        switch (subtype) {
                        case Constants.SUB_TYPE_COOKED:
                            aaruTrack.subChannelFile = stream.toString();
                            aaruTrack.subChannelType = TrackSubChannelType.PackedInterleaved;
                            aaruTrack.subChannelFilter = stream;

                            break;
                        case Constants.SUB_TYPE_NONE:
                            aaruTrack.subChannelType = TrackSubChannelType.None;

                            break;
                        case Constants.SUB_TYPE_RAW:
                            aaruTrack.subChannelFile = stream.toString();
                            aaruTrack.subChannelType = TrackSubChannelType.RawInterleaved;
                            aaruTrack.subChannelFilter = stream;

                            break;
                        default: {
                            Debug.println("Unsupported subchannel type " + subtype);

                            return -1;
                        }
                        }

                        aaruTrack.description = "Track {trackNo}";
                        aaruTrack.endSector = currentSector + frames - 1;
                        aaruTrack.file = stream.toString();
                        aaruTrack.fileType = "BINARY";
                        aaruTrack.filter = stream;
                        aaruTrack.startSector = currentSector;
                        aaruTrack.sequence = trackNo;
                        aaruTrack.session = (short) (trackNo > 2 ? 2 : 1);

                        if (aaruTrack.sequence == 1) {
                            if (pregap <= 150) {
                                aaruTrack.indexes.put(0, -150);
                                aaruTrack.preGap = 150;
                            } else {
                                aaruTrack.indexes.put(0, -1 * pregap);
                                aaruTrack.preGap = pregap;
                            }

                            aaruTrack.indexes.put(1, (int) currentSector);
                        } else if (pregap > 0) {
                            aaruTrack.indexes.put(0, (int) currentSector);
                            aaruTrack.preGap = pregap;
                            aaruTrack.indexes.put(1, (int) (currentSector + pregap));
                        } else
                            aaruTrack.indexes.put(1, (int) currentSector);

                        currentSector += frames;
                        currentTrack++;
                        tracks.put(aaruTrack.sequence, aaruTrack);
                    }

                    break;

                // "IDNT"
                case Constants.HARD_DISK_IDENT_METADATA:
                    Optional<Identify.IdentifyDevice> idnt = Identify.decode(meta);

                    if (idnt.isPresent()) {
                        imageInfo.mediaManufacturer = idnt.get().mediaManufacturer;
                        imageInfo.mediaSerialNumber = idnt.get().mediaSerial;
                        imageInfo.driveModel = idnt.get().model;
                        imageInfo.driveSerialNumber = idnt.get().serialNumber;
                        imageInfo.driveFirmwareRevision = idnt.get().firmwareRevision;

                        if (idnt.get().currentCylinders > 0 &&
                                idnt.get().currentHeads > 0 &&
                                idnt.get().currentSectorsPerTrack > 0) {
                            imageInfo.cylinders = idnt.get().currentCylinders;
                            imageInfo.heads = idnt.get().currentHeads;
                            imageInfo.sectorsPerTrack = idnt.get().currentSectorsPerTrack;
                        } else {
                            imageInfo.cylinders = idnt.get().cylinders;
                            imageInfo.heads = idnt.get().heads;
                            imageInfo.sectorsPerTrack = idnt.get().sectorsPerTrack;
                        }
                    }

                    identify = meta;

                    if (!imageInfo.readableMediaTags.contains(MediaTagType.ATA_IDENTIFY))
                        imageInfo.readableMediaTags.add(MediaTagType.ATA_IDENTIFY);

                    break;
                case Constants.PCMCIA_CIS_METADATA:
                    cis = meta;

                    if (!imageInfo.readableMediaTags.contains(MediaTagType.PCMCIA_CIS))
                        imageInfo.readableMediaTags.add(MediaTagType.PCMCIA_CIS);

                    break;
                }

                nextMetaOff = header.next;
            }

            if (isHdd) {
                sectorsPerHunk = bytesPerHunk / imageInfo.sectorSize;
                imageInfo.sectors = imageInfo.imageSize / imageInfo.sectorSize;
                imageInfo.mediaType = "GENERIC_HDD";
                imageInfo.xmlMediaType = XmlMediaType.BlockMedia;
            } else if (isCdrom) {
                // Hardcoded on MAME for CD-ROM
                sectorsPerHunk = 8;
                imageInfo.mediaType = "CDROM";
                imageInfo.xmlMediaType = XmlMediaType.OpticalDisc;

                for (Track aaruTrack : tracks.values())
                    imageInfo.sectors += aaruTrack.endSector - aaruTrack.startSector + 1;
            } else if (isGdrom) {
                // Hardcoded on MAME for GD-ROM
                sectorsPerHunk = 8;
                imageInfo.mediaType = "GDROM";
                imageInfo.xmlMediaType = XmlMediaType.OpticalDisc;

                for (Track track : tracks.values())
                    imageInfo.sectors += track.endSector - track.startSector + 1;
            } else {
                Debug.println("Image does not represent a known media, aborting");

                return -1;
            }
        }

        if (isCdrom || isGdrom) {
            offsetMap = new HashMap<>();
            partitions = new ArrayList<>();
            long partPos = 0;

            for (Track track : tracks.values()) {
                long pos = partPos;
                Partition partition = new Partition() {{
                    description = track.description;
                    size = (track.endSector - (long) track.indexes.get(1) + 1) *
                            (long) track.rawBytesPerSector;
                    length = track.endSector - (long) track.indexes.get(1) + 1;
                    sequence = track.sequence;
                    offset = pos;
                    start = (long) track.indexes.get(1);
                    type = track.type.name();
                }};

                partPos += partition.length;
                offsetMap.put(track.startSector, track.sequence);

                if (track.subChannelType != TrackSubChannelType.None)
                    if (!imageInfo.readableSectorTags.contains(SectorTagType.CdSectorSubchannel))
                        imageInfo.readableSectorTags.add(SectorTagType.CdSectorSubchannel);

                switch (track.type) {
                case CdMode1:
                case CdMode2Form1:
                    if (track.rawBytesPerSector == 2352) {
                        if (!imageInfo.readableSectorTags.contains(SectorTagType.CdSectorSync))
                            imageInfo.readableSectorTags.add(SectorTagType.CdSectorSync);

                        if (!imageInfo.readableSectorTags.contains(SectorTagType.CdSectorHeader))
                            imageInfo.readableSectorTags.add(SectorTagType.CdSectorHeader);

                        if (!imageInfo.readableSectorTags.contains(SectorTagType.CdSectorSubHeader))
                            imageInfo.readableSectorTags.add(SectorTagType.CdSectorSubHeader);

                        if (!imageInfo.readableSectorTags.contains(SectorTagType.CdSectorEcc))
                            imageInfo.readableSectorTags.add(SectorTagType.CdSectorEcc);

                        if (!imageInfo.readableSectorTags.contains(SectorTagType.CdSectorEccP))
                            imageInfo.readableSectorTags.add(SectorTagType.CdSectorEccP);

                        if (!imageInfo.readableSectorTags.contains(SectorTagType.CdSectorEccQ))
                            imageInfo.readableSectorTags.add(SectorTagType.CdSectorEccQ);

                        if (!imageInfo.readableSectorTags.contains(SectorTagType.CdSectorEdc))
                            imageInfo.readableSectorTags.add(SectorTagType.CdSectorEdc);
                    }

                    break;
                case CdMode2Form2:
                    if (track.rawBytesPerSector == 2352) {
                        if (!imageInfo.readableSectorTags.contains(SectorTagType.CdSectorSync))
                            imageInfo.readableSectorTags.add(SectorTagType.CdSectorSync);

                        if (!imageInfo.readableSectorTags.contains(SectorTagType.CdSectorHeader))
                            imageInfo.readableSectorTags.add(SectorTagType.CdSectorHeader);

                        if (!imageInfo.readableSectorTags.contains(SectorTagType.CdSectorSubHeader))
                            imageInfo.readableSectorTags.add(SectorTagType.CdSectorSubHeader);

                        if (!imageInfo.readableSectorTags.contains(SectorTagType.CdSectorEdc))
                            imageInfo.readableSectorTags.add(SectorTagType.CdSectorEdc);
                    }

                    break;
                case CdMode2Formless:
                    if (track.rawBytesPerSector == 2352) {
                        if (!imageInfo.readableSectorTags.contains(SectorTagType.CdSectorSync))
                            imageInfo.readableSectorTags.add(SectorTagType.CdSectorSync);

                        if (!imageInfo.readableSectorTags.contains(SectorTagType.CdSectorHeader))
                            imageInfo.readableSectorTags.add(SectorTagType.CdSectorHeader);
                    }

                    break;
                }

                if (track.bytesPerSector > imageInfo.sectorSize)
                    imageInfo.sectorSize = track.bytesPerSector;

                partitions.add(partition);
            }

            imageInfo.hasPartitions = true;
            imageInfo.hasSessions = true;
        }

        maxBlockCache = Constants.MAX_CACHE_SIZE / (imageInfo.sectorSize * sectorsPerHunk);
        maxSectorCache = Constants.MAX_CACHE_SIZE / imageInfo.sectorSize;

        imageStream = stream;

        sectorCache = new HashMap<>();
        hunkCache = new HashMap<>();

        // TODO: Detect CompactFlash
        // TODO: Get manufacturer and drive name from CIS if applicable
        if (cis != null)
            imageInfo.mediaType = "PCCardTypeI";

        sectorBuilder = new SectorBuilder();

        return 0;
    }

    /**  */
    public int readSector(long sectorAddress, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        if (sectorAddress > imageInfo.sectors - 1)
            throw new IndexOutOfBoundsException();

        Track track = new Track();
        int sectorSize;

        byte[] sector = sectorCache.getOrDefault(sectorAddress, null);
        if (sector == null) {
            if (isHdd)
                sectorSize = imageInfo.sectorSize;
            else {
                track = getTrack(sectorAddress);
                sectorSize = track.rawBytesPerSector;
            }

            long hunkNo = sectorAddress / sectorsPerHunk;
            long secOff = sectorAddress * sectorSize % ((long) sectorsPerHunk * sectorSize);

            byte[][] hunk = new byte[1][];
            int errno = getHunk(hunkNo, /*out byte[]*/ hunk);
            if (errno != 0)
                return errno;

            sector = new byte[imageInfo.sectorSize];
            System.arraycopy(hunk[0], (int) secOff, sector, 0, sector.length);

            if (sectorCache.size() >= maxSectorCache)
                sectorCache.clear();

            sectorCache.put(sectorAddress, sector);
        }

        if (isHdd) {
            buffer[0] = sector;

            return 0;
        }

        int sectorOffset;
        boolean mode2 = false;

        switch (track.type) {
        case CdMode1: {
            if (track.rawBytesPerSector == 2352) {
                sectorOffset = 16;
                sectorSize = 2048;
            } else {
                sectorOffset = 0;
                sectorSize = 2048;
            }

            break;
        }
        case CdMode2Form1: {
            if (track.rawBytesPerSector == 2352) {
                sectorOffset = 0;
                sectorSize = 2352;
                mode2 = true;
            } else {
                sectorOffset = 0;
                sectorSize = 2048;
            }

            break;
        }

        case CdMode2Form2: {
            if (track.rawBytesPerSector == 2352) {
                sectorOffset = 0;
                sectorSize = 2352;
                mode2 = true;
            } else {
                sectorOffset = 0;
                sectorSize = 2324;
            }

            break;
        }

        case CdMode2Formless: {
            sectorOffset = 0;
            sectorSize = track.rawBytesPerSector;
            mode2 = true;

            break;
        }

        case Audio: {
            sectorOffset = 0;
            sectorSize = 2352;

            break;
        }

        default:
            return -1;
        }

        buffer[0] = new byte[sectorSize];

        if (mode2)
            buffer[0] = Sector.GetUserDataFromMode2(sector, false, (byte) 0);
        else if (track.type == TrackType.Audio && swapAudio)
            for (int i = 0; i < 2352; i += 2) {
                buffer[0][i + 1] = sector[i];
                buffer[0][i] = sector[i + 1];
            }
        else
            System.arraycopy(sector, sectorOffset, buffer[0], 0, sectorSize);

        return 0;
    }

    /**  */
    public int readSectorTag(long sectorAddress, SectorTagType tag, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        if (isHdd)
            return -1;

        if (sectorAddress > imageInfo.sectors - 1)
            throw new IndexOutOfBoundsException();

        Track track = new Track();

        int sectorSize;

        byte[] sector = sectorCache.getOrDefault(sectorAddress, null);
        if (sector == null) {
            track = getTrack(sectorAddress);
            sectorSize = track.rawBytesPerSector;

            long hunkNo = sectorAddress / sectorsPerHunk;
            long secOff = sectorAddress * sectorSize % ((long) sectorsPerHunk * sectorSize);

            byte[][] hunk = new byte[1][];
            int errno = getHunk(hunkNo, /*out byte[]*/ hunk);

            if (errno != 0)
                return errno;

            sector = new byte[imageInfo.sectorSize];
            System.arraycopy(hunk[0], (int) secOff, sector, 0, sector.length);

            if (sectorCache.size() >= maxSectorCache)
                sectorCache.clear();

            sectorCache.put(sectorAddress, sector);
        }

        if (isHdd) {
            buffer[0] = sector;

            return 0;
        }

        int sectorOffset;

        if (tag == SectorTagType.CdSectorSubchannel)
            switch (track.subChannelType) {
            case None:
                throw new NoSuchElementException();
            case RawInterleaved:
                sectorOffset = track.rawBytesPerSector;
                sectorSize = 96;

                break;
            default:
                return -1;
            }
        else
            switch (track.type) {
            case CdMode1:
            case CdMode2Form1: {
                if (track.rawBytesPerSector == 2352)
                    switch (tag) {
                    case CdSectorSync: {
                        sectorOffset = 0;
                        sectorSize = 12;

                        break;
                    }

                    case CdSectorHeader: {
                        sectorOffset = 12;
                        sectorSize = 4;

                        break;
                    }

                    case CdSectorSubHeader:
                        return -1;
                    case CdSectorEcc: {
                        sectorOffset = 2076;
                        sectorSize = 276;

                        break;
                    }

                    case CdSectorEccP: {
                        sectorOffset = 2076;
                        sectorSize = 172;

                        break;
                    }

                    case CdSectorEccQ: {
                        sectorOffset = 2248;
                        sectorSize = 104;

                        break;
                    }

                    case CdSectorEdc: {
                        sectorOffset = 2064;
                        sectorSize = 4;

                        break;
                    }

                    default:
                        return -1;
                    }
                else
                    throw new NoSuchElementException();

                break;
            }

            case CdMode2Form2: {
                if (track.rawBytesPerSector == 2352)
                    switch (tag) {
                    case CdSectorSync: {
                        sectorOffset = 0;
                        sectorSize = 12;

                        break;
                    }

                    case CdSectorHeader: {
                        sectorOffset = 12;
                        sectorSize = 4;

                        break;
                    }

                    case CdSectorSubHeader: {
                        sectorOffset = 16;
                        sectorSize = 8;

                        break;
                    }

                    case CdSectorEdc: {
                        sectorOffset = 2348;
                        sectorSize = 4;

                        break;
                    }

                    default:
                        return -1;
                    }
                else
                    switch (tag) {
                    case CdSectorSync:
                    case CdSectorHeader:
                    case CdSectorSubchannel:
                    case CdSectorEcc:
                    case CdSectorEccP:
                    case CdSectorEccQ:
                        return -1;
                    case CdSectorSubHeader: {
                        sectorOffset = 0;
                        sectorSize = 8;

                        break;
                    }

                    case CdSectorEdc: {
                        sectorOffset = 2332;
                        sectorSize = 4;

                        break;
                    }

                    default:
                        return -1;
                    }

                break;
            }

            case CdMode2Formless: {
                if (track.rawBytesPerSector == 2352)
                    switch (tag) {
                    case CdSectorSync:
                    case CdSectorHeader:
                    case CdSectorEcc:
                    case CdSectorEccP:
                    case CdSectorEccQ:
                        return -1;
                    case CdSectorSubHeader: {
                        sectorOffset = 0;
                        sectorSize = 8;

                        break;
                    }

                    case CdSectorEdc: {
                        sectorOffset = 2332;
                        sectorSize = 4;

                        break;
                    }

                    default:
                        return -1;
                    }
                else
                    throw new NoSuchElementException();

                break;
            }

            case Audio:
                throw new NoSuchElementException();
            default:
                throw new UnsupportedOperationException();
            }

        buffer[0] = new byte[sectorSize];

        if (track.type == TrackType.Audio && swapAudio)
            for (int i = 0; i < 2352; i += 2) {
                buffer[0][i + 1] = sector[i];
                buffer[0][i] = sector[i + 1];
            }
        else
            System.arraycopy(sector, sectorOffset, buffer[0], 0, sectorSize);

        if (track.type == TrackType.Audio && swapAudio)
            for (int i = 0; i < 2352; i += 2) {
                buffer[0][i + 1] = sector[i];
                buffer[0][i] = sector[i + 1];
            }
        else
            System.arraycopy(sector, sectorOffset, buffer[0], 0, sectorSize);

        return 0;
    }

    /**  */
    public int readSectors(long sectorAddress, int length, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        if (sectorAddress > imageInfo.sectors - 1)
            throw new IndexOutOfBoundsException();

        if (sectorAddress + length > imageInfo.sectors)
            throw new IndexOutOfBoundsException();

        MemoryStream ms = new MemoryStream();

        for (int i = 0; i < length; i++) {
            byte[][] sector = new byte[1][];
            int errno = readSector(sectorAddress + i, /*out byte[]*/ sector);

            if (errno != 0)
                return errno;

            ms.write(sector[0], 0, sector[0].length);
        }

        buffer[0] = ms.toArray();

        return 0;
    }

    /**  */
    public int readSectorsTag(long sectorAddress, int length, SectorTagType tag, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        if (sectorAddress > imageInfo.sectors - 1)
            throw new IndexOutOfBoundsException();

        if (sectorAddress + length > imageInfo.sectors)
            throw new IndexOutOfBoundsException();

        MemoryStream ms = new MemoryStream();

        for (int i = 0; i < length; i++) {
            byte[][] sector = new byte[1][];
            int errno = readSectorTag(sectorAddress + i, tag, /*out byte[]*/ sector);

            if (errno != 0)
                return errno;

            ms.write(sector[0], 0, sector[0].length);
        }

        buffer[0] = ms.toArray();

        return 0;
    }

    /**  */
    public int readSectorLong(long sectorAddress, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        if (isHdd)
            return readSector(sectorAddress, /*out*/ buffer);

        if (sectorAddress > imageInfo.sectors - 1)
            throw new IndexOutOfBoundsException();

        Track track = new Track();

        byte[] sector = sectorCache.getOrDefault(sectorAddress, null);
        if (sector != null) {
            track = getTrack(sectorAddress);
            int sectorSize = track.rawBytesPerSector;

            long hunkNo = sectorAddress / sectorsPerHunk;
            long secOff = sectorAddress * sectorSize % ((long) sectorsPerHunk * sectorSize);

            byte[][] hunk = new byte[1][];
            int errno = getHunk(hunkNo, /*out byte[]*/ hunk);
            if (errno != 0)
                return errno;

            sector = new byte[imageInfo.sectorSize];
            System.arraycopy(hunk[0], (int) secOff, sector, 0, sector.length);

            if (sectorCache.size() >= maxSectorCache)
                sectorCache.clear();

            sectorCache.put(sectorAddress, sector);
        }

        buffer[0] = new byte[track.rawBytesPerSector];

        if (track.type == TrackType.Audio && swapAudio)
            for (int i = 0; i < 2352; i += 2) {
                buffer[0][i + 1] = sector[i];
                buffer[0][i] = sector[i + 1];
            }
        else
            System.arraycopy(sector, 0, buffer[0], 0, track.rawBytesPerSector);

        switch (track.type) {
        case CdMode1:
            if (track.rawBytesPerSector == 2048) {
                byte[] fullSector = new byte[2352];

                System.arraycopy(buffer[0], 0, fullSector, 16, 2048);
                sectorBuilder.reconstructPrefix(/*ref*/ fullSector, TrackType.CdMode1, sectorAddress);
                sectorBuilder.reconstructEcc(/*ref*/ fullSector, TrackType.CdMode1);

                buffer[0] = fullSector;
            }
            break;
        case CdMode2Form1:
            if (track.rawBytesPerSector == 2048) {
                byte[] fullSector = new byte[2352];

                System.arraycopy(buffer[0], 0, fullSector, 24, 2048);
                sectorBuilder.reconstructPrefix(/*ref*/ fullSector, TrackType.CdMode2Form1, sectorAddress);
                sectorBuilder.reconstructEcc(/*ref*/ fullSector, TrackType.CdMode2Form1);

                buffer[0] = fullSector;
            }
            break;
        case CdMode2Form2:
            if (track.rawBytesPerSector == 2324) {
                byte[] fullSector = new byte[2352];

                System.arraycopy(buffer[0], 0, fullSector, 24, 2324);
                sectorBuilder.reconstructPrefix(/*ref*/ fullSector, TrackType.CdMode2Form2, sectorAddress);
                sectorBuilder.reconstructEcc(/*ref*/ fullSector, TrackType.CdMode2Form2);

                buffer[0] = fullSector;
            }
            break;
        case CdMode2Formless:
            if (track.rawBytesPerSector == 2336) {
                byte[] fullSector = new byte[2352];

                sectorBuilder.reconstructPrefix(/*ref*/ fullSector, TrackType.CdMode2Formless, sectorAddress);
                System.arraycopy(buffer[0], 0, fullSector, 16, 2336);

                buffer[0] = fullSector;
            }
            break;
        }

        return 0;
    }

    /**  */
    public int readSectorsLong(long sectorAddress, int length, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        if (sectorAddress > imageInfo.sectors - 1)
            throw new IndexOutOfBoundsException();

        if (sectorAddress + length > imageInfo.sectors)
            throw new IndexOutOfBoundsException();

        MemoryStream ms = new MemoryStream();

        for (int i = 0; i < length; i++) {
            byte[][] sector = new byte[1][];
            int errno = readSectorLong(sectorAddress + i, /*out byte[]*/ sector);

            if (errno != 0)
                return errno;

            ms.write(sector[0], 0, sector.length);
        }

        buffer[0] = ms.toArray();

        return 0;
    }

    /**  */
    public int readMediaTag(MediaTagType tag, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        switch (tag) {
        case ATA_IDENTIFY:
            if (imageInfo.readableMediaTags.contains(MediaTagType.ATA_IDENTIFY))
                buffer[0] = identify != null ? identify.clone() : null;

            if (buffer[0] == null) throw new NoSuchElementException();
            return 0;
        case PCMCIA_CIS:
            if (imageInfo.readableMediaTags.contains(MediaTagType.PCMCIA_CIS))
                buffer[0] = cis != null ? cis.clone() : null;

            if (buffer[0] == null) throw new NoSuchElementException();
            return 0;
        default:
            return -1;
        }
    }

    /**  */
    public List<Track> getSessionTracks(Session session) {
        return isHdd ? null : getSessionTracks(null/*session.sequence*/);
    }

    /**  */
    public List<Track> getSessionTracks(short session) {
        return isHdd ? null : tracks.values().stream().filter(track -> track.session == session).collect(Collectors.toList());
    }

    /**  */
    public int readSector(long sectorAddress, int track, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        return isHdd ? -1 : readSector(getAbsoluteSector(sectorAddress, track), /*out*/ buffer);
    }

    /**  */
    public int readSectorTag(long sectorAddress, int track, SectorTagType tag, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        return isHdd ? -1
                : readSectorTag(getAbsoluteSector(sectorAddress, track), tag, /*out*/ buffer);
    }

    /**  */
    public int readSectors(long sectorAddress, int length, int track, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        return isHdd ? -1
                : readSectors(getAbsoluteSector(sectorAddress, track), length, /*out*/ buffer);
    }

    /**  */
    public int readSectorsTag(long sectorAddress, int length, int track, SectorTagType tag,
            /*out*/ byte[][] buffer) {
        buffer[0] = null;

        return isHdd ? -1
                : readSectorsTag(getAbsoluteSector(sectorAddress, track), length, tag, /*out*/ buffer);
    }

    /**  */
    public int readSectorLong(long sectorAddress, int track, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        return isHdd ? -1
                : readSectorLong(getAbsoluteSector(sectorAddress, track), /*out*/ buffer);
    }

    /**  */
    public int readSectorsLong(long sectorAddress, int length, int track, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        return isHdd ? -1
                : readSectorLong(getAbsoluteSector(sectorAddress, track), length, /*out*/ buffer);
    }

//#endregion

//#region Helper

    Track getTrack(long sector) {
        Track track = new Track();

        for (Map.Entry<Long, Integer> kvp : offsetMap.entrySet().stream().filter(kvp -> sector >= kvp.getKey()).collect(Collectors.toSet()))
            track = tracks.getOrDefault(kvp.getValue(), null);

        return track;
    }

    long getAbsoluteSector(long relativeSector, int track) {
        Track aaruTrack = tracks.getOrDefault(track, null);

        return (aaruTrack != null ? aaruTrack.startSector : 0) + relativeSector;
    }

    int getHunk(long hunkNo, /*out*/ byte[][] buffer) {
        if ((buffer[0] = hunkCache.getOrDefault(hunkNo, null)) != null)
            return 0;

        switch (mapVersion) {
        case 1:
            long offset = hunkTable[(int) hunkNo] & 0x00000FFFFFFFFFFFL;
            long length = hunkTable[(int) hunkNo] >> 44;

            byte[] compHunk = new byte[(int) length];
            imageStream.seek(offset, SeekOrigin.Begin);
            imageStream.read(compHunk, 0, compHunk.length);

            if (length == (long) sectorsPerHunk * imageInfo.sectorSize)
                buffer[0] = compHunk;
            else if (Enums.Compression.values()[hdrCompression].ordinal() > Enums.Compression.Zlib.ordinal()) {
                Debug.printf("Unsupported compression %s", hdrCompression);

                throw new IllegalArgumentException();
            } else {
                try (DeflateStream zStream = new DeflateStream(new MemoryStream(compHunk), CompressionMode.Decompress)) {
                    buffer[0] = new byte[sectorsPerHunk * imageInfo.sectorSize];
                    int read = zStream.read(buffer[0], 0, sectorsPerHunk * imageInfo.sectorSize);

                    if (read != sectorsPerHunk * imageInfo.sectorSize) {
                        Debug.printf("Unable to decompress hunk correctly, got %d bytes, expected %d", read, sectorsPerHunk * imageInfo.sectorSize);

                        throw new IOException();
                    }
                } catch (java.io.IOException e) {
                    throw new IOException(e);
                }
            }

            break;
        case 3:
            byte[] entryBytes = new byte[16];
            System.arraycopy(hunkMap, (int) (hunkNo * 16), entryBytes, 0, 16);
            Structs.MapEntryV3 entry = new Structs.MapEntryV3();
            try {
                Serdes.Util.deserialize(new ByteArrayInputStream(entryBytes), entry);
            } catch (java.io.IOException e) {
                throw new IOException(e);
            }

            switch (Enums.EntryFlagsV3.values()[entry.flags & 0x0F]) {
            case Invalid:
                Debug.println("Invalid hunk found.");

                throw new IllegalArgumentException();
            case Compressed:
                switch (Enums.Compression.values()[hdrCompression]) {
                case None:
                    buffer[0] = new byte[bytesPerHunk];
                    imageStream.seek(entry.offset, SeekOrigin.Begin);
                    imageStream.read(buffer[0], 0, buffer.length);
                    break; // uncompressedV3;
                case Zlib:
                case ZlibPlus:
                    if (isHdd) {
                        byte[] zHunk = new byte[(entry.lengthLsb << 16) + entry.lengthLsb];
                        imageStream.seek(entry.offset, SeekOrigin.Begin);
                        imageStream.read(zHunk, 0, zHunk.length);

                        try (DeflateStream zStream =
                                     new DeflateStream(new MemoryStream(zHunk), CompressionMode.Decompress)) {

                            buffer[0] = new byte[bytesPerHunk];
                            int read = zStream.read(buffer[0], 0, bytesPerHunk);

                            if (read != bytesPerHunk) {
                                Debug.printf("Unable to decompress hunk correctly, got %d bytes, expected %d", read, bytesPerHunk);

                                throw new IOException();
                            }
                        } catch (java.io.IOException e) {
                            throw new IOException(e);
                        }
                    }

                    // TODO: Guess wth is MAME doing with these hunks
                    else {
                        Debug.println("Compressed CD/GD-ROM hunks are not yet supported");

                        throw new UnsupportedOperationException();
                    }

                    break;
                case Av:
                    Debug.println("Unsupported compression " + Enums.Compression.values()[hdrCompression]);

                    throw new UnsupportedOperationException();
                }

                break;
            case Uncompressed:
                uncompressedV3:
                buffer[0] = new byte[bytesPerHunk];
                imageStream.seek(entry.offset, SeekOrigin.Begin);
                imageStream.read(buffer[0], 0, buffer.length);

                break;
            case Mini:
                buffer[0] = new byte[bytesPerHunk];
                byte[] mini;
                mini = ByteUtil.getBeBytes(entry.offset);

                for (int i = 0; i < bytesPerHunk; i++)
                    buffer[0][i] = mini[i % 8];

                break;
            case SelfHunk:
                return getHunk(entry.offset, /*out*/ buffer);
            case ParentHunk:
                Debug.println("Parent images are not supported");

                throw new UnsupportedOperationException();
            case SecondCompressed:
                Debug.println("FLAC is not supported");

                throw new UnsupportedOperationException();
            default:
                Debug.printf("Hunk type %d is not supported", entry.flags & 0xF);

                throw new UnsupportedOperationException();
            }

            break;
        case 5:
            if (hdrCompression == 0) {
                buffer[0] = new byte[bytesPerHunk];
                imageStream.seek((long) hunkTableSmall[(int) hunkNo] * bytesPerHunk, SeekOrigin.Begin);
                imageStream.read(buffer[0], 0, buffer.length);
            } else {
                Debug.println("Compressed v5 hunks not yet supported");

                throw new UnsupportedOperationException();
            }

            break;
        default:
            Debug.println("Unsupported hunk map version " + mapVersion);

            throw new UnsupportedOperationException();
        }

        if (hunkCache.size() >= maxBlockCache)
            hunkCache.clear();

        hunkCache.put(hunkNo, buffer[0]);

        return 0;
    }

//#endregion

//#region Identify

    /**  */
    public boolean identify(Stream stream) {
        stream.seek(0, SeekOrigin.Begin);
        byte[] magic = new byte[8];
        stream.read(magic, 0, 8);

        return Arrays.equals(chdTag, magic);
    }

//#endregion

//#region Verify

    /**  */
    public Boolean verifySector(long sectorAddress) {
        if (isHdd)
            return null;

        byte[][] buffer = new byte[1][];
        int errno = readSectorLong(sectorAddress, /*out byte[]*/ buffer);

        return errno != 0 ? null : CdChecksums.checkCdSector(buffer[0]);
    }

    /**  */
    public Boolean verifySectors(long sectorAddress, int length, /*out*/ List<Long> failingLbas,
            /*out*/ List<Long> unknownLbas) {
        unknownLbas = new ArrayList<>();
        failingLbas = new ArrayList<>();

        if (isHdd)
            return null;

        byte[][] buffer = new byte[1][];
        int errno = readSectorsLong(sectorAddress, length, /*out byte[]*/ buffer);

        if (errno != 0)
            return null;

        int bps = buffer.length / length;
        byte[] sector = new byte[bps];

        for (int i = 0; i < length; i++) {
            System.arraycopy(buffer[0], i * bps, sector, 0, bps);
            Boolean sectorStatus = CdChecksums.checkCdSector(sector);

            if (sectorStatus == null) {
                unknownLbas.add((long) i + sectorAddress);
            } else if (!sectorStatus) {
                failingLbas.add((long) i + sectorAddress);
            }
        }

        if (unknownLbas.size() > 0)
            return null;

        return failingLbas.size() <= 0;
    }

    /**  */
    public Boolean verifySectors(long sectorAddress, int length, int track, /*out*/ List<Long> failingLbas,
            /*out*/ List<Long> unknownLbas) {
        unknownLbas = new ArrayList<>();
        failingLbas = new ArrayList<>();

        if (isHdd)
            return null;

        byte[][] buffer = new byte[1][];
        int errno = readSectorsLong(sectorAddress, length, track, /*out byte[]*/ buffer);

        if (errno != 0)
            return null;

        int bps = buffer[0].length / length;
        byte[] sector = new byte[bps];

        for (int i = 0; i < length; i++) {
            System.arraycopy(buffer[0], i * bps, sector, 0, bps);
            Boolean sectorStatus = CdChecksums.checkCdSector(sector);

            if (sectorStatus == null) {
                unknownLbas.add((long) i + sectorAddress);
            } else if (!sectorStatus) {
                failingLbas.add((long) i + sectorAddress);
            }
        }

        if (unknownLbas.size() > 0)
            return null;

        return failingLbas.size() <= 0;
    }

    /**  */
    public Boolean verifyMediaImage() throws NoSuchAlgorithmException {
        byte[] calculated;

        if (mapVersion >= 3) {
            MessageDigest sha1Ctx = MessageDigest.getInstance("Sha1");

            for (int i = 0; i < totalHunks; i++) {
                byte[][] buffer = new byte[1][];
                int errno = getHunk(i, /*out byte[]*/ buffer);

                if (errno != 0)
                    return null;

                sha1Ctx.update(buffer[0]);
            }

            calculated = sha1Ctx.digest();
        } else {
            MessageDigest md5Ctx = MessageDigest.getInstance("Md5");

            for (int i = 0; i < totalHunks; i++) {
                byte[][] buffer = new byte[1][];
                int errno = getHunk(i, /*out byte[]*/ buffer);

                if (errno != 0)
                    return null;

                md5Ctx.update(buffer[0]);
            }

            calculated = md5Ctx.digest();
        }

        return Arrays.equals(expectedChecksum, calculated);
    }

//#endregion
}
