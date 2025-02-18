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

package discUtils.iscsi;

import java.io.IOException;
import java.net.URI;

import discUtils.core.FileLocator;
import discUtils.core.VirtualDisk;
import discUtils.core.internal.VirtualDiskTransport;
import discUtils.core.internal.VirtualDiskTransportAttribute;
import dotnet4j.io.FileAccess;


@VirtualDiskTransportAttribute(scheme = "iscsi")
public final class DiskTransport implements VirtualDiskTransport {

    private LunInfo lunInfo;

    private Session session;

    @Override
    public boolean isRawDisk() {
        return true;
    }

    @Override
    public void connect(URI uri, String username, String password) {
        lunInfo = LunInfo.parseUri(uri.toString());
        Initiator initiator = new Initiator();
        initiator.setCredentials(username, password);
        session = initiator.connectTo(lunInfo.getTarget());
    }

    @Override
    public VirtualDisk openDisk(FileAccess access) {
        return session.openDisk(lunInfo.getLun(), access);
    }

    @Override
    public FileLocator getFileLocator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFileName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getExtraInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        if (session != null) {
            session.close();
        }

        session = null;
    }
}
