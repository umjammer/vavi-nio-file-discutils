//
// Aaru Data Preservation Suite
//
//
// Filename       : QCOW2.cs
// Author(s)      : Natalia Portillo <claunia@claunia.com>
//
// Component      : Disk image plugins.
//
// Description
//
//     Manages QEMU Copy-On-Write v2 disk images.
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

package aaru.image.qcow2;

import java.io.ByteArrayInputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import aaru.commonType.ImageInfo;
import aaru.commonType.MediaTagType;
import aaru.commonType.SectorTagType;
import aaru.commonType.XmlMediaType;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.FileStream;
import dotnet4j.io.IOException;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Path;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;
import dotnet4j.io.compression.CompressionMode;
import dotnet4j.io.compression.DeflateStream;
import dotnet4j.util.compat.ArrayHelpers;
import dotnet4j.util.compat.Tuple4;
import vavi.util.ByteUtil;
import vavi.util.serdes.Serdes;

import static java.lang.System.getLogger;


/** Implements reading and writing QEMU's Copy On Write v2 and v3 disk images */
public class Qcow2 {

    private static final Logger logger = getLogger(Qcow2.class.getName());

    Map<Long, byte[]> clusterCache;
    int clusterSectors;
    int clusterSize;
    ImageInfo imageInfo;
    Stream imageStream;
    long l1Mask;
    int l1Shift;
    long[] l1Table;
    int l2Bits;
    long l2Mask;
    int l2Size;
    Map<Long, long[]> l2TableCache;
    int maxClusterCache;
    int maxL2TableCache;
    Header header;
    long[] refCountTable;
    Map<Long, byte[]> sectorCache;
    long sectorMask;
    FileStream writingStream;

    public Qcow2() {
        imageInfo = new ImageInfo() {{
            readableSectorTags = new ArrayList<>();
            readableMediaTags = new ArrayList<>();
            hasPartitions = false;
            hasSessions = false;
            version = null;
            application = "QEMU";
            applicationVersion = null;
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
        }};
    }

//#region Properties

    public ImageInfo getInfo() {
        return imageInfo;
    }

    public String name() {
        return "QEMU Copy-On-Write disk image v2";
    }

    public UUID id() {
        return UUID.fromString("F20107CB-95B3-4398-894B-975261F1E8C5");
    }

    public String author() {
        return "Natalia Portillo";
    }

    public String format() {
        return "QEMU Copy-On-Write";
    }

//    public List<DumpHardwareType> dumpHardware() {
//        return null;
//    }

//    public CICMMetadataType cicmMetadata() {
//        return null;
//    }

    public List<MediaTagType> supportedMediaTags() {
        return Collections.emptyList();
    }

    public List<SectorTagType> supportedSectorTags() {
        return Collections.emptyList();
    }

    public List<String/*MediaType*/> supportedMediaTypes() {
        return Arrays.asList(
                "Unknown", "GENERIC_HDD", "FlashDrive", "CompactFlash",
                "CompactFlashType2", "PCCardTypeI", "PCCardTypeII", "PCCardTypeIII",
                "PCCardTypeIV");
    }

    public List<Tuple4<String, Class<?>, String, Object>> supportedOptions() {
        return Collections.emptyList();
    }

    public String[] knownExtensions = new String[] {
            ".qcow2", ".qc2", ".qcow3", ".qc3"
    };
    private boolean isWriting;

    public boolean isWriting() {
        return isWriting;
    }

    private String errorMessage;

    public String getErrorMessage() {
        return errorMessage;
    }

//#endregion

//#region Read

    public int open(Stream stream) {
        stream.seek(0, SeekOrigin.Begin);

        if (stream.getLength() < 512)
            throw new IllegalArgumentException();

        byte[] qHdrB = new byte[512]; // TODO sizeof Header
        stream.read(qHdrB, 0, qHdrB.length);
        header = new Header();
        try {
            Serdes.Util.deserialize(new ByteArrayInputStream(qHdrB), header);
        } catch (java.io.IOException e) {
            throw new IOException(e);
        }

logger.log(Level.DEBUG, "QCOW plugin: qHdr.magic = 0x%08x".formatted(header.magic));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.version = %d".formatted(header.version));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.backingFileOffset = %d".formatted(header.backingFileOffset));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.backingFileSize = %d".formatted(header.backingFileSize));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.clusterBits = %d".formatted(header.clusterBits));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.size = %d".formatted(header.size));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.cryptMethod = %d".formatted(header.cryptMethod));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.l1Size = %d".formatted(header.l1Size));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.l1TableOffset = %d".formatted(header.l1TableOffset));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.refCountTableOffset = %d".formatted(header.refCountTableOffset));

