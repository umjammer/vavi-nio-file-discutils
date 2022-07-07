
package discUtils.ext;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public class BlockGroup implements IByteArraySerializable {
    public static final int DescriptorSize = 32;

    public int BlockBitmapBlock;

    private short freeBlocksCount;

    public int getFreeBlocksCount() {
        return freeBlocksCount & 0xffff;
    }

    public short FreeInodesCount;

    public int InodeBitmapBlock;

    public int InodeTableBlock;

    public short UsedDirsCount;

    public int size() {
        return DescriptorSize;
    }

    public int readFrom(byte[] buffer, int offset) {
        BlockBitmapBlock = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
        InodeBitmapBlock = EndianUtilities.toUInt32LittleEndian(buffer, offset + 4);
        InodeTableBlock = EndianUtilities.toUInt32LittleEndian(buffer, offset + 8);
        freeBlocksCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 12);
        FreeInodesCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 14);
        UsedDirsCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 16);
        return DescriptorSize;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
