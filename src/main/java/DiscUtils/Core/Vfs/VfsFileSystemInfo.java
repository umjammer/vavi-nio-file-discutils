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

package DiscUtils.Core.Vfs;

import java.util.logging.Level;

import vavi.util.Debug;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.FileSystemInfo;
import DiscUtils.Core.FileSystemParameters;
import DiscUtils.Core.VolumeInfo;
import dotnet4j.io.Stream;


/**
 * Class holding information about a file system.
 */
public final class VfsFileSystemInfo extends FileSystemInfo {
    private final VfsFileSystemOpener _openDelegate;

    /**
     * Initializes a new instance of the VfsFileSystemInfo class.
     *
     * @param name The name of the file system.
     * @param description A one-line description of the file system.
     * @param openDelegate A delegate that can open streams as the indicated
     *            file system.
     */
    public VfsFileSystemInfo(String name, String description, VfsFileSystemOpener openDelegate) {
        _name = name;
        _description = description;
        _openDelegate = openDelegate;
    }

    private String _description;

    /**
     * Gets a one-line description of the file system.
     */
    public String getDescription() {
        return _description;
    }

    private String _name;

    /**
     * Gets the name of the file system.
     */
    public String getName() {
        return _name;
    }

    /**
     * Opens a volume using the file system.
     *
     * @param volume The volume to access.
     * @param parameters Parameters for the file system.
     * @return A file system instance.
     */
    public DiscFileSystem open(VolumeInfo volume, FileSystemParameters parameters) {
Debug.println(Level.INFO, _openDelegate + ", " + volume);
        return _openDelegate.invoke(volume.open(), volume, parameters);
    }

    /**
     * Opens a stream using the file system.
     *
     * @param stream The stream to access.
     * @param parameters Parameters for the file system.
     * @return A file system instance.
     */
    public DiscFileSystem open(Stream stream, FileSystemParameters parameters) {
        return _openDelegate.invoke(stream, null, parameters);
    }
}
