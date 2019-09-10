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

package DiscUtils.Core;

import java.nio.file.attribute.BasicFileAttributes;


/**
 * Common information for Windows files.
 */
public class WindowsFileInformation {
    /**
     * Gets or sets the last time the file was changed.
     */
    private long __ChangeTime;

    public long getChangeTime() {
        return __ChangeTime;
    }

    public void setChangeTime(long value) {
        __ChangeTime = value;
    }

    /**
     * Gets or sets the creation time of the file.
     */
    private long __CreationTime;

    public long getCreationTime() {
        return __CreationTime;
    }

    public void setCreationTime(long value) {
        __CreationTime = value;
    }

    /**
     * Gets or sets the file attributes.
     */
    private BasicFileAttributes __FileAttributes;

    public BasicFileAttributes getFileAttributes() {
        return __FileAttributes;
    }

    public void setFileAttributes(BasicFileAttributes value) {
        __FileAttributes = value;
    }

    /**
     * Gets or sets the last access time of the file.
     */
    private long __LastAccessTime;

    public long getLastAccessTime() {
        return __LastAccessTime;
    }

    public void setLastAccessTime(long value) {
        __LastAccessTime = value;
    }

    /**
     * Gets or sets the modification time of the file.
     */
    private long __LastWriteTime;

    public long getLastWriteTime() {
        return __LastWriteTime;
    }

    public void setLastWriteTime(long value) {
        __LastWriteTime = value;
    }

}
