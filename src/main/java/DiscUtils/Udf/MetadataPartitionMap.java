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

package DiscUtils.Udf;

import DiscUtils.Streams.Util.EndianUtilities;


public final class MetadataPartitionMap extends PartitionMap {
    public short AlignmentUnitSize;

    public int AllocationUnitSize;

    public byte Flags;

    public int MetadataBitmapFileLocation;

    public int MetadataFileLocation;

    public int MetadataMirrorFileLocation;

    public short PartitionNumber;

    public short VolumeSequenceNumber;

    public int size() {
        return 64;
    }

    protected int parse(byte[] buffer, int offset) {
        VolumeSequenceNumber = EndianUtilities.toUInt16LittleEndian(buffer, offset + 36);
        PartitionNumber = EndianUtilities.toUInt16LittleEndian(buffer, offset + 38);
        MetadataFileLocation = EndianUtilities.toUInt32LittleEndian(buffer, offset + 40);
        MetadataMirrorFileLocation = EndianUtilities.toUInt32LittleEndian(buffer, offset + 44);
        MetadataBitmapFileLocation = EndianUtilities.toUInt32LittleEndian(buffer, offset + 48);
        AllocationUnitSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 52);
        AlignmentUnitSize = EndianUtilities.toUInt16LittleEndian(buffer, offset + 56);
        Flags = buffer[offset + 58];
        return 64;
    }
}
