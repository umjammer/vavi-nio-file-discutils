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

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class ExtendedFileEntry extends FileEntry implements IByteArraySerializable {
    public long CreationTime;

    public long ObjectSize;

    public LongAllocationDescriptor StreamDirectoryIcb;

    public int sizeOf() {
        throw new UnsupportedOperationException();
    }

    public int readFrom(byte[] buffer, int offset) {
        DescriptorTag = EndianUtilities.<DescriptorTag> toStruct(DescriptorTag.class, buffer, offset);
        InformationControlBlock = EndianUtilities
                .<InformationControlBlock> toStruct(InformationControlBlock.class, buffer, offset + 16);
        Uid = EndianUtilities.toUInt32LittleEndian(buffer, offset + 36);
        Gid = EndianUtilities.toUInt32LittleEndian(buffer, offset + 40);
        Permissions = FilePermissions.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 44));
        FileLinkCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 48);
        RecordFormat = buffer[offset + 50];
        RecordDisplayAttributes = buffer[offset + 51];
        RecordLength = EndianUtilities.toUInt16LittleEndian(buffer, offset + 52);
        InformationLength = EndianUtilities.toUInt64LittleEndian(buffer, offset + 56);
        ObjectSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 64);
        LogicalBlocksRecorded = EndianUtilities.toUInt64LittleEndian(buffer, offset + 72);
        AccessTime = UdfUtilities.parseTimestamp(buffer, offset + 80);
        ModificationTime = UdfUtilities.parseTimestamp(buffer, offset + 92);
        CreationTime = UdfUtilities.parseTimestamp(buffer, offset + 104);
        AttributeTime = UdfUtilities.parseTimestamp(buffer, offset + 116);
        Checkpoint = EndianUtilities.toUInt32LittleEndian(buffer, offset + 128);
        ExtendedAttributeIcb = EndianUtilities
                .<LongAllocationDescriptor> toStruct(LongAllocationDescriptor.class, buffer, offset + 136);
        StreamDirectoryIcb = EndianUtilities
                .<LongAllocationDescriptor> toStruct(LongAllocationDescriptor.class, buffer, offset + 152);
        ImplementationIdentifier = EndianUtilities
                .<ImplementationEntityIdentifier> toStruct(ImplementationEntityIdentifier.class, buffer, offset + 168);
        UniqueId = EndianUtilities.toUInt64LittleEndian(buffer, offset + 200);
        ExtendedAttributesLength = EndianUtilities.toInt32LittleEndian(buffer, offset + 208);
        AllocationDescriptorsLength = EndianUtilities.toInt32LittleEndian(buffer, offset + 212);
        AllocationDescriptors = EndianUtilities
                .toByteArray(buffer, offset + 216 + ExtendedAttributesLength, AllocationDescriptorsLength);
        byte[] eaData = EndianUtilities.toByteArray(buffer, offset + 216, ExtendedAttributesLength);
        ExtendedAttributes = readExtendedAttributes(eaData);
        return 216 + ExtendedAttributesLength + AllocationDescriptorsLength;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
