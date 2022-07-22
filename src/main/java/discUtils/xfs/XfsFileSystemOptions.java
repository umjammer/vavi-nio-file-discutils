//
// Copyright (c) 2016, Bianco Veigel
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

package discUtils.xfs;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import discUtils.core.DiscFileSystemOptions;
import discUtils.core.FileSystemParameters;


/**
 * XFS file system options.
 */
public final class XfsFileSystemOptions extends DiscFileSystemOptions {

    public XfsFileSystemOptions() {
        setFileNameEncoding(StandardCharsets.UTF_8);
    }

    public XfsFileSystemOptions(FileSystemParameters parameters) {
        if (parameters != null && parameters.getFileNameEncoding() != null) {
            fileNameEncoding = parameters.getFileNameEncoding();
        } else {
            fileNameEncoding = StandardCharsets.UTF_8;
        }
    }

    /**
     * Gets or sets the character encoding used for file names.
     */
    private Charset fileNameEncoding;

    public Charset getFileNameEncoding() {
        return fileNameEncoding;
    }

    public void setFileNameEncoding(Charset value) {
        fileNameEncoding = value;
    }
}
