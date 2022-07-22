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

package discUtils.iso9660;

import java.nio.charset.StandardCharsets;

import discUtils.core.internal.Utilities;


public class PrimaryVolumeDescriptor extends CommonVolumeDescriptor {
    public PrimaryVolumeDescriptor(byte[] src, int offset) {
        super(src, offset, StandardCharsets.US_ASCII);
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
              StandardCharsets.US_ASCII);
    }

    public void writeTo(byte[] buffer, int offset) {
        super.writeTo(buffer, offset);
        IsoUtilities.writeAChars(buffer, offset + 8, 32, systemIdentifier);
        IsoUtilities.writeString(buffer, offset + 40, 32, true, volumeIdentifier, StandardCharsets.US_ASCII, true);
        IsoUtilities.toBothFromUInt32(buffer, offset + 80, volumeSpaceSize);
        IsoUtilities.toBothFromUInt16(buffer, offset + 120, volumeSetSize);
        IsoUtilities.toBothFromUInt16(buffer, offset + 124, volumeSequenceNumber);
        IsoUtilities.toBothFromUInt16(buffer, offset + 128, logicalBlockSize);
        IsoUtilities.toBothFromUInt32(buffer, offset + 132, pathTableSize);
        IsoUtilities.toBytesFromUInt32(buffer, offset + 140, typeLPathTableLocation);
        IsoUtilities.toBytesFromUInt32(buffer, offset + 144, optionalTypeLPathTableLocation);
        IsoUtilities.toBytesFromUInt32(buffer, offset + 148, Utilities.bitSwap(typeMPathTableLocation));
        IsoUtilities.toBytesFromUInt32(buffer, offset + 152, Utilities.bitSwap(optionalTypeMPathTableLocation));
        rootDirectory.writeTo(buffer, offset + 156, StandardCharsets.US_ASCII);
        IsoUtilities.writeDChars(buffer, offset + 190, 129, volumeSetIdentifier);
        IsoUtilities.writeAChars(buffer, offset + 318, 129, publisherIdentifier);
        IsoUtilities.writeAChars(buffer, offset + 446, 129, dataPreparerIdentifier);
        IsoUtilities.writeAChars(buffer, offset + 574, 129, applicationIdentifier);
        IsoUtilities.writeDChars(buffer, offset + 702, 37, copyrightFileIdentifier); // FIXME!!
        IsoUtilities.writeDChars(buffer, offset + 739, 37, abstractFileIdentifier); // FIXME!!
        IsoUtilities.writeDChars(buffer, offset + 776, 37, bibliographicFileIdentifier); // FIXME!!
        IsoUtilities.toVolumeDescriptorTimeFromUTC(buffer, offset + 813, creationDateAndTime);
        IsoUtilities.toVolumeDescriptorTimeFromUTC(buffer, offset + 830, modificationDateAndTime);
        IsoUtilities.toVolumeDescriptorTimeFromUTC(buffer, offset + 847, expirationDateAndTime);
        IsoUtilities.toVolumeDescriptorTimeFromUTC(buffer, offset + 864, effectiveDateAndTime);
        buffer[offset + 881] = fileStructureVersion;
    }
}
