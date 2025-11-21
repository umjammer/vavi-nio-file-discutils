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

import discUtils.core.UnixFileSystemInfo;
import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.vfs.IVfsFile;
import discUtils.iso9660.rockRidge.PosixFileInfoSystemUseEntry;
import discUtils.iso9660.susp.SuspRecords;
import discUtils.streams.StreamBuffer;
import discUtils.streams.buffer.IBuffer;
import discUtils.streams.util.Ownership;


public class File implements IVfsFile {

    protected final IsoContext context;

    protected final ReaderDirEntry dirEntry;

    public File(IsoContext context, ReaderDirEntry dirEntry) {
        this.context = context;
        this.dirEntry = dirEntry;
    }

    public byte[] getSystemUseData() {
        return dirEntry.getRecord().systemUseData;
    }

    public UnixFileSystemInfo getUnixFileInfo() {
        if (!context.getSuspDetected() || context.getRockRidgeIdentifier() == null ||
            context.getRockRidgeIdentifier().isEmpty()) {
            throw new UnsupportedOperationException("No rockRidge file information available");
        }

        SuspRecords suspRecords = new SuspRecords(context, getSystemUseData(), 0);
        PosixFileInfoSystemUseEntry pfi = suspRecords.getEntry(context.getRockRidgeIdentifier(), "PX");
        if (pfi != null) {
            return new UnixFileSystemInfo();
        }

        throw new UnsupportedOperationException("No rockRidge file information available for this file");
    }

    @Override public long getLastAccessTimeUtc() {
        return dirEntry.getLastAccessTimeUtc();
    }

    @Override public void setLastAccessTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public long getLastWriteTimeUtc() {
        return dirEntry.getLastWriteTimeUtc();
    }

    @Override public void setLastWriteTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public long getCreationTimeUtc() {
        return dirEntry.getCreationTimeUtc();
    }

    @Override public void setCreationTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public EnumSet<FileAttributes> getFileAttributes() {
        return dirEntry.getFileAttributes();
    }

    @Override public void setFileAttributes(EnumSet<FileAttributes> value) {
        throw new UnsupportedOperationException();
    }

    @Override public long getFileLength() {
        return dirEntry.getRecord().dataLength;
    }

    @Override public IBuffer getFileContent() {
        ExtentStream es = new ExtentStream(context.getDataStream(),
                                           dirEntry.getRecord().locationOfExtent,
                                           dirEntry.getRecord().dataLength,
                                           dirEntry.getRecord().fileUnitSize,
                                           dirEntry.getRecord().interleaveGapSize);
        return new StreamBuffer(es, Ownership.Dispose);
    }

    @Override public String toString() {
        return getClass() + ": " + dirEntry.getFileName() + ", " + getFileLength();
    }
}
