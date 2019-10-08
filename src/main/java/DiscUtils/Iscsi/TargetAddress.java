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

import java.net.URI;


/**
 * Information about an iSCSI Target.
 *
 * A target contains zero or more LUNs.
 */
public class TargetAddress {
    public static final int DefaultPort = 3260;

    /**
     * Initializes a new instance of the TargetAddress class.
     *
     * @param address The IP address (or FQDN) of the Target.
     * @param port The network port of the Target.
     * @param targetGroupTag The Group Tag of the Target.
     */
    public TargetAddress(String address, int port, String targetGroupTag) {
        __NetworkAddress = address;
        __NetworkPort = port;
        __TargetGroupTag = targetGroupTag;
    }

    /**
     * Gets the IP address (or FQDN) of the Target.
     */
    private String __NetworkAddress;

    public String getNetworkAddress() {
        return __NetworkAddress;
    }

    /**
     * Gets the network port of the Target.
     */
    private int __NetworkPort;

    public int getNetworkPort() {
        return __NetworkPort;
    }

    /**
     * Gets the Group Tag of the Target.
     */
    private String __TargetGroupTag;

    public String getTargetGroupTag() {
        return __TargetGroupTag;
    }

    /**
     * Parses a Target address in string form.
     *
     * @param address The address to parse.
     * @return The structured address.
     */
    public static TargetAddress parse(String address) {
        int addrEnd = address.indexOf(':') & address.indexOf(',');
        if (addrEnd == -1) {
            return new TargetAddress(address, DefaultPort, "");
        }

        String addr = address.substring(0, addrEnd);
        int port = DefaultPort;
        String targetGroupTag = "";
        int focus = addrEnd;
        if (address.charAt(focus) == ':') {
            int portStart = addrEnd + 1;
            int portEnd = address.indexOf(',', portStart);
            if (portEnd == -1) {
                port = Integer.parseInt(address.substring(portStart));
                focus = address.length();
            } else {
                port = Integer.parseInt(address.substring(portStart, portEnd - portStart));
                focus = portEnd;
            }
        }

        if (focus < address.length()) {
            targetGroupTag = address.substring(focus + 1);
        }

        return new TargetAddress(addr, port, targetGroupTag);
    }

    /**
     * Gets the TargetAddress in string format.
     *
     * @return The string in 'host:port,targetgroup' format.
     */
    public String toString() {
        String result = getNetworkAddress();
        if (getNetworkPort() != DefaultPort) {
            result += ":" + getNetworkPort();
        }

        if (getTargetGroupTag() != null && !getTargetGroupTag().isEmpty()) {
            result += "," + getTargetGroupTag();
        }

        return result;
    }

    /**
     * Gets the target address as a URI.
     *
     * @return The target address in the form: iscsi://host[:port][/grouptag].
     */
    public URI toUri() {
        URI builder = URI.create("iscsi" + "://" + getNetworkAddress() + ":" +
                                 (getNetworkPort() != DefaultPort ? getNetworkPort() : -1) + "/" +
                                 (getTargetGroupTag() == null || getTargetGroupTag().isEmpty() ? "" : getTargetGroupTag()));
        return builder;
    }
}
