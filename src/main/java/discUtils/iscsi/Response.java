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


public class Response extends BaseResponse {

    public int bidiReadResidualCount;

    public int expectedDataSequenceNumber;

    public BasicHeaderSegment header;

    public int residualCount;

    public byte responseCode;

    public ScsiStatus status = ScsiStatus.Good;

    @Override public void parse(ProtocolDataUnit pdu) {
        parse(pdu.getHeaderData(), 0);
    }

    public void parse(byte[] headerData, int headerOffset) {
        header = new BasicHeaderSegment();
        header.readFrom(headerData, headerOffset);
        if (header.opCode != OpCode.ScsiResponse) {
            throw new IllegalArgumentException("Invalid opcode in response, expected " + OpCode.ScsiResponse + " was " +
                                               header.opCode);
        }

        responseCode = headerData[headerOffset + 2];
        statusPresent = true;
        status = ScsiStatus.valueOf(headerData[headerOffset + 3]);
        statusSequenceNumber = ByteUtil.readBeInt(headerData, headerOffset + 24);
        expectedCommandSequenceNumber = ByteUtil.readBeInt(headerData, headerOffset + 28);
        maxCommandSequenceNumber = ByteUtil.readBeInt(headerData, headerOffset + 32);
        expectedDataSequenceNumber = ByteUtil.readBeInt(headerData, headerOffset + 36);
        bidiReadResidualCount = ByteUtil.readBeInt(headerData, headerOffset + 40);
        residualCount = ByteUtil.readBeInt(headerData, headerOffset + 44);
    }
}
