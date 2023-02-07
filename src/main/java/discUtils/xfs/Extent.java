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

package discUtils.xfs;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.StreamUtilities;
import vavi.util.ByteUtil;


public class Extent implements IByteArraySerializable {

    /**
     * Number of Blocks
     */
    private int blockCount;

    public int getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(int value) {
        blockCount = value;
    }

    private long startBlock;

    public long getStartBlock() {
        return startBlock;
    }

    public void setStartBlock(long value) {
        startBlock = value;
    }

    private long startOffset;

    public long getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(long value) {
        startOffset = value;
    }

    private ExtentFlag flag = ExtentFlag.Normal;

    public ExtentFlag getFlag() {
        return flag;
    }

    public void setFlag(ExtentFlag value) {
        flag = value;
    }

    @Override public int size() {
        return 16;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        long lower = ByteUtil.readBeLong(buffer, offset + 0x8);
        long middle = ByteUtil.readBeLong(buffer, offset + 0x6);
        long upper = ByteUtil.readBeLong(buffer, offset + 0);
        blockCount = (int) (lower & 0x001F_FFFF);
        startBlock = (middle >>> 5) & 0x000F_FFFF_FFFF_FFFFL;
        startOffset = (upper >>> 9) & 0x003F_FFFF_FFFF_FFFFL;
        flag = ExtentFlag.values()[(buffer[offset + 0x0] >>> 6) & 0x3];
        return size();
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public long getOffset(Context context) {
        return getOffset(context, getStartBlock());
    }

    public static long getOffset(Context context, long block) {
        long dAddr = (block >>> context.getSuperBlock().getAgBlocksLog2()) * context.getSuperBlock().getAgBlocks() +
                     (block &
                      (1L << context.getSuperBlock().getAgBlocksLog2()) - 1) << (context.getSuperBlock().getBlocksizeLog2() - 9);
        return dAddr * 512;
    }

    public byte[] getData(Context context) {
        return getData(context, 0, context.getSuperBlock().getBlocksize() * getBlockCount());
    }

    public byte[] getData(Context context, long offset, int count) {
        context.getRawStream().position(getOffset(context) + offset);
        return StreamUtilities.readExact(context.getRawStream(), count);
    }

    /**
     *
     */
    @Override public String toString() {
        return String.format("[%d,%d,%d,%s]", startOffset, startBlock, blockCount, flag);
    }
}
