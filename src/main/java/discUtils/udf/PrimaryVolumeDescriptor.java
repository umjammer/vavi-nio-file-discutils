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

import discUtils.streams.util.EndianUtilities;


public final class PrimaryVolumeDescriptor extends TaggedDescriptor<PrimaryVolumeDescriptor> {

    public EntityIdentifier applicationIdentifier;

    public int characterSetList;

    public CharacterSetSpecification descriptorCharSet;

    public CharacterSetSpecification explanatoryCharSet;

    public short flags;

    public EntityIdentifier implementationIdentifier;

    public byte[] implementationUse;

    public short interchangeLevel;

    public int maxCharacterSetList;

    public short maxInterchangeLevel;

    public short maxVolumeSquenceNumber;

    public int predecessorVolumeDescriptorSequenceLocation;

    public int primaryVolumeDescriptorNumber;

    public long recordingTime;

    public ExtentDescriptor volumeAbstractExtent;

    public ExtentDescriptor volumeCopyrightNoticeExtent;

    public int volumeDescriptorSequenceNumber;

    public String volumeIdentifier;

    public short volumeSequenceNumber;

    public String volumeSetIdentifier;

    public PrimaryVolumeDescriptor() {
        super(TagIdentifier.PrimaryVolumeDescriptor);
    }

    public int parse(byte[] buffer, int offset) {
        volumeDescriptorSequenceNumber = EndianUtilities.toUInt32LittleEndian(buffer, offset + 16);
        primaryVolumeDescriptorNumber = EndianUtilities.toUInt32LittleEndian(buffer, offset + 20);
        volumeIdentifier = UdfUtilities.readDString(buffer, offset + 24, 32);
        volumeSequenceNumber = EndianUtilities.toUInt16LittleEndian(buffer, offset + 56);
        maxVolumeSquenceNumber = EndianUtilities.toUInt16LittleEndian(buffer, offset + 58);
        interchangeLevel = EndianUtilities.toUInt16LittleEndian(buffer, offset + 60);
        maxInterchangeLevel = EndianUtilities.toUInt16LittleEndian(buffer, offset + 62);
        characterSetList = EndianUtilities.toUInt32LittleEndian(buffer, offset + 64);
        maxCharacterSetList = EndianUtilities.toUInt32LittleEndian(buffer, offset + 68);
        volumeSetIdentifier = UdfUtilities.readDString(buffer, offset + 72, 128);
        descriptorCharSet = EndianUtilities
                .toStruct(CharacterSetSpecification.class, buffer, offset + 200);
        explanatoryCharSet = EndianUtilities
                .toStruct(CharacterSetSpecification.class, buffer, offset + 264);
        volumeAbstractExtent = new ExtentDescriptor();
        volumeAbstractExtent.readFrom(buffer, offset + 328);
        volumeCopyrightNoticeExtent = new ExtentDescriptor();
        volumeCopyrightNoticeExtent.readFrom(buffer, offset + 336);
        applicationIdentifier = EndianUtilities
                .toStruct(ApplicationEntityIdentifier.class, buffer, offset + 344);
        recordingTime = UdfUtilities.parseTimestamp(buffer, offset + 376);
        implementationIdentifier = EndianUtilities
                .toStruct(ImplementationEntityIdentifier.class, buffer, offset + 388);
        implementationUse = EndianUtilities.toByteArray(buffer, offset + 420, 64);
        predecessorVolumeDescriptorSequenceLocation = EndianUtilities.toUInt32LittleEndian(buffer, offset + 484);
        flags = EndianUtilities.toUInt16LittleEndian(buffer, offset + 488);
        return 512;
    }
}
