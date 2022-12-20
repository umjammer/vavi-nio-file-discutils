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

import java.time.Instant;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public class TimeSpec implements IByteArraySerializable {

    public static final int Length = 0xc;

    /**
    * Number of seconds since 1970-01-01T00:00:00Z.
    */
    private long seconds;

    public long getSeconds() {
        return seconds;
    }

    public void setSeconds(long value) {
        seconds = value;
    }

    /**
     * Number of nanoseconds since the beginning of the second.
     */
    private int nanoseconds;

    public int getNanoseconds() {
        return nanoseconds;
    }

    public void setNanoseconds(int value) {
        nanoseconds = value;
    }

    public long getValue() {
        return Instant.ofEpochSecond(seconds, nanoseconds).toEpochMilli();
    }

    @Override public int size() {
        return Length;
    }

    public long getDateTime() {
        return Instant.ofEpochSecond(seconds, nanoseconds).toEpochMilli();
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        seconds = ByteUtil.readLeLong(buffer, offset);
        nanoseconds = ByteUtil.readLeInt(buffer, offset + 0x8);
        return size();
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
