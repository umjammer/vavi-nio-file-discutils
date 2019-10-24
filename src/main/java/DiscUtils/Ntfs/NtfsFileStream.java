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

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.SeekOrigin;


public final class NtfsFileStream extends SparseStream {
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

        try (Closeable __newVar0 = new NtfsTransaction()) {
            _baseStream.setPosition(value);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public void close() throws IOException {
        if (_baseStream == null) {
            return;
        }

        try (Closeable __newVar1 = new NtfsTransaction()) {
            _baseStream.close();
            updateMetadata();
            _baseStream = null;
        }
    }

    public void flush() {
        assertOpen();

        try (Closeable __newVar2 = new NtfsTransaction()) {
            _baseStream.flush();
            updateMetadata();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public int read(byte[] buffer, int offset, int count) {
        assertOpen();
        StreamUtilities.assertBufferParameters(buffer, offset, count);

        try (Closeable __newVar3 = new NtfsTransaction()) {
            return _baseStream.read(buffer, offset, count);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public long seek(long offset, SeekOrigin origin) {
        assertOpen();

        try (Closeable __newVar4 = new NtfsTransaction()) {
            return _baseStream.seek(offset, origin);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public void setLength(long value) {
        assertOpen();

        try (Closeable __newVar5 = new NtfsTransaction()) {
            if (value != getLength()) {
                _isDirty = true;
                _baseStream.setLength(value);
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public void write(byte[] buffer, int offset, int count) {
        assertOpen();
        StreamUtilities.assertBufferParameters(buffer, offset, count);

        try (Closeable __newVar6 = new NtfsTransaction()) {
            _isDirty = true;
            _baseStream.write(buffer, offset, count);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public void clear(int count) {
        assertOpen();

        try (Closeable __newVar7 = new NtfsTransaction()) {
            _isDirty = true;
            _baseStream.clear(count);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
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
            throw new dotnet4j.io.IOException(_entry.getDetails().FileName + " Attempt to use closed stream");
        }
    }
}
