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

import java.util.Arrays;

/**
 * base class for DNS clients.
 */
public abstract class DnsClient {
    /**
     * Flushes any cached DNS records.
     */
    public abstract void flushCache();

    /**
     * Looks up a record in DNS.
     *
     * @param name The name to lookup.
     * @param type The type of record requested.
     * @return The records returned by the DNS server, if any.
     */
    public abstract ResourceRecord[] lookup(String name, RecordType type);

    public static String normalizeDomainName(String name) {
        String[] labels = Arrays.stream(name.split("\\.")).filter(e -> !e.isEmpty()).toArray(String[]::new);
        return String.join(".", labels) + ".";
    }
}
