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

package DiscUtils.Ntfs.Internals;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import DiscUtils.Ntfs.FileRecord;
import DiscUtils.Ntfs.FileRecordFlags;
import DiscUtils.Ntfs.INtfsContext;


/**
 * Provides read-only access to the Master File Table of an NTFS file system.
 */
public final class MasterFileTable {
    /**
     * Index of the Master File Table itself.
     */
    public static final long MasterFileTableIndex = 0;

    /**
     * Index of the Master File Table Mirror file.
     */
    public static final long MasterFileTableMirrorIndex = 1;

    /**
     * Index of the Log file.
     */
    public static final long LogFileIndex = 2;

    /**
     * Index of the Volume file.
     */
    public static final long VolumeIndex = 3;

    /**
     * Index of the Attribute Definition file.
     */
    public static final long AttributeDefinitionIndex = 4;

    /**
     * Index of the Root Directory.
     */
    public static final long RootDirectoryIndex = 5;

    /**
     * Index of the Bitmap file.
     */
    public static final long BitmapIndex = 6;

    /**
     * Index of the Boot sector(s).
     */
    public static final long BootIndex = 7;

    /**
     * Index of the Bad Cluster file.
     */
    public static final long BadClusterIndex = 8;

    /**
     * Index of the Security Descriptor file.
     */
    public static final long SecureIndex = 9;

    /**
     * Index of the Uppercase mapping file.
     */
    public static final long UppercaseIndex = 10;

    /**
     * Index of the Optional Extensions directory.
     */
    public static final long ExtendDirectoryIndex = 11;

    /**
     * First index available for 'normal' files.
     */
    private static final int FirstNormalFileIndex = 24;

    private final INtfsContext _context;

    private final DiscUtils.Ntfs.MasterFileTable _mft;

    public MasterFileTable(INtfsContext context, DiscUtils.Ntfs.MasterFileTable mft) {
        _context = context;
        _mft = mft;
    }

    /**
     * Gets an entry by index.
     *
     * @param index The index of the entry.
     * @return The entry.
     */
    public MasterFileTableEntry get___idx(long index) {
        FileRecord mftRecord = _mft.getRecord(index, true, true);
        if (mftRecord != null) {
            return new MasterFileTableEntry(_context, mftRecord);
        }
        return null;
    }

    /**
     * Enumerates all entries.
     *
     * @param filter Filter controlling which entries are returned.
     * @return An enumeration of entries matching the filter.
     */
    public List<MasterFileTableEntry> getEntries(EnumSet<EntryStates> filter) {
        List<MasterFileTableEntry> result = new ArrayList<>();
        for (FileRecord record : _mft.getRecords()) {
            EntryStates state;
            if (record.getFlags().contains(FileRecordFlags.InUse)) {
                state = EntryStates.InUse;
            } else {
                state = EntryStates.NotInUse;
            }

            if (filter.contains(state)) {
                result.add(new MasterFileTableEntry(_context, record));
            }
        }
        return result;
    }
}
