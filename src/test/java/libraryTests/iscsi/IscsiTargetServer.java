/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libraryTests.iscsi;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jscsi.target.Configuration;
import org.jscsi.target.Target;
import org.jscsi.target.TargetServer;
import org.jscsi.target.storage.IStorageModule;
import org.jscsi.target.storage.SynchronizedRandomAccessStorageModule;

import static java.lang.System.getLogger;


/**
 * An in-VM <a href="https://github.com/sebastiangraf/jSCSI">jSCSI</a> target,
 * serving one file as one LUN, so that {@link discUtils.iscsi.Initiator} has
 * something to talk to.
 * <p>
 * The portal listens on the loopback interface on a free port, which keeps an
 * iSCSI target that may already own the machine's port 3260, and concurrent
 * builds, out of the way.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 */
class IscsiTargetServer implements Closeable {

    private static final Logger logger = getLogger(IscsiTargetServer.class.getName());

    /** jSCSI serves 512 byte blocks, see {@link IStorageModule#getBlockSize()} */
    static final int BLOCK_SIZE = 512;

    static final String TARGET_NAME = "iqn.2026-01.local.discutils:test";

    static final String TARGET_ALIAS = "discUtils test target";

    /** {@link Configuration} only takes the external port as an argument */
    private static class PortConfiguration extends Configuration {

        PortConfiguration(String address, int port) throws IOException {
            super(address, address, port);
            this.port = port;
        }
    }

    private final int port;

    private final TargetServer server;

    private final ExecutorService executor;

    private final Future<?> serving;

    /**
     * Starts serving {@code image} and returns once the portal accepts
     * connections.
     *
     * @param image the backing file, its length should be a multiple of
     *            {@link #BLOCK_SIZE}
     */
    IscsiTargetServer(Path image) throws Exception {
        port = freePort();

        Configuration configuration = new PortConfiguration("127.0.0.1", port);
        File file = image.toFile();
        IStorageModule storage = new SynchronizedRandomAccessStorageModule(file.length() / BLOCK_SIZE, file);
        configuration.getTargets().add(new Target(TARGET_NAME, TARGET_ALIAS, storage));

        server = new TargetServer(configuration);
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "jscsi-target");
            thread.setDaemon(true);
            return thread;
        });
        serving = executor.submit(server);
        awaitPortal();
logger.log(Level.DEBUG, "iscsi target: " + getPortalAddress() + ", " + file.length() + " bytes");
    }

    /** the portal address in the {@code host:port} form the initiator parses */
    String getPortalAddress() {
        return "127.0.0.1:" + port;
    }

    /** a port nobody listens on yet, the target binds it a moment later */
    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * The server binds asynchronously, so wait until its channel is listening.
     * <p>
     * Neither of the two obvious probes works: connecting shuts the target down,
     * because the accept loop of {@code TargetServer} ends on any
     * {@link IOException} and a connection that does not go on to send a login
     * request causes one; and binding the port ourselves races the target for
     * it. So look at the server's own channel instead.
     */
    private void awaitPortal() throws Exception {
        for (int i = 0; i < 400; i++) {
            if (serving.isDone()) {
                // let a failure of the server thread surface
                serving.get();
            }
            if (isListening()) {
                return;
            }
            Thread.sleep(25);
        }
        throw new IOException("iscsi target did not come up on port " + port);
    }

    /** whether the portal of the target is bound to its port already */
    private boolean isListening() throws Exception {
        ServerSocketChannel channel = (ServerSocketChannel) get(server, "serverSocketChannel");
        return channel != null && channel.socket().isBound();
    }

    @Override
    public void close() throws IOException {
        server.stop();
        // TargetServer#stop leaves the portal in accept() and its worker pool,
        // whose threads are not daemons, running. Without this the jvm lingers
        // until the pool times its idle threads out.
        closeQuietly(server, "serverSocketChannel");
        shutdownQuietly(server, "workerPool");

        serving.cancel(true);
        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void closeQuietly(Object owner, String field) {
        try {
            Closeable closeable = (Closeable) get(owner, field);
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
logger.log(Level.DEBUG, "closing " + field, e);
        }
    }

    private static void shutdownQuietly(Object owner, String field) {
        try {
            ((ExecutorService) get(owner, field)).shutdownNow();
        } catch (Exception e) {
logger.log(Level.DEBUG, "shutting down " + field, e);
        }
    }

    private static Object get(Object owner, String field) throws Exception {
        Field f = owner.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return f.get(owner);
    }
}
