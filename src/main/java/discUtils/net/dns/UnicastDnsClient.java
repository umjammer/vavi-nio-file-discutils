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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;


/**
 * Implements the (conventional) unicast DNS protocol.
 */
public final class UnicastDnsClient extends DnsClient {

    private static final Logger logger = Logger.getLogger(UnicastDnsClient.class.getName());

    private short nextTransId;

    private final InetSocketAddress[] servers;

    private static final int maxRetries = 3;

    private static final int responseTimeout = 2000;

    private static Random random = new Random();

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
        nextTransId = (short) random.nextInt();
        this.servers = servers;
    }

    /**
     * Initializes a new instance of the UnicastDnsClient class, using nominated
     * DNS servers.
     *
     * @param servers The servers to use (the default DNS port, 53, is used).
     */
    public UnicastDnsClient(InetAddress... servers) {
        nextTransId = (short) random.nextInt();
        this.servers = new InetSocketAddress[servers.length];
        for (int i = 0; i < servers.length; ++i) {
            this.servers[i] = new InetSocketAddress(servers[i], 53);
        }
    }

    /**
     * Flushes any cached DNS records.
     */
    @Override
    public void flushCache() {
        // Nothing to do.
    }

    /**
     * Looks up a record in DNS.
     *
     * @param name The name to lookup.
     * @param type The type of record requested.
     * @return The records returned by the DNS server, if any.
     */
    @Override
    public ResourceRecord[] lookup(String name, RecordType type) {
        short transactionId = nextTransId++;
        String normName = normalizeDomainName(name);

        try (DatagramChannel udpClient = DatagramChannel.open()) {
            udpClient.bind(new InetSocketAddress(0)); // TODO needed?

            PacketWriter writer = new PacketWriter(1800);
            Message msg = new Message();
            msg.setTransactionId(transactionId);
            msg.setFlags(new MessageFlags(false, OpCode.Query, false, false, false, false, ResponseCode.Success));
            Question question = new Question();
            question.setName(normName);
            question.setType(type);
            question.setClass(RecordClass.Internet);
            msg.getQuestions().add(question);

            msg.writeTo(writer);

            byte[] msgBytes = writer.getBytes();

            for (InetSocketAddress server : servers) {
                udpClient.send(ByteBuffer.wrap(msgBytes), server);
            }

            for (int i = 0; i < maxRetries; ++i) {
                long now = System.currentTimeMillis();
                ExecutorService es = Executors.newSingleThreadExecutor();
                Future<ResourceRecord[]> future = es.submit(() -> {
                    try {
                        ByteBuffer packetBytes = ByteBuffer.allocate(8972);
                        udpClient.receive(packetBytes);
                        PacketReader reader = new PacketReader(packetBytes.array());
                        Message response = Message.read(reader);
                        if (response.getTransactionId() == transactionId) {
                            return response.getAnswers().toArray(new ResourceRecord[0]);
                        }
                    } catch (IOException e) {
                        // Do nothing - bad packet (probably...)
                        logger.info(e.getMessage());
                    }
                    return null;
                });
                try {
                    ResourceRecord[] result = future.get(Math.max(responseTimeout - (System.currentTimeMillis() - now), 0), TimeUnit.MILLISECONDS);
                    if (result != null) {
                        return result;
                    }
                } catch (TimeoutException e) {
                    logger.info(e.getMessage());
                }
            }

            return null;
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    private static InetSocketAddress[] getDefaultDnsServers() {
        try {
            Map<InetSocketAddress, Object> addresses = new HashMap<>();

            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (nic.isUp()) {
                    Hashtable<String, String> env = new Hashtable<>();
                    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
                    DirContext ictx = new InitialDirContext(env);
                    String dnsServers = (String) ictx.getEnvironment().get("java.naming.provider.url");
                    for (String url : dnsServers.split(" ")) {
                        URI uri = URI.create(url);
                        InetSocketAddress address = new InetSocketAddress(uri.getHost(), uri.getPort() == -1 ? 53 : uri.getPort());
                        if (!addresses.containsKey(address)) {
                            addresses.put(address, null);
                        }
                    }
                }
            }

            return new ArrayList<>(addresses.keySet()).toArray(new InetSocketAddress[addresses.size()]);
        } catch (NamingException | SocketException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }
}
