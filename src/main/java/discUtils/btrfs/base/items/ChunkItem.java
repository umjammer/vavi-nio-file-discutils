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

import discUtils.btrfs.base.BlockGroupFlag;
import discUtils.btrfs.base.Key;
import discUtils.btrfs.base.Stripe;
import discUtils.streams.util.EndianUtilities;


/**
 * Maps logical address to physical
 */
public class ChunkItem extends BaseItem {
    public ChunkItem(Key key) {
        super(key);
    }

    /**
     * size of chunk (bytes)
     */
    private long _chunkSize;

    public long getChunkSize() {
        return _chunkSize;
    }

    public void setChunkSize(long value) {
        _chunkSize = value;
    }

    /**
     * root referencing this chunk (2)
     */
    private long _objectId;

    public long getObjectId() {
        return _objectId;
    }

    public void setObjectId(long value) {
        _objectId = value;
    }

    /**
     * stripe length
     */
    private long _stripeLength;

    public long getStripeLength() {
        return _stripeLength;
    }

    public void setStripeLength(long value) {
        _stripeLength = value;
    }

    /**
     * type (same as flags for block group?)
     */
    private EnumSet<BlockGroupFlag> _type;

    public EnumSet<BlockGroupFlag> getType() {
        return _type;
    }

    public void setType(EnumSet<BlockGroupFlag> value) {
        _type = value;
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
     * number of stripes
     */
    private short _stripeCount;

    public int getStripeCount() {
        return _stripeCount & 0xffff;
    }

    public void setStripeCount(short value) {
        _stripeCount = value;
    }

    /**
     * sub stripes
     */
    private short _subStripes;

    public int getSubStripes() {
        return _subStripes & 0xffff;
    }

    public void setSubStripes(short value) {
        _subStripes = value;
    }

    private Stripe[] _stripes;

    public Stripe[] getStripes() {
        return _stripes;
    }

    public void setStripes(Stripe[] value) {
        _stripes = value;
    }

    public int size() {
        return 0x30 + getStripeCount() * Stripe.Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        setChunkSize(EndianUtilities.toUInt64LittleEndian(buffer, offset));
        setObjectId(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x8));
        setStripeLength(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x10));
        setType(BlockGroupFlag.valueOf((int) EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x18)));
        setOptimalIoAlignment(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x20));
        setOptimalIoWidth(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x24));
        setMinimalIoSize(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x28));
        setStripeCount(EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x2c));
        setSubStripes(EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x2e));
        setStripes(new Stripe[getStripeCount()]);
        offset += 0x30;
        for (int i = 0; i < getStripeCount(); i++) {
            getStripes()[i] = new Stripe();
            offset += getStripes()[i].readFrom(buffer, offset);
        }
        return size();
    }
}
