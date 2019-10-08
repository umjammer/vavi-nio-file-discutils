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

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import DiscUtils.Core.Internal.LocalFileLocator;
import DiscUtils.Core.Internal.VirtualDiskTransport;
import DiscUtils.Core.Internal.VirtualDiskTransportAttribute;
import moe.yo3explorer.dotnetio4j.FileAccess;


@VirtualDiskTransportAttribute(scheme = "file")
public final class FileTransport extends VirtualDiskTransport {
    private String _extraInfo;

    private String _path;

    public boolean getIsRawDisk() {
        return false;
    }

    public void connect(URI uri, String username, String password) {
        _path = uri.getPath();
        _extraInfo = uri.getFragment();
        if (!Files.exists(Paths.get(_path).getParent())) {
            throw new moe.yo3explorer.dotnetio4j.FileNotFoundException(String.format("No such file '%s'", uri.toString()));
        }

    }

    public VirtualDisk openDisk(FileAccess access) {
        throw new UnsupportedOperationException();
    }

    public FileLocator getFileLocator() {
        return new LocalFileLocator(Paths.get(_path).getParent() + "\\");
    }

    public String getFileName() {
        return Paths.get(_path).getFileName().toString();
    }

    public String getExtraInfo() {
        return _extraInfo;
    }

}
