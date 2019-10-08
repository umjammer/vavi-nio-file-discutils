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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import moe.yo3explorer.dotnetio4j.IOException;

/**
 * Represents a DNS A record.
 */
public final class IP4AddressRecord extends ResourceRecord {
    public IP4AddressRecord(String name, RecordType type, RecordClass rClass, long expiry, PacketReader reader) {
        super(name, type, rClass, expiry);
        try {
            short dataLen = reader.readUShort();
            int pos = reader.getPosition();
            __Address = Inet4Address.getByAddress(reader.readBytes(dataLen));
            reader.setPosition(pos + dataLen);
        } catch (UnknownHostException e) {
            throw new IOException(e);
        }
    }

    /**
     * Gets the IPv4 address.
     */
    private InetAddress __Address;

    public InetAddress getAddress() {
        return __Address;
    }
}