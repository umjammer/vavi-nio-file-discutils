/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.du;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * DiscUtilsTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/11/18 umjammer initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file://${user.dir}/local.properties")
class DiscUtilsTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property
    String discImage;
    @Property
    int volumeNumber;
    @Property
    String discImageDD;
    @Property
    String discImageNHD;
    @Property(name = "vhd")
    String discImageVHD;
    @Property(name = "vdi")
    String discImageVDI;
    @Property
    String discImageW10Reg = "src/test/resources/ntuser.dat";

    @BeforeEach
    void before() throws IOException {
        PropsEntity.Util.bind(this);
    }

    @Test
//    @Disabled
    @DisplayName("vdi/ntfs")
    void test() throws Exception {
        URI uri = DuFileSystemProvider.createURI(discImage);
        Map<String, Object> env = new HashMap<>();
        env.put("volumeNumber", volumeNumber);
        FileSystem fs = new DuFileSystemProvider().newFileSystem(uri, env);
//        Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
        Files.list(fs.getRootDirectories().iterator().next()).forEach(p -> {
            try {
                System.err.println(p + ", " + Files.getLastModifiedTime(p));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        fs.close();
    }

    @Test
    @DisplayName("raw/fat")
    void test2() throws Exception {
        String file = DiscUtilsTest.class.getResource("/fat16.dmg").getPath();
        URI uri = DuFileSystemProvider.createURI(file);
        Map<String, Object> env = new HashMap<>();
        env.put("forceType", "RAW");
        FileSystem fs = new DuFileSystemProvider().newFileSystem(uri, env);
        Files.walk(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
        fs.close();
    }

    // TODO doesn't work, wip
    @Test
    @Disabled("doesn't work")
    void test3() throws Exception {
        String file = discImageDD;
        URI uri = DuFileSystemProvider.createURI(file);
        Map<String, Object> env = new HashMap<>();
        env.put("forceType", "RAW");
        FileSystem fs = new DuFileSystemProvider().newFileSystem(uri, env);
        Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
        fs.close();
    }

    // TODO doesn't work, wip, not registered as a service provider
    @Test
    @Disabled("doesn't work")
    void test4() throws Exception {
        String file = discImageW10Reg;
        URI uri = DuFileSystemProvider.createURI(file);
        Map<String, Object> env = new HashMap<>();
        env.put("forceType", "RAW");
        FileSystem fs = new DuFileSystemProvider().newFileSystem(uri, env);
        Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
        fs.close();
    }

    @Test
    @DisplayName("vhd/fat16")
    void test5() throws Exception {
        String file = discImageVHD;
        URI uri = DuFileSystemProvider.createURI(file);
        FileSystem fs = new DuFileSystemProvider().newFileSystem(uri, Collections.emptyMap());
        Files.walk(fs.getRootDirectories().iterator().next()).forEach(System.err::println);

        Path from = fs.getPath("/WINDOWS/USER.DAT");
        Path to = Paths.get("tmp").resolve(from.getFileName().toString());
        if (!Files.exists(to.getParent())) {
            Files.createDirectories(to.getParent());
        }
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        assertEquals(Files.size(from), Files.size(to));
        assertEquals(Checksum.getChecksum(from), Checksum.getChecksum(to));
        fs.close();
    }

    @Test
    @DisplayName("vdi/fat16")
    void test6() throws Exception {
        String file = discImageVDI;
        URI uri = DuFileSystemProvider.createURI(file);
        FileSystem fs = new DuFileSystemProvider().newFileSystem(uri, Collections.emptyMap());

        Files.walk(fs.getRootDirectories().iterator().next()).forEach(System.err::println);

        Path from = fs.getPath("/WINDOWS/USER.DAT");
        Path to = Paths.get("tmp").resolve(from.getFileName().toString());
        if (!Files.exists(to.getParent())) {
            Files.createDirectories(to.getParent());
        }
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        Path original = Paths.get("../../vavi/vavi-apps-registryviewer/src/test/resources/user.dat");
        assertEquals(Files.size(from), Files.size(to));
        assertEquals(Files.size(original), Files.size(to));
        assertEquals(Checksum.getChecksum(from), Checksum.getChecksum(to));
        assertEquals(Checksum.getChecksum(original), Checksum.getChecksum(to));
        fs.close();
    }
}

/* */
