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
    public long ComponentId;

    public long DiskId;

    public long DiskOffsetLba;

    public long InterleaveOrder;

    public long OffsetInVolumeLba;

    public int PartitionComponentLink;

    public long SizeLba;

    public int Unknown1;

    public int Unknown2;

    protected void doReadFrom(byte[] buffer, int offset) {
        super.doReadFrom(buffer, offset);
        int pos = offset + 0x18;
        int[] refVar___0 = new int[] { pos };
        Id = readVarULong(buffer, refVar___0);
        pos = refVar___0[0];
        int[] refVar___1 = new int[] { pos };
        Name = readVarString(buffer, refVar___1);
        pos = refVar___1[0];
        int[] refVar___2 = new int[] { pos };
        Unknown1 = readUInt(buffer, refVar___2);
        pos = refVar___2[0];
        int[] refVar___3 = new int[] { pos };
        Unknown2 = readUInt(buffer, refVar___3);
        pos = refVar___3[0];
        int[] refVar___4 = new int[] { pos };
        PartitionComponentLink = readUInt(buffer, refVar___4);
        pos = refVar___4[0];
        int[] refVar___5 = new int[] { pos };
        DiskOffsetLba = readLong(buffer, refVar___5);
        pos = refVar___5[0];
        int[] refVar___6 = new int[] { pos };
        OffsetInVolumeLba = readLong(buffer, refVar___6);
        pos = refVar___6[0];
        int[] refVar___7 = new int[] { pos };
        SizeLba = readVarLong(buffer, refVar___7);
        pos = refVar___7[0];
        int[] refVar___8 = new int[] { pos };
        ComponentId = readVarULong(buffer, refVar___8);
        pos = refVar___8[0];
        int[] refVar___9 = new int[] { pos };
        DiskId = readVarULong(buffer, refVar___9);
        pos = refVar___9[0];
        if ((Flags & 0x0800) != 0) {
            int[] refVar___10 = new int[] {
                pos
            };
            InterleaveOrder = readVarULong(buffer, refVar___10);
            pos = refVar___10[0];
        }

    }

}
