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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import vavi.util.Debug;

import DiscUtils.Net.Dns.ServiceDiscoveryClient;
import DiscUtils.Net.Dns.ServiceInstance;
import DiscUtils.Net.Dns.ServiceInstanceFields;


/**
 * Provides access to Optical Disc Sharing services.
 */
public final class OpticalDiscServiceClient implements Closeable {

    private ServiceDiscoveryClient _sdClient;

    /**
     * Initializes a new instance of the OpticalDiscServiceClient class.
     */
    public OpticalDiscServiceClient() {
        _sdClient = new ServiceDiscoveryClient();
    }

    /**
     * Disposes of this instance.
     */
    public void close() throws IOException {
        if (_sdClient != null) {
            _sdClient.close();
            _sdClient = null;
        }
    }

    /**
     * Looks up the ODS services advertised.
     *
     * @return A list of discovered ODS services.
     */
    public List<OpticalDiscService> lookupServices() {
        return lookupServices("local.");
    }

    /**
     * Looks up the ODS services advertised in a domain.
     *
     * @param domain The domain to look in.
     * @return A list of discovered ODS services.
     */
    public List<OpticalDiscService> lookupServices(String domain) {
        List<OpticalDiscService> services = new ArrayList<>();
        for (ServiceInstance instance : _sdClient.lookupInstances("_odisk._tcp", domain, ServiceInstanceFields.All)) {
            services.add(new OpticalDiscService(instance, _sdClient));
        }
Debug.println("services: " + services.size());
        return services;
    }
}
