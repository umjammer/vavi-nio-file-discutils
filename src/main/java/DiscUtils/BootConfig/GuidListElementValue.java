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


public class GuidListElementValue extends ElementValue {
    private final String[] _values;

    public GuidListElementValue(String[] values) {
        _values = values;
    }

    public ElementFormat getFormat() {
        return ElementFormat.GuidList;
    }

    public String toString() {
        if (_values == null || _values.length == 0) {
            return "<none>";
        }

        StringBuilder result = new StringBuilder(_values[0]);
        for (int i = 1; i < _values.length; ++i) {
            result.append(",").append(_values[i]);
        }
        return result.toString();
    }

    public String[] getGuidStrings() {
        return _values;
    }
}
