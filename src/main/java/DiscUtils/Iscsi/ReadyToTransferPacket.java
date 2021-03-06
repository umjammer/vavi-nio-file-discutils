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

import DiscUtils.Streams.Util.EndianUtilities;


public class ReadyToTransferPacket extends BaseResponse {
    public int BufferOffset;

    public int DesiredTransferLength;

    public BasicHeaderSegment Header;

    public long Lun;

    public int ReadyToTransferSequenceNumber;

    public int TargetTransferTag;

    public void parse(ProtocolDataUnit pdu) {
        parse(pdu.getHeaderData(), 0);
    }

    public void parse(byte[] headerData, int headerOffset) {
        StatusPresent = false;
        Header = new BasicHeaderSegment();
        Header.readFrom(headerData, headerOffset);
        if (Header._OpCode != OpCode.ReadyToTransfer) {
            throw new IllegalArgumentException("Invalid opcode in response, expected " + OpCode.ReadyToTransfer + " was " +
                                               Header._OpCode);
        }

        Lun = EndianUtilities.toUInt64BigEndian(headerData, headerOffset + 8);
        TargetTransferTag = EndianUtilities.toUInt32BigEndian(headerData, headerOffset + 20);
        StatusSequenceNumber = EndianUtilities.toUInt32BigEndian(headerData, headerOffset + 24);
        ExpectedCommandSequenceNumber = EndianUtilities.toUInt32BigEndian(headerData, headerOffset + 28);
        MaxCommandSequenceNumber = EndianUtilities.toUInt32BigEndian(headerData, headerOffset + 32);
        ReadyToTransferSequenceNumber = EndianUtilities.toUInt32BigEndian(headerData, headerOffset + 36);
        BufferOffset = EndianUtilities.toUInt32BigEndian(headerData, headerOffset + 40);
        DesiredTransferLength = EndianUtilities.toUInt32BigEndian(headerData, headerOffset + 44);
    }
}
