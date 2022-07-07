//
// Copyright (c) 2017, Bianco Veigel
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

package discUtils.setup;

import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.Stream;


/**
 * Event arguments for opening a file
 */
public class FileOpenEventArgs /* extends EventArgs */ {
    private FileOpenDelegate _opener;

    public FileOpenEventArgs(String fileName, FileMode mode, FileAccess access, FileShare share, FileOpenDelegate opener) {
        _fileName = fileName;
        _fileMode = mode;
        _fileAccess = access;
        _fileShare = share;
        _opener = opener;
    }

    private String _fileName;

    public String getFileName() {
        return _fileName;
    }

    public void setFileName(String value) {
        _fileName = value;
    }

    private FileMode _fileMode;

    /**
     * Gets or sets the {@link FileMode}
     */
    public FileMode getFileMode() {
        return _fileMode;
    }

    public void setFileMode(FileMode value) {
        _fileMode = value;
    }

    private FileAccess _fileAccess;

    /**
     * Gets or sets the {@link FileAccess}
     */
    public FileAccess getFileAccess() {
        return _fileAccess;
    }

    public void setFileAccess(FileAccess value) {
        _fileAccess = value;
    }

    private FileShare _fileShare;

    /**
     * Gets or sets the {@link FileShare}
     */
    public FileShare getFileShare() {
        return _fileShare;
    }

    public void setFileShare(FileShare value) {
        _fileShare = value;
    }

    private Stream _result;

    /**
     * The resulting stream.
     *
     * If this is set to a non null value, this stream is used instead of opening
     * the supplied {@link #_fileName}
     */
    public Stream getResult() {
        return _result;
    }

    public void setResult(Stream value) {
        _result = value;
    }

    /**
     * returns the result from the builtin FileLocator
     *
     * @return
     */
    public Stream getFileStream() {
        return _opener.invoke(_fileName, _fileMode, _fileAccess, _fileShare);
    }
}
