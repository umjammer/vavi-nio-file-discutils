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
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
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
 * resolver configured to use the mDNS multicast address. The implementation is
 * aware of some of the unique semantics of mDNS, but because it is loaded in
 * arbitrary processes cannot claim port 5353. It attempts to honour the spirit
 * of mDNS to the extent possible whilst not binding to port 5353.
 */
public final class MulticastDnsClient extends DnsClient implements Closeable {

    private static final Logger logger = getLogger(MulticastDnsClient.class.getName());

    private Map<String, Map<RecordType, List<ResourceRecord>>> cache;

    private short nextTransId;

    private final Map<Short, Transaction> transactions;

    private DatagramChannel udpClient;

    private static Random random = new Random();

    private ExecutorService es = Executors.newSingleThreadExecutor();

    /**
     * Initializes a new instance of the MulticastDnsClient class.
     */
    public MulticastDnsClient() {
        try {
            nextTransId = (short) random.nextInt();
            transactions = new HashMap<>();
            udpClient = DatagramChannel.open(); // IPAddress.Any, 0
            udpClient.configureBlocking(false);
            udpClient.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            udpClient.socket().bind(new InetSocketAddress(5353));
            Selector selector = Selector.open();
            udpClient.register(selector, SelectionKey.OP_READ);
            es.execute(() -> {
                while (!selector.keys().isEmpty()) {
                    try {
                        selector.select(500);
                        for (SelectionKey key : selector.selectedKeys()) {
                            selector.keys().remove(key);
                            if (key.isValid()) {
                                if (key.isReadable()) {
                                    DatagramChannel channel = (DatagramChannel) key.channel();
                                    ByteBuffer packetBytes = ByteBuffer.allocate(8972);
                                    channel.receive(packetBytes);
logger.log(Level.DEBUG, "receive: " + packetBytes.position());
                                    packetBytes.flip();
                                    receiveCallback(packetBytes.array());
                                    channel.close();
                                }
                            }
                        }
                    } catch (IOException e) {
                        logger.log(Level.INFO, e.getMessage(), e);
                    }
                }
logger.log(Level.DEBUG, "receiver exit");
            });
            cache = new HashMap<>();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Disposes of this instance.
     */
    @Override public void close() throws IOException {
        es.shutdown();
        if (udpClient != null) {
            udpClient.close();
            udpClient = null;
        }
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

        records.add(record);
    }

    private ResourceRecord[] queryNetwork(String name, RecordType type) {
        short transactionId = nextTransId++;
        String normName = normalizeDomainName(name);

        Transaction transaction = new Transaction();
        try (DatagramChannel channel = DatagramChannel.open()) {
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

            InetSocketAddress mDnsAddress = new InetSocketAddress("224.0.0.251", 5353);
            channel.configureBlocking(false);
            channel.connect(mDnsAddress);
            while (!channel.isConnected())
                Thread.yield();
logger.log(Level.DEBUG, "send: " + msgBytes.length);
            channel.send(ByteBuffer.wrap(msgBytes), mDnsAddress);

            transaction.getCompleteEvent().await(2000, TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException e) {
            throw new dotnet4j.io.IOException(e);
        } finally {
            synchronized (transactions) {
                transactions.remove(transactionId);
            }
        }

        return transaction.getAnswers().toArray(new ResourceRecord[0]);
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

        synchronized (transactions) {
            Transaction transaction = transactions.get(msg.getTransactionId());

            for (ResourceRecord answer : msg.getAdditionalRecords()) {
                addRecord(cache, answer);
            }

            for (ResourceRecord answer : msg.getAnswers()) {
                if (transaction != null) {
                    transaction.getAnswers().add(answer);
                }

                addRecord(cache, answer);
            }

            if (transaction != null) {
                transaction.getCompleteEvent().countDown();
            }
        }
    }
}
