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

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public class ExtendedFileEntry extends FileEntry implements IByteArraySerializable {

    public long creationTime;

    public long objectSize;

    public LongAllocationDescriptor streamDirectoryIcb;

    public int size() {
        throw new UnsupportedOperationException();
    }

    public int readFrom(byte[] buffer, int offset) {
        descriptorTag = EndianUtilities.toStruct(DescriptorTag.class, buffer, offset);
        informationControlBlock = EndianUtilities
                .toStruct(InformationControlBlock.class, buffer, offset + 16);
        uid = EndianUtilities.toUInt32LittleEndian(buffer, offset + 36);
        gid = EndianUtilities.toUInt32LittleEndian(buffer, offset + 40);
        permissions = FilePermissions.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 44));
        fileLinkCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 48);
        recordFormat = buffer[offset + 50];
        recordDisplayAttributes = buffer[offset + 51];
        recordLength = EndianUtilities.toUInt16LittleEndian(buffer, offset + 52);
        informationLength = EndianUtilities.toUInt64LittleEndian(buffer, offset + 56);
        objectSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 64);
        logicalBlocksRecorded = EndianUtilities.toUInt64LittleEndian(buffer, offset + 72);
        accessTime = UdfUtilities.parseTimestamp(buffer, offset + 80);
        modificationTime = UdfUtilities.parseTimestamp(buffer, offset + 92);
        creationTime = UdfUtilities.parseTimestamp(buffer, offset + 104);
        attributeTime = UdfUtilities.parseTimestamp(buffer, offset + 116);
        checkpoint = EndianUtilities.toUInt32LittleEndian(buffer, offset + 128);
        extendedAttributeIcb = EndianUtilities
                .toStruct(LongAllocationDescriptor.class, buffer, offset + 136);
        streamDirectoryIcb = EndianUtilities
                .toStruct(LongAllocationDescriptor.class, buffer, offset + 152);
        implementationIdentifier = EndianUtilities
                .toStruct(ImplementationEntityIdentifier.class, buffer, offset + 168);
        uniqueId = EndianUtilities.toUInt64LittleEndian(buffer, offset + 200);
        extendedAttributesLength = EndianUtilities.toInt32LittleEndian(buffer, offset + 208);
        allocationDescriptorsLength = EndianUtilities.toInt32LittleEndian(buffer, offset + 212);
        allocationDescriptors = EndianUtilities
                .toByteArray(buffer, offset + 216 + extendedAttributesLength, allocationDescriptorsLength);
        byte[] eaData = EndianUtilities.toByteArray(buffer, offset + 216, extendedAttributesLength);
        extendedAttributes = readExtendedAttributes(eaData);
        return 216 + extendedAttributesLength + allocationDescriptorsLength;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
