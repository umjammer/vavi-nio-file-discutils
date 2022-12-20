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

package discUtils.hfsPlus;

import java.lang.reflect.InvocationTargetException;

import vavi.util.ByteUtil;


final class BTreeIndexRecord<TKey extends BTreeKey<?>> extends BTreeNodeRecord {

    private final int size;

    private Class<TKey> clazz;

    public BTreeIndexRecord(Class<TKey> clazz, int size) {
        this.size = size;
        this.clazz = clazz;
    }

    private int childId;

    public int getChildId() {
        return childId;
    }

    public void setChildId(int value) {
        childId = value;
    }

    private TKey key;

    public TKey getKey() {
        return key;
    }

    public void setKey(TKey value) {
        key = value;
    }

    @Override public int size() {
        return size;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        try {
            setKey(clazz.getDeclaredConstructor().newInstance());
            int keySize = getKey().readFrom(buffer, offset);

            if ((keySize & 1) != 0) {
                ++keySize;
            }

            setChildId(ByteUtil.readBeInt(buffer, offset + keySize));

            return size;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override public String toString() {
        return key + ":" + childId;
    }
}
