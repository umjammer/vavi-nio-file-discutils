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
import java.util.EnumSet;

import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.Util.EndianUtilities;


public class CommonVolumeDescriptor extends BaseVolumeDescriptor {
    public String AbstractFileIdentifier;

    public String ApplicationIdentifier;

    public String BibliographicFileIdentifier;

    public Charset CharacterEncoding;

    public String CopyrightFileIdentifier;

    public long CreationDateAndTime;

    public String DataPreparerIdentifier;

    public long EffectiveDateAndTime;

    public long ExpirationDateAndTime;

    public byte FileStructureVersion;

    protected short LogicalBlockSize;

    public int getLogicalBlockSize() {
        return LogicalBlockSize;
    }

    public long ModificationDateAndTime;

    public int OptionalTypeLPathTableLocation;

    public int OptionalTypeMPathTableLocation;

    public int PathTableSize;

    public String PublisherIdentifier;

    public DirectoryRecord RootDirectory;

    public String SystemIdentifier;

    public int TypeLPathTableLocation;

    public int TypeMPathTableLocation;

    public String VolumeIdentifier;

    public short VolumeSequenceNumber;

    public String VolumeSetIdentifier;

    public short VolumeSetSize;

    public int VolumeSpaceSize;

    public CommonVolumeDescriptor(byte[] src, int offset, Charset enc) {
        super(src, offset);
        CharacterEncoding = enc;
        SystemIdentifier = IsoUtilities.readChars(src, offset + 8, 32, CharacterEncoding);
        VolumeIdentifier = IsoUtilities.readChars(src, offset + 40, 32, CharacterEncoding);
        VolumeSpaceSize = IsoUtilities.toUInt32FromBoth(src, offset + 80);
        VolumeSetSize = IsoUtilities.toUInt16FromBoth(src, offset + 120);
        VolumeSequenceNumber = IsoUtilities.toUInt16FromBoth(src, offset + 124);
        LogicalBlockSize = IsoUtilities.toUInt16FromBoth(src, offset + 128);
        PathTableSize = IsoUtilities.toUInt32FromBoth(src, offset + 132);
        TypeLPathTableLocation = EndianUtilities.toUInt32LittleEndian(src, offset + 140);
        OptionalTypeLPathTableLocation = EndianUtilities.toUInt32LittleEndian(src, offset + 144);
        TypeMPathTableLocation = Utilities.bitSwap(EndianUtilities.toUInt32LittleEndian(src, offset + 148));
        OptionalTypeMPathTableLocation = Utilities.bitSwap(EndianUtilities.toUInt32LittleEndian(src, offset + 152));
        DirectoryRecord[] directoryRecord = new DirectoryRecord[1];
        DirectoryRecord.readFrom(src, offset + 156, CharacterEncoding, directoryRecord);
        RootDirectory = directoryRecord[0];
        VolumeSetIdentifier = IsoUtilities.readChars(src, offset + 190, 318 - 190, CharacterEncoding);
        PublisherIdentifier = IsoUtilities.readChars(src, offset + 318, 446 - 318, CharacterEncoding);
        DataPreparerIdentifier = IsoUtilities.readChars(src, offset + 446, 574 - 446, CharacterEncoding);
        ApplicationIdentifier = IsoUtilities.readChars(src, offset + 574, 702 - 574, CharacterEncoding);
        CopyrightFileIdentifier = IsoUtilities.readChars(src, offset + 702, 739 - 702, CharacterEncoding);
        AbstractFileIdentifier = IsoUtilities.readChars(src, offset + 739, 776 - 739, CharacterEncoding);
        BibliographicFileIdentifier = IsoUtilities.readChars(src, offset + 776, 813 - 776, CharacterEncoding);
        CreationDateAndTime = IsoUtilities.toDateTimeFromVolumeDescriptorTime(src, offset + 813);
        ModificationDateAndTime = IsoUtilities.toDateTimeFromVolumeDescriptorTime(src, offset + 830);
        ExpirationDateAndTime = IsoUtilities.toDateTimeFromVolumeDescriptorTime(src, offset + 847);
        EffectiveDateAndTime = IsoUtilities.toDateTimeFromVolumeDescriptorTime(src, offset + 864);
        FileStructureVersion = src[offset + 881];
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
        CharacterEncoding = enc;
        SystemIdentifier = "";
        VolumeIdentifier = "";
        VolumeSpaceSize = volumeSpaceSize;
        VolumeSetSize = 1;
        VolumeSequenceNumber = 1;
        LogicalBlockSize = IsoUtilities.SectorSize;
        PathTableSize = pathTableSize;
        TypeLPathTableLocation = typeLPathTableLocation;
//        OptionalTypeLPathTableLocation = 0;
        TypeMPathTableLocation = typeMPathTableLocation;
//        OptionalTypeMPathTableLocation = 0;
        RootDirectory = new DirectoryRecord();
        RootDirectory.ExtendedAttributeRecordLength = 0;
        RootDirectory.LocationOfExtent = rootDirExtentLocation;
        RootDirectory.DataLength = rootDirDataLength;
        RootDirectory.RecordingDateAndTime = buildTime;
        RootDirectory.Flags = EnumSet.of(FileFlags.Directory);
        RootDirectory.FileUnitSize = 0;
        RootDirectory.InterleaveGapSize = 0;
        RootDirectory.VolumeSequenceNumber = 1;
        RootDirectory.FileIdentifier = "\0";
        VolumeSetIdentifier = "";
        PublisherIdentifier = "";
        DataPreparerIdentifier = "";
        ApplicationIdentifier = "";
        CopyrightFileIdentifier = "";
        AbstractFileIdentifier = "";
        BibliographicFileIdentifier = "";
        CreationDateAndTime = buildTime;
        ModificationDateAndTime = buildTime;
        ExpirationDateAndTime = Long.MIN_VALUE;
        EffectiveDateAndTime = buildTime;
        FileStructureVersion = 1; // V1
    }
}

