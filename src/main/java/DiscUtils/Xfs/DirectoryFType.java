//
// Copyright (c) 2019, Bianco Veigel
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

package DiscUtils.Xfs;

import java.util.Arrays;

public enum DirectoryFType {
    /**
     * Inode type
     *
     * Entry points to a file.
     */
    __dummyEnum__0,
    File,
    /**
     * Entry points to another directory.
     */
    Directory,
    /**
     * Entry points to a character device.
     */
    CharDevice,
    /**
     * Entry points to a block device.
     */
    BlockDevice,
    /**
     * Entry points to a FIFO.
     */
    Fifo,
    /**
     * Entry points to a socket.
     */
    Socket,
    /**
     * Entry points to a symbolic link.
     */
    Symlink,
    /**
     * Entry points to an overlayfs whiteout file.
     */
    Whiteout;

    public static DirectoryFType valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.ordinal() == value).findFirst().get();
    }
}
