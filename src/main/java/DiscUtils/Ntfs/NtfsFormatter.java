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

package DiscUtils.Ntfs;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;

import DiscUtils.Core.Geometry;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Sizes;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.Stream;
import dotnet4j.security.accessControl.RawSecurityDescriptor;
import dotnet4j.security.principal.SecurityIdentifier;
import dotnet4j.security.principal.WellKnownSidType;


public class NtfsFormatter {
    private long _bitmapCluster;

    private int _clusterSize;

    private NtfsContext _context;

    private int _indexBufferSize;

    private long _mftCluster;

    private long _mftMirrorCluster;

    private int _mftRecordSize;

    private byte[] _bootCode;

    public byte[] getBootCode() {
        return _bootCode;
    }

    public void setBootCode(byte[] value) {
        _bootCode = value;
    }

    private SecurityIdentifier _computerAccount;

    public SecurityIdentifier getComputerAccount() {
        return _computerAccount;
    }

    public void setComputerAccount(SecurityIdentifier value) {
        _computerAccount = value;
    }

    private Geometry _diskGeometry;

    public Geometry getDiskGeometry() {
        return _diskGeometry;
    }

    public void setDiskGeometry(Geometry value) {
        _diskGeometry = value;
    }

    private long _firstSector;

    public long getFirstSector() {
        return _firstSector;
    }

    public void setFirstSector(long value) {
        _firstSector = value;
    }

    private String _label;

    public String getLabel() {
        return _label;
    }

    public void setLabel(String value) {
        _label = value;
    }

    private long _sectorCount;

    public long getSectorCount() {
        return _sectorCount;
    }

    public void setSectorCount(long value) {
        _sectorCount = value;
    }

