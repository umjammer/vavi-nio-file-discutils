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
import DiscUtils.Streams.Util.MathUtilities;


public class TextRequest {
    private int _commandSequenceNumber;

    // Per-session
    private final Connection _connection;

    private boolean _continue;

    private int _expectedStatusSequenceNumber;

    // Per-connection (ack)
    @SuppressWarnings("unused")
    private long _lun;

    private final int _targetTransferTag = 0xFFFFFFFF;

    public TextRequest(Connection connection) {
        _connection = connection;
    }

    public byte[] getBytes(long lun, byte[] data, int offset, int count, boolean isFinalData) {
        BasicHeaderSegment _basicHeader = new BasicHeaderSegment();
        _basicHeader.Immediate = true;
        _basicHeader._OpCode = OpCode.TextRequest;
        _basicHeader.FinalPdu = isFinalData;
        _basicHeader.TotalAhsLength = 0;
        _basicHeader.DataSegmentLength = count;
        _basicHeader.InitiatorTaskTag = _connection.getSession().getCurrentTaskTag();
        _continue = !isFinalData;
        _lun = lun;
        _commandSequenceNumber = _connection.getSession().getCommandSequenceNumber();
        _expectedStatusSequenceNumber = _connection.getExpectedStatusSequenceNumber();
        byte[] buffer = new byte[MathUtilities.roundUp(48 + count, 4)];
        _basicHeader.writeTo(buffer, 0);
        buffer[1] |= (byte) (_continue ? 0x40 : 0x00);
        EndianUtilities.writeBytesBigEndian(lun, buffer, 8);
        EndianUtilities.writeBytesBigEndian(_targetTransferTag, buffer, 20);
        EndianUtilities.writeBytesBigEndian(_commandSequenceNumber, buffer, 24);
        EndianUtilities.writeBytesBigEndian(_expectedStatusSequenceNumber, buffer, 28);
        System.arraycopy(data, offset, buffer, 48, count);
        return buffer;
    }
}
