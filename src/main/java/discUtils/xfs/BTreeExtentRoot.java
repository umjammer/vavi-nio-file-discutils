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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;
import vavi.util.ByteUtil;


public class BTreeExtentRoot implements IByteArraySerializable {

    private short level;

    public short getLevel() {
        return level;
    }

    public void setLevel(short value) {
        level = value;
    }

    private short numberOfRecords;

    public int getNumberOfRecords() {
        return numberOfRecords & 0xffff;
    }

    public void setNumberOfRecords(short value) {
        numberOfRecords = value;
    }

    private long[] keys;

    public long[] getKeys() {
        return keys;
    }

    public void setKeys(long[] value) {
        keys = value;
    }

    private long[] pointer;

    public long[] getPointer() {
        return pointer;
    }

    public void setPointer(long[] value) {
        pointer = value;
    }

    private Map<Long, BTreeExtentHeader> children;

    public Map<Long, BTreeExtentHeader> getChildren() {
        return children;
    }

    public void setChildren(Map<Long, BTreeExtentHeader> value) {
        children = value;
    }

    @Override public int size() {
        return 4 + (0x9 * 0x16);
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        level = ByteUtil.readBeShort(buffer, offset);
        numberOfRecords = ByteUtil.readBeShort(buffer, offset + 0x2);
        offset += 0x4;
        keys = new long[getNumberOfRecords()];
        pointer = new long[getNumberOfRecords()];
        for (int i = 0; i < getNumberOfRecords(); i++) {
            keys[i] = ByteUtil.readBeLong(buffer, offset + i * 0x8);
        }
        offset += ((buffer.length - offset) / 16) * 8;
        for (int i = 0; i < getNumberOfRecords(); i++) {
            pointer[i] = ByteUtil.readBeLong(buffer, offset + i * 0x8);
        }
        return size();
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public void loadBtree(Context context) {
        children = new HashMap<>(getNumberOfRecords());
        for (int i = 0; i < getNumberOfRecords(); i++) {
            BTreeExtentHeader child;
            if (level == 1) {
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
            data.position(Extent.getOffset(context, pointer[i]));
            byte[] buffer = StreamUtilities.readExact(data, context.getSuperBlock().getBlocksize());
            child.readFrom(buffer, 0);
            if (context.getSuperBlock().getSbVersion() < 5 && child.getMagic() != BTreeExtentHeader.BtreeMagic ||
                context.getSuperBlock().getSbVersion() == 5 && child.getMagic() != BTreeExtentHeaderV5.BtreeMagicV5) {
                throw new IOException("invalid btree directory magic");
            }

            child.loadBtree(context);
            children.put(keys[i], child);
        }
    }

    public List<Extent> getExtents() {
        List<Extent> result = new ArrayList<>();
        for (Map.Entry<Long, BTreeExtentHeader> child : children.entrySet()) {
            result.addAll(child.getValue().getExtents());
        }
        return result;
    }
}
