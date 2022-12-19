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


public final class LogicalVolumeDescriptor extends TaggedDescriptor<LogicalVolumeDescriptor> {

    public byte[] descriptorCharset;

    public EntityIdentifier domainIdentifier;

    public EntityIdentifier implementationIdentifier;

    public byte[] implementationUse;

    public ExtentDescriptor integritySequenceExtent;

    public int logicalBlockSize;

    public byte[] logicalVolumeContentsUse;

    public String logicalVolumeIdentifier;

    public int mapTableLength;

    public int numPartitionMaps;

    public PartitionMap[] partitionMaps;

    public int volumeDescriptorSequenceNumber;

    public LogicalVolumeDescriptor() {
        super(TagIdentifier.LogicalVolumeDescriptor);
    }

    public LongAllocationDescriptor getFileSetDescriptorLocation() {
        LongAllocationDescriptor lad = new LongAllocationDescriptor();
        lad.readFrom(logicalVolumeContentsUse, 0);
        return lad;
    }

    public int parse(byte[] buffer, int offset) {
        volumeDescriptorSequenceNumber = ByteUtil.readLeInt(buffer, offset + 16);
        descriptorCharset = EndianUtilities.toByteArray(buffer, offset + 20, 64);
        logicalVolumeIdentifier = UdfUtilities.readDString(buffer, offset + 84, 128);
        logicalBlockSize = ByteUtil.readLeInt(buffer, offset + 212);
        domainIdentifier = EndianUtilities.toStruct(DomainEntityIdentifier.class, buffer, offset + 216);
        logicalVolumeContentsUse = EndianUtilities.toByteArray(buffer, offset + 248, 16);
        mapTableLength = ByteUtil.readLeInt(buffer, offset + 264);
        numPartitionMaps = ByteUtil.readLeInt(buffer, offset + 268);
        implementationIdentifier = EndianUtilities.toStruct(ImplementationEntityIdentifier.class, buffer, offset + 272);
        implementationUse = EndianUtilities.toByteArray(buffer, offset + 304, 128);
        integritySequenceExtent = new ExtentDescriptor();
        integritySequenceExtent.readFrom(buffer, offset + 432);
        int pmOffset = 0;
        partitionMaps = new PartitionMap[numPartitionMaps];
        for (int i = 0; i < numPartitionMaps; ++i) {
            partitionMaps[i] = PartitionMap.createFrom(buffer, offset + 440 + pmOffset);
            pmOffset += partitionMaps[i].size();
        }
        return 440 + mapTableLength;
    }
}
