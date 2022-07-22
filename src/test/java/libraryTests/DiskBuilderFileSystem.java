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

package libraryTests;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import discUtils.core.DiscFileSystem;
import discUtils.core.DiskImageFileSpecification;
import discUtils.streams.SparseStream;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileNotFoundException;
import dotnet4j.io.Stream;


/**
 * Minimal implementation of DiscFileSystem, sufficient to support unit-testing
 * of disk formats.
 */
public class DiskBuilderFileSystem extends DiscFileSystem {

    private Map<String, DiskImageFileSpecification> files;

    public DiskBuilderFileSystem(List<DiskImageFileSpecification> fileSpecs) {
        files = new HashMap<>();
        for (DiskImageFileSpecification fileSpec : fileSpecs) {
            files.put(fileSpec.getName(), fileSpec);
        }
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
        throw new UnsupportedOperationException();
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
        if (files.containsKey(path)) {
            if (mode == FileMode.CreateNew) {
                throw new dotnet4j.io.IOException("File already exists");
            }

            return files.get(path).openStream();
        } else {
            throw new FileNotFoundException();
        }
    }

    public Map<String, Object> getAttributes(String path) {
        throw new UnsupportedOperationException();
    }

    public void setAttributes(String path,  Map<String, Object> newValue) {
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
        if (files.containsKey(path)) {
            try (Stream s = files.get(path).openStream()){
                    return s.getLength();
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        } else {
            throw new FileNotFoundException("No such file " + path);
        }
    }
}
