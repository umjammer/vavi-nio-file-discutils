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

package DiscUtils.Iscsi;

import java.io.IOException;
import java.net.URI;

import DiscUtils.Core.FileLocator;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.Internal.VirtualDiskTransport;
import DiscUtils.Core.Internal.VirtualDiskTransportAttribute;
import dotnet4j.io.FileAccess;


@VirtualDiskTransportAttribute(scheme = "iscsi")
public final class DiskTransport extends VirtualDiskTransport {
    private LunInfo _lunInfo;

    private Session _session;

    public boolean isRawDisk() {
        return true;
    }

    public void connect(URI uri, String username, String password) {
        _lunInfo = LunInfo.parseUri(uri.toString());
        Initiator initiator = new Initiator();
        initiator.setCredentials(username, password);
        _session = initiator.connectTo(_lunInfo.getTarget());
    }

    public VirtualDisk openDisk(FileAccess access) {
        return _session.openDisk(_lunInfo.getLun(), access);
    }

    public FileLocator getFileLocator() {
        throw new UnsupportedOperationException();
    }

    public String getFileName() {
        throw new UnsupportedOperationException();
    }

    public String getExtraInfo() {
        throw new UnsupportedOperationException();
    }

    public void close() throws IOException {
        if (_session != null) {
            _session.close();
        }

        _session = null;
    }
}
