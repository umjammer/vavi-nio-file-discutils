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

public enum UnixFileType {
    /**
     * Standard Unix-style file type.
     *
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
    __dummyEnum__0,
    /**
     * A normal directory.
     */
    Directory,
    __dummyEnum__1,
    /**
     * A block device.
     */
    Block,
    __dummyEnum__2,
    /**
     * A regular file.
     */
    Regular,
    __dummyEnum__3,
    /**
     * A soft link.
     */
    Link,
    __dummyEnum__4,
    /**
     * A unix socket.
     */
    Socket;

    public static UnixFileType valueOf(int value) {
        return values()[value];
    }
}
