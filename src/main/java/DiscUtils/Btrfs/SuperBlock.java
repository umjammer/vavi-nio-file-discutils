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

package DiscUtils.Btrfs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import DiscUtils.Btrfs.Base.ChecksumType;
import DiscUtils.Btrfs.Base.Key;
import DiscUtils.Btrfs.Base.Items.ChunkItem;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class SuperBlock implements IByteArraySerializable {
    public static final int Length = 0x1000;

    static {
        ByteBuffer buffer = ByteBuffer.wrap("_BHRfS_M".getBytes(Charset.forName("ASCII")));
        buffer.order(ByteOrder.nativeOrder());
        BtrfsMagic = buffer.getLong();
    }

    public static final long BtrfsMagic;

    /**
     * Checksum of everything past this field (from 20 to 1000)
     */
    private byte[] __Checksum;

    public byte[] getChecksum() {
        return __Checksum;
    }

    public void setChecksum(byte[] value) {
        __Checksum = value;
    }

    /**
     * FS UUID
     */
    private UUID __FsUuid;

    public UUID getFsUuid() {
        return __FsUuid;
    }

    public void setFsUuid(UUID value) {
        __FsUuid = value;
    }

    /**
     * physical address of this block (different for mirrors)
     */
    private long __PhysicalAddress;

    public long getPhysicalAddress() {
        return __PhysicalAddress;
    }

    public void setPhysicalAddress(long value) {
        __PhysicalAddress = value;
    }

    /**
     * flags
     */
    private long __Flags;

    public long getFlags() {
        return __Flags;
    }

    public void setFlags(long value) {
        __Flags = value;
    }

    /**
     * magic ("_BHRfS_M")
     */
    private long __Magic;

    public long getMagic() {
        return __Magic;
    }

    public void setMagic(long value) {
        __Magic = value;
    }

    /**
     * generation
     */
    private long __Generation;

    public long getGeneration() {
        return __Generation;
    }

    public void setGeneration(long value) {
        __Generation = value;
    }

    /**
     * logical address of the root tree root
     */
    private long __Root;

    public long getRoot() {
        return __Root;
    }

    public void setRoot(long value) {
        __Root = value;
    }

    /**
     * logical address of the chunk tree root
     */
    private long __ChunkRoot;

    public long getChunkRoot() {
        return __ChunkRoot;
    }

    public void setChunkRoot(long value) {
        __ChunkRoot = value;
    }

    /**
     * logical address of the log tree root
     */
    private long __LogRoot;

    public long getLogRoot() {
        return __LogRoot;
    }

    public void setLogRoot(long value) {
        __LogRoot = value;
    }

    /**
     * log_root_transid
     */
    private long __LogRootTransId;

    public long getLogRootTransId() {
        return __LogRootTransId;
    }

    public void setLogRootTransId(long value) {
        __LogRootTransId = value;
    }

    /**
     * total_bytes
     */
    private long __TotalBytes;

    public long getTotalBytes() {
        return __TotalBytes;
    }

    public void setTotalBytes(long value) {
        __TotalBytes = value;
    }

    /**
     * bytes_used
     */
    private long __BytesUsed;

    public long getBytesUsed() {
        return __BytesUsed;
    }

    public void setBytesUsed(long value) {
        __BytesUsed = value;
    }

    /**
     * root_dir_objectid (usually 6)
     */
    private long __RootDirObjectid;

    public long getRootDirObjectid() {
        return __RootDirObjectid;
    }

    public void setRootDirObjectid(long value) {
        __RootDirObjectid = value;
    }

    /**
     * num_devices
     */
    private long __NumDevices;

    public long getNumDevices() {
        return __NumDevices;
    }

    public void setNumDevices(long value) {
        __NumDevices = value;
    }

    /**
     * sectorsize
     */
    private int __SectorSize;

    public int getSectorSize() {
        return __SectorSize;
    }

    public void setSectorSize(int value) {
        __SectorSize = value;
    }

    /**
     * nodesize
     */
    private int __NodeSize;

    public int getNodeSize() {
        return __NodeSize;
    }

    public void setNodeSize(int value) {
        __NodeSize = value;
    }

    /**
     * leafsize
     */
    private int __LeafSize;

    public int getLeafSize() {
        return __LeafSize;
    }

    public void setLeafSize(int value) {
        __LeafSize = value;
    }

    /**
     * stripesize
     */
    private int __StripeSize;

    public int getStripeSize() {
        return __StripeSize;
    }

    public void setStripeSize(int value) {
        __StripeSize = value;
    }

    /**
     * chunk_root_generation
     */
    private long __ChunkRootGeneration;

    public long getChunkRootGeneration() {
        return __ChunkRootGeneration;
    }

    public void setChunkRootGeneration(long value) {
        __ChunkRootGeneration = value;
    }

    /**
     * compat_flags
     */
    private long __CompatFlags;

    public long getCompatFlags() {
        return __CompatFlags;
    }

    public void setCompatFlags(long value) {
        __CompatFlags = value;
    }

    /**
     * compat_ro_flags - only implementations that support the flags can write
     * to the filesystem
     */
    private long __CompatRoFlags;

    public long getCompatRoFlags() {
        return __CompatRoFlags;
    }

    public void setCompatRoFlags(long value) {
        __CompatRoFlags = value;
    }

    /**
     * incompat_flags - only implementations that support the flags can use the
     * filesystem
     */
    private long __IncompatFlags;

    public long getIncompatFlags() {
        return __IncompatFlags;
    }

    public void setIncompatFlags(long value) {
        __IncompatFlags = value;
    }

    /**
     * csum_type - Btrfs currently uses the CRC32c little-endian hash function
     * with seed -1.
     */
    private ChecksumType __ChecksumType = ChecksumType.Crc32C;

    public ChecksumType getChecksumType() {
        return __ChecksumType;
    }

    public void setChecksumType(ChecksumType value) {
        __ChecksumType = value;
    }

    /**
     * root_level
     */
    private byte __RootLevel;

    public byte getRootLevel() {
        return __RootLevel;
    }

    public void setRootLevel(byte value) {
        __RootLevel = value;
    }

    /**
     * chunk_root_level
     */
    private byte __ChunkRootLevel;

    public byte getChunkRootLevel() {
        return __ChunkRootLevel;
    }

    public void setChunkRootLevel(byte value) {
        __ChunkRootLevel = value;
    }

    /**
     * log_root_level
     */
    private byte __LogRootLevel;

    public byte getLogRootLevel() {
        return __LogRootLevel;
    }

    public void setLogRootLevel(byte value) {
        __LogRootLevel = value;
    }

    /**
     * label (may not contain '\\')
     */
    private String __Label;

    public String getLabel() {
        return __Label;
    }

    public void setLabel(String value) {
        __Label = value;
    }

    private ChunkItem[] __SystemChunkArray;

    public ChunkItem[] getSystemChunkArray() {
        return __SystemChunkArray;
    }

    public void setSystemChunkArray(ChunkItem[] value) {
        __SystemChunkArray = value;
    }

    public int sizeOf() {
        return Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        setMagic(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x40));
        if (getMagic() != BtrfsMagic)
            return sizeOf();

        setChecksum(EndianUtilities.toByteArray(buffer, offset, 0x20));
        setFsUuid(EndianUtilities.toGuidLittleEndian(buffer, offset + 0x20));
        setPhysicalAddress(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x30));
        setFlags(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x38));
        setGeneration(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x48));
        setRoot(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x50));
        setChunkRoot(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x58));
        setLogRoot(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x60));
        setLogRootTransId(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x68));
        setTotalBytes(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x70));
        setBytesUsed(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x78));
        setRootDirObjectid(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x80));
        setNumDevices(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x88));
        setSectorSize(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x90));
        setNodeSize(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x94));
        setLeafSize(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x98));
        setStripeSize(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x9c));
        setChunkRootGeneration(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0xa4));
        setCompatFlags(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0xac));
        setCompatRoFlags(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0xb4));
        setIncompatFlags(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0xbc));
        setChecksumType(ChecksumType.valueOf(EndianUtilities.toUInt16LittleEndian(buffer, offset + 0xc4)));
        setRootLevel(buffer[offset + 0xc6]);
        setChunkRootLevel(buffer[offset + 0xc7]);
        setLogRootLevel(buffer[offset + 0xc8]);
        //c9 62 DEV_ITEM data for this device
        byte[] labelData = EndianUtilities.toByteArray(buffer, offset + 0x12b, 0x100);
        int eos = Arrays.binarySearch(labelData, (byte) 0);
        if (eos != -1) {
            setLabel(new String(labelData, 0, eos, Charset.forName("UTF8")));
        }

        //22b 100 reserved
        int n = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0xa0);
        offset += 0x32b;
        List<ChunkItem> systemChunks = new ArrayList<>();
        while (n > 0) {
            Key key = new Key();
            offset += key.readFrom(buffer, offset);
            ChunkItem chunkItem = new ChunkItem(key);
            offset += chunkItem.readFrom(buffer, offset);
            systemChunks.add(chunkItem);
            n = n - key.sizeOf() - chunkItem.sizeOf();
        }
        setSystemChunkArray(systemChunks.toArray(new ChunkItem[0]));
        return sizeOf();
    }

    //32b 800 (n bytes valid) Contains (KEY, CHUNK_ITEM) pairs for all SYSTEM chunks. This is needed to bootstrap the mapping from logical addresses to physical.
    //b2b 4d5 Currently unused
    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
