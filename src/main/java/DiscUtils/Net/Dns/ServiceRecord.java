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

/**
 * Represents a DNS SRV record.
 */
public final class ServiceRecord extends ResourceRecord {
    private final short _port;

    private final short _priority;

    private final short _weight;

    public ServiceRecord(String name, RecordType type, RecordClass rClass, long expiry, PacketReader reader) {
        super(name, type, rClass, expiry);
        short dataLen = reader.readUShort();
        int pos = reader.getPosition();
        _priority = reader.readUShort();
        _weight = reader.readUShort();
        _port = reader.readUShort();
        _target = reader.readName();
        reader.setPosition(pos + dataLen);
    }

    /**
     * Gets the network port at which the service can be accessed.
     */
    public int getPort() {
        return _port & 0xffff;
    }

    /**
     * Gets the priority associated with this service record (lower value is
     * higher priority).
     */
    public int getPriority() {
        return _priority & 0xffff;
    }

    /**
     * Gets the DNS name at which the service can be accessed.
     */
    private String _target;

    public String getTarget() {
        return _target;
    }

    /**
     * Gets the relative weight associated with this service record when
     * randomly choosing between records of equal priority.
     */
    public int getWeight() {
        return _weight;
    }
}
