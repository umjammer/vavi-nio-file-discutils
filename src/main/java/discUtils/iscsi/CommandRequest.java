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


public class CommandRequest {

    private final Connection connection;

    private final long lun;

    public CommandRequest(Connection connection, long lun) {
        this.connection = connection;
        this.lun = lun;
    }

    public byte[] getBytes(ScsiCommand cmd,
                           byte[] immediateData,
                           int offset,
                           int count,
                           boolean isFinalData,
                           boolean willRead,
                           boolean willWrite,
                           int expected) {
        BasicHeaderSegment basicHeader = new BasicHeaderSegment();
        basicHeader.immediate = cmd.getImmediateDelivery();
        basicHeader.opCode = OpCode.ScsiCommand;
        basicHeader.finalPdu = isFinalData;
        basicHeader.totalAhsLength = 0;
        basicHeader.dataSegmentLength = count;
        basicHeader.initiatorTaskTag = connection.getSession().getCurrentTaskTag();
        byte[] buffer = new byte[48 + MathUtilities.roundUp(count, 4)];
        basicHeader.writeTo(buffer, 0);
        buffer[1] = packAttrByte(isFinalData, willRead, willWrite, cmd.getTaskAttributes());
        ByteUtil.writeBeLong(lun, buffer, 8);
        ByteUtil.writeBeInt(expected, buffer, 20);
        ByteUtil.writeBeInt(connection.getSession().getCommandSequenceNumber(), buffer, 24);
        ByteUtil.writeBeInt(connection.getExpectedStatusSequenceNumber(), buffer, 28);
        cmd.writeTo(buffer, 32);
        if (immediateData != null && count != 0) {
            System.arraycopy(immediateData, offset, buffer, 48, count);
        }

        return buffer;
    }

    private static byte packAttrByte(boolean isFinalData,
                                     boolean expectReadFromTarget,
                                     boolean expectWriteToTarget,
                                     TaskAttributes taskAttr) {
        byte value = 0;
        if (isFinalData) {
            value |= 0x80;
        }

        if (expectReadFromTarget) {
            value |= 0x40;
        }

        if (expectWriteToTarget) {
            value |= 0x20;
        }

        value |= (byte) (taskAttr.ordinal() & 0x3);
        return value;
    }
}
