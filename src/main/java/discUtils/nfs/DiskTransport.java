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

import java.io.File;
import java.io.IOException;
import java.net.URI;

import discUtils.core.DiscFileLocator;
import discUtils.core.FileLocator;
import discUtils.core.VirtualDisk;
import discUtils.core.internal.Utilities;
import discUtils.core.internal.VirtualDiskTransport;
import discUtils.core.internal.VirtualDiskTransportAttribute;
import dotnet4j.io.FileAccess;


@VirtualDiskTransportAttribute(scheme = "nfs")
public final class DiskTransport implements VirtualDiskTransport {

    private String extraInfo;

    private NfsFileSystem fileSystem;

    private String path;

    @Override
    public boolean isRawDisk() {
        return false;
    }

    @Override
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

        fileSystem = new NfsFileSystem(uri.getHost(), bestRoot);
        path = fsPath.substring(bestRoot.length()).replace('/', File.separatorChar);
        extraInfo = uri.getFragment().replaceFirst("^#*", "");
    }

    @Override
    public VirtualDisk openDisk(FileAccess access) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileLocator getFileLocator() {
        return new DiscFileLocator(fileSystem, Utilities.getDirectoryFromPath(path));
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
    public void close() throws IOException {
        if (fileSystem != null) {
            fileSystem.close();
            fileSystem = null;
        }
    }
}
