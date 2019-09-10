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

package DiscUtils.Core.ApplePartitionMap;

import java.io.Serializable;

import DiscUtils.Streams.Util.EndianUtilities;


public final class BlockZero implements Serializable {
    public int BlockCount;

    public short BlockSize;

    public short DeviceId;

    public short DeviceType;

    public short DriverCount;

    public int DriverData;

    public short Signature;

    public int getSize() {
        return 512;
    }

    public int readFrom(byte[] buffer, int offset) {
        Signature = EndianUtilities.toUInt16BigEndian(buffer, offset + 0);
        BlockSize = EndianUtilities.toUInt16BigEndian(buffer, offset + 2);
        BlockCount = EndianUtilities.toUInt32BigEndian(buffer, offset + 4);
        DeviceType = EndianUtilities.toUInt16BigEndian(buffer, offset + 8);
        DeviceId = EndianUtilities.toUInt16BigEndian(buffer, offset + 10);
        DriverData = EndianUtilities.toUInt32BigEndian(buffer, offset + 12);
        DriverCount = (short) EndianUtilities.toUInt16LittleEndian(buffer, offset + 16);
        return 512;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

}
