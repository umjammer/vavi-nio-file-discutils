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

import java.util.EnumSet;

/**
 * Flags controlling which details are discovered for a particular DNS-SD
 * service.
 */
public enum ServiceInstanceFields {
    /**
     * Resolves the display name for the service.
     */
    DisplayName, // 0x01
    /**
     * Resolves the parameters for the service (held in TXT records).
     */
    Parameters, // 0x02
    /**
     * Resolves the DNS address for the service (held in SRV records).
     */
    DnsAddresses, // 0x04
    /**
     * Resolves the IP address(es) for the service.
     */
    IPAddresses; // 0x08
    /**
     * Resolves all fields.
     */
    public static final EnumSet<ServiceInstanceFields> All = EnumSet.of(DisplayName, Parameters, DnsAddresses, IPAddresses);
}
