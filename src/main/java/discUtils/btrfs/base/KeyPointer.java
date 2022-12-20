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

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public class KeyPointer implements IByteArraySerializable {

    public static final int Length = 0x21;

    /**
     * key
     */
    private Key key;

    public Key getKey() {
        return key;
    }

    public void setKey(Key value) {
        key = value;
    }

    /**
     * block number
     */
    private long blockNumber;

    public long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(long value) {
        blockNumber = value;
    }

    /**
     * generation
     */
    private long generation;

    public long getGeneration() {
        return generation;
    }

    public void setGeneration(long value) {
        generation = value;
    }

    @Override public int size() {
        return Length;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        setKey(new Key());
        offset += getKey().readFrom(buffer, offset);
        setBlockNumber(ByteUtil.readLeLong(buffer, offset));
        setGeneration(ByteUtil.readLeLong(buffer, offset + 0x8));
        return size();
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
