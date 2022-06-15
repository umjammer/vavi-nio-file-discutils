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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import DiscUtils.Core.ClusterMap;
import DiscUtils.Core.DiscFileSystemChecker;
import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Core.InvalidFileSystemException;
import DiscUtils.Core.ReportLevels;
import DiscUtils.Streams.SnapshotStream;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Range;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.Tuple;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;


/**
 * Class that checks NTFS file system integrity. Poor relation of chkdsk/fsck.
 */
public final class NtfsFileSystemChecker extends DiscFileSystemChecker {
    private final Stream _target;

    private NtfsContext _context;

    private PrintWriter _report;

    private EnumSet<ReportLevels> _reportLevels;

    private ReportLevels _levelsDetected;

    private final ReportLevels _levelsConsideredFail = ReportLevels.Errors;

    /**
     * Initializes a new instance of the NtfsFileSystemChecker class.
     *
     * @param diskData The file system to check.
     */
    public NtfsFileSystemChecker(Stream diskData) {
        SnapshotStream protectiveStream = new SnapshotStream(diskData, Ownership.None);
        protectiveStream.snapshot();
        protectiveStream.freeze();
        _target = protectiveStream;
    }

    /**
     * Checks the integrity of an NTFS file system held in a stream.
     *
     * @param reportOutput A report on issues found.
     * @param levels The amount of detail to report.
     * @return {@code true} if the file system appears valid, else
     *         {@code false}.
     */
    public boolean check(PrintWriter reportOutput, EnumSet<ReportLevels> levels) {
        _context = new NtfsContext();
        _context.setRawStream(_target);
        _context.setOptions(new NtfsOptions());

        _report = reportOutput;
        _reportLevels = levels;
        _levelsDetected = ReportLevels.None;

        try {
            doCheck();
        } catch (AbortException ae) {
            reportError("File system check aborted: " + ae.getMessage(), ae);
            return false;
        } catch (Exception e) {
            reportError("File system check aborted with exception: " + e.getMessage(), e);
            return false;
        }

        return _levelsDetected != _levelsConsideredFail;
    }

    /**
     * Gets an object that can convert between clusters and files.
     *
     * @return The cluster map.
     */
    public ClusterMap buildClusterMap() {
        _context = new NtfsContext();
        _context.setRawStream(_target);
        _context.setOptions(new NtfsOptions());
        _context.getRawStream().setPosition(0);
        byte[] bytes = StreamUtilities.readExact(_context.getRawStream(), 512);
        _context.setBiosParameterBlock(BiosParameterBlock.fromBytes(bytes, 0));
        _context.setMft(new MasterFileTable(_context));
        File mftFile = new File(_context, _context.getMft().getBootstrapRecord());
        _context.getMft().initialize(mftFile);
        return _context.getMft().getClusterMap();
    }

    private static void abort() {
        throw new AbortException();
    }

