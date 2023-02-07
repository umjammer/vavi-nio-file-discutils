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

    private FileOpenDelegate opener;

    public FileOpenEventArgs(String fileName, FileMode mode, FileAccess access, FileShare share, FileOpenDelegate opener) {
        this.fileName = fileName;
        fileMode = mode;
        fileAccess = access;
        fileShare = share;
        this.opener = opener;
    }

    private String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String value) {
        fileName = value;
    }

    private FileMode fileMode;

    /**
     * Gets or sets the {@link FileMode}
     */
    public FileMode getFileMode() {
        return fileMode;
    }

    public void setFileMode(FileMode value) {
        fileMode = value;
    }

    private FileAccess fileAccess;

    /**
     * Gets or sets the {@link FileAccess}
     */
    public FileAccess getFileAccess() {
        return fileAccess;
    }

    public void setFileAccess(FileAccess value) {
        fileAccess = value;
    }

    private FileShare fileShare;

    /**
     * Gets or sets the {@link FileShare}
     */
    public FileShare getFileShare() {
        return fileShare;
    }

    public void setFileShare(FileShare value) {
        fileShare = value;
    }

    private Stream result;

    /**
     * The resulting stream.
     *
     * If this is set to a non null value, this stream is used instead of opening
     * the supplied {@link #fileName}
     */
    public Stream getResult() {
        return result;
    }

    public void setResult(Stream value) {
        result = value;
    }

    /**
     * returns the result from the builtin FileLocator
     *
     * @return a stream
     */
    public Stream getFileStream() {
        return opener.invoke(fileName, fileMode, fileAccess, fileShare);
    }
}
