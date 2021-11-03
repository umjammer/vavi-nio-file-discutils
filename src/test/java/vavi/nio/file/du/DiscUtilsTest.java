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
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * DiscUtilsTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/11/18 umjammer initial version <br>
 */
@PropsEntity(url = "file://${user.dir}/local.properties")
class DiscUtilsTest {

    @Property
    String discImage;

    @BeforeEach
    void before() throws IOException {
    	PropsEntity.Util.bind(this);
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void test() throws Exception {
        URI uri = URI.create("discutils:file:" + discImage);
        FileSystem fs = new DuFileSystemProvider().newFileSystem(uri, Collections.emptyMap());
        Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
    }
}

/* */