logger.log(Level.DEBUG, "QCOW plugin: qHdr.refCountTableClusters = %d".formatted(header.refCountTableClusters));

logger.log(Level.DEBUG, "QCOW plugin: qHdr.snapshots = %d".formatted(header.snapshots));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.snapshotsOffset = %d".formatted(header.snapshotsOffset));

        if (header.version >= Constants.QCOW_VERSION3) {
logger.log(Level.DEBUG, "QCOW plugin: qHdr.features = %x".formatted(header.features));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.compatFeatures = %x".formatted(header.compatFeatures));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.autoClearFeatures = %x".formatted(header.autoClearFeatures));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.refCountOrder = %d".formatted(header.refCountOrder));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.headerLength = %d".formatted(header.headerLength));

            if ((header.features & Constants.QCOW_FEATURE_MASK) != 0) {
logger.log(Level.DEBUG, "Unknown incompatible features %x enabled, not proceeding.".formatted(header.features & Constants.QCOW_FEATURE_MASK));

                throw new IllegalArgumentException();
            }
        }

        if (header.size <= 1) {
            logger.log(Level.DEBUG, "Image size is too small");

            throw new IllegalArgumentException();
        }

        if (header.clusterBits < 9 || header.clusterBits > 16) {
            throw new IllegalArgumentException("Cluster size must be between 512 bytes and 64 Kbytes");
        }

        if (header.cryptMethod > Constants.QCOW_ENCRYPTION_AES) {
            throw new IllegalArgumentException("Invalid encryption method");
        }

        if (header.cryptMethod > Constants.QCOW_ENCRYPTION_NONE) {
            throw new UnsupportedOperationException("AES encrypted images not yet supported");
        }

        if (header.backingFileOffset != 0) {
            throw new UnsupportedOperationException("Differencing images not yet supported");
        }

        clusterSize = 1 << header.clusterBits;
        clusterSectors = 1 << (header.clusterBits - 9);
        l2Bits = header.clusterBits - 3;
        l2Size = 1 << l2Bits;

logger.log(Level.DEBUG, "QCOW plugin: qHdr.clusterSize = %d".formatted(clusterSize));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.clusterSectors = %d".formatted(clusterSectors));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.qHdr.l1Size = %d".formatted(header.l1Size));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.l2Size = %d".formatted(l2Size));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.sectors = %d".formatted(imageInfo.sectors));

        byte[] l1TableB = new byte[header.l1Size * 8];
        stream.seek(header.l1TableOffset, SeekOrigin.Begin);
        stream.read(l1TableB, 0, header.l1Size * 8);
        l1Table = new long[header.l1Size];
        ByteBuffer.wrap(l1TableB).order(ByteOrder.LITTLE_ENDIAN).asLongBuffer().put(l1Table);
logger.log(Level.DEBUG, String.format("QCOW plugin: Reading L1 table"));

        l1Mask = 0;
        int c = 0;
        l1Shift = l2Bits + header.clusterBits;

        for (int i = 0; i < 64; i++) {
            l1Mask <<= 1;

            if (c >= 64 - l1Shift)
                continue;

            l1Mask += 1;
            c++;
        }

        l2Mask = 0;

        for (int i = 0; i < l2Bits; i++)
            l2Mask = (l2Mask << 1) + 1;

        l2Mask <<= header.clusterBits;

        sectorMask = 0;

        for (int i = 0; i < header.clusterBits; i++)
            sectorMask = (sectorMask << 1) + 1;

