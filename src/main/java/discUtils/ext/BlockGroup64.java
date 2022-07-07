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

package discUtils.ext;

import discUtils.streams.util.EndianUtilities;


public class BlockGroup64 extends BlockGroup {
    private int _descriptorSize;

    public int BlockBitmapBlockHigh;

    public int InodeBitmapBlockHigh;

    public int InodeTableBlockHigh;

    public short freeBlocksCountHigh;

    public int getFreeBlocksCountHigh() {
        return freeBlocksCountHigh & 0xffff;
    }

    public short FreeInodesCountHigh;

    public short UsedDirsCountHigh;

    public BlockGroup64(int descriptorSize) {
        this._descriptorSize = descriptorSize;
    }

    public int size() {
        return this._descriptorSize;
    }

    public int readFrom(byte[] buffer, int offset) {
        super.readFrom(buffer, offset);
        BlockBitmapBlockHigh = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x20);
        InodeBitmapBlockHigh = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x24);
        InodeTableBlockHigh = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x28);
        freeBlocksCountHigh = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x2C);
        FreeInodesCountHigh = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x2E);
        UsedDirsCountHigh = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x30);
        return this._descriptorSize;
    }
}
