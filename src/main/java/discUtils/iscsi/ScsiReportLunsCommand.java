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

package discUtils.iscsi;

import vavi.util.ByteUtil;


public class ScsiReportLunsCommand extends ScsiCommand {

    public static final int InitialResponseSize = 16;

    private final int expected;

    public ScsiReportLunsCommand(int expected) {
        super(0);
        this.expected = expected;
    }

    @Override public int size() {
        return 12;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        buffer[offset] = (byte) 0xA0;
        buffer[offset + 1] = 0;
        // Reserved
        buffer[offset + 2] = 0;
        // Report Type = 0
        buffer[offset + 3] = 0;
        // Reserved
        buffer[offset + 4] = 0;
        // Reserved
        buffer[offset + 5] = 0;
        // Reserved
        ByteUtil.writeBeInt(expected, buffer, offset + 6);
        buffer[offset + 10] = 0;
        // Reserved
        buffer[offset + 11] = 0;
    }
}

// Control
