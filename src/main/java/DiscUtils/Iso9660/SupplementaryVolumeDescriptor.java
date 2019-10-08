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

package DiscUtils.Iso9660;

import java.nio.charset.Charset;

import DiscUtils.Core.Internal.Utilities;


public class SupplementaryVolumeDescriptor extends CommonVolumeDescriptor {
    public SupplementaryVolumeDescriptor(byte[] src, int offset) {
        super(src, offset, IsoUtilities.encodingFromBytes(src, offset + 88));
    }

    public SupplementaryVolumeDescriptor(int volumeSpaceSize,
            int pathTableSize,
            int typeLPathTableLocation,
            int typeMPathTableLocation,
            int rootDirExtentLocation,
            int rootDirDataLength,
            long buildTime,
            Charset enc) {
        super(VolumeDescriptorType.Supplementary,
              (byte) 1,
              volumeSpaceSize,
              pathTableSize,
              typeLPathTableLocation,
              typeMPathTableLocation,
              rootDirExtentLocation,
              rootDirDataLength,
              buildTime,
              enc);
    }

    public void writeTo(byte[] buffer, int offset) {
        super.writeTo(buffer, offset);
        IsoUtilities.writeA1Chars(buffer, offset + 8, 32, SystemIdentifier, CharacterEncoding);
        IsoUtilities.writeString(buffer, offset + 40, 32, true, VolumeIdentifier, CharacterEncoding, true);
        IsoUtilities.toBothFromUInt32(buffer, offset + 80, VolumeSpaceSize);
        IsoUtilities.encodingToBytes(CharacterEncoding, buffer, offset + 88);
        IsoUtilities.toBothFromUInt16(buffer, offset + 120, VolumeSetSize);
        IsoUtilities.toBothFromUInt16(buffer, offset + 124, VolumeSequenceNumber);
        IsoUtilities.toBothFromUInt16(buffer, offset + 128, LogicalBlockSize);
        IsoUtilities.toBothFromUInt32(buffer, offset + 132, PathTableSize);
        IsoUtilities.toBytesFromUInt32(buffer, offset + 140, TypeLPathTableLocation);
        IsoUtilities.toBytesFromUInt32(buffer, offset + 144, OptionalTypeLPathTableLocation);
        IsoUtilities.toBytesFromUInt32(buffer, offset + 148, Utilities.bitSwap(TypeMPathTableLocation));
        IsoUtilities.toBytesFromUInt32(buffer, offset + 152, Utilities.bitSwap(OptionalTypeMPathTableLocation));
        RootDirectory.writeTo(buffer, offset + 156, CharacterEncoding);
        IsoUtilities.writeD1Chars(buffer, offset + 190, 129, VolumeSetIdentifier, CharacterEncoding);
        IsoUtilities.writeA1Chars(buffer, offset + 318, 129, PublisherIdentifier, CharacterEncoding);
        IsoUtilities.writeA1Chars(buffer, offset + 446, 129, DataPreparerIdentifier, CharacterEncoding);
        IsoUtilities.writeA1Chars(buffer, offset + 574, 129, ApplicationIdentifier, CharacterEncoding);
        IsoUtilities.writeD1Chars(buffer, offset + 702, 37, CopyrightFileIdentifier, CharacterEncoding);
        // FIXME!!
        IsoUtilities.writeD1Chars(buffer, offset + 739, 37, AbstractFileIdentifier, CharacterEncoding);
        // FIXME!!
        IsoUtilities.writeD1Chars(buffer, offset + 776, 37, BibliographicFileIdentifier, CharacterEncoding);
        // FIXME!!
        IsoUtilities.toVolumeDescriptorTimeFromUTC(buffer, offset + 813, CreationDateAndTime);
        IsoUtilities.toVolumeDescriptorTimeFromUTC(buffer, offset + 830, ModificationDateAndTime);
        IsoUtilities.toVolumeDescriptorTimeFromUTC(buffer, offset + 847, ExpirationDateAndTime);
        IsoUtilities.toVolumeDescriptorTimeFromUTC(buffer, offset + 864, EffectiveDateAndTime);
        buffer[offset + 881] = FileStructureVersion;
    }
}
