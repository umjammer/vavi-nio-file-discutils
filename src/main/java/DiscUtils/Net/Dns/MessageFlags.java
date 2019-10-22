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

package DiscUtils.Net.Dns;

public class MessageFlags {
    public MessageFlags() {
    }

    public MessageFlags(short value) {
        __Value = value;
    }

    public MessageFlags(boolean isResponse,
            OpCode opCode,
            boolean isAuthorative,
            boolean isTruncated,
            boolean recursionDesired,
            boolean recursionAvailable,
            ResponseCode responseCode) {
        int val = 0;
        val |= isResponse ? 0x8000 : 0x0000;
        val |= (opCode.ordinal() & 0xF) << 11;
        val |= isAuthorative ? 0x0400 : 0x0000;
        val |= isTruncated ? 0x0200 : 0x0000;
        val |= recursionDesired ? 0x0100 : 0x0000;
        val |= recursionAvailable ? 0x0080 : 0x0000;
        val |= responseCode.ordinal() & 0xF;
        __Value = (short) val;
    }

    private short __Value;

    public short getValue() {
        return __Value;
    }

    public boolean isResponse() {
        return (getValue() & 0x8000) != 0;
    }

    public OpCode getOpCode() {
        return OpCode.valueOf((getValue() >>> 11) & 0xF);
    }

    public boolean isAuthorative() {
        return (getValue() & 0x0400) != 0;
    }

    public boolean isTruncated() {
        return (getValue() & 0x0200) != 0;
    }

    public boolean getRecursionDesired() {
        return (getValue() & 0x0100) != 0;
    }

    public boolean getRecursionAvailable() {
        return (getValue() & 0x0080) != 0;
    }

    public ResponseCode getResponseCode() {
        return ResponseCode.valueOf(getValue() & 0x000F);
    }
}