    private void doCheck() {
        _context.getRawStream().setPosition(0);
        byte[] bytes = StreamUtilities.readExact(_context.getRawStream(), 512);

        _context.setBiosParameterBlock(BiosParameterBlock.fromBytes(bytes, 0));

        // -----------------------------------------------------------------------
        // MASTER FILE TABLE
        //

        // Bootstrap the Master File Table
        _context.setMft(new MasterFileTable(_context));
        File mftFile = new File(_context, _context.getMft().getBootstrapRecord());

        // Verify basic MFT records before initializing the Master File Table
        preVerifyMft(mftFile);
        _context.getMft().initialize(mftFile);

        // Now the MFT is up and running, do more detailed analysis of it's
        // contents -
        // double-accounted clusters, etc
        verifyMft();
        _context.getMft().dump(_report, "INFO: ");

        // -----------------------------------------------------------------------
        // INDEXES
        //

        // Need UpperCase in order to verify some indexes (i.e. directories).
        File ucFile = new File(_context, _context.getMft().getRecord(MasterFileTable.UpCaseIndex, false));
        _context.setUpperCase(new UpperCase(ucFile));
        selfCheckIndexes();

        // -----------------------------------------------------------------------
        // DIRECTORIES
        //
        verifyDirectories();

        // -----------------------------------------------------------------------
        // WELL KNOWN FILES
        //
        verifyWellKnownFilesExist();

        // -----------------------------------------------------------------------
        // OBJECT IDS
        //
        verifyObjectIds();

        // -----------------------------------------------------------------------
        // FINISHED
        //

        // Temporary...
        try (NtfsFileSystem fs = new NtfsFileSystem(_context.getRawStream())) {
            if (_reportLevels.contains(ReportLevels.Information)) {
                reportDump(fs);
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    private void verifyWellKnownFilesExist() {
        Directory rootDir = new Directory(_context, _context.getMft().getRecord(MasterFileTable.RootDirIndex, false));
        DirectoryEntry extendDirEntry = rootDir.getEntryByName("$Extend");
        if (extendDirEntry == null) {
            reportError("$Extend does not exist in root directory");
            abort();
        }

        Directory extendDir = new Directory(_context, _context.getMft().getRecord(extendDirEntry.getReference()));
        DirectoryEntry objIdDirEntry = extendDir.getEntryByName("$ObjId");
        if (objIdDirEntry == null) {
            reportError("$ObjId does not exist in $Extend directory");
            abort();
        }

        // Stash ObjectIds
        _context.setObjectIds(new ObjectIds(new File(_context, _context.getMft().getRecord(objIdDirEntry.getReference()))));
        DirectoryEntry sysVolInfDirEntry = rootDir.getEntryByName("System Volume Information");
        if (sysVolInfDirEntry == null) {
            reportError("'System Volume Information' does not exist in root directory");
            abort();
        }
//        Directory sysVolInfDir = new Directory(_context, _context.getMft().GetRecord(sysVolInfDirEntry.getReference()));
    }

    private void verifyObjectIds() {
        for (FileRecord fr : _context.getMft().getRecords()) {
            if (fr.getBaseFile().getValue() != 0) {
                File f = new File(_context, fr);
                for (NtfsStream stream : f.getAllStreams()) {
                    if (stream.getAttributeType() == AttributeType.ObjectId) {
                        ObjectId objId = stream.getContent(ObjectId.class);
                        ObjectIdRecord[] objIdRec = new ObjectIdRecord[1];
                        boolean r = !_context.getObjectIds().tryGetValue(objId.Id, objIdRec);
                        if (r) {
                            reportError("ObjectId %s for file %s is not indexed", objId.Id, f.getBestName());
                        } else {
                            if (!objIdRec[0].MftReference.equals(f.getMftReference())) {
                                reportError("ObjectId %s for file %s points to {2}",
                                            objId.Id,
                                            f.getBestName(),
                                            objIdRec[0].MftReference);
                            }
                        }
                    }
                }
            }
        }
        for (Map.Entry<UUID, ObjectIdRecord> objIdRec : _context.getObjectIds().getAll().entrySet()) {
            if (_context.getMft().getRecord(objIdRec.getValue().MftReference) == null) {
                reportError("ObjectId %s refers to non-existant file %s", objIdRec.getKey(), objIdRec.getValue().MftReference);
            }
        }
    }

    private void verifyDirectories() {
        for (FileRecord fr : _context.getMft().getRecords()) {
            if (fr.getBaseFile().getValue() != 0) {
                continue;
            }

            File f = new File(_context, fr);
            for (NtfsStream stream : f.getAllStreams()) {
                if (stream.getAttributeType() == AttributeType.IndexRoot && stream.getName().equals("$I30")) {
                    IndexView<FileNameRecord, FileRecordReference> dir = new IndexView<>(FileNameRecord.class,
                                                                                         FileRecordReference.class,
                                                                                         f.getIndex("$I30"));
                    for (Tuple<FileNameRecord, FileRecordReference> entry : dir.getEntries()) {
                        FileRecord refFile = _context.getMft().getRecord(entry.getValue());
                        // Make sure each referenced file actually exists...
                        if (refFile == null) {
                            reportError("Directory %s references non-existent file %s", f, entry.getKey());
                        }

                        File referencedFile = new File(_context, refFile);
                        StandardInformation si = referencedFile.getStandardInformation();
                        if (si._creationTime != entry.getKey()._creationTime ||
                            si._mftChangedTime != entry.getKey()._mftChangedTime ||
                            si._modificationTime != entry.getKey()._modificationTime) {
                            reportInfo("Directory entry %s in %s is out of date", entry.getKey(), f);
                        }
                    }
                }
            }
        }
    }

    private void selfCheckIndexes() {
        for (FileRecord fr : _context.getMft().getRecords()) {
            File f = new File(_context, fr);
            for (NtfsStream stream : f.getAllStreams()) {
                if (stream.getAttributeType() == AttributeType.IndexRoot) {
                    selfCheckIndex(f, stream.getName());
                }
            }
        }
    }

    private void selfCheckIndex(File file, String name) {
        reportInfo("About to self-check index %s in file %s (MFT:{2})", name, file.getBestName(), file.getIndexInMft());
        IndexRoot root = file.getStream(AttributeType.IndexRoot, name).getContent(IndexRoot.class);
        byte[] rootBuffer;
        Stream s = file.openStream(AttributeType.IndexRoot, name, FileAccess.Read);
        try {
            rootBuffer = StreamUtilities.readExact(s, (int) s.getLength());
        } finally {
            if (s != null)
                try {
                    s.close();
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
        }
        Bitmap indexBitmap = null;
        if (file.getStream(AttributeType.Bitmap, name) != null) {
            indexBitmap = new Bitmap(file.openStream(AttributeType.Bitmap, name, FileAccess.Read), Long.MAX_VALUE);
        }

        if (!selfCheckIndexNode(rootBuffer, IndexRoot.HeaderOffset, indexBitmap, root, file.getBestName(), name)) {
            reportError("Index %s in file %s (MFT:{2}) has corrupt IndexRoot attribute",
                        name,
                        file.getBestName(),
                        file.getIndexInMft());
        } else {
            reportInfo("Self-check of index %s in file %s (MFT:{2}) complete", name, file.getBestName(), file.getIndexInMft());
        }
    }

    private boolean selfCheckIndexNode(byte[] buffer,
                                       int offset,
                                       Bitmap bitmap,
                                       IndexRoot root,
                                       String fileName,
                                       String indexName) {
        boolean ok = true;

        IndexHeader header = new IndexHeader(buffer, offset);

        IndexEntry lastEntry = null;

        Comparator<byte[]> collator = root.getCollator(_context.getUpperCase());

        int pos = header._offsetToFirstEntry;
        while (pos < header._totalSizeOfEntries) {
            IndexEntry entry = new IndexEntry(indexName.equals("$I30"));
            entry.read(buffer, offset + pos);
            pos += entry.getSize();

            if (entry.getFlags().contains(IndexEntryFlags.Node)) {
                long bitmapIdx = entry.getChildrenVirtualCluster() / MathUtilities
                        .ceil(root.getIndexAllocationSize(),
                              _context.getBiosParameterBlock().getSectorsPerCluster() *
                                                             _context.getBiosParameterBlock().getBytesPerSector());
                if (!bitmap.isPresent(bitmapIdx)) {
                    reportError("Index entry %s is non-leaf, but child vcn %s is not in bitmap at index {2}",
                                Index.entryAsString(entry, fileName, indexName),
                                entry.getChildrenVirtualCluster(),
                                bitmapIdx);
                }
            }

            if (entry.getFlags().contains(IndexEntryFlags.End)) {
                if (pos != header._totalSizeOfEntries) {
                    reportError("Found END index entry %s, but not at end of node",
                                Index.entryAsString(entry, fileName, indexName));
                    ok = false;
                }
            }

            if (lastEntry != null && collator.compare(lastEntry.getKeyBuffer(), entry.getKeyBuffer()) >= 0) {
                reportError("Found entries out of order %s was before %s",
                            Index.entryAsString(lastEntry, fileName, indexName),
                            Index.entryAsString(entry, fileName, indexName));
                ok = false;
            }

            lastEntry = entry;
        }

        return ok;
    }

    private void preVerifyMft(File file) {
        int recordLength = _context.getBiosParameterBlock().getMftRecordSize();
        int bytesPerSector = _context.getBiosParameterBlock().getBytesPerSector();

        // Check out the MFT's clusters
        for (Range range : file.getAttribute(AttributeType.Data, null).getClusters()) {
            if (!verifyClusterRange(range)) {
                reportError("Corrupt cluster range in MFT data attribute %s", range.toString());
                abort();
            }
        }

        for (Range range : file.getAttribute(AttributeType.Bitmap, null).getClusters()) {
            if (!verifyClusterRange(range)) {
                reportError("Corrupt cluster range in MFT bitmap attribute %s", range.toString());
                abort();
            }
        }

        try (Stream mftStream = file.openStream(AttributeType.Data, null, FileAccess.Read);
             Stream bitmapStream = file.openStream(AttributeType.Bitmap, null, FileAccess.Read);
             Bitmap bitmap = new Bitmap(bitmapStream, Long.MAX_VALUE)) {

            long index = 0;
            while (mftStream.getPosition() < mftStream.getLength()) {
                byte[] recordData = StreamUtilities.readExact(mftStream, recordLength);

                String magic = EndianUtilities.bytesToString(recordData, 0, 4);
                if (!magic.equals("FILE")) {
                    if (bitmap.isPresent(index)) {
                        reportError("Invalid MFT record magic at index %s - was ({2},{3},{4},{5}) \"%s\"",
                                    index,
                                    magic.replaceAll("(^\0*|\0*$)", ""),
                                    (int) magic.charAt(0),
                                    (int) magic.charAt(1),
                                    (int) magic.charAt(2),
                                    (int) magic.charAt(3));
                    }
                } else {
                    if (!verifyMftRecord(recordData, bitmap.isPresent(index), bytesPerSector)) {
                        reportError("Invalid MFT record at index %s", index);
                        StringBuilder bldr = new StringBuilder();
                        for (byte recordDatum : recordData) {
                            bldr.append(String.format(" %2x}", recordDatum));
                        }

                        reportInfo("MFT record binary data for index %s:%s", index, bldr.toString());
                    }
                }

                index++;
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    private void verifyMft() {
        // Cluster allocation check - check for double allocations
        Map<Long, String> clusterMap = new HashMap<>();
        for (FileRecord fr : _context.getMft().getRecords()) {
            if (fr.getFlags().contains(FileRecordFlags.InUse)) {
                File f = new File(_context, fr);
                for (NtfsAttribute attr : f.getAllAttributes()) {
                    String attrKey = fr.getMasterFileTableIndex() + ":" + attr.getId();

                    for (Range range : attr.getClusters()) {
                        if (!verifyClusterRange(range)) {
                            reportError("Attribute %s contains bad cluster range %s", attrKey, range);
                        }

                        for (long cluster = range.getOffset(); cluster < range.getOffset() + range.getCount(); ++cluster) {
                            if (clusterMap.containsKey(cluster)) {
                                reportError("Two attributes referencing cluster %1$s (0x%16X) - %2$s and %3$s (as MftIndex:AttrId)",
                                            cluster,
                                            clusterMap.get(cluster),
                                            attrKey);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean verifyMftRecord(byte[] recordData, boolean presentInBitmap, int bytesPerSector) {
        boolean ok = true;

        //
        // Verify the attributes seem OK...
        //
        byte[] tempBuffer = new byte[recordData.length];
        System.arraycopy(recordData, 0, tempBuffer, 0, tempBuffer.length);
        GenericFixupRecord genericRecord = new GenericFixupRecord(bytesPerSector);
        genericRecord.fromBytes(tempBuffer, 0);

        int pos = EndianUtilities.toUInt16LittleEndian(genericRecord.getContent(), 0x14);
        while (EndianUtilities.toUInt32LittleEndian(genericRecord.getContent(), pos) != 0xFFFFFFFF) {
            int[] attrLen = new int[1];
            try {
                AttributeRecord ar = AttributeRecord.fromBytes(genericRecord.getContent(), pos, attrLen);
                if (attrLen[0] != ar.getSize()) {
                    reportError("Attribute size is different to calculated size.  AttrId=%s", ar.getAttributeId());
                    ok = false;
                }

                if (ar.isNonResident()) {
                    NonResidentAttributeRecord nrr = (NonResidentAttributeRecord) ar;
                    if (nrr.getDataRuns().size() > 0) {
                        long totalVcn = 0;
                        for (DataRun run : nrr.getDataRuns()) {
                            totalVcn += run.getRunLength();
                        }

                        if (totalVcn != nrr.getLastVcn() - nrr.getStartVcn() + 1) {
                            reportError("Declared VCNs doesn't match data runs.  AttrId=%s", ar.getAttributeId());
                            ok = false;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                reportError("Failure parsing attribute at pos=%s", pos);
                return false;
            }

            pos += attrLen[0];
        }

        //
        // Now consider record as a whole
        //
        FileRecord record = new FileRecord(bytesPerSector);
        record.fromBytes(recordData, 0);

        boolean inUse = record.getFlags().contains(FileRecordFlags.InUse);
        if (inUse != presentInBitmap) {
            reportError("MFT bitmap and record in-use flag don't agree.  Mft=%s, Record=%s",
                        presentInBitmap ? "InUse" : "Free",
                        inUse ? "InUse" : "Free");
            ok = false;
        }

        if (record.getSize() != record.getRealSize()) {
            reportError("MFT record real size is different to calculated size.  Stored in MFT=%s, Calculated=%s",
                        record.getRealSize(),
                        record.getSize());
            ok = false;
        }

        if (EndianUtilities.toUInt32LittleEndian(recordData, record.getRealSize() - 8) != 0xffffffff) {
            reportError("MFT record is not correctly terminated with 0xFFFFFFFF");
            ok = false;
        }

        return ok;
    }

    private boolean verifyClusterRange(Range range) {
        boolean ok = true;
        if (range.getOffset() < 0) {
            reportError("Invalid cluster range %s - negative start", range);
            ok = false;
        }

        if (range.getCount() <= 0) {
            reportError("Invalid cluster range %s - negative/zero count", range);
            ok = false;
        }

        if ((range.getOffset() + range.getCount()) *
            _context.getBiosParameterBlock().getBytesPerCluster() > _context.getRawStream().getLength()) {
            reportError("Invalid cluster range %s - beyond end of disk", range);
            ok = false;
        }

        return ok;
    }

    private void reportDump(IDiagnosticTraceable toDump) {
        _levelsDetected = ReportLevels.Information;
        if (_reportLevels.contains(ReportLevels.Information)) {
            toDump.dump(_report, "INFO: ");
        }
    }

    private void reportInfo(String str, Object... args) {
        _levelsDetected = ReportLevels.Information;
        if (_reportLevels.contains(ReportLevels.Information)) {
            _report.printf("INFO: " + str + "\n", args);
        }
    }

    private void reportError(String str, Object... args) {
        _levelsDetected = ReportLevels.Errors;
        if (_reportLevels.contains(ReportLevels.Errors)) {
            _report.printf("ERROR: " + str + "\n",
                           Arrays.stream(args).filter(a -> !(a instanceof Throwable)).toArray(Object[]::new));
            Arrays.stream(args)
                    .filter(a -> a instanceof Throwable)
                    .map(a -> (Throwable) a)
                    .forEach(e -> e.printStackTrace(_report));
        }
    }

    private final static class AbortException extends InvalidFileSystemException {
    }
}
