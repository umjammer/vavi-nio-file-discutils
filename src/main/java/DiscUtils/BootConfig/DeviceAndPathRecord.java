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

import java.nio.charset.StandardCharsets;


public class DeviceAndPathRecord extends DeviceRecord {
    private DeviceRecord _container;

    private String _path;

    public int getSize() {
        throw new UnsupportedOperationException();
    }

    public void getBytes(byte[] data, int offset) {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        return _container + ":" + _path;
    }

    protected void doParse(byte[] data, int offset) {
        super.doParse(data, offset);
        _container = parse(data, offset + 0x34);
        int pathStart = 0x34 + _container.getSize();
        _path = new String(data, offset + pathStart, getLength() - pathStart, StandardCharsets.UTF_16LE);
    }
}
