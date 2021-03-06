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

import java.util.Arrays;


public final class BTreeLeafRecord<TKey extends BTreeKey<?>> extends BTreeNodeRecord {
    private final int _size;

    Class<TKey> clazz;

    public BTreeLeafRecord(Class<TKey> clazz, int size) {
        _size = size;
        this.clazz = clazz;
    }

    private byte[] _data;

    public byte[] getData() {
        return _data;
    }

    public void setData(byte[] value) {
        _data = value;
    }

    private TKey _key;

    public TKey getKey() {
        return _key;
    }

    public void setKey(TKey value) {
        _key = value;
    }

    public int size() {
        return _size;
    }

    public int readFrom(byte[] buffer, int offset) {
        try {
            _key = clazz.newInstance();
            int keySize = _key.readFrom(buffer, offset);

            if ((keySize & 1) != 0) {
                ++keySize;
            }

            _data = new byte[_size - keySize];
            System.arraycopy(buffer, offset + keySize, _data, 0, getData().length);

            return _size;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        return _key + ":" + Arrays.toString(_data);
    }
}
