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

import discUtils.streams.util.EndianUtilities;
import vavi.util.ByteUtil;


public class PartitionDescriptor extends TaggedDescriptor<PartitionDescriptor> {

    public int accessType;

    public EntityIdentifier implementationIdentifier;

    public byte[] implementationUse;

    public EntityIdentifier partitionContents;

    public byte[] partitionContentsUse;

    public short partitionFlags;

    public int partitionLength;

    public short partitionNumber;

    public int partitionStartingLocation;

    public int volumeDescriptorSequenceNumber;

    public PartitionDescriptor() {
        super(TagIdentifier.PartitionDescriptor);
    }

    @Override
    public int parse(byte[] buffer, int offset) {
        volumeDescriptorSequenceNumber = ByteUtil.readLeInt(buffer, offset + 16);
        partitionFlags = ByteUtil.readLeShort(buffer, offset + 20);
        partitionNumber = ByteUtil.readLeShort(buffer, offset + 22);
        partitionContents = EndianUtilities
                .toStruct(ApplicationEntityIdentifier.class, buffer, offset + 24);
        partitionContentsUse = EndianUtilities.toByteArray(buffer, offset + 56, 128);
        accessType = ByteUtil.readLeInt(buffer, offset + 184);
        partitionStartingLocation = ByteUtil.readLeInt(buffer, offset + 188);
        partitionLength = ByteUtil.readLeInt(buffer, offset + 192);
        implementationIdentifier = EndianUtilities
                .toStruct(ImplementationEntityIdentifier.class, buffer, offset + 196);
        implementationUse = EndianUtilities.toByteArray(buffer, offset + 228, 128);
        return 512;
    }
}
