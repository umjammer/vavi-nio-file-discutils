//
// Copyright (c) 2016, Bianco Veigel
// Copyright (c) 2017, Timo Walter
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

import java.util.HashMap;
import java.util.Map;

import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


public class BTreeInodeNode extends BtreeHeader {

    private int[] keys;

    public int[] getKeys() {
        return keys;
    }

    public void setKeys(int[] value) {
        keys = value;
    }

    private int[] pointer;

    public int[] getPointer() {
        return pointer;
    }

    public void setPointer(int[] value) {
        pointer = value;
    }

    private Map<Integer, BtreeHeader> children;

    public Map<Integer, BtreeHeader> getChildren() {
        return children;
    }

    public void setChildren(Map<Integer, BtreeHeader> value) {
        children = value;
    }

    public int size() {
        return super.size() + (getNumberOfRecords() * 0x8);
    }

    public BTreeInodeNode(int superBlockVersion) {
        super(superBlockVersion);
    }

    public int readFrom(byte[] buffer, int offset) {
        super.readFrom(buffer, offset);
        offset += super.size();
        if (getLevel() == 0)
            throw new IOException("invalid B+tree level - expected 0");

        keys = new int[getNumberOfRecords()];
        pointer = new int[getNumberOfRecords()];
        for (int i = 0; i < getNumberOfRecords(); i++) {
            keys[i] = EndianUtilities.toUInt32BigEndian(buffer, offset);
        }
        for (int i = 0; i < getNumberOfRecords(); i++) {
            pointer[i] = EndianUtilities.toUInt32BigEndian(buffer, offset);
        }
        return size();
    }

    public void loadBtree(AllocationGroup ag) {
        children = new HashMap<>(getNumberOfRecords());
        for (int i = 0; i < getNumberOfRecords(); i++) {
            BtreeHeader child;
            if (getLevel() == 1) {
                child = new BTreeInodeLeaf(super.getSbVersion());
            } else {
                child = new BTreeInodeNode(super.getSbVersion());
            }
            Stream data = ag.getContext().getRawStream();
            data.position(((long) getPointer()[i] * ag.getContext().getSuperBlock().getBlocksize()) + ag.getOffset());
            byte[] buffer = StreamUtilities.readExact(data, ag.getContext().getSuperBlock().getBlocksize());
            child.readFrom(buffer, 0);
            child.loadBtree(ag);
            children.put(getKeys()[i], child);
        }
    }
}
