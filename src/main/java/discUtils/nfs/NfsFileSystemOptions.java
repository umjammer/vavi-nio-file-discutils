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

package discUtils.nfs;

import java.util.EnumSet;

import discUtils.core.DiscFileSystemOptions;
import discUtils.core.UnixFilePermissions;


/**
 * Options controlling the behaviour of NFS file system.
 */
public final class NfsFileSystemOptions extends DiscFileSystemOptions {

    /**
     * Initializes a new instance of the NfsFileSystemOptions class.
     */
    public NfsFileSystemOptions() {
        setNewFilePermissions(EnumSet.of(UnixFilePermissions.OwnerRead,
                                         UnixFilePermissions.OwnerWrite,
                                         UnixFilePermissions.GroupRead,
                                         UnixFilePermissions.GroupWrite));
        setNewDirectoryPermissions(EnumSet.of(UnixFilePermissions.OwnerRead,
                                              UnixFilePermissions.OwnerWrite,
                                              UnixFilePermissions.OwnerExecute,
                                              UnixFilePermissions.GroupRead,
                                              UnixFilePermissions.GroupWrite,
                                              UnixFilePermissions.GroupExecute));
    }

    /**
     * Gets or sets the permission mask to apply to newly created directories.
     */
    private EnumSet<UnixFilePermissions> newDirectoryPermissions;

    public EnumSet<UnixFilePermissions> getNewDirectoryPermissions() {
        return newDirectoryPermissions;
    }

    public void setNewDirectoryPermissions(EnumSet<UnixFilePermissions> value) {
        newDirectoryPermissions = value;
    }

    /**
     * Gets or sets the permission mask to apply to newly created files.
     */
    private EnumSet<UnixFilePermissions> newFilePermissions;

    public EnumSet<UnixFilePermissions> getNewFilePermissions() {
        return newFilePermissions;
    }

    public void setNewFilePermissions(EnumSet<UnixFilePermissions> value) {
        newFilePermissions = value;
    }
}
