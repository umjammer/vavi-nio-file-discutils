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

import java.util.EnumSet;

import discUtils.ntfs.FileRecord;
import discUtils.ntfs.FileRecordFlags;


public final class MasterFileTableRecord {

    private final FileRecord fileRecord;

    public MasterFileTableRecord(FileRecord fileRecord) {
        this.fileRecord = fileRecord;
    }

    public MasterFileTableReference getBaseRecordReference() {
        return new MasterFileTableReference(fileRecord.getBaseFile());
    }

    public EnumSet<FileRecordFlags> getFlags() {
        return fileRecord.getFlags();
    }

    public int getHardLinkCount() {
        return fileRecord.getHardLinkCount();
    }

    /**
     * Changes each time the file is modified by Windows, relates to the NTFS
     * journal.
     */
    public long getJournalSequenceNumber() {
        return fileRecord.getLogFileSequenceNumber();
    }

    public int getNextAttributeId() {
        return fileRecord.getNextAttributeId();
    }

    public int getSequenceNumber() {
        return fileRecord.getSequenceNumber();
    }
}
