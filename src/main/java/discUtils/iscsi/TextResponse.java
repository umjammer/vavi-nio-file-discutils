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

import discUtils.streams.util.EndianUtilities;


public class TextResponse extends BaseResponse {

    private long lun;

    private int targetTransferTag = 0xFFFF_FFFF;

    public boolean continue_;

    public byte[] textData;

    public void parse(ProtocolDataUnit pdu) {
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
        lun = EndianUtilities.toUInt64BigEndian(headerData, headerOffset + 8);
        targetTransferTag = EndianUtilities.toUInt32BigEndian(headerData, headerOffset + 20);
        statusSequenceNumber = EndianUtilities.toUInt32BigEndian(headerData, headerOffset + 24);
        expectedCommandSequenceNumber = EndianUtilities.toUInt32BigEndian(headerData, headerOffset + 28);
        maxCommandSequenceNumber = EndianUtilities.toUInt32BigEndian(headerData, headerOffset + 32);
        textData = bodyData;
    }
}
