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
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * DiscUtilsTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/11/18 umjammer initial version <br>
 */
@ExtendWith(ExistsLocalPropertiesCondition.class)
@PropsEntity(url = "file://${user.dir}/local.properties")
class DiscUtilsTest {

    @Property
    String discImage;

    @BeforeEach
    void before() throws IOException {
    	PropsEntity.Util.bind(this);
    }

    @Test
    @Disabled
    void test() throws Exception {
        URI uri = URI.create("discutils:file:" + discImage);
        Map<String, Object> env = new HashMap<>();
        env.put("volumeNumber", 1);
        FileSystem fs = new DuFileSystemProvider().newFileSystem(uri, env);
        Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
    }
        Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
    }
}

/* */
