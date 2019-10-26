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

import DiscUtils.Btrfs.Base.BlockGroupFlag;
import DiscUtils.Btrfs.Base.Key;
import DiscUtils.Btrfs.Base.Stripe;
import DiscUtils.Streams.Util.EndianUtilities;


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
    private long __ChunkSize;

    public long getChunkSize() {
        return __ChunkSize;
    }

    public void setChunkSize(long value) {
        __ChunkSize = value;
    }

    /**
     * root referencing this chunk (2)
     */
    private long __ObjectId;

    public long getObjectId() {
        return __ObjectId;
    }

    public void setObjectId(long value) {
        __ObjectId = value;
    }

    /**
     * stripe length
     */
    private long __StripeLength;

    public long getStripeLength() {
        return __StripeLength;
    }

    public void setStripeLength(long value) {
        __StripeLength = value;
    }

    /**
     * type (same as flags for block group?)
     */
    private EnumSet<BlockGroupFlag> __Type;

    public EnumSet<BlockGroupFlag> getType() {
        return __Type;
    }

    public void setType(EnumSet<BlockGroupFlag> value) {
        __Type = value;
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
     * number of stripes
     */
    private short __StripeCount;

    public short getStripeCount() {
        return __StripeCount;
    }

    public void setStripeCount(short value) {
        __StripeCount = value;
    }

    /**
     * sub stripes
     */
    private short __SubStripes;

    public short getSubStripes() {
        return __SubStripes;
    }

    public void setSubStripes(short value) {
        __SubStripes = value;
    }

    private Stripe[] __Stripes;

    public Stripe[] getStripes() {
        return __Stripes;
    }

    public void setStripes(Stripe[] value) {
        __Stripes = value;
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
