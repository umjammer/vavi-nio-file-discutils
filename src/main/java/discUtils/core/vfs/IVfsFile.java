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

package discUtils.core.vfs;

import java.util.EnumSet;

import discUtils.core.coreCompat.FileAttributes;
import discUtils.streams.buffer.IBuffer;


/**
 * Interface implemented by a class representing a file.
 *
 * File system implementations should have a class that implements this
 * interface. If the file system implementation is read-only, it is
 * acceptable to throw
 * {@code UnsupportedOperationException}
 * from setters.
 */
public interface IVfsFile {
    /**
     * Gets or sets the last creation time in UTC.
     */
    long getCreationTimeUtc();

    void setCreationTimeUtc(long value);

    /**
     * Gets or sets the file's attributes.
     */
    EnumSet<FileAttributes> getFileAttributes();

    void setFileAttributes(EnumSet<FileAttributes> value);

    /**
     * Gets a buffer to access the file's contents.
     */
    IBuffer getFileContent();

    /**
     * Gets the length of the file.
     */
    long getFileLength();

    /**
     * Gets or sets the last access time in UTC.
     */
    long getLastAccessTimeUtc();

    void setLastAccessTimeUtc(long value);

    /**
     * Gets or sets the last write time in UTC.
     */
    long getLastWriteTimeUtc();

    void setLastWriteTimeUtc(long value);

}
