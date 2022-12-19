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

package discUtils.core.applePartitionMap;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public final class BlockZero implements IByteArraySerializable {

    public int blockCount;

    public short blockSize;

    public short deviceId;

    public short deviceType;

    public short driverCount;

    public int driverData;

    public short signature;

    public int size() {
        return 512;
    }

    public int readFrom(byte[] buffer, int offset) {
        signature = ByteUtil.readBeShort(buffer, offset + 0);
        blockSize = ByteUtil.readBeShort(buffer, offset + 2);
        blockCount = ByteUtil.readBeInt(buffer, offset + 4);
        deviceType = ByteUtil.readBeShort(buffer, offset + 8);
        deviceId = ByteUtil.readBeShort(buffer, offset + 10);
        driverData = ByteUtil.readBeInt(buffer, offset + 12);
        driverCount = ByteUtil.readLeShort(buffer, offset + 16);
        return 512;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
