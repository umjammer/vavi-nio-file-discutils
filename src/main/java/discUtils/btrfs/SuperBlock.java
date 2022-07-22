//
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

package discUtils.btrfs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.IntStream;

import discUtils.btrfs.base.ChecksumType;
import discUtils.btrfs.base.Key;
import discUtils.btrfs.base.items.ChunkItem;
import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public class SuperBlock implements IByteArraySerializable {

    public static final int Length = 0x1000;

    static {
        ByteBuffer buffer = ByteBuffer.wrap("_BHRfS_M".getBytes(StandardCharsets.US_ASCII));
        buffer.order(ByteOrder.nativeOrder());
        BtrfsMagic = buffer.getLong();
    }

    public static final long BtrfsMagic;

    /**
     * Checksum of everything past this field (from 20 to 1000)
     */
    private byte[] checksum;

    public byte[] getChecksum() {
        return checksum;
    }

    public void setChecksum(byte[] value) {
        checksum = value;
    }

    /**
     * FS UUID
     */
    private UUID fsUuid;

    public UUID getFsUuid() {
        return fsUuid;
    }

    public void setFsUuid(UUID value) {
        fsUuid = value;
    }

    /**
     * physical address of this block (different for mirrors)
     */
    private long physicalAddress;

    public long getPhysicalAddress() {
        return physicalAddress;
    }

    public void setPhysicalAddress(long value) {
        physicalAddress = value;
    }

    /**
     * flags
     */
    private long flags;

    public long getFlags() {
        return flags;
    }

    public void setFlags(long value) {
        flags = value;
    }

    /**
     * magic ("_BHRfS_M")
     */
    private long magic;

    public long getMagic() {
        return magic;
    }

    public void setMagic(long value) {
        magic = value;
    }

    /**
     * generation
     */
    private long generation;

    public long getGeneration() {
        return generation;
    }

    public void setGeneration(long value) {
        generation = value;
    }

    /**
     * logical address of the root tree root
     */
    private long root;

    public long getRoot() {
        return root;
    }

    public void setRoot(long value) {
        root = value;
    }

    /**
     * logical address of the chunk tree root
     */
    private long chunkRoot;

    public long getChunkRoot() {
        return chunkRoot;
    }

    public void setChunkRoot(long value) {
        chunkRoot = value;
    }

    /**
     * logical address of the log tree root
     */
    private long logRoot;

    public long getLogRoot() {
        return logRoot;
    }

    public void setLogRoot(long value) {
        logRoot = value;
    }

    /**
     * log_root_transid
     */
    private long logRootTransId;

    public long getLogRootTransId() {
        return logRootTransId;
    }

    public void setLogRootTransId(long value) {
        logRootTransId = value;
    }

    /**
     * total_bytes
     */
    private long totalBytes;

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long value) {
        totalBytes = value;
    }

    /**
     * bytes_used
     */
    private long bytesUsed;

    public long getBytesUsed() {
        return bytesUsed;
    }

    public void setBytesUsed(long value) {
        bytesUsed = value;
    }

    /**
     * root_dir_objectid (usually 6)
     */
    private long rootDirObjectid;

    public long getRootDirObjectid() {
        return rootDirObjectid;
    }

    public void setRootDirObjectid(long value) {
        rootDirObjectid = value;
    }

    /**
     * num_devices
     */
    private long numDevices;

    public long getNumDevices() {
        return numDevices;
    }

    public void setNumDevices(long value) {
        numDevices = value;
    }

    /**
     * sectorsize
     */
    private int sectorSize;

    public int getSectorSize() {
        return sectorSize;
    }

    public void setSectorSize(int value) {
        sectorSize = value;
    }

    /**
     * nodesize
     */
    private int nodeSize;

    public int getNodeSize() {
        return nodeSize;
    }

    public void setNodeSize(int value) {
        nodeSize = value;
    }

    /**
     * leafsize
     */
    private int leafSize;

    public int getLeafSize() {
        return leafSize;
    }

    public void setLeafSize(int value) {
        leafSize = value;
    }

    /**
     * stripesize
     */
    private int stripeSize;

    public int getStripeSize() {
        return stripeSize;
    }

    public void setStripeSize(int value) {
        stripeSize = value;
    }

    /**
     * chunk_root_generation
     */
    private long chunkRootGeneration;

    public long getChunkRootGeneration() {
        return chunkRootGeneration;
    }

    public void setChunkRootGeneration(long value) {
        chunkRootGeneration = value;
    }

    /**
     * compat_flags
     */
    private long compatFlags;

    public long getCompatFlags() {
        return compatFlags;
    }

    public void setCompatFlags(long value) {
        compatFlags = value;
    }

    /**
     * compat_ro_flags - only implementations that support the flags can write
     * to the filesystem
     */
    private long compatRoFlags;

    public long getCompatRoFlags() {
        return compatRoFlags;
    }

    public void setCompatRoFlags(long value) {
        compatRoFlags = value;
    }

    /**
     * incompat_flags - only implementations that support the flags can use the
     * filesystem
     */
    private long incompatFlags;

    public long getIncompatFlags() {
        return incompatFlags;
    }

    public void setIncompatFlags(long value) {
        incompatFlags = value;
    }

    /**
     * csum_type - btrfs currently uses the CRC32c little-endian hash function
     * with seed -1.
     */
    private ChecksumType checksumType = ChecksumType.Crc32C;

    public ChecksumType getChecksumType() {
        return checksumType;
    }

    public void setChecksumType(ChecksumType value) {
        checksumType = value;
    }

    /**
     * root_level
     */
    private byte rootLevel;

    public int getRootLevel() {
        return rootLevel & 0xff;
    }

    public void setRootLevel(byte value) {
        rootLevel = value;
    }

    /**
     * chunk_root_level
     */
    private byte chunkRootLevel;

    public int getChunkRootLevel() {
        return chunkRootLevel & 0xff;
    }

    public void setChunkRootLevel(byte value) {
        chunkRootLevel = value;
    }

    /**
     * log_root_level
     */
    private byte logRootLevel;

    public int getLogRootLevel() {
        return logRootLevel & 0xff;
    }

    public void setLogRootLevel(byte value) {
        logRootLevel = value;
    }

    /**
     * label (may not contain '\\')
     */
    private String label;

    public String getLabel() {
        return label;
    }

    public void setLabel(String value) {
        label = value;
    }

    private ChunkItem[] systemChunkArray;

    public ChunkItem[] getSystemChunkArray() {
        return systemChunkArray;
    }

    public void setSystemChunkArray(ChunkItem[] value) {
        systemChunkArray = value;
    }

    public int size() {
        return Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        magic = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x40);
        if (magic != BtrfsMagic)
            return size();

        checksum = EndianUtilities.toByteArray(buffer, offset, 0x20);
        fsUuid = EndianUtilities.toGuidLittleEndian(buffer, offset + 0x20);
        physicalAddress = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x30);
        flags = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x38);
        generation = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x48);
        root = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x50);
        chunkRoot = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x58);
        logRoot = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x60);
        logRootTransId = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x68);
        totalBytes = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x70);
        bytesUsed = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x78);
        rootDirObjectid = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x80);
        numDevices = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x88);
        sectorSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x90);
        nodeSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x94);
        leafSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x98);
        stripeSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x9c);
        chunkRootGeneration = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0xa4);
        compatFlags = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0xac);
        compatRoFlags = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0xb4);
        incompatFlags = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0xbc);
        checksumType = ChecksumType.values()[EndianUtilities.toUInt16LittleEndian(buffer, offset + 0xc4)];
        rootLevel = buffer[offset + 0xc6];
        chunkRootLevel = buffer[offset + 0xc7];
        logRootLevel = buffer[offset + 0xc8];
        // c9 62 DEV_ITEM data for this device
        byte[] labelData = EndianUtilities.toByteArray(buffer, offset + 0x12b, 0x100);
        OptionalInt eos = IntStream.range(0, labelData.length).filter(i -> labelData[i] == (byte) 0).findFirst();
        if (eos.isPresent()) {
            label = new String(labelData, 0, eos.getAsInt(), StandardCharsets.UTF_8);
        }

        // 22b 100 reserved
        int n = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0xa0);
        offset += 0x32b;
        List<ChunkItem> systemChunks = new ArrayList<>();
        while (n > 0) {
            Key key = new Key();
            offset += key.readFrom(buffer, offset);
            ChunkItem chunkItem = new ChunkItem(key);
            offset += chunkItem.readFrom(buffer, offset);
            systemChunks.add(chunkItem);
            n = n - key.size() - chunkItem.size();
        }
        systemChunkArray = systemChunks.toArray(new ChunkItem[0]);
        // 32b 800 (n bytes valid) Contains (KEY, CHUNK_ITEM) pairs for all
        //         SYSTEM chunks. This is needed to bootstrap the mapping from logical
        //         addresses to physical.
        // b2b 4d5 Currently unused
        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
