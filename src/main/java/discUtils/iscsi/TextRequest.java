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


public class TextRequest {

    private int commandSequenceNumber;

    // Per-session
    private final Connection connection;

    private boolean continue_;

    private int expectedStatusSequenceNumber;

    // Per-connection (ack)
    @SuppressWarnings("unused")
    private long lun;

    private static final int targetTransferTag = 0xFFFF_FFFF;

    public TextRequest(Connection connection) {
        this.connection = connection;
    }

    public byte[] getBytes(long lun, byte[] data, int offset, int count, boolean isFinalData) {
        BasicHeaderSegment basicHeader = new BasicHeaderSegment();
        basicHeader.immediate = true;
        basicHeader.opCode = OpCode.TextRequest;
        basicHeader.finalPdu = isFinalData;
        basicHeader.totalAhsLength = 0;
        basicHeader.dataSegmentLength = count;
        basicHeader.initiatorTaskTag = connection.getSession().getCurrentTaskTag();
        continue_ = !isFinalData;
        this.lun = lun;
        commandSequenceNumber = connection.getSession().getCommandSequenceNumber();
        expectedStatusSequenceNumber = connection.getExpectedStatusSequenceNumber();
        byte[] buffer = new byte[MathUtilities.roundUp(48 + count, 4)];
        basicHeader.writeTo(buffer, 0);
        buffer[1] |= (byte) (continue_ ? 0x40 : 0x00);
        ByteUtil.writeBeLong(lun, buffer, 8);
        ByteUtil.writeBeInt(targetTransferTag, buffer, 20);
        ByteUtil.writeBeInt(commandSequenceNumber, buffer, 24);
        ByteUtil.writeBeInt(expectedStatusSequenceNumber, buffer, 28);
        System.arraycopy(data, offset, buffer, 48, count);
        return buffer;
    }
}
