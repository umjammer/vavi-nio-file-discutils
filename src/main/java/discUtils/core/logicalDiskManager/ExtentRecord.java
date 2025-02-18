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

package discUtils.core.logicalDiskManager;

public final class ExtentRecord extends DatabaseRecord {

    public long componentId;

    public long diskId;

    public long diskOffsetLba;

    public long interleaveOrder;

    public long offsetInVolumeLba;

    public int partitionComponentLink;

    public long sizeLba;

    public int unknown1;

    public int unknown2;

    @Override protected void doReadFrom(byte[] buffer, int offset) {
        super.doReadFrom(buffer, offset);
        int[] pos = new int[] {offset + 0x18};
        id = readVarULong(buffer, pos);
        name = readVarString(buffer, pos);
        unknown1 = readUInt(buffer, pos);
        unknown2 = readUInt(buffer, pos);
        partitionComponentLink = readUInt(buffer, pos);
        diskOffsetLba = readLong(buffer, pos);
        offsetInVolumeLba = readLong(buffer, pos);
        sizeLba = readVarLong(buffer, pos);
        componentId = readVarULong(buffer, pos);
        diskId = readVarULong(buffer, pos);
        if ((flags & 0x0800) != 0) {
            interleaveOrder = readVarULong(buffer, pos);
        }
    }
}
