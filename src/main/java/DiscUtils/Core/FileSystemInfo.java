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

import moe.yo3explorer.dotnetio4j.Stream;


/**
 * Base class holding information about a file system.
 *
 * File system implementations derive from this class, to provide information
 * about the file system.
 */
public abstract class FileSystemInfo {
    /**
     * Gets a one-line description of the file system.
     */
    public abstract String getDescription();

    /**
     * Gets the name of the file system.
     */
    public abstract String getName();

    /**
     * Opens a volume using the file system.
     *
     * @param volume The volume to access.
     * @return A file system instance.
     */
    public DiscFileSystem open(VolumeInfo volume) {
        return open(volume, null);
    }

    /**
     * Opens a stream using the file system.
     *
     * @param stream The stream to access.
     * @return A file system instance.
     */
    public DiscFileSystem open(Stream stream) {
        return open(stream, null);
    }

    /**
     * Opens a volume using the file system.
     *
     * @param volume The volume to access.
     * @param parameters Parameters for the file system.
     * @return A file system instance.
     */
    public abstract DiscFileSystem open(VolumeInfo volume, FileSystemParameters parameters);

    /**
     * Opens a stream using the file system.
     *
     * @param stream The stream to access.
     * @param parameters Parameters for the file system.
     * @return A file system instance.
     */
    public abstract DiscFileSystem open(Stream stream, FileSystemParameters parameters);

    /**
     * Gets the name of the file system.
     *
     * @return The file system name.
     */
    public String toString() {
        return getName();
    }

}
