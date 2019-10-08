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

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.IOException;
import moe.yo3explorer.dotnetio4j.Stream;


public class BTreeExtentRoot implements IByteArraySerializable {
    private short __Level;

    public short getLevel() {
        return __Level;
    }

    public void setLevel(short value) {
        __Level = value;
    }

    private short __NumberOfRecords;

    public short getNumberOfRecords() {
        return __NumberOfRecords;
    }

    public void setNumberOfRecords(short value) {
        __NumberOfRecords = value;
    }

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
        return 4 + (0x9 * 0x16);
    }

    public int readFrom(byte[] buffer, int offset) {
        setLevel(EndianUtilities.toUInt16BigEndian(buffer, offset));
        setNumberOfRecords(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x2));
        offset += 0x4;
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

    /**
     *
     */
    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public void loadBtree(Context context) {
        setChildren(new HashMap<>(getNumberOfRecords()));
        for (int i = 0; i < getNumberOfRecords(); i++) {
            BTreeExtentHeader child;
            if (getLevel() == 1) {
                if (context.getSuperBlock().getSbVersion() == 5)
                    child = new BTreeExtentLeafV5();
                else
                    child = new BTreeExtentLeaf();
            } else {
                if (context.getSuperBlock().getSbVersion() == 5)
                    child = new BTreeExtentNodeV5();
                else
                    child = new BTreeExtentNode();
            }
            Stream data = context.getRawStream();
            data.setPosition(Extent.getOffset(context, getPointer()[i]));
            byte[] buffer = StreamUtilities.readExact(data, context.getSuperBlock().getBlocksize());
            child.readFrom(buffer, 0);
            if (context.getSuperBlock().getSbVersion() < 5 && child.getMagic() != BTreeExtentHeader.BtreeMagic ||
                context.getSuperBlock().getSbVersion() == 5 && child.getMagic() != BTreeExtentHeaderV5.BtreeMagicV5) {
                throw new IOException("invalid btree directory magic");
            }

            child.loadBtree(context);
            getChildren().put(getKeys()[i], child);
        }
    }

    public List<Extent> getExtents() {
        List<Extent> result = new ArrayList<>();
        for (Map.Entry<Long, BTreeExtentHeader> child : getChildren().entrySet()) {
            result.addAll(child.getValue().getExtents());
        }
        return result;
    }
}
