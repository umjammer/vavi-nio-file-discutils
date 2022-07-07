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

package discUtils.core;

import java.util.EnumSet;

import discUtils.core.coreCompat.FileAttributes;


/**
 * Standard Unix-style file type.
 *
 * use ordinal
 */
public enum UnixFileType {
    /**
     * No type specified.
     */
    None,
    /**
     * A FIFO / Named Pipe.
     */
    Fifo,
    /**
     * A character device.
     */
    Character,
    __dummyEnum__3,
    /**
     * A normal directory.
     */
    Directory,
    __dummyEnum__5,
    /**
     * A block device.
     */
    Block,
    __dummyEnum__7,
    /**
     * A regular file.
     */
    Regular,
    __dummyEnum__9,
    /**
     * A soft link.
     */
    Link,
    __dummyEnum__b,
    /**
     * A unix socket.
     */
    Socket;

    public static EnumSet<FileAttributes> toFileAttributes(UnixFileType fileType) {
        switch (fileType) {
        case Fifo:
            return EnumSet.of(FileAttributes.Device, FileAttributes.System);
        case Character:
            return EnumSet.of(FileAttributes.Device, FileAttributes.System);
        case Directory:
            return EnumSet.of(FileAttributes.Directory);
        case Block:
            return EnumSet.of(FileAttributes.Device, FileAttributes.System);
        case Regular:
            return EnumSet.of(FileAttributes.Normal);
        case Link:
            return EnumSet.of(FileAttributes.ReparsePoint);
        case Socket:
            return EnumSet.of(FileAttributes.Device, FileAttributes.System);
        default:
            return EnumSet.noneOf(FileAttributes.class);
        }
    }
}
