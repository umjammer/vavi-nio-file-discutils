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

package DiscUtils.Core.Archives;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.Stream;


/**
 * Minimal tar file format implementation.
 */
public final class TarFile {
    private final Map<String, FileRecord> _files;

    private final Stream _fileStream;

    /**
     * Initializes a new instance of the TarFile class.
     *
     * @param fileStream The Tar file.
     */
    public TarFile(Stream fileStream) {
        _fileStream = fileStream;
        _files = new HashMap<>();
        TarHeader hdr = new TarHeader();
        byte[] hdrBuf = StreamUtilities.readExact(_fileStream, TarHeader.Length);
        hdr.readFrom(hdrBuf, 0);
        while (hdr.FileLength != 0 || (hdr.FileName != null && !hdr.FileName.isEmpty())) {
            FileRecord record = new FileRecord(hdr.FileName, _fileStream.getPosition(), hdr.FileLength);
            _files.put(record.Name, record);
            _fileStream.setPosition(_fileStream.getPosition() + (hdr.FileLength + 511) / 512 * 512);
            hdrBuf = StreamUtilities.readExact(_fileStream, TarHeader.Length);
            hdr.readFrom(hdrBuf, 0);
        }
    }

    /**
     * Tries to open a file contained in the archive, if it exists.
     *
     * @param path The path to the file within the archive.
     * @param stream {@cs out} A stream containing the file contents, or null.
     * @return {@code true} if the file could be opened, else {@code false} .
     */
    public boolean tryOpenFile(String path, Stream[] stream) {
        if (_files.containsKey(path)) {
            FileRecord file = _files.get(path);
            stream[0] = new SubStream(_fileStream, file.Start, file.Length);
            return true;
        }

        stream[0] = null;
        return false;
    }

    /**
     * Open a file contained in the archive.
     *
     * @param path The path to the file within the archive.
     * @return A stream containing the file contents.
     * @throws dotnet4j.io.FileNotFoundException Thrown if the file is not
     *             found.
     */
    public Stream openFile(String path) {
        if (_files.containsKey(path)) {
            FileRecord file = _files.get(path);
            return new SubStream(_fileStream, file.Start, file.Length);
        }

        throw new dotnet4j.io.FileNotFoundException("File is not in archive: " + path);
    }

    /**
     * Determines if a given file exists in the archive.
     *
     * @param path The file path to test.
     * @return {@code true} if the file is present, else {@code false} .
     */
    public boolean fileExists(String path) {
        return _files.containsKey(path);
    }

    /**
     * Determines if a given directory exists in the archive.
     *
     * @param path The file path to test.
     * @return {@code true} if the directory is present, else {@code false} .
     */
    public boolean dirExists(String path) {
        String searchStr = path;
        searchStr = searchStr.replace("\\", "/");
        searchStr = searchStr.endsWith("/") ? searchStr : searchStr + "/";
        for (String filePath : _files.keySet()) {
            if (filePath.startsWith(searchStr)) {
                return true;
            }
        }
        return false;
    }

    public List<FileRecord> getFiles(String dir) {
        List<FileRecord> result = new ArrayList<>();
        String searchStr = dir;
        searchStr = searchStr.replace("\\", "/");
        searchStr = searchStr.endsWith("/") ? searchStr : searchStr + "/";
        for (String filePath : _files.keySet()) {
            if (filePath.startsWith(searchStr)) {
                result.add(_files.get(filePath));
            }
        }
        return result;
    }
}
