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

package DiscUtils.Xfs;

import java.util.HashMap;
import java.util.Map;

import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


public class BTreeInodeNode extends BtreeHeader {
    private int[] __Keys;

    public int[] getKeys() {
        return __Keys;
    }

    public void setKeys(int[] value) {
        __Keys = value;
    }

    private int[] __Pointer;

    public int[] getPointer() {
        return __Pointer;
    }

    public void setPointer(int[] value) {
        __Pointer = value;
    }

    private Map<Integer, BtreeHeader> __Children;

    public Map<Integer, BtreeHeader> getChildren() {
        return __Children;
    }

    public void setChildren(Map<Integer, BtreeHeader> value) {
        __Children = value;
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

        setKeys(new int[getNumberOfRecords()]);
        setPointer(new int[getNumberOfRecords()]);
        for (int i = 0; i < getNumberOfRecords(); i++) {
            getKeys()[i] = EndianUtilities.toUInt32BigEndian(buffer, offset);
        }
        for (int i = 0; i < getNumberOfRecords(); i++) {
            getPointer()[i] = EndianUtilities.toUInt32BigEndian(buffer, offset);
        }
        return size();
    }

    public void loadBtree(AllocationGroup ag) {
        setChildren(new HashMap<>(getNumberOfRecords()));
        for (int i = 0; i < getNumberOfRecords(); i++) {
            BtreeHeader child;
            if (getLevel() == 1) {
                child = new BTreeInodeLeaf(super.getSbVersion());
            } else {
                child = new BTreeInodeNode(super.getSbVersion());
            }
            Stream data = ag.getContext().getRawStream();
            data.setPosition(((long) getPointer()[i] * ag.getContext().getSuperBlock().getBlocksize()) + ag.getOffset());
            byte[] buffer = StreamUtilities.readExact(data, ag.getContext().getSuperBlock().getBlocksize());
            child.readFrom(buffer, 0);
            child.loadBtree(ag);
            getChildren().put(getKeys()[i], child);
        }
    }
}
