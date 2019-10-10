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

package DiscUtils.BootConfig;

import DiscUtils.Streams.Util.EndianUtilities;


public class IntegerElementValue extends ElementValue {
    private final long _value;

    public IntegerElementValue(byte[] value) {
        // Actual bytes stored may be less than 8
        byte[] buffer = new byte[8];
        System.arraycopy(value, 0, buffer, 0, value.length);
        _value = EndianUtilities.toUInt64LittleEndian(buffer, 0);
    }

    public IntegerElementValue(long value) {
        _value = value;
    }

    public ElementFormat getFormat() {
        return ElementFormat.Integer;
    }

    public String toString() {
        return String.valueOf(_value);
    }

    public byte[] getBytes() {
        byte[] bytes = new byte[8];
        EndianUtilities.writeBytesLittleEndian(_value, bytes, 0);
        return bytes;
    }
}
