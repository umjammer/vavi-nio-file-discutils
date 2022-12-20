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

package discUtils.udf;

import vavi.util.ByteUtil;


public final class MetadataPartitionMap extends PartitionMap {

    public short alignmentUnitSize;

    public int allocationUnitSize;

    public byte flags;

    public int metadataBitmapFileLocation;

    public int metadataFileLocation;

    public int metadataMirrorFileLocation;

    public short partitionNumber;

    public short volumeSequenceNumber;

    @Override
    public int size() {
        return 64;
    }

    @Override
    protected int parse(byte[] buffer, int offset) {
        volumeSequenceNumber = ByteUtil.readLeShort(buffer, offset + 36);
        partitionNumber = ByteUtil.readLeShort(buffer, offset + 38);
        metadataFileLocation = ByteUtil.readLeInt(buffer, offset + 40);
        metadataMirrorFileLocation = ByteUtil.readLeInt(buffer, offset + 44);
        metadataBitmapFileLocation = ByteUtil.readLeInt(buffer, offset + 48);
        allocationUnitSize = ByteUtil.readLeInt(buffer, offset + 52);
        alignmentUnitSize = ByteUtil.readLeShort(buffer, offset + 56);
        flags = buffer[offset + 58];
        return 64;
    }
}
