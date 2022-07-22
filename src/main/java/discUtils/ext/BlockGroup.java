
package discUtils.ext;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


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

    public int size() {
        return DescriptorSize;
    }

    public int readFrom(byte[] buffer, int offset) {
        blockBitmapBlock = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
        inodeBitmapBlock = EndianUtilities.toUInt32LittleEndian(buffer, offset + 4);
        inodeTableBlock = EndianUtilities.toUInt32LittleEndian(buffer, offset + 8);
        freeBlocksCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 12);
        freeInodesCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 14);
        usedDirsCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 16);
        return DescriptorSize;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
