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


public class TextResponse extends BaseResponse {

    private long lun;

    private int targetTransferTag = 0xFFFF_FFFF;

    public boolean continue_;

    public byte[] textData;

    @Override public void parse(ProtocolDataUnit pdu) {
        parse(pdu.getHeaderData(), 0, pdu.getContentData());
    }

    public void parse(byte[] headerData, int headerOffset, byte[] bodyData) {
        BasicHeaderSegment headerSegment = new BasicHeaderSegment();
        headerSegment.readFrom(headerData, headerOffset);
        if (headerSegment.opCode != OpCode.TextResponse) {
            throw new IllegalArgumentException("Invalid opcode in response, expected " + OpCode.TextResponse + " was " +
                                               headerSegment.opCode);
        }

        continue_ = (headerData[headerOffset + 1] & 0x40) != 0;
        lun = ByteUtil.readBeLong(headerData, headerOffset + 8);
        targetTransferTag = ByteUtil.readBeInt(headerData, headerOffset + 20);
        statusSequenceNumber = ByteUtil.readBeInt(headerData, headerOffset + 24);
        expectedCommandSequenceNumber = ByteUtil.readBeInt(headerData, headerOffset + 28);
        maxCommandSequenceNumber = ByteUtil.readBeInt(headerData, headerOffset + 32);
        textData = bodyData;
    }
}
