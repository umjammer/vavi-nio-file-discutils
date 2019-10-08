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

public enum RecordType {
    /**
     * Enumeration of the known DNS record types.
     *
     * No record type defined.
     */
    None,
    /**
     * DNS A record.
     */
    Address,
    /**
     * DNS NS record.
     */
    NameServer,
    /**
     * DNS MD record.
     */
    MailDestination,
    /**
     * DNS MF record.
     */
    MailForwarder,
    /**
     * DNS CNAME record.
     */
    CanonicalName,
    /**
     * DNS SOA record.
     */
    StartOfAuthority,
    /**
     * DNS MB record.
     */
    Mailbox,
    /**
     * DNS MG record.
     */
    MailGroup,
    /**
     * DNS MR record.
     */
    MailRename,
    /**
     * DNS NULL record.
     */
    Null,
    /**
     * DNS WKS record.
     */
    WellKnownService,
    /**
     * DNS PTR record.
     */
    Pointer,
    /**
     * DNS HINFO record.
     */
    HostInformation,
    /**
     * DNS MINFO record.
     */
    MailboxInformation,
    /**
     * DNS MX record.
     */
    MailExchange,
    /**
     * DNS TXT record.
     */
    Text,
    /**
     * DNS RP record.
     */
    ResponsiblePerson,
    __dummyEnum__0,
    __dummyEnum__1,
    __dummyEnum__2,
    __dummyEnum__3,
    __dummyEnum__4,
    __dummyEnum__5,
    __dummyEnum__6,
    __dummyEnum__7,
    __dummyEnum__8,
    __dummyEnum__9,
    /**
     * DNS AAAA record.
     */
    IP6Address,
    __dummyEnum__10,
    __dummyEnum__11,
    __dummyEnum__12,
    __dummyEnum__13,
    /**
     * DNS SRV record.
     */
    Service,
    __dummyEnum__14,
    __dummyEnum__15,
    __dummyEnum__16,
    __dummyEnum__17,
    __dummyEnum__18,
    __dummyEnum__19,
    __dummyEnum__20,
    __dummyEnum__21,
    __dummyEnum__22,
    __dummyEnum__23,
    __dummyEnum__24,
    __dummyEnum__25,
    __dummyEnum__26,
    __dummyEnum__27,
    __dummyEnum__28,
    __dummyEnum__29,
    __dummyEnum__30,
    __dummyEnum__31,
    __dummyEnum__32,
    __dummyEnum__33,
    __dummyEnum__34,
    __dummyEnum__35,
    __dummyEnum__36,
    __dummyEnum__37,
    __dummyEnum__38,
    __dummyEnum__39,
    __dummyEnum__40,
    __dummyEnum__41,
    __dummyEnum__42,
    __dummyEnum__43,
    __dummyEnum__44,
    __dummyEnum__45,
    __dummyEnum__46,
    __dummyEnum__47,
    __dummyEnum__48,
    __dummyEnum__49,
    __dummyEnum__50,
    __dummyEnum__51,
    __dummyEnum__52,
    __dummyEnum__53,
    __dummyEnum__54,
    __dummyEnum__55,
    __dummyEnum__56,
    __dummyEnum__57,
    __dummyEnum__58,
    __dummyEnum__59,
    __dummyEnum__60,
    __dummyEnum__61,
    __dummyEnum__62,
    __dummyEnum__63,
    __dummyEnum__64,
    __dummyEnum__65,
    __dummyEnum__66,
    __dummyEnum__67,
    __dummyEnum__68,
    __dummyEnum__69,
    __dummyEnum__70,
    __dummyEnum__71,
    __dummyEnum__72,
    __dummyEnum__73,
    __dummyEnum__74,
    __dummyEnum__75,
    __dummyEnum__76,
    __dummyEnum__77,
    __dummyEnum__78,
    __dummyEnum__79,
    __dummyEnum__80,
    __dummyEnum__81,
    __dummyEnum__82,
    __dummyEnum__83,
    __dummyEnum__84,
    __dummyEnum__85,
    __dummyEnum__86,
    __dummyEnum__87,
    __dummyEnum__88,
    __dummyEnum__89,
    __dummyEnum__90,
    __dummyEnum__91,
    __dummyEnum__92,
    __dummyEnum__93,
    __dummyEnum__94,
    __dummyEnum__95,
    __dummyEnum__96,
    __dummyEnum__97,
    __dummyEnum__98,
    __dummyEnum__99,
    __dummyEnum__100,
    __dummyEnum__101,
    __dummyEnum__102,
    __dummyEnum__103,
    __dummyEnum__104,
    __dummyEnum__105,
    __dummyEnum__106,
    __dummyEnum__107,
    __dummyEnum__108,
    __dummyEnum__109,
    __dummyEnum__110,
    __dummyEnum__111,
    __dummyEnum__112,
    __dummyEnum__113,
    __dummyEnum__114,
    __dummyEnum__115,
    __dummyEnum__116,
    __dummyEnum__117,
    __dummyEnum__118,
    __dummyEnum__119,
    __dummyEnum__120,
    __dummyEnum__121,
    __dummyEnum__122,
    __dummyEnum__123,
    __dummyEnum__124,
    __dummyEnum__125,
    __dummyEnum__126,
    __dummyEnum__127,
    __dummyEnum__128,
    __dummyEnum__129,
    __dummyEnum__130,
    __dummyEnum__131,
    __dummyEnum__132,
    __dummyEnum__133,
    __dummyEnum__134,
    __dummyEnum__135,
    __dummyEnum__136,
    __dummyEnum__137,
    __dummyEnum__138,
    __dummyEnum__139,
    __dummyEnum__140,
    __dummyEnum__141,
    __dummyEnum__142,
    __dummyEnum__143,
    __dummyEnum__144,
    __dummyEnum__145,
    __dummyEnum__146,
    __dummyEnum__147,
    __dummyEnum__148,
    __dummyEnum__149,
    __dummyEnum__150,
    __dummyEnum__151,
    __dummyEnum__152,
    __dummyEnum__153,
    __dummyEnum__154,
    __dummyEnum__155,
    __dummyEnum__156,
    __dummyEnum__157,
    __dummyEnum__158,
    __dummyEnum__159,
    __dummyEnum__160,
    __dummyEnum__161,
    __dummyEnum__162,
    __dummyEnum__163,
    __dummyEnum__164,
    __dummyEnum__165,
    __dummyEnum__166,
    __dummyEnum__167,
    __dummyEnum__168,
    __dummyEnum__169,
    __dummyEnum__170,
    __dummyEnum__171,
    __dummyEnum__172,
    __dummyEnum__173,
    __dummyEnum__174,
    __dummyEnum__175,
    __dummyEnum__176,
    __dummyEnum__177,
    __dummyEnum__178,
    __dummyEnum__179,
    __dummyEnum__180,
    __dummyEnum__181,
    __dummyEnum__182,
    __dummyEnum__183,
    __dummyEnum__184,
    __dummyEnum__185,
    __dummyEnum__186,
    __dummyEnum__187,
    __dummyEnum__188,
    __dummyEnum__189,
    __dummyEnum__190,
    __dummyEnum__191,
    __dummyEnum__192,
    __dummyEnum__193,
    __dummyEnum__194,
    __dummyEnum__195,
    __dummyEnum__196,
    __dummyEnum__197,
    __dummyEnum__198,
    __dummyEnum__199,
    __dummyEnum__200,
    __dummyEnum__201,
    __dummyEnum__202,
    __dummyEnum__203,
    __dummyEnum__204,
    __dummyEnum__205,
    __dummyEnum__206,
    __dummyEnum__207,
    __dummyEnum__208,
    __dummyEnum__209,
    __dummyEnum__210,
    __dummyEnum__211,
    __dummyEnum__212,
    __dummyEnum__213,
    __dummyEnum__214,
    __dummyEnum__215,
    __dummyEnum__216,
    __dummyEnum__217,
    __dummyEnum__218,
    __dummyEnum__219,
    __dummyEnum__220,
    __dummyEnum__221,
    __dummyEnum__222,
    __dummyEnum__223,
    __dummyEnum__224,
    __dummyEnum__225,
    __dummyEnum__226,
    __dummyEnum__227,
    __dummyEnum__228,
    __dummyEnum__229,
    __dummyEnum__230,
    __dummyEnum__231,
    /**
     * DNS AXFR record.
     */
    ZoneTransfer,
    /**
     * DNS MAILB record.
     */
    MailboxRecords,
    /**
     * DNS MAILA record.
     */
    MailAgentRecords,
    /**
     * Wildcard matching all records (*).
     */
    All;

    public static RecordType valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.ordinal() == value).findFirst().get();
    }
}
