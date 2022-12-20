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
import java.util.List;

import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.SeekOrigin;


final class NtfsFileStream extends SparseStream {

    private SparseStream baseStream;

    private final DirectoryEntry entry;

    private final File file;

    private boolean isDirty;

    public NtfsFileStream(NtfsFileSystem fileSystem,
            DirectoryEntry entry,
            AttributeType attrType,
            String attrName,
            FileAccess access) {
        this.entry = entry;
        file = fileSystem.getFile(entry.getReference());
        baseStream = file.openStream(attrType, attrName, access);
    }

    @Override public boolean canRead() {
        assertOpen();
        return baseStream.canRead();
    }

    @Override public boolean canSeek() {
        assertOpen();
        return baseStream.canSeek();
    }

    @Override public boolean canWrite() {
        assertOpen();
        return baseStream.canWrite();
    }

    @Override public List<StreamExtent> getExtents() {
        assertOpen();
        return baseStream.getExtents();
    }

    @Override public long getLength() {
        assertOpen();
        return baseStream.getLength();
    }

    @Override public long position() {
        assertOpen();
        return baseStream.position();
    }

    @Override public void position(long value) {
        assertOpen();

        try (NtfsTransaction c = new NtfsTransaction()) {
            baseStream.position(value);
        }
    }

    @Override public void close() throws IOException {
        if (baseStream == null) {
            return;
        }

        try (NtfsTransaction c = new NtfsTransaction()) {
            baseStream.close();

            updateMetadata();

            baseStream = null;
        }
    }

    @Override public void flush() {
        assertOpen();

        try (NtfsTransaction c = new NtfsTransaction()) {
            baseStream.flush();

            updateMetadata();
        }
    }

    @Override public int read(byte[] buffer, int offset, int count) {
        assertOpen();
        StreamUtilities.assertBufferParameters(buffer, offset, count);

        try (NtfsTransaction c = new NtfsTransaction()) {
            return baseStream.read(buffer, offset, count);
        }
    }

    @Override public long seek(long offset, SeekOrigin origin) {
        assertOpen();

        try (NtfsTransaction c = new NtfsTransaction()) {
            return baseStream.seek(offset, origin);
        }
    }

    @Override public void setLength(long value) {
        assertOpen();

        try (NtfsTransaction c = new NtfsTransaction()) {
            if (value != getLength()) {
                isDirty = true;
                baseStream.setLength(value);
            }
        }
    }

    @Override public void write(byte[] buffer, int offset, int count) {
        assertOpen();
        StreamUtilities.assertBufferParameters(buffer, offset, count);

        try (NtfsTransaction c = new NtfsTransaction()) {
            isDirty = true;
            baseStream.write(buffer, offset, count);
        }
    }

    @Override public void clear(int count) {
        assertOpen();

        try (NtfsTransaction c = new NtfsTransaction()) {
            isDirty = true;
            baseStream.clear(count);
        }
    }

    private void updateMetadata() {
        if (!file.getContext().getReadOnly()) {
            // Update the standard information attribute - so it reflects the actual file
            // state
            if (isDirty) {
                file.modified();
            } else {
                file.accessed();
            }

            // Update the directory entry used to open the file, so it's accurate
            entry.updateFrom(file);

            // Write attribute changes back to the Master File Table
            file.updateRecordInMft();
            isDirty = false;
        }
    }

    private void assertOpen() {
        if (baseStream == null) {
            throw new dotnet4j.io.IOException(entry.getDetails().fileName + " Attempt to use closed stream");
        }
    }
}
