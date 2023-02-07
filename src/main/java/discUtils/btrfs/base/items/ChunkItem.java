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
import vavi.util.ByteUtil;


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
    private long chunkSize;

    public long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(long value) {
        chunkSize = value;
    }

    /**
     * root referencing this chunk (2)
     */
    private long objectId;

    public long getObjectId() {
        return objectId;
    }

    public void setObjectId(long value) {
        objectId = value;
    }

    /**
     * stripe length
     */
    private long stripeLength;

    public long getStripeLength() {
        return stripeLength;
    }

    public void setStripeLength(long value) {
        stripeLength = value;
    }

    /**
     * type (same as flags for block group?)
     */
    private EnumSet<BlockGroupFlag> type;

    public EnumSet<BlockGroupFlag> getType() {
        return type;
    }

    public void setType(EnumSet<BlockGroupFlag> value) {
        type = value;
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
     * number of stripes
     */
    private short stripeCount;

    public int getStripeCount() {
        return stripeCount & 0xffff;
    }

    public void setStripeCount(short value) {
        stripeCount = value;
    }

    /**
     * sub stripes
     */
    private short subStripes;

    public int getSubStripes() {
        return subStripes & 0xffff;
    }

    public void setSubStripes(short value) {
        subStripes = value;
    }

    private Stripe[] stripes;

    public Stripe[] getStripes() {
        return stripes;
    }

    public void setStripes(Stripe[] value) {
        stripes = value;
    }

    @Override public int size() {
        return 0x30 + getStripeCount() * Stripe.Length;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        chunkSize = ByteUtil.readLeLong(buffer, offset);
        objectId = ByteUtil.readLeLong(buffer, offset + 0x8);
        stripeLength = ByteUtil.readLeLong(buffer, offset + 0x10);
        type = BlockGroupFlag.valueOf((int) ByteUtil.readLeLong(buffer, offset + 0x18));
        optimalIoAlignment = ByteUtil.readLeInt(buffer, offset + 0x20);
        optimalIoWidth = ByteUtil.readLeInt(buffer, offset + 0x24);
        minimalIoSize = ByteUtil.readLeInt(buffer, offset + 0x28);
        stripeCount = ByteUtil.readLeShort(buffer, offset + 0x2c);
        subStripes = ByteUtil.readLeShort(buffer, offset + 0x2e);
        stripes = new Stripe[getStripeCount()];
        offset += 0x30;
        for (int i = 0; i < getStripeCount(); i++) {
            stripes[i] = new Stripe();
            offset += stripes[i].readFrom(buffer, offset);
        }
        return size();
    }
}
