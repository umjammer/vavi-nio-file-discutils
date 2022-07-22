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


public class LoginResponse extends BaseResponse {

    public byte activeVersion;

    public boolean continue_;

    public LoginStages currentStage = LoginStages.SecurityNegotiation;

    public byte maxVersion;

    public LoginStages nextStage = LoginStages.SecurityNegotiation;

    public byte statusClass;

    public LoginStatusCode statusCode = LoginStatusCode.Success;

    public short targetSessionId;

    public byte[] textData;

    public boolean transit;

    public void parse(ProtocolDataUnit pdu) {
        parse(pdu.getHeaderData(), 0, pdu.getContentData());
    }

    public void parse(byte[] headerData, int headerOffset, byte[] bodyData) {
        BasicHeaderSegment headerSegment = new BasicHeaderSegment();
        headerSegment.readFrom(headerData, headerOffset);
        if (headerSegment.opCode != OpCode.LoginResponse) {
            throw new IllegalArgumentException("Invalid opcode in response, expected " + OpCode.LoginResponse + " was " +
                                               headerSegment.opCode);
        }

        unpackState(headerData[headerOffset + 1]);
        maxVersion = headerData[headerOffset + 2];
        activeVersion = headerData[headerOffset + 3];
        targetSessionId = EndianUtilities.toUInt16BigEndian(headerData, headerOffset + 14);
        statusPresent = true;
        statusSequenceNumber = EndianUtilities.toUInt32BigEndian(headerData, headerOffset + 24);
        expectedCommandSequenceNumber = EndianUtilities.toUInt32BigEndian(headerData, headerOffset + 28);
        maxCommandSequenceNumber = EndianUtilities.toUInt32BigEndian(headerData, headerOffset + 32);
        statusClass = headerData[headerOffset + 36];
        statusCode = LoginStatusCode.valueOf(EndianUtilities.toUInt16BigEndian(headerData, headerOffset + 36));
        textData = bodyData;
    }

    private void unpackState(byte value) {
        transit = (value & 0x80) != 0;
        continue_ = (value & 0x40) != 0;
        currentStage = LoginStages.values()[(value >>> 2) & 0x3];
        nextStage = LoginStages.values()[value & 0x3];
    }
}
