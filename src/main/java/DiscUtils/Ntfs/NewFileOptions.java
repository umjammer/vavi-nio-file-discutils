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

package DiscUtils.Ntfs;

import java.security.SecurityPermission;
import java.util.Optional;

/**
 * Options controlling how new NTFS files are created.
 */
public final class NewFileOptions {
    /**
     * Initializes a new instance of the NewFileOptions class.
     */
    public NewFileOptions() {
        setCompressed(Optional.empty());
        setCreateShortNames(Optional.empty());
        setSecurityDescriptor(null);
    }

    /**
     * Gets or sets whether the new file should be compressed.
     * The default (
     * {@code null}
     * ) value indicates the file system default behaviour applies.
     */
    private Optional<Boolean> __Compressed;

    public Optional<Boolean> getCompressed() {
        return __Compressed;
    }

    public void setCompressed(Optional<Boolean> value) {
        __Compressed = value;
    }

    /**
     * Gets or sets whether a short name should be created for the file.
     * The default (
     * {@code null}
     * ) value indicates the file system default behaviour applies.
     */
    private Optional<Boolean> __CreateShortNames;

    public Optional<Boolean> getCreateShortNames() {
        return __CreateShortNames;
    }

    public void setCreateShortNames(Optional<Boolean> value) {
        __CreateShortNames = value;
    }

    /**
     * Gets or sets the security descriptor that to set for the new file.
     * The default (
     * {@code null}
     * ) value indicates the security descriptor is inherited.
     */
    private SecurityPermission __SecurityDescriptor;

    public SecurityPermission getSecurityDescriptor() {
        return __SecurityDescriptor;
    }

    public void setSecurityDescriptor(SecurityPermission value) {
        __SecurityDescriptor = value;
    }
}