    public NtfsFileSystem format(Stream stream) {
        _context = new NtfsContext();
        _context.setOptions(new NtfsOptions());
        _context.setRawStream(stream);
        _context.setAttributeDefinitions(new AttributeDefinitions());
        String localAdminString = getComputerAccount() == null ? "LA"
                                                               : (new SecurityIdentifier(WellKnownSidType.AccountAdministratorSid,
                                                                                         getComputerAccount())).toString();

        try (Closeable c = new NtfsTransaction()) {
            _clusterSize = 4096;
            _mftRecordSize = 1024;
            _indexBufferSize = 4096;
            long totalClusters = (getSectorCount() - 1) * Sizes.Sector / _clusterSize;
            // Allocate a minimum of 8KB for the boot loader, but allow for
            // more
            int numBootClusters = MathUtilities
                    .ceil(Math.max((int) (8 * Sizes.OneKiB), getBootCode() == null ? 0 : getBootCode().length), _clusterSize);
            // Place MFT mirror in the middle of the volume
            _mftMirrorCluster = totalClusters / 2;
            int numMftMirrorClusters = 1;
            // The bitmap is also near the middle
            _bitmapCluster = _mftMirrorCluster + 13;
            int numBitmapClusters = (int) MathUtilities.ceil(totalClusters / 8, _clusterSize);
            // The MFT bitmap goes 'near' the start - approx 10% in - but
            // ensure we avoid the bootloader
            long mftBitmapCluster = Math.max(3 + totalClusters / 10, numBootClusters);
            int numMftBitmapClusters = 1;
            // The MFT follows it's bitmap
            _mftCluster = mftBitmapCluster + numMftBitmapClusters;
            int numMftClusters = 8;
            if (_mftCluster + numMftClusters > _mftMirrorCluster || _bitmapCluster + numBitmapClusters >= totalClusters) {
                throw new dotnet4j.io.IOException("Unable to determine initial layout of NTFS metadata - disk may be too small");
            }

            createBiosParameterBlock(stream, numBootClusters * _clusterSize);
            _context.setMft(new MasterFileTable(_context));
            File mftFile = _context.getMft()
                    .initializeNew(_context, mftBitmapCluster, numMftBitmapClusters, _mftCluster, numMftClusters);
            File bitmapFile = createFixedSystemFile(MasterFileTable.BitmapIndex, _bitmapCluster, numBitmapClusters, true);
            _context.setClusterBitmap(new ClusterBitmap(bitmapFile));
            _context.getClusterBitmap().markAllocated(0, numBootClusters);
            _context.getClusterBitmap().markAllocated(_bitmapCluster, numBitmapClusters);
            _context.getClusterBitmap().markAllocated(mftBitmapCluster, numMftBitmapClusters);
            _context.getClusterBitmap().markAllocated(_mftCluster, numMftClusters);
            _context.getClusterBitmap().setTotalClusters(totalClusters);
            bitmapFile.updateRecordInMft();
            File mftMirrorFile = createFixedSystemFile(MasterFileTable.MftMirrorIndex,
                                                       _mftMirrorCluster,
                                                       numMftMirrorClusters,
                                                       true);
            File logFile = createSystemFile(MasterFileTable.LogFileIndex);

            try (Stream s = logFile.openStream(AttributeType.Data, null, FileAccess.ReadWrite)) {
                s.setLength(Math.min(Math.max(2 * Sizes.OneMiB, totalClusters / 500 * _clusterSize), 64 * Sizes.OneMiB));
                byte[] buffer = new byte[1024 * 1024];
                Arrays.fill(buffer, (byte) 0xFF);
                long totalWritten = 0;
                while (totalWritten < s.getLength()) {
                    int toWrite = (int) Math.min(s.getLength() - totalWritten, buffer.length);
                    s.write(buffer, 0, toWrite);
                    totalWritten += toWrite;
                }
            }
            File volumeFile = createSystemFile(MasterFileTable.VolumeIndex);
            NtfsStream volNameStream = volumeFile.createStream(AttributeType.VolumeName, null);
            volNameStream.setContent(new VolumeName(getLabel() != null ? getLabel() : "New Volume"));
            NtfsStream volInfoStream = volumeFile.createStream(AttributeType.VolumeInformation, null);
            volInfoStream.setContent(new VolumeInformation((byte) 3, (byte) 1, EnumSet.noneOf(VolumeInformationFlags.class)));
            setSecurityAttribute(volumeFile, "O:" + localAdminString + "G:BAD:(A;;0x12019f;;;SY)(A;;0x12019f;;;BA)");
            volumeFile.updateRecordInMft();
            _context.setGetFileByIndex(index-> new File(_context, _context.getMft().getRecord(index, false)));
            _context.setAllocateFile(frf -> new File(_context, _context.getMft().allocateRecord(frf, false)));
            File attrDefFile = createSystemFile(MasterFileTable.AttrDefIndex);
            _context.getAttributeDefinitions().writeTo(attrDefFile);
            setSecurityAttribute(attrDefFile, "O:" + localAdminString + "G:BAD:(A;;FR;;;SY)(A;;FR;;;BA)");
            attrDefFile.updateRecordInMft();
            File bootFile = createFixedSystemFile(MasterFileTable.BootIndex, 0, numBootClusters, false);
            setSecurityAttribute(bootFile, "O:" + localAdminString + "G:BAD:(A;;FR;;;SY)(A;;FR;;;BA)");
            bootFile.updateRecordInMft();
            File badClusFile = createSystemFile(MasterFileTable.BadClusIndex);
            badClusFile.createStream(AttributeType.Data, "$Bad");
            badClusFile.updateRecordInMft();
            File secureFile = createSystemFile(MasterFileTable.SecureIndex, EnumSet.of(FileRecordFlags.HasViewIndex));
            secureFile.removeStream(secureFile.getStream(AttributeType.Data, null));
            _context.setSecurityDescriptors(SecurityDescriptors.initialize(secureFile));
            secureFile.updateRecordInMft();
            File upcaseFile = createSystemFile(MasterFileTable.UpCaseIndex);
            _context.setUpperCase(UpperCase.initialize(upcaseFile));
            upcaseFile.updateRecordInMft();
            File objIdFile = File.createNew(_context,
                                            EnumSet.of(FileRecordFlags.IsMetaFile, FileRecordFlags.HasViewIndex),
                                            EnumSet.noneOf(FileAttributeFlags.class));
            objIdFile.removeStream(objIdFile.getStream(AttributeType.Data, null));
            objIdFile.createIndex("$O", null, AttributeCollationRule.MultipleUnsignedLongs);
            objIdFile.updateRecordInMft();
            File reparseFile = File.createNew(_context,
                                              EnumSet.of(FileRecordFlags.IsMetaFile, FileRecordFlags.HasViewIndex),
                                              EnumSet.noneOf(FileAttributeFlags.class));
            reparseFile.createIndex("$R", null, AttributeCollationRule.MultipleUnsignedLongs);
            reparseFile.updateRecordInMft();
            File quotaFile = File.createNew(_context,
                                            EnumSet.of(FileRecordFlags.IsMetaFile, FileRecordFlags.HasViewIndex),
                                            EnumSet.noneOf(FileAttributeFlags.class));
            Quotas.initialize(quotaFile);
            Directory extendDir = createSystemDirectory(MasterFileTable.ExtendIndex);
            extendDir.addEntry(objIdFile, "$ObjId", FileNameNamespace.Win32AndDos);
            extendDir.addEntry(reparseFile, "$Reparse", FileNameNamespace.Win32AndDos);
            extendDir.addEntry(quotaFile, "$Quota", FileNameNamespace.Win32AndDos);
            extendDir.updateRecordInMft();
            Directory rootDir = createSystemDirectory(MasterFileTable.RootDirIndex);
            rootDir.addEntry(mftFile, "$MFT", FileNameNamespace.Win32AndDos);
            rootDir.addEntry(mftMirrorFile, "$MFTMirr", FileNameNamespace.Win32AndDos);
            rootDir.addEntry(logFile, "$LogFile", FileNameNamespace.Win32AndDos);
            rootDir.addEntry(volumeFile, "$Volume", FileNameNamespace.Win32AndDos);
            rootDir.addEntry(attrDefFile, "$AttrDef", FileNameNamespace.Win32AndDos);
            rootDir.addEntry(rootDir, ".", FileNameNamespace.Win32AndDos);
            rootDir.addEntry(bitmapFile, "$Bitmap", FileNameNamespace.Win32AndDos);
            rootDir.addEntry(bootFile, "$Boot", FileNameNamespace.Win32AndDos);
            rootDir.addEntry(badClusFile, "$BadClus", FileNameNamespace.Win32AndDos);
            rootDir.addEntry(secureFile, "$Secure", FileNameNamespace.Win32AndDos);
            rootDir.addEntry(upcaseFile, "$UpCase", FileNameNamespace.Win32AndDos);
            rootDir.addEntry(extendDir, "$Extend", FileNameNamespace.Win32AndDos);
            setSecurityAttribute(rootDir,
                                 "O:" + localAdminString
                                         + "G:BUD:(A;OICI;FA;;;BA)(A;OICI;FA;;;SY)(A;OICIIO;GA;;;CO)(A;OICI;0x1200a9;;;BU)(A;CI;LC;;;BU)(A;CIIO;DC;;;BU)(A;;0x1200a9;;;WD)");
            rootDir.updateRecordInMft();
            for (long i = MasterFileTable.ExtendIndex + 1; i <= 15; i++) {
                // A number of records are effectively 'reserved'
                File f = createSystemFile(i);
                setSecurityAttribute(f,
                                     "O:S-1-5-21-1708537768-746137067-1060284298-1003G:BAD:(A;;0x12019f;;;SY)(A;;0x12019f;;;BA)");
                f.updateRecordInMft();
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
        // XP-style security permissions setup
        NtfsFileSystem ntfs = new NtfsFileSystem(stream);
        ntfs.setSecurity("$MFT", new RawSecurityDescriptor("O:" + localAdminString + "G:BAD:(A;;FR;;;SY)(A;;FR;;;BA)"));
        ntfs.setSecurity("$MFTMirr", new RawSecurityDescriptor("O:" + localAdminString + "G:BAD:(A;;FR;;;SY)(A;;FR;;;BA)"));
        ntfs.setSecurity("$LogFile", new RawSecurityDescriptor("O:" + localAdminString + "G:BAD:(A;;FR;;;SY)(A;;FR;;;BA)"));
        ntfs.setSecurity("$Bitmap", new RawSecurityDescriptor("O:" + localAdminString + "G:BAD:(A;;FR;;;SY)(A;;FR;;;BA)"));
        ntfs.setSecurity("$BadClus", new RawSecurityDescriptor("O:" + localAdminString + "G:BAD:(A;;FR;;;SY)(A;;FR;;;BA)"));
        ntfs.setSecurity("$UpCase", new RawSecurityDescriptor("O:" + localAdminString + "G:BAD:(A;;FR;;;SY)(A;;FR;;;BA)"));
        ntfs.setSecurity("$Secure",
                         new RawSecurityDescriptor("O:" + localAdminString + "G:BAD:(A;;0x12019f;;;SY)(A;;0x12019f;;;BA)"));
        ntfs.setSecurity("$Extend",
                         new RawSecurityDescriptor("O:" + localAdminString + "G:BAD:(A;;0x12019f;;;SY)(A;;0x12019f;;;BA)"));
        ntfs.setSecurity("$Extend\\$Quota",
                         new RawSecurityDescriptor("O:" + localAdminString + "G:BAD:(A;;0x12019f;;;SY)(A;;0x12019f;;;BA)"));
        ntfs.setSecurity("$Extend\\$ObjId",
                         new RawSecurityDescriptor("O:" + localAdminString + "G:BAD:(A;;0x12019f;;;SY)(A;;0x12019f;;;BA)"));
        ntfs.setSecurity("$Extend\\$Reparse",
                         new RawSecurityDescriptor("O:" + localAdminString + "G:BAD:(A;;0x12019f;;;SY)(A;;0x12019f;;;BA)"));
        ntfs.createDirectory("System Volume Information");
        ntfs.setAttributes("System Volume Information", new HashMap<String, Object>() {
            {
                put(FileAttributeFlags.Hidden.name(), true);
                put(FileAttributeFlags.System.name(), true);
                put(FileAttributeFlags.Directory.name(), true);
            }
        });
        ntfs.setSecurity("System Volume Information", new RawSecurityDescriptor("O:BAG:SYD:(A;OICI;FA;;;SY)"));
        try (Stream s = ntfs.openFile("System Volume Information\\MountPointManagerRemoteDatabase", FileMode.Create)) {
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
        ntfs.setAttributes("System Volume Information\\MountPointManagerRemoteDatabase", new HashMap<String, Object>() {
            {
                put(FileAttributeFlags.Hidden.name(), true);
                put(FileAttributeFlags.System.name(), true);
                put(FileAttributeFlags.Archive.name(), true);
            }
        });
        ntfs.setSecurity("System Volume Information\\MountPointManagerRemoteDatabase",
                         new RawSecurityDescriptor("O:BAG:SYD:(A;;FA;;;SY)"));
        return ntfs;
    }

    private static void setSecurityAttribute(File file, String secDesc) {
        NtfsStream rootSecurityStream = file.createStream(AttributeType.SecurityDescriptor, null);
        SecurityDescriptor sd = new SecurityDescriptor();
        sd.setDescriptor(new RawSecurityDescriptor(secDesc));
        rootSecurityStream.setContent(sd);
    }

    private File createFixedSystemFile(long mftIndex, long firstCluster, long numClusters, boolean wipe) {
        BiosParameterBlock bpb = _context.getBiosParameterBlock();
        if (wipe) {
            byte[] wipeBuffer = new byte[bpb.getBytesPerCluster()];
            _context.getRawStream().setPosition(firstCluster * bpb.getBytesPerCluster());
            for (long i = 0; i < numClusters; ++i) {
                _context.getRawStream().write(wipeBuffer, 0, wipeBuffer.length);
            }
        }

        FileRecord fileRec = _context.getMft().allocateRecord(mftIndex, EnumSet.noneOf(FileRecordFlags.class));
        fileRec.setFlags(EnumSet.of(FileRecordFlags.InUse));
        fileRec.setSequenceNumber((short) mftIndex);
        File file = new File(_context, fileRec);
        StandardInformation.initializeNewFile(file, EnumSet.of(FileAttributeFlags.Hidden, FileAttributeFlags.System));
        file.createStream(AttributeType.Data, null, firstCluster, numClusters, bpb.getBytesPerCluster());
        file.updateRecordInMft();
        if (_context.getClusterBitmap() != null) {
            _context.getClusterBitmap().markAllocated(firstCluster, numClusters);
        }

        return file;
    }

    private File createSystemFile(long mftIndex) {
        return createSystemFile(mftIndex, EnumSet.noneOf(FileRecordFlags.class));
    }

    private File createSystemFile(long mftIndex, EnumSet<FileRecordFlags> flags) {
        FileRecord fileRec = _context.getMft().allocateRecord(mftIndex, flags);
        fileRec.setSequenceNumber((short) mftIndex);
        File file = new File(_context, fileRec);
        EnumSet<FileAttributeFlags> _flags = FileRecord.convertFlags(flags);
        _flags.add(FileAttributeFlags.Hidden);
        _flags.add(FileAttributeFlags.System);
        StandardInformation.initializeNewFile(file, _flags);
        file.createStream(AttributeType.Data, null);
        file.updateRecordInMft();
        return file;
    }

    private Directory createSystemDirectory(long mftIndex) {
        FileRecord fileRec = _context.getMft().allocateRecord(mftIndex, EnumSet.noneOf(FileRecordFlags.class));
        fileRec.setFlags(EnumSet.of(FileRecordFlags.InUse, FileRecordFlags.IsDirectory));
        fileRec.setSequenceNumber((short) mftIndex);
        Directory dir = new Directory(_context, fileRec);
        StandardInformation.initializeNewFile(dir, EnumSet.of(FileAttributeFlags.Hidden, FileAttributeFlags.System));
        dir.createIndex("$I30", AttributeType.FileName, AttributeCollationRule.Filename);
        dir.updateRecordInMft();
        return dir;
    }

    private void createBiosParameterBlock(Stream stream, int bootFileSize) {
        byte[] bootSectors = new byte[bootFileSize];
        if (getBootCode() != null) {
            System.arraycopy(getBootCode(), 0, bootSectors, 0, getBootCode().length);
        }

        BiosParameterBlock bpb = BiosParameterBlock.initialized(getDiskGeometry(),
                                                                _clusterSize,
                                                                (int) getFirstSector(),
                                                                getSectorCount(),
                                                                _mftRecordSize,
                                                                _indexBufferSize);
        bpb._mftCluster = _mftCluster;
        bpb._mftMirrorCluster = _mftMirrorCluster;
        bpb.toBytes(bootSectors, 0);
        // Primary goes at the start of the partition
        stream.setPosition(0);
        stream.write(bootSectors, 0, bootSectors.length);
        // Backup goes at the end of the data in the partition
        stream.setPosition((getSectorCount() - 1) * Sizes.Sector);
        stream.write(bootSectors, 0, Sizes.Sector);
        _context.setBiosParameterBlock(bpb);
    }
}
