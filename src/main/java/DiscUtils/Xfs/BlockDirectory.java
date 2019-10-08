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

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class BlockDirectory implements IByteArraySerializable {
    private final Context _context;

    public static final int HeaderMagic = 0x58443242;

    private int __Magic;

    public int getMagic() {
        return __Magic;
    }

    public void setMagic(int value) {
        __Magic = value;
    }

    private int __LeafCount;

    public int getLeafCount() {
        return __LeafCount;
    }

    public void setLeafCount(int value) {
        __LeafCount = value;
    }

    private int __LeafStale;

    public int getLeafStale() {
        return __LeafStale;
    }

    public void setLeafStale(int value) {
        __LeafStale = value;
    }

    private BlockDirectoryDataFree[] __BestFree;

    public BlockDirectoryDataFree[] getBestFree() {
        return __BestFree;
    }

    public void setBestFree(BlockDirectoryDataFree[] value) {
        __BestFree = value;
    }

    private List<BlockDirectoryData> __Entries;

    public List<BlockDirectoryData> getEntries() {
        return __Entries;
    }

    public void setEntries(List<BlockDirectoryData> value) {
        __Entries = value;
    }

    public long getSize() {
        return 16 + 3 * 32;
    }

    protected int readHeader(byte[] buffer, int offset) {
        setMagic(EndianUtilities.toUInt32BigEndian(buffer, offset));
        return 0x4;
    }

    protected int getHeaderPadding() {
        return 0;
    }

    public BlockDirectory(Context context) {
        _context = context;
    }

    public boolean getHasValidMagic() {
        return getMagic() == HeaderMagic;
    }

    public int readFrom(byte[] buffer, int offset) {
        offset += readHeader(buffer, offset);
        setBestFree(new BlockDirectoryDataFree[3]);
        for (int i = 0; i < getBestFree().length; i++) {
            BlockDirectoryDataFree free = new BlockDirectoryDataFree();
            offset += free.readFrom(buffer, offset);
            getBestFree()[i] = free;
        }
        offset += getHeaderPadding();
        setLeafStale(EndianUtilities.toUInt32BigEndian(buffer, buffer.length - 0x4));
        setLeafCount(EndianUtilities.toUInt32BigEndian(buffer, buffer.length - 0x8));
        List<BlockDirectoryData> entries = new ArrayList<>();
        long eof = buffer.length - 0x8 - getLeafCount() * 0x8;
        while (offset < eof) {
            BlockDirectoryData entry;
            if (buffer[offset] == 0xff && buffer[offset + 0x1] == 0xff) {
                //unused
                entry = new BlockDirectoryDataUnused();
            } else {
                entry = new BlockDirectoryDataEntry(_context);
            }
            offset += entry.readFrom(buffer, offset);
            entries.add(entry);
        }
        setEntries(entries);
        return buffer.length - offset;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
