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

import java.util.Map;

import DiscUtils.Core.UnixFileSystemInfo;
import DiscUtils.Core.Vfs.IVfsFile;
import DiscUtils.Iso9660.RockRidge.PosixFileInfoSystemUseEntry;
import DiscUtils.Iso9660.Susp.SuspRecords;
import DiscUtils.Streams.StreamBuffer;
import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Util.Ownership;


public class File implements IVfsFile {
    protected IsoContext _context;

    protected ReaderDirEntry _dirEntry;

    public File(IsoContext context, ReaderDirEntry dirEntry) {
        _context = context;
        _dirEntry = dirEntry;
    }

    public byte[] getSystemUseData() {
        return _dirEntry.getRecord().SystemUseData;
    }

    public UnixFileSystemInfo getUnixFileInfo() {
        if (!_context.getSuspDetected() || _context.getRockRidgeIdentifier() == null ||
            _context.getRockRidgeIdentifier().isEmpty()) {
            throw new UnsupportedOperationException("No RockRidge file information available");
        }

        SuspRecords suspRecords = new SuspRecords(_context, getSystemUseData(), 0);
        PosixFileInfoSystemUseEntry pfi = suspRecords.getEntry(_context.getRockRidgeIdentifier(), "PX");
        if (pfi != null) {
            return new UnixFileSystemInfo();
        }

        throw new UnsupportedOperationException("No RockRidge file information available for this file");
    }

    public long getLastAccessTimeUtc() {
        return _dirEntry.getLastAccessTimeUtc();
    }

    public void setLastAccessTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getLastWriteTimeUtc() {
        return _dirEntry.getLastWriteTimeUtc();
    }

    public void setLastWriteTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getCreationTimeUtc() {
        return _dirEntry.getCreationTimeUtc();
    }

    public void setCreationTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getFileAttributes() {
        return _dirEntry.getFileAttributes();
    }

    public void setFileAttributes(Map<String, Object> value) {
        throw new UnsupportedOperationException();
    }

    public long getFileLength() {
        return _dirEntry.getRecord().DataLength;
    }

    public IBuffer getFileContent() {
        ExtentStream es = new ExtentStream(_context.getDataStream(),
                                           _dirEntry.getRecord().LocationOfExtent,
                                           _dirEntry.getRecord().DataLength,
                                           _dirEntry.getRecord().FileUnitSize,
                                           _dirEntry.getRecord().InterleaveGapSize);
        return new StreamBuffer(es, Ownership.Dispose);
    }
}
