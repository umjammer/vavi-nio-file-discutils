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


public class PrimaryVolumeDescriptor extends CommonVolumeDescriptor {
    public PrimaryVolumeDescriptor(byte[] src, int offset) {
        super(src, offset, Charset.forName("ASCII"));
    }

    public PrimaryVolumeDescriptor(int volumeSpaceSize,
            int pathTableSize,
            int typeLPathTableLocation,
            int typeMPathTableLocation,
            int rootDirExtentLocation,
            int rootDirDataLength,
            long buildTime) {
        super(VolumeDescriptorType.Primary,
              (byte) 1,
              volumeSpaceSize,
              pathTableSize,
              typeLPathTableLocation,
              typeMPathTableLocation,
              rootDirExtentLocation,
              rootDirDataLength,
              buildTime,
              Charset.forName("ASCII"));
    }

    public void writeTo(byte[] buffer, int offset) {
        super.writeTo(buffer, offset);
        IsoUtilities.writeAChars(buffer, offset + 8, 32, SystemIdentifier);
        IsoUtilities.writeString(buffer, offset + 40, 32, true, VolumeIdentifier, Charset.forName("ASCII"), true);
        IsoUtilities.toBothFromUInt32(buffer, offset + 80, VolumeSpaceSize);
        IsoUtilities.toBothFromUInt16(buffer, offset + 120, VolumeSetSize);
        IsoUtilities.toBothFromUInt16(buffer, offset + 124, VolumeSequenceNumber);
        IsoUtilities.toBothFromUInt16(buffer, offset + 128, LogicalBlockSize);
        IsoUtilities.toBothFromUInt32(buffer, offset + 132, PathTableSize);
        IsoUtilities.toBytesFromUInt32(buffer, offset + 140, TypeLPathTableLocation);
        IsoUtilities.toBytesFromUInt32(buffer, offset + 144, OptionalTypeLPathTableLocation);
        IsoUtilities.toBytesFromUInt32(buffer, offset + 148, Utilities.bitSwap(TypeMPathTableLocation));
        IsoUtilities.toBytesFromUInt32(buffer, offset + 152, Utilities.bitSwap(OptionalTypeMPathTableLocation));
        RootDirectory.writeTo(buffer, offset + 156, Charset.forName("ASCII"));
        IsoUtilities.writeDChars(buffer, offset + 190, 129, VolumeSetIdentifier);
        IsoUtilities.writeAChars(buffer, offset + 318, 129, PublisherIdentifier);
        IsoUtilities.writeAChars(buffer, offset + 446, 129, DataPreparerIdentifier);
        IsoUtilities.writeAChars(buffer, offset + 574, 129, ApplicationIdentifier);
        IsoUtilities.writeDChars(buffer, offset + 702, 37, CopyrightFileIdentifier); // FIXME!!
        IsoUtilities.writeDChars(buffer, offset + 739, 37, AbstractFileIdentifier); // FIXME!!
        IsoUtilities.writeDChars(buffer, offset + 776, 37, BibliographicFileIdentifier); // FIXME!!
        IsoUtilities.toVolumeDescriptorTimeFromUTC(buffer, offset + 813, CreationDateAndTime);
        IsoUtilities.toVolumeDescriptorTimeFromUTC(buffer, offset + 830, ModificationDateAndTime);
        IsoUtilities.toVolumeDescriptorTimeFromUTC(buffer, offset + 847, ExpirationDateAndTime);
        IsoUtilities.toVolumeDescriptorTimeFromUTC(buffer, offset + 864, EffectiveDateAndTime);
        buffer[offset + 881] = FileStructureVersion;
    }
}
