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


public class DataOutPacket {

    private final Connection connection;

    private final long lun;

    public DataOutPacket(Connection connection, long lun) {
        this.connection = connection;
        this.lun = lun;
    }

    public byte[] getBytes(byte[] data,
                           int offset,
                           int count,
                           boolean isFinalData,
                           int dataSeqNumber,
                           int bufferOffset,
                           int targetTransferTag) {
        BasicHeaderSegment basicHeader = new BasicHeaderSegment();
        basicHeader.immediate = false;
        basicHeader.opCode = OpCode.ScsiDataOut;
        basicHeader.finalPdu = isFinalData;
        basicHeader.totalAhsLength = 0;
        basicHeader.dataSegmentLength = count;
        basicHeader.initiatorTaskTag = connection.getSession().getCurrentTaskTag();
        byte[] buffer = new byte[48 + MathUtilities.roundUp(count, 4)];
        basicHeader.writeTo(buffer, 0);
        buffer[1] = (byte) (isFinalData ? 0x80 : 0x00);
        ByteUtil.writeBeLong(lun, buffer, 8);
        ByteUtil.writeBeInt(targetTransferTag, buffer, 20);
        ByteUtil.writeBeInt(connection.getExpectedStatusSequenceNumber(), buffer, 28);
        ByteUtil.writeBeInt(dataSeqNumber, buffer, 36);
        ByteUtil.writeBeInt(bufferOffset, buffer, 40);
        System.arraycopy(data, offset, buffer, 48, count);
        return buffer;
    }
}
