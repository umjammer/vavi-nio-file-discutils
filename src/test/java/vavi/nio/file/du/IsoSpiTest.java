/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.du;

import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.github.fge.filesystem.exceptions.ReadOnlyAttributeException;
import discUtils.iso9660.CDBuilder;
import discUtils.iso9660.CommonVolumeDescriptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * JSR-203 access to an ISO 9660 image, built on the fly so that the whole
 * round trip (build, detect, list, read) is covered.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @see vavi.nio.file.du.DuFileSystemProvider
 */
class IsoSpiTest {

    static final String VOLUME_ID = "A_SAMPLE_DISK";

    static final String HELLO = "Hello World!";

    /** the built image, shared by all tests */
    static Path iso;

    @BeforeAll
    static void beforeAll(@TempDir Path dir) throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.setUseJoliet(true);
        builder.setVolumeIdentifier(VOLUME_ID);
        builder.addFile("FOLDER" + File.separator + "HELLO.TXT", HELLO.getBytes(US_ASCII));
        builder.addFile("README.TXT", new byte[8192]);
        builder.addDirectory("EMPTY");

        iso = dir.resolve("sample.iso");
        builder.build(iso.toString());
    }

    static FileSystem newFileSystem() throws Exception {
        URI uri = DuFileSystemProvider.createURI(iso.toString());
        return new DuFileSystemProvider().newFileSystem(uri, Collections.emptyMap());
    }

    @Test
    @DisplayName("walk an iso, sub directories included")
    void walk() throws Exception {
        try (FileSystem fs = newFileSystem()) {
            Path root = fs.getRootDirectories().iterator().next();
            try (Stream<Path> walk = Files.walk(root)) {
                List<String> paths = walk.map(Path::toString).sorted().toList();
                assertEquals(List.of("/", "/EMPTY", "/FOLDER", "/FOLDER/HELLO.TXT", "/README.TXT"), paths);
            }

            assertTrue(Files.isDirectory(root.resolve("FOLDER")));
            assertTrue(Files.isRegularFile(root.resolve("FOLDER/HELLO.TXT")));
            assertEquals(HELLO.length(), Files.size(root.resolve("FOLDER/HELLO.TXT")));
            assertEquals(8192, Files.size(root.resolve("README.TXT")));
        }
    }

    @Test
    @DisplayName("read a file out of a sub directory")
    void read() throws Exception {
        try (FileSystem fs = newFileSystem()) {
            Path from = fs.getPath("/FOLDER/HELLO.TXT");
            assertEquals(HELLO, new String(Files.readAllBytes(from), US_ASCII));
        }
    }

    @Test
    @DisplayName("copy a file out of an iso")
    void copy(@TempDir Path dir) throws Exception {
        try (FileSystem fs = newFileSystem()) {
            Path from = fs.getPath("/README.TXT");
            Path to = dir.resolve("README.TXT");
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
            assertEquals(Files.size(from), Files.size(to));
        }
    }

    @Test
    @DisplayName("an iso is a read only, fully allocated file store")
    void fileStore() throws Exception {
        try (FileSystem fs = newFileSystem()) {
            FileStore store = fs.getFileStores().iterator().next();
            assertTrue(store.getTotalSpace() >= Files.size(iso), "totalSpace: " + store.getTotalSpace());
            assertEquals(store.getTotalSpace(), store.getUsableSpace());
            assertEquals(0, store.getUnallocatedSpace());
        }
    }

    @Test
    @DisplayName("the volume descriptor is readable as user attributes")
    void userAttributes() throws Exception {
        try (FileSystem fs = newFileSystem()) {
            Path path = fs.getPath("/FOLDER/HELLO.TXT");
            UserDefinedFileAttributeView view = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);

            assertTrue(view.list().contains("volumeIdentifier"), "list: " + view.list());
            assertEquals(VOLUME_ID, read(view, "volumeIdentifier"));
            // the spi asks for joliet, so the supplementary descriptor is the active one
            assertEquals("Joliet", read(view, "activeVariant"));
            assertEquals("2048", read(view, "logicalBlockSize"));
            assertEquals("1", read(view, "volumeSequenceNumber"));

            // volume meta data is the same wherever it is asked for
            assertEquals(VOLUME_ID, read(Files.getFileAttributeView(fs.getPath("/"),
                    UserDefinedFileAttributeView.class), "volumeIdentifier"));

            assertThrows(IllegalArgumentException.class, () -> view.size("noSuchAttribute"));
            // an iso can not be annotated
            assertThrows(ReadOnlyAttributeException.class,
                    () -> view.write("volumeIdentifier", ByteBuffer.wrap(new byte[1])));
        }
    }

    @Test
    @DisplayName("user:* reads the whole volume descriptor at once")
    void allUserAttributes() throws Exception {
        try (FileSystem fs = newFileSystem()) {
            var all = Files.readAttributes(fs.getPath("/README.TXT"), "user:*");
            assertEquals(VOLUME_ID, new String((byte[]) all.get("volumeIdentifier"), UTF_8));
            assertTrue(all.containsKey("creationDateAndTime"), "keys: " + all.keySet());
            // an image built by this library leaves the optional identifiers empty
            assertEquals("", new String((byte[]) all.get("publisherIdentifier"), UTF_8));
        }
    }

    @Test
    @DisplayName("a real world iso, not built by this library")
    void bundledIso() throws Exception {
        Path file = Paths.get(IsoSpiTest.class.getResource("/test.iso").toURI());
        URI uri = DuFileSystemProvider.createURI(file.toString());
        try (FileSystem fs = new DuFileSystemProvider().newFileSystem(uri, Collections.emptyMap())) {
            Path root = fs.getRootDirectories().iterator().next();
            try (Stream<Path> list = Files.list(root)) {
                List<String> paths = list.map(Path::toString).sorted().toList();
                assertEquals(List.of("/INET.exe", "/README.txt", "/autorun.inf"), paths);
            }

            UserDefinedFileAttributeView view = Files.getFileAttributeView(root, UserDefinedFileAttributeView.class);
            assertFalse(read(view, "volumeIdentifier").isEmpty());
            assertEquals("2048", read(view, "logicalBlockSize"));

            FileStore store = fs.getFileStores().iterator().next();
            assertEquals(store.getTotalSpace(), store.getUsableSpace());
        }
    }

    /** {@link CommonVolumeDescriptor} values are UTF-8 encoded text */
    static String read(UserDefinedFileAttributeView view, String name) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(view.size(name));
        view.read(name, buffer);
        return new String(buffer.array(), StandardCharsets.UTF_8);
    }
}
