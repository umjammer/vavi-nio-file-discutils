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

package discUtils.ntfs.internals;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import discUtils.ntfs.AttributeRecord;
import discUtils.ntfs.FileRecord;
import discUtils.ntfs.FileRecordFlags;
import discUtils.ntfs.INtfsContext;


/**
 * An entry within the Master File Table.
 */
public final class MasterFileTableEntry {
    private final INtfsContext _context;

    private final FileRecord _fileRecord;

    public MasterFileTableEntry(INtfsContext context, FileRecord fileRecord) {
        _context = context;
        _fileRecord = fileRecord;
    }

    /**
     * Gets the attributes contained in this entry.
     */
    public List<GenericAttribute> getAttributes() {
        List<GenericAttribute> result = new ArrayList<>();
        for (AttributeRecord attr : _fileRecord.getAttributes()) {
            result.add(GenericAttribute.fromAttributeRecord(_context, attr));
        }
        return result;
    }

    /**
     * Gets the identity of the base entry for files split over multiple
     * entries.
     *
     * All entries that form part of the same file have the same value for
     * this property.
     */
    public MasterFileTableReference getBaseRecordReference() {
        return new MasterFileTableReference(_fileRecord.getBaseFile());
    }

    /**
     * Gets the flags indicating the nature of the entry.
     */
    public EnumSet<MasterFileTableEntryFlags> getFlags() {
        return FileRecordFlags.cast(MasterFileTableEntryFlags.class, _fileRecord.getFlags());
    }

    /**
     * Gets the number of hard links referencing this file.
     */
    public int getHardLinkCount() {
        return _fileRecord.getHardLinkCount();
    }

    /**
     * Gets the index of this entry in the Master File Table.
     */
    public long getIndex() {
        return _fileRecord.getLoadedIndex();
    }

    /**
     * Gets the change identifier that is updated each time the file is modified
     * by Windows, relates to the NTFS log file.
     *
     * The NTFS log file provides journalling, preventing meta-data corruption
     * in the event of a system crash.
     */
    public long getLogFileSequenceNumber() {
        return _fileRecord.getLogFileSequenceNumber();
    }

    /**
     * Gets the next attribute identity that will be allocated.
     */
    public int getNextAttributeId() {
        return _fileRecord.getNextAttributeId();
    }

    /**
     * Gets the index of this entry in the Master File Table (as stored in the
     * entry itself).
     *
     * Note - older versions of Windows did not store this value, so it may be
     * Zero.
     */
    public long getSelfIndex() {
        return _fileRecord.getMasterFileTableIndex();
    }

    /**
     * Gets the revision number of the entry.
     *
     * Each time an entry is allocated or de-allocated, this number is
     * incremented by one.
     */
    public int getSequenceNumber() {
        return _fileRecord.getSequenceNumber();
    }
}
