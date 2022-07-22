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

import java.nio.charset.Charset;
import java.util.EnumSet;

import discUtils.core.internal.Utilities;
import discUtils.streams.util.EndianUtilities;


public class CommonVolumeDescriptor extends BaseVolumeDescriptor {

    public String abstractFileIdentifier;

    public String applicationIdentifier;

    public String bibliographicFileIdentifier;

    public Charset characterEncoding;

    public String copyrightFileIdentifier;

    public long creationDateAndTime;

    public String dataPreparerIdentifier;

    public long effectiveDateAndTime;

    public long expirationDateAndTime;

    public byte fileStructureVersion;

    protected short logicalBlockSize;

    public int getLogicalBlockSize() {
        return logicalBlockSize;
    }

    public long modificationDateAndTime;

    public int optionalTypeLPathTableLocation;

    public int optionalTypeMPathTableLocation;

    public int pathTableSize;

    public String publisherIdentifier;

    public DirectoryRecord rootDirectory;

    public String systemIdentifier;

    public int typeLPathTableLocation;

    public int typeMPathTableLocation;

    public String volumeIdentifier;

    public short volumeSequenceNumber;

    public String volumeSetIdentifier;

    public short volumeSetSize;

    public int volumeSpaceSize;

    public CommonVolumeDescriptor(byte[] src, int offset, Charset enc) {
        super(src, offset);
        characterEncoding = enc;
        systemIdentifier = IsoUtilities.readChars(src, offset + 8, 32, characterEncoding);
        volumeIdentifier = IsoUtilities.readChars(src, offset + 40, 32, characterEncoding);
        volumeSpaceSize = IsoUtilities.toUInt32FromBoth(src, offset + 80);
        volumeSetSize = IsoUtilities.toUInt16FromBoth(src, offset + 120);
        volumeSequenceNumber = IsoUtilities.toUInt16FromBoth(src, offset + 124);
        logicalBlockSize = IsoUtilities.toUInt16FromBoth(src, offset + 128);
        pathTableSize = IsoUtilities.toUInt32FromBoth(src, offset + 132);
        typeLPathTableLocation = EndianUtilities.toUInt32LittleEndian(src, offset + 140);
        optionalTypeLPathTableLocation = EndianUtilities.toUInt32LittleEndian(src, offset + 144);
        typeMPathTableLocation = Utilities.bitSwap(EndianUtilities.toUInt32LittleEndian(src, offset + 148));
        optionalTypeMPathTableLocation = Utilities.bitSwap(EndianUtilities.toUInt32LittleEndian(src, offset + 152));
        DirectoryRecord[] directoryRecord = new DirectoryRecord[1];
        DirectoryRecord.readFrom(src, offset + 156, characterEncoding, directoryRecord);
        rootDirectory = directoryRecord[0];
        volumeSetIdentifier = IsoUtilities.readChars(src, offset + 190, 318 - 190, characterEncoding);
        publisherIdentifier = IsoUtilities.readChars(src, offset + 318, 446 - 318, characterEncoding);
        dataPreparerIdentifier = IsoUtilities.readChars(src, offset + 446, 574 - 446, characterEncoding);
        applicationIdentifier = IsoUtilities.readChars(src, offset + 574, 702 - 574, characterEncoding);
        copyrightFileIdentifier = IsoUtilities.readChars(src, offset + 702, 739 - 702, characterEncoding);
        abstractFileIdentifier = IsoUtilities.readChars(src, offset + 739, 776 - 739, characterEncoding);
        bibliographicFileIdentifier = IsoUtilities.readChars(src, offset + 776, 813 - 776, characterEncoding);
        creationDateAndTime = IsoUtilities.toDateTimeFromVolumeDescriptorTime(src, offset + 813);
        modificationDateAndTime = IsoUtilities.toDateTimeFromVolumeDescriptorTime(src, offset + 830);
        expirationDateAndTime = IsoUtilities.toDateTimeFromVolumeDescriptorTime(src, offset + 847);
        effectiveDateAndTime = IsoUtilities.toDateTimeFromVolumeDescriptorTime(src, offset + 864);
        fileStructureVersion = src[offset + 881];
    }

    public CommonVolumeDescriptor(VolumeDescriptorType type,
            byte version,
            int volumeSpaceSize,
            int pathTableSize,
            int typeLPathTableLocation,
            int typeMPathTableLocation,
            int rootDirExtentLocation,
            int rootDirDataLength,
            long buildTime,
            Charset enc) {
        super(type, version);
        characterEncoding = enc;
        systemIdentifier = "";
        volumeIdentifier = "";
        this.volumeSpaceSize = volumeSpaceSize;
        volumeSetSize = 1;
        volumeSequenceNumber = 1;
        logicalBlockSize = IsoUtilities.SectorSize;
        this.pathTableSize = pathTableSize;
        this.typeLPathTableLocation = typeLPathTableLocation;
//        optionalTypeLPathTableLocation = 0;
        this.typeMPathTableLocation = typeMPathTableLocation;
//        optionalTypeMPathTableLocation = 0;
        rootDirectory = new DirectoryRecord();
        rootDirectory.extendedAttributeRecordLength = 0;
        rootDirectory.locationOfExtent = rootDirExtentLocation;
        rootDirectory.dataLength = rootDirDataLength;
        rootDirectory.recordingDateAndTime = buildTime;
        rootDirectory.flags = EnumSet.of(FileFlags.Directory);
        rootDirectory.fileUnitSize = 0;
        rootDirectory.interleaveGapSize = 0;
        rootDirectory.volumeSequenceNumber = 1;
        rootDirectory.fileIdentifier = "\0";
        volumeSetIdentifier = "";
        publisherIdentifier = "";
        dataPreparerIdentifier = "";
        applicationIdentifier = "";
        copyrightFileIdentifier = "";
        abstractFileIdentifier = "";
        bibliographicFileIdentifier = "";
        creationDateAndTime = buildTime;
        modificationDateAndTime = buildTime;
        expirationDateAndTime = Long.MIN_VALUE;
        effectiveDateAndTime = buildTime;
        fileStructureVersion = 1; // V1
    }
}

