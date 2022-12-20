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
import vavi.util.ByteUtil;


public class ExtendedFileEntry extends FileEntry implements IByteArraySerializable {

    public long creationTime;

    public long objectSize;

    public LongAllocationDescriptor streamDirectoryIcb;

    @Override public int size() {
        throw new UnsupportedOperationException();
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        descriptorTag = EndianUtilities.toStruct(DescriptorTag.class, buffer, offset);
        informationControlBlock = EndianUtilities
                .toStruct(InformationControlBlock.class, buffer, offset + 16);
        uid = ByteUtil.readLeInt(buffer, offset + 36);
        gid = ByteUtil.readLeInt(buffer, offset + 40);
        permissions = FilePermissions.valueOf(ByteUtil.readLeInt(buffer, offset + 44));
        fileLinkCount = ByteUtil.readLeShort(buffer, offset + 48);
        recordFormat = buffer[offset + 50];
        recordDisplayAttributes = buffer[offset + 51];
        recordLength = ByteUtil.readLeShort(buffer, offset + 52);
        informationLength = ByteUtil.readLeLong(buffer, offset + 56);
        objectSize = ByteUtil.readLeLong(buffer, offset + 64);
        logicalBlocksRecorded = ByteUtil.readLeLong(buffer, offset + 72);
        accessTime = UdfUtilities.parseTimestamp(buffer, offset + 80);
        modificationTime = UdfUtilities.parseTimestamp(buffer, offset + 92);
        creationTime = UdfUtilities.parseTimestamp(buffer, offset + 104);
        attributeTime = UdfUtilities.parseTimestamp(buffer, offset + 116);
        checkpoint = ByteUtil.readLeInt(buffer, offset + 128);
        extendedAttributeIcb = EndianUtilities
                .toStruct(LongAllocationDescriptor.class, buffer, offset + 136);
        streamDirectoryIcb = EndianUtilities
                .toStruct(LongAllocationDescriptor.class, buffer, offset + 152);
        implementationIdentifier = EndianUtilities
                .toStruct(ImplementationEntityIdentifier.class, buffer, offset + 168);
        uniqueId = ByteUtil.readLeLong(buffer, offset + 200);
        extendedAttributesLength = ByteUtil.readLeInt(buffer, offset + 208);
        allocationDescriptorsLength = ByteUtil.readLeInt(buffer, offset + 212);
        allocationDescriptors = EndianUtilities
                .toByteArray(buffer, offset + 216 + extendedAttributesLength, allocationDescriptorsLength);
        byte[] eaData = EndianUtilities.toByteArray(buffer, offset + 216, extendedAttributesLength);
        extendedAttributes = readExtendedAttributes(eaData);
        return 216 + extendedAttributesLength + allocationDescriptorsLength;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
