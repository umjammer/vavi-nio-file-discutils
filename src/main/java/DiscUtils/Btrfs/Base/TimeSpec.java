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

import java.time.Instant;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;

public class TimeSpec implements IByteArraySerializable {
    public static final int Length = 0xc;

    /**
    * Number of seconds since 1970-01-01T00:00:00Z.
    */
    private long __Seconds;

    public long getSeconds() {
        return __Seconds;
    }

    public void setSeconds(long value) {
        __Seconds = value;
    }

    /**
     * Number of nanoseconds since the beginning of the second.
     */
    private int __Nanoseconds;

    public int getNanoseconds() {
        return __Nanoseconds;
    }

    public void setNanoseconds(int value) {
        __Nanoseconds = value;
    }

    public long getValue() {
        return Instant.ofEpochSecond(getSeconds(), getNanoseconds()).toEpochMilli();
    }

    public long getSize() {
        return Length;
    }

    public long getDateTime() {
        return Instant.ofEpochSecond(getSeconds(), getNanoseconds()).toEpochMilli();
    }

    public int readFrom(byte[] buffer, int offset) {
        setSeconds(EndianUtilities.toInt64LittleEndian(buffer, offset));
        setNanoseconds(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x8));
        return (int) getSize();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
