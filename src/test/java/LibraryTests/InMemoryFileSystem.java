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

package LibraryTests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Streams.SparseMemoryBuffer;
import DiscUtils.Streams.SparseMemoryStream;
import DiscUtils.Streams.SparseStream;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileNotFoundException;
import dotnet4j.io.IOException;


/**
 * Minimal implementation of DiscFileSystem, sufficient to support unit-testing
 * of disk formats.
 */
public class InMemoryFileSystem extends DiscFileSystem {
    private Map<String, SparseMemoryBuffer> _files;

    public InMemoryFileSystem() {
        _files = new HashMap<>();
    }

    public String getFriendlyName() {
        throw new UnsupportedOperationException();
    }

    public boolean canWrite() {
        throw new UnsupportedOperationException();
    }

    public long getSize() {
        throw new UnsupportedOperationException();
    }

    public long getUsedSpace() {
        throw new UnsupportedOperationException();
    }

    public long getAvailableSpace() {
        throw new UnsupportedOperationException();
    }

    public void copyFile(String sourceFile, String destinationFile, boolean overwrite) {
        throw new UnsupportedOperationException();
    }

    public void createDirectory(String path) {
        throw new UnsupportedOperationException();
    }

    public void deleteDirectory(String path) {
        throw new UnsupportedOperationException();
    }

    public void deleteFile(String path) {
        throw new UnsupportedOperationException();
    }

    public boolean directoryExists(String path) {
        throw new UnsupportedOperationException();
    }

    public boolean fileExists(String path) {
        return _files.containsKey(path);
    }

    public List<String> getDirectories(String path, String searchPattern, String searchOption) {
        throw new UnsupportedOperationException();
    }

    public List<String> getFiles(String path, String searchPattern, String searchOption) {
        throw new UnsupportedOperationException();
    }

    public List<String> getFileSystemEntries(String path) {
        throw new UnsupportedOperationException();
    }

    public List<String> getFileSystemEntries(String path, String searchPattern) {
        throw new UnsupportedOperationException();
    }

    public void moveDirectory(String sourceDirectoryName, String destinationDirectoryName) {
        throw new UnsupportedOperationException();
    }

    public void moveFile(String sourceName, String destinationName, boolean overwrite) {
        throw new UnsupportedOperationException();
    }

    public SparseStream openFile(String path, FileMode mode, FileAccess access) {
        if (_files.containsKey(path)) {
            if (mode == FileMode.CreateNew) {
                throw new IOException("File already exists");
            }

            return new SparseMemoryStream(_files.get(path), access);
        } else if (mode == FileMode.Create || mode == FileMode.CreateNew || mode == FileMode.OpenOrCreate ||
                   mode == FileMode.Truncate) {
            _files.put(path, new SparseMemoryBuffer(16 * 1024));
            return new SparseMemoryStream(_files.get(path), access);
        } else {
            throw new FileNotFoundException();
        }
    }

    public Map<String, Object> getAttributes(String path) {
        throw new UnsupportedOperationException();
    }

    public void setAttributes(String path, Map<String, Object> newValue) {
        throw new UnsupportedOperationException();
    }

    public long getCreationTimeUtc(String path) {
        throw new UnsupportedOperationException();
    }

    public void setCreationTimeUtc(String path, long newTime) {
        throw new UnsupportedOperationException();
    }

    public long getLastAccessTimeUtc(String path) {
        throw new UnsupportedOperationException();
    }

    public void setLastAccessTimeUtc(String path, long newTime) {
        throw new UnsupportedOperationException();
    }

    public long getLastWriteTimeUtc(String path) {
        throw new UnsupportedOperationException();
    }

    public void setLastWriteTimeUtc(String path, long newTime) {
        throw new UnsupportedOperationException();
    }

    public long getFileLength(String path) {
        if (_files.containsKey(path)) {
            return _files.get(path).getCapacity();
        } else {
            throw new FileNotFoundException("No such file " + path);
        }
    }
}
