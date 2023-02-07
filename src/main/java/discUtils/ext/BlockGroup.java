
package discUtils.ext;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public class BlockGroup implements IByteArraySerializable {

    public static final int DescriptorSize = 32;

    public int blockBitmapBlock;

    private short freeBlocksCount;

    public int getFreeBlocksCount() {
        return freeBlocksCount & 0xffff;
    }

    public short freeInodesCount;

    public int inodeBitmapBlock;

    public int inodeTableBlock;

    public short usedDirsCount;

    @Override public int size() {
        return DescriptorSize;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        blockBitmapBlock = ByteUtil.readLeInt(buffer, offset + 0);
        inodeBitmapBlock = ByteUtil.readLeInt(buffer, offset + 4);
        inodeTableBlock = ByteUtil.readLeInt(buffer, offset + 8);
        freeBlocksCount = ByteUtil.readLeShort(buffer, offset + 12);
        freeInodesCount = ByteUtil.readLeShort(buffer, offset + 14);
        usedDirsCount = ByteUtil.readLeShort(buffer, offset + 16);
        return DescriptorSize;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
