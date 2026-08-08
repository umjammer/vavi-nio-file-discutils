/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libraryTests.opticalDiscSharing;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import static java.lang.System.getLogger;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;


/**
 * An Optical Disc Sharing server, as much of one as {@link discUtils.opticalDiscSharing}
 * talks to: the discs are announced over bonjour as {@code _odisk._tcp} and served
 * over http by byte range, the way apple's remote disc does it and the way
 * <a href="https://github.com/umjammer/vavi-net-ods">vavi-net-ods</a> and
 * <a href="https://github.com/klattimer/pyods">pyods</a> serve a real one.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 */
public class OdsTestServer implements Closeable {

    private static final Logger logger = getLogger(OdsTestServer.class.getName());

    /** the bonjour service type a mac browses for a remote disc */
    static final String TYPE = "_odisk._tcp.local.";

    /** what a client must call itself to be told a disc's size */
    static final String STAT_USER_AGENT = "CCURLBS::statImage";

    /** what a client must call itself to be allowed to read a disc */
    static final String READ_USER_AGENT = "CCURLBS::readDataFork";

    private static final Pattern RANGE = Pattern.compile("bytes=(\\d+)-(\\d*)");

    /** disk0, disk1, ... to the image serving them */
    private final Map<String, Path> discs = new HashMap<>();

    /** the volume label each disc is announced with */
    private final Map<String, String> labels = new HashMap<>();

    private final HttpServer http;

    private final JmDNS jmDns;

    private final String instanceName;

    private ServiceInfo info;

    /**
     * Starts a server sharing the given images, and announces it.
     *
     * @param instanceName the bonjour instance name, i.e. what a mac shows as the
     *                     name of the computer sharing the disc
     * @param images       label to image file, in announcement order
     */
    public OdsTestServer(String instanceName, Map<String, Path> images) throws IOException {
        this.instanceName = instanceName;

        int i = 0;
        for (Map.Entry<String, Path> image : images.entrySet()) {
            String name = "disk" + i++;
            discs.put(name, image.getValue());
            labels.put(name, image.getKey());
        }

        // the address a client is told to fetch a disc from, so not the loopback
        // one: bonjour over the loopback interface is not answered on every platform
        InetAddress address = localAddress();

        // port 0, so a run does not fail on whatever else holds the ods port
        http = HttpServer.create(new InetSocketAddress(address, 0), 0);
        http.createContext("/", this::handle);
        http.start();

        jmDns = JmDNS.create(address, instanceName);
        info = ServiceInfo.create(TYPE, instanceName, getPort(), 0, 0, txtRecord());
        jmDns.registerService(info);
logger.log(Level.DEBUG, "announced " + info.getQualifiedName() + " on port " + getPort());
    }

    /**
     * @return the address to announce and serve on: the first interface that can
     *         carry multicast, or the loopback one when the host has no network
     */
    private static InetAddress localAddress() throws IOException {
        for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!nic.isUp() || nic.isLoopback() || !nic.supportsMulticast()) {
                continue;
            }
            for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                if (address instanceof Inet4Address) {
                    return address;
                }
            }
        }
        return InetAddress.getLoopbackAddress();
    }

    /** @return the announcement, telling a client the discs on offer */
    private Map<String, String> txtRecord() {
        Map<String, String> txt = new HashMap<>();
        // adVF without 0x200 - this server hands out its discs without asking a human
        txt.put("sys", "waMA=00:00:00:00:00:00,adVF=0x4,adDT=0x3,adCC=1");
        for (Map.Entry<String, String> label : labels.entrySet()) {
            txt.put(label.getKey(), "adVN=" + label.getValue() + ",adVT=public.cd-media");
        }
        return txt;
    }

    /** @return the port the discs are served on */
    public int getPort() {
        return http.getAddress().getPort();
    }

    /** @return the bonjour instance name the discs are announced under */
    public String getInstanceName() {
        return instanceName;
    }

    /** @return the names ("disk0", ...) of the discs on offer */
    public List<String> getDiscNames() {
        return new ArrayList<>(discs.keySet());
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
            String method = exchange.getRequestMethod();

            // an ods server serves apple's client only, anything else gets a 403
            String expected = "HEAD".equals(method) ? STAT_USER_AGENT : READ_USER_AGENT;
            if (!expected.equals(userAgent)) {
logger.log(Level.DEBUG, method + " refused, User-Agent: " + userAgent);
                respond(exchange, 403, null);
                return;
            }

            Path image = discs.get(baseName(exchange));
            if (image == null) {
                respond(exchange, 404, null);
                return;
            }

            long size = Files.size(image);
            if ("HEAD".equals(method)) {
                exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
                exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                // a HEAD carries no body, but does carry the length one would have,
                // and that header has to be in place before the response goes out
                exchange.getResponseHeaders().set("Content-Length", String.valueOf(size));
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }

            if (!"GET".equals(method)) {
                respond(exchange, 405, null);
                return;
            }

            Matcher range = RANGE.matcher(String.valueOf(exchange.getRequestHeaders().getFirst("Range")));
            if (!range.find()) {
                respond(exchange, 500, null);
                return;
            }

            long start = Long.parseLong(range.group(1));
            long end = range.group(2).isEmpty() ? size - 1 : Long.parseLong(range.group(2));
            if (start >= size || end < start) {
                respond(exchange, 416, null);
                return;
            }
            end = Math.min(end, size - 1);

            byte[] data = read(image, start, (int) (end - start + 1));
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.getResponseHeaders().set("Content-Range", "bytes %d-%d/%d".formatted(start, end, size));
            respond(exchange, 200, data);
        } catch (RuntimeException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            respond(exchange, 500, null);
        }
    }

    /**
     * @return the disc a request is for, from {@code /disk0.dmg} or from
     *         {@code /?disk=disk0.dmg} - the extension is the client's, not ours
     */
    private static String baseName(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String name = path.substring(path.lastIndexOf('/') + 1);
        String query = exchange.getRequestURI().getQuery();
        if (name.isEmpty() && query != null && query.startsWith("disk=")) {
            name = query.substring("disk=".length());
        }
        int dot = name.lastIndexOf('.');
        return dot == -1 ? name : name.substring(0, dot);
    }

    private static byte[] read(Path image, long start, int count) throws IOException {
        try (FileChannel channel = FileChannel.open(image, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate(count);
            while (buffer.hasRemaining()) {
                if (channel.read(buffer, start + buffer.position()) < 0) {
                    break;
                }
            }
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            return data;
        }
    }

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body == null ? -1 : body.length);
        if (body != null) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
        exchange.close();
    }

    @Override
    public void close() throws IOException {
        if (info != null) {
            jmDns.unregisterService(info);
            info = null;
        }
        jmDns.close();
        http.stop(0);
    }
}
