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

package discUtils.ntfs;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import discUtils.core.ClusterMap;
import discUtils.core.DiscFileSystemChecker;
import discUtils.core.IDiagnosticTraceable;
import discUtils.core.InvalidFileSystemException;
import discUtils.core.ReportLevels;
import discUtils.streams.SnapshotStream;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Range;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;
import dotnet4j.util.compat.Tuple;
import vavi.util.ByteUtil;

import static java.lang.System.getLogger;


/**
 * Class that checks NTFS file system integrity. Poor relation of chkdsk/fsck.
 */
public final class NtfsFileSystemChecker extends DiscFileSystemChecker {

    private static final Logger logger = getLogger(NtfsFileSystemChecker.class.getName());

    private final Stream target;

    private NtfsContext context;

    private PrintWriter report;

    private EnumSet<ReportLevels> reportLevels;

    private ReportLevels levelsDetected;

    private final ReportLevels levelsConsideredFail = ReportLevels.Errors;

    /**
     * Initializes a new instance of the NtfsFileSystemChecker class.
     *
     * @param diskData The file system to check.
     */
    public NtfsFileSystemChecker(Stream diskData) {
        SnapshotStream protectiveStream = new SnapshotStream(diskData, Ownership.None);
        protectiveStream.snapshot();
        protectiveStream.freeze();
        target = protectiveStream;
    }

    /**
     * Checks the integrity of an NTFS file system held in a stream.
     *
     * @param reportOutput A report on issues found.
     * @param levels The amount of detail to report.
     * @return {@code true} if the file system appears valid, else
     *         {@code false}.
     */
    @Override public boolean check(PrintWriter reportOutput, EnumSet<ReportLevels> levels) {
        context = new NtfsContext();
        context.setRawStream(target);
        context.setOptions(new NtfsOptions());

        report = reportOutput;
        reportLevels = levels;
        levelsDetected = ReportLevels.None;

        try {
            doCheck();
        } catch (AbortException ae) {
            reportError("File system check aborted: " + ae.getMessage(), ae);
            return false;
        } catch (Exception e) {
            reportError("File system check aborted with exception: " + e.getMessage(), e);
            return false;
        }

        return levelsDetected != levelsConsideredFail;
    }

    /**
     * Gets an object that can convert between clusters and files.
     *
     * @return The cluster map.
     */
    public ClusterMap buildClusterMap() {
        context = new NtfsContext();
        context.setRawStream(target);
        context.setOptions(new NtfsOptions());
        context.getRawStream().position(0);
        byte[] bytes = StreamUtilities.readExact(context.getRawStream(), 512);
        context.setBiosParameterBlock(BiosParameterBlock.fromBytes(bytes, 0));
        context.setMft(new MasterFileTable(context));
        File mftFile = new File(context, context.getMft().getBootstrapRecord());
        context.getMft().initialize(mftFile);
        return context.getMft().getClusterMap();
    }

    private static void abort() {
        throw new AbortException();
    }

