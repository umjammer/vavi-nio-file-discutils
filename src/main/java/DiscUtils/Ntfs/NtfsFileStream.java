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

package DiscUtils.Ntfs;

import java.io.IOException;
import java.util.List;

import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.SeekOrigin;


final class NtfsFileStream extends SparseStream {
    private SparseStream _baseStream;

    private final DirectoryEntry _entry;

    private final File _file;

    private boolean _isDirty;

    public NtfsFileStream(NtfsFileSystem fileSystem,
            DirectoryEntry entry,
            AttributeType attrType,
            String attrName,
            FileAccess access) {
        _entry = entry;
        _file = fileSystem.getFile(entry.getReference());
        _baseStream = _file.openStream(attrType, attrName, access);
    }

    public boolean canRead() {
        assertOpen();
        return _baseStream.canRead();
    }

    public boolean canSeek() {
        assertOpen();
        return _baseStream.canSeek();
    }

    public boolean canWrite() {
        assertOpen();
        return _baseStream.canWrite();
    }

    public List<StreamExtent> getExtents() {
        assertOpen();
        return _baseStream.getExtents();
    }

    public long getLength() {
        assertOpen();
        return _baseStream.getLength();
    }

    public long getPosition() {
        assertOpen();
        return _baseStream.getPosition();
    }

    public void setPosition(long value) {
        assertOpen();

        try (NtfsTransaction c = new NtfsTransaction()) {
            _baseStream.setPosition(value);
        }
    }

    public void close() throws IOException {
        if (_baseStream == null) {
            return;
        }

        try (NtfsTransaction c = new NtfsTransaction()) {
            _baseStream.close();
            updateMetadata();
            _baseStream = null;
        }
    }

    public void flush() {
        assertOpen();

        try (NtfsTransaction c = new NtfsTransaction()) {
            _baseStream.flush();
            updateMetadata();
        }
    }

    public int read(byte[] buffer, int offset, int count) {
        assertOpen();
        StreamUtilities.assertBufferParameters(buffer, offset, count);

        try (NtfsTransaction c = new NtfsTransaction()) {
            return _baseStream.read(buffer, offset, count);
        }
    }

    public long seek(long offset, SeekOrigin origin) {
        assertOpen();

        try (NtfsTransaction c = new NtfsTransaction()) {
            return _baseStream.seek(offset, origin);
        }
    }

    public void setLength(long value) {
        assertOpen();

        try (NtfsTransaction c = new NtfsTransaction()) {
            if (value != getLength()) {
                _isDirty = true;
                _baseStream.setLength(value);
            }
        }
    }

    public void write(byte[] buffer, int offset, int count) {
        assertOpen();
        StreamUtilities.assertBufferParameters(buffer, offset, count);

        try (NtfsTransaction c = new NtfsTransaction()) {
            _isDirty = true;
            _baseStream.write(buffer, offset, count);
        }
    }

    public void clear(int count) {
        assertOpen();

        try (NtfsTransaction c = new NtfsTransaction()) {
            _isDirty = true;
            _baseStream.clear(count);
        }
    }

    private void updateMetadata() {
        if (!_file.getContext().getReadOnly()) {
            // Update the standard information attribute - so it reflects the actual file
            // state
            if (_isDirty) {
                _file.modified();
            } else {
                _file.accessed();
            }
            // Update the directory entry used to open the file, so it's accurate
            _entry.updateFrom(_file);
            // Write attribute changes back to the Master File Table
            _file.updateRecordInMft();
            _isDirty = false;
        }
    }

    private void assertOpen() {
        if (_baseStream == null) {
            throw new dotnet4j.io.IOException(_entry.getDetails()._fileName + " Attempt to use closed stream");
        }
    }
}
