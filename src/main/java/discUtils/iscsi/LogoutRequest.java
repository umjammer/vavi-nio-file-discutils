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

import discUtils.streams.util.MathUtilities;
import vavi.util.ByteUtil;


public class LogoutRequest {

    private final Connection connection;

    public LogoutRequest(Connection connection) {
        this.connection = connection;
    }

    public byte[] getBytes(LogoutReason reason) {
        BasicHeaderSegment basicHeader = new BasicHeaderSegment();
        basicHeader.immediate = true;
        basicHeader.opCode = OpCode.LogoutRequest;
        basicHeader.finalPdu = true;
        basicHeader.totalAhsLength = 0;
        basicHeader.dataSegmentLength = 0;
        basicHeader.initiatorTaskTag = connection.getSession().getCurrentTaskTag();
        byte[] buffer = new byte[MathUtilities.roundUp(48, 4)];
        basicHeader.writeTo(buffer, 0);
        buffer[1] |= (byte) (reason.ordinal() & 0x7F);
        ByteUtil.writeBeShort(connection.getId(), buffer, 20);
        ByteUtil.writeBeInt(connection.getSession().getCommandSequenceNumber(), buffer, 24);
        ByteUtil.writeBeInt(connection.getExpectedStatusSequenceNumber(), buffer, 28);
        return buffer;
    }
}
