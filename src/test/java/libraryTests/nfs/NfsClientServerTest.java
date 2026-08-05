/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libraryTests.nfs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import discUtils.core.DiscFileSystem;
import discUtils.core.UnixFilePermissions;
import discUtils.nfs.Nfs3AccessPermissions;
import discUtils.nfs.Nfs3Client;
import discUtils.nfs.Nfs3DirectoryEntry;
import discUtils.nfs.Nfs3Export;
import discUtils.nfs.Nfs3FileAttributes;
import discUtils.nfs.Nfs3FileHandle;
import discUtils.nfs.Nfs3FileSystemInfo;
import discUtils.nfs.Nfs3FileType;
import discUtils.nfs.Nfs3Mount;
import discUtils.nfs.Nfs3ReadResult;
import discUtils.nfs.Nfs3SetAttributes;
import discUtils.nfs.NfsFileSystem;
import dotnet4j.io.FileMode;
import dotnet4j.io.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;
import org.junit.jupiter.api.io.TempDir;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static libraryTests.nfs.NfsTestServer.EXPORT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * {@link discUtils.nfs} client against an nfs4j server, i.e. the client and the
 * server side of NFS v3 in one VM.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @see NfsTestServer
 */
@Timeout(value = 60, threadMode = ThreadMode.SEPARATE_THREAD)
class NfsClientServerTest {

    private static final String FS = File.separator;

    static final String HELLO = "Hello NFS!";

    /**
     * What a client sends on create and mkdir. nfs4j reads the mode of the
     * request unconditionally, so leaving it unset makes the server fault.
     */
    static Nfs3SetAttributes newFile() {
        Nfs3SetAttributes attributes = new Nfs3SetAttributes();
        attributes.setMode(UnixFilePermissions.OwnerAll);
        attributes.setSetMode(true);
        return attributes;
    }

    NfsTestServer server;

    /** what the server exports, also reachable through the local file system */
    Path exported;

    @BeforeEach
    void beforeEach(@TempDir Path root) throws Exception {
        server = new NfsTestServer(root);
        exported = server.getExportedDirectory();

        Files.writeString(exported.resolve("README.txt"), HELLO, US_ASCII);
        Files.createDirectory(exported.resolve("folder"));
        Files.write(exported.resolve("folder").resolve("data.bin"), new byte[4096]);
    }

    @AfterEach
    void afterEach() throws Exception {
        server.close();
    }

    @Test
    @DisplayName("the mount protocol lists the export")
    void exports() throws Exception {
        try (var rpc = server.newRpcClient()) {
            List<Nfs3Export> exports = new Nfs3Mount(rpc).exports();
            assertEquals(1, exports.size(), "exports: " + exports);
            assertEquals(EXPORT, exports.get(0).getDirPath());
        }
    }

    @Test
    @DisplayName("mounting the export yields a usable root handle")
    void mount() throws Exception {
        try (Nfs3Client client = server.newNfsClient()) {
            Nfs3FileHandle root = client.getRootHandle();
            assertNotNull(root);

            Nfs3FileAttributes attributes = client.getAttributes(root);
            assertEquals(Nfs3FileType.Directory, attributes.type);

            Nfs3FileSystemInfo info = client.getFileSystemInfo();
            assertTrue(info.getReadMaxBytes() > 0, "readMaxBytes: " + info.getReadMaxBytes());
            assertTrue(info.getWriteMaxBytes() > 0, "writeMaxBytes: " + info.getWriteMaxBytes());
        }
    }

    @Test
    @DisplayName("mounting something that is not exported fails")
    void mountUnknownExport() throws Exception {
        assertThrows(Exception.class, () -> new Nfs3Client(server.newRpcClient(), "/not-exported").close());
    }

