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

import java.util.EnumSet;
import java.util.List;

import DiscUtils.Core.UnixFileType;
import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Core.Vfs.VfsDirEntry;
import DiscUtils.Iso9660.RockRidge.ChildLinkSystemUseEntry;
import DiscUtils.Iso9660.RockRidge.FileTimeSystemUseEntry;
import DiscUtils.Iso9660.RockRidge.FileTimeSystemUseEntry.Timestamps;
import DiscUtils.Iso9660.RockRidge.PosixFileInfoSystemUseEntry;
import DiscUtils.Iso9660.RockRidge.PosixNameSystemUseEntry;
import DiscUtils.Iso9660.Susp.SuspRecords;
import DiscUtils.Iso9660.Susp.SystemUseEntry;
import DiscUtils.Streams.Util.StreamUtilities;


public final class ReaderDirEntry extends VfsDirEntry {
    private final IsoContext _context;

    private String _fileName;

    private final DirectoryRecord _record;

    public ReaderDirEntry(IsoContext context, DirectoryRecord dirRecord) {
        _context = context;
        _record = dirRecord;
        _fileName = _record.FileIdentifier;

        boolean rockRidge = _context.getRockRidgeIdentifier() != null && !_context.getRockRidgeIdentifier().isEmpty();

        if (context.getSuspDetected() && _record.SystemUseData != null) {
            _suspRecords = new SuspRecords(_context, _record.SystemUseData, 0);
        }

        if (rockRidge && getSuspRecords() != null) {
            // The full name is taken from this record, even if it's a child-link record
            List<SystemUseEntry> nameEntries = getSuspRecords().getEntries(_context.getRockRidgeIdentifier(), "NM");
            StringBuilder rrName = new StringBuilder();
            if (nameEntries != null && nameEntries.size() > 0) {
                for (SystemUseEntry nameEntry : nameEntries) {
                    rrName.append(((PosixNameSystemUseEntry) nameEntry).NameData);
                }

                _fileName = rrName.toString();
            }

            // If this is a Rock Ridge child link, replace the dir record with that from the 'self' record
            // in the child directory.
            ChildLinkSystemUseEntry clEntry = getSuspRecords().getEntry(_context.getRockRidgeIdentifier(), "CL");
            if (clEntry != null) {
                _context.getDataStream()
                        .setPosition((long) clEntry.ChildDirLocation * _context.getVolumeDescriptor().getLogicalBlockSize());
                byte[] firstSector = StreamUtilities.readExact(_context.getDataStream(),
                                                               _context.getVolumeDescriptor().getLogicalBlockSize());

                DirectoryRecord[] _record = new DirectoryRecord[1];
                DirectoryRecord.readFrom(firstSector, 0, _context.getVolumeDescriptor().CharacterEncoding, _record);
                if (_record[0].SystemUseData != null) {
                    _suspRecords = new SuspRecords(_context, _record[0].SystemUseData, 0);
                }
            }
        }

        _lastAccessTimeUtc = _record.RecordingDateAndTime;
        _lastWriteTimeUtc = _record.RecordingDateAndTime;
        _creationTimeUtc = _record.RecordingDateAndTime;

        if (rockRidge && getSuspRecords() != null) {
            FileTimeSystemUseEntry tfEntry = getSuspRecords().getEntry(_context.getRockRidgeIdentifier(), "TF");

            if (tfEntry != null) {
                if (tfEntry.TimestampsPresent.contains(Timestamps.Access)) {
                    _lastAccessTimeUtc = tfEntry.AccessTime;
                }

                if (tfEntry.TimestampsPresent.contains(Timestamps.Modify)) {
                    _lastWriteTimeUtc = tfEntry.ModifyTime;
                }

                if (tfEntry.TimestampsPresent.contains(Timestamps.Creation)) {
                    _creationTimeUtc = tfEntry.CreationTime;
                }
            }
        }
    }

    private long _creationTimeUtc;

    public long getCreationTimeUtc() {
        return _creationTimeUtc;
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        EnumSet<FileAttributes> attrs = EnumSet.noneOf(FileAttributes.class);

        if (_context.getRockRidgeIdentifier() != null && !_context.getRockRidgeIdentifier().isEmpty()) {
            // If Rock Ridge PX info is present, derive the attributes from the RR info.
            PosixFileInfoSystemUseEntry pfi = getSuspRecords().getEntry(_context.getRockRidgeIdentifier(), "PX");
            if (pfi != null) {
                attrs = UnixFileType.toFileAttributes(UnixFileType.values()[(pfi.FileMode >>> 12) & 0xF]);
            }

            if (_fileName.startsWith(".")) {
                attrs.add(FileAttributes.Hidden);
            }
        }

        attrs.add(FileAttributes.ReadOnly);

        if (_record.Flags.contains(FileFlags.Directory)) {
            attrs.add(FileAttributes.Directory);
        }

        if (_record.Flags.contains(FileFlags.Hidden)) {
            attrs.add(FileAttributes.Hidden);
        }

        return attrs;
    }

    public String getFileName() {
        return _fileName;
    }

    public boolean hasVfsFileAttributes() {
        return true;
    }

    public boolean hasVfsTimeInfo() {
        return true;
    }

    public boolean isDirectory() {
        return _record.Flags.contains(FileFlags.Directory);
    }

    public boolean isSymlink() {
        return false;
    }

    private long _lastAccessTimeUtc;

    public long getLastAccessTimeUtc() {
        return _lastAccessTimeUtc;
    }

    private long _lastWriteTimeUtc;

    public long getLastWriteTimeUtc() {
        return _lastWriteTimeUtc;
    }

    public DirectoryRecord getRecord() {
        return _record;
    }

    private SuspRecords _suspRecords;

    public SuspRecords getSuspRecords() {
        return _suspRecords;
    }

    public long getUniqueCacheId() {
        return ((_record.LocationOfExtent & 0xffffffffL) << 32) | _record.DataLength;
    }

    public String toString() {
        return _fileName;
    }
}
