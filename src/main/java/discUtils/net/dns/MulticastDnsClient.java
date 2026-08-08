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

import java.io.Closeable;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.System.getLogger;


/**
 * Implements the Multicast DNS (mDNS) protocol.
 *
 * This implementation is a hybrid of a 'proper' mDNS resolver and a classic DNS
 * resolver configured to use the mDNS multicast address. It asks from the mDNS port
 * where it can claim one - a query from there is a real mDNS query, answered to the
 * whole group, which the same socket listens to. Claiming port 5353 is best effort
 * though: this code is loaded in arbitrary processes and on a machine already
 * running a responder (every mac does) the bind only succeeds where the platform
 * offers SO_REUSEPORT. Failing that it falls back to asking from an ephemeral port,
 * a "one-shot" (legacy) query, which a responder answers by unicast.
 */
public final class MulticastDnsClient extends DnsClient implements Closeable {

    private static final Logger logger = getLogger(MulticastDnsClient.class.getName());

    /** the mDNS link local multicast group */
    private static final String MDNS_GROUP = "224.0.0.251";

    private static final int MDNS_PORT = 5353;

    /** how long to wait for responses to a query */
    private static final int RESPONSE_TIMEOUT = 3000;

    /** how long to wait before repeating an unanswered query */
    private static final int RETRY_INTERVAL = 250;

    private static final int MAX_PACKET_SIZE = 8972;

    private Map<String, Map<RecordType, List<ResourceRecord>>> cache;

    private short nextTransId;

    private final Map<Short, Transaction> transactions;

    /** sends queries and receives the unicast responses to them */
    private DatagramChannel queryChannel;

    /** joined to the mDNS group, receives the multicast responses, may be null */
    private DatagramChannel responseChannel;

    private final List<MembershipKey> memberships = new ArrayList<>();

    private Selector selector;

    private final InetAddress groupAddress;

    private static final Random random = new Random();