logger.log(Level.DEBUG, "QCOW plugin: qHdr.l1Mask = %x".formatted(l1Mask));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.l1Shift = %d".formatted(l1Shift));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.l2Mask = %x".formatted(l2Mask));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.sectorMask = %x".formatted(sectorMask));

        maxL2TableCache = Constants.MAX_CACHE_SIZE / (l2Size * 8);
        maxClusterCache = Constants.MAX_CACHE_SIZE / clusterSize;

        imageStream = stream;

        sectorCache = new HashMap<>();
        l2TableCache = new HashMap<>();
        clusterCache = new HashMap<>();

        imageInfo.creationTime = Instant.now(); // creationTime
        imageInfo.lastModificationTime = Instant.now(); // LastWriteTime
        imageInfo.mediaTitle = Path.getFileNameWithoutExtension(stream.toString());
        imageInfo.sectors = header.size / 512;
        imageInfo.sectorSize = 512;
        imageInfo.xmlMediaType = XmlMediaType.BlockMedia;
        imageInfo.mediaType = "GENERIC_HDD";
        imageInfo.imageSize = header.size;
        imageInfo.version = String.valueOf(header.version);

        imageInfo.cylinders = (int) (imageInfo.sectors / 16 / 63);
        imageInfo.heads = 16;
        imageInfo.sectorsPerTrack = 63;

        return 0;
    }

    public int readSector(long sectorAddress, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        if (sectorAddress > imageInfo.sectors - 1)
            throw new IndexOutOfBoundsException();

        // Check cache
        buffer[0] = sectorCache.getOrDefault(sectorAddress, null);
        if (buffer[0] != null)
            return 0;

        long byteAddress = sectorAddress * 512;

        long l1Off = (byteAddress & l1Mask) >> l1Shift;

        if (l1Off >= l1Table.length) {
logger.log(Level.DEBUG, "QCOW2 plugin: Trying to read past L1 table, position %d of a max %d".formatted(l1Off, l1Table.length));

            throw new IllegalArgumentException();
        }

        // TODO: Implement differential images
        if (l1Table[(int) l1Off] == 0) {
            buffer[0] = new byte[512];

            return 0;
        }

        long[] l2Table = l2TableCache.getOrDefault(l1Off, null);
        if (l2Table == null) {
            imageStream.seek(l1Table[(int) l1Off] & Constants.QCOW_FLAGS_MASK, SeekOrigin.Begin);
            byte[] l2TableB = new byte[l2Size * 8];
            imageStream.read(l2TableB, 0, l2Size * 8);
logger.log(Level.DEBUG, "QCOW plugin: Reading L2 table #%d".formatted(l1Off));
            l2Table = ByteBuffer.wrap(l2TableB).order(ByteOrder.LITTLE_ENDIAN).asLongBuffer().array();

            if (l2TableCache.size() >= maxL2TableCache)
                l2TableCache.clear();

            l2TableCache.put(l1Off, l2Table);
        }

        long l2Off = (byteAddress & l2Mask) >> header.clusterBits;

        long offset = l2Table[(int) l2Off];

        buffer[0] = new byte[512];

        if ((offset & Constants.QCOW_FLAGS_MASK) != 0) {
            byte[] cluster = clusterCache.getOrDefault(offset, null);
            if (cluster == null) {
                if ((offset & Constants.QCOW_COMPRESSED) == Constants.QCOW_COMPRESSED) {
                    long compSizeMask = (1L << (header.clusterBits - 8)) - 1;
                    byte countbits = (byte) (header.clusterBits - 8);
                    compSizeMask <<= 62 - countbits;
                    long offMask = ~compSizeMask & Constants.QCOW_FLAGS_MASK;

                    long realOff = offset & offMask;
                    long compSize = (((offset & compSizeMask) >> (62 - countbits)) + 1) * 512;

                    byte[] zCluster = new byte[(int) compSize];
                    imageStream.seek(realOff, SeekOrigin.Begin);
                    imageStream.read(zCluster, 0, (int) compSize);

                    DeflateStream zStream = new DeflateStream(new MemoryStream(zCluster), CompressionMode.Decompress);
                    cluster = new byte[clusterSize];
                    int read = zStream.read(cluster, 0, clusterSize);

                    if (read != clusterSize)
                        throw new IOException();
                } else {
                    cluster = new byte[clusterSize];
                    imageStream.seek(offset & Constants.QCOW_FLAGS_MASK, SeekOrigin.Begin);
                    imageStream.read(cluster, 0, clusterSize);
                }

                if (clusterCache.size() >= maxClusterCache)
                    clusterCache.clear();

                clusterCache.put(offset, cluster);
            }

            System.arraycopy(cluster, (int) (byteAddress & sectorMask), buffer[0], 0, 512);
        }

        if (sectorCache.size() >= Constants.MAX_CACHED_SECTORS)
            sectorCache.clear();

        sectorCache.put(sectorAddress, buffer[0]);

        return 0;
    }

    public int readSectors(long sectorAddress, int length, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        if (sectorAddress > imageInfo.sectors - 1)
            throw new IndexOutOfBoundsException();

        if (sectorAddress + length > imageInfo.sectors)
            throw new IndexOutOfBoundsException();

        MemoryStream ms = new MemoryStream();

        for (int i = 0; i < length; i++) {
            byte[][] sector = new byte[1][];
            int errno = readSector(sectorAddress + i, /*out*/sector);

            if (errno != 0)
                return errno;

            ms.write(sector[0], 0, sector.length);
        }

        buffer[0] = ms.toArray();

        ms.close();

        return 0;
    }

