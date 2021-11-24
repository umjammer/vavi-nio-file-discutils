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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import vavi.util.Debug;

import DiscUtils.Core.ClusterMap;
import DiscUtils.Core.ClusterRoles;
import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Core.Internal.ObjectCache;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Range;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;


/**
 * Class representing the $MFT file on disk, including mirror. This class only
 * understands basic record structure, and is ignorant of files that span
 * multiple records. This class should only be used by the NtfsFileSystem and
 * File classes.
 */
public class MasterFileTable implements IDiagnosticTraceable, Closeable {
    /**
     * MFT index of the MFT file itself.
     */
    public static final long MftIndex = 0;

    /**
     * MFT index of the MFT Mirror file.
     */
    public static final long MftMirrorIndex = 1;

    /**
     * MFT Index of the Log file.
     */
    public static final long LogFileIndex = 2;

    /**
     * MFT Index of the Volume file.
     */
    public static final long VolumeIndex = 3;

    /**
     * MFT Index of the Attribute Definition file.
     */
    public static final long AttrDefIndex = 4;

    /**
     * MFT Index of the Root Directory.
     */
    public static final long RootDirIndex = 5;

    /**
     * MFT Index of the Bitmap file.
     */
    public static final long BitmapIndex = 6;

    /**
     * MFT Index of the Boot sector(s).
     */
    public static final long BootIndex = 7;

    /**
     * MFT Index of the Bad Bluster file.
     */
    public static final long BadClusIndex = 8;

    /**
     * MFT Index of the Security Descriptor file.
     */
    public static final long SecureIndex = 9;

    /**
     * MFT Index of the Uppercase mapping file.
     */
    public static final long UpCaseIndex = 10;

    /**
     * MFT Index of the Optional Extensions directory.
     */
    public static final long ExtendIndex = 11;

    /**
     * First MFT Index available for 'normal' files.
     */
    private static final int FirstAvailableMftIndex = 24;

    private Bitmap _bitmap;

    private int _bytesPerSector;

    private final ObjectCache<Long, FileRecord> _recordCache;

    private Stream _recordStream;

    private File _self;

    public MasterFileTable(INtfsContext context) {
        BiosParameterBlock bpb = context.getBiosParameterBlock();

        _recordCache = new ObjectCache<>();
        _recordSize = bpb.getMftRecordSize();
        _bytesPerSector = bpb.getBytesPerSector();

        // Temporary record stream - until we've bootstrapped the MFT properly
        _recordStream = new SubStream(context.getRawStream(),
                                      bpb._mftCluster * bpb.getSectorsPerCluster() * bpb.getBytesPerSector(),
                                      24 * getRecordSize());
    }