    @Test
    @DisplayName("readdir sees what the local file system has")
    void readDirectory() throws Exception {
        try (Nfs3Client client = server.newNfsClient()) {
            List<String> names = client.readDirectory(client.getRootHandle(), true)
                    .stream()
                    .map(Nfs3DirectoryEntry::getName)
                    .filter(name -> !name.equals(".") && !name.equals(".."))
                    .sorted()
                    .toList();
            assertEquals(List.of("README.txt", "folder"), names);
        }
    }

    @Test
    @DisplayName("lookup, getattr and read of an existing file")
    void read() throws Exception {
        try (Nfs3Client client = server.newNfsClient()) {
            Nfs3FileHandle file = client.lookup(client.getRootHandle(), "README.txt");
            assertNotNull(file);

            Nfs3FileAttributes attributes = client.getAttributes(file);
            assertEquals(Nfs3FileType.File, attributes.type);
            assertEquals(HELLO.length(), attributes.size);

            Nfs3ReadResult result = client.read(file, 0, HELLO.length());
            assertEquals(HELLO.length(), result.getCount());
            assertTrue(result.getEof());
            assertEquals(HELLO, new String(result.getData(), 0, result.getCount(), US_ASCII));

            assertEquals(EnumSet.of(Nfs3AccessPermissions.Read),
                         client.access(file, EnumSet.of(Nfs3AccessPermissions.Read)));
        }
    }

    @Test
    @DisplayName("create and write a file, then see it on the local file system")
    void write() throws Exception {
        byte[] content = "written over nfs".getBytes(US_ASCII);

        try (Nfs3Client client = server.newNfsClient()) {
            Nfs3FileHandle file = client.create(client.getRootHandle(), "written.txt", true, newFile());
            assertNotNull(file);
            client.write(file, 0, content, 0, content.length);
        }

        assertArrayEquals(content, Files.readAllBytes(exported.resolve("written.txt")));
    }

    @Test
    @DisplayName("mkdir, rename and remove")
    void modify() throws Exception {
        try (Nfs3Client client = server.newNfsClient()) {
            Nfs3FileHandle root = client.getRootHandle();

            client.makeDirectory(root, "created", newFile());
            assertTrue(Files.isDirectory(exported.resolve("created")));

            client.rename(root, "created", root, "renamed");
            assertFalse(Files.exists(exported.resolve("created")));
            assertTrue(Files.isDirectory(exported.resolve("renamed")));

            client.removeDirectory(root, "renamed");
            assertFalse(Files.exists(exported.resolve("renamed")));

            client.remove(root, "README.txt");
            assertFalse(Files.exists(exported.resolve("README.txt")));
        }
    }

    @Test
    @DisplayName("NfsFileSystem walks and reads the export as a DiscFileSystem")
    void discFileSystem() throws Exception {
        try (DiscFileSystem fs = new NfsFileSystem(server.newRpcClient(), EXPORT)) {
            assertEquals("NFS", fs.getFriendlyName());
            assertTrue(fs.canWrite());

            // a search returns paths relative to the path searched
            assertEquals(List.of("folder"), fs.getDirectories(""));
            assertEquals(List.of("README.txt"), fs.getFiles(""));
            assertTrue(fs.fileExists("folder" + FS + "data.bin"));
            assertEquals(4096, fs.getFileInfo("folder" + FS + "data.bin").getLength());

            try (Stream file = fs.openFile("README.txt", FileMode.Open)) {
                byte[] buffer = new byte[HELLO.length()];
                assertEquals(HELLO.length(), file.read(buffer, 0, buffer.length));
                assertEquals(HELLO, new String(buffer, US_ASCII));
            }

            fs.createDirectory("nested" + FS + "deep");
            assertTrue(Files.isDirectory(exported.resolve("nested").resolve("deep")));

            try (Stream file = fs.openFile("nested" + FS + "deep" + FS + "new.txt", FileMode.Create)) {
                file.write(HELLO.getBytes(US_ASCII), 0, HELLO.length());
            }
            assertEquals(HELLO, Files.readString(exported.resolve("nested").resolve("deep").resolve("new.txt"), US_ASCII));
        }
    }
}
