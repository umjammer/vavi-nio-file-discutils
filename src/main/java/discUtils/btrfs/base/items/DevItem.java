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

package discUtils.btrfs.base.items;

import java.util.EnumSet;
import java.util.UUID;

import discUtils.btrfs.base.BlockGroupFlag;
import discUtils.btrfs.base.Key;
import discUtils.streams.util.EndianUtilities;


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
    private long deviceId;

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long value) {
        deviceId = value;
    }

    /**
     * size of the device
     */
    private long deviceSize;

    public long getDeviceSize() {
        return deviceSize;
    }

    public void setDeviceSize(long value) {
        deviceSize = value;
    }

    /**
     * number of bytes used
     */
    private long deviceSizeUsed;

    public long getDeviceSizeUsed() {
        return deviceSizeUsed;
    }

    public void setDeviceSizeUsed(long value) {
        deviceSizeUsed = value;
    }

    /**
     * optimal io alignment
     */
    private int optimalIoAlignment;

    public int getOptimalIoAlignment() {
        return optimalIoAlignment;
    }

    public void setOptimalIoAlignment(int value) {
        optimalIoAlignment = value;
    }

    /**
     * optimal io width
     */
    private int optimalIoWidth;

    public int getOptimalIoWidth() {
        return optimalIoWidth;
    }

    public void setOptimalIoWidth(int value) {
        optimalIoWidth = value;
    }

    /**
     * minimal io size (sector size)
     */
    private int minimalIoSize;

    public int getMinimalIoSize() {
        return minimalIoSize;
    }

    public void setMinimalIoSize(int value) {
        minimalIoSize = value;
    }

    /**
     * type and info about this device
     */
    private EnumSet<BlockGroupFlag> type;

    public EnumSet<BlockGroupFlag> getType() {
        return type;
    }

    public void setType(EnumSet<BlockGroupFlag> value) {
        type = value;
    }

    /**
     * expected generation for this device
     */
    private long generation;

    public long getGeneration() {
        return generation;
    }

    public void setGeneration(long value) {
        generation = value;
    }

    /**
     * starting byte of this partition on the device,
     * to allow for stripe alignment in the future
     */
    private long startOffset;

    public long getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(long value) {
        startOffset = value;
    }

    /**
     * grouping information for allocation decisions
     */
    private int devGroup;

    public int getDevGroup() {
        return devGroup;
    }

    public void setDevGroup(int value) {
        devGroup = value;
    }

    /**
    * seek speed 0-100 where 100 is fastest
    */
    private byte seekSpeed;

    public byte getSeekSpeed() {
        return seekSpeed;
    }

    public void setSeekSpeed(byte value) {
        seekSpeed = value;
    }

    /**
    * bandwidth 0-100 where 100 is fastest
    */
    private byte bandwidth;

    public byte getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(byte value) {
        bandwidth = value;
    }

    /**
     * btrfs generated uuid for this device
     */
    private UUID deviceUuid;

    public UUID getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(UUID value) {
        deviceUuid = value;
    }

    /**
     * uuid of FS who owns this device
     */
    private UUID fsUuid;

    public UUID getFsUuid() {
        return fsUuid;
    }

    public void setFsUuid(UUID value) {
        fsUuid = value;
    }

    public int size() {
        return Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        deviceId = EndianUtilities.toUInt64LittleEndian(buffer, offset);
        deviceSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x8);
        deviceSizeUsed = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x8);
        optimalIoAlignment = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x18);
        optimalIoWidth = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x1c);
        minimalIoSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x20);
        type = BlockGroupFlag.valueOf((int) EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x24));
        generation = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x2c);
        startOffset = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x34);
        devGroup = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x3c);
        seekSpeed = buffer[offset + 0x40];
        bandwidth = buffer[offset + 0x41];
        deviceUuid = EndianUtilities.toGuidLittleEndian(buffer, offset + 0x42);
        fsUuid = EndianUtilities.toGuidLittleEndian(buffer, offset + 0x52);
        return size();
    }
}
