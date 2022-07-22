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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public class FileEntry implements IByteArraySerializable {
    public long accessTime;

    public byte[] allocationDescriptors;

    public int allocationDescriptorsLength;

    public long attributeTime;

    public int checkpoint;

    public DescriptorTag descriptorTag;

    public LongAllocationDescriptor extendedAttributeIcb;

    public List<ExtendedAttributeRecord> extendedAttributes;

    public int extendedAttributesLength;

    public short fileLinkCount;

    public int gid;

    public ImplementationEntityIdentifier implementationIdentifier;

    public InformationControlBlock informationControlBlock;

    public long informationLength;

    public long logicalBlocksRecorded;

    public long modificationTime;

    public EnumSet<FilePermissions> permissions;

    public byte recordDisplayAttributes;

    public byte recordFormat;

    public int recordLength;

    public int uid;

    public long uniqueId;

    public int size() {
        throw new UnsupportedOperationException();
    }

    public int readFrom(byte[] buffer, int offset) {
        descriptorTag = EndianUtilities.toStruct(DescriptorTag.class, buffer, offset);
        informationControlBlock = EndianUtilities.toStruct(InformationControlBlock.class, buffer, offset + 16);
        uid = EndianUtilities.toUInt32LittleEndian(buffer, offset + 36);
        gid = EndianUtilities.toUInt32LittleEndian(buffer, offset + 40);
        permissions = FilePermissions.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 44));
        fileLinkCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 48);
        recordFormat = buffer[offset + 50];
        recordDisplayAttributes = buffer[offset + 51];
        recordLength = EndianUtilities.toUInt16LittleEndian(buffer, offset + 52);
        informationLength = EndianUtilities.toUInt64LittleEndian(buffer, offset + 56);
        logicalBlocksRecorded = EndianUtilities.toUInt64LittleEndian(buffer, offset + 64);
        accessTime = UdfUtilities.parseTimestamp(buffer, offset + 72);
        modificationTime = UdfUtilities.parseTimestamp(buffer, offset + 84);
        attributeTime = UdfUtilities.parseTimestamp(buffer, offset + 96);
        checkpoint = EndianUtilities.toUInt32LittleEndian(buffer, offset + 108);
        extendedAttributeIcb = EndianUtilities.toStruct(LongAllocationDescriptor.class, buffer, offset + 112);
        implementationIdentifier = EndianUtilities.toStruct(ImplementationEntityIdentifier.class, buffer, offset + 128);
        uniqueId = EndianUtilities.toUInt64LittleEndian(buffer, offset + 160);
        extendedAttributesLength = EndianUtilities.toInt32LittleEndian(buffer, offset + 168);
        allocationDescriptorsLength = EndianUtilities.toInt32LittleEndian(buffer, offset + 172);
        allocationDescriptors = EndianUtilities
                .toByteArray(buffer, offset + 176 + extendedAttributesLength, allocationDescriptorsLength);
        byte[] eaData = EndianUtilities.toByteArray(buffer, offset + 176, extendedAttributesLength);
        extendedAttributes = readExtendedAttributes(eaData);
        return 176 + extendedAttributesLength + allocationDescriptorsLength;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    protected static List<ExtendedAttributeRecord> readExtendedAttributes(byte[] eaData) {
        if (eaData != null && eaData.length != 0) {
            DescriptorTag eaTag = new DescriptorTag();
            eaTag.readFrom(eaData, 0);
            int implAttrLocation = EndianUtilities.toInt32LittleEndian(eaData, 16);
            @SuppressWarnings("unused")
            int appAttrLocation = EndianUtilities.toInt32LittleEndian(eaData, 20);
            List<ExtendedAttributeRecord> extendedAttrs = new ArrayList<>();
            int pos = 24;
            while (pos < eaData.length) {
                ExtendedAttributeRecord ea;
                if (pos >= implAttrLocation) {
                    ea = new ImplementationUseExtendedAttributeRecord();
                } else {
                    ea = new ExtendedAttributeRecord();
                }
                int numRead = ea.readFrom(eaData, pos);
                extendedAttrs.add(ea);
                pos += numRead;
            }
            return extendedAttrs;
        }

        return null;
    }
}
