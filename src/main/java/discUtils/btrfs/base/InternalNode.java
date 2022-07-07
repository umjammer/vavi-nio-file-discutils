//
// Copyright (c) 2017, Bianco Veigel
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

package discUtils.btrfs.base;

import java.util.ArrayList;
import java.util.List;

import discUtils.btrfs.Context;
import discUtils.btrfs.base.items.BaseItem;
import dotnet4j.io.IOException;


public class InternalNode extends NodeHeader {
    /**
     * key pointers
     */
    private KeyPointer[] _keyPointers;

    public KeyPointer[] getKeyPointers() {
        return _keyPointers;
    }

    public void setKeyPointers(KeyPointer[] value) {
        _keyPointers = value;
    }

    /**
     * data at {@link #_keyPointers}
     */
    private NodeHeader[] _nodes;

    public NodeHeader[] getNodes() {
        return _nodes;
    }

    public void setNodes(NodeHeader[] value) {
        _nodes = value;
    }

    public int size() {
        return super.size() + getItemCount() * KeyPointer.Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        offset += super.readFrom(buffer, offset);
        setKeyPointers(new KeyPointer[getItemCount()]);
        if (getKeyPointers().length == 0)
            throw new IOException("invalid InteralNode without KeyPointers");

        for (int i = 0; i < getItemCount(); i++) {
            getKeyPointers()[i] = new KeyPointer();
            offset += getKeyPointers()[i].readFrom(buffer, offset);
        }
        setNodes(new NodeHeader[getItemCount()]);
        return size();
    }

    public List<BaseItem> find(Key key, Context context) {
        List<BaseItem> result = new ArrayList<>();
        if (getKeyPointers()[0].getKey().getObjectId() > key.getObjectId())
            return result;

        int i = 1;
        while (i < getKeyPointers().length && getKeyPointers()[i].getKey().getObjectId() < key.getObjectId()) {
            i++;
        }
        for (int j = i - 1; j < getKeyPointers().length; j++) {
            KeyPointer keyPtr = getKeyPointers()[j];
            if (keyPtr.getKey().getObjectId() > key.getObjectId())
                return result;

            if (getNodes()[j] == null)
                getNodes()[j] = context.readTree(keyPtr.getBlockNumber(), getLevel());

            result.addAll(getNodes()[j].find(key, context));
        }
        return result;
    }
}
