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

import discUtils.streams.buffer.IBuffer;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.IOException;


public class MetadataPartition extends LogicalPartition {

    private final File metadataFile;

    @SuppressWarnings("unused")
    private MetadataPartitionMap partitionMap;

    public MetadataPartition(UdfContext context, LogicalVolumeDescriptor volumeDescriptor, MetadataPartitionMap partitionMap) {
        super(context, volumeDescriptor);
        this.partitionMap = partitionMap;
        PhysicalPartition physical = context.physicalPartitions.get(partitionMap.partitionNumber);
        long fileEntryPos = partitionMap.metadataFileLocation * (long) volumeDescriptor.logicalBlockSize;
        byte[] entryData = StreamUtilities.readExact(physical.getContent(), fileEntryPos, this.context.physicalSectorSize);
        if (!DescriptorTag.isValid(entryData, 0)) {
            throw new IOException("Invalid descriptor tag looking for Metadata file entry");
        }

        DescriptorTag dt = EndianUtilities.toStruct(DescriptorTag.class, entryData, 0);
        if (dt.tagIdentifier == TagIdentifier.ExtendedFileEntry) {
            ExtendedFileEntry efe = EndianUtilities.toStruct(ExtendedFileEntry.class, entryData, 0);
            metadataFile = new File(context, physical, efe, this.volumeDescriptor.logicalBlockSize);
        } else {
            throw new UnsupportedOperationException("Only EFE implemented for Metadata file entry");
        }
    }

    @Override
    public IBuffer getContent() {
        return metadataFile.getFileContent();
    }
}
