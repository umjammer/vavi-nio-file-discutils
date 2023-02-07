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

import java.util.ArrayList;
import java.util.List;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public class BlockDirectory implements IByteArraySerializable {

    private final Context context;

    public static final int HeaderMagic = 0x58443242;

    private int magic;

    public int getMagic() {
        return magic;
    }

    public void setMagic(int value) {
        magic = value;
    }

    private int leafCount;

    public int getLeafCount() {
        return leafCount;
    }

    public void setLeafCount(int value) {
        leafCount = value;
    }

    private int leafStale;

    public int getLeafStale() {
        return leafStale;
    }

    public void setLeafStale(int value) {
        leafStale = value;
    }

    private BlockDirectoryDataFree[] bestFree;

    public BlockDirectoryDataFree[] getBestFree() {
        return bestFree;
    }

    public void setBestFree(BlockDirectoryDataFree[] value) {
        bestFree = value;
    }

    private List<BlockDirectoryData> entries;

    public List<BlockDirectoryData> getEntries() {
        return entries;
    }

    public void setEntries(List<BlockDirectoryData> value) {
        entries = value;
    }

    @Override public int size() {
        return 16 + 3 * 32;
    }

    protected int readHeader(byte[] buffer, int offset) {
        setMagic(ByteUtil.readBeInt(buffer, offset));
        return 0x4;
    }

    protected int getHeaderPadding() {
        return 0;
    }

    public BlockDirectory(Context context) {
        this.context = context;
    }

    public boolean getHasValidMagic() {
        return magic == HeaderMagic;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        offset += readHeader(buffer, offset);
        bestFree = new BlockDirectoryDataFree[3];
        for (int i = 0; i < getBestFree().length; i++) {
            BlockDirectoryDataFree free = new BlockDirectoryDataFree();
            offset += free.readFrom(buffer, offset);
            getBestFree()[i] = free;
        }
        offset += getHeaderPadding();
        leafStale = ByteUtil.readBeInt(buffer, buffer.length - 0x4);
        leafCount = ByteUtil.readBeInt(buffer, buffer.length - 0x8);
        List<BlockDirectoryData> entries = new ArrayList<>();
        long eof = buffer.length - 0x8 - getLeafCount() * 0x8L;
        while (offset < eof) {
            BlockDirectoryData entry;
            if ((buffer[offset] & 0xff) == 0xff && (buffer[offset + 0x1] & 0xff) == 0xff) {
                // unused
                entry = new BlockDirectoryDataUnused();
            } else {
                entry = new BlockDirectoryDataEntry(context);
            }
            offset += entry.readFrom(buffer, offset);
            entries.add(entry);
        }
        this.entries = entries;
        return buffer.length - offset;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
