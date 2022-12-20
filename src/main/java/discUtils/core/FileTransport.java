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

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import discUtils.core.internal.LocalFileLocator;
import discUtils.core.internal.Utilities;
import discUtils.core.internal.VirtualDiskTransport;
import discUtils.core.internal.VirtualDiskTransportAttribute;
import dotnet4j.io.FileAccess;


@VirtualDiskTransportAttribute(scheme = "file")
public final class FileTransport implements VirtualDiskTransport {

    private String extraInfo;

    private String path;

    @Override
    public boolean isRawDisk() {
        return false;
    }

    @Override
    public void connect(URI uri, String username, String password) {
        path = uri.getPath();
        extraInfo = uri.getFragment();
        String path = Utilities.getDirectoryFromPath(this.path);
        if (path == null || !Files.exists(Paths.get(path))) {
            throw new dotnet4j.io.FileNotFoundException(String.format("No such file '%s'", uri));
        }
    }

    @Override
    public VirtualDisk openDisk(FileAccess access) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileLocator getFileLocator() {
        return new LocalFileLocator(Utilities.getDirectoryFromPath(path) + File.separator);
    }

    @Override
    public String getFileName() {
        return Utilities.getFileFromPath(path);
    }

    @Override
    public String getExtraInfo() {
        return extraInfo;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }
}
