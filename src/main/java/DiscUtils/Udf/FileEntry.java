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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class FileEntry implements IByteArraySerializable {
    public long AccessTime;

    public byte[] AllocationDescriptors;

    public int AllocationDescriptorsLength;

    public long AttributeTime;

    public int Checkpoint;

    public DescriptorTag DescriptorTag;

    public LongAllocationDescriptor ExtendedAttributeIcb;

    public List<ExtendedAttributeRecord> ExtendedAttributes;

    public int ExtendedAttributesLength;

    public short FileLinkCount;

    public int Gid;

    public ImplementationEntityIdentifier ImplementationIdentifier;

    public InformationControlBlock InformationControlBlock;

    public long InformationLength;

    public long LogicalBlocksRecorded;

    public long ModificationTime;

    public EnumSet<FilePermissions> Permissions;

    public byte RecordDisplayAttributes;

    public byte RecordFormat;

    public int RecordLength;

    public int Uid;

    public long UniqueId;

    public int size() {
        throw new UnsupportedOperationException();
    }

    public int readFrom(byte[] buffer, int offset) {
        DescriptorTag = EndianUtilities.toStruct(DescriptorTag.class, buffer, offset);
        InformationControlBlock = EndianUtilities.toStruct(InformationControlBlock.class, buffer, offset + 16);
        Uid = EndianUtilities.toUInt32LittleEndian(buffer, offset + 36);
        Gid = EndianUtilities.toUInt32LittleEndian(buffer, offset + 40);
        Permissions = FilePermissions.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 44));
        FileLinkCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 48);
        RecordFormat = buffer[offset + 50];
        RecordDisplayAttributes = buffer[offset + 51];
        RecordLength = EndianUtilities.toUInt16LittleEndian(buffer, offset + 52);
        InformationLength = EndianUtilities.toUInt64LittleEndian(buffer, offset + 56);
        LogicalBlocksRecorded = EndianUtilities.toUInt64LittleEndian(buffer, offset + 64);
        AccessTime = UdfUtilities.parseTimestamp(buffer, offset + 72);
        ModificationTime = UdfUtilities.parseTimestamp(buffer, offset + 84);
        AttributeTime = UdfUtilities.parseTimestamp(buffer, offset + 96);
        Checkpoint = EndianUtilities.toUInt32LittleEndian(buffer, offset + 108);
        ExtendedAttributeIcb = EndianUtilities.toStruct(LongAllocationDescriptor.class, buffer, offset + 112);
        ImplementationIdentifier = EndianUtilities.toStruct(ImplementationEntityIdentifier.class, buffer, offset + 128);
        UniqueId = EndianUtilities.toUInt64LittleEndian(buffer, offset + 160);
        ExtendedAttributesLength = EndianUtilities.toInt32LittleEndian(buffer, offset + 168);
        AllocationDescriptorsLength = EndianUtilities.toInt32LittleEndian(buffer, offset + 172);
        AllocationDescriptors = EndianUtilities
                .toByteArray(buffer, offset + 176 + ExtendedAttributesLength, AllocationDescriptorsLength);
        byte[] eaData = EndianUtilities.toByteArray(buffer, offset + 176, ExtendedAttributesLength);
        ExtendedAttributes = readExtendedAttributes(eaData);
        return 176 + ExtendedAttributesLength + AllocationDescriptorsLength;
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
