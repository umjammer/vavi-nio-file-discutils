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

import DiscUtils.BootConfig.ElementFormat;
import DiscUtils.BootConfig.ElementValue;
import DiscUtils.Streams.Util.EndianUtilities;


public class IntegerListElementValue extends ElementValue {
    private final long[] _values;

    public IntegerListElementValue(byte[] value) {
        _values = new long[value.length / 8];
        for (int i = 0; i < _values.length; ++i) {
            _values[i] = EndianUtilities.toUInt64LittleEndian(value, i * 8);
        }
    }

    public IntegerListElementValue(long[] values) {
        _values = values;
    }

    public ElementFormat getFormat() {
        return ElementFormat.IntegerList;
    }

    public String toString() {
        if (_values == null || _values.length == 0) {
            return "<none>";
        }

        String result = "";
        for (int i = 0; i < _values.length; ++i) {
            if (i != 0) {
                result += " ";
            }

            result += String.format("%16x", _values[i]);
        }
        return result;
    }

    public byte[] getBytes() {
        byte[] bytes = new byte[_values.length * 8];
        for (int i = 0; i < _values.length; ++i) {
            EndianUtilities.writeBytesLittleEndian(_values[i], bytes, i * 8);
        }
        return bytes;
    }
}
