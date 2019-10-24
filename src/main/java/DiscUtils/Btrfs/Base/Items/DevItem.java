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

package DiscUtils.Btrfs.Base.Items;

import java.util.EnumSet;
import java.util.UUID;

import DiscUtils.Btrfs.Base.BlockGroupFlag;
import DiscUtils.Btrfs.Base.Key;
import DiscUtils.Streams.Util.EndianUtilities;


/**
 * Maps logical address to physical
 */
public class DevItem extends BaseItem {
    public static final int Length = 0x62;

    public DevItem(Key key) {
        super(key);
    }

    /**
     * the internal btrfs device id
     */
    private long __DeviceId;

    public long getDeviceId() {
        return __DeviceId;
    }

    public void setDeviceId(long value) {
        __DeviceId = value;
    }

    /**
     * size of the device
     */
    private long __DeviceSize;

    public long getDeviceSize() {
        return __DeviceSize;
    }

    public void setDeviceSize(long value) {
        __DeviceSize = value;
    }

    /**
     * number of bytes used
     */
    private long __DeviceSizeUsed;

    public long getDeviceSizeUsed() {
        return __DeviceSizeUsed;
    }

    public void setDeviceSizeUsed(long value) {
        __DeviceSizeUsed = value;
    }

    /**
     * optimal io alignment
     */
    private int __OptimalIoAlignment;

    public int getOptimalIoAlignment() {
        return __OptimalIoAlignment;
    }

    public void setOptimalIoAlignment(int value) {
        __OptimalIoAlignment = value;
    }

    /**
     * optimal io width
     */
    private int __OptimalIoWidth;

    public int getOptimalIoWidth() {
        return __OptimalIoWidth;
    }

    public void setOptimalIoWidth(int value) {
        __OptimalIoWidth = value;
    }

    /**
     * minimal io size (sector size)
     */
    private int __MinimalIoSize;

    public int getMinimalIoSize() {
        return __MinimalIoSize;
    }

    public void setMinimalIoSize(int value) {
        __MinimalIoSize = value;
    }

    /**
     * type and info about this device
     */
    private EnumSet<BlockGroupFlag> __Type;

    public EnumSet<BlockGroupFlag> getType() {
        return __Type;
    }

    public void setType(EnumSet<BlockGroupFlag> value) {
        __Type = value;
    }

    /**
     * expected generation for this device
     */
    private long __Generation;

    public long getGeneration() {
        return __Generation;
    }

    public void setGeneration(long value) {
        __Generation = value;
    }

    /**
     * starting byte of this partition on the device,
     * to allow for stripe alignment in the future
     */
    private long __StartOffset;

    public long getStartOffset() {
        return __StartOffset;
    }

    public void setStartOffset(long value) {
        __StartOffset = value;
    }

    /**
     * grouping information for allocation decisions
     */
    private int __DevGroup;

    public int getDevGroup() {
        return __DevGroup;
    }

    public void setDevGroup(int value) {
        __DevGroup = value;
    }

    /**
    * seek speed 0-100 where 100 is fastest
    */
    private byte __SeekSpeed;

    public byte getSeekSpeed() {
        return __SeekSpeed;
    }

    public void setSeekSpeed(byte value) {
        __SeekSpeed = value;
    }

    /**
    * bandwidth 0-100 where 100 is fastest
    */
    private byte __Bandwidth;

    public byte getBandwidth() {
        return __Bandwidth;
    }

    public void setBandwidth(byte value) {
        __Bandwidth = value;
    }

    /**
     * btrfs generated uuid for this device
     */
    private UUID __DeviceUuid;

    public UUID getDeviceUuid() {
        return __DeviceUuid;
    }

    public void setDeviceUuid(UUID value) {
        __DeviceUuid = value;
    }

    /**
     * uuid of FS who owns this device
     */
    private UUID __FsUuid;

    public UUID getFsUuid() {
        return __FsUuid;
    }

    public void setFsUuid(UUID value) {
        __FsUuid = value;
    }

    public int sizeOf() {
        return Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        setDeviceId(EndianUtilities.toUInt64LittleEndian(buffer, offset));
        setDeviceSize(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x8));
        setDeviceSizeUsed(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x8));
        setOptimalIoAlignment(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x18));
        setOptimalIoWidth(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x1c));
        setMinimalIoSize(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x20));
        setType(BlockGroupFlag.valueOf((int) EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x24)));
        setGeneration(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x2c));
        setStartOffset(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x34));
        setDevGroup(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x3c));
        setSeekSpeed(buffer[offset + 0x40]);
        setBandwidth(buffer[offset + 0x41]);
        setDeviceUuid(EndianUtilities.toGuidLittleEndian(buffer, offset + 0x42));
        setFsUuid(EndianUtilities.toGuidLittleEndian(buffer, offset + 0x52));
        return sizeOf();
    }
}
