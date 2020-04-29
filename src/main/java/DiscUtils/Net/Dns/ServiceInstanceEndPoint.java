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

package DiscUtils.Net.Dns;

import java.net.InetSocketAddress;
import java.util.List;


/**
 * Represents an endpoint (address, port, etc) that provides a DNS-SD service
 * instance.
 */
public final class ServiceInstanceEndPoint {
    private List<InetSocketAddress> _ipEndPoints;

    public ServiceInstanceEndPoint(int priority, int weight, int port, String address, List<InetSocketAddress> ipEndPoints) {
        _priority = priority;
        _weight = weight;
        _port = port;
        _dnsAddress = address;
        _ipEndPoints = ipEndPoints;
    }

    /**
     * Gets the DNS address of this EndPoint.
     */
    private String _dnsAddress;

    public String getDnsAddress() {
        return _dnsAddress;
    }

    /**
     * Gets the IP addresses (as InetSocketAddress instances) of this EndPoint.
     */
    public List<InetSocketAddress> getInetSocketAddresss() {
        return _ipEndPoints;
    }

    /**
     * Gets the port of this EndPoint.
     */
    private int _port;

    public int getPort() {
        return _port;
    }

    /**
     * Gets the priority of this EndPoint (lower value is higher priority).
     */
    private int _priority;

    public int getPriority() {
        return _priority;
    }

    /**
     * Gets the relative weight of this EndPoint when randomly choosing between
     * EndPoints of equal priority.
     */
    private int _weight;

    public int getWeight() {
        return _weight;
    }
}
