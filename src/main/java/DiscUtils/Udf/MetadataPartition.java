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

import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.IOException;


public class MetadataPartition extends LogicalPartition {
    private final File _metadataFile;

    @SuppressWarnings("unused")
    private MetadataPartitionMap _partitionMap;

    public MetadataPartition(UdfContext context, LogicalVolumeDescriptor volumeDescriptor, MetadataPartitionMap partitionMap) {
        super(context, volumeDescriptor);
        _partitionMap = partitionMap;
        PhysicalPartition physical = context.PhysicalPartitions.get(partitionMap.PartitionNumber);
        long fileEntryPos = partitionMap.MetadataFileLocation * (long) volumeDescriptor.LogicalBlockSize;
        byte[] entryData = StreamUtilities.readExact(physical.getContent(), fileEntryPos, _context.PhysicalSectorSize);
        if (!DescriptorTag.isValid(entryData, 0)) {
            throw new IOException("Invalid descriptor tag looking for Metadata file entry");
        }

        DescriptorTag dt = EndianUtilities.<DescriptorTag> toStruct(DescriptorTag.class, entryData, 0);
        if (dt._TagIdentifier == TagIdentifier.ExtendedFileEntry) {
            ExtendedFileEntry efe = EndianUtilities.<ExtendedFileEntry> toStruct(ExtendedFileEntry.class, entryData, 0);
            _metadataFile = new File(context, physical, efe, _volumeDescriptor.LogicalBlockSize);
        } else {
            throw new UnsupportedOperationException("Only EFE implemented for Metadata file entry");
        }
    }

    public IBuffer getContent() {
        return _metadataFile.getFileContent();
    }
}
