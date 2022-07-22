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

    private BasicHeaderSegment basicHeader;

    private int commandSequenceNumber;

    // Per-session
    private final Connection connection;

    private short connectionId;

    private boolean continue_;

    private LoginStages currentStage = LoginStages.SecurityNegotiation;

    private int expectedStatusSequenceNumber;

    // Per-connection (ack)
    private LoginStages nextStage = LoginStages.SecurityNegotiation;

    private boolean transit;

    public LoginRequest(Connection connection) {
        this.connection = connection;
    }

    public byte[] getBytes(byte[] data, int offset, int count, boolean isFinalData) {
        basicHeader = new BasicHeaderSegment();
        basicHeader.immediate = true;
        basicHeader.opCode = OpCode.LoginRequest;
        basicHeader.finalPdu = isFinalData;
        basicHeader.totalAhsLength = 0;
        basicHeader.dataSegmentLength = count;
        basicHeader.initiatorTaskTag = connection.getSession().getCurrentTaskTag();

        transit = isFinalData;
        continue_ = !isFinalData;
        currentStage = connection.getCurrentLoginStage();
        if (transit) {
            nextStage = connection.getNextLoginStage();
        }

        connectionId = connection.getId();
        commandSequenceNumber = connection.getSession().getCommandSequenceNumber();
        expectedStatusSequenceNumber = connection.getExpectedStatusSequenceNumber();

        byte[] buffer = new byte[MathUtilities.roundUp(48 + count, 4)];
        basicHeader.writeTo(buffer, 0);
        buffer[1] = packState();
        buffer[2] = 0; // Max Version
        buffer[3] = 0; // Min Version
        EndianUtilities.writeBytesBigEndian(connection.getSession().getInitiatorSessionId(), buffer, 8);
        EndianUtilities.writeBytesBigEndian(IsidQualifier, buffer, 12);
        EndianUtilities.writeBytesBigEndian(connection.getSession().getTargetSessionId(), buffer, 14);
        EndianUtilities.writeBytesBigEndian(connectionId, buffer, 20);
        EndianUtilities.writeBytesBigEndian(commandSequenceNumber, buffer, 24);
        EndianUtilities.writeBytesBigEndian(expectedStatusSequenceNumber, buffer, 28);
        System.arraycopy(data, offset, buffer, 48, count);
        return buffer;
    }

    private byte packState() {
        byte val = 0;

        if (transit) {
            val |= 0x80;
        }

        if (continue_) {
            val |= 0x40;
        }

        val |= (byte) (currentStage.ordinal() << 2);
        val |= (byte) nextStage.ordinal();

        return val;
    }
}
