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

public final class ComponentRecord extends DatabaseRecord {
    // Identical on mirrors
    public int LinkId;

    // (02 Spanned, Simple, Mirrored)  (01 on striped)
    public ExtentMergeType MergeType = ExtentMergeType.None;

    // Could be num disks
    public long NumExtents;

    public String StatusString;

    public long StripeSizeSectors;

    // aka num partitions
    public long StripeStride;

    // Zero
    public int Unknown1;

    // Zero
    public int Unknown2;

    // 00 .. 00
    public long Unknown3;

    // ??
    public long Unknown4;

    public long VolumeId;

    protected void doReadFrom(byte[] buffer, int offset) {
        super.doReadFrom(buffer, offset);
        int[] pos = new int[] { offset + 0x18 };
        Id = readVarULong(buffer, pos);
        Name = readVarString(buffer, pos);
        StatusString = readVarString(buffer, pos);
        MergeType = ExtentMergeType.values()[readByte(buffer, pos)];
        Unknown1 = readUInt(buffer, pos); // Zero
        NumExtents = readVarULong(buffer, pos);
        Unknown2 = readUInt(buffer, pos);
        LinkId = readUInt(buffer, pos);
        Unknown3 = readULong(buffer, pos); // Zero
        VolumeId = readVarULong(buffer, pos);
        Unknown4 = readVarULong(buffer, pos); // Zero
        if ((Flags & 0x1000) != 0) {
            StripeSizeSectors = readVarLong(buffer, pos);
            StripeStride = readVarLong(buffer, pos);
        }
    }
}
