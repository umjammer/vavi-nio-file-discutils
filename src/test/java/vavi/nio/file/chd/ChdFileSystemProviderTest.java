/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.chd;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * ChdFileSystemProviderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-09-30 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class ChdFileSystemProviderTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property
    String chd = "src/test/resources/test.chd";

    @Property(name = "chd.files")
    int chdFiles = 5;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    @DisplayName("list")
    void test1() throws Exception {
Debug.print("chd: " + chd);
        URI subUri = Path.of(chd).toUri();
Debug.print("subUri: " + subUri);
Debug.print("subUri.path: " + subUri.getPath());
        URI uri = URI.create("chd:" + subUri);
Debug.print("uri: " + uri);

        FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());

        Path root = fs.getRootDirectories().iterator().next();
        Files.walk(root).forEach(System.err::println);
        assertEquals(chdFiles, Files.list(root).count());

        fs.close();
    }

    @Test
    @DisplayName("download")
    void test2() throws Exception {
Debug.print("chd: " + chd);
        URI subUri = Path.of(chd).toUri();
Debug.print("subUri: " + subUri);
Debug.print("subUri.path: " + subUri.getPath());
        URI uri = URI.create("chd:" + subUri);
Debug.print("uri: " + uri);

        FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());

        Path root = fs.getRootDirectories().iterator().next();
        Path out = Path.of("tmp", "test2.download");
        Files.copy(root.resolve("FILE3.txt"), out, StandardCopyOption.REPLACE_EXISTING);
        assertEquals(6, Files.size(out));

        fs.close();
    }
}
