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

package discUtils.opticalDiscSharing;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import discUtils.core.FileLocator;
import discUtils.core.VirtualDisk;
import discUtils.core.internal.VirtualDiskTransport;
import discUtils.core.internal.VirtualDiskTransportAttribute;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileNotFoundException;

import static java.lang.System.getLogger;


@VirtualDiskTransportAttribute(scheme = "ods")
public final class DiscTransport implements VirtualDiskTransport {

    private static final Logger logger = getLogger(DiscTransport.class.getName());

    private String disk;

    private OpticalDiscServiceClient odsClient;

    private OpticalDiscService service;

    @Override
    public boolean isRawDisk() {
        return true;
    }

    @Override
    public void connect(URI uri, String username, String password) {
        try {
            String domain = uri.getHost();
            String[] pathParts;
            pathParts = Arrays.stream(URLDecoder.decode(uri.getPath(), StandardCharsets.UTF_8.name()).split("/"))
                    .filter(e -> !e.isEmpty())
                    .toArray(String[]::new);
            String instance = pathParts[0];
            String volName = pathParts[1];

            odsClient = new OpticalDiscServiceClient();
            for (OpticalDiscService service : odsClient.lookupServices(domain)) {
logger.log(Level.DEBUG, "service: " + service.getDisplayName());
                if (service.getDisplayName().equals(instance)) {
                    this.service = service;
                    this.service.connect(System.getProperty("user.name"), InetAddress.getLocalHost().getHostName(), 30);

                    for (DiscInfo disk : this.service.getAdvertisedDiscs()) {
                        if (disk.getVolumeLabel().equals(volName)) {
                            this.disk = disk.getName();
                        }
                    }
                }
            }

            if (disk == null) {
                throw new FileNotFoundException("No such disk " + uri);
            }
        } catch (UnsupportedEncodingException | UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public VirtualDisk openDisk(FileAccess access) {
        return service.openDisc(disk);
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
        if (odsClient != null) {
            odsClient.close();
            odsClient = null;
        }
    }
}
