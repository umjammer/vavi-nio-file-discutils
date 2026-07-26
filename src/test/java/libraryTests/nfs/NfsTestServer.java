/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libraryTests.nfs;

import java.io.Closeable;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import discUtils.nfs.IRpcClient;
import discUtils.nfs.IRpcTransport;
import discUtils.nfs.Nfs3Client;
import discUtils.nfs.RpcCredentials;
import discUtils.nfs.RpcTcpTransport;
import discUtils.nfs.RpcUnixCredential;
import org.dcache.nfs.ExportTable;
import org.dcache.nfs.FsExport;
import org.dcache.nfs.v3.MountServer;
import org.dcache.nfs.v3.NfsServerV3;
import org.dcache.nfs.vfs.VirtualFileSystem;
import org.dcache.oncrpc4j.rpc.OncRpcProgram;
import org.dcache.oncrpc4j.rpc.OncRpcSvc;
import org.dcache.oncrpc4j.rpc.OncRpcSvcBuilder;
import org.dcache.oncrpc4j.rpc.net.IpProtocolType;

import static discUtils.nfs.RpcIdentifiers.Nfs3MountProgramIdentifier;
import static discUtils.nfs.RpcIdentifiers.Nfs3MountProgramVersion;
import static discUtils.nfs.RpcIdentifiers.Nfs3ProgramIdentifier;
import static discUtils.nfs.RpcIdentifiers.Nfs3ProgramVersion;
import static java.lang.System.getLogger;


/**
 * An in-VM <a href="https://github.com/dCache/nfs4j">nfs4j</a> NFS v3 server
 * exporting a local directory, so that {@link Nfs3Client} has something to talk
 * to.
 * <p>
 * Both the MOUNT and the NFS program are served on one port on the loopback
 * interface and no rpcbind is involved: binding port 111 needs root, and the
 * portmap lookup {@link discUtils.nfs.RpcClient} performs is exactly what
 * {@link #newRpcClient()} replaces.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @see LocalVirtualFileSystem
 */
class NfsTestServer implements Closeable {

    private static final Logger logger = getLogger(NfsTestServer.class.getName());

    /** the only export, i.e. the mount point a client may ask for */
    static final String EXPORT = "/export";

    private final OncRpcSvc service;

    private final Path exportedDirectory;

    /**
     * Starts serving, exporting the {@value #EXPORT} directory of {@code root}.
     * <p>
     * As on a real server an export path is a path of the served file system,
     * which nfs4j walks from its root, so the export is a directory below
     * {@code root} rather than {@code root} itself.
     *
     * @param root becomes the root of the served file system
     */
    NfsTestServer(Path root) throws IOException {
        exportedDirectory = root.resolve(EXPORT.substring(1));
        Files.createDirectories(exportedDirectory);

        VirtualFileSystem vfs = new LocalVirtualFileSystem(root);
        ExportTable exports = new SingleExport(FsExport.normalize(EXPORT));

        service = new OncRpcSvcBuilder()
                .withPort(0)
                .withTCP()
                .withBindAddress("127.0.0.1")
                .withoutAutoPublish()
                .withWorkerThreadIoStrategy()
                .withServiceName("nfs3-test")
                .withRpcService(new OncRpcProgram(Nfs3ProgramIdentifier, Nfs3ProgramVersion), new NfsServerV3(exports, vfs))
                .withRpcService(new OncRpcProgram(Nfs3MountProgramIdentifier, Nfs3MountProgramVersion),
                                new MountServer(exports, vfs))
                .build();
        service.start();
logger.log(Level.DEBUG, "nfs server: " + getAddress() + ":" + getPort() + " exporting " + exportedDirectory);
    }

    /** the local directory the clients of this server see under {@link #EXPORT} */
    Path getExportedDirectory() {
        return exportedDirectory;
    }

    String getAddress() {
        return "127.0.0.1";
    }

    int getPort() {
        return service.getInetSocketAddress(IpProtocolType.TCP).getPort();
    }

    /**
     * An {@link IRpcClient} talking to this server, with default unix
     * credentials.
     */
    IRpcClient newRpcClient() {
        return newRpcClient(RpcUnixCredential.Default);
    }

    IRpcClient newRpcClient(RpcCredentials credentials) {
        return new FixedPortRpcClient(getAddress(), getPort(), credentials);
    }

    /** a client of this server, rooted at {@link #EXPORT} */
    Nfs3Client newNfsClient() {
        return new Nfs3Client(newRpcClient(), EXPORT);
    }

    @Override
    public void close() throws IOException {
        service.stop();
    }

    /** one export for everybody, read write */
    private record SingleExport(String path) implements ExportTable {

        private FsExport export() {
            try {
                return new FsExport.FsExportBuilder()
                        .forClient("0.0.0.0/0")
                        .rw()
                        .trusted()
                        .withoutPrivilegedClientPort()
                        .build(path);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Stream<FsExport> exports() {
            return Stream.of(export());
        }

        @Override
        public Stream<FsExport> exports(InetAddress client) {
            return exports();
        }

        @Override
        public FsExport getExport(String path, InetAddress client) {
            return FsExport.normalize(path).equals(this.path) ? export() : null;
        }

        @Override
        public FsExport getExport(int index, InetAddress client) {
            return index == FsExport.getExportIndex(path) ? export() : null;
        }
    }

    /**
     * Serves every rpc program from one port, i.e. does what
     * {@link discUtils.nfs.RpcClient} does minus the portmap lookup.
     */
    private static final class FixedPortRpcClient implements IRpcClient {

        private final RpcCredentials credentials;

        private final String address;

        private final int port;

        /** one connection per rpc program, as {@code RpcClient} keeps too */
        private final Map<Integer, RpcTcpTransport> transports = new HashMap<>();

        private int nextTransaction = 1;

        FixedPortRpcClient(String address, int port, RpcCredentials credentials) {
            this.address = address;
            this.port = port;
            this.credentials = credentials;
        }

        @Override
        public RpcCredentials getCredentials() {
            return credentials;
        }

        @Override
        public IRpcTransport getTransport(int program, int version) {
            if (program != Nfs3ProgramIdentifier && program != Nfs3MountProgramIdentifier) {
                throw new IllegalArgumentException("unexpected rpc program: " + program);
            }
            return transports.computeIfAbsent(program, p -> new RpcTcpTransport(address, port));
        }

        @Override
        public int nextTransactionId() {
            return nextTransaction++;
        }

        @Override
        public void close() throws IOException {
            for (RpcTcpTransport transport : transports.values()) {
                transport.close();
            }
            transports.clear();
        }
    }
}
