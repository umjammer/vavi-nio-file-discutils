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
    private final BuilderContext _context;

    private final byte[] _currentBlock;

    private int _currentOffset;

    private final List<FragmentRecord> _fragmentBlocks;

    public FragmentWriter(BuilderContext context) {
        _context = context;
        _currentBlock = new byte[context.getDataBlockSize()];
        _currentOffset = 0;

        _fragmentBlocks = new ArrayList<>();
    }

    private int __FragmentCount;

    public int getFragmentCount() {
        return __FragmentCount;
    }

    public void setFragmentCount(int value) {
        __FragmentCount = value;
    }

    public void flush() {
        if (_currentOffset != 0) {
            nextBlock();
        }
    }

    /**
     * @param offset {@cs out}
     */
    public int writeFragment(int length, int[] offset) {
        if (_currentBlock.length - _currentOffset < length) {
            nextBlock();
            _currentOffset = 0;
        }

        offset[0] = _currentOffset;
        System.arraycopy(_context.getIoBuffer(), 0, _currentBlock, _currentOffset, length);
        _currentOffset += length;

        ++__FragmentCount;

        return _fragmentBlocks.size();
    }

    public long persist() {
        if (_fragmentBlocks.size() <= 0) {
            return -1;
        }

        if (_fragmentBlocks.size() * FragmentRecord.RecordSize > _context.getDataBlockSize()) {
            throw new UnsupportedOperationException("Large numbers of fragments");
        }

        // Persist the table that references the block containing the fragment records
        long blockPos = _context.getRawStream().getPosition();
        int recordSize = FragmentRecord.RecordSize;
        byte[] buffer = new byte[_fragmentBlocks.size() * recordSize];
        for (int i = 0; i < _fragmentBlocks.size(); ++i) {
            _fragmentBlocks.get(i).writeTo(buffer, i * recordSize);
        }

        MetablockWriter writer = new MetablockWriter();
        writer.write(buffer, 0, buffer.length);
        writer.persist(_context.getRawStream());

        long tablePos = _context.getRawStream().getPosition();
        byte[] tableBuffer = new byte[8];
        EndianUtilities.writeBytesLittleEndian(blockPos, tableBuffer, 0);
        _context.getRawStream().write(tableBuffer, 0, 8);

        return tablePos;
    }

    private void nextBlock() {
        long position = _context.getRawStream().getPosition();

        int writeLen = _context.getWriteDataBlock().invoke(_currentBlock, 0, _currentOffset);
        FragmentRecord blockRecord = new FragmentRecord();
        blockRecord.StartBlock = position;
        blockRecord.CompressedSize = writeLen;

        _fragmentBlocks.add(blockRecord);
    }
}
