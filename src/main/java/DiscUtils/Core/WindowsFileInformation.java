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

import java.util.EnumSet;

import DiscUtils.Core.CoreCompat.FileAttributes;


/**
 * Common information for Windows files.
 */
public class WindowsFileInformation {
    /**
     * Gets or sets the last time the file was changed.
     */
    private long _changeTime;

    public long getChangeTime() {
        return _changeTime;
    }

    public void setChangeTime(long value) {
        _changeTime = value;
    }

    /**
     * Gets or sets the creation time of the file.
     */
    private long _creationTime;

    public long getCreationTime() {
        return _creationTime;
    }

    public void setCreationTime(long value) {
        _creationTime = value;
    }

    /**
     * Gets or sets the file attributes.
     */
    private EnumSet<FileAttributes> _fileAttributes;

    public EnumSet<FileAttributes> getFileAttributes() {
        return _fileAttributes;
    }

    public void setFileAttributes(EnumSet<FileAttributes> value) {
        _fileAttributes = value;
    }

    /**
     * Gets or sets the last access time of the file.
     */
    private long _lastAccessTime;

    public long getLastAccessTime() {
        return _lastAccessTime;
    }

    public void setLastAccessTime(long value) {
        _lastAccessTime = value;
    }

    /**
     * Gets or sets the modification time of the file.
     */
    private long _lastWriteTime;

    public long getLastWriteTime() {
        return _lastWriteTime;
    }

    public void setLastWriteTime(long value) {
        _lastWriteTime = value;
    }
}
