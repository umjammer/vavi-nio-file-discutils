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

import java.time.Instant;

/**
 * Base class for all resource records (DNS RRs).
 */
public class ResourceRecord {
    public ResourceRecord(String name, RecordType type, RecordClass rClass, long expiry) {
        _name = name;
        _recordType = type;
        _class = rClass;
        _expiry = expiry;
    }

    /**
     * Gets the class of record.
     */
    private RecordClass _class;

    public RecordClass getClass_() {
        return _class;
    }

    /**
     * Gets the expiry time of the record.
     */
    private long _expiry;

    public long getExpiry() {
        return _expiry;
    }

    /**
     * Gets the name of the resource (domain).
     */
    private String _name;

    public String getName() {
        return _name;
    }

    /**
     * Gets the type of record.
     */
    private RecordType _recordType;

    public RecordType getRecordType() {
        return _recordType;
    }

    public static ResourceRecord readFrom(PacketReader reader) {
        String name = reader.readName();
        RecordType type = RecordType.valueOf(reader.readUShort());
        RecordClass rClass = RecordClass.valueOf(reader.readUShort());
        long expiry = Instant.now().plusSeconds(reader.readInt()).toEpochMilli();

        switch (type) {
        case Pointer:
            return new PointerRecord(name, type, rClass, expiry, reader);

        case CanonicalName:
            return new CanonicalNameRecord(name, type, rClass, expiry, reader);

        case Address:
            return new IP4AddressRecord(name, type, rClass, expiry, reader);

        case Text:
            return new TextRecord(name, type, rClass, expiry, reader);

        case Service:
            return new ServiceRecord(name, type, rClass, expiry, reader);

        default:
            int len = reader.readUShort();
            reader.setPosition(reader.getPosition() + len);
            return new ResourceRecord(name, type, rClass, expiry);
        }
    }
}
