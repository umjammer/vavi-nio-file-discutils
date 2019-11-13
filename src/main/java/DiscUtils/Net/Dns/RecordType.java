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

import java.util.Arrays;

/**
 * Enumeration of the known DNS record types.
 */
public enum RecordType {
    /**
     * No record type defined.
     */
    None(0),
    /**
     * DNS A record.
     */
    Address(1),
    /**
     * DNS NS record.
     */
    NameServer(2),
    /**
     * DNS MD record.
     */
    MailDestination(3),
    /**
     * DNS MF record.
     */
    MailForwarder(4),
    /**
     * DNS CNAME record.
     */
    CanonicalName(5),
    /**
     * DNS SOA record.
     */
    StartOfAuthority(6),
    /**
     * DNS MB record.
     */
    Mailbox(7),
    /**
     * DNS MG record.
     */
    MailGroup(8),
    /**
     * DNS MR record.
     */
    MailRename(9),
    /**
     * DNS NULL record.
     */
    Null(10),
    /**
     * DNS WKS record.
     */
    WellKnownService(11),
    /**
     * DNS PTR record.
     */
    Pointer(12),
    /**
     * DNS HINFO record.
     */
    HostInformation(13),
    /**
     * DNS MINFO record.
     */
    MailboxInformation(14),
    /**
     * DNS MX record.
     */
    MailExchange(15),
    /**
     * DNS TXT record.
     */
    Text(16),
    /**
     * DNS RP record.
     */
    ResponsiblePerson(17),
    /**
     * DNS AAAA record.
     */
    IP6Address(28),
    /**
     * DNS SRV record.
     */
    Service(33),
    /**
     * DNS AXFR record.
     */
    ZoneTransfer(252),
    /**
     * DNS MAILB record.
     */
    MailboxRecords(253),
    /**
     * DNS MAILA record.
     */
    MailAgentRecords(254),
    /**
     * Wildcard matching all records (*).
     */
    All(255);

    private int value;

    public int getValue() {
        return value;
    }

    private RecordType(int value) {
        this.value = value;
    }

    public static RecordType valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.getValue() == value).findFirst().get();
    }
}
