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

package discUtils.net.dns;

import java.util.List;
import java.util.Map;


/**
 * Represents an instance of a type of DNS-SD service.
 */
public final class ServiceInstance {

    public ServiceInstance(String name) {
        this.name = name;
    }

    /**
     * Gets the display name for the service instance.
     */
    private String displayName;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String value) {
        displayName = value;
    }

    /**
     * Gets the EndPoints that service this instance.
     */
    private List<ServiceInstanceEndPoint> endpoints;

    public List<ServiceInstanceEndPoint> getEndPoints() {
        return endpoints;
    }

    public void setEndPoints(List<ServiceInstanceEndPoint> value) {
        endpoints = value;
    }

    /**
     * Gets the network name for the service instance (think of this as the
     * unique key).
     */
    private String name;

    public String getName() {
        return name;
    }

    /**
     * Gets the parameters of the service instance.
     */
    private Map<String, byte[]> parameters;

    public Map<String, byte[]> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, byte[]> value) {
        parameters = value;
    }
}
