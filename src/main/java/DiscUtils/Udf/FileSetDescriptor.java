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


public class FileSetDescriptor implements IByteArraySerializable {
    public String AbstractFileIdentifier;

    public int CharacterSetList;

    public String CopyrightFileIdentifier;

    public DescriptorTag DescriptorTag;

    public DomainEntityIdentifier DomainIdentifier;

    public CharacterSetSpecification FileSetCharset;

    public int FileSetDescriptorNumber;

    public String FileSetIdentifier;

    public int FileSetNumber;

    public short InterchangeLevel;

    public String LogicalVolumeIdentifier;

    public CharacterSetSpecification LogicalVolumeIdentifierCharset;

    public int MaximumCharacterSetList;

    public short MaximumInterchangeLevel;

    public LongAllocationDescriptor NextExtent;

    public long RecordingTime;

    public LongAllocationDescriptor RootDirectoryIcb;

    public LongAllocationDescriptor SystemStreamDirectoryIcb;

    public long getSize() {
        return 512;
    }

    public int readFrom(byte[] buffer, int offset) {
        DescriptorTag = EndianUtilities.<DescriptorTag> toStruct(DescriptorTag.class, buffer, offset);
        RecordingTime = UdfUtilities.parseTimestamp(buffer, offset + 16);
        InterchangeLevel = (short) EndianUtilities.toUInt16LittleEndian(buffer, offset + 28);
        MaximumInterchangeLevel = (short) EndianUtilities.toUInt16LittleEndian(buffer, offset + 30);
        CharacterSetList = EndianUtilities.toUInt32LittleEndian(buffer, offset + 32);
        MaximumCharacterSetList = EndianUtilities.toUInt32LittleEndian(buffer, offset + 36);
        FileSetNumber = EndianUtilities.toUInt32LittleEndian(buffer, offset + 40);
        FileSetDescriptorNumber = EndianUtilities.toUInt32LittleEndian(buffer, offset + 44);
        LogicalVolumeIdentifierCharset = EndianUtilities
                .<CharacterSetSpecification> toStruct(CharacterSetSpecification.class, buffer, offset + 48);
        LogicalVolumeIdentifier = UdfUtilities.readDString(buffer, offset + 112, 128);
        FileSetCharset = EndianUtilities
                .<CharacterSetSpecification> toStruct(CharacterSetSpecification.class, buffer, offset + 240);
        FileSetIdentifier = UdfUtilities.readDString(buffer, offset + 304, 32);
        CopyrightFileIdentifier = UdfUtilities.readDString(buffer, offset + 336, 32);
        AbstractFileIdentifier = UdfUtilities.readDString(buffer, offset + 368, 32);
        RootDirectoryIcb = EndianUtilities
                .<LongAllocationDescriptor> toStruct(LongAllocationDescriptor.class, buffer, offset + 400);
        DomainIdentifier = EndianUtilities
                .<DomainEntityIdentifier> toStruct(DomainEntityIdentifier.class, buffer, offset + 416);
        NextExtent = EndianUtilities.<LongAllocationDescriptor> toStruct(LongAllocationDescriptor.class, buffer, offset + 448);
        SystemStreamDirectoryIcb = EndianUtilities
                .<LongAllocationDescriptor> toStruct(LongAllocationDescriptor.class, buffer, offset + 464);
        return 512;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
