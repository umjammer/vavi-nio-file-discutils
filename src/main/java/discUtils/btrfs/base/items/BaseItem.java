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

package discUtils.btrfs.base.items;

import discUtils.btrfs.base.Key;
import discUtils.streams.IByteArraySerializable;


/**
 * Maps logical address to physical
 */
public abstract class BaseItem implements IByteArraySerializable {

    public BaseItem(Key key) {
        this.key = key;
    }

    private Key key;

    public Key getKey() {
        return key;
    }

    public void setKey(Key value) {
        key = value;
    }

    @Override public abstract int size();

    @Override public abstract int readFrom(byte[] buffer, int offset);

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
