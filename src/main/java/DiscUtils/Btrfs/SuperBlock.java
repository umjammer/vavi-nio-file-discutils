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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.IntStream;

import DiscUtils.Btrfs.Base.ChecksumType;
import DiscUtils.Btrfs.Base.Key;
import DiscUtils.Btrfs.Base.Items.ChunkItem;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


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
    private byte[] _checksum;

    public byte[] getChecksum() {
        return _checksum;
    }

    public void setChecksum(byte[] value) {
        _checksum = value;
    }

    /**
     * FS UUID
     */
    private UUID _fsUuid;

    public UUID getFsUuid() {
        return _fsUuid;
    }

    public void setFsUuid(UUID value) {
        _fsUuid = value;
    }

    /**
     * physical address of this block (different for mirrors)
     */
    private long _physicalAddress;

    public long getPhysicalAddress() {
        return _physicalAddress;
    }

    public void setPhysicalAddress(long value) {
        _physicalAddress = value;
    }

    /**
     * flags
     */
    private long _flags;

    public long getFlags() {
        return _flags;
    }

    public void setFlags(long value) {
        _flags = value;
    }

    /**
     * magic ("_BHRfS_M")
     */
    private long _magic;

    public long getMagic() {
        return _magic;
    }

    public void setMagic(long value) {
        _magic = value;
    }

    /**
     * generation
     */
    private long _generation;

    public long getGeneration() {
        return _generation;
    }

    public void setGeneration(long value) {
        _generation = value;
    }

    /**
     * logical address of the root tree root
     */
    private long _root;

    public long getRoot() {
        return _root;
    }

    public void setRoot(long value) {
        _root = value;
    }

    /**
     * logical address of the chunk tree root
     */
    private long _chunkRoot;

    public long getChunkRoot() {
        return _chunkRoot;
    }

    public void setChunkRoot(long value) {
        _chunkRoot = value;
    }

    /**
     * logical address of the log tree root
     */
    private long _logRoot;

    public long getLogRoot() {
        return _logRoot;
    }

    public void setLogRoot(long value) {
        _logRoot = value;
    }

    /**
     * log_root_transid
     */
    private long _logRootTransId;

    public long getLogRootTransId() {
        return _logRootTransId;
    }

    public void setLogRootTransId(long value) {
        _logRootTransId = value;
    }

    /**
     * total_bytes
     */
    private long _totalBytes;

    public long getTotalBytes() {
        return _totalBytes;
    }

    public void setTotalBytes(long value) {
        _totalBytes = value;
    }

    /**
     * bytes_used
     */
    private long _bytesUsed;

    public long getBytesUsed() {
        return _bytesUsed;
    }

    public void setBytesUsed(long value) {
        _bytesUsed = value;
    }

    /**
     * root_dir_objectid (usually 6)
     */
    private long _rootDirObjectid;

    public long getRootDirObjectid() {
        return _rootDirObjectid;
    }

    public void setRootDirObjectid(long value) {
        _rootDirObjectid = value;
    }

    /**
     * num_devices
     */
    private long _numDevices;

    public long getNumDevices() {
        return _numDevices;
    }

    public void setNumDevices(long value) {
        _numDevices = value;
    }

    /**
     * sectorsize
     */
    private int _sectorSize;

    public int getSectorSize() {
        return _sectorSize;
    }

    public void setSectorSize(int value) {
        _sectorSize = value;
    }

    /**
     * nodesize
     */
    private int _nodeSize;

    public int getNodeSize() {
        return _nodeSize;
    }

    public void setNodeSize(int value) {
        _nodeSize = value;
    }

    /**
     * leafsize
     */
    private int _leafSize;

    public int getLeafSize() {
        return _leafSize;
    }

    public void setLeafSize(int value) {
        _leafSize = value;
    }

    /**
     * stripesize
     */
    private int _stripeSize;

    public int getStripeSize() {
        return _stripeSize;
    }

    public void setStripeSize(int value) {
        _stripeSize = value;
    }

    /**
     * chunk_root_generation
     */
    private long _chunkRootGeneration;

    public long getChunkRootGeneration() {
        return _chunkRootGeneration;
    }

    public void setChunkRootGeneration(long value) {
        _chunkRootGeneration = value;
    }

    /**
     * compat_flags
     */
    private long _compatFlags;

    public long getCompatFlags() {
        return _compatFlags;
    }

    public void setCompatFlags(long value) {
        _compatFlags = value;
    }

    /**
     * compat_ro_flags - only implementations that support the flags can write
     * to the filesystem
     */
    private long _compatRoFlags;

    public long getCompatRoFlags() {
        return _compatRoFlags;
    }

    public void setCompatRoFlags(long value) {
        _compatRoFlags = value;
    }

    /**
     * incompat_flags - only implementations that support the flags can use the
     * filesystem
     */
    private long _incompatFlags;

    public long getIncompatFlags() {
        return _incompatFlags;
    }

    public void setIncompatFlags(long value) {
        _incompatFlags = value;
    }

    /**
     * csum_type - Btrfs currently uses the CRC32c little-endian hash function
     * with seed -1.
     */
    private ChecksumType _checksumType = ChecksumType.Crc32C;

    public ChecksumType getChecksumType() {
        return _checksumType;
    }

    public void setChecksumType(ChecksumType value) {
        _checksumType = value;
    }

    /**
     * root_level
     */
    private byte _rootLevel;

    public int getRootLevel() {
        return _rootLevel & 0xff;
    }

    public void setRootLevel(byte value) {
        _rootLevel = value;
    }

    /**
     * chunk_root_level
     */
    private byte _chunkRootLevel;

    public int getChunkRootLevel() {
        return _chunkRootLevel & 0xff;
    }

    public void setChunkRootLevel(byte value) {
        _chunkRootLevel = value;
    }

    /**
     * log_root_level
     */
    private byte _logRootLevel;

    public int getLogRootLevel() {
        return _logRootLevel & 0xff;
    }

    public void setLogRootLevel(byte value) {
        _logRootLevel = value;
    }

    /**
     * label (may not contain '\\')
     */
    private String _label;

    public String getLabel() {
        return _label;
    }

    public void setLabel(String value) {
        _label = value;
    }

    private ChunkItem[] _systemChunkArray;

    public ChunkItem[] getSystemChunkArray() {
        return _systemChunkArray;
    }

    public void setSystemChunkArray(ChunkItem[] value) {
        _systemChunkArray = value;
    }

    public int size() {
        return Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        setMagic(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x40));
        if (getMagic() != BtrfsMagic)
            return size();

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
        setChecksumType(ChecksumType.values()[EndianUtilities.toUInt16LittleEndian(buffer, offset + 0xc4)]);
        setRootLevel(buffer[offset + 0xc6]);
        setChunkRootLevel(buffer[offset + 0xc7]);
        setLogRootLevel(buffer[offset + 0xc8]);
        // c9 62 DEV_ITEM data for this device
        byte[] labelData = EndianUtilities.toByteArray(buffer, offset + 0x12b, 0x100);
        OptionalInt eos = IntStream.range(0, labelData.length).filter(i -> labelData[i] == (byte) 0).findFirst();
        if (eos.isPresent()) {
            setLabel(new String(labelData, 0, eos.getAsInt(), StandardCharsets.UTF_8));
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
        setSystemChunkArray(systemChunks.toArray(new ChunkItem[0]));
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
