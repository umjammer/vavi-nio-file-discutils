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


public final class LogicalVolumeDescriptor extends TaggedDescriptor<LogicalVolumeDescriptor> {
    public byte[] DescriptorCharset;

    public EntityIdentifier DomainIdentifier;

    public EntityIdentifier ImplementationIdentifier;

    public byte[] ImplementationUse;

    public ExtentDescriptor IntegritySequenceExtent;

    public int LogicalBlockSize;

    public byte[] LogicalVolumeContentsUse;

    public String LogicalVolumeIdentifier;

    public int MapTableLength;

    public int NumPartitionMaps;

    public PartitionMap[] PartitionMaps;

    public int VolumeDescriptorSequenceNumber;

    public LogicalVolumeDescriptor() {
        super(TagIdentifier.LogicalVolumeDescriptor);
    }

    public LongAllocationDescriptor getFileSetDescriptorLocation() {
        LongAllocationDescriptor lad = new LongAllocationDescriptor();
        lad.readFrom(LogicalVolumeContentsUse, 0);
        return lad;
    }

    public int parse(byte[] buffer, int offset) {
        VolumeDescriptorSequenceNumber = EndianUtilities.toUInt32LittleEndian(buffer, offset + 16);
        DescriptorCharset = EndianUtilities.toByteArray(buffer, offset + 20, 64);
        LogicalVolumeIdentifier = UdfUtilities.readDString(buffer, offset + 84, 128);
        LogicalBlockSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 212);
        DomainIdentifier = EndianUtilities.<DomainEntityIdentifier> toStruct(DomainEntityIdentifier.class, buffer, offset + 216);
        LogicalVolumeContentsUse = EndianUtilities.toByteArray(buffer, offset + 248, 16);
        MapTableLength = EndianUtilities.toUInt32LittleEndian(buffer, offset + 264);
        NumPartitionMaps = EndianUtilities.toUInt32LittleEndian(buffer, offset + 268);
        ImplementationIdentifier = EndianUtilities.<ImplementationEntityIdentifier> toStruct(ImplementationEntityIdentifier.class, buffer, offset + 272);
        ImplementationUse = EndianUtilities.toByteArray(buffer, offset + 304, 128);
        IntegritySequenceExtent = new ExtentDescriptor();
        IntegritySequenceExtent.readFrom(buffer, offset + 432);
        int pmOffset = 0;
        PartitionMaps = new PartitionMap[NumPartitionMaps];
        for (int i = 0; i < NumPartitionMaps; ++i) {
            PartitionMaps[i] = PartitionMap.createFrom(buffer, offset + 440 + pmOffset);
            pmOffset += PartitionMaps[i].getSize();
        }
        return 440 + MapTableLength;
    }
}
