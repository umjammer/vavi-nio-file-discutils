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

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import discUtils.core.internal.LocalFileLocator;
import discUtils.core.internal.Utilities;
import discUtils.core.internal.VirtualDiskTransport;
import discUtils.core.internal.VirtualDiskTransportAttribute;
import dotnet4j.io.FileAccess;


@VirtualDiskTransportAttribute(scheme = "file")
public final class FileTransport extends VirtualDiskTransport {

    private String _extraInfo;

    private String _path;

    public boolean isRawDisk() {
        return false;
    }

    public void connect(URI uri, String username, String password) {
        _path = uri.getPath().replace("/", "\\");
        _extraInfo = uri.getFragment();
        String path = Utilities.getDirectoryFromPath(_path);
        if (path == null || !Files.exists(Paths.get(path.replace("\\", "/")))) {
            throw new dotnet4j.io.FileNotFoundException(String.format("No such file '%s'", uri));
        }
    }

    public VirtualDisk openDisk(FileAccess access) {
        throw new UnsupportedOperationException();
    }

    public FileLocator getFileLocator() {
        return new LocalFileLocator(Utilities.getDirectoryFromPath(_path) + "\\");
    }

    public String getFileName() {
        return Utilities.getFileFromPath(_path);
    }

    public String getExtraInfo() {
        return _extraInfo;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }
}