    private void doCheck() {
        context.getRawStream().position(0);
        byte[] bytes = StreamUtilities.readExact(context.getRawStream(), 512);

        context.setBiosParameterBlock(BiosParameterBlock.fromBytes(bytes, 0));

        //
        // MASTER FILE TABLE
        //

        // Bootstrap the Master File Table
        context.setMft(new MasterFileTable(context));
        File mftFile = new File(context, context.getMft().getBootstrapRecord());

        // Verify basic MFT records before initializing the Master File Table
        preVerifyMft(mftFile);
        context.getMft().initialize(mftFile);

        // Now the MFT is up and running, do more detailed analysis of it's
        // contents -
        // double-accounted clusters, etc
        verifyMft();
        context.getMft().dump(report, "INFO: ");

        //
        // INDEXES
        //

        // Need UpperCase in order to verify some indexes (i.e. directories).
        File ucFile = new File(context, context.getMft().getRecord(MasterFileTable.UpCaseIndex, false));
        context.setUpperCase(new UpperCase(ucFile));
        selfCheckIndexes();

        //
        // DIRECTORIES
        //
        verifyDirectories();

        //
        // WELL KNOWN FILES
        //
        verifyWellKnownFilesExist();

        //
        // OBJECT IDS
        //
        verifyObjectIds();

        //
        // FINISHED
        //

        // Temporary...
        try (NtfsFileSystem fs = new NtfsFileSystem(context.getRawStream())) {
            if (reportLevels.contains(ReportLevels.Information)) {
                reportDump(fs);
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    private void verifyWellKnownFilesExist() {
        Directory rootDir = new Directory(context, context.getMft().getRecord(MasterFileTable.RootDirIndex, false));
        DirectoryEntry extendDirEntry = rootDir.getEntryByName("$Extend");
        if (extendDirEntry == null) {
            reportError("$Extend does not exist in root directory");
            abort();
        }

        Directory extendDir = new Directory(context, context.getMft().getRecord(extendDirEntry.getReference()));
        DirectoryEntry objIdDirEntry = extendDir.getEntryByName("$ObjId");
        if (objIdDirEntry == null) {
            reportError("$ObjId does not exist in $Extend directory");
            abort();
        }

        // Stash ObjectIds
        context.setObjectIds(new ObjectIds(new File(context, context.getMft().getRecord(objIdDirEntry.getReference()))));
        DirectoryEntry sysVolInfDirEntry = rootDir.getEntryByName("System Volume Information");
        if (sysVolInfDirEntry == null) {
            reportError("'System Volume Information' does not exist in root directory");
            abort();
        }
//        Directory sysVolInfDir = new Directory(context, context.getMft().GetRecord(sysVolInfDirEntry.getReference()));
    }

    private void verifyObjectIds() {
        for (FileRecord fr : context.getMft().getRecords()) {
            if (fr.getBaseFile().getValue() != 0) {
                File f = new File(context, fr);
                for (NtfsStream stream : f.getAllStreams()) {
                    if (stream.getAttributeType() == AttributeType.ObjectId) {
                        ObjectId objId = stream.getContent(ObjectId.class);
                        ObjectIdRecord[] objIdRec = new ObjectIdRecord[1];
                        boolean r = !context.getObjectIds().tryGetValue(objId.Id, objIdRec);
                        if (r) {
                            reportError("ObjectId %s for file %s is not indexed", objId.Id, f.getBestName());
                        } else {
                            if (!objIdRec[0].mftReference.equals(f.getMftReference())) {
                                reportError("ObjectId %s for file %s points to {2}",
                                            objId.Id,
                                            f.getBestName(),
                                            objIdRec[0].mftReference);
                            }
                        }
                    }
                }
            }
        }
        for (Map.Entry<UUID, ObjectIdRecord> objIdRec : context.getObjectIds().getAll().entrySet()) {
            if (context.getMft().getRecord(objIdRec.getValue().mftReference) == null) {
                reportError("ObjectId %s refers to non-existent file %s", objIdRec.getKey(), objIdRec.getValue().mftReference);
            }
        }
    }

    private void verifyDirectories() {
        for (FileRecord fr : context.getMft().getRecords()) {
            if (fr.getBaseFile().getValue() != 0) {
                continue;
            }

            File f = new File(context, fr);
            for (NtfsStream stream : f.getAllStreams()) {
                if (stream.getAttributeType() == AttributeType.IndexRoot && stream.getName().equals("$I30")) {
                    IndexView<FileNameRecord, FileRecordReference> dir = new IndexView<>(FileNameRecord.class,
                                                                                         FileRecordReference.class,
                                                                                         f.getIndex("$I30"));
                    for (Tuple<FileNameRecord, FileRecordReference> entry : dir.getEntries()) {
                        FileRecord refFile = context.getMft().getRecord(entry.getValue());
                        // Make sure each referenced file actually exists...
                        if (refFile == null) {
                            reportError("Directory %s references non-existent file %s", f, entry.getKey());
                        }

                        File referencedFile = new File(context, refFile);
                        StandardInformation si = referencedFile.getStandardInformation();
                        if (si.creationTime != entry.getKey().creationTime ||
                            si.mftChangedTime != entry.getKey().mftChangedTime ||
                            si.modificationTime != entry.getKey().modificationTime) {
                            reportInfo("Directory entry %s in %s is out of date", entry.getKey(), f);
                        }
                    }
                }
            }
        }
    }

    private void selfCheckIndexes() {
        for (FileRecord fr : context.getMft().getRecords()) {
            File f = new File(context, fr);
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
                    logger.log(Level.DEBUG, e.getMessage(), e);
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

        Comparator<byte[]> collator = root.getCollator(context.getUpperCase());

        int pos = header.offsetToFirstEntry;
        while (pos < header.totalSizeOfEntries) {
            IndexEntry entry = new IndexEntry(indexName.equals("$I30"));
            entry.read(buffer, offset + pos);
            pos += entry.getSize();

            if (entry.getFlags().contains(IndexEntryFlags.Node)) {
                long bitmapIdx = entry.getChildrenVirtualCluster() / MathUtilities
                        .ceil(root.getIndexAllocationSize(),
                              context.getBiosParameterBlock().getSectorsPerCluster() *
                                                             context.getBiosParameterBlock().getBytesPerSector());
                if (!bitmap.isPresent(bitmapIdx)) {
                    reportError("Index entry %s is non-leaf, but child vcn %s is not in bitmap at index {2}",
                                Index.entryAsString(entry, fileName, indexName),
                                entry.getChildrenVirtualCluster(),
                                bitmapIdx);
                }
            }

            if (entry.getFlags().contains(IndexEntryFlags.End)) {
                if (pos != header.totalSizeOfEntries) {
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
        int recordLength = context.getBiosParameterBlock().getMftRecordSize();
        int bytesPerSector = context.getBiosParameterBlock().getBytesPerSector();

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
            while (mftStream.position() < mftStream.getLength()) {
                byte[] recordData = StreamUtilities.readExact(mftStream, recordLength);

                String magic = new String(recordData, 0, 4, StandardCharsets.US_ASCII);
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
                        StringBuilder sb = new StringBuilder();
                        for (byte recordDatum : recordData) {
                            sb.append(String.format(" %2x}", recordDatum));
                        }

                        reportInfo("MFT record binary data for index %s:%s", index, sb.toString());
                    }
                }

                index++;
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /** Cluster allocation check - check for double allocations */
    private void verifyMft() {
        Map<Long, String> clusterMap = new HashMap<>();
        for (FileRecord fr : context.getMft().getRecords()) {
            if (fr.getFlags().contains(FileRecordFlags.InUse)) {
                File f = new File(context, fr);
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
                            } else {
                                clusterMap.put(cluster, attrKey);
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

        int pos = ByteUtil.readLeShort(genericRecord.getContent(), 0x14);
        while (ByteUtil.readLeInt(genericRecord.getContent(), pos) != 0xFFFFFFFF) {
            int[] attrLen = new int[1];
            try {
                AttributeRecord ar = AttributeRecord.fromBytes(genericRecord.getContent(), pos, attrLen);
                if (attrLen[0] != ar.getSize()) {
                    reportError("Attribute size is different to calculated size.  AttrId=%s", ar.getAttributeId());
                    ok = false;
                }

                if (ar.isNonResident()) {
                    NonResidentAttributeRecord nrr = (NonResidentAttributeRecord) ar;
                    if (!nrr.getDataRuns().isEmpty()) {
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
                logger.log(Level.DEBUG, e.getMessage(), e);
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

        if (ByteUtil.readLeInt(recordData, record.getRealSize() - 8) != 0xffffffff) {
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
            context.getBiosParameterBlock().getBytesPerCluster() > context.getRawStream().getLength()) {
            reportError("Invalid cluster range %s - beyond end of disk", range);
            ok = false;
        }

        return ok;
    }

    private void reportDump(IDiagnosticTraceable toDump) {
        levelsDetected = ReportLevels.Information;
        if (reportLevels.contains(ReportLevels.Information)) {
            toDump.dump(report, "INFO: ");
        }
    }

    private void reportInfo(String str, Object... args) {
        levelsDetected = ReportLevels.Information;
        if (reportLevels.contains(ReportLevels.Information)) {
            report.printf("INFO: " + str + "\n", args);
        }
    }

    private void reportError(String str, Object... args) {
        levelsDetected = ReportLevels.Errors;
        if (reportLevels.contains(ReportLevels.Errors)) {
            report.printf("ERROR: " + str + "\n",
                           Arrays.stream(args).filter(a -> !(a instanceof Throwable)).toArray(Object[]::new));
            Arrays.stream(args)
                    .filter(a -> a instanceof Throwable)
                    .map(a -> (Throwable) a)
                    .forEach(e -> e.printStackTrace(report));
        }
    }

    private final static class AbortException extends InvalidFileSystemException {
    }
}
