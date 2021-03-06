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

package DiscUtils.Nfs;

import java.io.IOException;
import java.net.URI;

import DiscUtils.Core.DiscFileLocator;
import DiscUtils.Core.FileLocator;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Core.Internal.VirtualDiskTransport;
import DiscUtils.Core.Internal.VirtualDiskTransportAttribute;
import dotnet4j.io.FileAccess;


@VirtualDiskTransportAttribute(scheme = "nfs")
public final class DiskTransport extends VirtualDiskTransport {
    private String _extraInfo;

    private NfsFileSystem _fileSystem;

    private String _path;

    public boolean isRawDisk() {
        return false;
    }

    public void connect(URI uri, String username, String password) {
        String fsPath = uri.getPath();
        // Find the best (least specific) export
        String bestRoot = null;
        int bestMatchLength = Integer.MAX_VALUE;
        for (String export : NfsFileSystem.getExports(uri.getHost())) {
            if (fsPath.length() >= export.length()) {
                int matchLength = export.length();
                for (int i = 0; i < export.length(); ++i) {
                    if (export.charAt(i) != fsPath.charAt(i)) {
                        matchLength = i;
                        break;
                    }
                }
                if (matchLength < bestMatchLength) {
                    bestRoot = export;
                    bestMatchLength = matchLength;
                }
            }
        }
        if (bestRoot == null) {
            throw new dotnet4j.io.IOException(String
                    .format("Unable to find an NFS export providing access to '%s'", fsPath));
        }

        _fileSystem = new NfsFileSystem(uri.getHost(), bestRoot);
        _path = fsPath.substring(bestRoot.length()).replace('/', '\\');
        _extraInfo = uri.getFragment().replaceFirst("^#*", "");
    }

    public VirtualDisk openDisk(FileAccess access) {
        throw new UnsupportedOperationException();
    }

    public FileLocator getFileLocator() {
        return new DiscFileLocator(_fileSystem, Utilities.getDirectoryFromPath(_path));
    }

    public String getFileName() {
        return Utilities.getFileFromPath(_path);
    }

    public String getExtraInfo() {
        return _extraInfo;
    }

    public void close() throws IOException {
        if (_fileSystem != null) {
            _fileSystem.close();
            _fileSystem = null;
        }
    }
}
