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

import java.util.ArrayList;
import java.util.List;

import discUtils.streams.util.EndianUtilities;


public final class FragmentWriter {

    private final BuilderContext context;

    private final byte[] currentBlock;

    private int currentOffset;

    private final List<FragmentRecord> fragmentBlocks;

    public FragmentWriter(BuilderContext context) {
        this.context = context;
        currentBlock = new byte[context.getDataBlockSize()];
        currentOffset = 0;

        fragmentBlocks = new ArrayList<>();
    }

    private int fragmentCount;

    public int getFragmentCount() {
        return fragmentCount;
    }

    public void setFragmentCount(int value) {
        fragmentCount = value;
    }

    public void flush() {
        if (currentOffset != 0) {
            nextBlock();
        }
    }

    /**
     * @param offset {@cs out}
     */
    public int writeFragment(int length, int[] offset) {
        if (currentBlock.length - currentOffset < length) {
            nextBlock();
            currentOffset = 0;
        }

        offset[0] = currentOffset;
        System.arraycopy(context.getIoBuffer(), 0, currentBlock, currentOffset, length);
        currentOffset += length;

        ++fragmentCount;

        return fragmentBlocks.size();
    }

    public long persist() {
        if (fragmentBlocks.size() <= 0) {
            return -1;
        }

        if (fragmentBlocks.size() * FragmentRecord.RecordSize > context.getDataBlockSize()) {
            throw new UnsupportedOperationException("Large numbers of fragments");
        }

        // Persist the table that references the block containing the fragment records
        long blockPos = context.getRawStream().getPosition();
        int recordSize = FragmentRecord.RecordSize;
        byte[] buffer = new byte[fragmentBlocks.size() * recordSize];
        for (int i = 0; i < fragmentBlocks.size(); ++i) {
            fragmentBlocks.get(i).writeTo(buffer, i * recordSize);
        }

        MetablockWriter writer = new MetablockWriter();
        writer.write(buffer, 0, buffer.length);
        writer.persist(context.getRawStream());

        long tablePos = context.getRawStream().getPosition();
        byte[] tableBuffer = new byte[8];
        EndianUtilities.writeBytesLittleEndian(blockPos, tableBuffer, 0);
        context.getRawStream().write(tableBuffer, 0, 8);

        return tablePos;
    }

    private void nextBlock() {
        long position = context.getRawStream().getPosition();

        int writeLen = context.getWriteDataBlock().invoke(currentBlock, 0, currentOffset);
        FragmentRecord blockRecord = new FragmentRecord();
        blockRecord.startBlock = position;
        blockRecord.compressedSize = writeLen;

        fragmentBlocks.add(blockRecord);
    }
}
