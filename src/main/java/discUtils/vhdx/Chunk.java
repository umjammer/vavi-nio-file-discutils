//
// Copyright (c) 2008-2012, Kenneth Bell
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

package discUtils.vhdx;

import discUtils.streams.SparseStream;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


/**
 * Represents a chunk of blocks in the block Allocation Table.
 *
 * The BAT entries for a chunk are always present in the BAT, but the data
 * blocks and
 * sector bitmap blocks may (or may not) be present.
 */
public final class Chunk {

    private static final long SectorBitmapPresent = 6;

    private final Stream bat;

    private final byte[] batData;

    private final int blocksPerChunk;

    private final int chunk;

    private final SparseStream file;

    private final FileParameters fileParameters;

    private final FreeSpaceTable freeSpace;

    private byte[] sectorBitmap;

    public Chunk(Stream bat,
            SparseStream file,
            FreeSpaceTable freeSpace,
            FileParameters fileParameters,
            int chunk,
            int blocksPerChunk) {
        this.bat = bat;
        this.file = file;
        this.freeSpace = freeSpace;
        this.fileParameters = fileParameters;
        this.chunk = chunk;
        this.blocksPerChunk = blocksPerChunk;
        this.bat.position((long) this.chunk * (this.blocksPerChunk + 1) * 8);
        batData = StreamUtilities.readExact(bat, (this.blocksPerChunk + 1) * 8);
    }

    private boolean getHasSectorBitmap() {
        return new BatEntry(batData, blocksPerChunk * 8).getBitmapBlockPresent();
    }

    private long getSectorBitmapPos() {
        return new BatEntry(batData, blocksPerChunk * 8).getFileOffsetMB() * Sizes.OneMiB;
    }

    private void setSectorBitmapPos(long value) {
        BatEntry entry = new BatEntry();
        entry.setBitmapBlockPresent(value != 0);
        entry.setFileOffsetMB(value / Sizes.OneMiB);
        entry.writeTo(batData, blocksPerChunk * 8);
    }

    public long getBlockPosition(int block) {
        return new BatEntry(batData, block * 8).getFileOffsetMB() * Sizes.OneMiB;
    }

    public PayloadBlockStatus getBlockStatus(int block) {
        // TODO batData are all zero
        return new BatEntry(batData, block * 8).getPayloadBlockStatus();
    }

    public BlockBitmap getBlockBitmap(int block) {
        int bytesPerBlock = (int) (Sizes.OneMiB / blocksPerChunk);
        int offset = bytesPerBlock * block;
        byte[] data = loadSectorBitmap();
        return new BlockBitmap(data, offset, bytesPerBlock);
    }

    public void writeBlockBitmap(int block) {
        int bytesPerBlock = (int) (Sizes.OneMiB / blocksPerChunk);
        int offset = bytesPerBlock * block;
        file.position(getSectorBitmapPos() + offset);
        file.write(sectorBitmap, offset, bytesPerBlock);
    }

    public PayloadBlockStatus allocateSpaceForBlock(int block) {
        boolean dataModified = false;

        BatEntry blockEntry = new BatEntry(batData, block * 8);
        if (blockEntry.getFileOffsetMB() == 0) {
            blockEntry.setFileOffsetMB(allocateSpace(fileParameters.blockSize, false) / Sizes.OneMiB);
            dataModified = true;
        }

        if (blockEntry.getPayloadBlockStatus() != PayloadBlockStatus.FullyPresent &&
            blockEntry.getPayloadBlockStatus() != PayloadBlockStatus.PartiallyPresent) {
            if (fileParameters.flags.contains(FileParametersFlags.HasParent)) {
                if (!getHasSectorBitmap()) {
                    setSectorBitmapPos(allocateSpace((int) Sizes.OneMiB, true));
                }

                blockEntry.setPayloadBlockStatus(PayloadBlockStatus.PartiallyPresent);
            } else {
                blockEntry.setPayloadBlockStatus(PayloadBlockStatus.FullyPresent);
            }

            dataModified = true;
        }

        if (dataModified) {
            blockEntry.writeTo(batData, block * 8);

            bat.position((long) chunk * (blocksPerChunk + 1) * 8);
            bat.write(batData, 0, (blocksPerChunk + 1) * 8);
        }

        return blockEntry.getPayloadBlockStatus();
    }

    private byte[] loadSectorBitmap() {
        if (sectorBitmap == null) {
            file.position(getSectorBitmapPos());
            sectorBitmap = StreamUtilities.readExact(file, (int) Sizes.OneMiB);
        }

        return sectorBitmap;
    }

    private long allocateSpace(int sizeBytes, boolean zero) {
        long[] pos = new long[1];
        if (!freeSpace.tryAllocate(sizeBytes, pos)) {
            pos[0] = MathUtilities.roundUp(file.getLength(), Sizes.OneMiB);
            file.setLength(pos[0] + sizeBytes);
            freeSpace.extendTo(pos[0] + sizeBytes, false);
        } else if (zero) {
            file.position(pos[0]);
            file.clear(sizeBytes);
        }

        return pos[0];
    }
}