//#endregion

//#region Write

    public boolean create(String path, String mediaType, Map<String, String> options, long sectors,
                          int sectorSize) {
        if (sectorSize != 512) {
            errorMessage = "Unsupported sector size";

            return false;
        }

        if (!supportedMediaTypes().contains(mediaType)) {
            errorMessage = "Unsupported media format " + mediaType;

            return false;
        }

        // TODO: Correct this calculation
        if (sectors * sectorSize / 65536 > Integer.MAX_VALUE) {
            errorMessage = "Too many sectors for selected cluster size";

            return false;
        }

        imageInfo = new ImageInfo();
        imageInfo.mediaType = mediaType;
        imageInfo.sectorSize = sectorSize;
        imageInfo.sectors = sectors;

        try {
            writingStream = new FileStream(path, FileMode.OpenOrCreate, FileAccess.ReadWrite, FileShare.None);
        } catch (IOException e) {
            errorMessage = "Could not create new image file, exception " + e.getMessage();

            return false;
        }

        String extension = Path.getExtension(path);
        boolean version3 = extension.equals(".qcow3") || extension.equals(".qc3");

        header = new Header();
        header.magic = Constants.QCOW_MAGIC;
        header.version = version3 ? Constants.QCOW_VERSION3 : Constants.QCOW_VERSION2;
        header.size = sectors * sectorSize;
        header.clusterBits = 16;
        header.headerLength = 512; // TODO sizeof Header

        clusterSize = 1 << header.clusterBits;
        clusterSectors = 1 << (header.clusterBits - 9);
        l2Bits = header.clusterBits - 3;
        l2Size = 1 << l2Bits;

        l1Mask = 0;
        int c = 0;
        l1Shift = l2Bits + header.clusterBits;

        for (int i = 0; i < 64; i++) {
            l1Mask <<= 1;

            if (c >= 64 - l1Shift)
                continue;

            l1Mask += 1;
            c++;
        }

        l2Mask = 0;

        for (int i = 0; i < l2Bits; i++)
            l2Mask = (l2Mask << 1) + 1;

        l2Mask <<= header.clusterBits;

        sectorMask = 0;

        for (int i = 0; i < header.clusterBits; i++)
            sectorMask = (sectorMask << 1) + 1;

        header.l1Size = (int) ((header.size + (1 << l1Shift) - 1) >> l1Shift);

        if (header.l1Size == 0)
            header.l1Size = 1;

       l1Table = new long[header.l1Size];

        long clusters = header.size / (long) clusterSize;
        long refCountBlocks = clusters * 2 / (long) clusterSize;

        if (clusters * 2 % (long) clusterSize > 0)
            refCountBlocks++;

        if (refCountBlocks == 0)
            refCountBlocks = 1;

        header.refCountTableOffset = clusterSize;
        header.refCountTableClusters = (int) (refCountBlocks * 8 / (long) clusterSize);

        if (header.refCountTableClusters == 0)
            header.refCountTableClusters = 1;

        refCountTable = new long[(int) refCountBlocks];
        header.l1TableOffset = header.refCountTableOffset + ((long) header.refCountTableClusters * clusterSize);
        long l1TableClusters = header.l1Size * 8L / (long) clusterSize;

        if (l1TableClusters == 0)
            l1TableClusters = 1;

        byte[] empty = new byte[(int) (header.l1TableOffset + (l1TableClusters * (long) clusterSize))];
        writingStream.write(empty, 0, empty.length);

        isWriting = true;
        errorMessage = null;

        return true;
    }

    public boolean writeMediaTag(byte[] data, MediaTagType tag) {
        errorMessage = "Writing media tags is not supported.";

        return false;
    }

    public boolean writeSector(byte[] data, long sectorAddress) {
        if (!isWriting) {
            errorMessage = "Tried to write on a non-writable image";

            return false;
        }

        if (data.length != imageInfo.sectorSize) {
            errorMessage = "Incorrect data size";

            return false;
        }

        if (sectorAddress >= imageInfo.sectors) {
            errorMessage = "Tried to write past image size";

            return false;
        }

        // Ignore empty sectors
        if (ArrayHelpers.isArrayNullOrEmpty(data))
            return true;

        long byteAddress = sectorAddress * 512;

        long l1Off = (byteAddress & l1Mask) >> l1Shift;

        if (l1Off >= l1Table.length) {
            errorMessage = "Trying to write past L1 table, position %d of a max %d".formatted(l1Off, l1Table.length);

            return false;
        }

        if (l1Table[(int) l1Off] == 0) {
            writingStream.seek(0, SeekOrigin.End);
            l1Table[(int) l1Off] = writingStream.position();
            byte[] l2TableB = new byte[l2Size * 8];
            writingStream.seek(0, SeekOrigin.End);
            writingStream.write(l2TableB, 0, l2TableB.length);
        }

        writingStream.position(l1Table[(int) l1Off]);

        long l2Off = (byteAddress & l2Mask) >> header.clusterBits;

        writingStream.seek(l1Table[(int) l1Off] + (l2Off * 8), SeekOrigin.Begin);

        byte[] entry = new byte[8];
        writingStream.read(entry, 0, 8);
        long offset = ByteUtil.readBeLong(entry, 0);

        if (offset == 0) {
            offset = writingStream.getLength();
            byte[] cluster = new byte[clusterSize];
            entry = ByteUtil.getBeBytes(offset);
            writingStream.seek(l1Table[(int) l1Off] + (l2Off * 8), SeekOrigin.Begin);
            writingStream.write(entry, 0, 8);
            writingStream.seek(0, SeekOrigin.End);
            writingStream.write(cluster, 0, cluster.length);
        }

        writingStream.seek(offset + (byteAddress & sectorMask), SeekOrigin.Begin);
        writingStream.write(data, 0, data.length);

        int refCountBlockEntries = clusterSize * 8 / 16;
        long refCountBlockIndex = offset / (long) clusterSize % (long) refCountBlockEntries;
        long refCountTableIndex = offset / (long) clusterSize / (long) refCountBlockEntries;

        long refBlockOffset = refCountTable[(int) refCountTableIndex];

        if (refBlockOffset == 0) {
            refBlockOffset = writingStream.getLength();
            refCountTable[(int) refCountTableIndex] = refBlockOffset;
            byte[] cluster = new byte[clusterSize];
            writingStream.seek(0, SeekOrigin.End);
            writingStream.write(cluster, 0, cluster.length);
        }

        writingStream.seek(refBlockOffset + refCountBlockIndex, SeekOrigin.Begin);

        writingStream.write(new byte[] {
                0, 1
        }, 0, 2);

        errorMessage = "";

        return true;
    }

    // TODO: This can be optimized
    public boolean writeSectors(byte[] data, long sectorAddress, int length) {
        if (!isWriting) {
            errorMessage = "Tried to write on a non-writable image";

            return false;
        }

        if (data.length % imageInfo.sectorSize != 0) {
            errorMessage = "Incorrect data size";

            return false;
        }

        if (sectorAddress + length > imageInfo.sectors) {
            errorMessage = "Tried to write past image size";

            return false;
        }

        // Ignore empty sectors
        if (ArrayHelpers.isArrayNullOrEmpty(data))
            return true;

        for (int i = 0; i < length; i++) {
            byte[] tmp = new byte[imageInfo.sectorSize];
            System.arraycopy(data, i * imageInfo.sectorSize, tmp, 0, imageInfo.sectorSize);

            if (!writeSector(tmp, sectorAddress + i))
                return false;
        }

        errorMessage = "";

        return true;
    }

    public boolean writeSectorLong(byte[] data, long sectorAddress) {
        errorMessage = "Writing sectors with tags is not supported.";

        return false;
    }

    public boolean writeSectorsLong(byte[] data, long sectorAddress, int length) {
        errorMessage = "Writing sectors with tags is not supported.";

        return false;
    }

    public boolean close() {
        if (!isWriting) {
            errorMessage = "Image is not opened for writing";

            return false;
        }

        writingStream.seek(0, SeekOrigin.Begin);
        writingStream.write(ByteUtil.getBeBytes(header.magic), 0, 4);
        writingStream.write(ByteUtil.getBeBytes(header.version), 0, 4);
        writingStream.write(ByteUtil.getBeBytes(header.backingFileOffset), 0, 8);
        writingStream.write(ByteUtil.getBeBytes(header.backingFileSize), 0, 4);
        writingStream.write(ByteUtil.getBeBytes(header.clusterBits), 0, 4);
        writingStream.write(ByteUtil.getBeBytes(header.size), 0, 8);
        writingStream.write(ByteUtil.getBeBytes(header.cryptMethod), 0, 4);
        writingStream.write(ByteUtil.getBeBytes(header.l1Size), 0, 4);
        writingStream.write(ByteUtil.getBeBytes(header.l1TableOffset), 0, 8);
        writingStream.write(ByteUtil.getBeBytes(header.refCountTableOffset), 0, 8);
        writingStream.write(ByteUtil.getBeBytes(header.refCountTableClusters), 0, 4);
        writingStream.write(ByteUtil.getBeBytes(header.snapshots), 0, 4);
        writingStream.write(ByteUtil.getBeBytes(header.snapshotsOffset), 0, 8);

        if (header.version == Constants.QCOW_VERSION3) {
            writingStream.write(ByteUtil.getBeBytes(header.features), 0, 8);
            writingStream.write(ByteUtil.getBeBytes(header.compatFeatures), 0, 8);
            writingStream.write(ByteUtil.getBeBytes(header.autoClearFeatures), 0, 8);
            writingStream.write(ByteUtil.getBeBytes(header.refCountOrder), 0, 4);
            writingStream.write(ByteUtil.getBeBytes(header.headerLength), 0, 4);
        }

        writingStream.seek(header.refCountTableOffset, SeekOrigin.Begin);

        for (long l : refCountTable)
            writingStream.write(ByteUtil.getBeBytes(l), 0, 8);

        writingStream.seek(header.l1TableOffset, SeekOrigin.Begin);

        ByteBuffer bb = ByteBuffer.allocate(l1Table.length * 8);
        bb.order(ByteOrder.BIG_ENDIAN).asLongBuffer().put(l1Table);
        byte[] l1TableB = bb.array();
        writingStream.write(l1TableB, 0, l1TableB.length);

        writingStream.flush();
        writingStream.close();

        isWriting = false;
        errorMessage = "";

        return true;
    }

    public boolean setMetadata(ImageInfo metadata) {
        return true;
    }

    public boolean setGeometry(int cylinders, int heads, int sectorsPerTrack) {
        return true;
    }

    public boolean writeSectorTag(byte[] data, long sectorAddress, SectorTagType tag) {
        errorMessage = "Writing sectors with tags is not supported.";

        return false;
    }

    public boolean writeSectorsTag(byte[] data, long sectorAddress, int length, SectorTagType tag) {
        errorMessage = "Writing sectors with tags is not supported.";

        return false;
    }

