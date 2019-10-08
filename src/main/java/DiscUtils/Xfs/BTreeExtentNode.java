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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.IOException;
import moe.yo3explorer.dotnetio4j.Stream;


public class BTreeExtentNode extends BTreeExtentHeader {
    private long[] __Keys;

    public long[] getKeys() {
        return __Keys;
    }

    public void setKeys(long[] value) {
        __Keys = value;
    }

    private long[] __Pointer;

    public long[] getPointer() {
        return __Pointer;
    }

    public void setPointer(long[] value) {
        __Pointer = value;
    }

    private Map<Long, BTreeExtentHeader> __Children;

    public Map<Long, BTreeExtentHeader> getChildren() {
        return __Children;
    }

    public void setChildren(Map<Long, BTreeExtentHeader> value) {
        __Children = value;
    }

    public long getSize() {
        return super.getSize() + (getNumberOfRecords() * 0x8);
    }

    public int readFrom(byte[] buffer, int offset) {
        offset += super.readFrom(buffer, offset);
        if (getLevel() == 0)
            throw new IOException("invalid B+tree level - expected >= 1");

        setKeys(new long[getNumberOfRecords()]);
        setPointer(new long[getNumberOfRecords()]);
        for (int i = 0; i < getNumberOfRecords(); i++) {
            getKeys()[i] = EndianUtilities.toUInt64BigEndian(buffer, offset + i * 0x8);
        }
        offset += ((buffer.length - offset) / 16) * 8;
        for (int i = 0; i < getNumberOfRecords(); i++) {
            getPointer()[i] = EndianUtilities.toUInt64BigEndian(buffer, offset + i * 0x8);
        }
        return (int) getSize();
    }

    public void loadBtree(Context context) {
        setChildren(new HashMap<Long, BTreeExtentHeader>(getNumberOfRecords()));
        for (int i = 0; i < getNumberOfRecords(); i++) {
            BTreeExtentHeader child;
            if (getLevel() == 1) {
                child = new BTreeExtentLeaf();
            } else {
                child = new BTreeExtentNode();
            }
            Stream data = context.getRawStream();
            data.setPosition(Extent.getOffset(context, getPointer()[i]));
            byte[] buffer = StreamUtilities.readExact(data, context.getSuperBlock().getBlocksize());
            child.readFrom(buffer, 0);
            if (child.getMagic() != BtreeMagic) {
                throw new IOException("invalid btree directory magic");
            }

            child.loadBtree(context);
            getChildren().put(getKeys()[i], child);
        }
    }

    /**
     *
     */
    public List<Extent> getExtents() {
        List<Extent> result = new ArrayList<>();
        for (Map.Entry<Long, BTreeExtentHeader> child : getChildren().entrySet()) {
            result.addAll(child.getValue().getExtents());
        }
        return result;
    }
}