    private final ExecutorService es = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "mdns-receiver");
        t.setDaemon(true);
        return t;
    });

    /**
     * Initializes a new instance of the MulticastDnsClient class.
     */
    public MulticastDnsClient() {
        try {
            nextTransId = (short) random.nextInt();
            transactions = new HashMap<>();
            cache = new HashMap<>();
            groupAddress = InetAddress.getByName(MDNS_GROUP);

            selector = Selector.open();

            queryChannel = openChannel(0);
            queryChannel.register(selector, SelectionKey.OP_READ);

            responseChannel = openResponseChannel();
            if (responseChannel != null) {
                responseChannel.register(selector, SelectionKey.OP_READ);
            }

            es.execute(this::receiveLoop);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    private static DatagramChannel openChannel(int port) throws IOException {
        DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET);
        channel.configureBlocking(false);
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        // link local is where mDNS lives, but a router in between must not eat it
        channel.setOption(StandardSocketOptions.IP_MULTICAST_TTL, 255);
        channel.bind(new InetSocketAddress(port));
        return channel;
    }

    /**
     * Binds the mDNS port and joins the group on every interface that can carry
     * multicast. A responder already owns port 5353 on most machines, so failing to
     * bind is normal - queries then go out from {@link #queryChannel} instead, and
     * are answered to it by unicast.
     *
     * @return null when the port could not be claimed
     */
    private DatagramChannel openResponseChannel() {
        DatagramChannel channel = null;
        try {
            channel = DatagramChannel.open(StandardProtocolFamily.INET);
            channel.configureBlocking(false);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.setOption(StandardSocketOptions.IP_MULTICAST_TTL, 255);
            try {
                channel.setOption(StandardSocketOptions.SO_REUSEPORT, true);
            } catch (UnsupportedOperationException e) {
                logger.log(Level.DEBUG, "no SO_REUSEPORT: " + e.getMessage());
            }
            channel.bind(new InetSocketAddress(MDNS_PORT));

            for (NetworkInterface nic : multicastInterfaces()) {
                try {
                    memberships.add(channel.join(groupAddress, nic));
                } catch (IOException e) {
                    logger.log(Level.DEBUG, "join failed on " + nic.getName() + ": " + e.getMessage());
                }
            }

            if (memberships.isEmpty()) {
                channel.close();
                return null;
            }

            return channel;
        } catch (IOException e) {
            logger.log(Level.DEBUG, "cannot listen on the mDNS port, unicast responses only: " + e.getMessage());
            try {
                if (channel != null) {
                    channel.close();
                }
            } catch (IOException f) {
                logger.log(Level.DEBUG, f.getMessage(), f);
            }
            return null;
        }
    }

    /** @return the up, multicast capable interfaces, loopback included (a responder may be local) */
    private static List<NetworkInterface> multicastInterfaces() {
        try {
            List<NetworkInterface> result = new ArrayList<>();
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                boolean hasIp4 = Collections.list(nic.getInetAddresses()).stream()
                        .anyMatch(a -> a.getAddress().length == 4);
                if (nic.isUp() && nic.supportsMulticast() && hasIp4) {
                    result.add(nic);
                }
            }
            return result;
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    private void receiveLoop() {
        ByteBuffer packetBytes = ByteBuffer.allocate(MAX_PACKET_SIZE);
        while (selector.isOpen()) {
            try {
                if (selector.select(500) == 0) {
                    continue;
                }
                Iterator<SelectionKey> i = selector.selectedKeys().iterator();
                while (i.hasNext()) {
                    SelectionKey key = i.next();
                    i.remove();
                    if (!key.isValid() || !key.isReadable()) {
                        continue;
                    }
                    DatagramChannel channel = (DatagramChannel) key.channel();
                    SocketAddress from;
                    // a single readiness can cover several datagrams
                    while ((from = channel.receive(packetBytes.clear())) != null) {
                        packetBytes.flip();
                        byte[] bytes = new byte[packetBytes.remaining()];
                        packetBytes.get(bytes);
logger.log(Level.TRACE, "receive: " + bytes.length + " from " + from);
                        try {
                            receiveCallback(bytes);
                        } catch (RuntimeException e) {
                            // a malformed (or simply unsupported) packet must not kill the receiver
                            logger.log(Level.DEBUG, "cannot parse a response from " + from, e);
                        }
                    }
                }
            } catch (ClosedSelectorException | AsynchronousCloseException e) {
                break;
            } catch (IOException e) {
                logger.log(Level.INFO, e.getMessage(), e);
            }
        }
logger.log(Level.DEBUG, "receiver exit");
    }

    /**
     * Disposes of this instance.
     */
    @Override public void close() throws IOException {
        for (MembershipKey key : memberships) {
            key.drop();
        }
        memberships.clear();

        if (selector != null) {
            selector.close();
            selector = null;
        }
        if (queryChannel != null) {
            queryChannel.close();
            queryChannel = null;
        }
        if (responseChannel != null) {
            responseChannel.close();
            responseChannel = null;
        }
        es.shutdownNow();
    }

    /**
     * Flushes any cached DNS records.
     */
    @Override public void flushCache() {
        synchronized (transactions) {
            cache = new HashMap<>();
        }
    }

    /**
     * Looks up a record in DNS.
     *
     * @param name The name to lookup.
     * @param type The type of record requested.
     * @return The records returned by the DNS server, if any.
     */
    @Override public ResourceRecord[] lookup(String name, RecordType type) {
        String normName = normalizeDomainName(name);

        synchronized (transactions) {
            expireRecords();

            if (cache.containsKey(normName.toUpperCase())) {
                Map<RecordType, List<ResourceRecord>> typeRecords = cache.get(normName.toUpperCase());
                if (typeRecords.containsKey(type)) {
                    List<ResourceRecord> records = typeRecords.get(type);
                    return records.toArray(new ResourceRecord[0]);
                }
            }
        }

        return queryNetwork(name, type);
    }

    private static void addRecord(Map<String, Map<RecordType, List<ResourceRecord>>> store, ResourceRecord record) {
        Map<RecordType, List<ResourceRecord>> nameRec;
        if (!store.containsKey(record.getName().toUpperCase())) {
            nameRec = new HashMap<>();
            store.put(record.getName().toUpperCase(), nameRec);
        }
        nameRec = store.get(record.getName().toUpperCase());

        List<ResourceRecord> records;
        if (!nameRec.containsKey(record.getRecordType())) {
            records = new ArrayList<>();
            nameRec.put(record.getRecordType(), records);
        }
        records = nameRec.get(record.getRecordType());

        // the same record arrives once per interface it is announced on
        if (records.stream().noneMatch(r -> sameRecord(r, record))) {
            records.add(record);
        }
    }

    /** compares two records by their identity, ignoring the expiry a re-announce refreshes */
    private static boolean sameRecord(ResourceRecord a, ResourceRecord b) {
        if (a.getRecordType() != b.getRecordType() || !a.getName().equalsIgnoreCase(b.getName())) {
            return false;
        }
        if (a instanceof PointerRecord && b instanceof PointerRecord) {
            return ((PointerRecord) a).getTargetName().equals(((PointerRecord) b).getTargetName());
        }
        if (a instanceof IP4AddressRecord && b instanceof IP4AddressRecord) {
            return ((IP4AddressRecord) a).getAddress().equals(((IP4AddressRecord) b).getAddress());
        }
        if (a instanceof ServiceRecord && b instanceof ServiceRecord) {
            return ((ServiceRecord) a).getTarget().equals(((ServiceRecord) b).getTarget()) &&
                   ((ServiceRecord) a).getPort() == ((ServiceRecord) b).getPort();
        }
        return true;
    }

    private ResourceRecord[] queryNetwork(String name, RecordType type) {
        short transactionId = nextTransId++;
        String normName = normalizeDomainName(name);

        Transaction transaction = new Transaction(normName, type);
        try {
            synchronized (transactions) {
                transactions.put(transactionId, transaction);
            }

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

            // a query is a datagram, and a responder that is still announcing itself
            // may not answer the first one, so keep asking until the window is out
            long deadline = System.currentTimeMillis() + RESPONSE_TIMEOUT;
            long interval = RETRY_INTERVAL;
            long remaining;
            while ((remaining = deadline - System.currentTimeMillis()) > 0) {
                send(msgBytes);

                long wait = Math.min(interval, remaining);
                if (type == RecordType.Pointer) {
                    // a browse is answered by every host sharing the service, so
                    // collect for the whole window rather than stop with the first
                    Thread.sleep(wait);
                } else if (transaction.getCompleteEvent().await(wait, TimeUnit.MILLISECONDS)) {
                    break;
                }
                interval *= 2;
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            synchronized (transactions) {
                transactions.remove(transactionId);
            }
        }

        synchronized (transactions) {
            return transaction.getAnswers().toArray(new ResourceRecord[0]);
        }
    }

    /**
     * Sends a query to the mDNS group over every multicast capable interface - the
     * default route is not necessarily the one the responder is listening on.
     */
    private void send(byte[] msgBytes) throws IOException {
        InetSocketAddress mDnsAddress = new InetSocketAddress(groupAddress, MDNS_PORT);
        // asking from the mDNS port is what makes this a real mDNS query rather than
        // a one-shot one, and a real query is answered to the group, where the
        // socket listening on that port picks the answer up
        DatagramChannel channel = responseChannel != null ? responseChannel : queryChannel;
        boolean sent = false;
        for (NetworkInterface nic : multicastInterfaces()) {
            try {
                channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, nic);
                channel.send(ByteBuffer.wrap(msgBytes), mDnsAddress);
                sent = true;
logger.log(Level.TRACE, "send: " + msgBytes.length + " over " + nic.getName());
            } catch (IOException e) {
                logger.log(Level.DEBUG, "cannot query over " + nic.getName() + ": " + e.getMessage());
            }
        }

        if (!sent) {
            channel.send(ByteBuffer.wrap(msgBytes), mDnsAddress);
        }
    }

    private void expireRecords() {
        long now = System.currentTimeMillis();

        List<String> removeNames = new ArrayList<>();

        for (Map.Entry<String, Map<RecordType, List<ResourceRecord>>> nameRecord : cache.entrySet()) {
            List<RecordType> removeTypes = new ArrayList<>();

            for (Map.Entry<RecordType, List<ResourceRecord>> typeRecords : nameRecord.getValue().entrySet()) {
                int i = 0;
                while (i < typeRecords.getValue().size()) {
                    if (typeRecords.getValue().get(i).getExpiry() < now) {
                        typeRecords.getValue().remove(i);
                    } else {
                        ++i;
                    }
                }

                if (typeRecords.getValue().isEmpty()) {
                    removeTypes.add(typeRecords.getKey());
                }
            }

            for (RecordType recordType : removeTypes) {
                nameRecord.getValue().remove(recordType);
            }

            if (nameRecord.getValue().isEmpty()) {
                removeNames.add(nameRecord.getKey());
            }
        }

        for (String name : removeNames) {
            cache.remove(name);
        }
    }

    private void receiveCallback(byte[] packetBytes) {
        PacketReader reader = new PacketReader(packetBytes);

        Message msg = Message.read(reader);

        if (!msg.getFlags().isResponse()) {
            // somebody else's query, looped back to us
            return;
        }

        synchronized (transactions) {
            // a multicast response carries no transaction id (RFC 6762), so pending
            // transactions are matched by the question they asked rather than by id
            List<ResourceRecord> records = new ArrayList<>(msg.getAnswers());
            records.addAll(msg.getAdditionalRecords());

            for (ResourceRecord answer : records) {
                addRecord(cache, answer);

                for (Transaction candidate : transactions.values()) {
                    if (candidate.matches(answer) &&
                        candidate.getAnswers().stream().noneMatch(r -> sameRecord(r, answer))) {
                        candidate.getAnswers().add(answer);
                    }
                }
            }

            for (Transaction candidate : transactions.values()) {
                if (!candidate.getAnswers().isEmpty()) {
                    candidate.getCompleteEvent().countDown();
                }
            }
        }
    }
}