//    public boolean setDumpHardware(List<DumpHardwareType> dumpHardware) {
//        return false;
//    }

//    public boolean setCicmMetadata(CICMMetadataType metadata) {
//        return false;
//    }

//#endregion

//#region Unsupported

    public int readSectorTag(long sectorAddress, SectorTagType tag, /*out*/ byte[] buffer) {
        buffer = null;

        throw new UnsupportedOperationException();
    }

    public int readSectorsTag(long sectorAddress, int length, SectorTagType tag, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        throw new UnsupportedOperationException();
    }

    public int readMediaTag(MediaTagType tag, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        throw new UnsupportedOperationException();
    }

    public int readSectorLong(long sectorAddress, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        throw new UnsupportedOperationException();
    }

    public int readSectorsLong(long sectorAddress, int length, /*out*/ byte[][] buffer) {
        buffer[0] = null;

        throw new UnsupportedOperationException();
    }

//#endregion

//#region Identify

    public boolean identify(Stream stream) {
        stream.seek(0, SeekOrigin.Begin);

        if (stream.getLength() < 512)
            return false;

        byte[] qHdrB = new byte[512]; // TODO sizeof Header
        stream.read(qHdrB, 0, qHdrB.length);
        header = new Header();
        try {
            Serdes.Util.deserialize(new ByteArrayInputStream(qHdrB), header);
        } catch (java.io.IOException e) {
            throw new IOException(e);
        }

logger.log(Level.DEBUG, "QCOW plugin: qHdr.magic = 0x%08x".formatted(header.magic));
logger.log(Level.DEBUG, "QCOW plugin: qHdr.version = %d".formatted(header.version));

        return header.magic == Constants.QCOW_MAGIC && (header.version == Constants.QCOW_VERSION2 || header.version == Constants.QCOW_VERSION3);
    }

//#endregion
}