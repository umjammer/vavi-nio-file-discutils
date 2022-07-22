//
// Copyright (c) 2008-2011, Kenneth Bell
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

package discUtils.squashFs;

import discUtils.streams.util.EndianUtilities;


final class MetablockReader {

    private final Context context;

    private long currentBlockStart;

    private int currentOffset;

    private final long start;

    public MetablockReader(Context context, long start) {
        this.context = context;
        this.start = start;
    }

    public void setPosition(MetadataRef position) {
        setPosition(position.getBlock(), position.getOffset());
    }

    public void setPosition(long blockStart, int blockOffset) {
        if (blockOffset < 0 || blockOffset >= VfsSquashFileSystemReader.MetadataBufferSize) {
            throw new IndexOutOfBoundsException("Offset must be positive and less than block size");
        }

        currentBlockStart = blockStart;
        currentOffset = blockOffset;
    }

    public long distanceFrom(long blockStart, int blockOffset) {
        return (currentBlockStart - blockStart) * VfsSquashFileSystemReader.MetadataBufferSize +
               (currentOffset - blockOffset);
    }

    public void skip(int count) {
        Metablock block = context.getReadMetaBlock().invoke(start + currentBlockStart);

        int totalSkipped = 0;
        while (totalSkipped < count) {
            if (currentOffset >= block.getAvailable()) {
                int oldAvailable = block.getAvailable();
                block = context.getReadMetaBlock().invoke(block.getNextBlockStart());
                currentBlockStart = block.getPosition() - start;
                currentOffset -= oldAvailable;
            }

            int toSkip = Math.min(count - totalSkipped, block.getAvailable() - currentOffset);
            totalSkipped += toSkip;
            currentOffset += toSkip;
        }
    }

    public int read(byte[] buffer, int offset, int count) {
        Metablock block = context.getReadMetaBlock().invoke(start + currentBlockStart);

        int totalRead = 0;
        while (totalRead < count) {
            if (currentOffset >= block.getAvailable()) {
                int oldAvailable = block.getAvailable();
                block = context.getReadMetaBlock().invoke(block.getNextBlockStart());
                currentBlockStart = block.getPosition() - start;
                currentOffset -= oldAvailable;
            }

            int toRead = Math.min(count - totalRead, block.getAvailable() - currentOffset);
//Debug.println(currentOffset + ", " + offset + ", " + totalRead + ", " + toRead + ", " + count + ", " + block.getAvailable());
            System.arraycopy(block.getData(), currentOffset, buffer, offset + totalRead, toRead);
            totalRead += toRead;
            currentOffset += toRead;
        }

        return totalRead;
    }

    public int readUInt() {
        Metablock block = context.getReadMetaBlock().invoke(start + currentBlockStart);
        if (block.getAvailable() - currentOffset < 4) {
            byte[] buffer = new byte[4];
            read(buffer, 0, 4);
            return EndianUtilities.toUInt32LittleEndian(buffer, 0);
        }
        int result = EndianUtilities.toUInt32LittleEndian(block.getData(), currentOffset);
        currentOffset += 4;
        return result;
    }

    public int readInt() {
        Metablock block = context.getReadMetaBlock().invoke(start + currentBlockStart);
        if (block.getAvailable() - currentOffset < 4) {
            byte[] buffer = new byte[4];
            read(buffer, 0, 4);
            return EndianUtilities.toInt32LittleEndian(buffer, 0);
        }
        int result = EndianUtilities.toInt32LittleEndian(block.getData(), currentOffset);
        currentOffset += 4;
        return result;
    }

    public short readUShort() {
        Metablock block = context.getReadMetaBlock().invoke(start + currentBlockStart);
        if (block.getAvailable() - currentOffset < 2) {
            byte[] buffer = new byte[2];
            read(buffer, 0, 2);
            return EndianUtilities.toUInt16LittleEndian(buffer, 0);
        }
        short result = EndianUtilities.toUInt16LittleEndian(block.getData(), currentOffset);
        currentOffset += 2;
        return result;
    }

    public short readShort() {
        Metablock block = context.getReadMetaBlock().invoke(start + currentBlockStart);
        if (block.getAvailable() - currentOffset < 2) {
            byte[] buffer = new byte[2];
            read(buffer, 0, 2);
            return EndianUtilities.toInt16LittleEndian(buffer, 0);
        }
        short result = EndianUtilities.toInt16LittleEndian(block.getData(), currentOffset);
        currentOffset += 2;
        return result;
    }

    public String readString(int len) {
        Metablock block = context.getReadMetaBlock().invoke(start + currentBlockStart);
        if (block.getAvailable() - currentOffset < len) {
            byte[] buffer = new byte[len];
            read(buffer, 0, len);
            return EndianUtilities.bytesToString(buffer, 0, len);
        }
        String result = EndianUtilities.bytesToString(block.getData(), currentOffset, len);
        currentOffset += len;
        return result;
    }
}
