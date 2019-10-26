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

package DiscUtils.OpticalDiscSharing;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import DiscUtils.Core.FileLocator;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.Internal.VirtualDiskTransport;
import DiscUtils.Core.Internal.VirtualDiskTransportAttribute;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileNotFoundException;


@VirtualDiskTransportAttribute(scheme = "ods")
public final class DiscTransport extends VirtualDiskTransport {
    private String _disk;

    private OpticalDiscServiceClient _odsClient;

    private OpticalDiscService _service;

    public boolean isRawDisk() {
        return true;
    }

    public void connect(URI uri, String username, String password) {
        try {
            String domain = uri.getHost();
            String[] pathParts;
            pathParts = Arrays.stream(URLDecoder.decode(uri.getPath(), StandardCharsets.UTF_8.name()).split("/"))
                    .filter(e -> !e.isEmpty())
                    .toArray(String[]::new);
            String instance = pathParts[0];
            String volName = pathParts[1];
            _odsClient = new OpticalDiscServiceClient();
            for (OpticalDiscService service : _odsClient.lookupServices(domain)) {
                if (service.getDisplayName().equals(instance)) {
                    _service = service;
                    _service.connect(System.getProperty("user.name"), InetAddress.getLocalHost().getHostName(), 30);
                    for (DiscInfo disk : _service.getAdvertisedDiscs()) {
                        if (disk.getVolumeLabel().equals(volName)) {
                            _disk = disk.getName();
                        }
                    }
                }
            }
            if (_disk == null) {
                throw new FileNotFoundException("No such disk " + uri);
            }
        } catch (UnsupportedEncodingException | UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    public VirtualDisk openDisk(FileAccess access) {
        return _service.openDisc(_disk);
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
        if (_odsClient != null) {
            _odsClient.close();
            _odsClient = null;
        }
    }
}
