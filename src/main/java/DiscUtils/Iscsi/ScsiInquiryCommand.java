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

package DiscUtils.Iscsi;

import java.util.Arrays;

import DiscUtils.Streams.Util.EndianUtilities;


public class ScsiInquiryCommand extends ScsiCommand {
    public static final int InitialResponseDataLength = 36;

    private final boolean _askForPage = false;

    private final int _expected;

    private final byte _pageCode = 0;

    public ScsiInquiryCommand(long targetLun, int expected) {
        super(targetLun);
        _expected = expected;
    }

    public int size() {
        return 6;
    }

//    public ScsiInquiryCommand(long targetLun, byte pageCode, int expected) {
//        super(targetLun);
//        _askForPage = true;
//        _pageCode = pageCode;
//        _expected = expected;
//    }

    public TaskAttributes getTaskAttributes() {
        return TaskAttributes.Untagged;
    }

    public int readFrom(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public void writeTo(byte[] buffer, int offset) {
        Arrays.fill(buffer, offset, offset + 10, (byte) 0);
        buffer[offset] = 0x12;
        // OpCode
        buffer[offset + 1] = (byte) (_askForPage ? 0x01 : 0x00);
        buffer[offset + 2] = _pageCode;
        EndianUtilities.writeBytesBigEndian((short) _expected, buffer, offset + 3);
        buffer[offset + 5] = 0;
    }
}
