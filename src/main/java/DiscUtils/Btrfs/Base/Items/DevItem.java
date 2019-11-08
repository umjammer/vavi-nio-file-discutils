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
    private long _deviceId;

    public long getDeviceId() {
        return _deviceId;
    }

    public void setDeviceId(long value) {
        _deviceId = value;
    }

    /**
     * size of the device
     */
    private long _deviceSize;

    public long getDeviceSize() {
        return _deviceSize;
    }

    public void setDeviceSize(long value) {
        _deviceSize = value;
    }

    /**
     * number of bytes used
     */
    private long _deviceSizeUsed;

    public long getDeviceSizeUsed() {
        return _deviceSizeUsed;
    }

    public void setDeviceSizeUsed(long value) {
        _deviceSizeUsed = value;
    }

    /**
     * optimal io alignment
     */
    private int _optimalIoAlignment;

    public int getOptimalIoAlignment() {
        return _optimalIoAlignment;
    }

    public void setOptimalIoAlignment(int value) {
        _optimalIoAlignment = value;
    }

    /**
     * optimal io width
     */
    private int _optimalIoWidth;

    public int getOptimalIoWidth() {
        return _optimalIoWidth;
    }

    public void setOptimalIoWidth(int value) {
        _optimalIoWidth = value;
    }

    /**
     * minimal io size (sector size)
     */
    private int _minimalIoSize;

    public int getMinimalIoSize() {
        return _minimalIoSize;
    }

    public void setMinimalIoSize(int value) {
        _minimalIoSize = value;
    }

    /**
     * type and info about this device
     */
    private EnumSet<BlockGroupFlag> _type;

    public EnumSet<BlockGroupFlag> getType() {
        return _type;
    }

    public void setType(EnumSet<BlockGroupFlag> value) {
        _type = value;
    }

    /**
     * expected generation for this device
     */
    private long _generation;

    public long getGeneration() {
        return _generation;
    }

    public void setGeneration(long value) {
        _generation = value;
    }

    /**
     * starting byte of this partition on the device,
     * to allow for stripe alignment in the future
     */
    private long _startOffset;

    public long getStartOffset() {
        return _startOffset;
    }

    public void setStartOffset(long value) {
        _startOffset = value;
    }

    /**
     * grouping information for allocation decisions
     */
    private int _devGroup;

    public int getDevGroup() {
        return _devGroup;
    }

    public void setDevGroup(int value) {
        _devGroup = value;
    }

    /**
    * seek speed 0-100 where 100 is fastest
    */
    private byte _seekSpeed;

    public byte getSeekSpeed() {
        return _seekSpeed;
    }

    public void setSeekSpeed(byte value) {
        _seekSpeed = value;
    }

    /**
    * bandwidth 0-100 where 100 is fastest
    */
    private byte _bandwidth;

    public byte getBandwidth() {
        return _bandwidth;
    }

    public void setBandwidth(byte value) {
        _bandwidth = value;
    }

    /**
     * btrfs generated uuid for this device
     */
    private UUID _deviceUuid;

    public UUID getDeviceUuid() {
        return _deviceUuid;
    }

    public void setDeviceUuid(UUID value) {
        _deviceUuid = value;
    }

    /**
     * uuid of FS who owns this device
     */
    private UUID _fsUuid;

    public UUID getFsUuid() {
        return _fsUuid;
    }

    public void setFsUuid(UUID value) {
        _fsUuid = value;
    }

    public int size() {
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
        return size();
    }
}
