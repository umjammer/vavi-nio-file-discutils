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
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


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
    String discImageDD;
    @Property
    String discImageNHD;

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
        env.put("volumeNumber", 1);
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
}

/* */
