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

package DiscUtils.HfsPlus;

import DiscUtils.Streams.Util.EndianUtilities;


public final class BTreeIndexRecord<TKey extends BTreeKey<?>> extends BTreeNodeRecord {
    private final int _size;
    Class<TKey> clazz;
    public BTreeIndexRecord(Class<TKey> clazz, int size) {
        _size = size;
        this.clazz = clazz;
    }

    private int __ChildId;

    public int getChildId() {
        return __ChildId;
    }

    public void setChildId(int value) {
        __ChildId = value;
    }

    private TKey __Key;

    public TKey getKey() {
        return __Key;
    }

    public void setKey(TKey value) {
        __Key = value;
    }

    public int size() {
        return _size;
    }

    public int readFrom(byte[] buffer, int offset) {
        try {
            setKey(clazz.newInstance());
            int keySize = getKey().readFrom(buffer, offset);
            if ((keySize & 1) != 0) {
                ++keySize;
            }

            setChildId(EndianUtilities.toUInt32BigEndian(buffer, offset + keySize));
            return _size;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        return getKey() + ":" + getChildId();
    }
}
