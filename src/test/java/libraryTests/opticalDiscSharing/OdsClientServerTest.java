/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libraryTests.opticalDiscSharing;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import discUtils.core.VirtualDisk;
import discUtils.iso9660.CDBuilder;
import discUtils.iso9660.CDReader;
import discUtils.opticalDiscSharing.DiscInfo;
import discUtils.opticalDiscSharing.OpticalDiscService;
import discUtils.opticalDiscSharing.OpticalDiscServiceClient;
import dotnet4j.io.FileMode;
import dotnet4j.io.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;
import org.junit.jupiter.api.io.TempDir;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * {@link discUtils.opticalDiscSharing} client against an ods server, i.e. an
 * optical disc announced over bonjour, found over mDNS and read over http, all in
 * one VM. The same conversation happens against a mac sharing its drive, or against
 * <a href="https://github.com/umjammer/vavi-net-ods">vavi-net-ods</a>.
 *
 * The server announces itself on the loopback interface, so the test needs
 * multicast there - the discovery it exercises has no other way to work.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @see OdsTestServer
 */
@Timeout(value = 120, threadMode = ThreadMode.SEPARATE_THREAD)
class OdsClientServerTest {

    static final String VOLUME_ID = "ODSTEST";

    static final String SECOND_VOLUME_ID = "ODSTEST2";

    static final String HELLO = "Hello Optical Disc Sharing!";

    /** a name no other host on this network shares a disc under */
    static final String INSTANCE = "discUtils-" + UUID.randomUUID().toString().substring(0, 8);

    /**
     * How long to keep browsing for the shared disc. Bonjour probes a new service
     * before it announces it, which takes a handful of seconds, and a browse only
     * reports what has answered by the time it ends.
     */
    static final long LOOKUP_TIMEOUT = 30_000;

    static OdsTestServer server;

    /** the image the server shares, also readable straight from the file system */
    static Path iso;

    static OpticalDiscServiceClient client;

    /** the server, as the client found it announced */
    static OpticalDiscService service;

    @BeforeAll
    static void beforeAll(@TempDir Path dir) throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.setUseJoliet(true);
        builder.setVolumeIdentifier(VOLUME_ID);
        builder.addFile("README.TXT", HELLO.getBytes(US_ASCII));
        builder.addFile("FOLDER" + File.separator + "DATA.BIN", new byte[64 * 1024]);
        iso = dir.resolve("shared.iso");
        builder.build(iso.toString());

        CDBuilder second = new CDBuilder();
        second.setVolumeIdentifier(SECOND_VOLUME_ID);
        second.addFile("OTHER.TXT", "other".getBytes(US_ASCII));
        Path secondIso = dir.resolve("second.iso");
        second.build(secondIso.toString());

        Map<String, Path> images = new LinkedHashMap<>();
        images.put(VOLUME_ID, iso);
        images.put(SECOND_VOLUME_ID, secondIso);
        server = new OdsTestServer(INSTANCE, images);

        client = new OpticalDiscServiceClient();
        service = lookup(client);
    }

    @AfterAll
    static void afterAll() throws Exception {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.close();
        }
    }

    /** @return our server, as the client sees it announced, or null if it never is */
    static OpticalDiscService lookup(OpticalDiscServiceClient client) {
        long deadline = System.currentTimeMillis() + LOOKUP_TIMEOUT;
        do {
            for (OpticalDiscService found : client.lookupServices()) {
                if (INSTANCE.equals(found.getDisplayName())) {
                    return found;
                }
            }
        } while (System.currentTimeMillis() < deadline);
        return null;
    }

    @Test
    @DisplayName("a shared disc is found over mDNS, with the labels it is announced with")
    void test1() throws Exception {
        assertNotNull(service, "no service announced as " + INSTANCE);

        // this server shares its discs without asking a human, so connecting
        // changes nothing - but a mac would prompt its owner here
        service.connect(System.getProperty("user.name"), InetAddress.getLocalHost().getHostName(), 30);

        List<DiscInfo> discs = service.getAdvertisedDiscs();
        assertEquals(2, discs.size());

        DiscInfo disc = discs.stream()
                .filter(d -> VOLUME_ID.equals(d.getVolumeLabel()))
                .findFirst()
                .orElseThrow();
        assertTrue(server.getDiscNames().contains(disc.getName()));
        assertEquals("public.cd-media", disc.getVolumeType());
    }

    @Test
    @DisplayName("a shared disc reads back as the iso it is")
    void test2() throws Exception {
        assertNotNull(service, "no service announced as " + INSTANCE);

        DiscInfo info = service.getAdvertisedDiscs().stream()
                .filter(d -> VOLUME_ID.equals(d.getVolumeLabel()))
                .findFirst()
                .orElseThrow();

        try (VirtualDisk disk = service.openDisc(info.getName())) {
            assertEquals(Files.size(iso), disk.getCapacity());
            assertEquals(2048, disk.getBlockSize());

            CDReader cd = new CDReader(disk.getContent(), true);
            assertEquals(VOLUME_ID, cd.getVolumeLabel());
            assertTrue(cd.fileExists("README.TXT"));
            assertEquals(HELLO.length(), cd.getFileLength("README.TXT"));

            byte[] read = new byte[HELLO.length()];
            try (Stream stream = cd.openFile("README.TXT", FileMode.Open)) {
                assertEquals(read.length, stream.read(read, 0, read.length));
            }
            assertEquals(HELLO, new String(read, US_ASCII));

            // a read crossing the cache block boundary, i.e. several ranged gets
            assertEquals(64 * 1024, cd.getFileLength("FOLDER" + File.separator + "DATA.BIN"));
        }
    }

    @Test
    @DisplayName("a shared disc opens through an ods url")
    void test3() throws Exception {
        String url = "ods://local/" + INSTANCE + "/" + VOLUME_ID;

        try (VirtualDisk disk = VirtualDisk.openDisk(url, dotnet4j.io.FileAccess.Read)) {
            assertNotNull(disk, "no disk at " + url);
            assertEquals(Files.size(iso), disk.getCapacity());

            CDReader cd = new CDReader(disk.getContent(), true);
            assertEquals(VOLUME_ID, cd.getVolumeLabel());
        }
    }

    @Test
    @DisplayName("a disc nobody shares is not found")
    void test4() throws Exception {
        assertNull(client.lookupServices().stream()
                .filter(s -> "no such host".equals(s.getDisplayName()))
                .findFirst()
                .orElse(null));
    }
}
