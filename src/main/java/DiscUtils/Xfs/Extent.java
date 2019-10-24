//
// Copyright (c) 2016, Bianco Veigel
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

package DiscUtils.Xfs;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.StreamUtilities;


public class Extent implements IByteArraySerializable {
    /**
     * Number of Blocks
     */
    private int __BlockCount;

    public int getBlockCount() {
        return __BlockCount;
    }

    public void setBlockCount(int value) {
        __BlockCount = value;
    }

    private long __StartBlock;

    public long getStartBlock() {
        return __StartBlock;
    }

    public void setStartBlock(long value) {
        __StartBlock = value;
    }

    private long __StartOffset;

    public long getStartOffset() {
        return __StartOffset;
    }

    public void setStartOffset(long value) {
        __StartOffset = value;
    }

    private ExtentFlag __Flag = ExtentFlag.Normal;

    public ExtentFlag getFlag() {
        return __Flag;
    }

    public void setFlag(ExtentFlag value) {
        __Flag = value;
    }

    public int sizeOf() {
        return 16;
    }

    public int readFrom(byte[] buffer, int offset) {
        long lower = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x8);
        long middle = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x6);
        long upper = EndianUtilities.toUInt64BigEndian(buffer, offset + 0);
        setBlockCount((int) (lower & 0x001FFFFF));
        setStartBlock((middle >>> 5) & 0x000FFFFFFFFFFFFFl);
        setStartOffset((upper >>> 9) & 0x003FFFFFFFFFFFFFl);
        setFlag(ExtentFlag.valueOf((buffer[offset + 0x0] >>> 6) & 0x3));
        return sizeOf();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public long getOffset(Context context) {
        return getOffset(context, getStartBlock());
    }

    public static long getOffset(Context context, long block) {
        long daddr = (block >>> context.getSuperBlock().getAgBlocksLog2()) * context.getSuperBlock().getAgBlocks() +
                     (block &
                      (1 << context.getSuperBlock().getAgBlocksLog2()) - 1) << (context.getSuperBlock().getBlocksizeLog2() - 9);
        return daddr * 512;
    }

    public byte[] getData(Context context) {
        return getData(context, 0, context.getSuperBlock().getBlocksize() * getBlockCount());
    }

    public byte[] getData(Context context, long offset, int count) {
        context.getRawStream().setPosition(getOffset(context) + offset);
        return StreamUtilities.readExact(context.getRawStream(), count);
    }

    /**
     *
     */
    public String toString() {
        return String.format("[%d,%d,%d,%s]", __StartOffset, __StartBlock, __BlockCount, __Flag);
    }
}