    /**
     * Gets the MFT records directly from the MFT stream - bypassing the record
     * cache.
     */
    public List<FileRecord> getRecords() {
        List<FileRecord> result = new ArrayList<>();
        try (Stream mftStream = _self.openStream(AttributeType.Data, null, FileAccess.Read)) {
            int index = 0;
            while (mftStream.getPosition() < mftStream.getLength()) {
                byte[] recordData = StreamUtilities.readExact(mftStream, getRecordSize());

Debug.println(Level.FINE, "MFT records[" + index + "]: " + EndianUtilities.bytesToString(recordData, 0, 4));
                if (!EndianUtilities.bytesToString(recordData, 0, 4).equals("FILE")) {
                    continue;
                }

                FileRecord record = new FileRecord(_bytesPerSector);
                record.fromBytes(recordData, 0);
                record.setLoadedIndex(index);

                result.add(record);

                index++;
            }
            return result;
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    private int _recordSize;

    public int getRecordSize() {
        return _recordSize;
    }

    public void setRecordSize(int value) {
        _recordSize = value;
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "MASTER FILE TABLE");
        writer.println(indent + "  Record Length: " + _recordSize);
        for (FileRecord record : getRecords()) {
            record.dump(writer, indent + "  ");
            for (AttributeRecord attr : record.getAttributes()) {
                attr.dump(writer, indent + "     ");
            }
        }
    }

    public void close() throws IOException {
        if (_recordStream != null) {
            _recordStream.close();
            _recordStream = null;
        }

        if (_bitmap != null) {
            _bitmap.close();
            _bitmap = null;
        }
    }

    public FileRecord getBootstrapRecord() {
        _recordStream.setPosition(0);
        byte[] mftSelfRecordData = StreamUtilities.readExact(_recordStream, _recordSize);
        FileRecord mftSelfRecord = new FileRecord(_bytesPerSector);
        mftSelfRecord.fromBytes(mftSelfRecordData, 0);
        _recordCache.put(MftIndex, mftSelfRecord);
        return mftSelfRecord;
    }

    public void initialize(File file) {
        _self = file;

        if (_recordStream != null) {
            try {
                _recordStream.close();
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }

        NtfsStream bitmapStream = _self.getStream(AttributeType.Bitmap, null);
        _bitmap = new Bitmap(bitmapStream.open(FileAccess.ReadWrite), Long.MAX_VALUE);

        NtfsStream recordsStream = _self.getStream(AttributeType.Data, null);
        _recordStream = recordsStream.open(FileAccess.ReadWrite);
    }

    public File initializeNew(INtfsContext context,
                              long firstBitmapCluster,
                              long numBitmapClusters,
                              long firstRecordsCluster,
                              long numRecordsClusters) {
        BiosParameterBlock bpb = context.getBiosParameterBlock();

        FileRecord fileRec = new FileRecord(bpb.getBytesPerSector(), bpb.getMftRecordSize(), (int) MftIndex);
        fileRec.setFlags(EnumSet.of(FileRecordFlags.InUse));
        fileRec.setSequenceNumber((short) 1);
        _recordCache.put(MftIndex, fileRec);

        _self = new File(context, fileRec);

        StandardInformation.initializeNewFile(_self, EnumSet.of(FileAttributeFlags.Hidden, FileAttributeFlags.System));

        NtfsStream recordsStream = _self
                .createStream(AttributeType.Data, null, firstRecordsCluster, numRecordsClusters, bpb.getBytesPerCluster());
        _recordStream = recordsStream.open(FileAccess.ReadWrite);
        wipe(_recordStream);

        NtfsStream bitmapStream = _self
                .createStream(AttributeType.Bitmap, null, firstBitmapCluster, numBitmapClusters, bpb.getBytesPerCluster());

        try (Stream s = bitmapStream.open(FileAccess.ReadWrite)) {
            wipe(s);
            s.setLength(8);
            _bitmap = new Bitmap(s, Long.MAX_VALUE);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }

        setRecordSize(context.getBiosParameterBlock().getMftRecordSize());
        _bytesPerSector = context.getBiosParameterBlock().getBytesPerSector();

        _bitmap.markPresentRange(0, 1);

        // Write the MFT's own record to itself
        byte[] buffer = new byte[_recordSize];
        fileRec.toBytes(buffer, 0);
        _recordStream.setPosition(0);
        _recordStream.write(buffer, 0, _recordSize);
        _recordStream.flush();

        return _self;
    }

    public FileRecord allocateRecord(EnumSet<FileRecordFlags> flags, boolean isMft) {
        long index;
        if (isMft) {
            for (int i = 15; i > 11; --i) {
                // Have to take a lot of care extending the MFT itself, to
                // ensure we never end up unable to bootstrap the file system
                // via the MFT itself - hence why special records are
                // reserved for MFT's own MFT record overflow.
                FileRecord r = getRecord(i, false);
                if (r.getBaseFile().getSequenceNumber() == 0) {
                    r.reset();
                    r.getFlags().add(FileRecordFlags.InUse);
                    writeRecord(r);
                    return r;
                }
            }

            throw new dotnet4j.io.IOException("MFT too fragmented - unable to allocate MFT overflow record");
        }

        index = _bitmap.allocateFirstAvailable(FirstAvailableMftIndex);

        if (index * _recordSize >= _recordStream.getLength()) {
            // Note: 64 is significant, since bitmap extends by 8 bytes (=64
            // bits) at a time.
            long newEndIndex = MathUtilities.roundUp(index + 1, 64);
            _recordStream.setLength(newEndIndex * _recordSize);
            for (long i = index; i < newEndIndex; ++i) {
                FileRecord record = new FileRecord(_bytesPerSector, _recordSize, (int) i);
                writeRecord(record);
            }
        }

        FileRecord newRecord = getRecord(index, true);
        newRecord.reInitialize(_bytesPerSector, _recordSize, (int) index);

        _recordCache.put(index, newRecord);

        flags.add(FileRecordFlags.InUse);
        newRecord.setFlags(flags);

        writeRecord(newRecord);
        _self.updateRecordInMft();

        return newRecord;
    }

    public FileRecord allocateRecord(long index, EnumSet<FileRecordFlags> flags) {
        _bitmap.markPresent(index);

        FileRecord newRecord = new FileRecord(_bytesPerSector, _recordSize, (int) index);
        _recordCache.put(index, newRecord);
        flags.add(FileRecordFlags.InUse);
        newRecord.setFlags(flags);

        writeRecord(newRecord);
        _self.updateRecordInMft();
        return newRecord;
    }

    public void removeRecord(FileRecordReference fileRef) {
        FileRecord record = getRecord(fileRef.getMftIndex(), false);
        record.reset();
        writeRecord(record);

        _recordCache.remove(fileRef.getMftIndex());
        _bitmap.markAbsent(fileRef.getMftIndex());
        _self.updateRecordInMft();
    }

    public FileRecord getRecord(FileRecordReference fileReference) {
        FileRecord result = getRecord(fileReference.getMftIndex(), false);

        if (result != null) {
            if (fileReference.getSequenceNumber() != 0 && result.getSequenceNumber() != 0) {
                if (fileReference.getSequenceNumber() != result.getSequenceNumber()) {
                    throw new dotnet4j.io.IOException("Attempt to get an MFT record with an old reference");
                }
            }
        }

        return result;
    }

    public FileRecord getRecord(long index, boolean ignoreMagic) {
        return getRecord(index, ignoreMagic, false);
    }

    public FileRecord getRecord(long index, boolean ignoreMagic, boolean ignoreBitmap) {
        if (ignoreBitmap || _bitmap == null || _bitmap.isPresent(index)) {
            FileRecord result = _recordCache.get(index);
//if (NonResidentAttributeBuffer.debug) Debug.println(result + " / " + _recordCache);
            if (result != null) {
                return result;
            }

            if ((index + 1) * _recordSize <= _recordStream.getLength()) {
                _recordStream.setPosition(index * _recordSize);
                byte[] recordBuffer = StreamUtilities.readExact(_recordStream, _recordSize);

                result = new FileRecord(_bytesPerSector);
                result.fromBytes(recordBuffer, 0, ignoreMagic);
                result.setLoadedIndex((int) index);
            } else {
                result = new FileRecord(_bytesPerSector, _recordSize, (int) index);
            }

            _recordCache.put(index, result);
            return result;
        }

        return null;
    }

    public void writeRecord(FileRecord record) {
        int recordSize = (int) record.getSize();
        if (recordSize > _recordSize) {
            throw new dotnet4j.io.IOException("Attempting to write over-sized MFT record");
        }

        byte[] buffer = new byte[_recordSize];
        record.toBytes(buffer, 0);

        _recordStream.setPosition(record.getMasterFileTableIndex() * _recordSize);
        _recordStream.write(buffer, 0, _recordSize);
        _recordStream.flush();

        // We may have modified our own meta-data by extending the data stream,
        // so make sure our records are up-to-date.
        if (_self.getMftRecordIsDirty()) {
            DirectoryEntry dirEntry = _self.getDirectoryEntry();
            if (dirEntry != null) {
                dirEntry.updateFrom(_self);
            }

            _self.updateRecordInMft();
        }

        // Need to update Mirror. OpenRaw is OK because this is short duration,
        // and we don't extend or otherwise modify any meta-data, just the
        // content of the Data stream.
        if (record.getMasterFileTableIndex() < 4 && _self.getContext().getGetFileByIndex() != null) {
            File mftMirror = _self.getContext().getGetFileByIndex().invoke(MftMirrorIndex);
            if (mftMirror != null) {
                try (Stream s = mftMirror.openStream(AttributeType.Data, null, FileAccess.ReadWrite)) {
                    s.setPosition(record.getMasterFileTableIndex() * _recordSize);
                    s.write(buffer, 0, _recordSize);
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
            }
        }
    }

    public long getRecordOffset(FileRecordReference fileReference) {
        return fileReference.getMftIndex() * _recordSize;
    }

    public ClusterMap getClusterMap() {
        int totalClusters = (int) MathUtilities.ceil(_self.getContext().getBiosParameterBlock()._totalSectors64,
                                                     _self.getContext().getBiosParameterBlock().getSectorsPerCluster());

        @SuppressWarnings("unchecked")
        EnumSet<ClusterRoles>[] clusterToRole = new EnumSet[totalClusters];
        Object[] clusterToFile = new Object[totalClusters];
        Map<Object, String[]> fileToPaths = new HashMap<>();

        for (int i = 0; i < totalClusters; ++i) {
            clusterToRole[i] = EnumSet.of(ClusterRoles.Free);
        }

        for (FileRecord fr : getRecords()) {
            if (fr.getBaseFile().getValue() != 0 || !fr.getFlags().contains(FileRecordFlags.InUse)) {
                continue;
            }

            File f = new File(_self.getContext(), fr);

            for (NtfsStream stream : f.getAllStreams()) {
                String fileId;

                if (stream.getAttributeType() == AttributeType.Data && stream.getName() != null &&
                    !stream.getName().isEmpty()) {
                    fileId = f.getIndexInMft() + ":" + stream.getName();
                    fileToPaths.put(fileId, f.getNames().stream().map(n -> n + ":" + stream.getName()).toArray(String[]::new));
                } else {
                    fileId = String.valueOf(f.getIndexInMft());
                    fileToPaths.put(fileId, f.getNames().toArray(new String[0]));
                }

                EnumSet<ClusterRoles> roles = EnumSet.noneOf(ClusterRoles.class);
                if (f.getIndexInMft() < FirstAvailableMftIndex) {
                    roles.add(ClusterRoles.SystemFile);
                    if (f.getIndexInMft() == BootIndex) {
                        roles.add(ClusterRoles.BootArea);
                    }
                } else {
                    roles.add(ClusterRoles.DataFile);
                }

                if (stream.getAttributeType() != AttributeType.Data) {
                    roles.add(ClusterRoles.Metadata);
                }

                for (Range range : stream.getClusters()) {
                    for (long cluster = range.getOffset(); cluster < range.getOffset() + range.getCount(); ++cluster) {
                        clusterToRole[(int) cluster] = roles;
                        clusterToFile[(int) cluster] = fileId;
                    }
                }
            }
        }

        return new ClusterMap(clusterToRole, clusterToFile, fileToPaths);
    }

    private static void wipe(Stream s) {
        s.setPosition(0);

        byte[] buffer = new byte[(int) (64 * Sizes.OneKiB)];
        int numWiped = 0;
        while (numWiped < s.getLength()) {
            int toWrite = (int) Math.min(buffer.length, s.getLength() - s.getPosition());
            s.write(buffer, 0, toWrite);
            numWiped += toWrite;
        }
    }
}
