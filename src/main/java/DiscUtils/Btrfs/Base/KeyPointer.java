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

package DiscUtils.Btrfs.Base;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class KeyPointer implements IByteArraySerializable {
    public static final int Length = 0x21;

    /**
     * key
     */
    private Key __Key;

    public Key getKey() {
        return __Key;
    }

    public void setKey(Key value) {
        __Key = value;
    }

    /**
     * block number
     */
    private long __BlockNumber;

    public long getBlockNumber() {
        return __BlockNumber;
    }

    public void setBlockNumber(long value) {
        __BlockNumber = value;
    }

    /**
     * generation
     */
    private long __Generation;

    public long getGeneration() {
        return __Generation;
    }

    public void setGeneration(long value) {
        __Generation = value;
    }

    public long getSize() {
        return Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        setKey(new Key());
        offset += getKey().readFrom(buffer, offset);
        setBlockNumber(EndianUtilities.toUInt64LittleEndian(buffer, offset));
        setGeneration(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x8));
        return (int) getSize();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
