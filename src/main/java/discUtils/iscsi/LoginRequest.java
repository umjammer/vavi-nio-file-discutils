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
import discUtils.streams.util.MathUtilities;


public class LoginRequest {
    private static final short IsidQualifier = 0x0000;

    private BasicHeaderSegment _basicHeader;

    private int _commandSequenceNumber;

    // Per-session
    private final Connection _connection;

    private short _connectionId;

    private boolean _continue;

    private LoginStages _currentStage = LoginStages.SecurityNegotiation;

    private int _expectedStatusSequenceNumber;

    // Per-connection (ack)
    private LoginStages _nextStage = LoginStages.SecurityNegotiation;

    private boolean _transit;

    public LoginRequest(Connection connection) {
        _connection = connection;
    }

    public byte[] getBytes(byte[] data, int offset, int count, boolean isFinalData) {
        _basicHeader = new BasicHeaderSegment();
        _basicHeader.Immediate = true;
        _basicHeader._OpCode = OpCode.LoginRequest;
        _basicHeader.FinalPdu = isFinalData;
        _basicHeader.TotalAhsLength = 0;
        _basicHeader.DataSegmentLength = count;
        _basicHeader.InitiatorTaskTag = _connection.getSession().getCurrentTaskTag();

        _transit = isFinalData;
        _continue = !isFinalData;
        _currentStage = _connection.getCurrentLoginStage();
        if (_transit) {
            _nextStage = _connection.getNextLoginStage();
        }

        _connectionId = _connection.getId();
        _commandSequenceNumber = _connection.getSession().getCommandSequenceNumber();
        _expectedStatusSequenceNumber = _connection.getExpectedStatusSequenceNumber();

        byte[] buffer = new byte[MathUtilities.roundUp(48 + count, 4)];
        _basicHeader.writeTo(buffer, 0);
        buffer[1] = packState();
        buffer[2] = 0; // Max Version
        buffer[3] = 0; // Min Version
        EndianUtilities.writeBytesBigEndian(_connection.getSession().getInitiatorSessionId(), buffer, 8);
        EndianUtilities.writeBytesBigEndian(IsidQualifier, buffer, 12);
        EndianUtilities.writeBytesBigEndian(_connection.getSession().getTargetSessionId(), buffer, 14);
        EndianUtilities.writeBytesBigEndian(_connectionId, buffer, 20);
        EndianUtilities.writeBytesBigEndian(_commandSequenceNumber, buffer, 24);
        EndianUtilities.writeBytesBigEndian(_expectedStatusSequenceNumber, buffer, 28);
        System.arraycopy(data, offset, buffer, 48, count);
        return buffer;
    }

    private byte packState() {
        byte val = 0;

        if (_transit) {
            val |= 0x80;
        }

        if (_continue) {
            val |= 0x40;
        }

        val |= (byte) (_currentStage.ordinal() << 2);
        val |= (byte) _nextStage.ordinal();

        return val;
    }
}
