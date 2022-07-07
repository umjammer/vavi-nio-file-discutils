//
// Copyright (c) 2008-2013, Kenneth Bell
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

package discUtils.vhdx;

import java.util.List;
import java.util.stream.Collectors;

import discUtils.streams.util.Range;


/**
 * Provides information about a entry in the VHDX log.
 */
public final class LogEntryInfo {
    private final LogEntry _entry;

    public LogEntryInfo(LogEntry entry) {
        _entry = entry;
    }

    /**
     * Gets the VHDX file size (in bytes) that is at least as large as the size
     * of the VHDX file at the time the log entry was written.
     * When shrinking a VHDX file this field is used to indicate the new
     * (smaller) size.
     */
    public long getFlushedFileOffset() {
        return _entry.getFlushedFileOffset();
    }

    /**
     * Gets a value indicating whether this log entry doesn't contain any data
     * (or zero) descriptors.
     */
    public boolean getIsEmpty() {
        return _entry.isEmpty();
    }

    /**
     * Gets the file size (in bytes) that all allocated file structures fit
     * into, at the time the log entry was written.
     */
    public long getLastFileOffset() {
        return _entry.getLastFileOffset();
    }

    /**
     * Gets the file extents that would be modified by replaying this log entry.
     */
    public List<Range> getModifiedExtents() {
        return _entry.getModifiedExtents().stream().map(r -> new Range(r.getOffset(), r.getCount())).collect(Collectors.toList());
    }

    /**
     * Gets the sequence number of this log entry.
     * Consecutively numbered log entries form a sequence.
     */
    public long getSequenceNumber() {
        return _entry.getSequenceNumber();
    }

    /**
     * Gets the oldest logged activity that has not been persisted to disk.
     * The tail indicates how far back in the log replay must start in order
     * to fully recreate the state of the VHDX file's metadata.
     */
    public int getTail() {
        return _entry.getTail();
    }

}
