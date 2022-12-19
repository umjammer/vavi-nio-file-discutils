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

package discUtils.bootConfig;

import vavi.util.ByteUtil;


public class IntegerListElementValue extends ElementValue {

    private final long[] values;

    public IntegerListElementValue(byte[] value) {
        values = new long[value.length / 8];
        for (int i = 0; i < values.length; ++i) {
            values[i] = ByteUtil.readLeLong(value, i * 8);
        }
    }

    public IntegerListElementValue(long[] values) {
        this.values = values;
    }

    public ElementFormat getFormat() {
        return ElementFormat.IntegerList;
    }

    public String toString() {
        if (values == null || values.length == 0) {
            return "<none>";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; ++i) {
            if (i != 0) {
                result.append(" ");
            }

            result.append(String.format("%16x", values[i]));
        }
        return result.toString();
    }

    public byte[] getBytes() {
        byte[] bytes = new byte[values.length * 8];
        for (int i = 0; i < values.length; ++i) {
            ByteUtil.writeLeLong(values[i], bytes, i * 8);
        }
        return bytes;
    }
}
