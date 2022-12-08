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

import java.util.EnumSet;
import java.util.List;

import discUtils.core.UnixFileType;
import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.vfs.VfsDirEntry;
import discUtils.iso9660.rockRidge.ChildLinkSystemUseEntry;
import discUtils.iso9660.rockRidge.FileTimeSystemUseEntry;
import discUtils.iso9660.rockRidge.FileTimeSystemUseEntry.Timestamps;
import discUtils.iso9660.rockRidge.PosixFileInfoSystemUseEntry;
import discUtils.iso9660.rockRidge.PosixNameSystemUseEntry;
import discUtils.iso9660.susp.SuspRecords;
import discUtils.iso9660.susp.SystemUseEntry;
import discUtils.streams.util.StreamUtilities;


public final class ReaderDirEntry extends VfsDirEntry {

    private final IsoContext context;

    private String fileName;

    private final DirectoryRecord record;

    public ReaderDirEntry(IsoContext context, DirectoryRecord dirRecord) {
        this.context = context;
        record = dirRecord;
        fileName = record.fileIdentifier;

        boolean rockRidge = this.context.getRockRidgeIdentifier() != null && !this.context.getRockRidgeIdentifier().isEmpty();

        if (context.getSuspDetected() && record.systemUseData != null) {
            suspRecords = new SuspRecords(this.context, record.systemUseData, 0);
        }

        if (rockRidge && getSuspRecords() != null) {
            // The full name is taken from this record, even if it's a child-link record
            List<SystemUseEntry> nameEntries = getSuspRecords().getEntries(this.context.getRockRidgeIdentifier(), "NM");
            StringBuilder rrName = new StringBuilder();
            if (nameEntries != null && nameEntries.size() > 0) {
                for (SystemUseEntry nameEntry : nameEntries) {
                    rrName.append(((PosixNameSystemUseEntry) nameEntry).nameData);
                }

                fileName = rrName.toString();
            }

            // If this is a Rock Ridge child link, replace the dir record with that from the 'self' record
            // in the child directory.
            ChildLinkSystemUseEntry clEntry = getSuspRecords().getEntry(this.context.getRockRidgeIdentifier(), "CL");
            if (clEntry != null) {
                this.context.getDataStream()
                        .position((long) clEntry.childDirLocation * this.context.getVolumeDescriptor().getLogicalBlockSize());
                byte[] firstSector = StreamUtilities.readExact(this.context.getDataStream(),
                                                               this.context.getVolumeDescriptor().getLogicalBlockSize());

                DirectoryRecord[] record = new DirectoryRecord[1];
                DirectoryRecord.readFrom(firstSector, 0, this.context.getVolumeDescriptor().characterEncoding, record);
                if (record[0].systemUseData != null) {
                    suspRecords = new SuspRecords(this.context, record[0].systemUseData, 0);
                }
            }
        }

        lastAccessTimeUtc = record.recordingDateAndTime;
        lastWriteTimeUtc = record.recordingDateAndTime;
        creationTimeUtc = record.recordingDateAndTime;

        if (rockRidge && getSuspRecords() != null) {
            FileTimeSystemUseEntry tfEntry = getSuspRecords().getEntry(this.context.getRockRidgeIdentifier(), "TF");

            if (tfEntry != null) {
                if (tfEntry.timestampsPresent.contains(Timestamps.Access)) {
                    lastAccessTimeUtc = tfEntry.accessTime;
                }

                if (tfEntry.timestampsPresent.contains(Timestamps.Modify)) {
                    lastWriteTimeUtc = tfEntry.modifyTime;
                }

                if (tfEntry.timestampsPresent.contains(Timestamps.Creation)) {
                    creationTimeUtc = tfEntry.creationTime;
                }
            }
        }
    }

    private long creationTimeUtc;

    public long getCreationTimeUtc() {
        return creationTimeUtc;
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        EnumSet<FileAttributes> attrs = EnumSet.noneOf(FileAttributes.class);

        if (context.getRockRidgeIdentifier() != null && !context.getRockRidgeIdentifier().isEmpty()) {
            // If Rock Ridge PX info is present, derive the attributes from the RR info.
            PosixFileInfoSystemUseEntry pfi = getSuspRecords().getEntry(context.getRockRidgeIdentifier(), "PX");
            if (pfi != null) {
                attrs = UnixFileType.toFileAttributes(UnixFileType.values()[(pfi.fileMode >>> 12) & 0xF]);
            }

            if (fileName.startsWith(".")) {
                attrs.add(FileAttributes.Hidden);
            }
        }

        attrs.add(FileAttributes.ReadOnly);

        if (record.flags.contains(FileFlags.Directory)) {
            attrs.add(FileAttributes.Directory);
        }

        if (record.flags.contains(FileFlags.Hidden)) {
            attrs.add(FileAttributes.Hidden);
        }

        return attrs;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean hasVfsFileAttributes() {
        return true;
    }

    public boolean hasVfsTimeInfo() {
        return true;
    }

    public boolean isDirectory() {
        return record.flags.contains(FileFlags.Directory);
    }

    public boolean isSymlink() {
        return false;
    }

    private long lastAccessTimeUtc;

    public long getLastAccessTimeUtc() {
        return lastAccessTimeUtc;
    }

    private long lastWriteTimeUtc;

    public long getLastWriteTimeUtc() {
        return lastWriteTimeUtc;
    }

    public DirectoryRecord getRecord() {
        return record;
    }

    private SuspRecords suspRecords;

    public SuspRecords getSuspRecords() {
        return suspRecords;
    }

    public long getUniqueCacheId() {
        return ((record.locationOfExtent & 0xffffffffL) << 32) | record.dataLength;
    }

    public String toString() {
        return fileName;
    }
}
