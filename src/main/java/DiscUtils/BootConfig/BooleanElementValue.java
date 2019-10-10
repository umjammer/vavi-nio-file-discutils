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


public class BooleanElementValue extends ElementValue {
    private final boolean _value;

    public BooleanElementValue(byte[] value) {
        _value = value[0] != 0;
    }

    public BooleanElementValue(boolean value) {
        _value = value;
    }

    public ElementFormat getFormat() {
        return ElementFormat.Boolean;
    }

    public String toString() {
        return _value ? "True" : "False";
    }

    public byte[] getBytes() {
        return new byte[] {
            _value ? (byte) 1 : (byte) 0
        };
    }
}
