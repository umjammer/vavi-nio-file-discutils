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

import vavi.util.ByteUtil;


public class ScsiReadCapacityResponse extends ScsiResponse {

    private boolean truncated;

    private int logicalBlockSize;

    public int getLogicalBlockSize() {
        return logicalBlockSize;
    }

    public void setLogicalBlockSize(int value) {
        logicalBlockSize = value;
    }

    @Override public int getNeededDataLength() {
        return 8;
    }

    private int numLogicalBlocks;

    public int getNumLogicalBlocks() {
        return numLogicalBlocks;
    }

    public void setNumLogicalBlocks(int value) {
        numLogicalBlocks = value;
    }

    @Override public boolean getTruncated() {
        return truncated;
    }

    @Override public void readFrom(byte[] buffer, int offset, int count) {
        if (count < 8) {
            truncated = true;
            return;
        }

        setNumLogicalBlocks(ByteUtil.readBeInt(buffer, offset));
        setLogicalBlockSize(ByteUtil.readBeInt(buffer, offset + 4));
    }
}
