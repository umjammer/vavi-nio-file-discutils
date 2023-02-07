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


public class FileSetDescriptor implements IByteArraySerializable {

    public String abstractFileIdentifier;

    public int characterSetList;

    public String copyrightFileIdentifier;

    public DescriptorTag descriptorTag;

    public DomainEntityIdentifier domainIdentifier;

    public CharacterSetSpecification fileSetCharset;

    public int fileSetDescriptorNumber;

    public String fileSetIdentifier;

    public int fileSetNumber;

    public short interchangeLevel;

    public String logicalVolumeIdentifier;

    public CharacterSetSpecification logicalVolumeIdentifierCharset;

    public int maximumCharacterSetList;

    public short maximumInterchangeLevel;

    public LongAllocationDescriptor nextExtent;

    public long recordingTime;

    public LongAllocationDescriptor rootDirectoryIcb;

    public LongAllocationDescriptor systemStreamDirectoryIcb;

    @Override public int size() {
        return 512;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        descriptorTag = EndianUtilities.toStruct(DescriptorTag.class, buffer, offset);
        recordingTime = UdfUtilities.parseTimestamp(buffer, offset + 16);
        interchangeLevel = ByteUtil.readLeShort(buffer, offset + 28);
        maximumInterchangeLevel = ByteUtil.readLeShort(buffer, offset + 30);
        characterSetList = ByteUtil.readLeInt(buffer, offset + 32);
        maximumCharacterSetList = ByteUtil.readLeInt(buffer, offset + 36);
        fileSetNumber = ByteUtil.readLeInt(buffer, offset + 40);
        fileSetDescriptorNumber = ByteUtil.readLeInt(buffer, offset + 44);
        logicalVolumeIdentifierCharset = EndianUtilities
                .toStruct(CharacterSetSpecification.class, buffer, offset + 48);
        logicalVolumeIdentifier = UdfUtilities.readDString(buffer, offset + 112, 128);
        fileSetCharset = EndianUtilities
                .toStruct(CharacterSetSpecification.class, buffer, offset + 240);
        fileSetIdentifier = UdfUtilities.readDString(buffer, offset + 304, 32);
        copyrightFileIdentifier = UdfUtilities.readDString(buffer, offset + 336, 32);
        abstractFileIdentifier = UdfUtilities.readDString(buffer, offset + 368, 32);
        rootDirectoryIcb = EndianUtilities
                .toStruct(LongAllocationDescriptor.class, buffer, offset + 400);
        domainIdentifier = EndianUtilities
                .toStruct(DomainEntityIdentifier.class, buffer, offset + 416);
        nextExtent = EndianUtilities.toStruct(LongAllocationDescriptor.class, buffer, offset + 448);
        systemStreamDirectoryIcb = EndianUtilities
                .toStruct(LongAllocationDescriptor.class, buffer, offset + 464);
        return 512;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
