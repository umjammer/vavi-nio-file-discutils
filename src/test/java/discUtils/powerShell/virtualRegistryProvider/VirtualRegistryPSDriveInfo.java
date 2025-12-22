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

package discUtils.powerShell.virtualRegistryProvider;

import java.io.IOException;

import discUtils.powerShell.conpat.PSDriveInfo;
import discUtils.registry.RegistryHive;
import discUtils.streams.util.Ownership;
import dotnet4j.io.Stream;


public final class VirtualRegistryPSDriveInfo extends PSDriveInfo {

    private final Stream hiveStream;

    private RegistryHive hive;

    public VirtualRegistryPSDriveInfo(PSDriveInfo toCopy, String root, Stream stream) {
        super(toCopy.getName(), toCopy.getProvider(), root, toCopy.getDescription(), toCopy.getCredential());
        hiveStream = stream;
        hive = new RegistryHive(hiveStream, Ownership.Dispose);
    }

    public void close() throws IOException {
        if (hive != null) {
            hive.close();
            hive = null;
        }
    }

    public RegistryHive getHive() {
        return hive;
    }

}
