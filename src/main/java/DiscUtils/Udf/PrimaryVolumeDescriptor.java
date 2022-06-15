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

import DiscUtils.Streams.Util.EndianUtilities;


public final class PrimaryVolumeDescriptor extends TaggedDescriptor<PrimaryVolumeDescriptor> {
    public EntityIdentifier ApplicationIdentifier;

    public int CharacterSetList;

    public CharacterSetSpecification DescriptorCharSet;

    public CharacterSetSpecification ExplanatoryCharSet;

    public short Flags;

    public EntityIdentifier ImplementationIdentifier;

    public byte[] ImplementationUse;

    public short InterchangeLevel;

    public int MaxCharacterSetList;

    public short MaxInterchangeLevel;

    public short MaxVolumeSquenceNumber;

    public int PredecessorVolumeDescriptorSequenceLocation;

    public int PrimaryVolumeDescriptorNumber;

    public long RecordingTime;

    public ExtentDescriptor VolumeAbstractExtent;

    public ExtentDescriptor VolumeCopyrightNoticeExtent;

    public int VolumeDescriptorSequenceNumber;

    public String VolumeIdentifier;

    public short VolumeSequenceNumber;

    public String VolumeSetIdentifier;

    public PrimaryVolumeDescriptor() {
        super(TagIdentifier.PrimaryVolumeDescriptor);
    }

    public int parse(byte[] buffer, int offset) {
        VolumeDescriptorSequenceNumber = EndianUtilities.toUInt32LittleEndian(buffer, offset + 16);
        PrimaryVolumeDescriptorNumber = EndianUtilities.toUInt32LittleEndian(buffer, offset + 20);
        VolumeIdentifier = UdfUtilities.readDString(buffer, offset + 24, 32);
        VolumeSequenceNumber = EndianUtilities.toUInt16LittleEndian(buffer, offset + 56);
        MaxVolumeSquenceNumber = EndianUtilities.toUInt16LittleEndian(buffer, offset + 58);
        InterchangeLevel = EndianUtilities.toUInt16LittleEndian(buffer, offset + 60);
        MaxInterchangeLevel = EndianUtilities.toUInt16LittleEndian(buffer, offset + 62);
        CharacterSetList = EndianUtilities.toUInt32LittleEndian(buffer, offset + 64);
        MaxCharacterSetList = EndianUtilities.toUInt32LittleEndian(buffer, offset + 68);
        VolumeSetIdentifier = UdfUtilities.readDString(buffer, offset + 72, 128);
        DescriptorCharSet = EndianUtilities
                .toStruct(CharacterSetSpecification.class, buffer, offset + 200);
        ExplanatoryCharSet = EndianUtilities
                .toStruct(CharacterSetSpecification.class, buffer, offset + 264);
        VolumeAbstractExtent = new ExtentDescriptor();
        VolumeAbstractExtent.readFrom(buffer, offset + 328);
        VolumeCopyrightNoticeExtent = new ExtentDescriptor();
        VolumeCopyrightNoticeExtent.readFrom(buffer, offset + 336);
        ApplicationIdentifier = EndianUtilities
                .toStruct(ApplicationEntityIdentifier.class, buffer, offset + 344);
        RecordingTime = UdfUtilities.parseTimestamp(buffer, offset + 376);
        ImplementationIdentifier = EndianUtilities
                .toStruct(ImplementationEntityIdentifier.class, buffer, offset + 388);
        ImplementationUse = EndianUtilities.toByteArray(buffer, offset + 420, 64);
        PredecessorVolumeDescriptorSequenceLocation = EndianUtilities.toUInt32LittleEndian(buffer, offset + 484);
        Flags = EndianUtilities.toUInt16LittleEndian(buffer, offset + 488);
        return 512;
    }
}
