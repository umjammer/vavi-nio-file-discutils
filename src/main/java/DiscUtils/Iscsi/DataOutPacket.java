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


public class DataOutPacket {
    private final Connection _connection;

    private final long _lun;

    public DataOutPacket(Connection connection, long lun) {
        _connection = connection;
        _lun = lun;
    }

    public byte[] getBytes(byte[] data,
                           int offset,
                           int count,
                           boolean isFinalData,
                           int dataSeqNumber,
                           int bufferOffset,
                           int targetTransferTag) {
        BasicHeaderSegment _basicHeader = new BasicHeaderSegment();
        _basicHeader.Immediate = false;
        _basicHeader._OpCode = OpCode.ScsiDataOut;
        _basicHeader.FinalPdu = isFinalData;
        _basicHeader.TotalAhsLength = 0;
        _basicHeader.DataSegmentLength = count;
        _basicHeader.InitiatorTaskTag = _connection.Session.CurrentTaskTag;
        byte[] buffer = new byte[48 + MathUtilities.roundUp(count, 4)];
        _basicHeader.writeTo(buffer, 0);
        buffer[1] = (byte) (isFinalData ? 0x80 : 0x00);
        EndianUtilities.writeBytesBigEndian(_lun, buffer, 8);
        EndianUtilities.writeBytesBigEndian(targetTransferTag, buffer, 20);
        EndianUtilities.writeBytesBigEndian(_connection.ExpectedStatusSequenceNumber, buffer, 28);
        EndianUtilities.writeBytesBigEndian(dataSeqNumber, buffer, 36);
        EndianUtilities.writeBytesBigEndian(bufferOffset, buffer, 40);
        System.arraycopy(data, offset, buffer, 48, count);
        return buffer;
    }
}
