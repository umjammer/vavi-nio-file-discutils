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


public class ScsiReadCommand extends ScsiCommand {

    private final int logicalBlockAddress;

    private final short numBlocks;

    public ScsiReadCommand(long targetLun, int logicalBlockAddress, short numBlocks) {
        super(targetLun);
        this.logicalBlockAddress = logicalBlockAddress;
        this.numBlocks = numBlocks;
    }

    public int size() {
        return 10;
    }

    public int readFrom(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public void writeTo(byte[] buffer, int offset) {
        buffer[offset + 0] = 0x28; // OpCode: READ(10)
        buffer[offset + 1] = 0;
        ByteUtil.writeBeInt(logicalBlockAddress, buffer, offset + 2);
        buffer[offset + 6] = 0;
        ByteUtil.writeBeShort(numBlocks, buffer, offset + 7);
        buffer[offset + 9] = 0;
    }
}
