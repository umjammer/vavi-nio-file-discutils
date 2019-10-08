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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.bouncycastle.util.IPAddress;


/**
 * Implements the (conventional) unicast DNS protocol.
 */
public final class UnicastDnsClient extends DnsClient {
    private short _nextTransId;

    private final InetSocketAddress[] _servers;

    private final int maxRetries = 3;

    private final int responseTimeout = 2000;

    /**
     * Initializes a new instance of the UnicastDnsClient class.
     *
     * This constructor attempts to detect the DNS servers in use by the local
     * OS, and use those servers.
     */
    public UnicastDnsClient() {
        this(getDefaultDnsServers());
    }

    /**
     * Initializes a new instance of the UnicastDnsClient class, using nominated
     * DNS servers.
     *
     * @param servers The servers to use (non-standard ports may be specified).
     */
    public UnicastDnsClient(InetSocketAddress... servers) {
        _nextTransId = (short) (new Random()).nextInt();
        _servers = servers;
    }

    /**
     * Initializes a new instance of the UnicastDnsClient class, using nominated
     * DNS servers.
     *
     * @param servers The servers to use (the default DNS port, 53, is used).
     */
    public UnicastDnsClient(InetAddress... servers) {
        _nextTransId = (short) (new Random()).nextInt();
        _servers = new InetSocketAddress[servers.length];
        for (int i = 0; i < servers.length; ++i) {
            _servers[i] = new InetSocketAddress(servers[i], 53);
        }
    }

    /**
     * Flushes any cached DNS records.
     */
    public void flushCache() {
    }

    // Nothing to do.
    /**
     * Looks up a record in DNS.
     *
     * @param name The name to lookup.
     * @param type The type of record requested.
     * @return The records returned by the DNS server, if any.
     */
    public ResourceRecord[] lookup(String name, RecordType type) {
        short transactionId = _nextTransId++;
        String normName = normalizeDomainName(name);
        DatagramChannel udpClient = DatagramChannel.open();
        try {
            ByteBuffer result = udpClient.beginReceive(null, null);
            PacketWriter writer = new PacketWriter(1800);
            Message msg = new Message();
            msg.setTransactionId(transactionId);
            msg.setFlags(new MessageFlags(false, OpCode.Query, false, false, false, false, ResponseCode.Success));
            msg.getQuestions().add(new Question());
            msg.writeTo(writer);
            byte[] msgBytes = writer.getBytes();
            for (InetSocketAddress server : _servers) {
                udpClient.send(ByteBuffer.wrap(msgBytes), server);
            }
            for (int i = 0; i < maxRetries; ++i) {
                long now = System.currentTimeMillis();
                while (result.AsyncWaitHandle.WaitOne(Math.max(responseTimeout - (System.currentTimeMillis() - now), 0))) {
                    try {
                        InetSocketAddress[] sourceEndPoint = new InetSocketAddress[1];
                        byte[] packetBytes = udpClient.endReceive(result, sourceEndPoint);
                        PacketReader reader = new PacketReader(packetBytes);
                        Message response = Message.read(reader);
                        if (response.getTransactionId() == transactionId) {
                            return response.getAnswers().toArray(new ResourceRecord[0]);
                        }

                    } catch (Exception __dummyCatchVar0) {
                    }
                }
            }
        } finally {
            if (udpClient != null)
                udpClient.close();
        }
        return null;
    }

    // Do nothing - bad packet (probably...)
    private static InetSocketAddress[] getDefaultDnsServers() {
        Map<InetSocketAddress, Object> addresses = new HashMap<>();
        for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (nic.isUp()) {
                Hashtable<String, String> env = new Hashtable<>();
                env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
                DirContext ictx = new InitialDirContext(env);
                String dnsServers = (String) ictx.getEnvironment().get("java.naming.provider.url");
                for (String _address : dnsServers.split(",")) {
                    URI uri = URI.create(_address);
                    InetSocketAddress address = new InetSocketAddress(uri.getHost(), uri.getPort());
                    if (address.AddressFamily == AddressFamily.InterNetwork && !addresses.containsKey(address)) {
                        addresses.put(address, null);
                    }
                }
            }
        }
        return new ArrayList<>(addresses.keySet()).toArray(new InetSocketAddress[addresses.size()]);
    }
}
